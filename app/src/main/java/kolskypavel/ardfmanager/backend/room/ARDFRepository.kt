package kolskypavel.ardfmanager.backend.room

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import kolskypavel.ardfmanager.backend.room.database.EventDatabase
import kolskypavel.ardfmanager.backend.room.entity.Alias
import kolskypavel.ardfmanager.backend.room.entity.Category
import kolskypavel.ardfmanager.backend.room.entity.Competitor
import kolskypavel.ardfmanager.backend.room.entity.ControlPoint
import kolskypavel.ardfmanager.backend.room.entity.Punch
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.Result
import kolskypavel.ardfmanager.backend.room.entity.ResultService
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.RaceData
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ARDFRepository private constructor(context: Context) {

    private val eventDatabase: EventDatabase = Room
        .databaseBuilder(
            context.applicationContext,
            EventDatabase::class.java,
            "event-database"
        )
        .build()

    //-------------------Races-------------------
    fun getRaces(): Flow<List<Race>> = eventDatabase.raceDao().getRaces()
    suspend fun getRace(id: UUID): Race = eventDatabase.raceDao().getRace(id)
    suspend fun createRace(race: Race) = eventDatabase.raceDao().createRace(race)
    suspend fun updateRace(race: Race) = eventDatabase.raceDao().updateRace(race)
    suspend fun deleteRace(id: UUID) = eventDatabase.raceDao().deleteRace(id)

    //-------------------Categories-------------------
    fun getCategoryDataFlowForRace(raceId: UUID) =
        eventDatabase.categoryDao().getCategoryFlowForRace(raceId)

    suspend fun getCategoriesForRace(raceId: UUID): List<Category> =
        eventDatabase.categoryDao().getCategoriesForRace(raceId)

    suspend fun getCategory(id: UUID) =
        eventDatabase.categoryDao().getCategory(id)

    suspend fun getCategoryData(id: UUID, raceId: UUID) =
        eventDatabase.categoryDao().getCategoryData(id, raceId)

    suspend fun getCategoryDataForRace(raceId: UUID) =
        eventDatabase.categoryDao().getCategoryDataForRace(raceId)

    suspend fun getHighestCategoryOrder(raceId: UUID) =
        eventDatabase.categoryDao().getHighestCategoryOrder(raceId)

    suspend fun getCategoryByName(name: String, raceId: UUID) =
        eventDatabase.categoryDao().getCategoryByName(name, raceId)

    suspend fun getCategoryByMaxAge(maxAge: Int, isMan: Boolean, raceId: UUID) =
        eventDatabase.categoryDao().getCategoryByMaxAge(maxAge, isMan, raceId)

    suspend fun getCategoryByBirthYear(birthYear: Int, woman: Boolean, raceId: UUID): Category? =
        eventDatabase.categoryDao().getCategoryByAge(birthYear, woman, raceId)

    suspend fun createCategory(category: Category) =
        eventDatabase.categoryDao().createOrUpdateCategory(category)

    suspend fun createOrUpdateCategory(category: Category, controlPoints: List<ControlPoint>?) {
        eventDatabase.withTransaction {
            eventDatabase.categoryDao().createOrUpdateCategory(category)

            if (controlPoints != null) {
                deleteControlPointsByCategory(category.id)
                createControlPoints(controlPoints)
            }
        }
    }

    private suspend fun createControlPoints(controlPoints: List<ControlPoint>) {
        controlPoints.forEach { cp ->
            createControlPoint(cp)
        }
    }

    suspend fun deleteCategory(id: UUID) = eventDatabase.categoryDao().deleteCategory(id)

    suspend fun createControlPoint(cp: ControlPoint) =
        eventDatabase.controlPointDao().createControlPoint(cp)

    //-------------------Control point-------------------
    suspend fun getControlPointsByCategory(categoryId: UUID) =
        eventDatabase.controlPointDao().getControlPointsByCategory(categoryId)

    suspend fun getControlPointAliasesByCategory(categoryId: UUID) =
        eventDatabase.controlPointDao().getControlPointAliasesByCategory(categoryId)

    suspend fun deleteControlPointsByCategory(categoryId: UUID) =
        eventDatabase.controlPointDao().deleteControlPointsByCategory(categoryId)

    //-------------------Aliases-------------------
    suspend fun getAliasesByRace(raceId: UUID) =
        eventDatabase.aliasDao().getAliasesByRace(raceId)

    suspend fun createOrUpdateAlias(alias: Alias) =
        eventDatabase.aliasDao().createOrUpdateAlias(alias)

    suspend fun deleteAliasesByRace(raceId: UUID) =
        eventDatabase.aliasDao().deleteAliasesByRace(raceId)

    //-------------------Competitors-------------------
    suspend fun getCompetitor(id: UUID) =
        eventDatabase.competitorDao().getCompetitor(id)

    suspend fun getCompetitorBySINumber(siNumber: Int, raceId: UUID): Competitor? =
        eventDatabase.competitorDao().getCompetitorBySINumber(siNumber, raceId)

    suspend fun getHighestStartNumberByRace(raceId: UUID) =
        eventDatabase.competitorDao().getHighestStartNumberByRace(raceId)

    fun getCompetitorDataFlowByRace(raceId: UUID): Flow<List<CompetitorData>> =
        eventDatabase.competitorDao().getCompetitorDataFlow(raceId)

    suspend fun getCompetitorsByCategory(categoryId: UUID) =
        eventDatabase.competitorDao().getCompetitorsByCategory(categoryId)

    suspend fun getCompetitorsByRace(raceId: UUID) =
        eventDatabase.competitorDao().getCompetitorsByRace(raceId)

    suspend fun getUnmatchedCompetitorsByRace(raceId: UUID) =
        eventDatabase.competitorDao().getUnmatchedCompetitorsByRace(raceId)

    fun createCompetitor(competitor: Competitor) =
        eventDatabase.competitorDao().createCompetitor(competitor)

    suspend fun deleteCompetitor(id: UUID) = eventDatabase.competitorDao().deleteCompetitor(id)

    suspend fun deleteAllCompetitorsByRace(raceId: UUID) =
        eventDatabase.competitorDao().deleteAllCompetitorsByRace(raceId)

    suspend fun checkIfSINumberExists(siNumber: Int, raceId: UUID): Int =
        eventDatabase.competitorDao().checkIfSINumberExists(siNumber, raceId)

    suspend fun checkIfStartNumberExists(startNumber: Int, raceId: UUID): Int =
        eventDatabase.competitorDao().checkIfStartNumberExists(startNumber, raceId)

    //-------------------Results-------------------
    suspend fun getResult(id: UUID) = eventDatabase.resultDao().getResult(id)

    suspend fun getResultData(id: UUID) = eventDatabase.resultDao().getResultData(id)

    fun getResultDataFlowByRace(raceId: UUID) =
        eventDatabase.resultDao().getResultDataFlowByRace(raceId)

    suspend fun getResultBySINumber(siNumber: Int, raceId: UUID) =
        eventDatabase.resultDao().getResultForSINumber(siNumber, raceId)

    suspend fun getResultByCompetitor(competitorId: UUID) =
        eventDatabase.resultDao().getResultByCompetitor(competitorId)

    suspend fun createOrUpdateResult(result: Result) =
        eventDatabase.resultDao().createOrUpdateResult(result)

    suspend fun setAllResultsUnsent(raceId: UUID) =
        eventDatabase.resultDao().setAllResultsUnsent(raceId)

    suspend fun saveResultPunches(
        result: Result,
        punches: List<Punch>
    ) {
        eventDatabase.withTransaction {
            eventDatabase.punchDao().deletePunchesByResult(result.id)
            eventDatabase.resultDao().createOrUpdateResult(result)
            punches.forEach { punch -> eventDatabase.punchDao().createOrUpdatePunch(punch) }
        }
    }

    suspend fun deleteResult(id: UUID) = eventDatabase.resultDao().deleteResult(id)
    suspend fun deleteResultForCompetitor(competitorId: UUID) =
        eventDatabase.resultDao().deleteResultByCompetitor(competitorId)

    suspend fun deleteAllResultsByRace(raceId: UUID) =
        eventDatabase.resultDao().deleteAllResultsByRace(raceId)

    //-------------------Punches-------------------
    suspend fun createPunch(punch: Punch) = eventDatabase.punchDao().createOrUpdatePunch(punch)

    suspend fun getPunchesByResult(resultId: UUID) =
        eventDatabase.punchDao().getPunchesByResult(resultId)


    //-------------------Result service-------------------
    fun getResultServiceByRaceId(raceId: UUID) =
        eventDatabase.resultServiceDao().getResultServiceByRaceId(raceId)

    fun getResultServiceLiveDataWithCountByRaceId(raceId: UUID) =
        eventDatabase.resultServiceDao().getResultServiceLiveDataWithCountByRaceId(raceId)

    suspend fun createOrUpdateResultService(resultService: ResultService) =
        eventDatabase.resultServiceDao().createOrUpdateResultService(resultService)

    //-------------------Race data-------------------
    suspend fun saveRaceData(raceData: RaceData) {
        eventDatabase.withTransaction {
            createRace(raceData.race)
            raceData.categories.forEach { cd ->
                createOrUpdateCategory(
                    cd.category,
                    cd.controlPoints
                )
            }
            raceData.aliases.forEach { alias -> createOrUpdateAlias(alias) }
            raceData.competitorData.forEach { cd ->
                createCompetitor(cd.competitorCategory.competitor)
                cd.readoutData?.let {
                    saveResultPunches(
                        it.result,
                        cd.readoutData!!.punches.map { ap -> ap.punch })
                }
            }
            raceData.unmatchedReadoutData.forEach { rd ->
                saveResultPunches(rd.result, rd.punches.map { it -> it.punch })
            }
        }
    }

    //-------------------Singleton instantiation-------------------
    companion object {
        private var INSTANCE: ARDFRepository? = null
        fun initialize(context: Context) {
            if (INSTANCE == null) {
                INSTANCE =
                    ARDFRepository(context)
            }
        }

        fun get(): ARDFRepository {
            return INSTANCE ?: throw IllegalStateException("ARDFRepository must be initialized")
        }
    }
}