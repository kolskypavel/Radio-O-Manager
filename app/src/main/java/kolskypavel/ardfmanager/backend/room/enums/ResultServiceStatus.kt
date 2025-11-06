package kolskypavel.ardfmanager.backend.room.enums

enum class ResultServiceStatus(val value: Int) {
    DISABLED(0),
    INIT(1),
    RUNNING(2),
    NO_NETWORK(3),
    UNAUTHORIZED(4),
    ERROR(5);
}