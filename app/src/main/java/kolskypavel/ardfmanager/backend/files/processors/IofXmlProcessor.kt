package kolskypavel.ardfmanager.backend.files.processors

import android.content.Context
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.constants.DataFormat
import kolskypavel.ardfmanager.backend.files.constants.DataType
import kolskypavel.ardfmanager.backend.files.wrappers.DataImportWrapper
import kolskypavel.ardfmanager.backend.files.xml.XmlHelper
import kolskypavel.ardfmanager.backend.results.ResultsProcessor
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CategoryData
import kolskypavel.ardfmanager.backend.wrappers.ResultWrapper
import kotlinx.coroutines.flow.first
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import kotlin.collections.emptyList

object IofXmlProcessor : FormatProcessor {

    override suspend fun importData(
        inStream: InputStream,
        dataType: DataType,
        race: Race,
        dataProcessor: DataProcessor,
        stopOnInvalid: Boolean
    ): DataImportWrapper {
        return when (dataType) {
            DataType.CATEGORIES -> importCategories(
                inStream,
                race,
                dataProcessor.getContext()
            )

            else -> {
                TODO()
            }
        }
    }

    override suspend fun exportData(
        outStream: OutputStream,
        dataType: DataType,
        format: DataFormat,
        dataProcessor: DataProcessor,
        race: Race
    ) {
        when (dataType) {
            DataType.COMPETITORS -> TODO()
            DataType.RESULTS -> exportResults(
                outStream,
                race, ResultsProcessor.getResultWrapperFlowByRace(race.id, dataProcessor).first()
                    .filter { it.category != null },
                dataProcessor
            )

            else -> TODO()
        }
    }

    fun importCompetitorData(
        inStream: InputStream,
        race: Race,
        categories: HashSet<CategoryData>
    ): DataImportWrapper {
        // Competitor import not implemented yet; return empty wrapper
        return DataImportWrapper(emptyList(), emptyList(), arrayListOf())
    }

    fun importCategories(
        inStream: InputStream,
        race: Race,
        context: Context
    ): DataImportWrapper {

        val cats = XmlHelper.parseCategories(inStream, race, context)
        return DataImportWrapper(emptyList(), cats, arrayListOf())
    }

    fun exportCategories(
        outStream: OutputStream,
        race: Race,
        dataProcessor: DataProcessor
    ) {
    }

    suspend fun exportStartList(
        outStream: OutputStream,
        race: Race,
        data: List<CategoryData>
    ) {
        var writer: OutputStreamWriter? = null
        try {
            val (serializer, w) = XmlHelper.createSerializer(outStream)
            writer = w

            // Use helper to write root and race
            XmlHelper.writeRootTag(serializer, race, "StartList")

            // Write each category result with helper
            for (res in data) {
                XmlHelper.writeCategoryStartList(serializer, res, race.startDateTime)
            }

            // Finish document
            serializer.endTag(null, "StartList")
            XmlHelper.finishSerializer(serializer, writer)
        } catch (ex: Exception) {
            throw RuntimeException("Failed to export IOF XML startlist: ${ex.message}", ex)
        }
    }

    suspend fun exportResults(
        outStream: OutputStream,
        race: Race,
        results: List<ResultWrapper>,
        dataProcessor: DataProcessor
    ) {
        var writer: OutputStreamWriter? = null
        try {
            val (serializer, w) = XmlHelper.createSerializer(outStream)
            writer = w

            // Use helper to write root and race
            XmlHelper.writeRootTag(serializer, race, "ResultList")

            // Write each category result with helper
            for (res in results) {
                XmlHelper.writeCategoryResult(serializer, res, race.startDateTime)
            }

            // Finish document
            serializer.endTag(null, "ResultList")
            XmlHelper.finishSerializer(serializer, writer)
        } catch (ex: Exception) {
            throw RuntimeException("Failed to export IOF XML: ${ex.message}", ex)
        }
    }
}
