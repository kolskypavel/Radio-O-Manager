package kolskypavel.ardfmanager.backend

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.preference.PreferenceManager
import kolskypavel.ardfmanager.R
import kolskypavel.ardfmanager.backend.files.FileProcessor
import kolskypavel.ardfmanager.backend.files.constants.DataFormat
import kolskypavel.ardfmanager.backend.files.constants.DataType
import kolskypavel.ardfmanager.backend.files.wrappers.DataImportWrapper
import kolskypavel.ardfmanager.backend.helpers.ControlPointsHelper
import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import kolskypavel.ardfmanager.backend.prints.PrintProcessor
import kolskypavel.ardfmanager.backend.results.ResultsProcessor
import kolskypavel.ardfmanager.backend.results.ResultsProcessor.updateResultsForCategory
import kolskypavel.ardfmanager.backend.results.ResultsProcessor.updateResultsForCompetitor
import kolskypavel.ardfmanager.backend.room.ARDFRepository
import kolskypavel.ardfmanager.backend.room.entity.Alias
import kolskypavel.ardfmanager.backend.room.entity.Category
import kolskypavel.ardfmanager.backend.room.entity.Competitor
import kolskypavel.ardfmanager.backend.room.entity.ControlPoint
import kolskypavel.ardfmanager.backend.room.entity.Punch
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.Result
import kolskypavel.ardfmanager.backend.room.entity.ResultService
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CategoryData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.RaceData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ReadoutData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ResultData
import kolskypavel.ardfmanager.backend.room.enums.PunchStatus
import kolskypavel.ardfmanager.backend.room.enums.RaceBand
import kolskypavel.ardfmanager.backend.room.enums.RaceLevel
import kolskypavel.ardfmanager.backend.room.enums.RaceType
import kolskypavel.ardfmanager.backend.room.enums.ResultServiceStatus
import kolskypavel.ardfmanager.backend.room.enums.ResultServiceType
import kolskypavel.ardfmanager.backend.room.enums.ResultStatus
import kolskypavel.ardfmanager.backend.room.enums.StandardCategoryType
import kolskypavel.ardfmanager.backend.sportident.SIPort.CardData
import kolskypavel.ardfmanager.backend.sportident.SIReaderService
import kolskypavel.ardfmanager.backend.sportident.SIReaderState
import kolskypavel.ardfmanager.backend.sportident.SIReaderStatus
import kolskypavel.ardfmanager.backend.wrappers.ResultWrapper
import kolskypavel.ardfmanager.backend.wrappers.StatisticsWrapper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.lang.ref.WeakReference
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID


/**
 * This is the main backend interface, processing and providing various sources of data
 */
class DataProcessor private constructor(context: Context) {

    private val ardfRepository = ARDFRepository.get()
    private var appContext: WeakReference<Context> = WeakReference(context)

    var currentState = MutableLiveData<AppState>()
    var fileProcessor: FileProcessor? = null
    var printProcessor = PrintProcessor(context, this)

