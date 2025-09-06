package kolskypavel.ardfmanager.ui.aliases

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.RecyclerView
import kolskypavel.ardfmanager.R
import kolskypavel.ardfmanager.backend.room.entity.Alias
import kolskypavel.ardfmanager.backend.sportident.SIConstants.isSICodeValid
import kolskypavel.ardfmanager.backend.wrappers.AliasEditItemWrapper
import java.util.UUID

class AliasRecyclerViewAdapter(
    var values: ArrayList<AliasEditItemWrapper>,
    val raceId: UUID
) :
    RecyclerView.Adapter<AliasRecyclerViewAdapter.AliasViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AliasViewHolder {
        val adapterLayout = LayoutInflater.from(parent.context)
            .inflate(R.layout.recycler_item_alias, parent, false)

        return AliasViewHolder(adapterLayout)
    }

    override fun getItemCount(): Int = values.size

    override fun onBindViewHolder(holder: AliasViewHolder, position: Int) {
        val item = values[position]
        holder.siCode.setText(item.alias.siCode.toString())
        holder.name.setText(item.alias.name)

        holder.name.doOnTextChanged { cs: CharSequence?, i: Int, i1: Int, i2: Int ->
            try {
                nameWatcher(holder.adapterPosition, cs.toString(), holder.name.context)
            } catch (e: IllegalArgumentException) {
                holder.name.error = e.message
            }
        }

        holder.siCode.doOnTextChanged { cs: CharSequence?, i: Int, i1: Int, i2: Int ->
            try {
                codeWatcher(holder.adapterPosition, cs.toString(), holder.name.context)
            } catch (e: IllegalArgumentException) {
                holder.siCode.error = e.message
            }
        }

        holder.addBtn.setOnClickListener {
            addAlias(holder.adapterPosition)
        }

        holder.deleteBtn.setOnClickListener {
            //Remove focus to prevent crash
            holder.name.clearFocus()
            holder.siCode.clearFocus()
            deleteAlias(holder.adapterPosition)
        }
    }

    private fun codeWatcher(position: Int, code: String, context: Context) {
        if (code.isEmpty()) {
            values[position].isCodeValid = true
            throw IllegalArgumentException(context.getString(R.string.general_required))
        }

        val codeValue = code.toInt();

        if (!isSICodeValid(codeValue)) {
            values[position].isCodeValid = false
            throw IllegalArgumentException(context.getString(R.string.general_invalid))
        }

        if (!isCodeAvailable(codeValue)) {
            values[position].isCodeValid = false
            throw IllegalArgumentException(context.getString(R.string.general_duplicate))
        }

        values[position].isCodeValid = true
        values[position].alias.siCode = if (code.isEmpty()) 0 else code.toInt()
    }

    private fun nameWatcher(position: Int, name: String, context: Context) {
        if (name.isEmpty()) {
            values[position].isNameValid = false
            throw IllegalArgumentException(context.getString(R.string.general_required))
        }

        if (!isNameAvailable(name)) {
            values[position].isNameValid = false
            throw IllegalArgumentException(context.getString(R.string.general_duplicate))
        }

        values[position].isNameValid = true
        values[position].alias.name = name
    }

    private fun isCodeAvailable(code: Int): Boolean = values.all { a -> code != a.alias.siCode }

    private fun isNameAvailable(name: String): Boolean = values.all { a -> name != a.alias.name }

    fun checkFields(): Boolean = values.all { a -> a.isNameValid && a.isCodeValid }

    fun addAlias(position: Int) {
        val aliasWrapper = AliasEditItemWrapper(
            Alias(
                UUID.randomUUID(),
                raceId,
                0,
                ""
            ),
            isCodeValid = false, isNameValid = false
        )

        if (position == values.size - 1) {
            values.add(aliasWrapper)
        } else {
            values.add(position + 1, aliasWrapper)
        }
        notifyItemInserted(position + 1)
    }

    private fun deleteAlias(position: Int) {
        if (position in 0..<values.size) {
            values.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    inner class AliasViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var siCode: EditText = view.findViewById(R.id.alias_item_code)
        var name: EditText = view.findViewById(R.id.alias_item_name)
        var addBtn: ImageButton = view.findViewById(R.id.alias_item_add_btn)
        var deleteBtn: ImageButton =
            view.findViewById(R.id.alias_item_delete_btn)
    }
}