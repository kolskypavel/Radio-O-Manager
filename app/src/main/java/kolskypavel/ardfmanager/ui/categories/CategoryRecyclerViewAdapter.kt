package kolskypavel.ardfmanager.ui.categories

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
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CategoryData
import kolskypavel.ardfmanager.backend.room.enums.RaceBand
import kolskypavel.ardfmanager.backend.room.enums.RaceType
import kolskypavel.ardfmanager.ui.SelectedRaceViewModel

class CategoryRecyclerViewAdapter(
    private var values: List<CategoryData>,
    private val onMoreClicked: (action: Int, position: Int, categoryData: CategoryData) -> Unit,
    private val context: Context,
    private val selectedRaceViewModel: SelectedRaceViewModel
) :
    RecyclerView.Adapter<CategoryRecyclerViewAdapter.CategoryViewHolder>() {

    private val dataProcessor = DataProcessor.get()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_item_category, parent, false)

        return CategoryViewHolder(adapterLayout)
    }

    override fun getItemCount(): Int {
        return values.size
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val item = values[position]
        holder.title.text = item.category.name
        holder.numCompeititors.text =
            "(${item.competitors.size} ${
                context.getString(R.string.general_competitors).lowercase()
            })"
        holder.type.text = dataProcessor.raceTypeToString(
            item.category.raceType ?: (selectedRaceViewModel.getCurrentRace()?.raceType
                ?: RaceType.CLASSIC)
        )

        holder.band.text = dataProcessor.raceBandToString(
            item.category.categoryBand ?: (selectedRaceViewModel.getCurrentRace()?.raceBand
                ?: RaceBand.M80)
        )
        holder.gender.text = dataProcessor.genderToString(item.category.isMan)
        holder.siCodes.text = item.category.controlPointsString

        if (item.category.maxAge != null) {
            holder.maxAge.text = item.category.maxAge.toString()
        }

        holder.moreBtn.setOnClickListener {

            val popupMenu = PopupMenu(context, holder.moreBtn)
            popupMenu.inflate(R.menu.context_menu_category)

            popupMenu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_item_edit_category -> {
                        onMoreClicked(0, position, item)
                        true
                    }

                    R.id.menu_item_duplicate_category -> {
                        onMoreClicked(1, position, item)
                        true
                    }

                    R.id.menu_item_delete_category -> {
                        onMoreClicked(2, position, item)
                        true
                    }

                    else -> {
                        false
                    }
                }
            }
            popupMenu.show()
        }

        holder.upBtn.setOnClickListener {
            if (holder.layoutPosition != 0) {
                moveCategory(holder.layoutPosition, true)
            }
        }

        holder.downBtn.setOnClickListener {
            if (holder.layoutPosition != values.size - 1)
                moveCategory(holder.layoutPosition, false)
        }

        //Hide the buttons for last and first item
        if (position == 0) {
            holder.upBtn.visibility = View.GONE
        } else if (position == values.size - 1) {
            holder.downBtn.visibility = View.GONE
        }
    }

    /**
     * Changes the category's order and saves it to database
     */
    private fun moveCategory(position: Int, up: Boolean) {
        if (up) {
            values[position - 1].category.order++
            values[position].category.order--
            selectedRaceViewModel.createOrUpdateCategory(values[position - 1].category, null)
            selectedRaceViewModel.createOrUpdateCategory(values[position].category, null)
            notifyItemMoved(position, position - 1)
        } else {
            values[position + 1].category.order--
            values[position].category.order++
            selectedRaceViewModel.createOrUpdateCategory(values[position + 1].category, null)
            selectedRaceViewModel.createOrUpdateCategory(values[position].category, null)
            notifyItemMoved(position, position + 1)
        }
    }

    inner class CategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var title: TextView = view.findViewById(R.id.category_item_title)
        var type: TextView = view.findViewById(R.id.category_item_type)
        var band: TextView = view.findViewById(R.id.category_item_band)
        var numCompeititors: TextView = view.findViewById(R.id.category_item_competitor_number)
        var gender: TextView = view.findViewById(R.id.category_item_gender)
        var maxAge: TextView = view.findViewById(R.id.category_item_max_age)
        var siCodes: TextView = view.findViewById(R.id.category_item_codes)
        var upBtn: ImageButton = view.findViewById(R.id.category_item_up_btn)
        var moreBtn: ImageButton = view.findViewById(R.id.category_item_more_btn)
        var downBtn: ImageButton = view.findViewById(R.id.category_item_down_btn)
    }
}