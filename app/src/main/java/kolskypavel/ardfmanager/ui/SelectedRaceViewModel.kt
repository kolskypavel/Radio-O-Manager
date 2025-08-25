package kolskypavel.ardfmanager.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.preference.PreferenceManager
import kolskypavel.ardfmanager.R
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.constants.DataFormat
import kolskypavel.ardfmanager.backend.files.constants.DataType
import kolskypavel.ardfmanager.backend.files.wrappers.DataImportWrapper
import kolskypavel.ardfmanager.backend.results.ResultsProcessor
import kolskypavel.ardfmanager.backend.room.entity.Alias
import kolskypavel.ardfmanager.backend.room.entity.Category
import kolskypavel.ardfmanager.backend.room.entity.Competitor
import kolskypavel.ardfmanager.backend.room.entity.ControlPoint
import kolskypavel.ardfmanager.backend.room.entity.Punch
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.Result
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CategoryData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ResultData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ResultServiceData
import kolskypavel.ardfmanager.backend.room.enums.ResultStatus
import kolskypavel.ardfmanager.backend.room.enums.StandardCategoryType
import kolskypavel.ardfmanager.backend.wrappers.ResultWrapper
import kolskypavel.ardfmanager.backend.wrappers.StatisticsWrapper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale
import java.util.UUID

/**
 * Represents the current selected race and its data - properties, categories, competitors and readouts
 */
class SelectedRaceViewModel : ViewModel() {
    private val dataProcessor = DataProcessor.get()
    private val _race = MutableLiveData<Race>()

    val race: LiveData<Race> get() = _race
    private val _categories: MutableStateFlow<List<CategoryData>> = MutableStateFlow(emptyList())
    val categories: StateFlow<List<CategoryData>> get() = _categories.asStateFlow()

    private val _competitorData: MutableStateFlow<List<CompetitorData>> =
        MutableStateFlow(emptyList())
    val competitorData: StateFlow<List<CompetitorData>>
        get() = _competitorData.asStateFlow()

    private val _readoutData: MutableStateFlow<List<ResultData>> = MutableStateFlow(emptyList())
    val readoutData: StateFlow<List<ResultData>> get() = _readoutData.asStateFlow()

    private val _resultWrappers: MutableStateFlow<List<ResultWrapper>> =
        MutableStateFlow(emptyList())
    val resultWrappers: StateFlow<List<ResultWrapper>> get() = _resultWrappers.asStateFlow()

    var resultService: LiveData<ResultServiceData> = MutableLiveData(null)

    @Throws(IllegalStateException::class)
    fun getCurrentRace(): Race {
        if (race.value != null) {
            return race.value!!
        }
        throw IllegalStateException("Race value accessed without race set")
    }

    /**
     * Updates the current selected race and corresponding data
     */
    fun setRace(id: UUID) {
        CoroutineScope(Dispatchers.IO).launch {
            val race = dataProcessor.setCurrentRace(id)
            _race.postValue(race)

            launch {
                dataProcessor.getCategoryDataFlowForRace(id).collect {
                    _categories.value = it
                }
            }
            launch {
                dataProcessor.getCompetitorDataFlowByRace(id).collect {
                    _competitorData.value = it
                }
            }
            launch {
                dataProcessor.getResultDataFlowByRace(id).collect {
                    _readoutData.value = it
                }
            }

            launch {
                ResultsProcessor.getResultWrapperFlowByRace(id, dataProcessor).collect {
                    _resultWrappers.value = it
                }
            }
        }

        // Start result service again
        CoroutineScope(Dispatchers.IO).launch { dataProcessor.setResultServiceDisabledByRaceId(id) }
        resultService = dataProcessor.getResultServiceLiveDataWithCountByRaceId(id)

    }

    fun updateRace(race: Race) {
        CoroutineScope(Dispatchers.IO).launch {
            dataProcessor.updateRace(race)
            _race.postValue(race)
        }
    }

    fun removeReaderRace() = dataProcessor.removeCurrentRace()

    // get current locale
    fun getCurrentLocale(context: Context): Locale {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val preference = sharedPref.getString(context.getString(R.string.key_app_language), "en")

        return Locale(preference)
    }

    //Category
    suspend fun getCategory(id: UUID) = dataProcessor.getCategory(id)

    fun getCategories(): List<Category> = categories.value.map { it.category }
    fun getCategoryByName(string: String): Category? {
        return runBlocking {
            return@runBlocking dataProcessor.getCategoryByName(string, getCurrentRace().id)
        }
    }

    fun getCategoryByMaxAge(maxAge: Int): Category? {
        return runBlocking {
            return@runBlocking dataProcessor.getCategoryByMaxAge(maxAge, getCurrentRace().id)
        }
    }

    fun getHighestCategoryOrder(raceId: UUID): Int {
        return runBlocking {
            return@runBlocking dataProcessor.getHighestCategoryOrder(raceId)
        }
    }

    fun createOrUpdateCategory(category: Category, controlPoints: List<ControlPoint>?) =
        CoroutineScope(Dispatchers.IO).launch {
            dataProcessor.createOrUpdateCategory(category, controlPoints)
        }

    fun duplicateCategory(categoryData: CategoryData) {
        CoroutineScope(Dispatchers.IO).launch {
            dataProcessor.duplicateCategory(categoryData)
        }
    }

