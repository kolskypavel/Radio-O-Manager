package kolskypavel.ardfmanager.ui.races

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import kolskypavel.ardfmanager.R
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import kolskypavel.ardfmanager.backend.room.entity.Race
import java.util.UUID

/**
 * [RecyclerView.Adapter] that can display a [Race].
 */
class RaceRecyclerViewAdapter(
    private var values: List<Race>, private val onRaceClicked: (raceId: UUID) -> Unit,
    private val onMoreClicked: (action: Int, position: Int, race: Race) -> Unit,
    private val context: Context
) : RecyclerView.Adapter<RaceRecyclerViewAdapter.RaceViewHolder>() {

    private val dataProcessor = DataProcessor.get()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RaceViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_item_race, parent, false)

        return RaceViewHolder(adapterLayout)
    }

    override fun onBindViewHolder(holder: RaceViewHolder, position: Int) {
        val item = values[position]
        holder.title.text = item.name
        holder.date.text =
            item.startDateTime.toLocalDate()
                .toString() + " " + TimeProcessor.hoursMinutesFormatter(item.startDateTime)
        holder.type.text = dataProcessor.raceTypeToString(item.raceType)
        holder.level.text = dataProcessor.raceLevelToString(
            item.raceLevel
        )
        holder.itemView.setOnClickListener {
            onRaceClicked(item.id)
        }
        holder.moreBtn.setOnClickListener {

            val popupMenu = PopupMenu(context, holder.moreBtn)
            popupMenu.inflate(R.menu.context_menu_race)

            popupMenu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_item_edit_race -> {
                        onMoreClicked(0, position, item)
                        true
                    }

                    R.id.menu_item_export_race -> {
                        onMoreClicked(1, position, item)
                        true
                    }


                    R.id.menu_item_delete_race -> {
                        onMoreClicked(2, position, item)
                        true
                    }

                    else -> {
                        onMoreClicked(3, position, item)
                        true
                    }
                }
            }
            popupMenu.show()
        }
    }

    override fun getItemCount(): Int = values.size

    inner class RaceViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.race_item_title)
        val date: TextView = view.findViewById(R.id.race_item_date)
        val level: TextView = view.findViewById(R.id.race_item_level)
        val type: TextView = view.findViewById(R.id.race_item_type)
        val moreBtn: ImageButton = view.findViewById(R.id.race_item_more_btn)
    }

}