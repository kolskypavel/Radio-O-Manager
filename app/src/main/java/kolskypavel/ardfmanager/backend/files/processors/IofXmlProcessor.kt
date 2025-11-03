package kolskypavel.ardfmanager.backend.files.processors

import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.constants.DataFormat
import kolskypavel.ardfmanager.backend.files.constants.DataType
import kolskypavel.ardfmanager.backend.files.wrappers.DataImportWrapper
import kolskypavel.ardfmanager.backend.files.xml.XmlHelper
import kolskypavel.ardfmanager.backend.results.ResultsProcessor
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CategoryData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.wrappers.ResultWrapper
import kotlinx.coroutines.flow.first
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.util.UUID

object IofXmlProcessor : FormatProcessor {

    override suspend fun importData(
        inStream: InputStream,
        dataType: DataType,
        race: Race,
        dataProcessor: DataProcessor,
        stopOnInvalid: Boolean
    ): DataImportWrapper {
        TODO("Not yet implemented")
    }

    override suspend fun exportData(
        outStream: OutputStream,
        dataType: DataType,
        format: DataFormat,
        dataProcessor: DataProcessor,
        raceId: UUID
    ) {
        when (dataType) {
            DataType.COMPETITORS -> TODO()
            DataType.RESULTS -> exportResults(
                outStream,
                raceId, ResultsProcessor.getResultWrapperFlowByRace(raceId, dataProcessor).first()
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
        TODO("Not yet implemented")
    }

    fun importCategories() {

    }

    fun exportCategories(
        outStream: OutputStream,
        race: Race,
        dataProcessor: DataProcessor
    ) {
    }

    suspend fun exportResults(
        outStream: OutputStream,
        raceId: UUID,
        results: List<ResultWrapper>,
        dataProcessor: DataProcessor
    ) {
        var writer: OutputStreamWriter? = null
        try {
            val (serializer, w) = XmlHelper.createSerializer(outStream)
            writer = w

            val race = dataProcessor.getRace(raceId)!!

            // Use helper to write root and race
            XmlHelper.writeResultListRoot(serializer, race)

            // Write each category result with helper
            for (res in results) {
                XmlHelper.writeCategoryResult(serializer, res,race.startDateTime)
            }

            // Finish document
            XmlHelper.finishSerializer(serializer, writer)
        } catch (ex: Exception) {
            throw RuntimeException("Failed to export IOF XML: ${ex.message}", ex)
        }
    }
}
