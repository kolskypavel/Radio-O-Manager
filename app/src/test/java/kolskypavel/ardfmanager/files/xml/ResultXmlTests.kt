package kolskypavel.ardfmanager.files.xml

import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.processors.IofXmlProcessor
import kolskypavel.ardfmanager.backend.results.ResultsProcessor
import kolskypavel.ardfmanager.backend.results.ResultsProcessor.toResultWrappers
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
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.diff.DefaultNodeMatcher
import org.xmlunit.diff.ElementSelectors

@RunWith(RobolectricTestRunner::class)
class ResultXmlTests {
    @Test
    fun testResultExport() = runTest {
        val dataProcessor = mock(DataProcessor::class.java)

        val race = Race()

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

        val comp = Competitor()
        val compData = listOf(
            CompetitorData(
                CompetitorCategory(comp, Category("A")),
                readoutData
            )
        )

        val out = ByteArrayOutputStream()
        IofXmlProcessor.exportResults(out, race, compData.toResultWrappers(), dataProcessor)
        val xml = out.toString("UTF-8")

        val stream =
            this::class.java.classLoader?.getResourceAsStream("xml/xml_results_example.xml")!!
        val valid = stream.bufferedReader().use { it.readText() }

        assertEquals(xml, "")
        // Use XMLUnit to compare structure, ignoring whitespace and attribute order
//        val diff = DiffBuilder.compare(valid)
//            .withTest(xml)
//            .ignoreWhitespace()
//            .ignoreComments()
//            .withNodeMatcher(DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes))
//            .checkForSimilar()
//            .build()
//
//        assertEquals("XMLs are different: ${diff.toString()}",false, diff.hasDifferences())
    }
}