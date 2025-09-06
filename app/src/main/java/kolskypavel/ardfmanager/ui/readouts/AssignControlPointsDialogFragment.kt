package kolskypavel.ardfmanager.ui.readouts

import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kolskypavel.ardfmanager.R
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.helpers.ControlPointsHelper
import kolskypavel.ardfmanager.backend.room.entity.Category
import kolskypavel.ardfmanager.backend.room.entity.ControlPoint
import kolskypavel.ardfmanager.ui.SelectedRaceViewModel

class AssignControlPointsDialogFragment : DialogFragment() {
    private val args: AssignControlPointsDialogFragmentArgs by navArgs()
    private lateinit var selectedRaceViewModel: SelectedRaceViewModel
    private val dataProcessor = DataProcessor.get()

    private lateinit var categories: List<Category>
    private val categoryArr = ArrayList<String>()
    private lateinit var categoryPicker: MaterialAutoCompleteTextView
    private lateinit var categoryLayout: TextInputLayout
    private lateinit var controlPointsLayout: TextInputLayout
    private lateinit var controlPointsEditText: TextInputEditText

    private lateinit var okButton: Button
    private lateinit var cancelButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_assign_control_points, container, false)
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
        setWidthPercent(98)

        val sl: SelectedRaceViewModel by activityViewModels()
        selectedRaceViewModel = sl
        categories = selectedRaceViewModel.getCategories()

        categoryLayout = view.findViewById(R.id.assign_dialog_category_layout)
        categoryPicker = view.findViewById(R.id.assign_dialog_category)
        controlPointsLayout = view.findViewById(R.id.assign_dialog_control_points_layout)
        controlPointsEditText = view.findViewById(R.id.assign_dialog_control_points)

        okButton = view.findViewById(R.id.assign_dialog_ok)
        cancelButton = view.findViewById(R.id.assign_dialog_cancel)

        populateFields()
        setButtons()
    }

    private fun populateFields() {
        dialog?.setTitle(R.string.readout_assign_control_points)

        // Set category adapter
        for (cat in categories) {
            categoryArr.add(cat.name)
        }
        val categoriesAdapter: ArrayAdapter<String> =
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categoryArr)

        categoryPicker.setAdapter(categoriesAdapter)
        categoryPicker.setText(categoryArr.first(), false)

        // Set control point strings
        controlPointsEditText.setText(args.controlPointsString)
    }

    private fun setButtons() {
        // Set watcher to reset the control points string

        controlPointsEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                controlPointsLayout.error = ""
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        okButton.setOnClickListener {
            if (validateFields()) {
                val currCat = getCurrentCategory()
                val controlPoints = getControlPoints(currCat)

                selectedRaceViewModel.createOrUpdateCategory(currCat, controlPoints)
                dialog?.dismiss()
            }
        }

        cancelButton.setOnClickListener {
            dialog?.cancel()
        }
    }

    private fun validateFields(): Boolean {
        if (controlPointsEditText.text.toString().isNotBlank()) {

            try {
                val currCat = getCurrentCategory()
                getControlPoints(currCat)
                return true
            } catch (e: Exception) {
                controlPointsLayout.error = e.message
            }
        }
        return false
    }

    private fun getCurrentCategory(): Category {
        val catPos = categoryArr.indexOf(categoryPicker.text.toString()).or(0)
        return categories[catPos]
    }

    private fun getControlPoints(category: Category): List<ControlPoint> {
        val controlPointsString =
            controlPointsEditText.text.toString().trim()

        selectedRaceViewModel.getCurrentRace()?.let { race ->
            return ControlPointsHelper.getControlPointsFromString(
                controlPointsString,
                category.id,
                category.raceType ?: race.raceType,
                requireContext()
            )
        }
        return emptyList()
    }
}