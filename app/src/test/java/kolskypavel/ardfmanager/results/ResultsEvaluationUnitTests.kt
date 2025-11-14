package kolskypavel.ardfmanager.results

import kolskypavel.ardfmanager.backend.results.ResultsProcessor
import kolskypavel.ardfmanager.backend.room.entity.ControlPoint
import kolskypavel.ardfmanager.backend.room.entity.Punch
import kolskypavel.ardfmanager.backend.room.entity.Result
import kolskypavel.ardfmanager.backend.room.enums.ControlPointType
import kolskypavel.ardfmanager.backend.room.enums.PunchStatus
import kolskypavel.ardfmanager.backend.room.enums.ResultStatus
import kolskypavel.ardfmanager.backend.room.enums.SIRecordType
import kolskypavel.ardfmanager.backend.sportident.SIConstants
import kolskypavel.ardfmanager.backend.sportident.SITime
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.util.Random
import java.util.UUID

/**
 * Tests the correct evaluation of the punches
 * TODO: Add more random data
 */
class ResultsEvaluationUnitTests {

    @Test
    fun testClassicsCorrectData() {
        val result = Result(
            UUID.randomUUID(),
            UUID.randomUUID(),
            siNumber = null,
            cardType = SIConstants.SI_CARD5,
            checkTime = SITime(),
            origCheckTime = SITime(),
            startTime = SITime(),
            origStartTime = SITime(),
            finishTime = SITime(),
            origFinishTime = SITime(),
            automaticStatus = true,
            resultStatus = ResultStatus.NO_RANKING,
            runTime = Duration.ZERO,
            modified = false,
            sent = false
        )
        val punches = ArrayList<Punch>()
        val controlPoints = ArrayList<ControlPoint>()

        for (i in 1..6) {
            punches.add(
                Punch(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    null,
                    null,
                    30 + i,
                    SITime(),
                    SITime(),
                    SIRecordType.CONTROL, i, PunchStatus.UNKNOWN, Duration.ZERO
                )
            )
            controlPoints.add(
                ControlPoint(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    30 + i,
                    ControlPointType.CONTROL,
                    i
                )
            )
        }
        controlPoints.last().type = ControlPointType.BEACON
        ResultsProcessor.evaluateClassics(punches, controlPoints, result)
        assertEquals(ResultStatus.OK, result.resultStatus)
        //Check the punches
        for (punch in punches) {
            assertEquals(PunchStatus.VALID, punch.punchStatus)
        }
        assertEquals(6, result.points)
    }

    @Test
    fun testClassicsRandomData() {
        for (t in 0..50) {
            val result = Result(
                UUID.randomUUID(),
                UUID.randomUUID(),
                siNumber = null,
                cardType = SIConstants.SI_CARD5,
                checkTime = SITime(),
                origCheckTime = SITime(),
                startTime = SITime(),
                origStartTime = SITime(),
                finishTime = SITime(),
                origFinishTime = SITime(),
                automaticStatus = true,
                resultStatus = ResultStatus.NO_RANKING,
                runTime = Duration.ZERO,
                modified = false,
                sent = false
            )
            val punches = ArrayList<Punch>()
            val controlPoints = ArrayList<ControlPoint>()

            val randLength = Random().nextInt(1000) + 1
            var randCode = 0

            for (i in 0..randLength) {

                randCode += Random().nextInt(10) + 1

                punches.add(
                    Punch(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        null,
                        randCode,
                        SITime(),
                        SITime(),
                        SIRecordType.CONTROL, i, PunchStatus.UNKNOWN, Duration.ZERO
                    )
                )
                controlPoints.add(
                    ControlPoint(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        randCode,
                        ControlPointType.CONTROL,
                        i
                    )
                )
            }

            controlPoints.last().type = ControlPointType.BEACON
            ResultsProcessor.evaluateClassics(punches, controlPoints, result)
            assertEquals(ResultStatus.OK, result.resultStatus)
            assertEquals(randLength + 1, result.points)
        }
    }

