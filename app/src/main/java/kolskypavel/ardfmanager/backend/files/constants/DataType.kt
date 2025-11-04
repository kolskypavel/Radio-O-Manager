package kolskypavel.ardfmanager.backend.files.constants

enum class DataType(var value: Int) {
    CATEGORIES(0),
    COMPETITORS(1),
    COMPETITOR_STARTS(2),
    RESULTS(3),
    READOUT_DATA(4);

    companion object {
        fun getByValue(value: Int) = DataType.entries.firstOrNull { it.value == value }
    }
}