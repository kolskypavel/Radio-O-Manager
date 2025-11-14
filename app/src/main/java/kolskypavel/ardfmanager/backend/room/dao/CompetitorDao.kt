package kolskypavel.ardfmanager.backend.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.room.Upsert
import kolskypavel.ardfmanager.backend.room.entity.Competitor
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface CompetitorDao {
    @Query("SELECT * FROM competitor WHERE race_id=(:raceId) ")
    @Transaction
    @RewriteQueriesToDropUnusedColumns
    fun getCompetitorDataFlow(raceId: UUID): Flow<List<CompetitorData>>

    @Query("SELECT * FROM competitor WHERE race_id=(:raceId) ")
    suspend fun getCompetitorsByRace(raceId: UUID): List<Competitor>

    @Query("SELECT * FROM competitor WHERE id=(:id) LIMIT 1")
    suspend fun getCompetitor(id: UUID): Competitor?

    @Query("SELECT * FROM competitor WHERE si_number=(:siNumber) AND race_id = (:raceId) LIMIT 1")
    suspend fun getCompetitorBySINumber(siNumber: Int, raceId: UUID): Competitor?

    @Query("SELECT start_number FROM competitor WHERE race_id=(:raceId) ORDER BY start_number DESC LIMIT 1 ")
    suspend fun getHighestStartNumberByRace(raceId: UUID): Int

    @Query("SELECT * FROM competitor WHERE category_id=(:categoryId)")
    suspend fun getCompetitorsByCategory(categoryId: UUID): List<Competitor>

    @Query("SELECT * FROM competitor WHERE category_id IS NULL AND race_id=(:raceId)")
    suspend fun getUnmatchedCompetitorsByRace(raceId: UUID): List<Competitor>

    @Query("SELECT COUNT(*) FROM competitor WHERE si_number=(:siNumber) AND race_id =(:raceId)  LIMIT 1")
    suspend fun checkIfSINumberExists(siNumber: Int, raceId: UUID): Int

    @Query("SELECT COUNT(*) FROM competitor WHERE start_number=(:startNumber) AND race_id =(:raceId)  LIMIT 1")
    suspend fun checkIfStartNumberExists(startNumber: Int, raceId: UUID): Int

    @Upsert
    suspend fun createCompetitor(competitor: Competitor)

    @Query("DELETE FROM competitor WHERE id =(:id)")
    suspend fun deleteCompetitor(id: UUID)

    @Query("DELETE FROM competitor WHERE category_id =(:categoryId)")
    suspend fun deleteCompetitorsByCategory(categoryId: UUID)

    @Query("DELETE FROM competitor WHERE race_id =(:raceId)")
    suspend fun deleteAllCompetitorsByRace(raceId: UUID)
}