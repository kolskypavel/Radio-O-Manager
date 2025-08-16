package kolskypavel.ardfmanager.ui.readouts

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kolskypavel.ardfmanager.R
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.AliasPunch
import kolskypavel.ardfmanager.backend.room.enums.PunchStatus
import kolskypavel.ardfmanager.backend.room.enums.SIRecordType

class PunchRecyclerViewAdapter(
    private var values: List<AliasPunch>,
    private val context: Context
) :
    RecyclerView.Adapter<PunchRecyclerViewAdapter.PunchViewHolder>() {
    private val dataProcessor = DataProcessor.get()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PunchViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_item_punch, parent, false)

        return PunchViewHolder(adapterLayout)
    }

    override fun getItemCount() = values.size

    override fun onBindViewHolder(holder: PunchViewHolder, position: Int) {
        val item = values[position]

        holder.punchRealTime.text = item.punch.siTime.getTimeString()
        holder.punchSplit.text = TimeProcessor.durationToFormattedString(
            item.punch.split,
            dataProcessor.useMinuteTimeFormat()
        )

        //Set the fields, based on the type of the punch
        when (item.punch.punchType) {
            SIRecordType.START -> {
                holder.punchSiCode.text = context.getText(R.string.punch_type_start)
            }

            SIRecordType.FINISH -> {
                holder.punchSiCode.text = context.getText(R.string.punch_type_finish)
            }

            else -> {
                holder.punchOrder.text = position.toString()
                var cpDisplay = item.punch.siCode.toString()

                if (item.alias != null) {
                    cpDisplay += " (${item.alias!!.name})"
                }
                holder.punchSiCode.text = cpDisplay
                holder.punchStatus.text = when (item.punch.punchStatus) {
                    PunchStatus.VALID -> context.getString(R.string.punch_status_valid)
                    PunchStatus.INVALID -> context.getString(R.string.punch_status_invalid)
                    PunchStatus.DUPLICATE -> context.getString(R.string.punch_status_duplicate)
                    PunchStatus.UNKNOWN -> context.getString(R.string.punch_status_unknown)
                }
            }
        }
    }

    inner class PunchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var punchOrder: TextView = view.findViewById(R.id.punch_item_order)
        var punchSiCode: TextView = view.findViewById(R.id.punch_item_si_code)
        var punchRealTime: TextView = view.findViewById(R.id.punch_item_real_time)
        var punchSplit: TextView = view.findViewById(R.id.punch_item_split)
        var punchStatus: TextView = view.findViewById(R.id.punch_item_status)
    }
}