package kolskypavel.ardfmanager.backend.room.enums

enum class ResultServiceStatus(val value: Int) {
    RUNNING(0),
    OK(1),
    NO_NETWORK(2),
    UNAUTHORIZED(3),
    ERROR(4);
}