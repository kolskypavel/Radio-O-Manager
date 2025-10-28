package kolskypavel.ardfmanager.backend.room.enums

enum class RaceLevel(val value: Int) {
    INTERNATIONAL(0),
    NATIONAL(1),
    REGIONAL(2),
    DISTRICT(3),
    PRACTICE(4),
    OTHER(5);

    companion object {
        fun getByValue(value: Int) = entries.firstOrNull { it.value == value } ?: PRACTICE
    }
}