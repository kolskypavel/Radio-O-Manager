package kolskypavel.ardfmanager.files.xml

import android.content.Context
import junit.framework.TestCase.assertEquals
import kolskypavel.ardfmanager.backend.files.processors.IofXmlProcessor
import kolskypavel.ardfmanager.backend.room.entity.Race
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CategoryXmlTests {

    @Test
    fun testCategoryValidImport() = runTest {

        val race = Race()
        val context = mock(Context::class.java)

        val stream =
            this::class.java.classLoader?.getResourceAsStream("xml/xml_category_valid_example.xml")!!

        val wrapper = IofXmlProcessor.importCategories(stream, race, context)
        val catA = wrapper.categories[0]
        val catB = wrapper.categories[1]

        assertEquals(2, wrapper.categories.size)
        assertEquals("A", catA.category.name)
        assertEquals(2960, catA.category.length)
        assertEquals(95, catA.category.climb)
        assertEquals("B", catB.category.name)
        assertEquals(2960, catB.category.length)
        assertEquals(95, catB.category.climb)

        val cpA = catA.controlPoints
        val cpB = catB.controlPoints

        assertEquals(31, cpA[0].siCode)
        assertEquals(32, cpA[1].siCode)
        assertEquals(33, cpA[2].siCode)
        assertEquals(31, cpA[3].siCode)
        assertEquals(34, cpA[4].siCode)
        assertEquals(35, cpA[5].siCode)
        assertEquals(31, cpA[6].siCode)
        assertEquals(100, cpA[7].siCode)

        assertEquals(31, cpB[0].siCode)
        assertEquals(34, cpB[1].siCode)
        assertEquals(35, cpB[2].siCode)
        assertEquals(31, cpB[3].siCode)
        assertEquals(32, cpB[4].siCode)
        assertEquals(33, cpB[5].siCode)
        assertEquals(31, cpB[6].siCode)
        assertEquals(100, cpB[7].siCode)
    }

    @Test
    fun testCategoryNameMissing() = runTest {

        val race = Race()
        val context = mock(Context::class.java)

        `when`(context.getString(any())).thenReturn($$"Category name missing at line: %1$d")

        val stream =
            this::class.java.classLoader?.getResourceAsStream("xml/xml_category_invalid_example.xml")!!

        assertThrows(IllegalArgumentException::class.java) {
            IofXmlProcessor.importCategories(stream, race, context)
        }
    }
}