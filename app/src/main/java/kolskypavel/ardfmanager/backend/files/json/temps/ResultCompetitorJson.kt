package kolskypavel.ardfmanager.backend.files.json.temps

data class ResultCompetitorJson (
    val competitor_index: String?,
    val si_number: Int?,
    val last_name: String,
    val first_name: String,
    val category_name: String,
    val result: ResultJson
){}