    @Test
    fun testClassicsDuplicateBeaconEvaluation() {
        val result = Result(
            UUID.randomUUID(),
            UUID.randomUUID(),
            siNumber = null,
            cardType = SIConstants.SI_CARD5,
            checkTime = SITime(),
            origCheckTime = SITime(),
            startTime = SITime(),
            origStartTime = SITime(),
            finishTime = SITime(),
            origFinishTime = SITime(),
            automaticStatus = true,
            resultStatus = ResultStatus.NO_RANKING,
            runTime = Duration.ZERO,
            modified = false,
            sent = false
        )
        val punches = ArrayList<Punch>()
        val controlPoints = ArrayList<ControlPoint>()

        for (i in 1..6) {
            punches.add(
                Punch(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    null,
                    null,
                    30 + i,
                    SITime(),
                    SITime(),
                    SIRecordType.CONTROL, i, PunchStatus.UNKNOWN, Duration.ZERO
                )
            )
            controlPoints.add(
                ControlPoint(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    30 + i,
                    ControlPointType.CONTROL,
                    i
                )
            )
        }
        controlPoints.last().type = ControlPointType.BEACON

        punches.add(
            Punch(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                36,
                SITime(),
                SITime(),
                SIRecordType.CONTROL, 19, PunchStatus.UNKNOWN, Duration.ZERO
            )
        )

        ResultsProcessor.evaluateClassics(punches, controlPoints, result)
        assertEquals(ResultStatus.OK, result.resultStatus)

        //Check the punches
        assertEquals(6, result.points)
        assertEquals(PunchStatus.INVALID, punches[punches.size - 2].punchStatus)
    }

    @Test
    fun testClassicsPunchesAfterBeaconEvaluation() {
        val result = Result(
            UUID.randomUUID(),
            UUID.randomUUID(),
            siNumber = null,
            cardType = SIConstants.SI_CARD5,
            checkTime = SITime(),
            origCheckTime = SITime(),
            startTime = SITime(),
            origStartTime = SITime(),
            finishTime = SITime(),
            origFinishTime = SITime(),
            automaticStatus = true,
            resultStatus = ResultStatus.NO_RANKING,
            runTime = Duration.ZERO,
            modified = false,
            sent = false
        )
        val punches = ArrayList<Punch>()
        val controlPoints = ArrayList<ControlPoint>()

        for (i in 1..6) {
            controlPoints.add(
                ControlPoint(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    30 + i,
                    ControlPointType.CONTROL,
                    i
                )
            )
        }

        controlPoints.last().type = ControlPointType.BEACON

        for (i in 3..6) {
            punches.add(
                Punch(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    null,
                    null,
                    30 + i,
                    SITime(),
                    SITime(),
                    SIRecordType.CONTROL, i, PunchStatus.UNKNOWN, Duration.ZERO
                )
            )
        }

        for (i in 1..2) {
            punches.add(
                Punch(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    null,
                    null,
                    30 + i,
                    SITime(),
                    SITime(),
                    SIRecordType.CONTROL, i, PunchStatus.UNKNOWN, Duration.ZERO
                )
            )
        }

        ResultsProcessor.evaluateClassics(punches, controlPoints, result)
        assertEquals(ResultStatus.OK, result.resultStatus)

        //Check the punches
        assertEquals(5, result.points)
    }

    @Test
    fun testClassicsRankingOrNoRanking() {
        val result = Result(
            UUID.randomUUID(),
            UUID.randomUUID(),
            siNumber = null,
            cardType = SIConstants.SI_CARD5,
            checkTime = SITime(),
            origCheckTime = SITime(),
            startTime = SITime(),
            origStartTime = SITime(),
            finishTime = SITime(),
            origFinishTime = SITime(),
            automaticStatus = true,
            resultStatus = ResultStatus.NO_RANKING,
            runTime = Duration.ZERO,
            modified = false,
            sent = false
        )
        val punches = ArrayList<Punch>()
        val controlPoints = ArrayList<ControlPoint>()
        ResultsProcessor.evaluateClassics(punches, controlPoints, result)
        assertEquals(ResultStatus.NO_RANKING, result.resultStatus)

        punches.add(
            Punch(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                31,
                SITime(),
                SITime(),
                SIRecordType.CONTROL, 0, PunchStatus.UNKNOWN, Duration.ZERO
            )
        )
        controlPoints.add(
            ControlPoint(
                UUID.randomUUID(),
                UUID.randomUUID(),
                31,
                ControlPointType.CONTROL,
                0
            )
        )
        ResultsProcessor.evaluateClassics(punches, controlPoints, result)
        assertEquals(ResultStatus.NO_RANKING, result.resultStatus)
        punches.add(
            Punch(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                32,
                SITime(),
                SITime(),
                SIRecordType.CONTROL, 1, PunchStatus.UNKNOWN, Duration.ZERO
            )
        )
        controlPoints.add(
            ControlPoint(
                UUID.randomUUID(),
                UUID.randomUUID(),
                32,
                ControlPointType.CONTROL,
                0
            )
        )
        ResultsProcessor.evaluateClassics(punches, controlPoints, result)
        assertEquals(ResultStatus.OK, result.resultStatus)
    }

