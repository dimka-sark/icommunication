package com.dimkasark.utils

import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

fun asParametersMap(xmlContent: String): Map<String, String> {
  val parametersMap = mutableMapOf<String, String>()

  xmlContent.byteInputStream().use { content ->
    val builderFactory = DocumentBuilderFactory.newInstance()
    val docBuilder = builderFactory.newDocumentBuilder()
    val doc = docBuilder.parse(content)
    val node = doc.getElementsByTagName("dict").item(0)

    var lastKey = ""
    for (i in 0 until node.childNodes.length) {
      val item = node.childNodes.item(i) as? Element ?: continue
      if (item.tagName == "key") {
        lastKey = item.childNodes.item(0).nodeValue.trim()
      }
      if (item.tagName == "string") {
        parametersMap[lastKey] = if (item.childNodes.length < 1) {
          ""
        } else {
          item.childNodes.item(0).nodeValue.trim()
        }
        lastKey = ""
      }
    }
  }
  return parametersMap
}