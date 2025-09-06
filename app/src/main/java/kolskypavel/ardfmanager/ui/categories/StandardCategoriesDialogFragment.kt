package kolskypavel.ardfmanager.ui.categories

import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import kolskypavel.ardfmanager.R
import kolskypavel.ardfmanager.backend.room.enums.StandardCategoryType
import kolskypavel.ardfmanager.ui.SelectedRaceViewModel

class StandardCategoriesDialogFragment : DialogFragment() {
    private lateinit var selectedRaceViewModel: SelectedRaceViewModel
    private lateinit var presetGroup: RadioGroup
    private lateinit var okButton: Button
    private lateinit var cancelButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_standard_categories, container, false)
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
        setWidthPercent(95)
        setStyle(STYLE_NORMAL, R.style.add_dialog)
        dialog?.setTitle(R.string.category_create_standard_categories)

        val sl: SelectedRaceViewModel by activityViewModels()
        selectedRaceViewModel = sl

        okButton = view.findViewById(R.id.standard_cat_dialog_ok)
        cancelButton = view.findViewById(R.id.standard_cat_dialog_cancel)
        presetGroup = view.findViewById(R.id.standard_cat_dialog_radio_group)
        setButtons()
    }

    private fun setButtons() {
        presetGroup.check(R.id.standard_cat_dialog_btn_international)
        okButton.setOnClickListener {

            val currentCheck = when (presetGroup.checkedRadioButtonId) {
                R.id.standard_cat_dialog_btn_czech -> StandardCategoryType.CZECH
                else -> StandardCategoryType.INTERNATIONAL
            }

            selectedRaceViewModel.getCurrentRace()?.let { it1 ->
                selectedRaceViewModel.createStandardCategories(
                    currentCheck,
                    it1.id
                )
                dialog?.dismiss()
            }
        }
        cancelButton.setOnClickListener {
            dialog?.cancel()
        }
    }
}