package kolskypavel.ardfmanager.backend.results

import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData

class ResultDataComparator : Comparator<CompetitorData> {
    override fun compare(o1: CompetitorData, o2: CompetitorData): Int {
        val readoutData1 = o1.readoutData
        val readoutData2 = o2.readoutData

        // Compare based on the existence of readoutResult
        if (readoutData1 == null && readoutData2 == null) {
            return 0
        } else if (readoutData1 == null) {
            return 1
        } else if (readoutData2 == null) {
            return -1
        }

        // Both readoutResult are not null, compare based on their result
        return readoutData1.result.compareTo(readoutData2.result)
    }
}