    @Test
    fun testOrienteeringCorrectData() {
        val result = Result(
            UUID.randomUUID(),
            UUID.randomUUID(),
            siNumber = null,
            cardType = SIConstants.SI_CARD5,
            checkTime = SITime(),
            origCheckTime = SITime(),
            startTime = SITime(),
            origStartTime = SITime(),
            finishTime = SITime(),
            origFinishTime = SITime(),
            automaticStatus = true,
            resultStatus = ResultStatus.NO_RANKING,
            runTime = Duration.ZERO,
            modified = false,
            sent = false
        )
        val punches = ArrayList<Punch>()
        val controlPoints = ArrayList<ControlPoint>()

        for (i in 1..6) {
            punches.add(
                Punch(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    null,
                    null,
                    30 + i,
                    SITime(),
                    SITime(),
                    SIRecordType.CONTROL, i, PunchStatus.UNKNOWN, Duration.ZERO
                )
            )
            controlPoints.add(
                ControlPoint(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    30 + i,
                    ControlPointType.CONTROL,
                    i
                )
            )
        }
        ResultsProcessor.evaluateOrienteering(punches, controlPoints, result)
        assertEquals(ResultStatus.OK, result.resultStatus)
        assertEquals(6, result.points)
    }

    @Test
    fun testOrienteeringDataWithMistake() {
        val result = Result(
            UUID.randomUUID(),
            UUID.randomUUID(),
            siNumber = null,
            cardType = SIConstants.SI_CARD5,
            checkTime = SITime(),
            origCheckTime = SITime(),
            startTime = SITime(),
            origStartTime = SITime(),
            finishTime = SITime(),
            origFinishTime = SITime(),
            automaticStatus = true,
            resultStatus = ResultStatus.NO_RANKING,
            runTime = Duration.ZERO,
            modified = false,
            sent = false
        )
        val punches = ArrayList<Punch>()
        val controlPoints = ArrayList<ControlPoint>()

        for (i in 1..6) {
            punches.add(
                Punch(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    null,
                    null,
                    30 + i,
                    SITime(),
                    SITime(),
                    SIRecordType.CONTROL, i, PunchStatus.UNKNOWN, Duration.ZERO
                )
            )
            controlPoints.add(
                ControlPoint(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    30 + i,
                    ControlPointType.CONTROL,
                    i
                )
            )
        }
        punches.add(
            2, Punch(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                62,
                SITime(),
                SITime(),
                SIRecordType.CONTROL, 0, PunchStatus.UNKNOWN, Duration.ZERO
            )
        )
        punches.add(
            4, Punch(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                64,
                SITime(),
                SITime(),
                SIRecordType.CONTROL, 5, PunchStatus.UNKNOWN, Duration.ZERO

            )
        )
        ResultsProcessor.evaluateOrienteering(punches, controlPoints, result)
        assertEquals(ResultStatus.OK, result.resultStatus)
        assertEquals(6, result.points)
        assertEquals(PunchStatus.INVALID, punches[2].punchStatus)
        assertEquals(PunchStatus.INVALID, punches[4].punchStatus)
    }

    @Test
    fun testOrienteeringWithEmptyControls() {
        val result = Result(
            UUID.randomUUID(),
            UUID.randomUUID(),
            siNumber = null,
            cardType = SIConstants.SI_CARD5,
            checkTime = SITime(),
            origCheckTime = SITime(),
            startTime = SITime(),
            origStartTime = SITime(),
            finishTime = SITime(),
            origFinishTime = SITime(),
            automaticStatus = true,
            resultStatus = ResultStatus.NO_RANKING,
            runTime = Duration.ZERO,
            modified = false,
            sent = false
        )
        val punches = ArrayList<Punch>()
        val controlPoints = ArrayList<ControlPoint>()

        for (i in 1..6) {
            punches.add(
                Punch(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    null,
                    null,
                    30 + i,
                    SITime(),
                    SITime(),
                    SIRecordType.CONTROL, i, PunchStatus.UNKNOWN, Duration.ZERO
                )
            )
        }
        ResultsProcessor.evaluateOrienteering(punches, controlPoints, result)
        assertEquals(ResultStatus.OK, result.resultStatus)
    }

