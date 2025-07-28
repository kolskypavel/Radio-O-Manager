package kolskypavel.ardfmanager.backend.files.processors

import android.util.Log
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
import kolskypavel.ardfmanager.backend.room.entity.Category
import kolskypavel.ardfmanager.backend.room.entity.Competitor
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CategoryData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorCategory
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ResultData
import kolskypavel.ardfmanager.backend.room.enums.RaceType
import kolskypavel.ardfmanager.backend.room.enums.StandardCategoryType
import kolskypavel.ardfmanager.backend.wrappers.ResultWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.Duration
import java.time.LocalTime
import java.util.UUID

object CsvProcessor : FormatProcessor {

    override suspend fun importData(
        inStream: InputStream,
        dataType: DataType,
        race: Race,
        dataProcessor: DataProcessor
    ): DataImportWrapper? {
        return when (dataType) {
            DataType.CATEGORIES -> return importCategories(inStream, race, dataProcessor)
            DataType.COMPETITORS -> return importCompetitorData(
                inStream,
                race,
                dataProcessor.getCategoryDataFlowForRace(race.id).first().toHashSet()
            )

            DataType.COMPETITOR_STARTS_TIME -> return importCompetitorStarts(
                inStream,
                race,
                dataProcessor.getCompetitorDataFlowByRace(race.id).first().toHashSet()
            )

            else -> DataImportWrapper(emptyList(), emptyList())
        }
    }

    override suspend fun exportData(
        outStream: OutputStream,
        dataType: DataType,
        format: DataFormat,
        dataProcessor: DataProcessor,
        raceId: UUID
    ): Boolean {
        try {

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
                    dataProcessor.getResultWrapperFlowByRace(raceId).first()
                )

                DataType.READOUT_DATA -> {
                    exportReadoutData(
                        outStream,
                        dataProcessor.getResultDataFlowByRace(raceId).first()
                    )
                }

            }
            return true
        } catch (e: Exception) {
            Log.e("EXPORT", e.stackTraceToString())
            return false
        }
    }


    //Use reader with semicolon separator
    private fun getReader(): CsvReader {
        val context = CsvReaderContext()
        context.delimiter = ';'
        return CsvReader(context)
    }

    //TODO: Finish
    private fun importCategories(
        inStream: InputStream,
        race: Race,
        dataProcessor: DataProcessor
    ): DataImportWrapper? {
        val readData = getReader().readAll(inStream)
        val categories = ArrayList<CategoryData>()

        if (readData.isNotEmpty()) {

            for (row in readData) {
                if (row.size == FileConstants.CATEGORY_CSV_COLUMNS) {
                    try {
                        val categoryName = row[0]
                        val isMan = row[1] == "1"
                        val maxAge = row[2].toInt()
                        val length = row[3].toFloat() ?: 0f
                        val climb = row[4].toFloat() ?: 0f
                        val orderInResults = row[5].toInt() ?: 0
                        val followRacePresets = row[6] == "1"

                        //Check validity
                        if (categoryName.isEmpty() || maxAge <= 0 || length <= 0 || climb < 0 || orderInResults < 0) {
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
                            orderInResults,
                            false,
                            race.raceType,
                            race.raceBand,
                            race.timeLimit,
                            ""
                        )

                        // Parse the category specific fields
                        if (!followRacePresets) {
                            val raceType = RaceType.valueOf(row[7])
                            val timeLimit = row[8].toLong()
                            val band = row[9]

                            category.differentProperties = true
                            category.raceType = raceType
                            category.timeLimit = Duration.ofMinutes(timeLimit)
                            category.categoryBand = dataProcessor.raceBandStringToEnum(band)
                        }

                        val controlPointString = row[10]
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
                        Log.e(
                            "CSV import",
                            "Failed to import categories: \n" + e.stackTraceToString()
                        )
                    }
                }
            }
        }
        return DataImportWrapper(emptyList(), categories.toList())
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
                dataProcessor.getCategoryByName(split[0], race.id) == null
            ) {
                val cat = Category(
                    UUID.randomUUID(),
                    race.id,
                    split[0],
                    split[1] == "1",
                    split[2].toInt(),
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


    private fun importCompetitorData(
        inStream: InputStream,
        race: Race,
        categories: HashSet<CategoryData>
    ): DataImportWrapper {

        val csvReader = getReader().readAll(inStream)
        val competitors = ArrayList<CompetitorCategory>()

        for (row in csvReader) {
            try {
                var category: CategoryData? = null

                //Check if category exists
                if (row[4].isNotEmpty()) {
                    val catName = row[4]
                    val origCat = categories.find { it.category.name == catName }
                    if (origCat != null) {
                        category = origCat
                    } else {
                        category = CategoryData(
                            Category(
                                UUID.randomUUID(),
                                race.id,
                                row[4],
                                false,
                                null,
                                0F,
                                0F,
                                0,
                                false,
                                race.raceType,
                                race.raceBand,
                                race.timeLimit,
                                ""
                            ), emptyList(), emptyList()
                        )
                        categories.add(category)
                    }
                }

                val categoryId = category?.category?.id
                val startNumber = row[1].toInt()
                val firstName = row[2]
                val lastName = row[3]
                val isMan = row[5].toIntOrNull() == 0
                val birthYear = if (row.size > 6) row[6].toIntOrNull() else null
                val club = if (row.size > 7) row[7] else ""
                val index = if (row.size > 8) row[8] else ""
                val siNumber = row[0].toIntOrNull()
                val drawnRelativeStartTime: Duration? =
                    if (row.size > 9 && row[9].isNotEmpty()) {
                        TimeProcessor.minuteStringToDuration(row[9])
                    } else null

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
                    false,
                    startNumber,
                    drawnRelativeStartTime
                )
                if (category != null) {
                    competitors.add(CompetitorCategory(competitor, category.category))
                }
            } catch (e: Exception) {
                Log.e(
                    "CSV import",
                    "Failed to import competitor \n\" " + e.stackTraceToString()
                )
                //TODO: Add break based on option
            }
        }
        return DataImportWrapper(competitors, categories.toList())
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

    private fun importCompetitorStarts(
        inStream: InputStream,
        race: Race,
        competitors: HashSet<CompetitorData>
    ): DataImportWrapper {
        val csvReader = getReader().readAll(inStream)

        for (start in csvReader) {
            if (start.size == FileConstants.OCM_START_CSV_COLUMNS) {
                try {
                    val startNumber = start[0].toInt()

                    val relativeTime = if (start[4].isNotEmpty()) {
                        Duration.parse(start[4])
                    } else null

                    val realTime = if (start[5].isNotEmpty()) {
                        LocalTime.parse(start[5])
                    } else null

                    val match =
                        competitors.find { it.competitorCategory.competitor.startNumber == startNumber }

                    if (match != null) {
                        if (relativeTime != null) {
                            match.competitorCategory.competitor.drawnRelativeStartTime =
                                relativeTime
                        } else if (realTime != null) {
                            match.competitorCategory.competitor.drawnRelativeStartTime =
                                Duration.between(race.startDateTime.toLocalTime(), realTime)
                        } else throw IllegalArgumentException("Nor relative or real start time entered")
                    }
                } catch (e: Exception) {
                    Log.e(
                        "CSV import",
                        "Failed to import competitor start: \n" + e.stackTraceToString()
                    )
                    //TODO: Add break based on option
                }
            }
        }

        return DataImportWrapper(competitors.map { it.competitorCategory }, emptyList())
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