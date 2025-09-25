package kolskypavel.ardfmanager.files

import kolskypavel.ardfmanager.backend.room.entity.Category
import kolskypavel.ardfmanager.backend.room.entity.Competitor
import kolskypavel.ardfmanager.backend.room.entity.ControlPoint
import kolskypavel.ardfmanager.backend.room.enums.ControlPointType
import org.junit.Assert.assertEquals
import org.junit.Test

class CsvExportEntityUnitTest {

    @Test
    fun testCategoryCsvString() {
        val category = Category("TEST")
        assertEquals("TEST;1;0;0.0;0.0;0;;;;", category.toCSVString())
    }

    @Test
    fun testControlPointCsvString() {
        val controlPoint = ControlPoint()
        assertEquals("31#TEST#0#0#1", controlPoint.toCsvString())
        controlPoint.siCode = 99
        controlPoint.type = ControlPointType.BEACON
        assertEquals("99#B#1#0#2", controlPoint.toCsvString())
    }

    @Test
    fun testCompetitorCsvString() {
        val competitor = Competitor()
        val categoryStr = "M20"
        assertEquals(
            "123456789;Test;Tester;M20;1;2000;;AC Test;;0;ACT0001",
            competitor.toSimpleCsvString(categoryStr)
        )
    }
}