package kolskypavel.ardfmanager.backend.network.workers

import android.content.Context
import android.util.Log
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.processors.IofXmlProcessor
import kolskypavel.ardfmanager.backend.network.NetworkConstants
import kolskypavel.ardfmanager.backend.results.ResultsProcessor
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.ResultService
import kolskypavel.ardfmanager.backend.room.enums.ResultServiceStatus
import kotlinx.coroutines.flow.first
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.time.LocalTime
import java.util.zip.GZIPOutputStream

object OResultsWorker : ResultServiceWorker {

    override suspend fun init(
        resultService: ResultService,
        race: Race,
        httpClient: OkHttpClient,
        dataProcessor: DataProcessor,
        context: Context
    ) {
        if (uploadStartlist(resultService, race, httpClient, dataProcessor, context)) {
            resultService.init = true
        }
    }

    override suspend fun uploadResults(
        final: Boolean,
        resultService: ResultService,
        race: Race,
        httpClient: OkHttpClient,
        dataProcessor: DataProcessor,
        context: Context
    ): Boolean {

        val results =
            ResultsProcessor.getResultWrapperFlowByRace(resultService.raceId, dataProcessor)
                .first()
                .filter { it.category != null }

        val stream = ByteArrayOutputStream()
        IofXmlProcessor.exportResults(stream, race, results, dataProcessor,final)
        val xml = stream.toString()

        try {
            if (sendFile(xml, resultService, httpClient, "/results")) {
                resultService.status = ResultServiceStatus.RUNNING
                resultService.sentAt = LocalTime.now()
                return true
            } else {
                resultService.status = ResultServiceStatus.ERROR
            }

        } catch (exception: Exception) {
            // Handle exceptions during the request
            resultService.status = ResultServiceStatus.ERROR
            resultService.errorText = exception.message ?: "Unknown error"
            Log.e(LOG_TAG, "Exception sending : ${exception.message}")
        }
        return false
    }

    override suspend fun uploadStartlist(
        resultService: ResultService,
        race: Race,
        httpClient: OkHttpClient,
        dataProcessor: DataProcessor,
        context: Context
    ): Boolean {
        resultService.status = ResultServiceStatus.RUNNING
        return try {
            val stream = ByteArrayOutputStream()
            val data = dataProcessor.getCategoryDataFlowForRace(race.id).first()
            IofXmlProcessor.exportStartList(stream, race, data, dataProcessor)

            val xml = stream.toString()
            if (sendFile(xml, resultService, httpClient, "/start-lists")) {
                true
            } else {
                resultService.status = ResultServiceStatus.ERROR
                false
            }
        } catch (e: Exception) {
            Log.e(LOG_TAG, "Exception when uploadStartlist: ${e.message}")
            resultService.status = ResultServiceStatus.ERROR
            false
        }
    }

    @Throws(Exception::class)
    fun sendFile(
        data: String,
        resultService: ResultService,
        httpClient: OkHttpClient,
        path: String
    ): Boolean {
        // Compress data with gzip
        val compressed = gzipStringToByteArray(data)

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(NetworkConstants.ORESULTS_API_HEADER, resultService.apiKey)
            .addFormDataPart("Content-Encoding", "application/gzip")
            .addFormDataPart(
                "file",
                null,
                compressed.toRequestBody(NetworkConstants.CONTENT_TYPE_GZIP)
            ).build()

        val request = Request.Builder()
            .url(NetworkConstants.ORESULTS_RESULTS_API_URL + path)
            .post(multipartBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                return true
            } else {
                Log.e(
                    LOG_TAG,
                    "Error sending file, code ${response.code}, message ${response.message}"
                )
                return false
            }
        }
    }

    // Helper to gzip a String to ByteArray using UTF-8
    fun gzipStringToByteArray(input: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { gzip ->
            gzip.write(input.toByteArray(Charsets.UTF_8))
            gzip.finish()
        }
        return bos.toByteArray()
    }

    const val LOG_TAG = "SERVICE ORESULTS"
}
