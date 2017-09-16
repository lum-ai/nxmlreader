package ai.lum.nxmlreader

import java.io.{ File, InputStream, Reader }


class NxmlReader(val preprocessor: Preprocessor) {
  def this(
      sectionsToIgnore: Set[String] = Set.empty,
      ignoreFloats: Boolean = true,
      transformText: String => String = identity
  ) = this(new NXMLPreprocessor(sectionsToIgnore, ignoreFloats, transformText))

  def parse(string: String): NxmlDocument = new NxmlDocument(XMLWithoutDTD.loadString(string), preprocessor)
  def read(name: String): NxmlDocument = new NxmlDocument(XMLWithoutDTD.loadFile(name), preprocessor)
  def read(file: File): NxmlDocument = new NxmlDocument(XMLWithoutDTD.loadFile(file), preprocessor)
  def read(inputStream: InputStream): NxmlDocument = new NxmlDocument(XMLWithoutDTD.load(inputStream), preprocessor)
  def read(reader: Reader): NxmlDocument = new NxmlDocument(XMLWithoutDTD.load(reader), preprocessor)
}
