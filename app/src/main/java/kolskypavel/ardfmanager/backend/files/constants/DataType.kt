package kolskypavel.ardfmanager.backend.files.constants

enum class DataType(var value: Int) {
    CATEGORIES(0),
    COMPETITORS(1),
    COMPETITOR_STARTS_TIME(2),
    COMPETITOR_STARTS_CATEGORIES(3),
    COMPETITOR_STARTS_CLUBS(4),
    RESULTS(5),
    READOUT_DATA(6);

    companion object {
        fun getByValue(value: Int) = DataType.entries.firstOrNull { it.value == value }
    }
}