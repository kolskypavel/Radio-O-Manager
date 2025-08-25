package kolskypavel.ardfmanager.backend.room.entity.embeddeds

import androidx.room.Embedded
import androidx.room.Relation
import kolskypavel.ardfmanager.backend.room.entity.Category
import kolskypavel.ardfmanager.backend.room.entity.Competitor
import java.io.Serializable

// Used to group competitor with category
data class CompetitorCategory(
    @Embedded var competitor: Competitor,
    @Relation(
        parentColumn = "category_id",
        entityColumn = "id",
        entity = Category::class
    ) var category: Category?
) : Serializable