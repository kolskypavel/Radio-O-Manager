package kolskypavel.ardfmanager.ui.races

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.room.entity.Race
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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

    init {
        viewModelScope.launch {
            dataProcessor.getRaces().collect { races ->
                _races.value = races.sortedBy { it.startDateTime }
            }
        }
    }
}