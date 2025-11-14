package kolskypavel.ardfmanager.backend.files.processors

import ResultJsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.constants.DataFormat
import kolskypavel.ardfmanager.backend.files.constants.DataType
import kolskypavel.ardfmanager.backend.files.json.adapters.LocalDateTimeAdapter
import kolskypavel.ardfmanager.backend.files.json.adapters.RaceDataJsonAdapter
import kolskypavel.ardfmanager.backend.files.json.temps.ResultCompetitorJson
import kolskypavel.ardfmanager.backend.files.json.temps.RobisResponseJson
import kolskypavel.ardfmanager.backend.files.wrappers.DataImportWrapper
import kolskypavel.ardfmanager.backend.results.ResultsProcessor
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CategoryData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.RaceData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

@OptIn(ExperimentalStdlibApi::class)
object JsonProcessor : FormatProcessor {

    override suspend fun importData(
        inStream: InputStream,
        dataType: DataType,
        race: Race,
        dataProcessor: DataProcessor,
        stopOnInvalid: Boolean
    ): DataImportWrapper {
        when (dataType) {
            DataType.CATEGORIES -> TODO()
            DataType.COMPETITORS -> TODO()
            else -> TODO()
        }
    }

    override suspend fun exportData(
        outStream: OutputStream,
        dataType: DataType,
        format: DataFormat,
        dataProcessor: DataProcessor,
        race: Race
    ) {
        when (dataType) {
            DataType.CATEGORIES -> TODO()
            DataType.COMPETITORS -> TODO()
            DataType.RESULTS -> exportResults(
                outStream,
                ResultsProcessor.getCompetitorDataByRace(race.id, dataProcessor),
                race,
                dataProcessor
            )

            else -> TODO()
        }
    }

    fun importCompetitorData(
        inStream: InputStream,
        race: Race,
        categories: HashSet<CategoryData>
    ): DataImportWrapper {
        TODO()
    }

    suspend fun importCategories() {

    }

    suspend fun importCompetitors() {

    }

    fun importRaceData(inStream: InputStream, dataProcessor: DataProcessor): RaceData {
        val jsonString = inStream.bufferedReader(Charsets.UTF_8).use { it.readText() }

        val moshi: Moshi = Moshi.Builder()
            .add(RaceDataJsonAdapter(dataProcessor))
            .add(LocalDateTimeAdapter())
            .add(KotlinJsonAdapterFactory())
            .build()
        val adapter = moshi.adapter<RaceData>()

        return adapter.nonNull().fromJson(jsonString)!!
    }

    fun parseRobisResponse(response: String): RobisResponseJson? {
        val moshi: Moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(RobisResponseJson::class.java)

        return adapter.fromJson(response)
    }

    suspend fun exportResults(
        outStream: OutputStream,
        results: List<CompetitorData>,
        race: Race,
        dataProcessor: DataProcessor
    ) {
        withContext(Dispatchers.IO) {
            val moshi: Moshi = Moshi.Builder()
                .add(ResultJsonAdapter(race, dataProcessor))
                .add(LocalDateTimeAdapter())
                .add(KotlinJsonAdapterFactory())
                .build()

            val type =
                Types.newParameterizedType(List::class.java, ResultCompetitorJson::class.java)
            val adapter = moshi.adapter<List<ResultCompetitorJson>>(type)

            val exportList = results.mapNotNull { rd ->
                val result = rd.readoutData ?: return@mapNotNull null

                val compCat = rd.competitorCategory
                val competitor = compCat.competitor
                val category = compCat.category ?: return@mapNotNull null

                ResultCompetitorJson(
                    competitor_index = competitor.index,
                    si_number = competitor.siNumber ?: 0,
                    last_name = competitor.lastName,
                    first_name = competitor.firstName,
                    category_name = category.name,
                    result = ResultJsonAdapter(race, dataProcessor).toJson(rd)
                )
            }

            val json = adapter.toJson(exportList)
            outStream.write(json.toByteArray(Charsets.UTF_8))
            outStream.flush()
        }
    }


    suspend fun exportRaceData(
        outStream: OutputStream,
        dataProcessor: DataProcessor,
        raceId: UUID
    ) {
        withContext(Dispatchers.IO) {
            val moshi: Moshi = Moshi.Builder()
                .add(RaceDataJsonAdapter(dataProcessor))
                .add(LocalDateTimeAdapter())
                .add(KotlinJsonAdapterFactory())
                .build()
            val raceData: RaceData = dataProcessor.getRaceData(raceId);
            val adapter = moshi.adapter<RaceData>()

            val json = adapter.toJson(raceData)

            outStream.write(json.toByteArray(Charsets.UTF_8))

            outStream.flush()
        }
    }
}