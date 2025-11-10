package kolskypavel.ardfmanager.backend.results.workers

import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.ResultService
import okhttp3.OkHttpClient

object OFeedWorker: ResultServiceWorker {
    override suspend fun init(
        resultService: ResultService,
        race: Race,
        httpClient: OkHttpClient,
        dataProcessor: DataProcessor
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun exportResults(
        resultService: ResultService,
        race: Race,
        httpClient: OkHttpClient,
        dataProcessor: DataProcessor
    ) {
        TODO("Not yet implemented")
    }

    const val LOG_TAG = "SERVICE OFEED"
}