package kolskypavel.ardfmanager.ui.services

import android.content.res.Resources
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.navArgs
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kolskypavel.ardfmanager.R
import kolskypavel.ardfmanager.backend.DataProcessor
import kolskypavel.ardfmanager.backend.helpers.TimeProcessor
import kolskypavel.ardfmanager.backend.results.ResultServiceProcessor
import kolskypavel.ardfmanager.backend.room.entity.ResultService
import kolskypavel.ardfmanager.backend.room.entity.embeddeds.ResultServiceData
import kolskypavel.ardfmanager.backend.room.enums.ResultServiceType
import kolskypavel.ardfmanager.ui.SelectedRaceViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.Duration

class ResultServiceDialogFragment : DialogFragment() {

    private val args: ResultServiceDialogFragmentArgs by navArgs()
    private lateinit var selectedRaceViewModel: SelectedRaceViewModel

    private val dataProcessor = DataProcessor.get()

    private lateinit var resultService: ResultService

    private lateinit var enableSwitch: SwitchMaterial
    private lateinit var typePicker: MaterialAutoCompleteTextView
    private lateinit var urlInputLayout: TextInputLayout
    private lateinit var urlInput: TextInputEditText
    private lateinit var apiKeyLayout: TextInputLayout
    private lateinit var apiKeyInput: TextInputEditText
    private lateinit var intervalLayout: TextInputLayout
    private lateinit var intervalInput: TextInputEditText
    private lateinit var statusView: TextView
    private lateinit var errorTextView: TextView
    private lateinit var resendResultsButton: Button
    private lateinit var closeButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_result_service, container, false)
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

        enableSwitch = view.findViewById(R.id.result_service_dialog_enable)
        typePicker = view.findViewById(R.id.result_service_dialog_type)
        urlInputLayout = view.findViewById(R.id.results_service_dialog_url_layout)
        urlInput = view.findViewById(R.id.results_service_dialog_url)
        apiKeyLayout = view.findViewById(R.id.results_service_dialog_api_key_layout)
        apiKeyInput = view.findViewById(R.id.results_service_dialog_api_key)
        intervalLayout = view.findViewById(R.id.results_service_dialog_interval_layout)
        intervalInput = view.findViewById(R.id.results_service_dialog_interval)
        statusView = view.findViewById(R.id.results_service_dialog_status)
        errorTextView = view.findViewById(R.id.results_service_dialog_error)
        resendResultsButton = view.findViewById(R.id.results_service_dialog_resend_results)
        closeButton = view.findViewById(R.id.results_service_dialog_close)

        populateFields()
        setButtons()
    }

    private fun populateFields() {
        dialog?.setTitle(R.string.results_service)

        runBlocking {
            resultService = selectedRaceViewModel.resultService.value?.resultService
                ?: ResultService(args.race.id)
        }
        enableSwitch.isChecked = resultService.enabled
        typePicker.setText(
            dataProcessor.resultServiceTypeToString(resultService.serviceType),
            false
        )

        urlInput.setText(resultService.url)

        // Preset the apiKey
        val apiKey = resultService.apiKey.ifBlank {
            args.race.apiKey
        }

        apiKeyInput.setText(apiKey)
        intervalInput.setText(resultService.interval.seconds.toString())
        enableSendAgainButton()

        // Result service observer
        dataProcessor.getResultServiceLiveDataWithCountByRaceId(args.race.id)
            .observe(viewLifecycleOwner) { data ->
                if (data != null) {
                    setStatusString(data)
                }
            }
    }

    private fun setButtons() {
        // Turn off the result service after changing type
        typePicker.onItemClickListener =
            AdapterView.OnItemClickListener { parent, view, position, id ->
                selectedRaceViewModel.disableResultService()
                enableSwitch.isChecked = false
                statusView.setText("")
                errorTextView.setText("")

                enableSendAgainButton()
            }

        enableSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (!isChecked) {
                selectedRaceViewModel.disableResultService()
            } else if (validateFields()) {
                enableResultService()
                apiKeyLayout.error = ""
                intervalLayout.error = ""
            } else {
                enableSwitch.isChecked = false
            }
        }

        // Set all results as unsent -> this will trigger the worker to resend them
        resendResultsButton.setOnClickListener {
            val currService = selectedRaceViewModel.resultService.value?.resultService
            if (currService != null) {
                selectedRaceViewModel.setAllResultsUnsent(args.race.id)
                currService.sent = 0

                // Update the service in the database
                CoroutineScope(Dispatchers.IO).launch {
                    dataProcessor.createOrUpdateResultService(currService)
                }
            }
        }

        closeButton.setOnClickListener {
            dialog?.cancel()
        }
    }

    private fun validateFields(): Boolean {
        var valid = true

//        val serviceType =
//            dataProcessor.resultServiceTypeFromString(typePicker.text.toString())
//        val url = urlInput.text.toString()

        if (apiKeyInput.text.toString().isEmpty()) {
            valid = false
            apiKeyLayout.error = getString(R.string.result_service_api_key_missing)
        }

        if (intervalInput.text.toString().isEmpty()) {
            valid = false
            intervalLayout.error = getString(R.string.general_required)
        } else if (intervalInput.text.toString().toIntOrNull() == null) {
            valid = false
            intervalLayout.error = getString(R.string.general_invalid)
        }

        return valid
    }

    private fun getResultServiceType(): ResultServiceType {
        val text = typePicker.text.toString()
        return dataProcessor.resultServiceTypeFromString(text)
    }

    private fun enableSendAgainButton() {
        val currType = getResultServiceType()
        if (currType == ResultServiceType.ORESULTS) {
            resendResultsButton.visibility = View.GONE
        } else {
            resendResultsButton.visibility = View.VISIBLE
        }
    }

    // Get the status text about result status
    private fun setStatusString(data: ResultServiceData) {

        var text = ""
        if (data.resultService != null) {
            text = getString(
                R.string.result_service_status_text,
                dataProcessor.resultServiceStatusToString(data.resultService.status),
                TimeProcessor.formatLocalTime(data.resultService.sentAt)
            )
            if (data.resultService.serviceType == ResultServiceType.ROBIS_TEST ||
                data.resultService.serviceType == ResultServiceType.ROBIS
            ) {
                text += " ${
                    getString(
                        R.string.result_service_status_text_counter,
                        data.resultService.sent,
                        data.resultCount
                    )
                }"
            }
            errorTextView.text = data.resultService.errorText
        }
        statusView.text = text
    }

    private fun enableResultService() {

        // Copy the fields
        resultService.enabled = true
        resultService.serviceType = getResultServiceType()
        resultService.url = urlInput.text.toString()
        resultService.apiKey = apiKeyInput.text.toString()
        resultService.interval = Duration.ofSeconds(intervalInput.text.toString().toLong())

        CoroutineScope(Dispatchers.IO).launch {
            dataProcessor.createOrUpdateResultService(resultService)
            dataProcessor.setResultServiceJob(
                ResultServiceProcessor.resultServiceJob(
                    args.race.id,
                    dataProcessor,
                    requireContext()
                )
            )
        }
    }
}