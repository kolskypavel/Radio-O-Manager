package kolskypavel.ardfmanager.ui.competitors

import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import java.text.Collator

// TODO: add localization
class CompetitorNameComparator(
    private val collator: Collator
) : Comparator<CompetitorData> {

    override fun compare(o1: CompetitorData, o2: CompetitorData): Int {
        val lastName1 = o1.competitorCategory.competitor.lastName
        val lastName2 = o2.competitorCategory.competitor.lastName
        return collator.compare(lastName1, lastName2)
    }
}

class CompetitorStartNumComparator : Comparator<CompetitorData> {
    override fun compare(o1: CompetitorData, o2: CompetitorData): Int {
        return o1.competitorCategory.competitor.startNumber.compareTo(o2.competitorCategory.competitor.startNumber)
    }
}

class CompetitorClubComparator(
    private val collator: Collator
) : Comparator<CompetitorData> {

    override fun compare(o1: CompetitorData, o2: CompetitorData): Int {
        val club1 = o1.competitorCategory.competitor.club
        val club2 = o2.competitorCategory.competitor.club
        return collator.compare(club1, club2)
    }
}

class CompetitorCategoryComparator(
    private val collator: Collator
) : Comparator<CompetitorData> {

    override fun compare(o1: CompetitorData, o2: CompetitorData): Int {
        val name1 = o1.competitorCategory.category?.name
        val name2 = o2.competitorCategory.category?.name

        return when {
            name1 != null && name2 != null -> collator.compare(name1, name2)
            name1 != null -> -1 // non-null comes before null
            name2 != null -> 1
            else -> 0
        }
    }
}

class CompetitorSINumberComparator : Comparator<CompetitorData> {
    override fun compare(o1: CompetitorData, o2: CompetitorData): Int {

        val si1 = o1.competitorCategory.competitor.siNumber
        val si2 = o2.competitorCategory.competitor.siNumber

        return when {
            si1 == null && si2 == null -> 0            // Both null, consider them equal
            si1 == null -> 1                            // si1 is null, place it after si2
            si2 == null -> -1                           // si2 is null, place it after si1
            else -> si1.compareTo(si2)                  // Both are non-null, compare by value
        }
    }
}

class CompetitorDrawnStartTimeComparator : Comparator<CompetitorData> {
    override fun compare(o1: CompetitorData, o2: CompetitorData): Int {
        val t1 = o1.competitorCategory.competitor.drawnRelativeStartTime
        val t2 = o2.competitorCategory.competitor.drawnRelativeStartTime

        if (t1 == null && t2 == null) return 0
        else if (t1 == null) return 1
        else if (t2 == null) return -1

        return t1.compareTo(t2)
    }
}

class CompetitorStartTimeComparator : Comparator<CompetitorData> {
    override fun compare(o1: CompetitorData, o2: CompetitorData): Int {
        return o1.readoutData?.result?.startTime?.compareTo(o2.readoutData?.result?.startTime) ?: -1
    }
}

class CompetitorFinishTimeComparator : Comparator<CompetitorData> {
    override fun compare(o1: CompetitorData, o2: CompetitorData): Int {
        return o1.readoutData?.result?.finishTime?.compareTo(o2.readoutData?.result?.finishTime)
            ?: -1
    }
}

class CompetitorRunTimeComparator : Comparator<CompetitorData> {
    override fun compare(o1: CompetitorData, o2: CompetitorData): Int {
        return o1.readoutData?.result?.runTime?.compareTo(o2.readoutData?.result?.runTime) ?: -1
    }
}