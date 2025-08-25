package kolskypavel.ardfmanager.backend.files.processors

import android.content.Context
import android.util.Log
import androidx.preference.PreferenceManager
import com.github.doyaaaaaken.kotlincsv.client.CsvReader
import com.github.doyaaaaaken.kotlincsv.dsl.context.CsvReaderContext
import kolskypavel.ardfmanager.R
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.constants.DataFormat
import kolskypavel.ardfmanager.backend.files.constants.DataType
import kolskypavel.ardfmanager.backend.files.constants.FileConstants
import kolskypavel.ardfmanager.backend.files.wrappers.DataImportWrapper
import kolskypavel.ardfmanager.backend.helpers.ControlPointsHelper
import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import kolskypavel.ardfmanager.backend.results.ResultsProcessor
import kolskypavel.ardfmanager.backend.room.entity.Category
import kolskypavel.ardfmanager.backend.room.entity.Competitor
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CategoryData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorCategory
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ResultData
import kolskypavel.ardfmanager.backend.room.enums.RaceType
import kolskypavel.ardfmanager.backend.room.enums.StandardCategoryType
import kolskypavel.ardfmanager.backend.sportident.SIConstants
import kolskypavel.ardfmanager.backend.wrappers.ResultWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.Duration
import java.util.UUID

object CsvProcessor : FormatProcessor {

    override suspend fun importData(
        inStream: InputStream,
        dataType: DataType,
        race: Race,
        dataProcessor: DataProcessor,
        stopOnInvalid: Boolean
    ): DataImportWrapper {
        return when (dataType) {
            DataType.CATEGORIES -> return importCategories(
                inStream,
                race,
                stopOnInvalid,
                dataProcessor,
                dataProcessor.getContext()
            )

            DataType.COMPETITORS -> return importCompetitorData(
                inStream,
                race,
                dataProcessor.getCategoryDataFlowForRace(race.id).first().toHashSet(),
                stopOnInvalid,
                dataProcessor,
                dataProcessor.getContext()
            )

            DataType.COMPETITOR_STARTS_TIME -> return importCompetitorStarts(
                inStream,
                dataProcessor.getCompetitorDataFlowByRace(race.id).first().toHashSet(),
                stopOnInvalid,
                dataProcessor.getContext()
            )

            else -> DataImportWrapper(emptyList(), emptyList(), ArrayList())
        }
    }

    override suspend fun exportData(
        outStream: OutputStream,
        dataType: DataType,
        format: DataFormat,
        dataProcessor: DataProcessor,
        raceId: UUID
    ) {

        when (dataType) {
            DataType.CATEGORIES -> exportCategories(
                outStream,
                dataProcessor.getCategoryDataForRace(raceId)
            )

            DataType.COMPETITORS -> exportCompetitors(
                outStream,
                dataProcessor.getCompetitorDataFlowByRace(raceId).first()
            )

            DataType.COMPETITOR_STARTS_TIME,
            DataType.COMPETITOR_STARTS_CATEGORIES,
            DataType.COMPETITOR_STARTS_CLUBS -> exportStarts(
                outStream,
                dataProcessor.getCompetitorDataFlowByRace(raceId).first(),
                dataProcessor.getCurrentRace()
            )

            DataType.RESULTS -> exportResults(
                outStream,
                ResultsProcessor.getResultWrapperFlowByRace(raceId, dataProcessor).first()
            )

            DataType.READOUT_DATA -> {
                exportReadoutData(
                    outStream,
                    dataProcessor.getResultDataFlowByRace(raceId).first()
                )
            }

        }
    }


    //Use reader with semicolon separator
    private fun getReader(): CsvReader {
        val context = CsvReaderContext()
        context.delimiter = ';'
        return CsvReader(context)
    }

