package kolskypavel.ardfmanager.backend.files.constants

enum class DataFormat(var value: Int) {
    TXT(0),
    HTML(1),
    JSON(2),
    CSV(3),
    IOF_XML(4);

    companion object {
        fun getByValue(value: Int) = DataFormat.entries.firstOrNull { it.value == value }
    }
}