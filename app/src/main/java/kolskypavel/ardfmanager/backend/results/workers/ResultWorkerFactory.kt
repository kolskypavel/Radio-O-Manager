package kolskypavel.ardfmanager.backend.results.workers

import kolskypavel.ardfmanager.backend.room.enums.ResultServiceType

object ResultWorkerFactory {
    fun getResultWorker(type: ResultServiceType): ResultServiceWorker {
        return when (type) {
            ResultServiceType.ROBIS, ResultServiceType.ROBIS_TEST -> RobisWorker
            ResultServiceType.ORESULTS -> OResultsWorker
            ResultServiceType.OFEED -> OFeedWorker
        }
    }
}