package ai.lum.nxmlreader

import java.io.{ File, InputStream, Reader }
import scala.xml._
import scala.xml.transform.RewriteRule

class NxmlReader(val preprocessor: Preprocessor) {
  def this(sectionsToIgnore: Set[String], ignoreFloats: Boolean) = this(new NXMLPreprocessor(sectionsToIgnore, ignoreFloats))
  def this(sectionsToIgnore: Set[String]) = this(new NXMLPreprocessor(sectionsToIgnore, true))

  def parse(string: String): NxmlDocument = new NxmlDocument(XMLWithoutDTD.loadString(string), preprocessor)
  def read(name: String): NxmlDocument = new NxmlDocument(XMLWithoutDTD.loadFile(name), preprocessor)
  def read(file: File): NxmlDocument = new NxmlDocument(XMLWithoutDTD.loadFile(file), preprocessor)
  def read(inputStream: InputStream): NxmlDocument = new NxmlDocument(XMLWithoutDTD.load(inputStream), preprocessor)
  def read(reader: Reader): NxmlDocument = new NxmlDocument(XMLWithoutDTD.load(reader), preprocessor)
}