    companion object {
        private var INSTANCE: DataProcessor? = null
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = DataProcessor(context)
            }
        }

        fun get(): DataProcessor {
            return INSTANCE ?: throw IllegalStateException("DataProcessor must be initialized")
        }
    }

    init {
        currentState.postValue(
            AppState(null, SIReaderState(SIReaderStatus.DISCONNECTED))
        )
    }

    fun getContext(): Context = appContext.get()!!

    fun getAppVersion(): String {
        val packageInfo =
            appContext.get()!!.packageManager.getPackageInfo(appContext.get()!!.packageName, 0)
        return packageInfo.versionName ?: "Unknown Version"
    }

    @SuppressLint("NullSafeMutableLiveData")
    fun updateReaderState(newSIState: SIReaderState) {
        val stateToUpdate = currentState.value

        if (stateToUpdate != null) {
            stateToUpdate.siReaderState = newSIState
            currentState.postValue(stateToUpdate)
        }
    }

    suspend fun setCurrentRace(raceId: UUID): Race {
        val race = getRace(raceId)
        currentState.postValue(currentState.value?.let { AppState(race, it.siReaderState) })

        return race
    }

    fun removeCurrentRace() {
        currentState.postValue(currentState.value?.let { AppState(null, it.siReaderState, null) })
    }

    //METHODS TO HANDLE RACES
    fun getRaces(): Flow<List<Race>> = ardfRepository.getRaces()

    suspend fun getRace(id: UUID): Race = ardfRepository.getRace(id)

    suspend fun createRace(race: Race) = ardfRepository.createRace(race)

    suspend fun updateRace(race: Race) {
        ardfRepository.updateRace(race)
        updateResults(race.id)
    }

    suspend fun deleteRace(id: UUID) {
        ardfRepository.deleteRace(id)
    }

    //CATEGORIES
    fun getCategoryDataFlowForRace(raceId: UUID) =
        ardfRepository.getCategoryDataFlowForRace(raceId)

    suspend fun getCategory(id: UUID): Category? = ardfRepository.getCategory(id)

    suspend fun getCategoriesForRace(raceId: UUID) = ardfRepository.getCategoriesForRace(raceId)

    suspend fun getCategoryData(id: UUID, raceId: UUID): CategoryData? {
        return ardfRepository.getCategoryData(id, raceId)
    }

    suspend fun getCategoryDataForRace(raceId: UUID): List<CategoryData> =
        ardfRepository.getCategoryDataForRace(raceId)


    suspend fun getCategoryByName(string: String, raceId: UUID): Category? =
        ardfRepository.getCategoryByName(string, raceId)

    private suspend fun getCategoryByBirthYear(
        birthYear: Int,
        isMan: Boolean,
        raceId: UUID
    ): Category? {
        //Calculate the age difference
        val age = LocalDate.now().year - birthYear
        return ardfRepository.getCategoryByBirthYear(age, isMan, raceId)
    }

    suspend fun getStartTimeForCategory(categoryId: UUID): Duration? {
        val competitors = ardfRepository.getCompetitorsByCategory(categoryId)
            .sortedBy { it.drawnRelativeStartTime }

        return if (competitors.isNotEmpty()) {
            competitors.first().drawnRelativeStartTime
        } else null
    }

    suspend fun getHighestCategoryOrder(raceId: UUID) =
        ardfRepository.getHighestCategoryOrder(raceId)

    suspend fun getCategoryByMaxAge(maxAge: Int, isMan: Boolean, raceId: UUID) =
        ardfRepository.getCategoryByMaxAge(maxAge, isMan, raceId)

    suspend fun createOrUpdateCategory(category: Category, controlPoints: List<ControlPoint>?) {
        // Update the control points string
        controlPoints?.let {
            category.controlPointsString = ControlPointsHelper.getStringFromControlPoints(it)
        }
        ardfRepository.createOrUpdateCategory(category, controlPoints)
        updateResultsForCategory(category.id, false, this)
    }

    /**
     * Creates a duplicate of the given category with a suffix "Copy" (or translated)
     * The control points are duplicated as well
     */
    suspend fun duplicateCategory(categoryData: CategoryData) {
        categoryData.category.name += "_" + (appContext.get()?.getString(R.string.general_copy)
            ?: "_Copy")
        categoryData.category.order = getHighestCategoryOrder(categoryData.category.raceId) + 1

        //Adjust the IDs
        categoryData.category.id = UUID.randomUUID()
        for (cp in categoryData.controlPoints) {
            cp.id = UUID.randomUUID()
            cp.categoryId = categoryData.category.id
        }

        createOrUpdateCategory(categoryData.category, categoryData.controlPoints)
    }

    suspend fun createStandardCategories(type: StandardCategoryType, raceId: UUID) {
        val categories = fileProcessor?.importStandardCategories(type, getRace(raceId))
        if (categories != null) {
            for (cat in categories) {
                ardfRepository.createCategory(cat)
            }
        }
    }

    suspend fun deleteCategory(id: UUID, raceId: UUID) {
        ardfRepository.deleteCategory(id)
        ardfRepository.deleteControlPointsByCategory(id)
        updateResultsForCategory(id, true, this)
        updateCategoryOrder(raceId)
    }

    //Updates category order after one is deleted - starts at 0
    private suspend fun updateCategoryOrder(raceId: UUID) {
        val categories = ardfRepository.getCategoriesForRace(raceId)
        for (c in categories.withIndex()) {
            c.value.order = c.index
            ardfRepository.createOrUpdateCategory(c.value, null)
        }
    }

    //CONTROL POINTS
    suspend fun getControlPointsByCategory(categoryId: UUID) =
        ardfRepository.getControlPointsByCategory(categoryId)

    //ALIASES
    suspend fun getAliasesByRace(raceId: UUID) = ardfRepository.getAliasesByRace(raceId)

    suspend fun getControlPointAliasesByCategory(categoryId: UUID) =
        ardfRepository.getControlPointAliasesByCategory(categoryId)

    suspend fun createOrUpdateAliases(aliases: List<Alias>, raceId: UUID) {
        ardfRepository.deleteAliasesByRace(raceId)
        for (alias in aliases) {
            ardfRepository.createOrUpdateAlias(alias)
        }
    }

    //COMPETITORS
    fun getCompetitorDataFlowByRace(raceId: UUID) =
        ardfRepository.getCompetitorDataFlowByRace(raceId)

    suspend fun getCompetitor(id: UUID) = ardfRepository.getCompetitor(id)

    suspend fun getCompetitorBySINumber(siNumber: Int, raceId: UUID): Competitor? =
        ardfRepository.getCompetitorBySINumber(siNumber, raceId)

    suspend fun getCompetitorsByCategory(categoryId: UUID): List<Competitor> =
        ardfRepository.getCompetitorsByCategory(categoryId)

    suspend fun getStatisticsByRace(raceId: UUID): StatisticsWrapper {
        val competitors = ardfRepository.getCompetitorDataFlowByRace(raceId).first()
        val statistics = StatisticsWrapper(competitors.size, 0, 0, 0)
        val race = getRace(raceId)

        for (cd in competitors) {
            val competitor = cd.competitorCategory.competitor
            val category = cd.competitorCategory.category

            if (cd.readoutData == null) {
                if (competitor.drawnRelativeStartTime != null) {
                    //Count started
                    if (TimeProcessor.hasStarted(
                            race.startDateTime,
                            competitor.drawnRelativeStartTime!!,
                            LocalDateTime.now()
                        )
                    ) {
                        statistics.startedCompetitors++
                    }

                    val limit = category?.timeLimit ?: race.timeLimit
                    if (TimeProcessor.isInLimit(
                            race.startDateTime,
                            competitor.drawnRelativeStartTime!!,
                            limit, LocalDateTime.now()
                        )
                    ) {
                        statistics.inLimitCompetitors++
                    }
                }
            } else {
                statistics.startedCompetitors++
                statistics.finishedCompetitors++
            }

        }
        return statistics
    }

    fun checkIfSINumberExists(siNumber: Int, raceId: UUID): Boolean {
        return runBlocking {
            return@runBlocking ardfRepository.checkIfSINumberExists(siNumber, raceId) > 0
        }
    }

    fun checkIfStartNumberExists(startNumber: Int, raceId: UUID): Boolean {
        return runBlocking {
            return@runBlocking ardfRepository.checkIfStartNumberExists(startNumber, raceId) > 0
        }
    }

    suspend fun getHighestStartNumberByRace(raceId: UUID) =
        ardfRepository.getHighestStartNumberByRace(raceId)

    suspend fun createOrUpdateCompetitor(
        competitor: Competitor,
    ) {
        ardfRepository.createCompetitor(competitor)
        ResultsProcessor.updateResultsForCompetitor(competitor.id, this)
    }

    suspend fun deleteCompetitor(id: UUID, deleteResult: Boolean) {
        if (deleteResult) {
            ardfRepository.deleteResultForCompetitor(id)
        }
        ardfRepository.deleteCompetitor(id)
    }

    suspend fun deleteAllCompetitorsByRace(raceId: UUID) {
        ardfRepository.deleteAllCompetitorsByRace(raceId)
    }

    suspend fun addCategoriesAutomatically(raceId: UUID) {
        val competitors = ardfRepository.getCompetitorsByRace(raceId)

        for (comp in competitors) {
            if (comp.categoryId == null && comp.birthYear != null) {
                comp.categoryId = getCategoryByBirthYear(comp.birthYear!!, comp.isMan, raceId)?.id
                createOrUpdateCompetitor(comp)
            }
        }
    }

    //RESULTS
    suspend fun getResult(id: UUID) = ardfRepository.getResult(id)

    suspend fun getResultData(resultId: UUID) = ardfRepository.getResultData(resultId)

    fun getResultDataFlowByRace(raceId: UUID) = ardfRepository.getResultDataFlowByRace(raceId)

    suspend fun getResultByCompetitor(competitorId: UUID) =
        ardfRepository.getResultByCompetitor(competitorId)

    suspend fun getResultBySINumber(siNumber: Int, raceId: UUID) =
        ardfRepository.getResultBySINumber(siNumber, raceId)

    suspend fun saveResultPunches(result: Result, punches: List<Punch>) {
        result.sent = false     // Mark as unsent
        ardfRepository.saveResultPunches(result, punches)
    }

    suspend fun createOrUpdateResult(result: Result) =
        ardfRepository.createOrUpdateResult(result)

    /**
     *     Recalculates all results in a race
     *     Since race edit could mean a change in start time 00, results for each competitor need to be recalculated
     */
    private suspend fun updateResults(raceId: UUID) {
        getCategoriesForRace(raceId).forEach { category ->
            updateResultsForCategory(category.id, false, this)
        }
        ardfRepository.getUnmatchedCompetitorsByRace(raceId)
            .forEach { comp -> updateResultsForCompetitor(comp.id, this) }
    }

    suspend fun deleteResult(id: UUID) = ardfRepository.deleteResult(id)

    suspend fun deleteAllResultsByRace(raceId: UUID) {
        ardfRepository.deleteAllResultsByRace(raceId)
    }

    // Return wherever the "mm:ss" format should be used
    fun useMinuteTimeFormat(): Boolean {
        val context = getContext()
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val preference =
            sharedPref.getString(
                context.getString(R.string.key_results_time_format),
                context.getString(R.string.preferences_results_time_format_minutes)
            )
        return (preference == context.getString(R.string.preferences_results_time_format_minutes))
    }

    //PUNCHES
    suspend fun getPunchesByResult(resultId: UUID) =
        ardfRepository.getPunchesByResult(resultId)

    private suspend fun createPunch(punch: Punch) = ardfRepository.createPunch(punch)

    suspend fun createPunches(punches: ArrayList<Punch>) {
        punches.forEach { punch -> createPunch(punch) }
    }

    suspend fun processCardData(cardData: CardData, race: Race) =
        appContext.get()?.let { ResultsProcessor.processCardData(cardData, race, it, this) }

    suspend fun setAllResultsUnsent(raceId: UUID) =
        ardfRepository.setAllResultsUnsent(raceId)

    //RESULT SERVICE
    fun getResultServiceByRaceId(raceId: UUID) =
        ardfRepository.getResultServiceByRaceId(raceId)

    fun getResultServiceLiveDataWithCountByRaceId(raceId: UUID) =
        ardfRepository.getResultServiceLiveDataWithCountByRaceId(raceId)

    suspend fun createOrUpdateResultService(resultService: ResultService) =
        ardfRepository.createOrUpdateResultService(resultService)

    suspend fun setResultServiceDisabledByRaceId(raceId: UUID) {
        val service = getResultServiceByRaceId(raceId)
        if (service != null) {
            service.enabled = false
            service.status = ResultServiceStatus.DISABLED
            service.errorText = ""
            ardfRepository.createOrUpdateResultService(service)
        }
    }

    fun setResultServiceJob(job: Job) {
        currentState.postValue(currentState.value?.let {
            AppState(
                it.currentRace,
                it.siReaderState,
                job
            )
        })
        currentState.value?.resultServiceJob?.start()
    }

    fun removeResultServiceJob() {
        currentState.value?.resultServiceJob?.cancel()
        currentState.postValue(currentState.value?.let {
            AppState(
                it.currentRace,
                it.siReaderState,
                null
            )
        })
    }

    //DATA IMPORT/EXPORT
    suspend fun importData(
        uri: Uri,
        dataType: DataType,
        dataFormat: DataFormat,
        raceId: UUID
    ): DataImportWrapper {
        val data =
            fileProcessor?.importData(uri, dataType, dataFormat, getRace(raceId), getContext())

        validateDataImport(data!!, raceId, dataType, getContext())
        return data
    }

    @Throws(IllegalArgumentException::class)
    private fun validateDataImport(
        data: DataImportWrapper,
        raceId: UUID,
        dataType: DataType,
        context: Context
    ) {
        when (dataType) {

            //Name is required for each category and must be unique, categories can be empty
            DataType.CATEGORIES -> {
                val names = data.categories.map { it.category.name }

                val duplicates = names.groupingBy { it }.eachCount().filter { it.value > 1 }.keys
                if (duplicates.isNotEmpty()) {
                    throw IllegalArgumentException(
                        context.getString(
                            R.string.data_import_category_duplicate,
                            duplicates.joinToString(", ")
                        )
                    )
                }
            }

            // SI number and start number must be unique
            DataType.COMPETITORS -> {

                //TODO: add check for duplicate names
                val startNumbers = HashSet<Int>()
                val siNumbers = HashSet<Int>()
                for (comp in data.competitorCategories) {
                    val siNumber = comp.competitor.siNumber
                    val startNumber = comp.competitor.startNumber

                    // Check if SI is duplicated in the list or in the database
                    if (siNumber != null) {
                        if (siNumbers.contains(siNumber)
                        ) {
                            throw IllegalArgumentException(
                                context.getString(
                                    R.string.data_import_competitor_duplicate_si_file,
                                    siNumber
                                )
                            )
                        }
                        if (checkIfSINumberExists(siNumber, raceId)) {
                            throw IllegalArgumentException(
                                context.getString(
                                    R.string.data_import_competitor_duplicate_si_race,
                                    siNumber
                                )
                            )
                        }
                    }

                    // Start number checks
                    if (startNumbers.contains(startNumber)
                    ) {
                        throw IllegalArgumentException(
                            context.getString(
                                R.string.data_import_competitor_duplicate_start_number_file,
                                startNumber
                            )
                        )
                    }

                    if (checkIfStartNumberExists(startNumber, raceId)) {
                        throw IllegalArgumentException(
                            context.getString(
                                R.string.data_import_competitor_duplicate_start_number_race,
                                startNumber
                            )
                        )
                    }

                    // Add the numbers to the sets
                    if (siNumber != null) {
                        siNumbers.add(siNumber)
                    }
                    startNumbers.add(startNumber)
                }
            }

            DataType.COMPETITOR_STARTS_TIME -> {
                // TODO: implement - based on settings
            }

            else -> {
                throw IllegalArgumentException(context.getString(R.string.data_import_format_not_supported))
            }
        }
    }

    suspend fun exportData(
        uri: Uri,
        dataType: DataType,
        dataFormat: DataFormat,
        raceId: UUID
    ) =
        fileProcessor?.exportData(
            uri,
            dataType,
            dataFormat,
            raceId
        )

    //-----------------------RACE DATA-----------------------

    suspend fun getRaceData(raceId: UUID): RaceData {
        val race = getRace(raceId)
        val categories = getCategoryDataForRace(raceId)
        val aliases = getAliasesByRace(raceId)
        val competitorData =
            ResultsProcessor.getCompetitorDataByRace(raceId, this)
        val unknownReadoutData =
            getResultDataFlowByRace(raceId).first().filter { it.competitorCategory == null }
                .map { fil -> ReadoutData(fil.result, fil.punches) }

        return RaceData(race, categories, aliases, competitorData, unknownReadoutData)
    }

    suspend fun importRaceData(uri: Uri) {
        val raceData = fileProcessor?.importRaceData(uri)
        if (raceData != null) {
            ardfRepository.saveRaceData(raceData)
        }
    }

    suspend fun exportRaceData(uri: Uri, raceId: UUID) =
        fileProcessor?.exportRaceData(uri, raceId)

    suspend fun saveDataImportWrapper(
        data: DataImportWrapper
    ) {
        //Upsert categories
        for (catData in data.categories) {
            createOrUpdateCategory(
                catData.category,
                catData.controlPoints
            )
        }
        //Create competitors - TODO: ADD duplicates check
        for (compData in data.competitorCategories) {
            createOrUpdateCompetitor(compData.competitor)
        }
    }

    //SportIdent manipulation
    fun connectDevice(usbDevice: UsbDevice) {
        Intent(appContext.get(), SIReaderService::class.java).also {
            it.action = SIReaderService.ReaderServiceActions.START.toString()
            it.putExtra(SIReaderService.USB_DEVICE, usbDevice)
            appContext.get()?.startService(it)
        }
    }

    fun detachDevice(usbDevice: UsbDevice) {
        Intent(appContext.get(), SIReaderService::class.java).also {
            it.action = SIReaderService.ReaderServiceActions.STOP.toString()
            it.putExtra(SIReaderService.USB_DEVICE, usbDevice)
            appContext.get()?.startService(it)
        }
    }

    fun getLastReadCard(): Int? = currentState.value?.siReaderState?.lastCard

    //PRINTING
    fun disablePrinter() {
        printProcessor.disablePrinter()
    }

    fun printFinishTicket(resultData: ResultData, race: Race) =
        printProcessor.printFinishTicket(resultData, race)


    fun printResults(results: List<ResultWrapper>, race: Race) =
        printProcessor.printResults(results, race)

