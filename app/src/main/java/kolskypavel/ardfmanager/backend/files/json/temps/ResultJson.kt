package kolskypavel.ardfmanager.backend.files.json.temps


data class ResultCompetitorJson(
    val competitor_index: String?,
    val si_number: Int?,
    val last_name: String,
    val first_name: String,
    val category_name: String,
    val result: ResultJson
) {}

data class ResultJson(
    val run_time: String,
    val place: Int,
    val controls_num: Int,
    val result_status: String,
    val punches: List<ResultPunchJson>
)

data class ResultPunchJson(
    val code: String,
    val control_type: String,
    val punch_status: String,
    val real_time: String,
    val week: Int,
    val day_of_week: Int,
    val split_time: String
)