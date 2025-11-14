package kolskypavel.ardfmanager.backend.files.json.adapters

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LocalDateTimeAdapter : JsonAdapter<LocalDateTime>() {
    private val legacyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val isoFormatterSeconds = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    @FromJson
    override fun fromJson(reader: JsonReader): LocalDateTime? {
        // Handle explicit JSON nulls gracefully
        val peek = reader.peek()
        if (peek == JsonReader.Token.NULL) {
            reader.nextNull<Unit>()
            return null
        }

        val string = reader.nextString()
        if (string.isNullOrBlank()) return null

        return try {
            // Try ISO-8601 without zone/offset (e.g. 2025-09-20T14:30:00)
            LocalDateTime.parse(string, isoFormatter)
        } catch (_: Exception) {
            try {
                // Fallback: old format
                LocalDateTime.parse(string, legacyFormatter)
            } catch (_: Exception) {
                throw JsonDataException("Invalid date format: $string")
            }
        }
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: LocalDateTime?) {
        writer.value(value?.format(isoFormatterSeconds))
    }
}