package kolskypavel.ardfmanager.backend.files.processors

import android.content.Context
import androidx.preference.PreferenceManager
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
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.AliasPunch
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CategoryData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ControlPointAlias
import kolskypavel.ardfmanager.backend.room.enums.ResultStatus
import kolskypavel.ardfmanager.backend.room.enums.SIRecordType
import kolskypavel.ardfmanager.backend.wrappers.ResultWrapper
import kotlinx.coroutines.flow.first
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDateTime

/**
 * Processor for generating text-based files (TXT and HTML).
 * It handles both results and startlists by processing templates with dynamic parameters.
 */
object TextProcessor : FormatProcessor {

    override suspend fun importData(
        inStream: InputStream,
        dataType: DataType,
        race: Race,
        dataProcessor: DataProcessor
    ): DataImportWrapper {
        throw NotImplementedError("Text processor not intended for data import")
    }

    override suspend fun exportData(
        outStream: OutputStream,
        dataType: DataType,
        format: DataFormat,
        dataProcessor: DataProcessor,
        race: Race
    ) {
        when (dataType) {
            DataType.RESULTS_LIVE, DataType.RESULTS_FINAL -> exportResults(
                format,
                outStream,
                race,
                dataProcessor
            )

            DataType.STARTLIST -> exportStartlist(
                format,
                outStream,
                race,
                dataProcessor
            )

            else -> {}
        }
    }

    // --------------------------------------------------------------------------------------------
    // HIGH-LEVEL EXPORT ORCHESTRATION
    // --------------------------------------------------------------------------------------------

    /**
     * Orchestrates the export of race results. Fetches data, generates content, and writes to stream.
     */
    @Throws(IllegalArgumentException::class)
    private suspend fun exportResults(
        format: DataFormat,
        outStream: OutputStream,
        race: Race,
        dataProcessor: DataProcessor
    ) {
        val results = ResultsProcessor.getResultWrapperFlowByRace(race.id, dataProcessor).first()
        val context = dataProcessor.getContext() ?: return
        val params = getBaseParams(dataProcessor, context, race)

        if (format == DataFormat.TXT) {
            params[FileConstants.KEY_RACE_RESULTS] =
                generateTxtResults(dataProcessor, context, results, race, false)
            params[FileConstants.KEY_RACE_RESULTS_SPLITS] =
                generateTxtResults(dataProcessor, context, results, race, true)
        } else {
            params[FileConstants.KEY_RACE_RESULTS] =
                generateHtmlResults(dataProcessor, context, results, race)
        }

        val templateType = if (format == DataFormat.TXT) {
            FileConstants.TEMPLATE_TEXT_RESULTS
        } else {
            FileConstants.TEMPLATE_HTML_RESULTS
        }

        writeTemplateToStream(templateType, params, context, outStream)
    }

    /**
     * Orchestrates the export of the race startlist.
     */
    private suspend fun exportStartlist(
        format: DataFormat,
        outStream: OutputStream,
        race: Race,
        dataProcessor: DataProcessor
    ) {
        val categoryData = dataProcessor.getCategoryDataForRace(race.id)
        val context = dataProcessor.getContext() ?: return
        val params = getBaseParams(dataProcessor, context, race)

        if (format == DataFormat.TXT) {
            params[FileConstants.KEY_RACE_STARTLIST] =
                generateTxtStartListData(categoryData, context)
        } else {
            params[FileConstants.KEY_RACE_STARTLIST] =
                generateHtmlStartListData(context, categoryData)
        }

        val templateType = if (format == DataFormat.TXT) {
            FileConstants.TEMPLATE_TEXT_STARTLIST
        } else {
            FileConstants.TEMPLATE_HTML_STARTLIST
        }

        writeTemplateToStream(templateType, params, context, outStream)
    }

    // --------------------------------------------------------------------------------------------
    // TXT GENERATION LOGIC
    // --------------------------------------------------------------------------------------------

