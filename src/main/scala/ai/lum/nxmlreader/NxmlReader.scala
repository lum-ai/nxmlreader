package ai.lum.nxmlreader

import java.io.{ File, InputStream, Reader }
import scala.xml._
import scala.xml.transform.RewriteRule

object NxmlReader {
  def parse(string: String): NxmlDocument = new NxmlDocument(XMLWithoutDTD.loadString(string))
  def read(name: String): NxmlDocument = new NxmlDocument(XMLWithoutDTD.loadFile(name))
  def read(file: File): NxmlDocument = new NxmlDocument(XMLWithoutDTD.loadFile(file))
  def read(inputStream: InputStream): NxmlDocument = new NxmlDocument(XMLWithoutDTD.load(inputStream))
  def read(reader: Reader): NxmlDocument = new NxmlDocument(XMLWithoutDTD.load(reader))
}
