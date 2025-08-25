package kolskypavel.ardfmanager.backend.results

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager
import kolskypavel.ardfmanager.R
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import kolskypavel.ardfmanager.backend.room.entity.Category
import kolskypavel.ardfmanager.backend.room.entity.Competitor
import kolskypavel.ardfmanager.backend.room.entity.ControlPoint
import kolskypavel.ardfmanager.backend.room.entity.Punch
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.Result
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.room.enums.ControlPointType
import kolskypavel.ardfmanager.backend.room.enums.PunchStatus
import kolskypavel.ardfmanager.backend.room.enums.RaceType
import kolskypavel.ardfmanager.backend.room.enums.ResultStatus
import kolskypavel.ardfmanager.backend.room.enums.SIRecordType
import kolskypavel.ardfmanager.backend.sportident.SIConstants
import kolskypavel.ardfmanager.backend.sportident.SIPort.CardData
import kolskypavel.ardfmanager.backend.sportident.SITime
import kolskypavel.ardfmanager.backend.wrappers.ResultWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.TreeSet
import java.util.UUID


object ResultsProcessor {
    private fun adjustTime(previous: SITime, current: SITime): SITime {
        if (current.isAtOrAfter(previous)) {
            return current
        }

        val cmp = SITime(current)
        cmp.addHalfDay()

        if (cmp.isAtOrAfter(previous)) {
            return cmp
        }

        current.addDay()
        return current
    }

    /**
     * Adjust the times for the SI_CARD5, because it operates on 12h mode instead of 24h
     */
    fun card5TimeAdjust(result: Result, punches: List<Punch>, zeroTimeBase: LocalTime) {
        //Solve start and check
        if (result.startTime != null) {
            result.startTime = adjustTime(SITime(zeroTimeBase), result.origStartTime!!)
        }

        //Adjust the punches
        for (punch in punches.withIndex()) {

            val previousTime = if (punch.index == 0) {
                if (result.startTime != null) {
                    result.startTime!!
                } else {
                    SITime(zeroTimeBase)
                }
            } else {
                punches[punch.index - 1].siTime
            }

            val currentTime = punch.value.siTime
            currentTime.setDayOfWeek(previousTime.getDayOfWeek())
            currentTime.setWeek(previousTime.getWeek())
            punches[punch.index].siTime = adjustTime(previousTime, currentTime)
        }

        if (result.finishTime != null) {

            val prefinishTime = if (punches.isEmpty()) {
                if (result.startTime != null) {
                    result.startTime!!
                } else {
                    SITime(zeroTimeBase)
                }
            } else {
                punches.last().siTime
            }

            val finishTime = result.finishTime!!
            finishTime.setDayOfWeek(prefinishTime.getDayOfWeek())
            finishTime.setWeek(prefinishTime.getWeek())

            result.finishTime = adjustTime(prefinishTime, finishTime)
        }
    }

    private fun removeStartAndFinishPunch(result: Result, punches: ArrayList<Punch>) {
        if (punches.first().punchType == SIRecordType.START) {
            result.startTime = punches.first().siTime
            punches.removeAt(0)
        }
        if (punches.last().punchType == SIRecordType.FINISH) {
            result.finishTime = punches.last().siTime
            punches.removeAt(punches.lastIndex)
        }
    }

    /**
     * Processes the punches - converts PunchData to Punch entity
     */
    fun processCardPunches(
        cardData: CardData,
        raceId: UUID,
        result: Result,
        zeroTimeBase: LocalTime
    ): ArrayList<Punch> {
        val punches = ArrayList<Punch>()

        var orderCounter = 1
        cardData.punchData.forEach { punchData ->
            val punch = Punch(
                UUID.randomUUID(),
                raceId,
                result.id,
                cardData.siNumber,
                punchData.siCode,
                punchData.siTime,
                punchData.siTime,
                SIRecordType.CONTROL,
                orderCounter,
                PunchStatus.UNKNOWN,
                Duration.ZERO
            )
            punches.add(punch)
            orderCounter++
        }

        if (cardData.cardType == SIConstants.SI_CARD5) {
            card5TimeAdjust(result, punches, zeroTimeBase)
        }
        return punches
    }