    fun createStandardCategories(type: StandardCategoryType) {
        CoroutineScope(Dispatchers.IO).launch {
            dataProcessor.createStandardCategories(type, getCurrentRace().id)
        }
    }

    fun deleteCategory(categoryId: UUID) = CoroutineScope(Dispatchers.IO).launch {
        dataProcessor.deleteCategory(
            categoryId, getCurrentRace().id
        )
    }


    fun getControlPointsByCategory(categoryId: UUID): ArrayList<ControlPoint> {
        return runBlocking {
            ArrayList(dataProcessor.getControlPointsByCategory(categoryId))
        }
    }

    //Alias
    fun getAliasesByRace() = runBlocking { dataProcessor.getAliasesByRace(getCurrentRace().id) }

    fun createOrUpdateAliases(aliases: List<Alias>) {
        CoroutineScope(Dispatchers.IO).launch {
            dataProcessor.createOrUpdateAliases(aliases, getCurrentRace().id)
        }
    }

    //Competitor
    fun getCompetitors(): List<Competitor> =
        competitorData.value.map { it.competitorCategory.competitor }

    fun getCompetitor(id: UUID) = runBlocking {
        return@runBlocking dataProcessor.getCompetitor(id)
    }

    fun createOrUpdateCompetitor(competitor: Competitor) = CoroutineScope(Dispatchers.IO).launch {
        dataProcessor.createOrUpdateCompetitor(competitor)
    }

    fun deleteCompetitor(competitorId: UUID, deleteResult: Boolean) =
        CoroutineScope(Dispatchers.IO).launch {
            dataProcessor.deleteCompetitor(
                competitorId, deleteResult
            )
        }

    fun deleteAllCompetitorsByRace() = CoroutineScope(Dispatchers.IO).launch {
        race.value?.let {
            dataProcessor.deleteAllCompetitorsByRace(
                it.id
            )
        }
    }

    fun addCategoriesAutomatically() {
        CoroutineScope(Dispatchers.IO).launch {
            dataProcessor.addCategoriesAutomatically(getCurrentRace().id)
        }
    }

    suspend fun getStatistics(): StatisticsWrapper =
        dataProcessor.getStatisticsByRace(getCurrentRace().id)

    /**
     * Checks if the SI number is unique
     */
    fun checkIfSINumberExists(siNumber: Int): Boolean {
        if (race.value != null) {
            return dataProcessor.checkIfSINumberExists(siNumber, race.value!!.id)
        }
        return true
    }

    fun checkIfStartNumberExists(siNumber: Int): Boolean {
        if (race.value != null) {
            return dataProcessor.checkIfStartNumberExists(siNumber, race.value!!.id)
        }
        return true
    }

    fun getLastReadCard() = dataProcessor.getLastReadCard()

    suspend fun processManualPunchData(
        result: Result, punches: ArrayList<Punch>, manualStatus: ResultStatus?, modified: Boolean
    ) {
        ResultsProcessor.processManualPunchData(
            result,
            punches,
            manualStatus,
            DataProcessor.get(),
            modified
        )
    }

    fun getResultData(id: UUID): ResultData {
        return runBlocking {
            return@runBlocking dataProcessor.getResultData(id)
        }
    }

    fun getResultBySINumber(siNumber: Int) = runBlocking {
        return@runBlocking dataProcessor.getResultBySINumber(siNumber, getCurrentRace().id)
    }

    fun getResultByCompetitor(competitorId: UUID) = runBlocking {
        return@runBlocking dataProcessor.getResultByCompetitor(competitorId)
    }

    fun deleteResult(id: UUID) {
        CoroutineScope(Dispatchers.IO).launch {
            dataProcessor.deleteResult(id)
        }
    }

    fun deleteAllResultsByRace() = CoroutineScope(Dispatchers.IO).launch {
        race.value?.let {
            dataProcessor.deleteAllResultsByRace(
                it.id
            )
        }
    }

    //RESULT SERVICE

    fun disableResultService() {
        CoroutineScope(Dispatchers.IO).launch {
            dataProcessor.removeResultServiceJob()
            race.value?.let { dataProcessor.setResultServiceDisabledByRaceId(it.id) }
        }
    }

    fun setAllResultsUnsent() {
        CoroutineScope(Dispatchers.IO).launch {
            dataProcessor.setAllResultsUnsent(getCurrentRace().id)
        }
    }

    //DATA IMPORT/EXPORT
    suspend fun importData(
        uri: Uri, dataType: DataType, dataFormat: DataFormat
    ): DataImportWrapper {
        return dataProcessor.importData(
            uri, dataType, dataFormat, getCurrentRace().id
        )
    }

    fun importRaceData(
        uri: Uri
    ) = runBlocking {
        dataProcessor.importRaceData(uri)
    }

    fun exportData(
        uri: Uri, dataType: DataType, dataFormat: DataFormat
    ) = runBlocking {
        dataProcessor.exportData(
            uri, dataType, dataFormat, getCurrentRace().id
        )
    }

    fun exportRaceData(
        uri: Uri, raceId: UUID
    ) = runBlocking {
        dataProcessor.exportRaceData(uri, raceId)
    }


    fun saveDataImportWrapper(
        dataImportWrapper: DataImportWrapper
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            dataProcessor.saveDataImportWrapper(dataImportWrapper)
        }
    }
}