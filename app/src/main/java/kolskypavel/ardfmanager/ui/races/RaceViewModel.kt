package kolskypavel.ardfmanager.ui.races

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.RaceData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.UUID

class RaceViewModel : ViewModel() {
    private val dataProcessor = DataProcessor.get()
    private val _races: MutableStateFlow<List<Race>> = MutableStateFlow(emptyList())
    val races: StateFlow<List<Race>> get() = _races.asStateFlow()


    fun createRace(
        race: Race
    ) = CoroutineScope(Dispatchers.IO).launch { dataProcessor.createRace(race) }

    fun updateRace(
        race: Race
    ) = CoroutineScope(Dispatchers.IO).launch { dataProcessor.updateRace(race) }

    fun deleteRace(id: UUID) {
        CoroutineScope(Dispatchers.IO).launch {
            dataProcessor.deleteRace(id)
        }
    }

    fun saveRaceData(raceData: RaceData) = CoroutineScope(Dispatchers.IO).launch {
        dataProcessor.saveRaceData(raceData)
    }

    fun importRaceData(
        uri: Uri
    ) = runBlocking {
        dataProcessor.importRaceData(uri)
    }

    fun exportRaceData(
        uri: Uri, raceId: UUID
    ) = runBlocking {
        dataProcessor.exportRaceData(uri, raceId)
    }

    init {
        viewModelScope.launch {
            dataProcessor.getRaces().collect { races ->
                _races.value = races.sortedBy { it.startDateTime }
            }
        }
    }
}