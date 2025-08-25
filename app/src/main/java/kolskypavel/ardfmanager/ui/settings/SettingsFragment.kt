package kolskypavel.ardfmanager.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.findNavController
import androidx.preference.CheckBoxPreference
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import kolskypavel.ardfmanager.R


class SettingsFragment : PreferenceFragmentCompat() {
    private lateinit var prefs: SharedPreferences

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Toolbar>(R.id.settings_toolbar)?.let { toolbar ->
            toolbar.title = getString(R.string.global_settings)
            toolbar.subtitle = getString(R.string.general_main)
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        setPreferences()
    }

    private fun setPreferences() {
        val editor = prefs.edit()

        findPreference<CheckBoxPreference>(requireContext().getString(R.string.key_keep_screen_open))
            ?.setOnPreferenceChangeListener { _, keepOpen ->

                editor.putBoolean(
                    requireContext().getString(R.string.key_keep_screen_open),
                    keepOpen as Boolean
                )
                editor.apply()
                true
            }

        val langPref =
            findPreference<ListPreference>(requireContext().getString(R.string.key_app_language))

        langPref?.setOnPreferenceChangeListener { pref, code ->
            editor.putString(
                requireContext().getString(R.string.key_app_language),
                code.toString()
            )
            editor.apply()

            langPref.summary = requireContext().getString(
                R.string.preferences_general_language_hint,
                code
            )
            true
        }

        // Duplicate readout preferences
        val duplReadoutPref =
            findPreference<ListPreference>(requireContext().getString(R.string.key_readout_duplicate))

        val currReadoutPref = prefs.getString(
            requireContext().getString(R.string.key_readout_duplicate),
            requireContext().getString(R.string.preferences_readout_keep_original)
        )

        duplReadoutPref?.summary = requireContext().getString(
            R.string.preferences_readout_duplicate_hint,
            currReadoutPref
        )

        duplReadoutPref?.setOnPreferenceChangeListener { _, action ->
            editor.putString(
                requireContext().getString(R.string.key_readout_duplicate),
                action.toString()
            )
            editor.apply()

            duplReadoutPref.summary = requireContext().getString(
                R.string.preferences_readout_duplicate_hint,
                action
            )
            true
        }

        //Sound preferences
        findPreference<androidx.preference.Preference>(
            requireContext().getString(
                R.string.key_readout_sounds
            )
        )
            ?.setOnPreferenceClickListener {
                findNavController().navigate(SettingsFragmentDirections.configureSounds())
                true
            }

        //Time format
        val timeFormatPref =
            findPreference<ListPreference>(requireContext().getString(R.string.key_results_time_format))

        val currTimeFormatPref = prefs.getString(
            requireContext().getString(R.string.key_results_time_format),
            requireContext().getString(R.string.preferences_results_time_format_minutes)
        )

        timeFormatPref?.summary = requireContext().getString(
            R.string.preferences_results_time_format_hint,
            currTimeFormatPref
        )

        timeFormatPref?.setOnPreferenceChangeListener { _, timeFormat ->

            editor.putString(
                requireContext().getString(R.string.key_results_time_format),
                timeFormat.toString()
            )
            editor.apply()

            timeFormatPref.summary = requireContext().getString(
                R.string.preferences_results_time_format_hint,
                timeFormat
            )
            true
        }

        //Aliases
        findPreference<CheckBoxPreference>(requireContext().getString(R.string.key_results_use_aliases))
            ?.setOnPreferenceChangeListener { _, useAliases ->

                editor.putBoolean(
                    requireContext().getString(R.string.key_results_use_aliases),
                    useAliases as Boolean
                )
                editor.apply()
                true
            }

        // Result service
        val resultServicePref =
            findPreference<ListPreference>(requireContext().getString(R.string.key_result_service))

        val currServicePref = prefs.getString(
            requireContext().getString(R.string.key_result_service),
            requireContext().getString(R.string.preferences_result_service_matched)
        )

        resultServicePref?.summary = requireContext().getString(
            R.string.preferences_results_service_hint,
            currServicePref
        )

        resultServicePref?.setOnPreferenceChangeListener { _, service ->
            editor.putString(
                requireContext().getString(R.string.key_result_service),
                service.toString()
            )
            editor.apply()

            resultServicePref.summary = requireContext().getString(
                R.string.preferences_results_service_hint,
                service
            )
            true
        }

        // Files and imports
        findPreference<CheckBoxPreference>(requireContext().getString(R.string.key_files_invalid_stops_import))
            ?.setOnPreferenceChangeListener { _, keepOpen ->

                editor.putBoolean(
                    requireContext().getString(R.string.key_files_invalid_stops_import),
                    keepOpen as Boolean
                )
                editor.apply()
                true
            }

        findPreference<CheckBoxPreference>(requireContext().getString(R.string.key_files_prefer_app_start_time))
            ?.setOnPreferenceChangeListener { _, keepOpen ->

                editor.putBoolean(
                    requireContext().getString(R.string.key_files_prefer_app_start_time),
                    keepOpen as Boolean
                )
                editor.apply()
                true
            }

        findPreference<androidx.preference.Preference>(
            requireContext().getString(
                R.string.key_prints
            )
        )
            ?.setOnPreferenceClickListener {
                findNavController().navigate(SettingsFragmentDirections.configurePrints())
                true
            }

//        HELP
        findPreference<androidx.preference.Preference>(
            requireContext().getString(
                R.string.key_about_app
            )
        )
            ?.setOnPreferenceClickListener {
                findNavController().navigate(SettingsFragmentDirections.openAboutApp())
                true
            }

        findPreference<androidx.preference.Preference>(
            requireContext().getString(
                R.string.key_changelog
            )
        )
            ?.setOnPreferenceClickListener {
                findNavController().navigate(SettingsFragmentDirections.openChangeLog())
                true
            }
    }
}