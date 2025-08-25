package kolskypavel.ardfmanager.ui.settings

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import io.noties.markwon.Markwon
import kolskypavel.ardfmanager.R
import java.io.BufferedReader
import java.io.InputStreamReader

class ChangelogDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())
        val view: View = inflater.inflate(R.layout.dialog_changelog, null)

        val changelogTextView: TextView = view.findViewById(R.id.changelog_text)
        val markdown = loadChangelogFromAssets()

        // Initialize Markwon and render the Markdown
        val markwon = Markwon.create(requireContext())
        markwon.setMarkdown(changelogTextView, markdown)

        return AlertDialog.Builder(requireContext())
            .setTitle("Changelog")
            .setView(view)
            .setPositiveButton("Close", null)
            .create()
    }

    private fun loadChangelogFromAssets(): String {
        return try {
            requireContext().assets.open("CHANGELOG.md").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).readText()
            }
        } catch (e: Exception) {
            "Unable to load changelog."
        }
    }
}