package kolskypavel.ardfmanager.backend.files.json.temps


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
    val modified: Boolean,
    val controls_num: Int,
    val result_status: String,
    val punches: List<PunchJson>
)

data class PunchJson(
    var code: String,
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