    @Test
    fun testOrienteeringIncorrectData() {
        val result = Result(
            UUID.randomUUID(),
            UUID.randomUUID(),
            siNumber = null,
            cardType = SIConstants.SI_CARD5,
            checkTime = SITime(),
            origCheckTime = SITime(),
            startTime = SITime(),
            origStartTime = SITime(),
            finishTime = SITime(),
            origFinishTime = SITime(),
            automaticStatus = true,
            resultStatus = ResultStatus.NO_RANKING,
            runTime = Duration.ZERO,
            modified = false,
            sent = false
        )
        val punches = ArrayList<Punch>()
        val controlPoints = ArrayList<ControlPoint>()

        for (i in 1..6) {
            punches.add(
                Punch(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    null,
                    null,
                    30 + i,
                    SITime(),
                    SITime(),
                    SIRecordType.CONTROL, i, PunchStatus.UNKNOWN, Duration.ZERO
                )
            )
            controlPoints.add(
                ControlPoint(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    30 + i,
                    ControlPointType.CONTROL,
                    i
                )
            )
        }
        punches[2].siCode = 44
        ResultsProcessor.evaluateOrienteering(punches, controlPoints, result)
        assertEquals(ResultStatus.MISPUNCHED, result.resultStatus)
    }

    @Test
    fun testSprintCorrectData() {
        val result = Result(
            UUID.randomUUID(),
            UUID.randomUUID(),
            siNumber = null,
            cardType = SIConstants.SI_CARD5,
            checkTime = SITime(),
            origCheckTime = SITime(),
            startTime = SITime(),
            origStartTime = SITime(),
            finishTime = SITime(),
            origFinishTime = SITime(),
            automaticStatus = true,
            resultStatus = ResultStatus.NO_RANKING,
            runTime = Duration.ZERO,
            modified = false,
            sent = false
        )
        val punches = ArrayList<Punch>()
        val controlPoints = ArrayList<ControlPoint>()
        ResultsProcessor.evaluateSprint(punches, controlPoints, result)
        assertEquals(ResultStatus.NO_RANKING, result.resultStatus)

        for (i in 1..12) {
            punches.add(
                Punch(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    null,
                    null,
                    30 + i,
                    SITime(),
                    SITime(),
                    SIRecordType.CONTROL, i, PunchStatus.UNKNOWN, Duration.ZERO
                )
            )
            controlPoints.add(
                ControlPoint(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    30 + i,
                    ControlPointType.CONTROL,
                    i
                )
            )
        }
        controlPoints[4].type = ControlPointType.SEPARATOR
        controlPoints[7].type = ControlPointType.SEPARATOR
        controlPoints.last().type = ControlPointType.BEACON

        ResultsProcessor.evaluateSprint(punches, controlPoints, result)
        assertEquals(ResultStatus.OK, result.resultStatus)
        assertEquals(12, result.points)
        for (punch in punches) {
            assertEquals(PunchStatus.VALID, punch.punchStatus)
        }

        //Add some random invalid data
        punches.add(
            5, Punch(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                99,
                SITime(),
                SITime(),
                SIRecordType.CONTROL, 15, PunchStatus.UNKNOWN, Duration.ZERO
            )
        )

        punches.add(
            9, Punch(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                67,
                SITime(),
                SITime(),
                SIRecordType.CONTROL, 15, PunchStatus.UNKNOWN, Duration.ZERO
            )
        )
        ResultsProcessor.evaluateSprint(punches, controlPoints, result)
        assertEquals(ResultStatus.OK, result.resultStatus)
        assertEquals(12, result.points)
        assertEquals(PunchStatus.UNKNOWN, punches[5].punchStatus)
        assertEquals(PunchStatus.UNKNOWN, punches[9].punchStatus)
    }

