package kolskypavel.ardfmanager.backend.room.enums

enum class RaceType(val value: Int) {
    CLASSIC(0),
    SPRINT(1),
    FOXORING(2),
    ORIENTEERING(3);

    companion object {
        fun getByValue(value: Int) = entries.firstOrNull { it.value == value } ?: CLASSIC
    }
}
