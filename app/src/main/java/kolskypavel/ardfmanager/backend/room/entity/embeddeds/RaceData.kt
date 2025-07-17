package kolskypavel.ardfmanager.backend.room.entity.embeddeds

import kolskypavel.ardfmanager.backend.room.entity.Category
import kolskypavel.ardfmanager.backend.room.entity.Race

/**
 * Contains all data of one race
 */
data class RaceData (
    val race: Race,
    val categories: List<Category>,
    val resultData: List<ResultData>
    )
{}