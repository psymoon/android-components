/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.components.browser.search.provider.custom

import android.graphics.Bitmap
import android.util.Base64
import mozilla.components.support.base.log.logger.Logger
import org.w3c.dom.Document
import java.io.ByteArrayOutputStream
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerConfigurationException
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

private const val BITMAP_COMPRESS_QUALITY = 100
internal fun Bitmap.toBase64(): String {
    val stream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.PNG, BITMAP_COMPRESS_QUALITY, stream)
    val encodedImage = Base64.encodeToString(stream.toByteArray(), Base64.DEFAULT)
    return "data:image/png;base64,$encodedImage"
}

/**
 * Custom search engine writer that transforms a custom search engine into XML.
 */
class CustomSearchEngineWriter {
    private val logger = Logger("CustomSearchEngineWriter")
    internal val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

    /**
     * builds given custom search engine into XML string.
     * @param searchEngineName custom search engine name.
     * @param searchQuery custom search engine query.
     * @param iconBitmap custom search engine icon bitmap.
     * @return XML string of the given custom search engine, null if there was an error during transformation.
     */
    fun buildSearchEngineXML(searchEngineName: String, searchQuery: String, iconBitmap: Bitmap): String? {
        try {
            val document = builder.newDocument()
            val rootElement = document!!.createElement("OpenSearchDescription")
            rootElement.setAttribute("xmlns", "http://a9.com/-/spec/opensearch/1.1/")
            document.appendChild(rootElement)

            val shortNameElement = document.createElement("ShortName")
            shortNameElement.textContent = searchEngineName
            rootElement.appendChild(shortNameElement)

            val imageElement = document.createElement("Image")
            imageElement.setAttribute("width", "16")
            imageElement.setAttribute("height", "16")
            imageElement.textContent = iconBitmap.toBase64()
            rootElement.appendChild(imageElement)

            val descriptionElement = document.createElement("Description")
            descriptionElement.textContent = searchEngineName
            rootElement.appendChild(descriptionElement)

            val urlElement = document.createElement("Url")
            urlElement.setAttribute("type", "text/html")

            val templateSearchString = searchQuery.replace("%s", "{searchTerms}")
            urlElement.setAttribute("template", templateSearchString)
            rootElement.appendChild(urlElement)

            return xmlToString(document)
        } catch (e: ParserConfigurationException) {
            logger.error("Couldn't create new Document for building search engine XML", e)
            return null
        }
    }

    private fun xmlToString(doc: Document): String? {
        val writer = StringWriter()
        try {
            val tf = TransformerFactory.newInstance().newTransformer()
            tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            tf.transform(DOMSource(doc), StreamResult(writer))
        } catch (e: TransformerConfigurationException) {
            return null
        } catch (e: TransformerException) {
            return null
        }

        return writer.toString()
    }
}