    /**
     * Generates TXT content for all result categories.
     */
    private suspend fun generateTxtResults(
        dataProcessor: DataProcessor,
        context: Context,
        results: List<ResultWrapper>,
        race: Race,
        generateSplits: Boolean
    ): String {
        val output = StringBuilder()
        val filteredResults = results.filter { rw -> rw.competitorData.any { it.readoutData != null } }

        for (rwWrapper in filteredResults.withIndex()) {
            val result = rwWrapper.value
            val category = result.category ?: continue

            // Category Header
            output.append(
                generateResCategoryHeader(
                    FileConstants.TEMPLATE_TEXT_RES_CATEGORY,
                    dataProcessor,
                    context,
                    category,
                    race
                )
            ).append("\n")

            // Competitor Rows
            for (rd in result.competitorData) {
                if (rd.readoutData != null) {
                    output.append(
                        generateTxtResCompData(
                            dataProcessor,
                            context,
                            rd,
                            generateSplits
                        )
                    ).append("\n")
                }
            }

            if (rwWrapper.index < filteredResults.size - 1) {
                output.append("\n\n")
            }
        }
        return output.toString()
    }

    /**
     * Generates TXT content for the startlist, grouped by categories.
     */
    private fun generateTxtStartListData(
        categoryData: List<CategoryData>,
        context: Context
    ): String {
        val output = StringBuilder()
        for (catWrapper in categoryData.withIndex()) {
            val cd = catWrapper.value
            val sortedComp = cd.competitors.sortedBy { it.drawnRelativeStartTime }

            output.append(cd.category.name).append("\n")
            output.append("-".repeat(FileConstants.LINE_LENGTH)).append("\n")

            for (comp in sortedComp) {
                output.append(generateStartlistRow(comp, DataFormat.TXT, context)).append("\n")
            }

            if (catWrapper.index < categoryData.size - 1) {
                output.append("\n\n")
            }
        }
        return output.toString()
    }

    /**
     * Generates one line of competitor result for TXT format.
     */
    private fun generateTxtResCompData(
        dataProcessor: DataProcessor,
        context: Context,
        competitorData: CompetitorData,
        generateSplits: Boolean
    ): String {
        val templateName = if (generateSplits) FileConstants.TEMPLATE_TEXT_RES_COMPETITOR_SPLITS
        else FileConstants.TEMPLATE_TEXT_RES_COMPETITOR

        val template = TemplateProcessor.loadTemplate(templateName, context)
        val params = getCompetitorParams(competitorData, dataProcessor)

        // Add special fields for TXT results
        params[FileConstants.KEY_COMP_CONTROLS] = ControlPointsHelper.getStringFromAliasPunches(
            competitorData.readoutData!!.punches,
            context
        )
        params[FileConstants.KEY_COMP_SPLITS] =
            getSplitsString(competitorData.readoutData!!.punches, dataProcessor)

        return TemplateProcessor.processTemplate(template, params)
    }

    // --------------------------------------------------------------------------------------------
    // HTML GENERATION LOGIC
    // --------------------------------------------------------------------------------------------

    /**
     * Generates HTML content for results.
     */
    private suspend fun generateHtmlResults(
        dataProcessor: DataProcessor,
        context: Context,
        results: List<ResultWrapper>,
        race: Race
    ): String {
        val output = StringBuilder()
        val filteredResults = results.filter { rw -> rw.competitorData.any { it.readoutData != null } }

        for (resultWrapper in filteredResults.withIndex()) {
            val result = resultWrapper.value
            val category = result.category ?: continue

            output.append(
                generateResCategoryHeader(
                    FileConstants.TEMPLATE_HTML_RES_CATEGORY,
                    dataProcessor,
                    context,
                    category,
                    race
                )
            )

            for (rd in result.competitorData) {
                if (rd.readoutData != null) {
                    output.append(generateHtmlCompetitorResults(dataProcessor, context, rd))
                }
            }
            output.append(FileConstants.HTML_TABLE_END)

            if (resultWrapper.index < filteredResults.size - 1) {
                output.append(FileConstants.HTML_DOUBLE_BREAK)
            }
        }
        return output.toString()
    }

    /**
     * Generates HTML content for startlist.
     */
    private suspend fun generateHtmlStartListData(
        context: Context,
        categoryData: List<CategoryData>
    ): String {
        val output = StringBuilder()
        for (cdWrapper in categoryData.withIndex()) {
            val cd = cdWrapper.value
            output.append(generateHtmlStartListCatHeader(context, cd.category))

            val sortedComp = cd.competitors.sortedBy { it.drawnRelativeStartTime }
            for (comp in sortedComp) {
                output.append(generateStartlistRow(comp, DataFormat.HTML, context))
            }

            output.append(FileConstants.HTML_TABLE_END)

            if (cdWrapper.index < categoryData.size - 1) {
                output.append(FileConstants.HTML_DOUBLE_BREAK)
            }
        }
        return output.toString()
    }

