package kolskypavel.ardfmanager.backend.results

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.results.workers.ResultWorkerFactory
import kolskypavel.ardfmanager.backend.room.entity.ResultService
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.room.enums.ResultServiceStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import okhttp3.OkHttpClient
import java.util.UUID

/**
 * Main result service worker - executes
 */
object ResultServiceProcessor {

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
                    dataProcessor.getRace(raceId)?.let { race ->
                        val worker = ResultWorkerFactory.getResultWorker(resultService.serviceType)

                        // Init the service
                        if (resultService.status == ResultServiceStatus.DISABLED) {
                            worker.init(
                                resultService,
                                race,
                                httpClient,
                                dataProcessor
                            )
                        }

                        // Redo the check to prevent additional waiting
                        if (resultService.status != ResultServiceStatus.DISABLED) {
                            // Main result sending
                            worker.exportResults(
                                resultService,
                                race,
                                httpClient,
                                dataProcessor
                            )
                        }
                    }
                    updateResultService(dataProcessor, resultService)
                    delay(resultService.interval)
                } else {
                    delay(1000)     // Failsafe - should never occur
                }

            }
        }
    }

    private fun isNetworkConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        // Require both that the network advertises internet and that the system validated it.
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }


    fun filterCompetitorDataBySent(
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

    // Mark the results as sent
    fun updateSentResults(
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