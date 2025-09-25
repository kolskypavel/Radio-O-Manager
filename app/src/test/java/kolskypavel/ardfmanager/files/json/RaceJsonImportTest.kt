package kolskypavel.ardfmanager.files.json

import com.squareup.moshi.JsonDataException
import kolskypavel.ardfmanager.backend.files.processors.JsonProcessor
import kolskypavel.ardfmanager.backend.room.enums.RaceBand
import kolskypavel.ardfmanager.backend.room.enums.RaceLevel
import kolskypavel.ardfmanager.backend.room.enums.RaceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDateTime

class RaceJsonImportTest {

//    @Test
//    fun testToJson() {
//        val race = Race()
//        val cat1 = Category("A")
//        val cat2 = Category("B")
//
//        val adapter = RaceDataJsonAdapter()
//        val raceData = RaceData(race, listOf(cat1, cat2),)
//        val raceJson = adapter.fromJson(raceData)
//
//    }


    @Test
    fun testValidFromJson() {
        val stream = this::class.java.classLoader.getResourceAsStream("valid_race_import.ardfjs")
        val raceData = JsonProcessor.importRaceData(stream)

        assertEquals("EXAMPLE", raceData.race.name)
        assertEquals(LocalDateTime.of(2025, 11, 28, 13, 0, 0), raceData.race.startDateTime)
        assertEquals(RaceType.CLASSIC, raceData.race.raceType)
        assertEquals(RaceBand.M80, raceData.race.raceBand)
        assertEquals(RaceLevel.DISTRICT, raceData.race.raceLevel)

        val categories = raceData.categories.map { it -> it.category.name }.sorted()
        assertEquals(listOf("D19", "D20", "M19", "M20", "Ostatní", "RT"), categories)

        val competitors = raceData.competitorData.map { it -> it.competitorCategory.competitor }
        val startNumbers = competitors.map { it -> it.startNumber }.sorted()
        assertEquals(listOf(40, 41, 42, 43, 44, 45, 46), startNumbers)

        val comp1 = competitors.find { it -> it.siNumber == 10000 }
        assertEquals("KOLSKÝ Pavel", comp1?.getFullName())

    }

    @Test
    fun testInvalidFromJson() {
        val stream = this::class.java.classLoader.getResourceAsStream("invalid_race_import.ardfjs")
        assertThrows(JsonDataException::class.java) { JsonProcessor.importRaceData(stream) }

    }
}