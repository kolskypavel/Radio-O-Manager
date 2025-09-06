package kolskypavel.ardfmanager.ui.data

import android.app.Activity
import android.content.Intent
import android.content.res.Resources
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kolskypavel.ardfmanager.R
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.constants.DataFormat
import kolskypavel.ardfmanager.backend.files.constants.DataType
import kolskypavel.ardfmanager.backend.files.wrappers.DataImportWrapper
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.CategoryData
import kolskypavel.ardfmanager.ui.SelectedRaceViewModel
import kotlinx.coroutines.runBlocking

class DataImportDialogFragment : DialogFragment() {

    private val dataProcessor = DataProcessor.get()
    private val selectedRaceViewModel: SelectedRaceViewModel by activityViewModels()

    private var data: DataImportWrapper? = null

    private lateinit var dataTypePicker: MaterialAutoCompleteTextView
    private lateinit var dataFormatPicker: MaterialAutoCompleteTextView
    private lateinit var dataPreviewLayout: LinearLayout
    private lateinit var dataPreviewRecyclerView: RecyclerView
    private lateinit var errorView: TextView
    private lateinit var importInfoView: TextView
    private lateinit var importButton: Button
    private lateinit var okButton: Button
    private lateinit var cancelButton: Button

    private val getResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            val value = it.data
            val uri = value?.data

            if (uri != null) {
                openData(uri)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_data_import, container, false)
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
        dialog?.setTitle(R.string.data_import_data)

        dataTypePicker = view.findViewById(R.id.data_import_type)
        dataFormatPicker = view.findViewById(R.id.data_import_format)
        importButton = view.findViewById(R.id.data_import_import_btn)
        dataPreviewLayout = view.findViewById(R.id.data_import_preview_layout)
        dataPreviewRecyclerView = view.findViewById(R.id.data_import_recyclerview)
        importInfoView = view.findViewById(R.id.data_import_preview_info)
        errorView = view.findViewById(R.id.data_import_error)
        okButton = view.findViewById(R.id.data_import_ok)
        cancelButton = view.findViewById(R.id.data_import_cancel)

        dataTypePicker.setText(getString(R.string.data_type_categories), false)
        dataFormatPicker.setText(getString(R.string.data_format_csv), false)
        importButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            setFlags(intent, getCurrentFormat())
            getResult.launch(intent)
        }

        okButton.setOnClickListener {
            confirmImport()
            dialog?.dismiss()
        }

        cancelButton.setOnClickListener {
            dialog?.cancel()
        }
    }

    // TODO: finish
    private fun setFlags(intent: Intent, dataFormat: DataFormat) {
        intent.type = "text/*"
//        when (dataFormat) {
//            DataFormat.CSV -> {
//
//            }
//
//            DataFormat.JSON -> {
//                intent.type = "text/*"
//            }
//
//            DataFormat.IOF_XML -> {
//                intent.type = "text/*"
//            }
//
//            else -> {}
//        }
    }

    private fun getCurrentType(): DataType {
        val text = dataTypePicker.text.toString()
        return dataProcessor.dataTypeFromString(text)
    }

    private fun getCurrentFormat(): DataFormat {
        val text = dataFormatPicker.text.toString()
        return dataProcessor.dataFormatFromString(text)
    }

    private fun openData(uri: Uri) {
        val currType = getCurrentType()
        val format = getCurrentFormat()

        try {
            runBlocking {
                data = selectedRaceViewModel.getCurrentRace()?.let {
                    selectedRaceViewModel.importData(
                        uri, currType,
                        format,
                        it.id
                    )
                }
            }

            dataPreviewRecyclerView.adapter =
                DataPreviewRecyclerViewAdapater(data!!, currType)

            //Inform about invalid lines
            if (data!!.invalidLines.isNotEmpty()) {
                var errorText = ""
                for (err in data!!.invalidLines) {
                    errorText += requireContext().getString(
                        R.string.data_import_invalid_line,
                        err.first,
                        err.second
                    )
                }
                errorView.text = errorText
            } else {
                errorView.text = ""
            }

            importInfoView.text = getString(
                R.string.data_import_preview_info,
                data!!.getCount(currType)
            )
            dataPreviewLayout.visibility = View.VISIBLE

        } catch (e: IllegalArgumentException) {
            errorView.text = e.message
            dataPreviewLayout.visibility = View.GONE
        }
        // Generic error message for other exceptions
        catch (e: Exception) {
            errorView.text = getString(R.string.data_import_file_error)
            dataPreviewLayout.visibility = View.GONE
        }
    }

    //Import data after confirmation
    private fun confirmImport() {
        if (data != null) {
            val currType = getCurrentType()
            when (currType) {
                DataType.CATEGORIES -> importCategories(data!!.categories)

                DataType.COMPETITORS -> {
                    selectedRaceViewModel.saveDataImportWrapper(data!!)
                }

                DataType.COMPETITOR_STARTS_TIME -> {
                    //Save competitor starts - TODO: ADD duplicates check
                    for (compData in data!!.competitorCategories) {
                        selectedRaceViewModel.createOrUpdateCompetitor(compData.competitor)
                    }
                }

                else -> {}
            }
        }
    }

    private fun importCategories(categories: List<CategoryData>) {
        for (cd in categories) {

            //Check if category already exists - if it does, update it
            val existingCategory = selectedRaceViewModel.getCurrentRace()
                ?.let { selectedRaceViewModel.getCategoryByName(cd.category.name, it.id) }
            if (existingCategory != null) {
                cd.category.id = existingCategory.id

                for (cp in cd.controlPoints) {
                    cp.categoryId = existingCategory.id
                }
            }
            // Update the order for non existent category
            else {
                selectedRaceViewModel.getCurrentRace()?.let {
                    cd.category.order =
                        selectedRaceViewModel.getHighestCategoryOrder(it.id) + 1
                }
            }

            selectedRaceViewModel.createOrUpdateCategory(cd.category, cd.controlPoints)
        }
    }
}