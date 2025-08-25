package kolskypavel.ardfmanager.ui.settings

import android.app.Dialog
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import kolskypavel.ardfmanager.R

class AboutDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val view: View = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_about_app, null)

        val appVersionTextView: TextView = view.findViewById(R.id.tv_version)
        val appLinkTextView: TextView = view.findViewById(R.id.tv_link)

        val context = requireContext()
        val packageManager = context.packageManager
        val versionName = packageManager.getPackageInfo(context.packageName, 0).versionName

        appVersionTextView.text = context.getString(R.string.about_app_version, versionName)
        appLinkTextView.movementMethod = LinkMovementMethod.getInstance()

        return AlertDialog.Builder(requireContext())
            .setTitle("About")
            .setView(view)
            .setPositiveButton("OK", null)
            .create()
    }
}