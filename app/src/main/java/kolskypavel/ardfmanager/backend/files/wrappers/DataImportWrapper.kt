package kolskypavel.ardfmanager.backend.files.wrappers

import kolskypavel.ardfmanager.backend.files.constants.DataType
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CategoryData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorCategory

data class DataImportWrapper(
    var competitorCategories: List<CompetitorCategory>,
    var categories: List<CategoryData>,
    var invalidLines: ArrayList<Pair<Int, String>>      // Marks each error with cause
) {
    fun getCount(dataType: DataType): Int {
        return when (dataType) {
            DataType.CATEGORIES -> categories.size
            DataType.COMPETITORS -> competitorCategories.size
            else -> 0
        }
    }
}