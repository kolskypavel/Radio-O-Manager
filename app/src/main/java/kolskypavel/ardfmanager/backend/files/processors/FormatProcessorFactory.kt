package kolskypavel.ardfmanager.backend.files.processors

import kolskypavel.ardfmanager.backend.files.constants.DataFormat

object FormatProcessorFactory {
    fun getFormatProcessor(dataFormat: DataFormat): FormatProcessor {
        return when (dataFormat) {
            DataFormat.TXT, DataFormat.HTML -> TextProcessor
            DataFormat.CSV -> CsvProcessor
            DataFormat.JSON -> JsonProcessor
            DataFormat.IOF_XML -> IofXmlProcessor
        }
    }
}