    private fun importCategories(
        inStream: InputStream,
        race: Race,
        stopOnInvalid: Boolean,
        dataProcessor: DataProcessor,
        context: Context
    ): DataImportWrapper {
        val readData = getReader().readAll(inStream)
        val categories = ArrayList<CategoryData>()
        val invalidLines = ArrayList<Pair<Int, String>>()

        if (readData.isNotEmpty()) {

            for (csvRow in readData.withIndex()) {
                val row = csvRow.value
                if (row.size == FileConstants.CATEGORY_CSV_COLUMNS) {

                    try {

                        val categoryName = row[0].trim()
                        val isMan = row[1].trim() == "1"
                        val maxAge = row[2].trim().toInt()
                        val length = if (row[3].isNotBlank()) {
                            row[3].trim().toFloat()
                        } else 0f
                        val climb = if (row[4].isNotBlank()) {
                            row[4].trim().toFloat()
                        } else 0f
                        val followRacePresets = row[5].trim() == "1"

                        //Check validity
                        if (categoryName.isEmpty() || maxAge <= 0 || length <= 0 || climb < 0) {
                            throw IllegalArgumentException("Invalid category data: $row")
                        }

                        val category = Category(
                            UUID.randomUUID(),
                            race.id,
                            categoryName,
                            isMan,
                            maxAge,
                            length,
                            climb,
                            0,
                            false,
                            race.raceType,
                            race.raceBand,
                            race.timeLimit,
                            ""
                        )

                        // Parse the category specific fields
                        if (!followRacePresets) {
                            val raceType = RaceType.valueOf(row[6].trim())
                            val timeLimit = row[7].trim().toLong()
                            val band = row[8].trim()

                            category.differentProperties = true
                            category.raceType = raceType
                            category.timeLimit = Duration.ofMinutes(timeLimit)
                            category.categoryBand = dataProcessor.raceBandStringToEnum(band)
                        }

                        val controlPointString = row[9].trim()
                        val controlPoints = ControlPointsHelper.getControlPointsFromString(
                            controlPointString,
                            category.id,
                            category.raceType ?: race.raceType,
                            dataProcessor.getContext()
                        )
                        category.controlPointsString = controlPointString

                        categories.add(
                            CategoryData(
                                category,
                                controlPoints,
                                emptyList()
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(
                            "CSV import",
                            "Failed to import category: ${row.joinToString(", ")}\n" + e.stackTraceToString()
                        )

                        if (stopOnInvalid) {
                            throw IllegalArgumentException(
                                context.getString(
                                    R.string.data_import_invalid_line,
                                    csvRow.index,
                                    e.message
                                )
                            )
                        }
                        invalidLines.add(Pair(csvRow.index, e.message ?: ""))
                    }
                }
            }
        }
        return DataImportWrapper(emptyList(), categories.toList(), invalidLines)
    }

    suspend fun importStandardCategories(
        type: StandardCategoryType,
        race: Race,
        dataProcessor: DataProcessor
    ): List<Category> {
        val categoryString = when (type) {
            StandardCategoryType.INTERNATIONAL -> dataProcessor.getContext().resources.getStringArray(
                R.array.standard_categories_international
            )

            StandardCategoryType.CZECH -> dataProcessor.getContext().resources.getStringArray(
                R.array.standard_categories_czech
            )
        }
        val categories = ArrayList<Category>()

        for (line in categoryString.withIndex()) {
            val split = line.value.split(";")
            if (split.size == 3 &&
                dataProcessor.getCategoryByName(split[0].trim(), race.id) == null
            ) {
                val cat = Category(
                    UUID.randomUUID(),
                    race.id,
                    split[0].trim(),
                    split[1].trim() == "1",
                    split[2].trim().toInt(),
                    0F,
                    0F,
                    line.index,
                    false,
                    null,
                    null,
                    null,
                    ""
                )
                categories.add(cat)
            }
        }

        return categories.toList()
    }

    private suspend fun importCompetitorData(
        inStream: InputStream,
        race: Race,
        categories: HashSet<CategoryData>,
        stopOnInvalid: Boolean,
        dataProcessor: DataProcessor,
        context: Context
    ): DataImportWrapper {

        val csvReader = getReader().readAll(inStream)
        val competitors = ArrayList<CompetitorCategory>()
        var currOrder =
            dataProcessor.getHighestCategoryOrder(race.id) + 1    // Used to keep order of categories correct
        var currStartNum = dataProcessor.getHighestStartNumberByRace(race.id) + 1
        val invalidLines = ArrayList<Pair<Int, String>>()

        for (csvRow in csvReader.withIndex()) {
            try {
                val row = csvRow.value
                var category: CategoryData? = null

                //Check if category exists
                if (row[4].isNotEmpty()) {
                    val catName = row[4].trim()
                    val origCat = categories.find { it.category.name == catName }
                    if (origCat != null) {
                        category = origCat
                    } else {
                        category = CategoryData(
                            Category(
                                UUID.randomUUID(),
                                race.id,
                                row[4].trim(),
                                false,
                                null,
                                0F,
                                0F,
                                currOrder,
                                false,
                                null,
                                null,
                                null,
                                ""
                            ), emptyList(), emptyList()
                        )
                        currOrder++
                        categories.add(category)
                    }
                }

                val categoryId = category?.category?.id
                var startNumber = currStartNum

                if (row[1].isNotEmpty()) {
                    startNumber = row[1].trim().toInt()
                } else {
                    currStartNum++
                }
                val firstName = row[2].trim()
                val lastName = row[3].trim()
                val isMan = row[5].trim().toIntOrNull() == 0
                val birthYear = if (row.size > 6) row[6].trim().toIntOrNull() else null
                val club = if (row.size > 7) row[7].trim() else ""
                val index = if (row.size > 8) row[8].trim() else ""
                val siNumber = row[0].trim().toIntOrNull()

                // Validate SI number
                if (siNumber != null && !SIConstants.isSINumberValid(siNumber)) {
                    throw IllegalArgumentException(
                        context.getString(
                            R.string.data_import_competitor_invalid_si,
                            csvRow.index
                        )
                    )
                }

                val drawnRelativeStartTime: Duration? =
                    if (row.size > 9 && row[9].isNotEmpty()) {
                        TimeProcessor.minuteStringToDuration(row[9].trim())
                    } else null

                // Validate first name and last name
                if (firstName.isEmpty() || lastName.isEmpty()) {
                    throw IllegalArgumentException(
                        context.getString(
                            R.string.data_import_competitor_blank_name,
                            csvRow.index
                        )
                    )
                }

                val siRent = if (row.size > 10) {
                    row[10].trim().toInt() == 1
                } else false

                val competitor = Competitor(
                    UUID.randomUUID(),
                    race.id,
                    categoryId,
                    firstName,
                    lastName,
                    club,
                    index,
                    isMan,
                    birthYear,
                    siNumber,
                    siRent,
                    startNumber,
                    drawnRelativeStartTime
                )
                if (category != null) {
                    competitors.add(CompetitorCategory(competitor, category.category))
                }
            } catch (e: Exception) {
                Log.w(
                    "CSV import",
                    "Failed to import competitor \n\" " + e.stackTraceToString()
                )

                //TODO: Add detailed information to exception
                if (stopOnInvalid) {
                    throw IllegalArgumentException(
                        context.getString(
                            R.string.data_import_invalid_line,
                            csvRow.index,
                            e.message
                        )
                    )
                }
                invalidLines.add(Pair(csvRow.index, e.message ?: ""))
            }
        }
        return DataImportWrapper(competitors, categories.toList(), invalidLines)
    }

    private fun importCompetitorStarts(
        inStream: InputStream,
        competitors: HashSet<CompetitorData>,
        stopOnInvalid: Boolean,
        context: Context
    ): DataImportWrapper {
        val csvReader = getReader().readAll(inStream)
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val preferAppStartTime =
            sharedPref.getBoolean(
                context.getString(R.string.key_files_prefer_app_start_time),
                false
            )
        val invalidLines = ArrayList<Pair<Int, String>>()

        for (csvRow in csvReader.withIndex()) {
            val row = csvRow.value
            if (row.size == FileConstants.OCM_START_CSV_COLUMNS) {
                try {
                    val startNumber = row[0].trim().toInt()
                    val relativeTime = TimeProcessor.minuteStringToDuration(row[1].trim())
                    val siNumber = row[2].trim().toIntOrNull()

                    // Validate SI number
                    if (siNumber != null && !SIConstants.isSINumberValid(siNumber)) {
                        throw IllegalArgumentException(
                            context.getString(
                                R.string.data_import_competitor_invalid_si,
                                csvRow.index
                            )
                        )
                    }

                    val match =
                        competitors.find { it.competitorCategory.competitor.startNumber == startNumber }

                    if (match != null && !preferAppStartTime) {
                        match.competitorCategory.competitor.drawnRelativeStartTime =
                            relativeTime

                        if (siNumber != null) {
                            match.competitorCategory.competitor.siNumber = siNumber
                        }
                    }
                } catch (e: Exception) {
                    Log.e(
                        "CSV import",
                        "Failed to import competitor start: \n" + e.stackTraceToString()
                    )
                    //TODO: Add detailed information to exception
                    if (stopOnInvalid) {
                        throw IllegalArgumentException(
                            context.getString(
                                R.string.data_import_invalid_line,
                                csvRow.index,
                                e.message
                            )
                        )
                    }
                    invalidLines.add(Pair(csvRow.index, e.message ?: ""))
                }
            }
        }

        return DataImportWrapper(
            competitors.map { it.competitorCategory },
            emptyList(),
            invalidLines
        )
    }


    // ------------- TODO: To be finished - non priority exports

    @Throws(IOException::class)
    suspend fun exportCategories(outStream: OutputStream, categories: List<CategoryData>) {

        withContext(Dispatchers.IO) {
            val writer = outStream.bufferedWriter()
            for (data in categories) {

                writer.write(data.category.toCSVString())
                writer.write(";")
                writer.write(data.controlPoints.size.toString())
                writer.write(";")

                //Write all control points
                for (cp in data.controlPoints.withIndex()) {
                    writer.write(cp.value.toCsvString())

                    //Separate control points by comma
                    if (cp.index < data.controlPoints.size - 1) {
                        writer.write(",")
                    }
                }
                writer.newLine()
            }
            writer.flush()
        }
    }

    @Throws(IOException::class)
    suspend fun exportCompetitors(
        outStream: OutputStream,
        competitorData: List<CompetitorData>
    ) {
        val writer = outStream.bufferedWriter()
        withContext(Dispatchers.IO) {

            for (com in competitorData) {
                writer.write(
                    com.competitorCategory.competitor.toSimpleCsvString(
                        com.competitorCategory.category?.name ?: ""
                    )
                )
                writer.newLine()
            }
            writer.flush()
        }
    }

    @Throws(IOException::class)
    suspend fun exportStarts(
        outStream: OutputStream,
        competitorData: List<CompetitorData>,
        race: Race
    ) {
        val writer = outStream.bufferedWriter()
        withContext(Dispatchers.IO) {
            for (com in competitorData) {
                val category = com.competitorCategory.category
                writer.write(
                    com.competitorCategory.competitor.toStartCsvString(
                        category?.name ?: "",
                        race.startDateTime
                    )
                )
                writer.newLine()
            }
            writer.flush()
        }
    }

    @Throws(IOException::class)
    suspend fun exportReadoutData(outStream: OutputStream, readoutData: List<ResultData>) {
        val writer = outStream.bufferedWriter()
        withContext(Dispatchers.IO) {
            for (rd in readoutData) {
                writer.write(rd.toReadoutCSVString())
                writer.newLine()
            }
            writer.flush()
        }
    }

    @Throws(IOException::class)
    suspend fun exportResults(outStream: OutputStream, results: List<ResultWrapper>) {
        val writer = outStream.bufferedWriter()
        withContext(Dispatchers.IO) {
            for (res in results) {

                writer.newLine()
            }
            writer.flush()
        }
    }
}