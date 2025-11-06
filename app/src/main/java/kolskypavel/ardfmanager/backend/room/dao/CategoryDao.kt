package kolskypavel.ardfmanager.backend.room.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kolskypavel.ardfmanager.backend.room.entity.Category
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CategoryData
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface CategoryDao {

    @Transaction
    @Query("SELECT * FROM category WHERE race_id=(:raceId) ORDER BY `order`")
    fun getCategoryFlowForRace(raceId: UUID): Flow<List<CategoryData>>

    @Query("SELECT * FROM category WHERE race_id=(:raceId) ORDER BY `order`")
    suspend fun getCategoriesForRace(raceId: UUID): List<Category>

    @Query("SELECT * FROM category WHERE id=(:id) LIMIT 1")
    suspend fun getCategory(id: UUID): Category?

    @Query("SELECT * FROM category WHERE id=(:id) LIMIT 1")
    suspend fun getCategoryData(id: UUID): CategoryData?

    @Query("SELECT * FROM category WHERE  race_id=(:raceId) ")
    suspend fun getCategoryDataForRace(raceId: UUID): List<CategoryData>

    @Query("SELECT `order` FROM category WHERE race_id =(:raceId) ORDER BY `order` DESC LIMIT 1")
    suspend fun getHighestCategoryOrder(raceId: UUID): Int

    @Query("SELECT * FROM category WHERE name=(:name) AND race_id = (:raceId) LIMIT 1")
    suspend fun getCategoryByName(name: String, raceId: UUID): Category?

    @Upsert
    suspend fun createOrUpdateCategory(category: Category)

    @Query("DELETE FROM category WHERE id=(:id) ")
    suspend fun deleteCategory(id: UUID)
}