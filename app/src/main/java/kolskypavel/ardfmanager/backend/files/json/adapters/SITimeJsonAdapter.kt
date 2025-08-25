package kolskypavel.ardfmanager.backend.files.json.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson
import kolskypavel.ardfmanager.backend.files.json.temps.SITimeJson
import kolskypavel.ardfmanager.backend.sportident.SITime
import java.time.LocalTime

class SITimeJsonAdapter {

    @ToJson
    fun toJson(siTime: SITime): SITimeJson {
        return SITimeJson(
            real_time = siTime.getTimeString(),
            week = siTime.getWeek(),
            day_of_week = siTime.getDayOfWeek()
        )
    }

    @FromJson
    fun fromJson(json: SITimeJson): SITime {
        return SITime(
            time = LocalTime.parse(json.real_time),
            dayOfWeek = json.day_of_week,
            week = json.week
        )
    }
}