package kolskypavel.ardfmanager.backend.room.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kolskypavel.ardfmanager.backend.room.entity.ResultService
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ResultServiceData
import java.util.UUID

@Dao
interface ResultServiceDao {
    @Query("SELECT * FROM result_service WHERE id=(:id)")
    suspend fun getResultService(id: UUID): ResultService

    @Query(
        """
    SELECT *, 
    (SELECT COUNT(*) FROM result WHERE result.race_id = :raceId) AS resultCount
    FROM result_service 
    WHERE race_id = :raceId 
    LIMIT 1"""
    )
    fun getResultServiceLiveDataWithCountByRaceId(raceId: UUID): LiveData<ResultServiceData>

    @Query("SELECT * FROM result_service WHERE race_id = (:raceId) LIMIT 1")
    fun getResultServiceByRaceId(raceId: UUID): ResultService?

    @Upsert
    suspend fun createOrUpdateResultService(resultService: ResultService)

    @Query("DELETE FROM result_service WHERE id =(:id)")
    suspend fun deleteResultService(id: UUID)
}