    /**
     * Generates one HTML row for competitor results.
     */
    private fun generateHtmlCompetitorResults(
        dataProcessor: DataProcessor,
        context: Context,
        competitorData: CompetitorData
    ): String {
        val template =
            TemplateProcessor.loadTemplate(FileConstants.TEMPLATE_HTML_RES_COMPETITOR, context)
        val params = getCompetitorParams(competitorData, dataProcessor)

        val resultStatus = competitorData.readoutData?.result?.resultStatus

        // generate splits only if finish reached
        params[FileConstants.KEY_COMP_SPLITS] =
            if (resultStatus != ResultStatus.DID_NOT_START && resultStatus != ResultStatus.DID_NOT_FINISH) {
                generateHtmlCompetitorSplits(
                    competitorData.readoutData!!.punches,
                    context,
                    dataProcessor
                )
            } else ""

        return TemplateProcessor.processTemplate(template, params)
    }

    /**
     * Generates HTML formatted splits.
     */
    private fun generateHtmlCompetitorSplits(
        splits: List<AliasPunch>,
        context: Context,
        dataProcessor: DataProcessor
    ): String {
        val output = StringBuilder()
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val useAlias =
            sharedPref.getBoolean(context.getString(R.string.key_results_use_aliases), true)
        val useMinuteFormat = dataProcessor.useMinuteTimeFormat()

        for (split in splits) {
            val punch = split.punch
            if (punch.punchType == SIRecordType.START) continue

            val aliasCode = when {
                punch.punchType == SIRecordType.FINISH -> "F"
                useAlias && split.alias != null -> split.alias?.name ?: punch.siCode.toString()
                else -> punch.siCode.toString()
            }

            output.append(
                TemplateProcessor.processTemplate(
                    FileConstants.HTML_SPLITS_CODE,
                    mapOf(
                        FileConstants.KEY_COMP_SPLIT_CODE to aliasCode,
                        FileConstants.KEY_COMP_SPLIT_TIME to TimeProcessor.durationToFormattedString(
                            punch.split, useMinuteFormat
                        )
                    )
                )
            )
        }
        return output.toString()
    }

    // --------------------------------------------------------------------------------------------
    // SHARED GENERATION HELPERS
    // --------------------------------------------------------------------------------------------

    /**
     * Generates a category header (works for both TXT and HTML depending on template).
     */
    private suspend fun generateResCategoryHeader(
        templateName: String,
        dataProcessor: DataProcessor,
        context: Context,
        category: Category,
        race: Race
    ): String {
        val template = TemplateProcessor.loadTemplate(templateName, context)
        val aliases = dataProcessor.getControlPointAliasesByCategory(category.id)

        val params = getBaseParams(dataProcessor, context, race)
        params.putAll(getCategoryParams(dataProcessor, category, race, aliases, context))

        return TemplateProcessor.processTemplate(template, params)
    }

    /**
     * Generates a header for HTML startlist category table.
     */
    private fun generateHtmlStartListCatHeader(
        context: Context,
        category: Category
    ): String {
        val template =
            TemplateProcessor.loadTemplate(FileConstants.TEMPLATE_HTML_STARTLIST_CATEGORY, context)
        val params = getCategoryParams(null, category, null, null, null)

        // Add headers required by the template
        params[FileConstants.KEY_TITLE_CATEGORY] = context.getString(R.string.general_category)
        params[FileConstants.KEY_TITLE_NAME] = context.getString(R.string.general_name)
        params[FileConstants.KEY_TITLE_CLUB] = context.getString(R.string.general_club)
        params[FileConstants.KEY_TITLE_POINTS] = context.getString(R.string.general_points)
        params[FileConstants.KEY_TITLE_RUN_TIME] = context.getString(R.string.general_run_time)
        params[FileConstants.KEY_TITLE_SI_NUMBER] = context.getString(R.string.general_si_number)
        params[FileConstants.KEY_TITLE_SPLITS] = context.getString(R.string.general_splits)

        return TemplateProcessor.processTemplate(template, params)
    }

