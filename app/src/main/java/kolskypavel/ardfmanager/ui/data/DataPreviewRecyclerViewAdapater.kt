package kolskypavel.ardfmanager.ui.data;

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kolskypavel.ardfmanager.R
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.constants.DataType
import kolskypavel.ardfmanager.backend.files.wrappers.DataImportWrapper
import kolskypavel.ardfmanager.backend.helpers.ControlPointsHelper
import kolskypavel.ardfmanager.backend.helpers.TimeProcessor

class DataPreviewRecyclerViewAdapater(
    var value: DataImportWrapper,
    var dataType: DataType
) :
    RecyclerView.Adapter<DataPreviewRecyclerViewAdapater.DataPreviewViewHolder>() {
    private val dataProcessor = DataProcessor.get()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataPreviewViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_item_data, parent, false)

        return DataPreviewViewHolder(adapterLayout)
    }

    //Select the right number of elements
    override fun getItemCount(): Int {
        return when (dataType) {
            DataType.CATEGORIES -> value.categories.size
            DataType.COMPETITORS, DataType.COMPETITOR_STARTS ->
                if (value.competitorCategories.size < 5) {
                    value.competitorCategories.size
                } else {
                    5
                }

            else -> 0
        }
    }

    override fun onBindViewHolder(holder: DataPreviewViewHolder, position: Int) {
        when (dataType) {
            DataType.CATEGORIES -> {
                val item = value.categories[position]
                holder.columnOne.text = item.category.name
                holder.columnTwo.text = dataProcessor.genderToString(item.category.isMan)
                holder.columnThree.text = item.category.maxAge.toString()
                holder.columnFour.text =
                    ControlPointsHelper.getStringFromControlPoints(item.controlPoints)
            }

            DataType.COMPETITORS -> {
                val item = value.competitorCategories[position]
                holder.columnOne.text = item.competitor.siNumber?.toString() ?: "-"
                holder.columnTwo.text = item.competitor.getFullName()
                holder.columnThree.text = item.competitor.birthYear.toString()
                holder.columnFour.text = item.category?.name ?: "-"
            }

            DataType.COMPETITOR_STARTS -> {
                val item = value.competitorCategories[position]
                holder.columnOne.text = if (item.competitor.drawnRelativeStartTime != null) {
                    TimeProcessor.durationToFormattedString(
                        item.competitor.drawnRelativeStartTime!!, true
                    )
                } else {
                    "-"
                }
                holder.columnTwo.text = item.competitor.getFullName()
                holder.columnThree.text = item.competitor.siNumber?.toString() ?: "-"
                holder.columnFour.text = item.category?.name ?: "-"
            }

            else -> {}
        }
    }

    inner class DataPreviewViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var columnOne: TextView = view.findViewById(R.id.data_import_item_column_1)
        var columnTwo: TextView = view.findViewById(R.id.data_import_item_column_2)
        var columnThree: TextView = view.findViewById(R.id.data_import_item_column_3)
        var columnFour: TextView = view.findViewById(R.id.data_import_item_column_4)
    }

}
