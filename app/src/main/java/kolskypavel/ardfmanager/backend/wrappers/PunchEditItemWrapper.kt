package kolskypavel.ardfmanager.backend.wrappers

import kolskypavel.ardfmanager.backend.room.entity.Punch
import kolskypavel.ardfmanager.backend.room.entity.Result
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.AliasPunch
import kolskypavel.ardfmanager.backend.room.enums.PunchStatus
import kolskypavel.ardfmanager.backend.room.enums.SIRecordType
import kolskypavel.ardfmanager.backend.sportident.SITime
import java.time.Duration
import java.time.LocalTime
import java.util.UUID

data class PunchEditItemWrapper(
    var punch: Punch,
    var isCodeValid: Boolean,
    var isTimeValid: Boolean,
    var isDayValid: Boolean,
    var isWeekValid: Boolean,
) {
    companion object {
        fun getWrappers(punches: ArrayList<AliasPunch>): ArrayList<PunchEditItemWrapper> {
            return ArrayList(punches.map { ap ->
                PunchEditItemWrapper(
                    ap.punch,
                    isCodeValid = true,
                    isTimeValid = true,
                    isDayValid = true,
                    isWeekValid = true
                )
            })
        }

        fun getPunches(punchEditItemWrappers: ArrayList<PunchEditItemWrapper>): ArrayList<Punch> {
            val punches = ArrayList<Punch>()
            punchEditItemWrappers.forEach { wrapper ->
                punches.add(wrapper.punch)
            }
            return punches
        }

        fun getStartOrFinishWrapper(
            start: Boolean,
            result: Result?,
            raceId: UUID
        ): PunchEditItemWrapper {

            if (start) {
                return if (result?.startTime != null) {
                    PunchEditItemWrapper(
                        Punch(
                            UUID.randomUUID(),
                            raceId,
                            null,
                            null,
                            0,
                            result.startTime!!,
                            result.startTime!!,
                            SIRecordType.START,
                            0,
                            PunchStatus.VALID,
                            Duration.ZERO
                        ), true, true, true, true
                    )
                } else {
                    PunchEditItemWrapper(
                        Punch(
                            UUID.randomUUID(),
                            raceId,
                            null,
                            null,
                            0,
                            SITime(LocalTime.MIN),
                            SITime(LocalTime.MIN),
                            SIRecordType.START,
                            0,
                            PunchStatus.VALID,
                            Duration.ZERO
                        ), true, true, true, true
                    )

                }
            }

            // FINISH punch
            else {
                return if (result?.finishTime != null) {
                    PunchEditItemWrapper(
                        Punch(
                            UUID.randomUUID(),
                            raceId,
                            null,
                            null,
                            0,
                            result.finishTime!!,
                            result.finishTime!!,
                            SIRecordType.FINISH,
                            0,
                            PunchStatus.VALID,
                            Duration.ZERO
                        ), isCodeValid = true, isTimeValid = true, isDayValid = true, isWeekValid = true
                    )
                } else {
                    PunchEditItemWrapper(
                        Punch(
                            UUID.randomUUID(),
                            raceId,
                            null,
                            null,
                            0,
                            SITime(LocalTime.MIN),
                            SITime(LocalTime.MIN),
                            SIRecordType.FINISH,
                            0,
                            PunchStatus.VALID,
                            Duration.ZERO
                        ), isCodeValid = true, isTimeValid = true, isDayValid = true, isWeekValid = true
                    )

                }
            }
        }
    }
}