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
import kolskypavel.ardfmanager.backend.room.entity.ControlPoint
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.AliasPunch
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.room.enums.ResultStatus
import kolskypavel.ardfmanager.backend.room.enums.SIRecordType
import kolskypavel.ardfmanager.backend.wrappers.ResultWrapper
import kotlinx.coroutines.flow.first
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.util.UUID

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
        raceId: UUID
    ): Boolean {
        when (dataType) {
            DataType.RESULTS -> exportResults(format, outStream, raceId, dataProcessor)
            else -> {
                TODO()
            }
        }
        return true
    }

    @Throws(IllegalArgumentException::class)
    private suspend fun exportResults(
        format: DataFormat,
        outStream: OutputStream,
        raceId: UUID,
        dataProcessor: DataProcessor
    ) {
        val results = ResultsProcessor.getResultWrapperFlowByRace(raceId, dataProcessor).first()
        val params = HashMap<String, String>()

        // Init all the parameters for the template
        initParams(
            dataProcessor,
            dataProcessor.getContext(),
            params,
            results,
            dataProcessor.getRace(raceId),
            format
        )

        var templateType =
            if (format == DataFormat.TXT) {
                FileConstants.TEMPLATE_TEXT_RESULTS
            } else {
                FileConstants.TEMPLATE_HTML_RESULTS
            }

        val template = TemplateProcessor.loadTemplate(
            templateType,
            dataProcessor.getContext()
        )
        val out = TemplateProcessor.processTemplate(
            template,
            params
        )

        outStream.write(out.toByteArray())
        outStream.flush()
    }

    private suspend fun initParams(
        dataProcessor: DataProcessor,
        context: Context,
        params: HashMap<String, String>,
        results: List<ResultWrapper>,
        race: Race,
        format: DataFormat
    ) {
        params[FileConstants.KEY_TITLE_RESULTS] = context.getString(R.string.general_results)

        params[FileConstants.KEY_TITLE_RACE_NAME] = context.getString(R.string.general_race)
        params[FileConstants.KEY_RACE_NAME] = race.name
        params[FileConstants.KEY_TITLE_RACE_DATE] = context.getString(R.string.general_date)
        params[FileConstants.KEY_RACE_DATE] =
            TimeProcessor.formatLocalDate(race.startDateTime.toLocalDate())
        params[FileConstants.KEY_TITLE_RACE_START_TIME] =
            context.getString(R.string.general_start_time)
        params[FileConstants.KEY_RACE_START_TIME] =
            TimeProcessor.formatLocalTime(race.startDateTime.toLocalTime())
        params[FileConstants.KEY_TITLE_RACE_LEVEL] = context.getString(R.string.race_level)
        params[FileConstants.KEY_RACE_LEVEL] = dataProcessor.raceLevelToString(race.raceLevel)
        params[FileConstants.KEY_TITLE_RACE_BAND] = context.getString(R.string.general_band)
        params[FileConstants.KEY_RACE_BAND] = dataProcessor.raceBandToString(race.raceBand)

        params[FileConstants.KEY_TITLE_PLACE] = context.getString(R.string.general_place)
        params[FileConstants.KEY_TITLE_NAME] = context.getString(R.string.general_name)
        params[FileConstants.KEY_TITLE_INDEX] = context.getString(R.string.general_index)
        params[FileConstants.KEY_TITLE_RUN_TIME] = context.getString(R.string.general_run_time)
        params[FileConstants.KEY_TITLE_POINTS] = context.getString(R.string.general_points)
        params[FileConstants.KEY_TITLE_CONTROLS] = context.getString(R.string.general_controls)
        params[FileConstants.KEY_TITLE_RESULTS_SPLITS] = context.getString(R.string.results_splits)

        // Init format specific parameters
        if (format == DataFormat.TXT) {
            params[FileConstants.KEY_RACE_RESULTS] =
                generateTxtResults(dataProcessor, context, results, race)
            params[FileConstants.KEY_RACE_RESULTS_SPLITS] =
                generateTxtResults(dataProcessor, context, results, race, true)
        } else {
            params[FileConstants.KEY_RACE_RESULTS] =
                generateHtmlResults(dataProcessor, context, results, race)
        }

        params[FileConstants.KEY_GENERATED_WITH] =
            context.getString(
                R.string.results_generated_with,
                TimeProcessor.formatLocalDateTime(LocalDateTime.now())
            )
        params[FileConstants.KEY_VERSION] = dataProcessor.getAppVersion()
    }


    //Generates one line of competitor data
    private fun generateTxtCompetitorData(
        dataProcessor: DataProcessor,
        context: Context,
        competitorData: CompetitorData,
        generateSplits: Boolean = false
    ): String {

        val templateName =
            if (generateSplits) FileConstants.TEMPLATE_TEXT_COMPETITOR_SPLITS
            else FileConstants.TEMPLATE_TEXT_COMPETITOR

        val template =
            TemplateProcessor.loadTemplate(templateName, context)
        val params = HashMap<String, String>()

        val result = competitorData.readoutData?.result!!

        params[FileConstants.KEY_COMP_PLACE] =
            if (result.resultStatus == ResultStatus.OK) {
                "${result.place}."
            } else {
                dataProcessor.resultStatusToShortString(result.resultStatus)
            }

        params[FileConstants.KEY_COMP_NAME] =
            competitorData.competitorCategory.competitor.getFullName()
        params[FileConstants.KEY_COMP_INDEX] =
            competitorData.competitorCategory.competitor.index
        params[FileConstants.KEY_COMP_RUN_TIME] =
            TimeProcessor.durationToMinuteString(
                result.runTime
            )
        params[FileConstants.KEY_COMP_POINTS] =
            result.points.toString()

        // TODO: remove for orienteering
        params[FileConstants.KEY_COMP_CONTROLS] = ControlPointsHelper.getStringFromAliasPunches(
            competitorData.readoutData!!.punches,
            context
        )

        params[FileConstants.KEY_COMP_SPLITS] =
            getSplitsString(competitorData.readoutData!!.punches)

        val out = TemplateProcessor.processTemplate(template, params)
        return out
    }

    private fun getSplitsString(
        punches: List<AliasPunch>
    ): String {
        var out = ""

        for (aliasPunch in punches.withIndex()) {
            out += TimeProcessor.durationToMinuteString(aliasPunch.value.punch.split)

            if (aliasPunch.index < punches.size - 1) {
                out += " "
            }
        }
        return out
    }

    private fun generateCategoryHeader(
        templateName: String,
        context: Context,
        category: Category,
        controlPoints: List<ControlPoint>,
        race: Race
    ): String {
        val template = TemplateProcessor.loadTemplate(templateName, context)
        val params = HashMap<String, String>()

        params[FileConstants.KEY_TITLE_CATEGORY] = context.getString(R.string.category)
        params[FileConstants.KEY_CAT_NAME] = category.name
        params[FileConstants.KEY_TITLE_LIMIT] = context.getString(R.string.general_limit)
        params[FileConstants.KEY_CAT_LIMIT] = if (category.timeLimit != null) {
            TimeProcessor.durationToMinuteString(category.timeLimit!!)
        } else {
            TimeProcessor.durationToMinuteString(race.timeLimit)
        }

        params[FileConstants.KEY_TITLE_LENGTH] = context.getString(R.string.general_length)
        params[FileConstants.KEY_CAT_LENGTH] = category.length.toString()

        params[FileConstants.KEY_TITLE_CONTROLS] = context.getString(R.string.general_controls)
        params[FileConstants.KEY_CAT_CONTROLS] = controlPoints.size.toString()

        params[FileConstants.KEY_TITLE_PLACE] = context.getString(R.string.general_place)
        params[FileConstants.KEY_TITLE_NAME] = context.getString(R.string.general_name)
        params[FileConstants.KEY_TITLE_POINTS] = context.getString(R.string.general_points)
        params[FileConstants.KEY_TITLE_RUN_TIME] = context.getString(R.string.general_run_time)

        val gen = TemplateProcessor.processTemplate(template, params)
        return gen
    }

    // Generates the whole result block
    private suspend fun generateTxtResults(
        dataProcessor: DataProcessor,
        context: Context,
        results: List<ResultWrapper>,
        race: Race,
        generateSplits: Boolean = false
    ): String {
        var output = ""

        for (result in results) {
            if (result.category != null) {
                output += generateCategoryHeader(
                    FileConstants.TEMPLATE_TEXT_CATEGORY,
                    context,
                    result.category,
                    dataProcessor.getControlPointsByCategory(result.category.id),
                    race
                )
                output += "\n"

                for (rd in result.subList) {
                    if (rd.readoutData != null) {
                        val competitorData =
                            generateTxtCompetitorData(dataProcessor, context, rd, generateSplits)
                        output += competitorData + "\n"
                    }
                }
                output += "\n\n"
            }
        }

        return output
    }

    // HTML SECTION
    private suspend fun generateHtmlResults(
        dataProcessor: DataProcessor,
        context: Context,
        results: List<ResultWrapper>,
        race: Race
    ): String {
        var output = ""

        for (result in results.withIndex()) {

            output += FileConstants.HTML_TABLE_START

            if (result.value.category != null) {
                output += generateCategoryHeader(
                    FileConstants.TEMPLATE_HTML_CATEGORY,
                    context,
                    result.value.category!!,
                    dataProcessor.getControlPointsByCategory(result.value.category!!.id),
                    race
                )

                for (rd in result.value.subList.withIndex()) {
                    if (rd.value.readoutData != null) {
                        val competitorData =
                            generateHtmlCompetitorData(dataProcessor, context, rd.value)
                        output += competitorData

                        if (rd.index < result.value.subList.size - 1) {
                            output += FileConstants.HTML_EMPTY_ROW
                        }
                    }
                }
                output += FileConstants.HTML_TABLE_END

                // Add a line break between categories
                if (result.index < results.size - 1) {
                    output += FileConstants.HTML_DOUBLE_BREAK
                }
            }
        }
        return output
    }

    // Generates two rows of competitor splits
    private fun generateHtmlCompetitorSplits(
        splits: List<AliasPunch>,
        context: Context
    ): Pair<String, String> {
        var row1 = ""
        var row2 = ""

        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val useAlias =
            sharedPref.getBoolean(context.getString(R.string.key_results_use_aliases), true)

        // TODO: Limit row length
        for (split in splits) {
            if (split.punch.punchType == SIRecordType.CONTROL) {
                val aliasCode: String = if (useAlias && split.alias != null) {
                    split.alias!!.name
                } else {
                    split.punch.siCode.toString()
                }

                val splitCode = TemplateProcessor.processTemplate(
                    FileConstants.HTML_SPLITS_CODE,
                    mapOf(FileConstants.KEY_COMP_SPLIT_CODE to aliasCode)
                )
                val splitTime = TemplateProcessor.processTemplate(
                    FileConstants.HTML_SPLITS_TIME,
                    mapOf(
                        FileConstants.KEY_COMP_SPLIT_TIME to TimeProcessor.durationToMinuteString(
                            split.punch.split
                        )
                    )
                )

                row1 += splitCode
                row2 += splitTime
            }
        }

        return Pair(row1, row2)
    }

    private fun generateHtmlCompetitorData(
        dataProcessor: DataProcessor,
        context: Context,
        competitorData: CompetitorData
    ): String {

        val template =
            TemplateProcessor.loadTemplate(FileConstants.TEMPLATE_HTML_COMPETITOR, context)
        val params = HashMap<String, String>()

        val result = competitorData.readoutData?.result!!

        params[FileConstants.KEY_COMP_PLACE] =
            if (result.resultStatus == ResultStatus.OK) {
                "${result.place}."
            } else {
                dataProcessor.resultStatusToShortString(result.resultStatus)
            }

        params[FileConstants.KEY_COMP_NAME] =
            competitorData.competitorCategory.competitor.getFullName()
        params[FileConstants.KEY_COMP_INDEX] =
            competitorData.competitorCategory.competitor.index
        params[FileConstants.KEY_COMP_RUN_TIME] =
            TimeProcessor.durationToMinuteString(
                result.runTime
            )
        params[FileConstants.KEY_COMP_POINTS] =
            result.points.toString()

        val splits = generateHtmlCompetitorSplits(competitorData.readoutData!!.punches, context)

        params[FileConstants.KEY_COMP_SPLITS_CODES] = splits.first
        params[FileConstants.KEY_COMP_SPLITS_TIMES] = splits.second

        val out = TemplateProcessor.processTemplate(template, params)
        return out
    }
}