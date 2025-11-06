package kolskypavel.ardfmanager.backend.results.workers

import android.util.Log
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.processors.IofXmlProcessor
import kolskypavel.ardfmanager.backend.results.ResultsConstants
import kolskypavel.ardfmanager.backend.results.ResultsProcessor
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.ResultService
import kolskypavel.ardfmanager.backend.room.enums.ResultServiceStatus
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

object OResultsWorker : ResultServiceWorker {

    override suspend fun init(
        resultService: ResultService,
        race: Race,
        httpClient: OkHttpClient,
        dataProcessor: DataProcessor
    ) {
        try {
            val stream = ByteArrayOutputStream()
            val data = dataProcessor.getCategoryDataFlowForRace(race.id).first()
            IofXmlProcessor.exportStartList(stream, race, data)

            val xml = stream.toString()
            if (sendFile(xml, resultService, httpClient)) {
                resultService.status = ResultServiceStatus.RUNNING
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Exception when init: ${e.message}")
        }
    }

    override suspend fun exportResults(
        resultService: ResultService,
        race: Race,
        httpClient: OkHttpClient,
        dataProcessor: DataProcessor
    ) {
        if (resultService.status == ResultServiceStatus.INIT) {
            init(resultService, race, httpClient, dataProcessor)
        } else {
            val results =
                ResultsProcessor.getResultWrapperFlowByRace(resultService.raceId, dataProcessor)
                    .first()
                    .filter { it.category != null }

            val stream = ByteArrayOutputStream()
            IofXmlProcessor.exportResults(stream, race, results, dataProcessor)
            val xml = stream.toString()

            try {
                if (sendFile(xml, resultService, httpClient)) {
                    resultService.status = ResultServiceStatus.RUNNING
                }

            } catch (exception: Exception) {
                // Handle exceptions during the request
                resultService.status = ResultServiceStatus.ERROR
                resultService.errorText = exception.message ?: "Unknown error"
                Log.e(LOG_TAG, "Exception sending : ${exception.message}")
            }
        }
    }

    @Throws(Exception::class)
    fun sendFile(data: String, resultService: ResultService, httpClient: OkHttpClient): Boolean {
        val body = data.toRequestBody(ResultsConstants.CONTENT_TYPE_XML)
        val request = Request.Builder()
            .url(ResultsConstants.ORESULTS_API_URL)
            .addHeader(ResultsConstants.ORESULTS_API_HEADER, resultService.apiKey)
            .put(body)
            .build()

        httpClient.newCall(request).execute().use { response ->
            return response.isSuccessful
        }
    }

    const val LOG_TAG = "SERVICE ORESULTS"
}