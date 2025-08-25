package kolskypavel.ardfmanager.backend.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kolskypavel.ardfmanager.backend.room.entity.Result
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ResultData
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface ResultDao {
    @Query("SELECT * FROM result WHERE id=(:id)")
    suspend fun getResult(id: UUID): Result

    @Query("SELECT * FROM result WHERE id=(:id)")
    suspend fun getResultData(id: UUID): ResultData

    @Query("SELECT * FROM result WHERE race_id=(:raceId)")
    fun getResultDataFlowByRace(raceId: UUID): Flow<List<ResultData>>

    @Query("SELECT * FROM result WHERE competitor_id=(:competitorId) LIMIT 1")
    suspend fun getResultByCompetitor(competitorId: UUID): Result?

    @Query("SELECT * FROM result WHERE si_number=(:siNumber) AND race_id=(:raceId) LIMIT 1")
    suspend fun getResultForSINumber(siNumber: Int, raceId: UUID): Result?

    @Upsert
    suspend fun createOrUpdateResult(result: Result)

    @Query("UPDATE result SET sent = 0 WHERE race_id =(:raceId)")
    suspend fun setAllResultsUnsent(raceId: UUID)

    @Query("DELETE FROM result WHERE id =(:id)")
    suspend fun deleteResult(id: UUID)

    @Query("DELETE FROM result WHERE competitor_id =(:competitorId)")
    suspend fun deleteResultByCompetitor(competitorId: UUID)

    @Query("DELETE FROM result WHERE race_id =(:raceId)")
    suspend fun deleteAllResultsByRace(raceId: UUID)
}