    /**
     * Generates one row for the startlist (either TXT or HTML).
     */
    private fun generateStartlistRow(
        competitor: Competitor,
        dataFormat: DataFormat,
        context: Context
    ): String {
        val templateName =
            if (dataFormat == DataFormat.TXT) FileConstants.TEMPLATE_TEXT_STARTLIST_ROW
            else FileConstants.TEMPLATE_HTML_STARTLIST_ROW
        val template = TemplateProcessor.loadTemplate(templateName, context)

        val params = getCompetitorParams(null, null, competitor)
        return TemplateProcessor.processTemplate(template, params)
    }

    /**
     * Formats splits into a space-separated string for TXT files.
     */
    private fun getSplitsString(punches: List<AliasPunch>, dataProcessor: DataProcessor): String {
        val output = StringBuilder()
        val nonStartPunches = punches.filter { it.punch.punchType != SIRecordType.START }

        for (aliasPunch in nonStartPunches.withIndex()) {
            output.append(
                TimeProcessor.durationToFormattedString(
                    aliasPunch.value.punch.split,
                    dataProcessor.useMinuteTimeFormat()
                )
            )
            if (aliasPunch.index < nonStartPunches.size - 1) output.append(" ")
        }
        return output.toString()
    }

    /**
     * Processes a template and writes it to the output stream.
     */
    private fun writeTemplateToStream(
        templatePath: String,
        params: Map<String, String>,
        context: Context,
        outStream: OutputStream
    ) {
        val template = TemplateProcessor.loadTemplate(templatePath, context)
        val out = TemplateProcessor.processTemplate(template, params)
        outStream.write(out.toByteArray())
        outStream.flush()
    }

    // --------------------------------------------------------------------------------------------
    // PARAMETER EXTRACTION HELPERS (Centralized mapping)
    // --------------------------------------------------------------------------------------------

    /**
     * Initializes a map with basic race info, global UI titles, and metadata.
     */
    private fun getBaseParams(
        dataProcessor: DataProcessor,
        context: Context,
        race: Race
    ): HashMap<String, String> {
        val params = HashMap<String, String>()

        // Race Information
        params[FileConstants.KEY_RACE_NAME] = race.name
        params[FileConstants.KEY_RACE_DATE] =
            TimeProcessor.formatLocalDate(race.startDateTime.toLocalDate())
        params[FileConstants.KEY_RACE_START_TIME] =
            TimeProcessor.formatLocalTime(race.startDateTime.toLocalTime())
        params[FileConstants.KEY_RACE_LEVEL] = dataProcessor.raceLevelToString(race.raceLevel)

        // General UI Titles
        params[FileConstants.KEY_TITLE_RESULTS] = context.getString(R.string.general_results)
        params[FileConstants.KEY_TITLE_STARTLIST] = context.getString(R.string.general_startlist)
        params[FileConstants.KEY_TITLE_RACE_NAME] = context.getString(R.string.general_race)
        params[FileConstants.KEY_TITLE_RACE_DATE] = context.getString(R.string.general_date)
        params[FileConstants.KEY_TITLE_START_TIME] = context.getString(R.string.general_start_time)
        params[FileConstants.KEY_TITLE_RACE_LEVEL] = context.getString(R.string.race_level)
        params[FileConstants.KEY_TITLE_CATEGORY] = context.getString(R.string.general_category)
        params[FileConstants.KEY_TITLE_LIMIT] = context.getString(R.string.general_limit)
        params[FileConstants.KEY_TITLE_BAND] = context.getString(R.string.general_band)
        params[FileConstants.KEY_TITLE_LENGTH] = context.getString(R.string.general_length)
        params[FileConstants.KEY_TITLE_CONTROLS] = context.getString(R.string.general_controls)
        params[FileConstants.KEY_TITLE_PLACE] = context.getString(R.string.general_place)
        params[FileConstants.KEY_TITLE_NAME] = context.getString(R.string.general_name)
        params[FileConstants.KEY_TITLE_CLUB] = context.getString(R.string.general_club)
        params[FileConstants.KEY_TITLE_INDEX] = context.getString(R.string.general_index)
        params[FileConstants.KEY_TITLE_POINTS] = context.getString(R.string.general_points)
        params[FileConstants.KEY_TITLE_RUN_TIME] = context.getString(R.string.general_run_time)
        params[FileConstants.KEY_TITLE_SI_NUMBER] = context.getString(R.string.general_si_number)
        params[FileConstants.KEY_TITLE_START_NUMBER] =
            context.getString(R.string.competitor_start_number)
        params[FileConstants.KEY_TITLE_SPLITS] = context.getString(R.string.general_splits)
        params[FileConstants.KEY_TITLE_RESULTS_SPLITS] = context.getString(R.string.results_splits)

        // Generation Metadata
        val now = LocalDateTime.now()
        params[FileConstants.KEY_GENERATED_WITH] = context.getString(
            R.string.generated_with,
            TimeProcessor.formatDisplayLocalDateTime(now)
        )
        params[FileConstants.KEY_VERSION] = dataProcessor.getAppVersion()
        params[FileConstants.KEY_CURR_TIME] = TimeProcessor.formatDisplayLocalDateTime(now)

        // Support for TAB characters in TXT templates
        params[FileConstants.KEY_TAB] = "\t"

        return params
    }

