package kolskypavel.ardfmanager.controlpoints

import androidx.test.platform.app.InstrumentationRegistry
import kolskypavel.ardfmanager.backend.helpers.ControlPointsHelper
import kolskypavel.ardfmanager.backend.room.enums.ControlPointType
import kolskypavel.ardfmanager.backend.room.enums.RaceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.UUID

/**
 * Checks wherever the control point parsing system works
 */
class ControlPointsParsingUnitTest {

    private val categoryId = UUID.randomUUID()
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun testOrienteeringValidParsing() {
        var cpString = "31 32 33 34 35 36 38 40"
        var result = ControlPointsHelper.getControlPointsFromString(
            cpString,
            categoryId,
            RaceType.ORIENTEERING,
            appContext
        )

        assertEquals(
            listOf(31, 32, 33, 34, 35, 36, 38, 40),
            result.map { cp -> cp.siCode }.toList()
        )
        assertEquals((1..8).toList(), result.map { cp -> cp.order }.toList())
        assertEquals(List(8) { ControlPointType.CONTROL }, result.map { cp -> cp.type }.toList())

        cpString = "102 31 49 55 52 35"
        result = ControlPointsHelper.getControlPointsFromString(
            cpString,
            categoryId,
            RaceType.ORIENTEERING,
            appContext
        )

        assertEquals(listOf(102, 31, 49, 55, 52, 35), result.map { cp -> cp.siCode }.toList())
        assertEquals((1..6).toList(), result.map { cp -> cp.order }.toList())
        assertEquals(List(6) { ControlPointType.CONTROL }, result.map { cp -> cp.type }.toList())

        cpString = "" // Empty string is fine
        result = ControlPointsHelper.getControlPointsFromString(
            cpString,
            categoryId,
            RaceType.ORIENTEERING,
            appContext
        )
        assertEquals(0, result.size)
    }

    @Test
    fun testOrienteeringInvalidParsing() {
        var cpString = "31 32433 34 35 36 38 40" //Invalid range of SI
        System.err.println(assertThrows(IllegalArgumentException::class.java) {
            ControlPointsHelper.getControlPointsFromString(
                cpString,
                categoryId,
                RaceType.ORIENTEERING,
                appContext
            )
        }.message)

        cpString = "22;33;44" //Invalid characters
        System.err.println(assertThrows(IllegalArgumentException::class.java) {
            ControlPointsHelper.getControlPointsFromString(
                cpString,
                categoryId,
                RaceType.ORIENTEERING,
                appContext
            )
        }.message)

        cpString = "#%99%%" //Invalid characters
        System.err.println(assertThrows(IllegalArgumentException::class.java) {
            ControlPointsHelper.getControlPointsFromString(
                cpString,
                categoryId,
                RaceType.ORIENTEERING,
                appContext
            )
        }.message)

        cpString = "44B 33!" //Classics and sprint not valid in orienteering
        System.err.println(assertThrows(IllegalArgumentException::class.java) {
            ControlPointsHelper.getControlPointsFromString(
                cpString,
                categoryId,
                RaceType.ORIENTEERING,
                appContext
            )
        }.message)

        cpString = "33 33" //Same control point in a row
        System.err.println(assertThrows(IllegalArgumentException::class.java) {
            ControlPointsHelper.getControlPointsFromString(
                cpString,
                categoryId,
                RaceType.ORIENTEERING,
                appContext
            )
        }.message)
    }

    @Test
    fun testClassicsValidParsing() {

        var cpString = "31 32 33 34 35 99B"
        var result = ControlPointsHelper.getControlPointsFromString(
            cpString,
            categoryId,
            RaceType.CLASSIC,
            appContext
        )
        assertEquals(listOf(31, 32, 33, 34, 35, 99), result.map { cp -> cp.siCode }.toList())
        assertEquals((1..6).toList(), result.map { cp -> cp.order }.toList())
        assertEquals(
            listOf(
                ControlPointType.CONTROL,
                ControlPointType.CONTROL,
                ControlPointType.CONTROL,
                ControlPointType.CONTROL,
                ControlPointType.CONTROL,
                ControlPointType.BEACON
            ), result.map { cp -> cp.type }.toList()
        )

        cpString = ""   // Empty string is fine
        result = ControlPointsHelper.getControlPointsFromString(
            cpString,
            categoryId,
            RaceType.CLASSIC,
            appContext
        )
        assertEquals(0, result.size)
    }

