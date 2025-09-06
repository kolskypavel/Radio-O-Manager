package kolskypavel.ardfmanager.ui.aliases

import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import kolskypavel.ardfmanager.R
import kolskypavel.ardfmanager.backend.wrappers.AliasEditItemWrapper
import kolskypavel.ardfmanager.ui.SelectedRaceViewModel

class AliasEditDialogFragment : DialogFragment() {
    private lateinit var selectedRaceViewModel: SelectedRaceViewModel
    private val args: AliasEditDialogFragmentArgs by navArgs()

    private lateinit var addButton: ImageButton
    private lateinit var okButton: Button
    private lateinit var cancelButton: Button
    private lateinit var aliasRecyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_manage_aliases, container, false)
    }

    private fun DialogFragment.setWidthPercent(percentage: Int) {
        val percent = percentage.toFloat() / 100
        val dm = Resources.getSystem().displayMetrics
        val rect = dm.run { Rect(0, 0, widthPixels, heightPixels) }
        val percentWidth = rect.width() * percent
        dialog?.window?.setLayout(percentWidth.toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.add_dialog)
        setWidthPercent(95)

        val sl: SelectedRaceViewModel by activityViewModels()
        selectedRaceViewModel = sl

        addButton = view.findViewById(R.id.alias_dialog_add_btn)
        cancelButton = view.findViewById(R.id.alias_dialog_cancel)
        okButton = view.findViewById(R.id.alias_dialog_ok)
        aliasRecyclerView = view.findViewById(R.id.alias_recycler_view)

        dialog?.setTitle(getString(R.string.category_manage_aliases))
        setAdapter()
        setButtons()

        addButton.setOnClickListener {
            (aliasRecyclerView.adapter as AliasRecyclerViewAdapter).addAlias(-1)
        }
    }

    private fun setAdapter() {
        val aliases =
            selectedRaceViewModel.getAliasesByRace(args.raceId)
        aliasRecyclerView.adapter =
            AliasRecyclerViewAdapter(
                AliasEditItemWrapper.getWrappers(
                    ArrayList(aliases)
                ), args.raceId
            )
    }

    private fun setButtons() {
        cancelButton.setOnClickListener {
            dialog?.cancel()
        }

        okButton.setOnClickListener {
            val adapter = (aliasRecyclerView.adapter as AliasRecyclerViewAdapter)

            if (adapter.checkFields()) {
                val values =
                    AliasEditItemWrapper.getAliases((aliasRecyclerView.adapter as AliasRecyclerViewAdapter).values)
                selectedRaceViewModel.createOrUpdateAliases(values, args.raceId)
                dialog?.dismiss()
            }
        }
    }
}