package kolskypavel.ardfmanager.backend.files.json.temps

data class RobisResultJson(
    var last_name: String,
    var first_name: String,
    var competitor_index: String,
    var si_number: Int,
    var reason: String?
) {}

data class RobisResponseJson(
    val created_entries: List<RobisResultJson>,
    val invalid_data: List<RobisResultJson>,
) {}