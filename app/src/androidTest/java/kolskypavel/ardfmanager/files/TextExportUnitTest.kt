package kolskypavel.ardfmanager.files

import junit.framework.TestCase.assertEquals
import kolskypavel.ardfmanager.backend.files.processors.TemplateProcessor
import org.junit.Test

class TextExportUnitTest {

    @Test
    fun testTemplateOutput() {
        val template = "{{test}}{{out}}" +
                "\t{{render}} okay"

        val params = HashMap<String,String>()
        params["{{test}}"] = "TEST"
        params["{{out}}"] = "OUT"
        params["{{render}}"] = "RENDER"

        val res = TemplateProcessor.processTemplate(template,params)
        assertEquals("TESTOUT\tRENDER okay",res)
    }
}