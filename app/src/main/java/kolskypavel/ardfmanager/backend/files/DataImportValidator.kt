package kolskypavel.ardfmanager.backend.files

import android.content.Context
import kolskypavel.ardfmanager.R
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.constants.DataType
import kolskypavel.ardfmanager.backend.files.wrappers.DataImportWrapper
import kolskypavel.ardfmanager.backend.room.entity.Alias
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CategoryData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.RaceData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ReadoutData
import kolskypavel.ardfmanager.backend.room.enums.SIRecordType
import java.util.UUID

// Used to validate imported data / RaceData
object DataImportValidator {

    @Throws(IllegalArgumentException::class)
    fun validateDataImport(
        data: DataImportWrapper,
        raceId: UUID,
        dataType: DataType,
        dataProcessor: DataProcessor,
        context: Context
    ) {
        when (dataType) {

            //Name is required for each category and must be unique, categories can be empty
            DataType.CATEGORIES -> {
                validateCategories(data.categories, context)
            }

            // SI number and start number must be unique
            DataType.COMPETITORS -> {

                //TODO: add check for duplicate names
                val startNumbers = HashSet<Int>()
                val siNumbers = HashSet<Int>()
                for (comp in data.competitorCategories) {
                    val siNumber = comp.competitor.siNumber
                    val startNumber = comp.competitor.startNumber

                    // Check the raceId and eventually set it - should not happen
                    if (comp.competitor.raceId != raceId) {
                        comp.competitor.raceId = raceId
                    }

                    // Check if SI is duplicated in the list or in the database
                    if (siNumber != null) {
                        if (siNumbers.contains(siNumber)
                        ) {
                            throw IllegalArgumentException(
                                context.getString(
                                    R.string.data_import_competitor_duplicate_si_file,
                                    siNumber
                                )
                            )
                        }
                        if (dataProcessor.checkIfSINumberExists(siNumber, raceId)) {
                            throw IllegalArgumentException(
                                context.getString(
                                    R.string.data_import_competitor_duplicate_si_race,
                                    siNumber
                                )
                            )
                        }
                    }

                    // Start number checks
                    if (startNumbers.contains(startNumber)
                    ) {
                        throw IllegalArgumentException(
                            context.getString(
                                R.string.data_import_competitor_duplicate_start_number_file,
                                startNumber
                            )
                        )
                    }

                    if (dataProcessor.checkIfStartNumberExists(startNumber, raceId)) {
                        throw IllegalArgumentException(
                            context.getString(
                                R.string.data_import_competitor_duplicate_start_number_race,
                                startNumber
                            )
                        )
                    }

                    // Add the numbers to the sets
                    if (siNumber != null) {
                        siNumbers.add(siNumber)
                    }
                    startNumbers.add(startNumber)
                }
            }

            DataType.COMPETITOR_STARTS -> {
                // TODO: implement - based on settings
            }

            else -> {
                throw IllegalArgumentException(context.getString(R.string.data_import_format_not_supported))
            }
        }
    }

    @Throws(IllegalArgumentException::class)
    fun validateRaceDataImport(
        raceData: RaceData,
        context: Context
    ) {
        val race = raceData.race

        if (race.name.isEmpty()) {
            throw IllegalArgumentException(context.getString(R.string.data_import_race_blank_name))
        }

        validateCategories(raceData.categories, context)
        validateRaceDataAliases(raceData.aliases, context)
        validateRaceDataCompetitors(raceData.competitorData, race.id, context)
        for (unmatched in raceData.unmatchedReadoutData) {
            validateRaceDataReadoutData(unmatched, race.id, context)
        }
    }

    // Checks for duplicate category names
    @Throws(IllegalArgumentException::class)
    fun validateCategories(categories: List<CategoryData>, context: Context) {
        val names = categories.map { it.category.name }

        val catNames = names.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
        if (catNames.isNotEmpty()) {
            throw IllegalArgumentException(
                context.getString(
                    R.string.data_import_category_duplicate,
                    catNames.joinToString(", ")
                )
            )
        }
    }

    @Throws(IllegalArgumentException::class)
    fun validateRaceDataCompetitors(
        competitors: List<CompetitorData>,
        raceId: UUID,
        context: Context
    ) {

        val startNumbers = HashSet<Int>()
        val siNumbers = HashSet<Int>()

        for (comp in competitors) {
            val siNumber = comp.competitorCategory.competitor.siNumber
            val startNumber = comp.competitorCategory.competitor.startNumber

            // Check the raceId and eventually set it - should not happen
            if (comp.competitorCategory.competitor.raceId != raceId) {
                comp.competitorCategory.competitor.raceId = raceId
            }

            // Check if SI is duplicated in the list or in the database
            if (siNumber != null) {
                if (siNumbers.contains(siNumber)
                ) {
                    throw IllegalArgumentException(
                        context.getString(
                            R.string.data_import_competitor_duplicate_si_file,
                            siNumber
                        )
                    )
                }
            }

            // Start number checks
            if (startNumbers.contains(startNumber)
            ) {
                throw IllegalArgumentException(
                    context.getString(
                        R.string.data_import_competitor_duplicate_start_number_file,
                        startNumber
                    )
                )
            }

            //Validate readout
            comp.readoutData?.let { validateRaceDataReadoutData(it, raceId, context) }
        }
    }

    @Throws(IllegalArgumentException::class)
    fun validateRaceDataReadoutData(
        readoutData: ReadoutData,
        raceId: UUID,
        context: Context
    ) {

        val result = readoutData.result
        val punches = readoutData.punches.map { it -> it.punch }

        // Fill the raceId if missing
        if (result.raceId != raceId) {
            result.raceId = raceId
        }

        // Check the punches for duplicate START/FINISH punches
        var startPunchPresent = false
        var finishPunchPresent = false

        for (punch in punches) {
            if (punch.raceId != raceId) {
                punch.raceId = raceId
            }
            if (punch.resultId != result.id) {
                punch.resultId = result.id
            }

            if (punch.punchType == SIRecordType.START) {
                if (startPunchPresent) {
                    throw IllegalArgumentException(
                        context.getString(
                            R.string.data_import_readout_multiple_start,
                            result.siNumber ?: "?"
                        )
                    )
                } else {
                    startPunchPresent = true
                }
            }

            if (punch.punchType == SIRecordType.FINISH) {
                if (finishPunchPresent) {
                    throw IllegalArgumentException(
                        context.getString(
                            R.string.data_import_readout_multiple_finish,
                            result.siNumber ?: "?"
                        )
                    )
                } else {
                    finishPunchPresent = true
                }
            }
        }

    }

    // Check if either name or code of alias is duplicate
    @Throws(IllegalArgumentException::class)
    fun validateRaceDataAliases(aliases: List<Alias>, context: Context) {

        val names = aliases.map { it.name }
        val codes = aliases.map { it.siCode }

        // Find duplicates
        val duplicateNames = names.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
        val duplicateCodes = codes.groupingBy { it }.eachCount().filter { it.value > 1 }.keys

        // Build error message(s)
        val errors = mutableListOf<String>()
        if (duplicateNames.isNotEmpty()) {
            errors.add(
                context.getString(
                    R.string.data_import_alias_name_duplicate,
                    duplicateNames.joinToString(", ")
                )
            )
        }
        if (duplicateCodes.isNotEmpty()) {
            errors.add(
                context.getString(
                    R.string.data_import_alias_code_duplicate,
                    duplicateCodes.joinToString(", ")
                )
            )
        }

        // If any errors exist, throw exception
        if (errors.isNotEmpty()) {
            throw IllegalArgumentException(errors.joinToString("\n"))
        }
    }
}