    @Test
    fun testClassicsInvalidParsing() {
        var cpString = "31 32 33 34 31 35 99B"
        System.err.println(assertThrows(java.lang.IllegalArgumentException::class.java) {
            ControlPointsHelper.getControlPointsFromString(
                cpString,
                categoryId,
                RaceType.CLASSIC,
                appContext
            )
        }.message)
        cpString = "31 32B 33 34 35 99" //Beacon must be the last CP
        System.err.println(assertThrows(java.lang.IllegalArgumentException::class.java) {
            ControlPointsHelper.getControlPointsFromString(
                cpString,
                categoryId,
                RaceType.CLASSIC,
                appContext
            )
        }.message)
        cpString = "31! 32B" //Beacon must be the last CP - check first
        System.err.println(assertThrows(java.lang.IllegalArgumentException::class.java) {
            ControlPointsHelper.getControlPointsFromString(
                cpString,
                categoryId,
                RaceType.CLASSIC,
                appContext
            )
        }.message)
        cpString = "31 32 33 34! 35 99" //No spectator controls are allowed on classics
        System.err.println(assertThrows(java.lang.IllegalArgumentException::class.java) {
            ControlPointsHelper.getControlPointsFromString(
                cpString,
                categoryId,
                RaceType.CLASSIC,
                appContext
            )
        }.message)
        cpString = "31! 32" //No spectator controls are allowed on classics - check first
        System.err.println(assertThrows(java.lang.IllegalArgumentException::class.java) {
            ControlPointsHelper.getControlPointsFromString(
                cpString,
                categoryId,
                RaceType.CLASSIC,
                appContext
            )
        }.message)
        cpString = "32 35 43 44B 99B" //Two beacons
        System.err.println(assertThrows(java.lang.IllegalArgumentException::class.java) {
            ControlPointsHelper.getControlPointsFromString(
                cpString,
                categoryId,
                RaceType.CLASSIC,
                appContext
            )
        }.message)
        cpString = "32 35 35 44" //Duplicate control points
        System.err.println(assertThrows(java.lang.IllegalArgumentException::class.java) {
            ControlPointsHelper.getControlPointsFromString(
                cpString,
                categoryId,
                RaceType.CLASSIC,
                appContext
            )
        }.message)
        cpString = "32 35 35 44 44B" //Same control point and beacon
        System.err.println(assertThrows(java.lang.IllegalArgumentException::class.java) {
            ControlPointsHelper.getControlPointsFromString(
                cpString,
                categoryId,
                RaceType.CLASSIC,
                appContext
            )
        }.message)
        cpString = "#%99%%" //Invalid characters
        System.err.println(assertThrows(java.lang.IllegalArgumentException::class.java) {
            ControlPointsHelper.getControlPointsFromString(
                cpString,
                categoryId,
                RaceType.CLASSIC,
                appContext
            )
        }.message)
    }


