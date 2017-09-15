package ai.lum.nxmlreader

import scala.xml.{Elem, Node, Text}
import scala.xml.transform.RewriteRule


trait Preprocessor extends RewriteRule

case class NXMLPreprocessor(
  sectionsToIgnore: Set[String],
  ignoreFloats: Boolean,
  transformText: String => String
) extends Preprocessor {

  // surrounds sup/sub content with spaces
  override def transform(n: Node): Seq[Node] = n match {

    // handle tag representing a line break
    case <break/> => Text("\n")

    // surround subscripts and superscripts with spaces
    case <sup>{text}</sup> => <sup> {transformText(text.text)} </sup>
    case <sub>{text}</sub> => <sub> {transformText(text.text)} </sub>

    // remove latex and tables from text
    case <tex-math>{text}</tex-math> => Nil
    case e: Elem if e.label == "table-wrap" => Nil // FIXME this is removing captions too

    // append dots to title and surround it with newlines
    case e: Elem if e.label == "article-title" =>
      val e2 = transformChildren(e)
      val txt = Text(s"${transformText(e2.text)}.\n\n")
      <article-title>{txt}</article-title>

    case e: Elem if e.label == "abstract" =>
      val e2 = transformChildren(e)
      val txt = Text(s"\n\n${transformText(e2.text)}\n\n")
      <abstract>{txt}</abstract>

    case e: Elem if e.label == "title" =>
      val e2 = transformChildren(e)
      val txt = Text(s"\n\n${transformText(e2.text)}.\n\n")
      <title>{txt}</title>

    // append newlines to paragraphs
    case e: Elem if e.label == "p" =>
      val e2 = transformChildren(e)
      val txt = Text(s"${transformText(e2.text)}\n\n")
      <p>{txt}</p>

    // remove floats
    case e: Elem if ignoreFloats && attr(e, "position", "float") => Nil

    // NOTE we are removing graphics that are not floats too
    // maybe we need to ignore tables and figures instead of floats?
    case e: Elem if e.label == "fig" => Nil

    // remove some sections
    case e: Elem if sectionsToIgnore.contains(e.label) | attr(e, "sec-type", sectionsToIgnore) => Nil

    // retain xref
    case e: Elem if e.label == "xref" && attr(e, "ref-type", "bibr") =>
      val e2 = transformChildren(e)
      val txt = Text(transformText(e2.text))
      <xref-bibr>{txt}</xref-bibr>

    case e: Elem if e.label == "xref" && attr(e, "ref-type", "fig") =>
      val e2 = transformChildren(e)
      val txt = Text(transformText(e2.text))
      <xref-fig>{txt}</xref-fig>

    case e: Elem if e.label == "xref" && attr(e, "ref-type", "table") =>
      val e2 = transformChildren(e)
      val txt = Text(transformText(e2.text))
      <xref-table>{txt}</xref-table>

    case e: Elem if e.label == "xref" && attr(e, "ref-type", "supplementary-material") =>
      val e2 = transformChildren(e)
      val txt = Text(transformText(e2.text))
      <xref-supplementary>{txt}</xref-supplementary>

    // recurse
    case e: Elem => transformChildren(e)

    // return unmodified
    case other => other

  }

  def transformChildren(e: Elem): Seq[Node] = {
    e.copy(child = e.child.flatMap(this.transform))
  }

  def transformChildren(e: Elem, append: Node): Seq[Node] = {
    e.copy(child = e.child.flatMap(this.transform) :+ append)
  }

  def transformChildren(e: Elem, prepend: Node, append: Node): Seq[Node] = {
    e.copy(child = prepend +: e.child.flatMap(this.transform) :+ append)
  }

  def attr(e: Elem, name: String, value: String): Boolean = {
    e.attribute(name).exists(_.text == value)
  }

  def attr(e: Elem, name: String, values: Set[String]): Boolean = {
    e.attribute(name).exists(values contains _.text)
  }

}

object NXMLPreprocessor extends Preprocessor {
  // These are strings serving as substitutions
  val BIBR = "XREF_BIBR" // replaces a bibliography reference
  val FIG = "XREF_FIG" // replaces a figure
  val TABLE = "XREF_TABLE" // replaces a table
  val SUPPL = "XREF_SUPPLEMENTARY" // replaces a supplementary material section
}
