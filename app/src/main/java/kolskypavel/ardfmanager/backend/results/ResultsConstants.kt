package kolskypavel.ardfmanager.backend.results

import okhttp3.MediaType.Companion.toMediaType

object ResultsConstants {

    // API URLS
    const val ROBIS_API_URL = "https://rob-is.cz/api/results/?name=json"
    const val ROBIS_PLAYGROUND_API_URL = "https://playground.rob-is.cz/api/results/?name=json"
    const val ORESULTS_API_URL =  "https://api.oresults.eu"
    const val OFEED_API_URL =  "https://api.orienteerfeed.com/"

    // API HEADERS
    const val ROBIS_API_HEADER = "Race-Api-Key"
    const val ORESULTS_API_HEADER = "apiKey"

    val CONTENT_TYPE_JSON = "application/json; charset=utf-8".toMediaType()
    val CONTENT_TYPE_XML = "application/xml; charset=utf-8".toMediaType()
    val CONTENT_TYPE_GZIP = "application/gzip; charset=utf-8".toMediaType()

}