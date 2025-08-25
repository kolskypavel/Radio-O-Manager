package kolskypavel.ardfmanager.ui.settings

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import kolskypavel.ardfmanager.R
import kolskypavel.ardfmanager.backend.DataProcessor


class PrintsFragment : PreferenceFragmentCompat() {
    private lateinit var prefs: SharedPreferences
    private val dataProcessor = DataProcessor.get()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_prints, rootKey)
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())

        setPreferences()
        val printsEnabled =
            prefs.getBoolean(requireContext().getString(R.string.key_prints_enabled), false)
        enableOrDisablePreferences(printsEnabled)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //Add a back button to the toolbar
        view.findViewById<Toolbar>(R.id.settings_toolbar)?.let { toolbar ->
            toolbar.title = getString(R.string.global_settings)
            toolbar.subtitle = getString(R.string.general_print)
        }
    }

    private fun setPreferences() {
        val editor = prefs.edit()

        //Enable printing
        val enablePrintingPreference =
            findPreference<SwitchPreference>(requireContext().getString(R.string.key_prints_enabled))

        enablePrintingPreference?.setOnPreferenceChangeListener { _, enablePrints ->

            // If printing is disabled -> let the PrintProcessor know
            if (enablePrints as Boolean) {
                // Request bluetooth permissions if needed
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                        ),
                        1
                    )
                }
            } else {
                dataProcessor.disablePrinter()
            }

            editor.putBoolean(
                requireContext().getString(R.string.key_prints_enabled),
                enablePrints as Boolean
            )
            editor.apply()
            enableOrDisablePreferences(enablePrints)

            true
        }

        //Printer selection
        val printerSelectPreference =
            findPreference<ListPreference>(requireContext().getString(R.string.key_prints_selected_printer_address))

        printerSelectPreference?.setOnPreferenceClickListener {

            val bluetoothAvailable =
                requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)

            if (bluetoothAvailable) {
                // Get the BluetoothManager
                val bluetoothAdapter =
                    ContextCompat.getSystemService(
                        requireContext(),
                        BluetoothManager::class.java
                    )?.adapter

                val pairedDevices = emptySet<BluetoothDevice>().toMutableSet()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                ) {
                    // Request the BLUETOOTH_CONNECT permission
                    ActivityCompat.requestPermissions(
                        requireActivity(),
                        arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                        1
                    )
                }

                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S ||
                    ContextCompat.checkSelfPermission(
                        requireContext(),
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // Permission already granted, proceed with accessing paired devices
                    if (bluetoothAdapter != null && bluetoothAdapter.bondedDevices != null) {
                        pairedDevices.addAll(bluetoothAdapter.bondedDevices!!)
                    }
                }
                val deviceNames = pairedDevices.map { it.name }.toTypedArray()
                val deviceAddresses = pairedDevices.map { it.address }.toTypedArray()

                printerSelectPreference.entries = deviceNames
                printerSelectPreference.entryValues = deviceAddresses
            }
            //Warning about missing bluetooth
            else {
                val toast = Toast.makeText(
                    requireContext(),
                    requireContext().getString(R.string.print_bluetooth_not_supported),
                    Toast.LENGTH_LONG
                )
                toast.show()
            }
            true
        }
        val currPrinterPref = prefs.getString(
            requireContext().getString(R.string.key_prints_selected_printer_name),
            ""
        )
        printerSelectPreference?.summary = requireContext().getString(
            R.string.preferences_prints_select_printer_hint,
            currPrinterPref
        )

        // Save both printer name and address
        printerSelectPreference?.setOnPreferenceChangeListener { preference, printer ->
            val listPref = preference as ListPreference
            val address = printer as String
            val index = listPref.entryValues.indexOf(address)
            val name = if (index >= 0) listPref.entries[index].toString() else ""

            editor.putString(
                requireContext().getString(R.string.key_prints_selected_printer_name),
                name
            )
            editor.putString(
                requireContext().getString(R.string.key_prints_selected_printer_address),
                address
            )
            editor.apply()

            true
        }

        val automaticPrintPreference =
            findPreference<ListPreference>(requireContext().getString(R.string.key_prints_automatic_printout))

        automaticPrintPreference?.setOnPreferenceChangeListener { _, action ->
            editor.putString(
                requireContext().getString(R.string.key_prints_automatic_printout),
                action.toString()
            )
            editor.apply()

            automaticPrintPreference.summary = requireContext().getString(
                R.string.preferences_prints_automatic_hint,
                action
            )
            true
        }

        val removeDiacriticsPreference =
            findPreference<CheckBoxPreference>(requireContext().getString(R.string.key_prints_remove_diacritics))

        removeDiacriticsPreference?.setOnPreferenceChangeListener { _, removeDiacritics ->
            editor.putBoolean(
                requireContext().getString(R.string.key_prints_remove_diacritics),
                removeDiacritics as Boolean
            )
            editor.apply()
            true
        }
    }


    private fun enableOrDisablePreferences(enable: Boolean) {
        val printerSelectPreference =
            findPreference<ListPreference>(requireContext().getString(R.string.key_prints_selected_printer_address))

        val automaticPrintPreference =
            findPreference<ListPreference>(requireContext().getString(R.string.key_prints_automatic_printout))

        printerSelectPreference?.isEnabled = enable
        automaticPrintPreference?.isEnabled = enable
    }
}