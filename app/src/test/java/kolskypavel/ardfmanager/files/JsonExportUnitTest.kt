package kolskypavel.ardfmanager.files

import kolskypavel.ardfmanager.backend.files.processors.JsonProcessor
import kolskypavel.ardfmanager.backend.room.entity.Category
import kolskypavel.ardfmanager.backend.room.entity.Competitor
import kolskypavel.ardfmanager.backend.room.entity.Result
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorCategory
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ResultData
import kolskypavel.ardfmanager.backend.room.enums.ResultStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.util.UUID

class JsonExportUnitTest {

    @Test
    fun testJsonResultExport() = runBlocking {
        val raceIdRandom = UUID.randomUUID()
        val competitorId = UUID.randomUUID()
        val categoryId = UUID.randomUUID()

        val category = Category().apply {
            id = categoryId
            raceId = raceIdRandom
            name = "M20"
            isMan = true
            maxAge = null
            length = 0f
            climb = 0f
            order = 0
            differentProperties = false
            raceType = null
            categoryBand = null
            timeLimit = null
            controlPointsString = ""
        }


        val result = Result(
            id = UUID.randomUUID(),
            raceId = raceIdRandom,
            competitorID = competitorId,
            categoryId = categoryId,
            siNumber = 12345,
            cardType = 0,
            checkTime = null,
            origCheckTime = null,
            startTime = null,
            origStartTime = null,
            finishTime = null,
            origFinishTime = null,
            automaticStatus = true,
            resultStatus = ResultStatus.VALID,
            runTime = Duration.ofSeconds(2142), // 35:42
            modified = false,
            sent = false
        ).apply {
            place = 1
            points = 5
        }

        val competitor = Competitor(
            id = competitorId,
            raceId = raceIdRandom,
            categoryId = categoryId,
            firstName = "Petr",
            lastName = "Novák",
            club = "SK RADIOSPORT",
            index = "GBM0012",
            isMan = true,
            birthYear = 2000,
            siNumber = 12345,
            siRent = false,
            startNumber = 42,
            drawnRelativeStartTime = null
        )

        val competitorCategory = CompetitorCategory(
            competitor = competitor,
            category   = category
        )

        val resultData = ResultData(
            result = result,
            punches = emptyList(),
            competitorCategory = competitorCategory
        )



        val outputStream = ByteArrayOutputStream()
        JsonProcessor.exportResults(outputStream, listOf(resultData))

        val json = outputStream.toString(Charsets.UTF_8.name())
        println(json)

    }


    @Test
    fun testJsonRaceExport() {

    }
}