    /**
     * Extracts parameters specific to a category.
     */
    private fun getCategoryParams(
        dataProcessor: DataProcessor?,
        category: Category,
        race: Race?,
        aliases: List<ControlPointAlias>?,
        context: Context?
    ): HashMap<String, String> {
        val params = HashMap<String, String>()
        params[FileConstants.KEY_CAT_NAME] = category.name
        params[FileConstants.KEY_CAT_LENGTH] = category.length.toString()

        race?.let {
            params[FileConstants.KEY_CAT_LIMIT] =
                (category.timeLimit ?: it.timeLimit).toMinutes().toString()
            dataProcessor?.let { dp ->
                params[FileConstants.KEY_CAT_BAND] =
                    dp.raceBandToString(category.categoryBand ?: it.raceBand)
            }
        }

        if (aliases != null && context != null) {
            params[FileConstants.KEY_CAT_CONTROLS] =
                ControlPointsHelper.getStringFromControlPointAliases(aliases, context)
        }
        return params
    }

    /**
     * Extracts parameters for a competitor. Handles both startlist and result scenarios.
     */
    private fun getCompetitorParams(
        competitorData: CompetitorData? = null,
        dataProcessor: DataProcessor? = null,
        directCompetitor: Competitor? = null
    ): HashMap<String, String> {
        val params = HashMap<String, String>()
        val competitor =
            directCompetitor ?: competitorData?.competitorCategory?.competitor ?: return params

        // Competitor Info
        params[FileConstants.KEY_COMP_NAME] = competitor.getFullName()
        params[FileConstants.KEY_COMP_CLUB] = competitor.club
        params[FileConstants.KEY_COMP_INDEX] = competitor.index
        params[FileConstants.KEY_COMP_SI_NUMBER] = competitor.siNumber?.toString().orEmpty()
        params[FileConstants.KEY_COMP_START_NUMBER] = competitor.startNumber.toString()
        params[FileConstants.KEY_COMP_START_TIME] = competitor.drawnRelativeStartTime?.let {
            TimeProcessor.durationToFormattedString(it, true)
        } ?: "-"

        // Result Info (if available)
        competitorData?.readoutData?.result?.let { res ->
            params[FileConstants.KEY_COMP_PLACE] =
                if (res.resultStatus == ResultStatus.OK) "${res.place}."
                else dataProcessor?.resultStatusToShortString(res.resultStatus).orEmpty()

            // Display result status only if competitor has finished "normally"
            params[FileConstants.KEY_COMP_RUN_TIME] =
                if (ResultStatus.displayRunTime(res.resultStatus)) {
                    TimeProcessor.durationToFormattedString(
                        res.runTime,
                        dataProcessor?.useMinuteTimeFormat() ?: false
                    )
                } else "-"
            params[FileConstants.KEY_COMP_POINTS] =
                if (ResultStatus.displayRunTime(res.resultStatus)) {
                    res.points.toString()
                } else "-"
        }

        return params
    }
}
