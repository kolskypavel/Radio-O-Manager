package kolskypavel.ardfmanager.backend.room.enums

enum class PunchStatus(val value: Int) {
    VALID(0),
    INVALID(1),
    DUPLICATE(2),
    UNKNOWN(3);

    companion object {
        fun getByValue(value: Int) =
            PunchStatus.entries.firstOrNull { it.value == value } ?: VALID
    }
}