package kolskypavel.ardfmanager.ui.results

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kolskypavel.ardfmanager.R
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CompetitorData
import kolskypavel.ardfmanager.backend.room.enums.ResultStatus
import kolskypavel.ardfmanager.backend.wrappers.ResultWrapper
import kolskypavel.ardfmanager.ui.SelectedRaceViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class ResultsFragmentRecyclerViewAdapter(
    var values: ArrayList<ResultWrapper>,
    var context: Context,
    var selectedRaceViewModel: SelectedRaceViewModel,
    private val openDetail: (competitorData: CompetitorData) -> Unit
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(
    ) {
    val dataProcessor = DataProcessor.get()

    override fun onCreateViewHolder(parent: ViewGroup, child: Int): RecyclerView.ViewHolder {

        return if (child == 0) {
            val rowView: View =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.recycler_item_result_category, parent, false)
            CategoryViewHolder(rowView)
        } else {
            val rowView: View =
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.recycler_item_result_competitor, parent, false)
            CompetitorViewHolder(rowView)
        }
    }

    private fun toggleArrow(expandButton: ImageButton, isExpanded: Boolean) {
        if (isExpanded) {
            expandButton.setImageResource(R.drawable.ic_collapse)
        } else {
            expandButton.setImageResource(R.drawable.ic_expand)
        }
    }

    override fun getItemCount(): Int = values.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val dataList = values[position]

        if (dataList.isChild == 0) {
            holder as CategoryViewHolder
            holder.apply {
                if (dataList.category != null) {
                    categoryName.text =
                        "${dataList.category.name} (${dataList.finished}/${
                            dataList.competitorData.size
                        })"
                } else {
                    categoryName.text =
                        "${context.getText(R.string.no_category)} (${dataList.finished}/${dataList.competitorData.size})"
                }
                if (dataList.competitorData.isNotEmpty()) {
                    expandButton.visibility = View.VISIBLE

                    //Set on click expansion + icon
                    holder.itemView.setOnClickListener {
                        expandOrCollapseParentItem(dataList, position)
                        toggleArrow(holder.expandButton, dataList.isExpanded)
                    }

                    holder.expandButton.setOnClickListener {
                        expandOrCollapseParentItem(dataList, position)
                        toggleArrow(holder.expandButton, dataList.isExpanded)
                    }

                } else {
                    expandButton.visibility = View.GONE
                }
                toggleArrow(holder.expandButton, dataList.isExpanded)
            }

        } else {
            holder as CompetitorViewHolder

            holder.apply {
                val singleResult = dataList.competitorData.first()

                //Set the competitor place
                if (singleResult.readoutData != null) {
                    val res = singleResult.readoutData!!.result
                    competitorPlace.text =
                        if (res.resultStatus == ResultStatus.OK) {
                            "${res.place}."
                        } else {
                            dataProcessor.resultStatusToShortString(res.resultStatus)
                        }
                } else {
                    competitorPlace.text = "-"
                }

                var compName = singleResult.competitorCategory.competitor.getFullName().take(40)

                // Inform that the readout was modified
                if (singleResult.readoutData?.result?.modified == true) {
                    compName += " *"
                }
                competitorName.text = compName

                // Cancel previous timer job if exists
                holder.timerJob?.cancel()

                // Set the competitor time
                val competitor = singleResult.competitorCategory.competitor
                val drawnStartTime = competitor.drawnRelativeStartTime

                if (singleResult.readoutData != null) {
                    holder.competitorTime.text = TimeProcessor.durationToFormattedString(
                        singleResult.readoutData!!.result.runTime,
                        dataProcessor.useMinuteTimeFormat()
                    )
                } else if (drawnStartTime != null) {
                    holder.timerJob = CoroutineScope(Dispatchers.Main).launch {
                        while (true) {
                            selectedRaceViewModel.getCurrentRace()?.let {
                                holder.competitorTime.text =
                                    TimeProcessor.runDurationFromStartString(
                                        it.startDateTime,
                                        drawnStartTime,
                                        dataProcessor, LocalDateTime.now()
                                    )
                            }
                            delay(1000)
                        }
                    }
                } else {
                    holder.competitorTime.text = "-"
                }

                //Set points
                competitorPoints.text = if (singleResult.readoutData?.result?.points != null) {
                    singleResult.readoutData?.result?.points.toString()
                } else {
                    "-"
                }
                holder.itemView.setOnClickListener {
                    if (singleResult.readoutData != null) {
                        openDetail(singleResult)
                    }
                }


                if (dataList.childPosition % 2 == 1)
                    holder.itemView.setBackgroundResource(R.color.light_grey)
                else {
                    holder.itemView.setBackgroundResource(R.color.white)
                }
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is CompetitorViewHolder) {
            holder.timerJob?.cancel()
            holder.timerJob = null
        }
    }

    private fun expandOrCollapseParentItem(singleBoarding: ResultWrapper, position: Int) {
        if (singleBoarding.isExpanded) {
            collapseParentRow(position)
        } else {
            expandParentRow(position)
        }
    }

    private fun expandParentRow(position: Int) {
        val currentBoardingRow = values[position]
        val competitors = currentBoardingRow.competitorData
        currentBoardingRow.isExpanded = true
        var nextPosition = position
        if (currentBoardingRow.isChild == 0) {

            competitors.forEachIndexed { index, service ->
                val parentModel = ResultWrapper(isChild = 1, childPosition = index, finished = 0)
                parentModel.competitorData.add(service)
                values.add(++nextPosition, parentModel)
            }
            notifyDataSetChanged()
        }
    }

    private fun collapseParentRow(position: Int) {
        val currentBoardingRow = values[position]
        val services = currentBoardingRow.competitorData
        values[position].isExpanded = false
        if (values[position].isChild == 0) {
            services.forEach { _ ->
                values.removeAt(position + 1)
            }
            notifyDataSetChanged()
        }
    }

    fun expandAllItems() {
        var index = 0
        while (index < values.size) {
            if (values[index].isExpanded) {
                expandParentRow(index)
            }
            index++
        }
    }

    override fun getItemViewType(position: Int): Int = values[position].isChild

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    class CategoryViewHolder(row: View) : RecyclerView.ViewHolder(row) {
        val categoryName: TextView = row.findViewById(R.id.result_category_name)
        val expandButton: ImageButton = row.findViewById(R.id.down_iv)
    }

    class CompetitorViewHolder(row: View) : RecyclerView.ViewHolder(row) {
        val competitorPlace: TextView = row.findViewById(R.id.result_competitor_place)
        val competitorName: TextView = row.findViewById(R.id.result_competitor_name)
        val competitorTime: TextView = row.findViewById(R.id.result_competitor_time)
        val competitorPoints: TextView = row.findViewById(R.id.result_competitor_points)

        var timerJob: Job? = null
    }
}
