package kolskypavel.ardfmanager.backend.room.enums

enum class ResultStatus(val value: Int) : Comparable<ResultStatus> {
    OK(0),
    MISPUNCHED(1),
    NO_RANKING(2),          //Did not fulfill the min CP requirement
    DISQUALIFIED(3),
    DID_NOT_START(4),      //Did not finish the race
    DID_NOT_FINISH(5),      //Did not finish the race
    OVER_TIME_LIMIT(6),     //Over the time limit / not enough points
    UNOFFICIAL(7),          //Did not run officially
    ERROR(8);              //Error - missing finish record etc

    companion object {
        fun getByValue(value: Int) =
            ResultStatus.entries.firstOrNull { it.value == value } ?: NO_RANKING
    }
}