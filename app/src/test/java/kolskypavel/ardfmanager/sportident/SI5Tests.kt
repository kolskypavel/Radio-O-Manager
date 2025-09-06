package kolskypavel.ardfmanager.sportident

import junit.framework.TestCase.assertEquals
import kolskypavel.ardfmanager.backend.results.ResultsProcessor
import kolskypavel.ardfmanager.backend.room.entity.Result
import kolskypavel.ardfmanager.backend.room.enums.ResultStatus
import kolskypavel.ardfmanager.backend.sportident.SIConstants
import kolskypavel.ardfmanager.backend.sportident.SIPort
import kolskypavel.ardfmanager.backend.sportident.SIPort.CardData
import kolskypavel.ardfmanager.backend.sportident.SITime
import org.junit.Test
import java.time.Duration
import java.time.LocalTime
import java.util.UUID

class SI5Tests {

    //Test the SI5 time adjustment because of the 12h format
    @Test
    fun testSI5DataAdjustment() {

        val checkTime = SITime(LocalTime.of(10, 5, 0))
        val startTime = SITime(LocalTime.of(10, 10, 0))

        val punchData = arrayListOf(
            SIPort.PunchData(1, SITime(LocalTime.of(10, 20, 0))),
            SIPort.PunchData(2, SITime(LocalTime.of(2, 21, 11))),
            SIPort.PunchData(3, SITime(LocalTime.of(3, 27, 25))),
            SIPort.PunchData(4, SITime(LocalTime.of(10, 20, 33))),
            SIPort.PunchData(5, SITime(LocalTime.of(4, 14, 44))),
            SIPort.PunchData(6, SITime(LocalTime.of(8, 14, 7))),
            SIPort.PunchData(6, SITime(LocalTime.of(8, 20, 24))),
            SIPort.PunchData(7, SITime(LocalTime.of(1, 33, 24))),
            SIPort.PunchData(8, SITime(LocalTime.of(3, 33, 2))),
            SIPort.PunchData(9, SITime(LocalTime.of(0, 0, 0))),
            SIPort.PunchData(10, SITime(LocalTime.of(9, 17, 10))),
        )

        val finishTime = SITime(LocalTime.of(9, 43, 0))

        var zeroTimeBase = LocalTime.of(10, 0)
        val cardData =
            CardData(SIConstants.SI_CARD5, 12345, checkTime, startTime, finishTime, punchData)

        var result =
            Result(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                cardData.siNumber,
                cardData.cardType,
                cardData.checkTime,
                cardData.checkTime,
                cardData.startTime,
                cardData.startTime,
                cardData.finishTime,
                cardData.finishTime,
                automaticStatus = false,
                resultStatus = ResultStatus.NO_RANKING,
                runTime = Duration.ZERO,
                modified = false,
                sent = false
            )

        val resultPunches = ResultsProcessor.processCardPunches(
            cardData,
            UUID.randomUUID(),
            result,
            zeroTimeBase
        )


        assertEquals("10:05:00,0,0", cardData.checkTime.toString())
        assertEquals("10:10:00,0,0", cardData.startTime.toString())

        assertEquals("10:20:00,0,0", resultPunches[0].siTime.toString())
        assertEquals("14:21:11,0,0", resultPunches[1].siTime.toString())
        assertEquals("15:27:25,0,0", resultPunches[2].siTime.toString())
        assertEquals("22:20:33,0,0", resultPunches[3].siTime.toString())
        assertEquals("04:14:44,1,0", resultPunches[4].siTime.toString())
        assertEquals("08:14:07,1,0", resultPunches[5].siTime.toString())
        assertEquals("08:20:24,1,0", resultPunches[6].siTime.toString())
        assertEquals("13:33:24,1,0", resultPunches[7].siTime.toString())
        assertEquals("15:33:02,1,0", resultPunches[8].siTime.toString())
        assertEquals("00:00:00,2,0", resultPunches[9].siTime.toString())
        assertEquals("09:17:10,2,0", resultPunches[10].siTime.toString())

        assertEquals("09:43:00,2,0", cardData.finishTime.toString())

        cardData.startTime = SITime(LocalTime.of(2, 33, 5))
        cardData.finishTime = SITime(LocalTime.of(2, 33, 33))

        cardData.punchData = arrayListOf(
            SIPort.PunchData(1, SITime(LocalTime.of(2, 33, 11))),
            SIPort.PunchData(2, SITime(LocalTime.of(2, 33, 12))),
            SIPort.PunchData(3, SITime(LocalTime.of(2, 33, 15))),
            SIPort.PunchData(4, SITime(LocalTime.of(2, 33, 17))),
            SIPort.PunchData(5, SITime(LocalTime.of(2, 33, 19))),
            SIPort.PunchData(6, SITime(LocalTime.of(2, 33, 21))),
            SIPort.PunchData(7, SITime(LocalTime.of(2, 33, 23))),
            SIPort.PunchData(8, SITime(LocalTime.of(2, 33, 25))),
            SIPort.PunchData(9, SITime(LocalTime.of(2, 33, 28))),
            SIPort.PunchData(10, SITime(LocalTime.of(2, 33, 30))),
            SIPort.PunchData(11, SITime(LocalTime.of(2, 33, 33))),
        )

        result =
            Result(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                cardData.siNumber,
                cardData.cardType,
                cardData.checkTime,
                cardData.checkTime,
                cardData.startTime,
                cardData.startTime,
                cardData.finishTime,
                cardData.finishTime,
                automaticStatus = false,
                resultStatus = ResultStatus.NO_RANKING,
                runTime = Duration.ZERO,
                modified = false,
                sent = false
            )

        ResultsProcessor.processCardPunches(
            cardData,
            UUID.randomUUID(),
            result,
            zeroTimeBase
        )

        assertEquals("14:33:33,0,0", result.finishTime.toString())
    }
}