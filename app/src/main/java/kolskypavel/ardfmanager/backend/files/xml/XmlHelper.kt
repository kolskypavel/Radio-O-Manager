package kolskypavel.ardfmanager.backend.files.xml

import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import kolskypavel.ardfmanager.backend.room.entity.Competitor
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.AliasPunch
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ReadoutData
import kolskypavel.ardfmanager.backend.room.enums.ResultStatus
import kolskypavel.ardfmanager.backend.room.enums.SIRecordType
import kolskypavel.ardfmanager.backend.wrappers.ResultWrapper
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlSerializer
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime

object XmlHelper {

    fun createSerializer(outStream: OutputStream): Pair<XmlSerializer, OutputStreamWriter> {
        val factory = XmlPullParserFactory.newInstance()
        val serializer = factory.newSerializer()
        val writer = OutputStreamWriter(outStream, StandardCharsets.UTF_8)
        serializer.setOutput(writer)
        serializer.startDocument("UTF-8", true)
        return Pair(serializer, writer)
    }

    fun finishSerializer(serializer: XmlSerializer, writer: OutputStreamWriter) {
        try {
            serializer.endTag(null, "ResultList")
        } catch (_: Exception) {
        }
        try {
            serializer.endDocument()
        } catch (_: Exception) {
        }
        try {
            writer.flush()
        } catch (_: Exception) {
        }
    }

    /**
     * @param resultComplete : marks if the results are complete or temporary
     */
    fun writeResultListRoot(serializer: XmlSerializer, race: Race) {
        serializer.startTag(null, "ResultList")
        serializer.attribute(null, "xmlns", "http://www.orienteering.org/datastandard/3.0")
        serializer.attribute(null, "status", "complete")
        serializer.attribute(null, "iofVersion", "3.0")
        writeRaceTag(serializer, race)
    }

    fun writeRaceTag(serializer: XmlSerializer, race: Race) {
        serializer.startTag(null, "Event")
        writeTextElement(serializer, "Name", race.name)
        serializer.startTag(null, "StartDate")
        serializer.startTag(null, "Date")
        serializer.attribute(null, "dateFormat", "YYYY-MM-DD")
        serializer.text(TimeProcessor.formatLocalDate(race.startDateTime.toLocalDate()))
        serializer.endTag(null, "Date")
        serializer.endTag(null, "StartDate")
        serializer.endTag(null, "Event")
    }

    fun writeCategoryResult(
        serializer: XmlSerializer,
        res: ResultWrapper,
        startZero: LocalDateTime
    ) {

        serializer.startTag(null, "ClassResult")
        serializer.startTag(null, "Class")
        serializer.attribute(null, "sex", if (res.category!!.isMan) "M" else "F")
        writeTextElement(serializer, "Name", res.category.name)
        serializer.endTag(null, "Class")

        for (cd in res.subList) {
            writePersonResult(serializer, cd, startZero)
        }
        serializer.endTag(null, "ClassResult")
    }

    fun writePersonResult(
        serializer: XmlSerializer,
        competitorData: CompetitorData,
        startZero: LocalDateTime
    ) {
        serializer.startTag(null, "PersonResult")

        writePersonAndClub(serializer, competitorData.competitorCategory.competitor)

        writeResult(serializer, competitorData.readoutData, startZero)

        writeSplitTimes(serializer, competitorData.readoutData?.punches)

        serializer.endTag(null, "PersonResult")
    }

    private fun writePersonAndClub(
        serializer: XmlSerializer,
        competitor: Competitor
    ) {
        serializer.startTag(null, "Person")

        if (competitor.index.isNotBlank()) {
            serializer.startTag(null, "Id")
            serializer.attribute(null, "type", "CZE")
            serializer.text(competitor.index)
            serializer.endTag(null, "Id")
        }

        serializer.startTag(null, "Name")
        writeTextElement(serializer, "Family", competitor.lastName)
        writeTextElement(serializer, "Given", competitor.firstName)
        serializer.endTag(null, "Name")

        if (competitor.siNumber != null) {
            writeTextElement(serializer, "ControlCard", competitor.siNumber.toString())
        }
        writeTextElement(serializer, "BibNumber", competitor.startNumber.toString())
        serializer.endTag(null, "Person")

        if (competitor.club.isNotBlank()) {
            serializer.startTag(null, "Organisation")
            writeTextElement(serializer, "Name", competitor.club)
            serializer.endTag(null, "Organisation")
        }
    }


    private fun writeResult(
        serializer: XmlSerializer,
        readout: ReadoutData?,
        startZero: LocalDateTime
    ) {
        serializer.startTag(null, "Result")
        if (readout != null) {
            val res = readout.result
            try {
                val seconds = res.runTime.toMillis() / 1000
                writeTextElement(serializer, "Time", seconds.toString())
                res.startTime?.let {
                    writeTextElement(
                        serializer,
                        "StartTime",
                        TimeProcessor.formatLocalDateTime(it.toLocalDateTime(startZero))
                    )
                }
                res.finishTime?.let {
                    writeTextElement(
                        serializer,
                        "FinishTime",
                        TimeProcessor.formatLocalDateTime(it.toLocalDateTime(startZero))
                    )
                }
            } catch (_: Exception) {
            }
            if (res.place > 0) {
                writeTextElement(serializer, "Position", res.place.toString())
            }
            writeTextElement(serializer, "Status", convertResultStatus(res.resultStatus))
        } else {
            writeTextElement(serializer, "Status", "Active")
        }
        serializer.endTag(null, "Result")
    }

    private fun writeSplitTimes(serializer: XmlSerializer, punches: List<AliasPunch>?) {
        if (punches == null || punches.isEmpty()) return
        serializer.startTag(null, "SplitTimes")
        var cumulativeMillis: Long = 0
        for (p in punches.filter { it.punch.punchType == SIRecordType.CONTROL }) {
            serializer.startTag(null, "SplitTime")
            val control = p.punch.siCode.toString()
            writeTextElement(serializer, "ControlCode", control)
            try {
                cumulativeMillis += p.punch.split.toMillis()
                val tSeconds = cumulativeMillis / 1000
                writeTextElement(serializer, "Time", tSeconds.toString())
            } catch (_: Exception) {
            }
            serializer.endTag(null, "SplitTime")
        }
        serializer.endTag(null, "SplitTimes")
    }

    // Converts result status to the IOF version
    fun convertResultStatus(resultStatus: ResultStatus): String {
        return when (resultStatus) {
            ResultStatus.OK -> "OK"
            ResultStatus.MISPUNCHED -> "MissingPunch"
            ResultStatus.NO_RANKING -> "MissingPunch"
            ResultStatus.DISQUALIFIED -> "Disqualified"
            ResultStatus.DID_NOT_START -> "DidNotStart"
            ResultStatus.DID_NOT_FINISH -> "DidNotFinish"
            ResultStatus.OVER_TIME_LIMIT -> "OverTime"
            ResultStatus.UNOFFICIAL -> "NotCompeting"
            ResultStatus.ERROR -> "Cancelled"
        }
    }

    fun writeTextElement(serializer: XmlSerializer, name: String, value: String) {
        serializer.startTag(null, name)
        serializer.text(value)
        serializer.endTag(null, name)
    }
}