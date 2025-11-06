package kolskypavel.ardfmanager.backend.results.workers

import android.content.Context
import android.util.Log
import kolskypavel.ardfmanager.R
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.json.temps.RobisResultJson
import kolskypavel.ardfmanager.backend.files.processors.JsonProcessor
import kolskypavel.ardfmanager.backend.results.ResultServiceProcessor
import kolskypavel.ardfmanager.backend.results.ResultServiceProcessor.updateSentResults
import kolskypavel.ardfmanager.backend.results.ResultsConstants
import kolskypavel.ardfmanager.backend.results.ResultsConstants.CONTENT_TYPE_JSON
import kolskypavel.ardfmanager.backend.results.ResultsConstants.ROBIS_API_HEADER
import kolskypavel.ardfmanager.backend.results.ResultsProcessor
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.ResultService
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.room.enums.ResultServiceStatus
import kolskypavel.ardfmanager.backend.room.enums.ResultServiceType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream

object RobisWorker : ResultServiceWorker {

    // No init actions needed
    override suspend fun init(
        resultService: ResultService,
        race: Race,
        httpClient: OkHttpClient,
        dataProcessor: DataProcessor
    ) {
    }

    override suspend fun exportResults(
        resultService: ResultService,
        race: Race,
        httpClient: OkHttpClient,
        dataProcessor: DataProcessor
    ) {
        Log.i(LOG_TAG, "Starting to export results")

        // Fetch results and convert them to JSON
        val filteredResults = ResultServiceProcessor.filterCompetitorDataBySent(
            ResultsProcessor.getCompetitorDataByRace(
                resultService.raceId,
                dataProcessor
            )
        )

        // If there are no results to send, return early
        if (filteredResults.isEmpty()) {
            Log.i(LOG_TAG, "  nothing to send, exiting")
            return
        }

        val outStream = ByteArrayOutputStream()
        JsonProcessor.exportResults(outStream, filteredResults, race, dataProcessor)
        val resultString = outStream.toString("UTF-8")
        Log.i(LOG_TAG, "Export JSON payload:\n$resultString")
        val body: RequestBody = resultString.toRequestBody(CONTENT_TYPE_JSON)

        // Send the results to the ROBIS API
        val request: Request = Request.Builder()
            .url(
                if (resultService.serviceType == ResultServiceType.ROBIS_TEST) {
                    ResultsConstants.ROBIS_PLAYGROUND_API_URL
                } else {
                    ResultsConstants.ROBIS_API_URL
                }
            )
            .addHeader(ROBIS_API_HEADER, resultService.apiKey)
            .put(body)
            .build()


        try {
            //TODO: Handle loging
            httpClient.newCall(request).execute().use { response ->
                val bodyString = response.body.string()

                Log.i(LOG_TAG, "ROBIS response code=${response.code}, body=$bodyString")

                when (response.code) {
                    in 200..299 -> {

                        filterInvalidResults(
                            filteredResults,
                            bodyString,
                            resultService,
                            dataProcessor.getContext()
                        )
                        updateSentResults(dataProcessor, filteredResults)
                        resultService.status = ResultServiceStatus.RUNNING
                        resultService.sent += filteredResults.size
                    }

                    401 -> {
                        // Handle unauthorized response
                        resultService.status = ResultServiceStatus.UNAUTHORIZED
                        resultService.errorText = response.message

                        Log.e(
                            LOG_TAG,
                            "Error ${response.code} sending results to ROBis: ${response.message}"
                        )
                    }

                    else -> {
                        // Handle error response and log it
                        resultService.status = ResultServiceStatus.ERROR
                        resultService.errorText = bodyString
                        Log.e(
                            LOG_TAG,
                            "Error ${response.code} sending results to ROBis: ${response.message}"
                        )
                    }
                }
            }
        } catch (exception: Exception) {
            // Handle exceptions during the request
            resultService.status = ResultServiceStatus.ERROR
            resultService.errorText = exception.message ?: "Unknown error"
            Log.e(LOG_TAG, "Exception sending results to ROBis: ${exception.message}")
        }
    }

    // Filter the invalid results that were sent to ROBIS and inform about the errors
    private fun filterInvalidResults(
        results: ArrayList<CompetitorData>,
        robisResponse: String,
        resultService: ResultService,
        context: Context
    ) {
        val response = JsonProcessor.parseRobisResponse(robisResponse)

        if (response != null) {

            var invalidString = ""
            for (invalid in response.invalid_data) {
                findAndRemoveMatchingResult(results, invalid)
                val fullName = "${invalid.last_name.uppercase()} ${invalid.first_name}"
                invalidString += context.getString(
                    R.string.result_service_invalid_result,
                    fullName,
                    invalid.si_number,
                    invalid.competitor_index,
                    invalid.reason
                )
                invalidString += "\n"
            }
            resultService.errorText = invalidString
        }
    }

    /** Find the invalid result and remove it from the array list
     * First try to match via index, then with SI number and then matching last and first name
     */
    fun findAndRemoveMatchingResult(
        results: ArrayList<CompetitorData>,
        response: RobisResultJson
    ) {
        val found = results.find {
            it.competitorCategory.competitor.index == response.competitor_index
        } ?: results.find {
            it.readoutData?.result?.siNumber == response.si_number
        } ?: results.find {
            it.competitorCategory.competitor.firstName == response.first_name &&
                    it.competitorCategory.competitor.lastName == response.last_name
        }
        results.remove(found)
    }

    const val LOG_TAG = "SERVICE ROBIS"
}