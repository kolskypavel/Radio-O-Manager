package kolskypavel.ardfmanager.backend

import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.sportident.SIReaderState
import kotlinx.coroutines.Job

data class AppState(
    var currentRace: Race? = null,
    var siReaderState: SIReaderState,
    var resultServiceJob: Job? = null
) {}