    private fun calculateSplits(punches: ArrayList<Punch>) {
        //Calculate splits
        punches.forEachIndexed { index, punch ->
            if (index != 0) {
                punch.split = SITime.split(punches[index - 1].siTime, punch.siTime)
            }
        }
    }

    /**
     * Processes the given result - saves the data into the db
     */
    suspend fun processCardData(
        cardData: CardData,
        race: Race,
        context: Context,
        dataProcessor: DataProcessor
    ): Boolean {

        //Check if result already exists
        if (dataProcessor.getResultBySINumber(cardData.siNumber, race.id) == null) {
            val competitor = dataProcessor.getCompetitorBySINumber(cardData.siNumber, race.id)
            val category = competitor?.categoryId?.let { dataProcessor.getCategory(it) }

            //Create the result
            val result =
                Result(
                    UUID.randomUUID(),
                    race.id,
                    competitor?.id,
                    cardData.siNumber,
                    cardData.cardType,
                    cardData.checkTime,
                    cardData.checkTime,
                    cardData.startTime,
                    cardData.startTime,
                    cardData.finishTime,
                    cardData.finishTime,
                    LocalDateTime.now(),
                    false,
                    ResultStatus.NO_RANKING,
                    0,
                    Duration.ZERO,
                    false,
                    false
                )

            //TODO: Based on options, set start time to the predefined value
            if (competitor != null) {
                if (result.startTime == null && competitor.drawnRelativeStartTime != null) {
                    val startTime = TimeProcessor.getAbsoluteDateTimeFromRelativeTime(
                        dataProcessor.getCurrentRace().startDateTime,
                        competitor.drawnRelativeStartTime!!
                    )
                    result.startTime = SITime(
                        startTime.toLocalTime(), startTime.dayOfWeek.value - 1
                    )
                }
            }

            //Process the punches
            val punches = processCardPunches(
                cardData,
                race.id,
                result,
                race.startDateTime.toLocalTime()
            )

            calculateResult(
                result,
                category,
                punches,
                null,
                dataProcessor
            )

            // Add printing based on option
            if (isToPrintFinishTicket(competitor, category, context)) {
                CoroutineScope(Dispatchers.IO).launch {
                    dataProcessor.printFinishTicket(
                        dataProcessor.getResultData(result.id),
                        dataProcessor.getRace(result.raceId)
                    )
                }
            }
            return true
        }
        //Duplicate result
        else {

            //Run on the main UI thread
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(
                    context,
                    context.getText(R.string.readout_si_exists),
                    Toast.LENGTH_LONG
                )
                    .show()
            }
            return false
        }
    }

    // Returns if the ticket should be printed based on the current settings
    private fun isToPrintFinishTicket(
        competitor: Competitor?,
        category: Category?,
        context: Context,
    ): Boolean {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val preference =
            sharedPref.getString(
                context.getString(R.string.key_prints_automatic_printout),
                context.getString(R.string.print_automatic_manually)
            )

        when (preference) {
            context.getString(R.string.print_automatic_manually_entry) -> return true
            context.getString(R.string.print_automatic_competitor_matched_entry) -> {
                return competitor != null
            }

            context.getString(R.string.print_automatic_category_matched_entry) -> {
                return competitor != null && category != null
            }

            else -> {}
        }
        return false
    }

    suspend fun processManualPunchData(
        result: Result,
        punches: ArrayList<Punch>,
        manualStatus: ResultStatus?,
        dataProcessor: DataProcessor,
        modified: Boolean
    ) {
        var competitor: Competitor? = null

        if (result.competitorID != null) {
            competitor = dataProcessor.getCompetitor(result.competitorID!!)
        } else if (result.siNumber != null) {
            competitor = dataProcessor.getCompetitorBySINumber(result.siNumber!!, result.raceId)

            if (competitor != null) {
                result.competitorID = competitor.id
            }
        }

        val category = if (competitor?.categoryId != null) {
            dataProcessor.getCategory(competitor.categoryId!!)
        } else {
            null
        }

        //  Mark the result punches were modified and need to be sent again
        if (modified) {
            result.modified = true
        }
        result.sent = false

        punches.forEachIndexed { order, punch ->
            punch.resultId = result.id
            punch.order = order
        }

        //Modify the start and finish times
        removeStartAndFinishPunch(result, punches)

        calculateResult(
            result,
            category,
            punches,
            manualStatus,
            dataProcessor
        )
    }

    private suspend fun calculateResult(
        result: Result,
        category: Category?,
        punches: ArrayList<Punch>,
        manualStatus: ResultStatus?,
        dataProcessor: DataProcessor
    ) {

        if (category != null) {
            evaluatePunches(punches, category, result, dataProcessor)
        }

        val originalStartTime = result.startTime
        val originalFinishTime = result.finishTime

        if (result.cardType == SIConstants.SI_CARD5) {
            val zeroTimeBase = dataProcessor.getCurrentRace().startDateTime.toLocalTime()
            card5TimeAdjust(result, punches, zeroTimeBase)
        }

        // Add back start and finish - TODO: finish double start and finish punch bug
        if (result.startTime != null) {

            punches.add(
                0,
                Punch(
                    UUID.randomUUID(),
                    result.raceId,
                    result.id,
                    result.siNumber,
                    0,
                    result.startTime!!,
                    originalStartTime!!,
                    SIRecordType.START,
                    0,
                    PunchStatus.VALID,
                    Duration.ZERO
                )
            )
        }

        //Add finish punch
        if (result.finishTime != null) {
            punches.add(
                Punch(
                    UUID.randomUUID(),
                    result.raceId,
                    result.id,
                    result.siNumber,
                    0,
                    result.finishTime!!,
                    originalFinishTime!!,
                    SIRecordType.FINISH,
                    punches.size,
                    PunchStatus.VALID,
                    Duration.ZERO
                )
            )
        }

        calculateSplits(punches)

        // Result time calculation
        if (result.startTime != null && result.finishTime != null) {
            result.runTime = SITime.split(result.startTime!!, result.finishTime!!)
        }
        // TODO: solve situation when start or finish is missing

        // Set the result status based on user preference
        if (manualStatus != null) {
            result.automaticStatus = false
            result.resultStatus = manualStatus
        } else {
            result.automaticStatus = true
        }
        dataProcessor.saveResultPunches(result, punches)
    }

    private suspend fun evaluatePunches(
        punches: ArrayList<Punch>,
        category: Category, result: Result,
        dataProcessor: DataProcessor
    ) {

        var controlPoints: List<ControlPoint> = ArrayList()
        try {
            controlPoints = dataProcessor.getControlPointsByCategory(category.id)
        } catch (e: Exception) {
            Log.d("ResultsProcess", e.message.toString())
        }
        result.points = 0

        val raceType = if (category.differentProperties) {
            category.raceType!!
        } else {
            dataProcessor.getCurrentRace().raceType
        }
        when (raceType) {
            RaceType.CLASSIC, RaceType.FOXORING -> evaluateClassics(
                punches,
                controlPoints,
                result
            )

            RaceType.SPRINT -> evaluateSprint(
                punches,
                controlPoints,
                result
            )

            RaceType.ORIENTEERING -> evaluateOrienteering(
                punches,
                controlPoints,
                result
            )

        }
    }

    /**
     * Updates the already read out data in case of a change in category / competitor
     */
    suspend fun updateResultsForCategory(
        categoryId: UUID,
        delete: Boolean,
        dataProcessor: DataProcessor
    ) {
        val competitors = dataProcessor.getCompetitorsByCategory(categoryId)

        competitors.forEach { competitor ->

            val result = dataProcessor.getResultByCompetitor(competitor.id)
            result?.let {
                val punches = ArrayList(dataProcessor.getPunchesByResult(result.id))

                if (!delete) {
                    val category = dataProcessor.getCategory(categoryId)
                    evaluatePunches(punches, category!!, result, dataProcessor)
                } else {
                    clearEvaluation(punches, result)
                }
                dataProcessor.saveResultPunches(result, punches)
            }
        }
    }

    suspend fun updateResultsForCompetitor(competitorId: UUID, dataProcessor: DataProcessor) {
        var result = dataProcessor.getResultByCompetitor(competitorId)
        val competitor = dataProcessor.getCompetitor(competitorId)

        //Try to get result by SI instead and update competitor ID
        if (result == null && competitor?.siNumber != null) {
            val siResult = dataProcessor.getResultBySINumber(
                competitor.siNumber!!, competitor.raceId
            )
            if (siResult != null) {
                result = siResult
                result.competitorID = competitorId
            }
        }

        //If result is found, recalculate it
        if (result != null) {
            val punches = ArrayList(dataProcessor.getPunchesByResult(result.id))
            val category = competitor?.categoryId?.let { dataProcessor.getCategory(it) }

            if (category == null) {
                clearEvaluation(punches, result)
            }

            removeStartAndFinishPunch(
                result,
                punches
            )  // Remove start and finish punches before calculation
            calculateResult(result, category, punches, null, dataProcessor)
        }
    }

    suspend fun getCompetitorPlace(
        competitorId: UUID,
        raceId: UUID,
        dataProcessor: DataProcessor
    ): Int? {
        val results = dataProcessor.getCompetitorDataFlowByRace(raceId)
        val sorted = results.first().groupByCategoryAndSortByPlace()

        // Find the competitor in the sorted results
        return sorted.values.flatten()
            .find { it.competitorCategory.competitor.id == competitorId }?.readoutData?.result?.place

    }

    fun List<CompetitorData>.sortByPlace(): List<CompetitorData> {
        val sorted = this.sortedWith(ResultDataComparator())

        var place = 0
        for (cd in sorted.withIndex()) {
            val curr = cd.value.readoutData

            //Check for first element
            if (cd.index != 0) {
                val prev = sorted[cd.index - 1].readoutData

                if (curr != null && prev != null
                    && curr.result.runTime == prev.result.runTime
                    && curr.result.points == prev.result.points
                ) {
                    curr.result.place = place
                } else if (curr != null) {
                    place++
                    curr.result.place = place
                }
            } else if (curr != null) {
                place++
                curr.result.place = place
            }
        }
        return sorted
    }


    private fun List<CompetitorData>.toResultDisplayWrappers(): List<ResultWrapper> {
        // Transform each ReadoutData item into a ResultDisplayWrapper
        val res = this.groupByCategoryAndSortByPlace()

        return res.map { result ->
            ResultWrapper(
                category = result.key,
                subList = result.value.toMutableList()
            )
        }
    }

    fun List<CompetitorData>.groupByCategoryAndSortByPlace(): Map<Category?, List<CompetitorData>> {
        val grouped = this.groupBy { it.competitorCategory.category }.toMutableMap()
        grouped.forEach { cg ->
            grouped[cg.key] = cg.value.sortByPlace()
        }
        return grouped
    }

    fun getResultWrapperFlowByRace(
        raceId: UUID,
        dataProcessor: DataProcessor
    ): Flow<List<ResultWrapper>> {
        return dataProcessor.getCompetitorDataFlowByRace(raceId).map { resultDataList ->
            resultDataList.toResultDisplayWrappers()
        }
    }

    suspend fun getCompetitorDataByRace(
        raceId: UUID,
        dataProcessor: DataProcessor
    ): List<CompetitorData> {
        val grouped = dataProcessor.getCompetitorDataFlowByRace(raceId).first()
            .groupByCategoryAndSortByPlace()
        return grouped.values.flatten().toList()
    }

    /**
     * Resets all the punches to unknown, e. g. when the category has been deleted
     */
    fun clearEvaluation(punches: ArrayList<Punch>, result: Result) {
        result.points = 0
        punches.forEach { punch ->
            punch.punchStatus = PunchStatus.UNKNOWN
        }
        result.resultStatus = ResultStatus.NO_RANKING
    }

    /**
     * Process one loop of classics race
     * @return Number of points
     */
    private fun evaluateLoop(
        punches: List<Punch>,
        controlPoints: List<ControlPoint>
    ): Int {
        val codes = controlPoints.map { p -> p.siCode }.toSet()
        val taken = TreeSet<Int>()  //Already taken CPs
        var points = 0

        val beacon: Int =
            if (controlPoints.isNotEmpty() && controlPoints.last().type == ControlPointType.BEACON) {
                controlPoints.last().siCode
            } else -1

        punches.forEach { punch ->
            if (punch.punchType == SIRecordType.CONTROL && codes.contains(punch.siCode)) {

                //Valid punch
                if (!taken.contains(punch.siCode) && punch.siCode != beacon) {
                    punch.punchStatus = PunchStatus.VALID
                    points++
                    taken.add(punch.siCode)
                }
                //Check if beacon is the last punch
                else if (punch.siCode == beacon) {
                    if (punches.indexOf(punch) == punches.lastIndex) {
                        points++
                        punch.punchStatus = PunchStatus.VALID
                    } else {
                        punch.punchStatus = PunchStatus.INVALID
                    }
                }
                //Duplicate punch
                else {
                    punch.punchStatus = PunchStatus.DUPLICATE
                }
            }
            //Unknown punch
            else {
                punch.punchStatus = PunchStatus.UNKNOWN
            }
        }
        return points
    }

    /**
     * Process the classics race
     */
    fun evaluateClassics(
        punches: ArrayList<Punch>,
        controlPoints: List<ControlPoint>,
        result: Result
    ) {
        result.points = evaluateLoop(punches, controlPoints)

        //Set the status accordingly
        if (result.points > 1) {
            result.resultStatus = ResultStatus.OK
        } else {
            result.resultStatus = ResultStatus.NO_RANKING
        }
    }

    /**
     * Process the sprint race
     */
    fun evaluateSprint(
        punches: ArrayList<Punch>,
        controlPoints: List<ControlPoint>,
        result: Result
    ) {
        //First is code, second is index
        val separators = ArrayList<Pair<Int, Int>>()
        var points = 0

        //Find separators in the control points
        for (cp in controlPoints.withIndex()) {
            if (cp.value.type == ControlPointType.SEPARATOR) {
                separators.add(Pair(cp.value.siCode, cp.index))
            }
        }

        if (separators.isNotEmpty()) {
            var prevPunchSep = 0
            var prevControlSep = 0
            var separIndex = 0

            //Find separators in punches and evaluate loops
            for (pun in punches.withIndex()) {
                if ((separIndex < separators.size &&
                            pun.value.siCode == separators[separIndex].first)
                ) {
                    points += evaluateLoop(
                        ArrayList(punches.subList(prevPunchSep, pun.index)),
                        controlPoints.subList(
                            prevControlSep,
                            separators[separIndex].second
                        )
                    )
                    prevPunchSep = pun.index
                    prevControlSep = separators[separIndex].second
                    separIndex++
                }
            }
            //Get last loop
            points += evaluateLoop(
                punches.subList(prevPunchSep, punches.size),
                controlPoints.subList(
                    prevControlSep,
                    controlPoints.size
                )
            )
        }

        //No separator taken
        else {
            points = evaluateLoop(
                punches,
                controlPoints
            )    //TODO: Fix the last beacon to not be required
        }

        //Set the status accordingly
        result.points = points
        if (result.points > 1) {
            result.resultStatus = ResultStatus.OK
        } else {
            result.resultStatus = ResultStatus.NO_RANKING
        }
    }

    /**
     * Process the orienteering race
     */
    fun evaluateOrienteering(
        punches: ArrayList<Punch>,
        controlPoints: List<ControlPoint>,
        result: Result
    ) {
        var cpIndex = 0

        //TODO: Inform about missing punches
        for (punch in punches) {
            //Check bounds
            if (cpIndex >= controlPoints.size) {
                break
            }

            if (punch.siCode == controlPoints[cpIndex].siCode) {
                cpIndex++
                punch.punchStatus = PunchStatus.VALID
                result.points++
            }
            //Break in a loop
            else {
                punch.punchStatus = PunchStatus.INVALID
            }
        }

        if (result.points == controlPoints.size) {
            result.resultStatus = ResultStatus.OK
        } else {
            result.resultStatus = ResultStatus.DISQUALIFIED
        }
    }
}

