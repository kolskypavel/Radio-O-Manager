package kolskypavel.ardfmanager.ui.readouts

import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import kolskypavel.ardfmanager.R
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.room.entity.Category
import kolskypavel.ardfmanager.backend.room.entity.Competitor
import kolskypavel.ardfmanager.backend.room.entity.Punch
import kolskypavel.ardfmanager.backend.room.entity.Result
import kolskypavel.ardfmanager.backend.room.enums.PunchStatus
import kolskypavel.ardfmanager.backend.room.enums.ResultStatus
import kolskypavel.ardfmanager.backend.room.enums.SIRecordType
import kolskypavel.ardfmanager.backend.sportident.SITime
import kolskypavel.ardfmanager.backend.wrappers.PunchEditItemWrapper
import kolskypavel.ardfmanager.ui.SelectedRaceViewModel
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class ReadoutEditDialogFragment : DialogFragment() {
    private val args: ReadoutEditDialogFragmentArgs by navArgs()
    private lateinit var selectedRaceViewModel: SelectedRaceViewModel
    private val dataProcessor = DataProcessor.get()

    private lateinit var result: Result
    private var origResult: Result? = null
    private var modified = false    // If the readout was modified or not

    private lateinit var competitors: List<Competitor>
    private lateinit var categories: List<Category>
    private val competitorArr = ArrayList<String>()
    private val categoryArr = ArrayList<String>()
    private var competitor: Competitor? = null
    private var origCategoryId: UUID? = null

    private lateinit var competitorPicker: MaterialAutoCompleteTextView
    private lateinit var competitorPickerLayout: TextInputLayout
    private lateinit var siNumberView: TextView
    private lateinit var categoryPicker: MaterialAutoCompleteTextView
    private lateinit var categoryPickerLayout: TextInputLayout
    private lateinit var raceStatusPicker: MaterialAutoCompleteTextView
    private val statusArr = ArrayList<String>()
    private lateinit var editSwitch: SwitchMaterial
    private lateinit var punchEditRecyclerView: RecyclerView
    private lateinit var okButton: Button
    private lateinit var cancelButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_edit_readout, container, false)
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
        competitors = selectedRaceViewModel.getCompetitors().sortedBy { com -> com.lastName }
        categories = selectedRaceViewModel.getCategories()

        competitorPicker = view.findViewById(R.id.readout_dialog_competitor)
        competitorPickerLayout = view.findViewById(R.id.readout_dialog_competitor_layout)
        siNumberView = view.findViewById(R.id.readout_dialog_si_number)
        categoryPicker = view.findViewById(R.id.readout_dialog_category)
        categoryPickerLayout = view.findViewById(R.id.readout_dialog_category_layout)
        raceStatusPicker = view.findViewById(R.id.readout_dialog_status)
        editSwitch = view.findViewById(R.id.readout_dialog_edit_switch)
        punchEditRecyclerView = view.findViewById(R.id.readout_dialog_punch_recycler_view)
        okButton = view.findViewById(R.id.readout_dialog_ok)
        cancelButton = view.findViewById(R.id.readout_dialog_cancel)

        populateFields()
        setButtons()
    }

    private fun populateFields() {
        if (args.create) {
            dialog?.setTitle(R.string.readout_create_readout)

            result =
                Result(
                    UUID.randomUUID(),
                    selectedRaceViewModel.getCurrentRace().id,
                    null,
                    null,
                    0,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    LocalDateTime.now(),
                    true,
                    ResultStatus.NO_RANKING,
                    0,
                    Duration.ZERO,
                    true,
                    false
                )

            raceStatusPicker.setText(getString(R.string.general_automatic), false)
            competitorPicker.setText(getString(R.string.readout_unknown_competitor), false)
            editSwitch.visibility = View.GONE
            punchEditRecyclerView.visibility = View.VISIBLE
            modified = true     // Manually created readout is always modified

        } else {
            dialog?.setTitle(R.string.readout_edit_readout)
            result = args.resultData!!.result
            origResult = result

            siNumberView.text = requireContext().getString(
                R.string.readout_si_number,
                result.siNumber ?: "?"
            )

            if (!args.resultData!!.result.automaticStatus) {
                raceStatusPicker.setText(
                    dataProcessor.resultStatusToString(args.resultData!!.result.resultStatus),
                    false
                )
            } else {
                raceStatusPicker.setText(getString(R.string.general_automatic), false)
            }

            if (result.competitorID != null) {
                competitor = selectedRaceViewModel.getCompetitor(result.competitorID!!)
                competitorPicker.setText(competitor?.getNameWithStartNumber())
            } else {
                competitorPicker.setText(getString(R.string.readout_unknown_competitor), false)
            }
        }

        // Category setup
        for (cat in categories) {
            categoryArr.add(cat.name)
        }

        categoryArr.add(
            0,
            getString(R.string.readout_unknown_category)
        )

        val categoryAdapter: ArrayAdapter<String> =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                categoryArr
            )

        categoryPicker.setAdapter(categoryAdapter)
        setCategoryPicker()

        // Competitor setup
        for (comp in competitors) {
            competitorArr.add("${comp.getFullName()} (${comp.startNumber})")
        }
        competitorArr.add(
            0,
            getString(R.string.readout_unknown_competitor)
        ) //Add the empty competitor option
        val competitorAdapter: ArrayAdapter<String> =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                competitorArr
            )

        competitorPicker.setAdapter(competitorAdapter)

        // Punches setup
        var punchWrappers = ArrayList<PunchEditItemWrapper>()

        if (args.create || args.resultData?.punches?.isEmpty() == true) {
            punchWrappers.add(
                PunchEditItemWrapper(
                    Punch(
                        UUID.randomUUID(),
                        dataProcessor.getCurrentRace().id,
                        null,
                        null,
                        0,
                        SITime(LocalTime.MIN),
                        SITime(LocalTime.MIN),
                        SIRecordType.START,
                        0,
                        PunchStatus.VALID,
                        Duration.ZERO
                    ), true, true, true, true
                )
            )
            punchWrappers.add(
                PunchEditItemWrapper(
                    Punch(
                        UUID.randomUUID(),
                        dataProcessor.getCurrentRace().id,
                        null,
                        null,
                        0,
                        SITime(LocalTime.MIN),
                        SITime(LocalTime.MIN),
                        SIRecordType.FINISH,
                        0,
                        PunchStatus.VALID,
                        Duration.ZERO
                    ), true, true, true, true
                )
            )
        } else {
            punchWrappers =
                PunchEditItemWrapper.getWrappers(ArrayList(args.resultData!!.punches))
        }

        punchEditRecyclerView.adapter =
            PunchEditRecyclerViewAdapter(punchWrappers)

        //Populate the status options
        for (status in ResultStatus.entries) {
            statusArr.add(dataProcessor.resultStatusToString(status))
        }

        statusArr.add(0, getString(R.string.general_automatic))
        val statusAdapter: ArrayAdapter<String> =
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                statusArr
            )

        raceStatusPicker.setAdapter(statusAdapter)
    }

    private fun setCategoryPicker() {
        // Preset the category
        if (competitor != null) {
            categoryPickerLayout.isEnabled = true

            origCategoryId = competitor!!.categoryId
            val cat = categories.find { it.id == competitor!!.categoryId }
            if (cat != null) {
                categoryPicker.setText(cat.name, false)
            } else {
                categoryPicker.setText(getString(R.string.readout_unknown_category), false)
            }
        } else {
            categoryPicker.setText("")
            categoryPickerLayout.isEnabled = false
        }
    }

    private fun setButtons() {

        //Competitor picker
        competitorPicker.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                competitorPickerLayout.error = ""
                competitor = getCompetitorFromPicker()
                result.competitorID = competitor?.id

                setCategoryPicker()
            }

        categoryPicker.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                competitor?.categoryId = getCategoryFromPicker()

            }

        editSwitch.setOnCheckedChangeListener { p0, checked ->
            if (checked) {
                modified = true
                editSwitch.visibility = View.GONE
                punchEditRecyclerView.visibility = View.VISIBLE
            }
        }

        okButton.setOnClickListener {
            if (validateFields()) {

                val punches = PunchEditItemWrapper.getPunches(
                    (punchEditRecyclerView.adapter as PunchEditRecyclerViewAdapter).values
                )

                // Save the competitor if category was changed
                if (competitor?.categoryId != origCategoryId && competitor != null) {
                    runBlocking {
                        selectedRaceViewModel.createOrUpdateCompetitor(competitor!!)
                    }
                }

                // Save punch data
                runBlocking {
                    selectedRaceViewModel.processManualPunchData(
                        result,
                        punches,
                        getRaceStatusFromPicker(),
                        modified
                    )
                }

                setFragmentResult(
                    REQUEST_READOUT_MODIFICATION, bundleOf(
                        BUNDLE_RESULT_ID to result.id.toString()
                    )
                )
                dialog?.dismiss()
            }
        }

        cancelButton.setOnClickListener {
            dialog?.cancel()
        }
    }

    private fun validateFields(): Boolean {
        var valid = true

        //Check competitor
        if (result.competitorID != null
            && origResult?.competitorID != result.competitorID
            && selectedRaceViewModel.getResultByCompetitor(result.competitorID!!) != null
        ) {
            competitorPickerLayout.error = getString(R.string.readout_competitor_exists)
            valid = false
        }

        //Check punches
        if (!(punchEditRecyclerView.adapter as PunchEditRecyclerViewAdapter).isValid()) {
            valid = false
        }

        return valid
    }

    private fun getCompetitorFromPicker(): Competitor? {
        val compText = competitorPicker.text.toString()
        val compPos = competitorArr.indexOf(compText)
        return if (compPos > 0) {
            competitors[compPos - 1]
        } else null
    }

    private fun getCategoryFromPicker(): UUID? {
        val catText = categoryPicker.text.toString()
        val catPos = categoryArr.indexOf(catText)
        return if (catPos > 0) {
            categories[catPos - 1].id
        } else null
    }

    private fun getRaceStatusFromPicker(): ResultStatus? {
        val raceStatusString = raceStatusPicker.text.toString()
        return if (raceStatusString.isNotEmpty()
            && raceStatusString == requireContext().getString(R.string.general_automatic)
        ) {
            null
        } else {
            dataProcessor.resultStatusStringToEnum(raceStatusString)
        }
    }

    companion object {
        const val REQUEST_READOUT_MODIFICATION = "REQUEST_READOUT_MODIFICATION"
        const val BUNDLE_RESULT_ID = "BUNDLE_KEY_READOUT_ID"
    }
}