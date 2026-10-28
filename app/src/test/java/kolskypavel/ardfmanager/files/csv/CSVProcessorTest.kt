package kolskypavel.ardfmanager.files.csv

import android.content.Context
import android.content.SharedPreferences
import kolskypavel.ardfmanager.R
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.constants.DataType
import kolskypavel.ardfmanager.backend.files.processors.CsvProcessor
import kolskypavel.ardfmanager.backend.room.entity.Category
import kolskypavel.ardfmanager.backend.room.entity.Competitor
import kolskypavel.ardfmanager.backend.room.entity.ControlPoint
import kolskypavel.ardfmanager.backend.room.entity.Punch
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.Result
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.AliasPunch
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CategoryData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorCategory
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ResultData
import kolskypavel.ardfmanager.backend.room.enums.ControlPointType
import kolskypavel.ardfmanager.backend.room.enums.RaceBand
import kolskypavel.ardfmanager.backend.room.enums.RaceType
import kolskypavel.ardfmanager.backend.room.enums.SIRecordType
import kolskypavel.ardfmanager.backend.room.enums.StandardCategoryType
import kolskypavel.ardfmanager.backend.sportident.SITime
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.time.LocalTime

@RunWith(MockitoJUnitRunner::class)
class CSVProcessorTest {
    val dataProcessor: DataProcessor = mock()
    val context: Context = mock()
    val sharedPrefs: SharedPreferences = mock()

    @Before
    fun setup() {
        `when`(dataProcessor.getContext()).thenReturn(context)
    }

    // Import tests
    @Test
    fun testCategoryValidImport() {
        val race = Race()

        val stream =
            this::class.java.classLoader?.getResourceAsStream("csv/categories_valid_import.csv")
        val raceData = CsvProcessor.importCategories(stream!!, race, dataProcessor)

        assertTrue(raceData.competitorCategories.isEmpty())
        assertTrue(raceData.invalidLines.isEmpty())
        assertEquals(7, raceData.categories.size)

        val sortedActual = raceData.categories.sortedBy { it.category.name }
        val expectedNames = listOf("M21", "M40", "M50", "M60", "M70", "W21", "W60")
        assertEquals(expectedNames, sortedActual.map { it.category.name })

        val m21 = sortedActual.find { it.category.name == "M21" }!!
        assertEquals(true, m21.category.isMan)
        assertEquals(39, m21.category.maxAge)
        assertEquals("31 32 33 34 35 46B", m21.category.controlPointsString)
        assertEquals(6, m21.controlPoints.size)
        assertEquals(listOf(31, 32, 33, 34, 35, 46), m21.controlPoints.map { it.siCode })
        assertEquals(ControlPointType.BEACON, m21.controlPoints.last().type)
    }

    @Test
    fun testCategoryInvalidImport() {
        val race = Race()

        val stream =
            this::class.java.classLoader?.getResourceAsStream("csv/categories_invalid_import.csv")
        val raceData = CsvProcessor.importCategories(stream!!, race, dataProcessor)
        assertTrue(raceData.categories.isEmpty())
        assertEquals(8, raceData.invalidLines.size)
    }

    @Test
    fun testCompetitorValidImport() = runTest {
        val race = Race()
        `when`(dataProcessor.getHighestCategoryOrder(any())).thenReturn(0)
        `when`(dataProcessor.getHighestStartNumberByRace(any())).thenReturn(0)

        val stream =
            this::class.java.classLoader?.getResourceAsStream("csv/competitors_valid_import.csv")
        val raceData =
            CsvProcessor.importCompetitorData(
                stream!!,
                race,
                HashSet(),
                dataProcessor,
                context
            )

        assertTrue(raceData.invalidLines.isEmpty())
        assertEquals(5, raceData.competitorCategories.size)

        val john =
            raceData.competitorCategories.find { it.competitor.firstName == "John" }?.competitor
        assertTrue(john != null)
        assertEquals("Test", john?.lastName)
        assertEquals(1, john?.startNumber)
        assertEquals(200200, john?.siNumber)
        assertEquals("AC Prague", john?.club)
        assertEquals("AAP0004", john?.index)
        assertEquals(2000, john?.birthYear)
        assertEquals(Duration.ofMinutes(35), john?.drawnRelativeStartTime)
        assertEquals(false, john?.siRent)
    }

    @Test
    fun testCompetitorInvalidImport() = runTest {
        val race = Race()
        `when`(dataProcessor.getHighestCategoryOrder(any())).thenReturn(0)
        `when`(dataProcessor.getHighestStartNumberByRace(any())).thenReturn(0)

        val stream =
            this::class.java.classLoader?.getResourceAsStream("csv/competitors_invalid_import.csv")
        val raceData = CsvProcessor.importCompetitorData(
            stream!!, race, HashSet(),
            dataProcessor, context
        )

        assertEquals(3, raceData.competitorCategories.size)
        assertEquals(2, raceData.invalidLines.size)
    }

