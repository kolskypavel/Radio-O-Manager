package kolskypavel.ardfmanager.backend.results

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kolskypavel.ardfmanager.R
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.json.temps.RobisResultJson
import kolskypavel.ardfmanager.backend.files.processors.JsonProcessor
import kolskypavel.ardfmanager.backend.results.ResultsConstants.JSON
import kolskypavel.ardfmanager.backend.results.ResultsConstants.RESULTS_LOG_TAG
import kolskypavel.ardfmanager.backend.results.ResultsConstants.ROBIS_API_HEADER
import kolskypavel.ardfmanager.backend.room.entity.ResultService
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.room.enums.ResultServiceStatus
import kolskypavel.ardfmanager.backend.room.enums.ResultServiceType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.UUID

object ResultServiceWorker {

    fun resultServiceJob(
        raceId: UUID,
        dataProcessor: DataProcessor,
        context: Context
    ): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            val httpClient = OkHttpClient.Builder().build()
            var resultService: ResultService?

            while (true) {

                // Get the result service from db
                resultService = dataProcessor.getResultServiceByRaceId(raceId)
                if (resultService != null) {
                    //Test connection before sending - TODO: fix
                    if (!isNetworkConnected(context)) {
                        resultService.status = ResultServiceStatus.NO_NETWORK
                        updateResultService(dataProcessor, resultService)
                        continue
                    }

                    when (resultService.serviceType) {
                        ResultServiceType.ROBIS, ResultServiceType.ROBIS_TEST -> exportResultsRobis(
                            dataProcessor,
                            resultService,
                            httpClient,
                            resultService.serviceType == ResultServiceType.ROBIS_TEST
                        )
                    }
                }
                delay(ResultsConstants.RESULT_DELAY)
            }
        }
    }

    private suspend fun exportResultsRobis(
        dataProcessor: DataProcessor,
        resultService: ResultService,
        httpClient: OkHttpClient,
        test: Boolean
    ) {
        Log.i(RESULTS_LOG_TAG, ">> exportResultsRobis START")


        // Fetch results and convert them to JSON
        val filteredResults = filterResultDataBySent(
            ResultsProcessor.getCompetitorDataByRace(
                resultService.raceId,
                dataProcessor
            )
        )

        // If there are no results to send, return early
        if (filteredResults.isEmpty()) {
            Log.i(RESULTS_LOG_TAG, "  nothing to send, exiting")
            return
        }

        val outStream = ByteArrayOutputStream()
        JsonProcessor.exportResults(outStream, filteredResults, resultService.raceId)
        val resultString = outStream.toString("UTF-8")
        Log.i(RESULTS_LOG_TAG, "Export JSON payload:\n$resultString")
        val body: RequestBody = resultString.toRequestBody(JSON)

        // Send the results to the ROBIS API
        val request: Request = Request.Builder()
            .url(
                if (test) {
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
                val bodyString = response.body.string() ?: ""

                Log.i(RESULTS_LOG_TAG, "ROBIS response code=${response.code}, body=$bodyString")

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
                            RESULTS_LOG_TAG,
                            "Error ${response.code} sending results to ROBis: ${response.message}"
                        )
                    }

                    else -> {
                        // Handle error response and log it
                        resultService.status = ResultServiceStatus.ERROR
                        resultService.errorText = bodyString
                        Log.e(
                            RESULTS_LOG_TAG,
                            "Error ${response.code} sending results to ROBis: ${response.message}"
                        )
                    }
                }
            }
        } catch (exception: Exception) {
            // Handle exceptions during the request
            resultService.status = ResultServiceStatus.ERROR
            resultService.errorText = exception.message ?: "Unknown error"
            Log.e(RESULTS_LOG_TAG, "Exception sending results to ROBis: ${exception.message}")
        }
        updateResultService(dataProcessor, resultService)
    }

    private fun isNetworkConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        // Require both that the network advertises internet and that the system validated it.
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // Filter the results by sent
    private fun filterResultDataBySent(
        results: List<CompetitorData>
    ): ArrayList<CompetitorData> {
        val filtered = ArrayList<CompetitorData>()
        for (cd in results) {
            if (cd.readoutData != null && !cd.readoutData!!.result.sent) {
                filtered.add(cd)
            }
        }
        return filtered
    }

    /** Find the invalid result and remove it from the array list
     * First try to match via index, then with SI number and then matching last and first name
     */
    private fun findAndRemoveMatchingResult(
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

    // Mark the results as sent
    private fun updateSentResults(
        dataProcessor: DataProcessor,
        resultData: List<CompetitorData>
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            for (r in resultData) {
                val result = r.readoutData?.result
                if (result != null) {
                    result.sent = true
                    dataProcessor.createOrUpdateResult(result)
                }
            }
        }
    }

    private fun updateResultService(
        dataProcessor: DataProcessor,
        resultService: ResultService
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            dataProcessor.createOrUpdateResultService(resultService)
        }
    }
}