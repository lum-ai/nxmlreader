package ai.lum.nxmlreader

import scala.xml.Elem
import scala.xml.factory.XMLLoader
import javax.xml.parsers.{ SAXParser, SAXParserFactory }


/** Reads an XML file and ignores DTD */
object XMLWithoutDTD extends XMLLoader[Elem] {
  override def parser: SAXParser = {
    val f = SAXParserFactory.newInstance()
    f.setNamespaceAware(false)
    f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
    f.newSAXParser()
  }
}