    @Test
    fun testSprintValidParsing() {
        var cpString = "31 32 33 34 36! 31 35 99B"
        var result = ControlPointsHelper.getControlPointsFromString(
            cpString,
            categoryId,
            RaceType.SPRINT,
            appContext
        )
        assertEquals(
            listOf(31, 32, 33, 34, 36, 31, 35, 99),
            result.map { cp -> cp.siCode }.toList()
        )
        assertEquals((1..8).toList(), result.map { cp -> cp.order }.toList())
        assertEquals(
            listOf(
                ControlPointType.CONTROL,
                ControlPointType.CONTROL,
                ControlPointType.CONTROL,
                ControlPointType.CONTROL,
                ControlPointType.SEPARATOR,
                ControlPointType.CONTROL,
                ControlPointType.CONTROL,
                ControlPointType.BEACON
            ), result.map { cp -> cp.type }.toList()
        )

        cpString = "31 32 33 34 36! 31 35 99"   //Beacon doesn't need to be present
        result = ControlPointsHelper.getControlPointsFromString(
            cpString,
            categoryId,
            RaceType.SPRINT,
            appContext
        )
        assertEquals(
            listOf(31, 32, 33, 34, 36, 31, 35, 99),
            result.map { cp -> cp.siCode }.toList()
        )
        assertEquals((1..8).toList(), result.map { cp -> cp.order }.toList())
        assertEquals(
            listOf(
                ControlPointType.CONTROL,
                ControlPointType.CONTROL,
                ControlPointType.CONTROL,
                ControlPointType.CONTROL,
                ControlPointType.SEPARATOR,
                ControlPointType.CONTROL,
                ControlPointType.CONTROL,
                ControlPointType.CONTROL
            ), result.map { cp -> cp.type }.toList()
        )

        cpString = "33 34 35 36"    //No separator is needed
        result = ControlPointsHelper.getControlPointsFromString(
            cpString,
            categoryId,
            RaceType.SPRINT,
            appContext
        )
        assertEquals(listOf(33, 34, 35, 36), result.map { cp -> cp.siCode }.toList())
        assertEquals((1..4).toList(), result.map { cp -> cp.order }.toList())
        assertEquals(
            listOf(
                ControlPointType.CONTROL,
                ControlPointType.CONTROL,
                ControlPointType.CONTROL,
                ControlPointType.CONTROL
            ), result.map { cp -> cp.type }.toList()
        )

        cpString = "31 32 33 34 36! 31 32 99B"  //Duplicate controls separated
        result = ControlPointsHelper.getControlPointsFromString(
            cpString,
            categoryId,
            RaceType.SPRINT,
            appContext
        )
        assertEquals(
            listOf(31, 32, 33, 34, 36, 31, 32, 99),
            result.map { cp -> cp.siCode }.toList()
        )
        assertEquals((1..8).toList(), result.map { cp -> cp.order }.toList())
        assertEquals(
            listOf(
                ControlPointType.CONTROL,
                ControlPointType.CONTROL,
                ControlPointType.CONTROL,
                ControlPointType.CONTROL,
                ControlPointType.SEPARATOR,
                ControlPointType.CONTROL,
                ControlPointType.CONTROL,
                ControlPointType.BEACON
            ), result.map { cp -> cp.type }.toList()
        )

        cpString = "31 36! 42 36!"  //Same separators are fine
        result = ControlPointsHelper.getControlPointsFromString(
            cpString,
            categoryId,
            RaceType.SPRINT,
            appContext
        )
        assertEquals(listOf(31, 36, 42, 36), result.map { cp -> cp.siCode }.toList())
        assertEquals((1..4).toList(), result.map { cp -> cp.order }.toList())
        assertEquals(
            listOf(
                ControlPointType.CONTROL,
                ControlPointType.SEPARATOR,
                ControlPointType.CONTROL,
                ControlPointType.SEPARATOR
            ), result.map { cp -> cp.type }.toList()
        )

        cpString = "31 32 36! 41 42 43 99B"  //Same separator and beacon
        result = ControlPointsHelper.getControlPointsFromString(
            cpString,
            categoryId,
            RaceType.SPRINT,
            appContext
        )
        assertEquals(listOf(31, 32, 36, 41, 42, 43, 99), result.map { cp -> cp.siCode }.toList())
        assertEquals((1..7).toList(), result.map { cp -> cp.order }.toList())
        assertEquals(
            listOf(
                ControlPointType.CONTROL,
                ControlPointType.CONTROL,
                ControlPointType.SEPARATOR,
                ControlPointType.CONTROL,
                ControlPointType.CONTROL,
                ControlPointType.CONTROL,
                ControlPointType.BEACON
            ), result.map { cp -> cp.type }.toList()
        )

        cpString = ""   // Empty string is fine
        result = ControlPointsHelper.getControlPointsFromString(
            cpString,
            categoryId,
            RaceType.SPRINT,
            appContext
        )
        assertEquals(0, result.size)
    }

    @Test
    fun testSprintInvalidParsing() {
        var cpString = "45B 45B"    //Two beacons
        System.err.println(assertThrows(java.lang.IllegalArgumentException::class.java) {
            ControlPointsHelper.getControlPointsFromString(
                cpString,
                categoryId,
                RaceType.SPRINT,
                appContext
            )
        }.message)

        cpString = "31 32 33 34 31 36! 31 32 99B"  //Duplicate controls in the same loop
        System.err.println(assertThrows(java.lang.IllegalArgumentException::class.java) {
            ControlPointsHelper.getControlPointsFromString(
                cpString,
                categoryId,
                RaceType.SPRINT,
                appContext
            )
        }.message)

        cpString = "#%99%%" //Invalid characters
        System.err.println(assertThrows(java.lang.IllegalArgumentException::class.java) {
            ControlPointsHelper.getControlPointsFromString(
                cpString,
                categoryId,
                RaceType.SPRINT,
                appContext
            )
        }.message)
    }
}