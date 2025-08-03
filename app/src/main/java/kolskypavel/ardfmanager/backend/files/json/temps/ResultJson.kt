package kolskypavel.ardfmanager.backend.files.json.temps

data class ResultJson(
    val run_time: String,
    val place: Int,
    val controls_num: Int,
    val result_status: String,
    val punches: List<ResultPunchJson>
)