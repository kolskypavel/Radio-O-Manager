package kolskypavel.ardfmanager.files.xml

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID

import kolskypavel.ardfmanager.backend.files.processors.IofXmlProcessor
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.Category
import kolskypavel.ardfmanager.backend.room.entity.Competitor
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CategoryData
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.diff.DefaultNodeMatcher
import org.xmlunit.diff.ElementSelectors


@RunWith(RobolectricTestRunner::class)
class StartListXmlTests {

    @Test
    fun testExportStartList() = runBlocking {
        // Prepare a race with a fixed start date/time
        val raceStart = LocalDateTime.of(2023, 6, 15, 9, 30, 0)
        val race = Race(
            UUID.randomUUID(),
            "Test Race",
            "",
            raceStart,
            kolskypavel.ardfmanager.backend.room.enums.RaceType.CLASSIC,
            kolskypavel.ardfmanager.backend.room.enums.RaceLevel.PRACTICE,
            kolskypavel.ardfmanager.backend.room.enums.RaceBand.M80,
            Duration.ZERO
        )

        // Prepare category
        val category = Category(
            UUID.randomUUID(),
            race.id,
            "M21",
            true,
            null,
            5.2f,
            120f,
            0,
            false,
            null,
            null,
            null,
            ""
        )

        // Prepare two competitors with relative start times
        val comp1 = Competitor(
            UUID.randomUUID(),
            race.id,
            category.id,
            "Jan",
            "Novak",
            "Club A",
            "IDX1",
            true,
            1990,
            111,
            false,
            1,
            Duration.ofSeconds(0)
        )
        val comp2 = Competitor(
            UUID.randomUUID(),
            race.id,
            category.id,
            "Petr",
            "Svoboda",
            "Club B",
            "IDX2",
            true,
            1992,
            222,
            false,
            2,
            Duration.ofSeconds(60)
        )

        val catData = CategoryData(category, emptyList(), listOf(comp1, comp2))

        val out = ByteArrayOutputStream()

        // Call the suspend export function
        IofXmlProcessor.exportStartList(out, race, listOf(catData))

        val xml = out.toString()
        val stream =
            this::class.java.classLoader?.getResourceAsStream("xml/xml_startlist_example.xml")!!
        val valid = stream.bufferedReader().use { it.readText() }

        // assertEquals(valid, xml)
        val diff = DiffBuilder.compare(valid)
            .withTest(xml)
            .ignoreWhitespace()
            .ignoreComments()
            .withNodeMatcher(DefaultNodeMatcher(ElementSelectors.byNameAndAllAttributes))
            .checkForSimilar()
            .build()

        assertEquals("XMLs are different: $diff", false, diff.hasDifferences())
    }
}