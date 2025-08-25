package kolskypavel.ardfmanager.ui.readouts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import kolskypavel.ardfmanager.R
import kolskypavel.ardfmanager.backend.room.entity.Punch
import kolskypavel.ardfmanager.backend.room.enums.PunchStatus
import kolskypavel.ardfmanager.backend.room.enums.SIRecordType
import kolskypavel.ardfmanager.backend.sportident.SIConstants
import kolskypavel.ardfmanager.backend.sportident.SITime
import kolskypavel.ardfmanager.backend.wrappers.PunchEditItemWrapper
import java.time.Duration
import java.time.LocalTime
import java.util.UUID

class PunchEditRecyclerViewAdapter(
    var values: ArrayList<PunchEditItemWrapper>
) :
    RecyclerView.Adapter<PunchEditRecyclerViewAdapter.PunchViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PunchViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_item_punch_edit, parent, false)

        return PunchViewHolder(adapterLayout)
    }

    override fun getItemCount() = values.size

    override fun onBindViewHolder(holder: PunchViewHolder, position: Int) {
        val item = values[position]

        holder.time.setText(item.punch.siTime.getTimeString())
        holder.weekday.setText(item.punch.siTime.getDayOfWeek().toString())
        holder.week.setText(item.punch.siTime.getWeek().toString())

        holder.addBtn.setOnClickListener {
            addPunch(holder.adapterPosition)
        }

        holder.deleteBtn.setOnClickListener {
            holder.code.clearFocus()
            holder.time.clearFocus()
            holder.week.clearFocus()
            holder.weekday.clearFocus()
            deletePunch(holder.adapterPosition)
        }

        //Set the start punch
        when (item.punch.punchType) {
            SIRecordType.CHECK -> {}

            SIRecordType.START -> {
                holder.code.setText("S")
                holder.code.isEnabled = false
                holder.deleteBtn.visibility = View.GONE
            }

            SIRecordType.FINISH -> {
                holder.code.setText("F")
                holder.code.isEnabled = false
                holder.addBtn.visibility = View.GONE
                holder.deleteBtn.visibility = View.GONE
            }

            SIRecordType.CONTROL -> {
                if (item.punch.siCode != 0) {
                    holder.code.setText(item.punch.siCode.toString())
                } else {
                    holder.code.setText("")
                }
                holder.code.isEnabled = true
                holder.addBtn.visibility = View.VISIBLE
                holder.deleteBtn.visibility = View.VISIBLE
            }
        }

        //Set watchers
        holder.code.doOnTextChanged { cs: CharSequence?, i: Int, i1: Int, i2: Int ->
            if (!codeWatcher(holder.adapterPosition, cs.toString())) {
                holder.code.error = holder.code.context.getString(R.string.general_invalid)
            }
        }

        holder.time.doOnTextChanged { cs: CharSequence?, i: Int, i1: Int, i2: Int ->
            if (!timeWatcher(holder.adapterPosition, cs.toString())) {
                holder.time.error = holder.code.context.getString(R.string.general_invalid)
            }
        }

        holder.weekday.doOnTextChanged { cs: CharSequence?, i: Int, i1: Int, i2: Int ->
            if (!dayWatcher(holder.adapterPosition, cs.toString())) {
                holder.weekday.error = holder.code.context.getString(R.string.general_invalid)
            }
        }

        holder.week.doOnTextChanged { cs: CharSequence?, i: Int, i1: Int, i2: Int ->
            if (!weekWatcher(holder.adapterPosition, cs.toString())) {
                holder.week.error = holder.code.context.getString(R.string.general_invalid)
            }
        }
    }

    private fun addPunch(position: Int) {
        values.add(
            position + 1, PunchEditItemWrapper(
                Punch(
                    UUID.randomUUID(),
                    values[0].punch.raceId,
                    null,
                    null,
                    0,
                    SITime(values[position].punch.siTime),
                    SITime(values[position].punch.siTime),
                    SIRecordType.CONTROL,
                    values[position].punch.order++,
                    PunchStatus.UNKNOWN, Duration.ZERO,
                ), false, true, true, true
            )
        )
        notifyItemInserted(position + 1)
    }

    private fun deletePunch(position: Int) {
        values.removeAt(position)
        notifyItemRemoved(position)
    }

    //Text watchers
    private fun codeWatcher(position: Int, text: String): Boolean {
        try {
            val code = text.toInt()
            if (SIConstants.isSICodeValid(code)) {
                values[position].punch.siCode = code
                values[position].isCodeValid = true
            } else {
                values[position].isCodeValid = false
                return false
            }
        } catch (e: Exception) {
            values[position].isCodeValid = false
            return false
        }
        return true
    }


    private fun timeWatcher(position: Int, text: String): Boolean {
        //Try parsing the time into SI time
        try {
            val time = LocalTime.parse(text)
            values[position].punch.siTime.setTime(time)
            values[position].isTimeValid = true
        } catch (e: Exception) {
            values[position].isTimeValid = false
            return false
        }
        return true
    }

    private fun dayWatcher(position: Int, text: String): Boolean {
        try {
            val day = text.toInt()
            if (day in 0..7) {
                values[position].punch.siTime.setDayOfWeek(day)
                values[position].isDayValid = true
            }
        } catch (e: Exception) {
            values[position].isDayValid = false
            return false
        }
        return true
    }

    private fun weekWatcher(position: Int, text: String): Boolean {
        try {
            val week = text.toInt()
            if (week in 0..3) {
                values[position].punch.siTime.setWeek(week)
                values[position].isWeekValid = true
            }
        } catch (e: Exception) {
            values[position].isWeekValid = false
            return false
        }
        return true
    }

    fun isValid(): Boolean {
        for (item in values) {
            if (!item.isCodeValid || !item.isTimeValid || !item.isDayValid || !item.isWeekValid) {
                return false
            }
        }
        return true
    }

    inner class PunchViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var code: EditText = view.findViewById(R.id.punch_edit_item_si_code)
        var time: EditText = view.findViewById(R.id.punch_edit_item_time)
        var weekday: EditText = view.findViewById(R.id.punch_edit_item_weekday)
        var week: EditText = view.findViewById(R.id.punch_edit_item_week)
        var addBtn: ImageButton = view.findViewById(R.id.punch_edit_item_add_btn)
        var deleteBtn: ImageButton = view.findViewById(R.id.punch_edit_item_delete_btn)
    }

}