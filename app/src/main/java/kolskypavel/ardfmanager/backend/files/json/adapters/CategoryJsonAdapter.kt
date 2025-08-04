package kolskypavel.ardfmanager.backend.files.json.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kolskypavel.ardfmanager.backend.files.json.temps.CategoryJson
import kolskypavel.ardfmanager.backend.files.json.temps.ControlPointJson
import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import kolskypavel.ardfmanager.backend.room.entity.Category
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CategoryData

class CategoryJsonAdapter {
    @ToJson
    fun toJson(categoryData: CategoryData): CategoryJson {
        val category = categoryData.category
        return CategoryJson(
            category_name = category.name,
            category_gender = category.isMan,
            category_max_age = category.maxAge,
            category_length = category.length,
            category_climb = category.climb,
            category_different_properties = category.differentProperties,
            category_race_type = category.raceType,
            category_time_limit = category.timeLimit?.let { TimeProcessor.durationToMinuteString(it) }
                ?: "",
            category_band = category.categoryBand,
            category_control_points = categoryData.controlPoints.map { cp ->
                ControlPointJson(cp.siCode, cp.type)
            }
        )
    }

    @FromJson
    fun fromJson(jsonString: String): Category {
        val parsed = Category();

        return parsed
    }
}