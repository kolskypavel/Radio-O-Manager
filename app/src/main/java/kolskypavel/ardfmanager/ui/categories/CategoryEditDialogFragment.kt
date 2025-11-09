package kolskypavel.ardfmanager.ui.categories

import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kolskypavel.ardfmanager.R
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.helpers.ControlPointsHelper
import kolskypavel.ardfmanager.backend.room.entity.Category
import kolskypavel.ardfmanager.backend.room.enums.RaceType
import kolskypavel.ardfmanager.ui.SelectedRaceViewModel
import java.time.Duration
import java.util.UUID


class CategoryEditDialogFragment : DialogFragment() {

    private val args: CategoryEditDialogFragmentArgs by navArgs()
    private lateinit var selectedRaceViewModel: SelectedRaceViewModel
    private val dataProcessor = DataProcessor.get()
    private lateinit var category: Category

    private lateinit var nameEditText: TextInputEditText
    private lateinit var samePropertiesCheckBox: CheckBox
    private lateinit var raceTypeLayout: TextInputLayout
    private lateinit var raceTypePicker: MaterialAutoCompleteTextView
    private lateinit var limitEditText: TextInputEditText
    private lateinit var limitLayout: TextInputLayout
    private lateinit var genderPicker: MaterialAutoCompleteTextView
    private lateinit var bandLayout: TextInputLayout
    private lateinit var bandPicker: MaterialAutoCompleteTextView
    private lateinit var maxAgeLayout: TextInputLayout
    private lateinit var maxAgeEditText: TextInputEditText
    private lateinit var lengthEditText: TextInputEditText
    private lateinit var climbEditText: TextInputEditText
    private lateinit var controlPointsLayout: TextInputLayout
    private lateinit var controlPointsEditText: TextInputEditText