//GENERAL HELPER METHODS

    //Enums manipulation
    fun raceTypeToString(raceType: RaceType): String {
        val raceTypeStrings =
            appContext.get()?.resources?.getStringArray(R.array.race_types_array)!!
        return raceTypeStrings[raceType.value]!!
    }

    fun raceTypeStringToEnum(string: String): RaceType {
        val raceTypeStrings =
            appContext.get()?.resources?.getStringArray(R.array.race_types_array)!!
        return RaceType.getByValue(raceTypeStrings.indexOf(string))
    }

    fun raceLevelToString(raceLevel: RaceLevel): String {
        val raceLevelStrings =
            appContext.get()?.resources?.getStringArray(R.array.race_levels_array)!!
        return raceLevelStrings[raceLevel.value]
    }

    fun raceLevelStringToEnum(string: String): RaceLevel {
        val raceLevelStrings =
            appContext.get()?.resources?.getStringArray(R.array.race_levels_array)!!
        return RaceLevel.getByValue(raceLevelStrings.indexOf(string))
    }

    fun raceBandToString(raceBand: RaceBand): String {
        val raceBandStrings =
            appContext.get()?.resources?.getStringArray(R.array.race_bands_array)!!
        return raceBandStrings[raceBand.value]
    }

    fun raceBandStringToEnum(string: String): RaceBand {
        val raceBandStrings =
            appContext.get()?.resources?.getStringArray(R.array.race_bands_array)!!
        return RaceBand.getByValue(raceBandStrings.indexOf(string))
    }

    fun resultStatusToString(resultStatus: ResultStatus): String {
        val raceStatusStrings =
            appContext.get()?.resources?.getStringArray(R.array.race_status_array)!!
        return raceStatusStrings[resultStatus.value]
    }

    fun resultStatusStringToEnum(string: String): ResultStatus {
        val raceStatusStrings =
            appContext.get()?.resources?.getStringArray(R.array.race_status_array)!!
        return ResultStatus.getByValue(raceStatusStrings.indexOf(string))
    }

    fun resultStatusToShortString(resultStatus: ResultStatus): String {
        val raceStatusStrings =
            appContext.get()?.resources?.getStringArray(R.array.race_status_array_short)!!
        return raceStatusStrings[resultStatus.value]
    }

    fun genderToString(isMan: Boolean?): String {
        return when (isMan) {
            false -> appContext.get()!!.resources.getString(R.string.general_gender_woman)
            else -> appContext.get()!!.resources.getString(R.string.general_gender_man)
        }
    }

    fun resultServiceTypeFromString(string: String): ResultServiceType {
        val raceStatusStrings =
            appContext.get()?.resources?.getStringArray(R.array.result_service_types)!!
        return ResultServiceType.getByValue(raceStatusStrings.indexOf(string))
    }

    fun resultServiceTypeToString(resultServiceType: ResultServiceType): String {
        val resultServiceTypes =
            appContext.get()?.resources?.getStringArray(R.array.result_service_types)!!
        return resultServiceTypes[resultServiceType.value]
    }

    fun resultServiceStatusToString(status: ResultServiceStatus): CharSequence? {
        val resultServiceStatus =
            appContext.get()?.resources?.getStringArray(R.array.result_service_status)!!
        return resultServiceStatus[status.value]
    }

    fun punchStatusToShortString(punchStatus: PunchStatus): String {
        val arr = getContext().resources.getStringArray(R.array.punch_status_array_short)
        return arr[punchStatus.ordinal]
    }

    fun shortStringToPunchStatus(string: String): PunchStatus {
        val punchStatusStrings =
            appContext.get()?.resources?.getStringArray(R.array.punch_status_array_short)!!
        return PunchStatus.getByValue(punchStatusStrings.indexOf(string))
    }

    /**
     * @return false for woman, true for man
     */
    fun genderFromString(string: String): Boolean {
        val genderStrings =
            appContext.get()?.resources?.getStringArray(R.array.genders)!!
        return when (genderStrings.indexOf(string)) {
            0 -> false
            1 -> true
            else -> false
        }
    }

    fun dataFormatFromString(string: String): DataFormat {
        val dataStrings = appContext.get()?.resources?.getStringArray(R.array.data_formats)!!
        val index = dataStrings.indexOf(string).or(0)
        return DataFormat.getByValue(index)!!
    }

    fun dataTypeFromString(string: String): DataType {
        val dataStrings = appContext.get()?.resources?.getStringArray(R.array.data_types)!!
        val index = dataStrings.indexOf(string).or(0)
        return DataType.getByValue(index)!!
    }
}