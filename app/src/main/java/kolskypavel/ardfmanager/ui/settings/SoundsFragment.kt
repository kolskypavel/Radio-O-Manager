package kolskypavel.ardfmanager.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.preference.SwitchPreference
import kolskypavel.ardfmanager.R

class SoundsFragment : PreferenceFragmentCompat() {
    private lateinit var prefs: SharedPreferences

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_sounds, rootKey)
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        setPreferences()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Toolbar>(R.id.settings_toolbar)?.let { toolbar ->
            toolbar.title = getString(R.string.global_settings)
            toolbar.subtitle =getString(R.string.preferences_readout_sounds)
        }
    }

    private fun setPreferences() {
        val editor = prefs.edit()

        findPreference<SwitchPreference>(requireContext().getString(R.string.key_sounds_enabled))
            ?.setOnPreferenceChangeListener { _, enableSounds ->

                editor.putBoolean(
                    requireContext().getString(R.string.key_sounds_enabled),
                    enableSounds as Boolean
                )
                editor.apply()
                enableOrDisablePreferences(enableSounds)
                true
            }
    }

    private fun enableOrDisablePreferences(enable: Boolean) {
        val competitorMatched =
            findPreference<Preference>(requireContext().getString(R.string.key_sounds_ok))

        val unknownCategory =
            findPreference<Preference>(requireContext().getString(R.string.key_sounds_unknown_category))

        val unknownCompetitor =
            findPreference<Preference>(requireContext().getString(R.string.key_sounds_unknown_competitor))

        competitorMatched?.isEnabled = enable
        unknownCategory?.isEnabled = enable
        unknownCompetitor?.isEnabled = enable
    }
}