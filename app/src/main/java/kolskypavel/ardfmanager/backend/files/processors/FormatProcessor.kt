package kolskypavel.ardfmanager.backend.files.processors

import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.constants.DataFormat
import kolskypavel.ardfmanager.backend.files.constants.DataType
import kolskypavel.ardfmanager.backend.files.wrappers.DataImportWrapper
import kolskypavel.ardfmanager.backend.room.entity.Race
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

interface FormatProcessor {
    suspend fun importData(
        inStream: InputStream,
        dataType: DataType,
        race: Race,
        dataProcessor: DataProcessor,
        stopOnInvalid: Boolean      // Stop import on invalid line - based on preferences
    ): DataImportWrapper

    suspend fun exportData(
        outStream: OutputStream,
        dataType: DataType,
        format: DataFormat,
        dataProcessor: DataProcessor,
        raceId: UUID
    )
}