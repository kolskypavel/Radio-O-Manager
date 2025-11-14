package kolskypavel.ardfmanager.files.json

import ResultJsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import junit.framework.TestCase.assertEquals
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.json.adapters.LocalDateTimeAdapter
import kolskypavel.ardfmanager.backend.files.json.adapters.RaceDataJsonAdapter
import kolskypavel.ardfmanager.backend.files.json.temps.ResultJson
import kolskypavel.ardfmanager.backend.results.ResultsProcessor
import kolskypavel.ardfmanager.backend.room.entity.Alias
import kolskypavel.ardfmanager.backend.room.entity.Category
import kolskypavel.ardfmanager.backend.room.entity.Competitor
import kolskypavel.ardfmanager.backend.room.entity.Punch
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.Result
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.AliasPunch
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorCategory
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ReadoutData
import kolskypavel.ardfmanager.backend.room.enums.ResultStatus
import kolskypavel.ardfmanager.backend.room.enums.SIRecordType
import kolskypavel.ardfmanager.backend.sportident.SITime
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.LocalTime
import java.util.UUID
import java.time.Duration
import java.time.LocalDateTime

class ResultJsonTests {

    @Test
    fun testToJson() {
        val dataProcessor = mock(DataProcessor::class.java)
        val race = Race()

        `when`(dataProcessor.resultStatusToShortString(org.mockito.kotlin.any()))
            .thenReturn("OK")

        `when`(dataProcessor.punchStatusToShortString(org.mockito.kotlin.any()))
            .thenReturn("OK")

        val result = Result()
        result.startTime = SITime(LocalTime.of(13, 0, 0))
        result.finishTime = SITime(LocalTime.of(14, 15, 0))
        result.resultStatus = ResultStatus.OK
        result.runTime = Duration.ofMinutes(75)
        result.readoutTime = LocalDateTime.of(2025, 9, 25, 14, 18, 24)

        val punches = arrayListOf(
            Punch(13, SITime(LocalTime.of(13, 0, 0)), SIRecordType.START, 1),
            Punch(31, SITime(LocalTime.of(13, 35, 0)), SIRecordType.CONTROL, 1),
            Punch(32, SITime(LocalTime.of(13, 43, 11)), SIRecordType.CONTROL, 1),
            Punch(33, SITime(LocalTime.of(14, 5, 50)), SIRecordType.CONTROL, 1),
            Punch(34, SITime(LocalTime.of(14, 10, 22)), SIRecordType.CONTROL, 1),
        )
        ResultsProcessor.calculateSplits(punches)

        val ap = punches.mapIndexed { index, punch ->
            AliasPunch(
                punch,
                Alias(punch.siCode, index.toString())
            )
        }
        val readoutData = ReadoutData(result, ap)

        val compData = CompetitorData(
            CompetitorCategory(Competitor(), Category("A")),
            readoutData
        )
        val json = ResultJsonAdapter(race, dataProcessor).toJson(compData)

        val moshi: Moshi = Moshi.Builder()
            .add(RaceDataJsonAdapter(dataProcessor))
            .add(LocalDateTimeAdapter())
            .add(KotlinJsonAdapterFactory())
            .build()
        val out = moshi.adapter(ResultJson::class.java).toJson(json)

        val stream =
            this::class.java.classLoader.getResourceAsStream("json/json_results_filtered_start.ardfjs")
        val valid = stream.bufferedReader().use { it.readText() }.filterNot { it.isWhitespace() }

        assertEquals(valid, out)
    }
}