    @Test
    fun testSprintDatWithMistakes() {
        // Double punched separator
        val result = Result(
            UUID.randomUUID(),
            UUID.randomUUID(),
            siNumber = null,
            cardType = SIConstants.SI_CARD5,
            checkTime = SITime(),
            origCheckTime = SITime(),
            startTime = SITime(),
            origStartTime = SITime(),
            finishTime = SITime(),
            origFinishTime = SITime(),
            automaticStatus = true,
            resultStatus = ResultStatus.NO_RANKING,
            runTime = Duration.ZERO,
            modified = false,
            sent = false
        )
        val punches = ArrayList<Punch>()
        val controlPoints = ArrayList<ControlPoint>()
        ResultsProcessor.evaluateSprint(punches, controlPoints, result)
        assertEquals(ResultStatus.NO_RANKING, result.resultStatus)

        for (i in 1..12) {
            punches.add(
                Punch(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    null,
                    null,
                    30 + i,
                    SITime(),
                    SITime(),
                    SIRecordType.CONTROL, i, PunchStatus.UNKNOWN, Duration.ZERO
                )
            )
            controlPoints.add(
                ControlPoint(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    30 + i,
                    ControlPointType.CONTROL,
                    i
                )
            )
        }

        controlPoints[4].type = ControlPointType.SEPARATOR
        controlPoints[7].type = ControlPointType.SEPARATOR
        controlPoints.last().type = ControlPointType.BEACON

        punches.add(
            3, Punch(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                34,
                SITime(),
                SITime(),
                SIRecordType.CONTROL, 15, PunchStatus.UNKNOWN, Duration.ZERO
            )
        )

        punches.add(
            8, Punch(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                null,
                37,
                SITime(),
                SITime(),
                SIRecordType.CONTROL, 15, PunchStatus.UNKNOWN, Duration.ZERO
            )
        )
        for (pun in punches.withIndex()) {
            pun.value.order = pun.index + 1
        }

        ResultsProcessor.evaluateSprint(punches, controlPoints, result)
        assertEquals(ResultStatus.OK, result.resultStatus)
        assertEquals(12, result.points)
        assertEquals(PunchStatus.DUPLICATE, punches[4].punchStatus)
        assertEquals(PunchStatus.DUPLICATE, punches[8].punchStatus)
    }

    @Test
    fun testSprintAllSeparators() {
        val result = Result(
            UUID.randomUUID(),
            UUID.randomUUID(),
            siNumber = null,
            cardType = SIConstants.SI_CARD5,
            checkTime = SITime(),
            origCheckTime = SITime(),
            startTime = SITime(),
            origStartTime = SITime(),
            finishTime = SITime(),
            origFinishTime = SITime(),
            automaticStatus = true,
            resultStatus = ResultStatus.NO_RANKING,
            runTime = Duration.ZERO,
            modified = false,
            sent = false
        )
        val punches = ArrayList<Punch>()
        val controlPoints = ArrayList<ControlPoint>()

        for (i in 1..12) {
            punches.add(
                Punch(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    null,
                    null,
                    30 + i,
                    SITime(),
                    SITime(),
                    SIRecordType.CONTROL, i, PunchStatus.UNKNOWN, Duration.ZERO
                )
            )
            controlPoints.add(
                ControlPoint(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    30 + i,
                    ControlPointType.CONTROL,
                    i
                )
            )
        }

        ResultsProcessor.evaluateSprint(punches, controlPoints, result)
        assertEquals(ResultStatus.OK, result.resultStatus)
        assertEquals(12, result.points)
        for (punch in punches) {
            assertEquals(PunchStatus.VALID, punch.punchStatus)
        }
    }

    @Test
    fun testWithStartPunches() {
        for (t in 0..50) {
            val result = Result(
                UUID.randomUUID(),
                UUID.randomUUID(),
                siNumber = null,
                cardType = SIConstants.SI_CARD5,
                checkTime = SITime(),
                origCheckTime = SITime(),
                startTime = SITime(),
                origStartTime = SITime(),
                finishTime = SITime(),
                origFinishTime = SITime(),
                automaticStatus = true,
                resultStatus = ResultStatus.NO_RANKING,
                runTime = Duration.ZERO,
                modified = false,
                sent = false
            )
            val punches = ArrayList<Punch>()
            val controlPoints = ArrayList<ControlPoint>()

            val randLength = Random().nextInt(1000) + 1
            var randCode = 0

            for (i in 0..randLength) {

                randCode += Random().nextInt(10) + 1

                punches.add(
                    Punch(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        null,
                        null,
                        30 + i,
                        SITime(),
                        SITime(),
                        SIRecordType.CONTROL, i, PunchStatus.UNKNOWN, Duration.ZERO
                    )
                )
                controlPoints.add(
                    ControlPoint(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        randCode,
                        ControlPointType.CONTROL,
                        i
                    )
                )
            }

            controlPoints.last().type = ControlPointType.BEACON
        }
    }
}