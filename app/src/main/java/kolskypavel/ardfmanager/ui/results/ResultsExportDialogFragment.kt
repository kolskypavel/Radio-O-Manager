package kolskypavel.ardfmanager.ui.results

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import kolskypavel.ardfmanager.R
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.files.constants.DataFormat
import kolskypavel.ardfmanager.backend.files.constants.DataType
import kolskypavel.ardfmanager.ui.SelectedRaceViewModel

class ResultsExportDialogFragment : DialogFragment() {

    private val selectedRaceViewModel: SelectedRaceViewModel by activityViewModels()
    val dataProcessor = DataProcessor.get()
    private lateinit var dataTypePicker: MaterialAutoCompleteTextView
    private lateinit var dataFormatPicker: MaterialAutoCompleteTextView
    private lateinit var errorText: TextView
    private lateinit var exportButton: Button
    private lateinit var cancelButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_share_results, container, false)
    }

    private val getResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == Activity.RESULT_OK) {
            val value = it.data
            val uri = value?.data

            if (uri != null) {
                exportData(uri)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.add_dialog)
        dialog?.setTitle(R.string.results_share)

        dataTypePicker = view.findViewById(R.id.results_data_type_picker)
        dataFormatPicker = view.findViewById(R.id.results_data_format_picker)
        errorText = view.findViewById(R.id.results_error_view)

        exportButton = view.findViewById(R.id.results_file_export_button)
        cancelButton = view.findViewById(R.id.results_file_cancel)

        setButtons()
    }

    private fun setButtons() {
        dataTypePicker.setText(getString(R.string.data_type_results), false)
        dataFormatPicker.setText(getText(R.string.data_format_txt), false)

        // Filter data formats based on the selected data type
        dataTypePicker.setOnItemClickListener { _, _, _, _ ->
            val selectedType = getCurrentType()
            when (selectedType) {
                DataType.RESULTS -> {
                    dataFormatPicker.setSimpleItems(R.array.results_data_formats)
                    dataFormatPicker.setText(getString(R.string.data_format_txt), false)
                }

                DataType.READOUT_DATA -> {
                    dataFormatPicker.setSimpleItems(R.array.readout_data_data_formats)
                    dataFormatPicker.setText(getString(R.string.data_format_csv), false)
                }

                else -> {
                    //safeguard against unsupported types
                }
            }
        }

        exportButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            setFlags(intent, getCurrentFormat())
            getResult.launch(intent)
        }


        cancelButton.setOnClickListener {
            dialog?.cancel()
        }
    }

    private fun setFlags(intent: Intent, dataFormat: DataFormat) {
        when (dataFormat) {

            DataFormat.TXT -> {
                intent.type = "text/txt"
                intent.putExtra(Intent.EXTRA_TITLE, "results.txt")
            }

            DataFormat.CSV -> {
                intent.type = "text/csv"
                intent.putExtra(Intent.EXTRA_TITLE, "results.csv")
            }

            DataFormat.JSON -> {
                intent.type = "text/*"
                intent.putExtra(Intent.EXTRA_TITLE, "results.json")
            }

            DataFormat.IOF_XML -> {
                intent.type = "text/xml"
                intent.putExtra(Intent.EXTRA_TITLE, "results.xml")
            }

            DataFormat.HTML -> {
                intent.type = "text/html"
                intent.putExtra(Intent.EXTRA_TITLE, "results.html")
            }

        }
    }

    private fun getCurrentType(): DataType {
        val text = dataTypePicker.text.toString()
        return dataProcessor.dataTypeFromString(text)
    }

    private fun getCurrentFormat(): DataFormat {
        val text = dataFormatPicker.text.toString()
        return dataProcessor.dataFormatFromString(text)
    }

    private fun exportData(uri: Uri) {

        try {
            selectedRaceViewModel.getCurrentRace()?.let {
                selectedRaceViewModel.exportData(
                    uri,
                    getCurrentType(),
                    getCurrentFormat(),
                    it.id
                )
            }
            errorText.text = ""
            val intent = Intent()
            intent.setAction(Intent.ACTION_VIEW)
            intent.setData(uri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("File intent opening", e.stackTraceToString())
            val err = e.message ?: e.toString()
            errorText.text = requireContext().getString(
                R.string.results_export_error,
                err.take(100)
            )
        }
    }

    private fun previewData() {
        val currType = getCurrentType()
        val format = getCurrentFormat()

        when (currType) {
            DataType.CATEGORIES -> TODO()
            DataType.COMPETITORS -> TODO()
            DataType.COMPETITOR_STARTS -> TODO()
            DataType.RESULTS -> TODO()
            DataType.READOUT_DATA -> TODO()
        }
    }
}