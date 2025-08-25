package kolskypavel.ardfmanager.backend.files.json.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LocalDateTimeAdapter : JsonAdapter<LocalDateTime>() {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    @FromJson
    override fun fromJson(reader: JsonReader): LocalDateTime? {
        val string = reader.nextString()
        return LocalDateTime.parse(string, formatter)
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: LocalDateTime?) {
        writer.value(value?.let { TimeProcessor.formatLocalDateTime(it) } ?: "")
    }
}