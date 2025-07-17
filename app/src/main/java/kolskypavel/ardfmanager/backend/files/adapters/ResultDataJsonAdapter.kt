package kolskypavel.ardfmanager.backend.files.adapters

import com.squareup.moshi.ToJson
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ResultData

class ResultDataJsonAdapter {
    @ToJson
    fun toJson(resultData: ResultData): String {
        return ""
    }

}