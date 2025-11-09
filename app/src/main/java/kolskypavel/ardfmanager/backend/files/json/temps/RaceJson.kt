package kolskypavel.ardfmanager.backend.files.json.temps

import kolskypavel.ardfmanager.backend.room.enums.ControlPointType
import kolskypavel.ardfmanager.backend.room.enums.RaceBand
import kolskypavel.ardfmanager.backend.room.enums.RaceLevel
import kolskypavel.ardfmanager.backend.room.enums.RaceType
import java.time.LocalDateTime

data class RaceJson(
    val race_name: String,
    val race_start: LocalDateTime,
    val race_type: RaceType,
    val race_band: RaceBand?,
    val race_level: RaceLevel?,
    val race_time_limit: String?,
    val race_api_key: String?,
    val categories: List<CategoryJson>,
    val aliases: List<AliasJson>?,
    val competitors: List<CompetitorJson>,
    val unmatched_results: List<UnmatchedResultJson>?
)

data class CategoryJson(
    val category_name: String,
    val category_gender: Boolean,
    val category_max_age: Int?,
    val category_length: Int?,
    val category_climb: Int?,
    val category_control_points: List<ControlPointJson>,
    val category_different_properties: Boolean,
    val category_race_type: RaceType?,
    val category_time_limit: String?,
    val category_band: RaceBand?
)

data class ControlPointJson(
    val si_code: Int,
    val control_type: ControlPointType
)

data class AliasJson(
    val alias_si_code: Int,
    val alias_name: String
)

data class CompetitorJson(
    val first_name: String,
    val last_name: String,
    val competitor_club: String?,
    val competitor_category: String,
    val competitor_index: String?,
    val competitor_gender: Boolean,
    val birth_year: Int?,
    val si_number: Int?,
    val si_rent: Boolean?,
    val start_number: Int?,
    val competitor_start_time: String?,
    val result: ResultJson?
)