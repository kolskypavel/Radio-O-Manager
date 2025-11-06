package kolskypavel.ardfmanager.backend.results.workers

import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.ResultService
import okhttp3.OkHttpClient

/**
 * Interface defining a worker class for any result service
 */
interface ResultServiceWorker {

    /**
     * Performs optional actions on result service startup, such as start list upload
     */
    suspend fun init(
        resultService: ResultService,
        race: Race,
        httpClient: OkHttpClient,
        dataProcessor: DataProcessor
    )

    /**;
     * Export results using the provided http client
     * The method is responsible for fetching and updating results in the db, as well as updating the result service status
     */
    suspend fun exportResults(
        resultService: ResultService,
        race: Race,
        httpClient: OkHttpClient,
        dataProcessor: DataProcessor
    )
}