    private lateinit var okButton: Button
    private lateinit var cancelButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_edit_category, container, false)
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

        nameEditText = view.findViewById(R.id.category_dialog_name)
        samePropertiesCheckBox = view.findViewById(R.id.category_dialog_same_properties_checkbox)
        raceTypeLayout = view.findViewById(R.id.category_dialog_type_layout)
        limitEditText = view.findViewById(R.id.category_dialog_limit)
        limitLayout = view.findViewById(R.id.category_dialog_limit_layout)
        raceTypePicker = view.findViewById(R.id.category_dialog_type)
        bandLayout = view.findViewById(R.id.category_dialog_band_layout)
        bandPicker = view.findViewById(R.id.category_dialog_band)
        genderPicker = view.findViewById(R.id.category_gender)
        maxAgeLayout = view.findViewById(R.id.category_dialog_max_age_layout)
        maxAgeEditText = view.findViewById(R.id.category_dialog_max_age)
        lengthEditText = view.findViewById(R.id.category_dialog_length)
        climbEditText = view.findViewById(R.id.category_dialog_climb)
        controlPointsLayout = view.findViewById(R.id.category_dialog_control_points_layout)
        controlPointsEditText =
            view.findViewById(R.id.category_dialog_control_points)

        cancelButton = view.findViewById(R.id.category_dialog_cancel)
        okButton = view.findViewById(R.id.category_dialog_ok)

        populateFields()
        setButtons()
    }

    /**
     * Populate the data fields - text views, pickers
     */
    private fun populateFields() {
        val race = args.race

        if (args.create) {
            val order = selectedRaceViewModel.getHighestCategoryOrder(race.id) + 1

            dialog?.setTitle(R.string.category_create)
            category = Category(
                UUID.randomUUID(),
                race.id,
                "", isMan = false,
                null,
                0,
                0,
                order,
                false,
                race.raceType,
                race.raceBand,
                race.timeLimit,
                args.controlPoints
            )

            //Preset the data from the race
            raceTypePicker.setText(
                dataProcessor.raceTypeToString(race.raceType),
                false
            )
            bandPicker.setText(
                dataProcessor.raceBandToString(race.raceBand),
                false
            )
            limitEditText.setText(race.timeLimit.toMinutes().toString())

            raceTypeLayout.isEnabled = false
            bandLayout.isEnabled = false
            limitLayout.isEnabled = false
        }

        //Edit category
        else {
            dialog?.setTitle(R.string.category_edit)
            category = args.category!!
            nameEditText.setText(category.name)

            if (category.maxAge != null) {
                maxAgeEditText.setText(category.maxAge.toString())
            }

            if (category.length != 0) {
                lengthEditText.setText(category.length.toString())
            }

            if (category.climb != 0) {
                climbEditText.setText(category.climb.toString())
            }

            // Custom properties
            if (category.differentProperties) {
                samePropertiesCheckBox.isChecked = false
            } else {
                raceTypeLayout.isEnabled = false
                limitLayout.isEnabled = false
                bandLayout.isEnabled = false
            }

            raceTypePicker.setText(
                dataProcessor.raceTypeToString(category.raceType ?: race.raceType),
                false
            )
            bandPicker.setText(
                dataProcessor.raceBandToString(category.categoryBand ?: race.raceBand),
                false
            )
            limitEditText.setText(
                if (category.timeLimit != null) {
                    category.timeLimit!!.toMinutes().toString()
                } else {
                    race.timeLimit.toMinutes().toString()
                }
            )
        }

        //Set gender
        genderPicker.setText(dataProcessor.genderToString(category.isMan), false)
        controlPointsEditText.setText(category.controlPointsString)

        //TODO: Process the saving - this is just to preserve the filtering after screen rotation
        raceTypePicker.isSaveEnabled = false
    }


    private fun raceTypeWatcher(position: Int) {
        category.raceType = RaceType.getByValue(position)
    }

    private fun checkFields(): Boolean {
        var valid = true

        if (nameEditText.text?.isBlank() == true) {
            nameEditText.error = getString(R.string.general_required)
            valid = false
        }
        //Check if the name is unique
        else {
            val name = nameEditText.text.toString()
            val orig = selectedRaceViewModel.getCategoryByName(name, args.race.id)
            if (orig != null && orig.id != category.id) {
                valid = false
                nameEditText.error = getString(R.string.category_exists)
            }
        }

        if (!samePropertiesCheckBox.isChecked) {
            if (limitEditText.text?.isBlank() == false) {
                try {
                    Duration.ofMinutes(limitEditText.text.toString().toLong())
                } catch (e: Exception) {
                    limitEditText.error = getString(R.string.general_invalid)
                    valid = false
                }
            } else {
                limitEditText.error = getString(R.string.general_required)
                valid = false
            }
        }

        if (controlPointsEditText.text.toString().isNotBlank()) {
            val text = controlPointsEditText.text.toString().trim()

            try {
                ControlPointsHelper.getControlPointsFromString(
                    text,
                    category.id,
                    category.raceType ?: args.race.raceType,
                    requireContext()
                )
            } catch (e: Exception) {
                controlPointsLayout.error = e.message
                valid = false
            }
        }
        return valid
    }

    private fun setButtons() {
        val race = args.race

        controlPointsEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                controlPointsLayout.error = ""
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        //Set the race type checkbox functionality
        samePropertiesCheckBox.setOnClickListener {

            if (samePropertiesCheckBox.isChecked) {
                raceTypePicker.setText(
                    dataProcessor.raceTypeToString(race.raceType),
                    false
                )
                raceTypeWatcher(race.raceType.value)
                bandPicker.setText(
                    dataProcessor.raceBandToString(race.raceBand),
                    false
                )
                limitEditText.setText(race.timeLimit.toMinutes().toString())

                raceTypeLayout.isEnabled = false
                bandLayout.isEnabled = false
                limitLayout.isEnabled = false
            }

            //Hide the shading and enable input
            else {
                raceTypeLayout.isEnabled = true
                limitLayout.isEnabled = true
                bandLayout.isEnabled = true
                raceTypePicker.setOnItemClickListener { _, _, position, _ ->
                    raceTypeWatcher(position)
                }
            }
        }

        okButton.setOnClickListener {
            if (checkFields()) {
                category.name = nameEditText.text.toString().trim()

                if (maxAgeEditText.text.toString().isNotBlank()) {
                    category.maxAge = (maxAgeEditText.text.toString().trim()).toInt()
                } else {
                    category.maxAge = null
                }

                if (lengthEditText.text?.isBlank() == false) {
                    category.length = lengthEditText.text.toString().trim().toInt()
                }
                if (climbEditText.text?.isBlank() == false) {
                    category.climb = climbEditText.text.toString().trim().toInt()
                }

                //Set the data from pickers
                category.isMan = getGenderFromPicker()

                category.differentProperties = !samePropertiesCheckBox.isChecked
                if (category.differentProperties) {
                    category.raceType =
                        dataProcessor.raceTypeStringToEnum(raceTypePicker.text.toString())
                    category.categoryBand =
                        dataProcessor.raceBandStringToEnum(bandPicker.text.toString())
                    category.timeLimit = Duration.ofMinutes(limitEditText.text.toString().toLong())
                } else {
                    category.raceType = null
                    category.timeLimit = null
                }
                val controlPointsString =
                    controlPointsEditText.text.toString().trim()
                //Get control points
                val controlPoints = ControlPointsHelper.getControlPointsFromString(
                    controlPointsString,
                    category.id,
                    category.raceType ?: race.raceType,
                    requireContext()
                )
                selectedRaceViewModel.createOrUpdateCategory(category, controlPoints)

                setFragmentResult(
                    REQUEST_CATEGORY_MODIFICATION, bundleOf(
                        BUNDLE_KEY_CREATE to args.create,
                        BUNDLE_KEY_POSITION to args.position,
                        BUNDLE_KEY_CATEGORY_ID to category.id.toString()
                    )
                )
                dialog?.dismiss()
            }
        }

        cancelButton.setOnClickListener {
            dialog?.cancel()
        }
    }

    fun getGenderFromPicker(): Boolean {
        return dataProcessor.genderFromString(genderPicker.text.toString())
    }

    companion object {
        const val REQUEST_CATEGORY_MODIFICATION = "REQUEST_CATEGORY_MODIFICATION"
        const val BUNDLE_KEY_CREATE = "BUNDLE_KEY_CREATE"
        const val BUNDLE_KEY_POSITION = "BUNDLE_KEY_POSITION"
        const val BUNDLE_KEY_CATEGORY_ID = "BUNDLE_KEY_CATEGORY_ID"
    }
}