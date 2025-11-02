package kolskypavel.ardfmanager.backend.room.entity.embeddeds

import kolskypavel.ardfmanager.backend.room.entity.Alias
import kolskypavel.ardfmanager.backend.room.entity.Race

// Contains all data of one race
data class RaceData(
    var race: Race,
    val categories: List<CategoryData>,
    val aliases: List<Alias>,
    val competitorData: List<CompetitorData>,
    val unmatchedReadoutData: List<ReadoutData>
) {}