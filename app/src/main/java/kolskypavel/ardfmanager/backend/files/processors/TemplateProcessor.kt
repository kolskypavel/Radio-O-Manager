package kolskypavel.ardfmanager.backend.files.processors;

import android.content.Context
import kolskypavel.ardfmanager.backend.files.constants.FileConstants
import java.io.IOException

object TemplateProcessor {

    @Throws(IOException::class)
    fun loadTemplate(templateName: String, context: Context): String {
        val inputStream = context.assets.open(templateName)
        return inputStream.bufferedReader().use { it.readText() }
    }

    fun processTemplate(template: String, params: Map<String, String>): String {
        var output = template

        for (par in params) {
            output = output.replace(par.key, par.value)
        }
        // Replace TAB as the last parameter
        output = output.replace(FileConstants.KEY_TAB, "\t")

        return output
    }
}