    @Test
    fun testImportCompetitorStarts() = runTest {
        val race = Race()
        val competitor1 = Competitor().apply {
            startNumber = 1
            drawnRelativeStartTime = Duration.ZERO
            siNumber = 100000
        }
        val competitorData = CompetitorData(
            competitorCategory = CompetitorCategory(competitor1, Category("M20")),
            readoutData = null
        )

        `when`(context.packageName).thenReturn("kolskypavel.ardfmanager")
        `when`(context.getString(any())).thenReturn("key_files_prefer_app_start_time")
        `when`(context.getSharedPreferences(any(), ArgumentMatchers.anyInt()))
            .thenReturn(sharedPrefs)
        `when`(sharedPrefs.getBoolean(any(), ArgumentMatchers.anyBoolean()))
            .thenReturn(false)

        `when`(dataProcessor.getCompetitorDataFlowByRace(any()))
            .thenReturn(flowOf(listOf(competitorData)))

        val csvContent = "1;35:00;200200\n"
        val stream = csvContent.byteInputStream()

        val result = CsvProcessor.importData(
            stream,
            DataType.STARTLIST,
            race,
            dataProcessor
        )

        assertTrue(result.invalidLines.isEmpty())
        assertEquals(1, result.competitorCategories.size)
        assertEquals(
            Duration.ofMinutes(35),
            result.competitorCategories[0].competitor.drawnRelativeStartTime
        )
        assertEquals(200200, result.competitorCategories[0].competitor.siNumber)
    }

    @Test
    fun testImportStandardCategories() = runTest {
        val race = Race()
        `when`(context.resources).thenReturn(mock())
        `when`(context.resources.getStringArray(R.array.standard_categories_international))
            .thenReturn(arrayOf("M21;1;21", "W21;0;21"))
        `when`(dataProcessor.getCategoryByName(any(), any()))
            .thenReturn(null)

        val categories = CsvProcessor.importStandardCategories(
            StandardCategoryType.INTERNATIONAL,
            race,
            dataProcessor
        )

        assertEquals(2, categories.size)
        assertEquals("M21", categories[0].name)
        assertEquals(true, categories[0].isMan)
        assertEquals(21, categories[0].maxAge)
        assertEquals("W21", categories[1].name)
        assertEquals(false, categories[1].isMan)
        assertEquals(21, categories[1].maxAge)
    }

    @Test
    fun testImportDataDelegation() = runTest {
        val race = Race()
        val stream = "W21;0;39;;;1;;;;31 32 33 34 35 46B".byteInputStream()

        val result = CsvProcessor.importData(stream, DataType.CATEGORIES, race, dataProcessor)
        assertEquals(1, result.categories.size)
        assertEquals("W21", result.categories[0].category.name)
    }

    // Export tests
    @Test
    fun testCategoryExport() = runTest {
        val category1 = Category("W21").apply {
            isMan = false
            maxAge = 39
            length = 3500
            climb = 150
            order = 1
            differentProperties = true
            raceType = RaceType.CLASSIC
            categoryBand = RaceBand.M2
            timeLimit = Duration.ofMinutes(120)
        }
        val category2 = Category("M20")
        val controlPoint1 = ControlPoint().apply {
            siCode = 31
            type = ControlPointType.CONTROL
        }
        val controlPoint2 = ControlPoint().apply {
            siCode = 46
            type = ControlPointType.BEACON
        }

        val outStream = ByteArrayOutputStream()
        CsvProcessor.exportCategories(
            outStream, listOf(
                CategoryData(
                    category1,
                    listOf(controlPoint1, controlPoint2),
                    emptyList()
                ),
                CategoryData(
                    category2,
                    listOf(controlPoint2),
                    emptyList()
                )
            )
        )

        val csvString = outStream.toString("UTF-8")
        assertEquals(
            "W21;0;39;3500;150;1;0;120;M2;31 46B\n" +
                    "M20;1;100;0;0;0;;;;46B", csvString
        )
    }

    @Test
    fun testReadoutDataExport() = runTest {
        val outStream = ByteArrayOutputStream()
        CsvProcessor.exportReadoutData(
            outStream,
            listOf(
                ResultData(
                    Result().apply {
                        siNumber = 200200
                        checkTime = SITime(LocalTime.of(10, 0, 0))
                        startTime = SITime(LocalTime.of(10, 15, 0))
                        finishTime = SITime(LocalTime.of(11, 45, 0))
                    },
                    listOf(
                        AliasPunch(Punch(31, SITime(LocalTime.of(10, 30, 0)), SIRecordType.CONTROL, 1)),
                        AliasPunch(Punch(32, SITime(LocalTime.of(10, 45, 0)), SIRecordType.CONTROL, 2))
                    ),
                    null
                ),
                ResultData(
                    Result().apply {
                        siNumber = 140201
                        checkTime = SITime(LocalTime.of(10, 5, 0))
                        startTime = SITime(LocalTime.of(10, 20, 0))
                        finishTime = SITime(LocalTime.of(11, 50, 0))
                    },
                    listOf(
                        AliasPunch(Punch(31, SITime(LocalTime.of(10, 35, 0)), SIRecordType.CONTROL, 1)),
                        AliasPunch(Punch(33, SITime(LocalTime.of(10, 55, 0)), SIRecordType.CONTROL, 2)),
                        AliasPunch(Punch(35, SITime(LocalTime.of(11, 15, 0)), SIRecordType.CONTROL, 3))
                    ),
                    null
                )
            )
        )

        assertEquals(
            "200200;10:00:00;10:15:00;11:45:00;2;31;10:30:00;32;10:45:00\n" +
                    "140201;10:05:00;10:20:00;11:50:00;3;31;10:35:00;33;10:55:00;35;11:15:00\n",
            outStream.toString("UTF-8")
        )
    }
}