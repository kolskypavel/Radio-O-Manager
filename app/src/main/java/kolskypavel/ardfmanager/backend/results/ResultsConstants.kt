package kolskypavel.ardfmanager.backend.results

import okhttp3.MediaType.Companion.toMediaType

object ResultsConstants {
    const val RESULTS_LOG_TAG = "RESULT_SERVICE"
    const val ROBIS_API_URL = "https://rob-is.cz/api/results/?name=json"
    const val ROBIS_PLAYGROUND_API_URL = "https://playground.rob-is.cz/api/results/?name=json"
    const val ROBIS_API_HEADER = "Race-Api-Key"
    const val RESULT_DELAY = 3000L // Delay in milliseconds for result processing
    val JSON = "application/json; charset=utf-8".toMediaType()

}