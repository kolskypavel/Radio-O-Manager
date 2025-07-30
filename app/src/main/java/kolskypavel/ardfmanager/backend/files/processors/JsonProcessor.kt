package kolskypavel.ardfmanager.backend.files.processors

import ResultDataJsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.constants.DataFormat
import kolskypavel.ardfmanager.backend.files.constants.DataType
import kolskypavel.ardfmanager.backend.files.json.adapters.RaceDataJsonAdapter
import kolskypavel.ardfmanager.backend.files.wrappers.DataImportWrapper
import kolskypavel.ardfmanager.backend.room.entity.Race
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CategoryData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.RaceData
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ResultData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import com.squareup.moshi.Types
import kolskypavel.ardfmanager.backend.files.json.temps.ResultCompetitorJson

@OptIn(ExperimentalStdlibApi::class)
object JsonProcessor : FormatProcessor {

    override suspend fun importData(
        inStream: InputStream,
        dataType: DataType,
        race: Race,
        dataProcessor: DataProcessor
    ): DataImportWrapper {
        when (dataType) {
            DataType.CATEGORIES -> TODO()
            DataType.C0MPETITORS -> TODO()
            else -> TODO()
        }
    }

    override suspend fun exportData(
        outStream: OutputStream,
        dataType: DataType,
        format: DataFormat,
        dataProcessor: DataProcessor,
        raceId: UUID
    ): Boolean {
        when (dataType) {
            DataType.CATEGORIES -> TODO()
            DataType.C0MPETITORS -> TODO()
            DataType.RESULTS_SIMPLE -> exportResults(
                outStream,
                dataProcessor.getResultDataFlowByRace(raceId).first()
            )

            else -> TODO()
        }
        return true
    }

    fun importCompetitorData(
        inStream: InputStream,
        race: Race,
        categories: HashSet<CategoryData>
    ): DataImportWrapper {
        return DataImportWrapper(emptyList(), categories.toList())
    }

    suspend fun exportResults(outStream: OutputStream, results: List<ResultData>) {
        withContext(Dispatchers.IO) {
            val moshi: Moshi = Moshi.Builder()
                .add(ResultDataJsonAdapter())
                .add(KotlinJsonAdapterFactory())
                .build()

            val type = Types.newParameterizedType(List::class.java, ResultCompetitorJson::class.java)
            val adapter = moshi.adapter<List<ResultCompetitorJson>>(type)

            val exportList = results.mapNotNull { rd ->
                val compCat = rd.competitorCategory ?: return@mapNotNull null
                val competitor = compCat.competitor
                val category   = compCat.category   ?: return@mapNotNull null

                ResultCompetitorJson(
                    competitor_index = competitor.index,
                    si_number = competitor.siNumber ?: 0,
                    last_name = competitor.lastName,
                    first_name = competitor.firstName,
                    category_name = category.name,
                    result = ResultDataJsonAdapter().toJson(rd)
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
        val raceData: RaceData = dataProcessor.getRaceData(raceId);
    }

    suspend fun importCategories() {

    }

    suspend fun importCompetitors() {

    }

    suspend fun importRaceData(jsonString: String): RaceData {
        val moshi: Moshi = Moshi.Builder().add(RaceDataJsonAdapter()).build()
        val adapter = moshi.adapter<RaceData>()

        return adapter.nonNull().fromJson(jsonString)!!
    }


}