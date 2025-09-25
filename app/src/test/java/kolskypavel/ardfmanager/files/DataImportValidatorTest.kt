package kolskypavel.ardfmanager.files

import android.content.Context
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.DataImportValidator
import kolskypavel.ardfmanager.backend.files.constants.DataType
import kolskypavel.ardfmanager.backend.files.wrappers.DataImportWrapper
import kolskypavel.ardfmanager.backend.room.entity.Category
import kolskypavel.ardfmanager.backend.room.entity.Competitor
import kolskypavel.ardfmanager.backend.room.entity.Punch
import kolskypavel.ardfmanager.backend.room.entity.Result
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.AliasPunch
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CategoryData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorCategory
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ReadoutData
import kolskypavel.ardfmanager.backend.room.enums.SIRecordType
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.anyVararg
import java.util.UUID

// Test class for DataImportValidator
class DataImportValidatorTest {

    private lateinit var context: Context
    private lateinit var dataProcessor: DataProcessor
    private val raceId = UUID.randomUUID()

    @Before
    fun setup() {
        context = mock(Context::class.java)
        dataProcessor = mock(DataProcessor::class.java)

        // Stub getString(id, vararg) to return a simple readable message
        `when`(context.getString(anyInt())).thenAnswer { invocation ->
            "msg_${invocation.arguments[0]}"
        }
        `when`(context.getString(anyInt(), anyVararg())).thenAnswer { invocation ->
            val id = invocation.arguments[0] as Int
            val args = invocation.arguments.drop(1).joinToString(",")
            "msg_${id}($args)"
        }
    }

    @Test
    fun testValidateThrowsOnDuplicateCategoryNames() {

        val categories = listOf(
            CategoryData(category = Category("A"), emptyList(), emptyList()),
            CategoryData(category = Category("A"), emptyList(), emptyList())
        )

        assertThrows(IllegalArgumentException::class.java) {
            DataImportValidator.validateCategories(categories, context)
        }
    }

    @Test
    fun testValidateThrowsOnDuplicateCompetitorSI() {
        val competitor1 = Competitor()
        competitor1.siNumber = 10000

        val competitor2 = Competitor()
        competitor2.siNumber = 10000

        val wrapper = DataImportWrapper(
            competitorCategories = listOf(
                CompetitorCategory(competitor1, Category("")),
                CompetitorCategory(competitor2, Category(""))
            ), categories = emptyList(), invalidLines = arrayListOf()
        )

        assertThrows(IllegalArgumentException::class.java) {
            DataImportValidator.validateDataImport(
                wrapper,
                raceId,
                DataType.COMPETITORS,
                dataProcessor,
                context
            )
        }
    }

    @Test
    fun testValidateThrowsOnDuplicateCompetitorStartNumber() {
        val competitor1 = Competitor()
        competitor1.startNumber = 1

        val competitor2 = Competitor()
        competitor2.startNumber = 1

        val wrapper = DataImportWrapper(
            competitorCategories = listOf(
                CompetitorCategory(competitor1, Category("")),
                CompetitorCategory(competitor2, Category(""))
            ), categories = emptyList(), invalidLines = arrayListOf()
        )

        assertThrows(IllegalArgumentException::class.java) {
            DataImportValidator.validateDataImport(
                wrapper,
                raceId,
                DataType.COMPETITORS,
                dataProcessor,
                context
            )
        }
    }

    @Test
    fun testValidateThrowsOnDuplicateReadoutStarts() {
        val punch1 = Punch()
        punch1.punchType = SIRecordType.START

        val punches = listOf(AliasPunch(punch1, null), AliasPunch(punch1, null))
        val readoutData = ReadoutData(Result(), punches)


        assertThrows(IllegalArgumentException::class.java) {
            DataImportValidator.validateRaceDataReadoutData(
                readoutData,
                raceId,
                context
            )
        }
    }
}
