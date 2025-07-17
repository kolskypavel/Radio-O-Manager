package kolskypavel.ardfmanager.backend.files.processors;

import android.content.Context
import kolskypavel.ardfmanager.backend.room.entity.Category
import java.io.IOException

object TemplateProcessor {

    @Throws(IOException::class)
    fun loadTemplate(templateName: String, context: Context): String {
        return context.assets.open(templateName).readAllBytes().toString()

    }

    fun processTemplate(template: String, params: HashMap<String, String>): String {
        var output = template

        for (par in params) {
            output = output.replace(par.key, par.value)
        }

        return output
    }

    //Generates one line of competitor data
    fun generateCompetitorData(context: Context): String {
        var output = ""

        return output
    }


    fun generateCategoryHeader(context: Context, category: Category): String {
        var output = ""
        var params = HashMap<String, String>()

        params["cat_name"] = category.name
        params["title_name"]
        return output
    }

    // Generates the whole result block
    fun generateResults(context: Context): String {
        var output = ""

        return output
    }
}
