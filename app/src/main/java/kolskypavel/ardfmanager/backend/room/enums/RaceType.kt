package kolskypavel.ardfmanager.backend.room.enums

enum class RaceType(val value: Int) {
    CLASSIC(0),
    SHORT(1),
    SPRINT(2),
    FOXORING(3),
    ORIENTEERING(4);

    companion object {
        fun getByValue(value: Int) = entries.firstOrNull { it.value == value } ?: CLASSIC
    }
}
