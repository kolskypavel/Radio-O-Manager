package kolskypavel.ardfmanager.backend.files.json.temps

import com.squareup.moshi.Json
import java.time.LocalDateTime


data class ResultCompetitorJson(
    val competitor_index: String?,
    val si_number: Int?,
    val last_name: String,
    val first_name: String,
    val category_name: String,
    val result: ResultJson
)

data class ResultJson(
    val check_time: SITimeJson?,
    val start_time: SITimeJson?,
    val finish_time: SITimeJson?,
    val run_time: String,
    val place: Int,
    val readoutTime: LocalDateTime?,
    val modified: Boolean,
    // New preferred field for JSON output/input
    val punch_count: Int?,
    // Backwards-compatible field: older JSON may contain controls_num.
    @Deprecated("Use punch_count instead")
    @Json(ignore = true)
    val controls_num: Int? = null,
    val result_status: String,
    val automatic_status: Boolean?,
    val punches: List<PunchJson>
)

data class PunchJson(
    var code: String,
    var si_code: Int?,
    val control_type: String,
    val punch_status: String,
    val si_time: SITimeJson,
    val split_time: String
)

data class UnmatchedResultJson(
    val si_number: Int?,
    val check_time: SITimeJson?,
    val start_time: SITimeJson?,
    val finish_time: SITimeJson?,
    val run_time: String,
    val punches: List<PunchJson>
)

data class SITimeJson(
    val real_time: String,
    val week: Int,
    val day_of_week: Int
)