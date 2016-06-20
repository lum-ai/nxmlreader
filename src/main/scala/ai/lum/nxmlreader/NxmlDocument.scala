package ai.lum.nxmlreader

import scala.xml._
import scala.xml.transform.RewriteRule
import scala.collection.mutable.LinkedHashMap

class NxmlDocument(val root: Node) {

  def articleMeta: Node = (root \ "front" \ "article-meta").head

  def getIdByType(idType: String): String = {
    (articleMeta \ "article-id").filter(_ \@ "pub-id-type" == idType).text
  }

  def pmid: String = getIdByType("pmid")
  def pmc: String = getIdByType("pmc")
  def doi: String = getIdByType("doi")

  def pubDate: Seq[PubDate] = for {
    date <- articleMeta \ "pub-date"
    day = (date \ "day").headOption.map(_.text.toInt)
    month = (date \ "month").headOption.map(_.text.toInt)
    year = (date \ "year").text.toInt
    pubType = date \@ "pub-type"
  } yield PubDate(day, month, year, pubType)

  def title: String = (articleMeta \ "title-group" \ "article-title").text

  def paperAbstract: String = getTextFrom((articleMeta \ "abstract").head)

  def authors: Seq[Author] = for {
    contrib <- articleMeta \ "contrib-group" \ "contrib"
    if contrib \@ "contrib-type" == "author"
    surname = contrib \ "name" \ "surname"
    givenNames = contrib \ "name" \ "given-names"
  } yield Author(surname.text, givenNames.text)

  def figures: Seq[Figure] = for {
    fig <- root \\ "fig"
    id = fig \@ "id"
    label = fig \ "label"
    caption = fig \ "caption"
  } yield Figure(id, label.text, getTextFrom(caption.head))

  def tables: Seq[Table] = for {
    tbl <- root \\ "table-wrap"
    id = tbl \@ "id"
    label = tbl \ "label"
    caption = tbl \ "caption"
    xhtml = tbl \\ "table"
  } yield Table(id, label.text, getTextFrom(caption.head), xhtml.head)

  def references: Seq[Reference] = for {
    ref <- root \ "back" \ "ref-list" \ "ref"
    id = ref \@ "id"
    label = ref \ "label"
    title = ref \ "mixed-citation" \ "article-title"
    pubId = ref \ "mixed-citation" \ "pub-id"
    authors = (ref \ "mixed-citation" \ "person-group" \ "name").map(n => Author((n \ "surname").text, (n \ "given-names").text))
  } yield Reference(id, label.text, title.text, authors, PubId(pubId \@ "pub-id-type", pubId.text))

  def getTextFrom(node: Node): String = {
    val preprocess = new PreprocessNxml(Set("supplementary-material"))
    preprocess(node).text
  }

  def paperBody: String = {
    val body = root \\ "body"
    if (body.length == 1) getTextFrom(body.head)
    else ""
  }

  def text: String = s"$title.\n\n\n$paperAbstract\n\n\n$paperBody"

  def mkStandoff = {
    def populate(nodesWithPath: List[(String, Node)], results: List[(String, String)]): List[(String, String)] = nodesWithPath match {
      case (path, Text(string)) :: rest =>
        populate(rest, (path, string) :: results)
      case (path, n) :: rest if n.label == "title" =>
        populate(rest, (s"title $path", n.text) :: results)
      case (path, n) :: rest if n.label == "p" =>
        populate(rest, (s"p $path", n.text) :: results)
      case (path, n) :: rest =>
        val label = n.label
        val newPath = if (path.isEmpty) label else s"$label $path"
        val children = n.child.toList.map(c => (newPath, c))
        populate(children ::: rest, results)
      case Nil => results.reverse
    }
    val preprocess = new PreprocessNxml(Set("supplementary-material"))
    val newRoot = preprocess(root)
    val start = List(
      ("", (newRoot \\ "article-title").head),
      ("", (newRoot \\ "abstract").head),
      ("", (newRoot \\ "body").head)
    )
    val chunks = populate(start, Nil)
    var currIndex = 0
    val ranges = chunks.map { tup =>
      val size = tup._2.length
      val range = (currIndex, currIndex + size - 1)
      currIndex += size
      (tup._1, range)
    }
    val text = chunks.map(_._2).mkString
    (text, ranges)
  }

}

// class Section {
//   // def type: Option[String]
//   def title: String
//   def paragraphs: Seq[String]
//   def sections: Seq[Section]
//   def text: String
// }

// class Figure {
//   def label: String
//   def caption: String
//

case class PubDate(day: Option[Int], month: Option[Int], year: Int, pubType: String)

case class Reference(id: String, label: String, title: String, authors: Seq[Author], pubId: PubId)

case class PubId(idType: String, id: String)

case class Author(surname: String, givenNames: String)

case class Figure(id: String, label: String, caption: String)

case class Table(id: String, label: String, caption: String, xhtml: Node)

class PreprocessNxml(
    val sectionsToIgnore: Set[String] = Set.empty,
    val ignoreFloats: Boolean = true
) extends RewriteRule {

  // surrounds sup/sub content with spaces
  override def transform(n: Node): Seq[Node] = n match {
    // surround subscripts and superscripts with spaces
    case <sup>{text}</sup> => <sup> {text} </sup>
    case <sub>{text}</sub> => <sub> {text} </sub>
    // append dots to title and surround it with newlines
    case e: Elem if e.label == "article-title" =>
      val e2 = transformChildren(e)
      val txt = Text(s"${e2.text}.\n\n")
      <article-title>{txt}</article-title>
    case e: Elem if e.label == "abstract" =>
      val e2 = transformChildren(e)
      val txt = Text(s"\n\n${e2.text}\n\n")
      <abstract>{txt}</abstract>
    case e: Elem if e.label == "title" =>
      val e2 = transformChildren(e)
      val txt = Text(s"\n\n${e2.text}\n\n")
      <title>{txt}</title>
    // append newlines to paragraphs
    case e: Elem if e.label == "p" =>
      val e2 = transformChildren(e)
      val txt = Text(s"${e2.text}\n\n")
      <p>{txt}</p>
    // remove floats
    case e: Elem if ignoreFloats && attr(e, "position", "float") => Nil
    // remove some sections
    case e: Elem if attr(e, "sec-type", sectionsToIgnore) => Nil
    case e: Elem if e.label == "xref" && attr(e, "ref-type", "bibr") => Text("XREF_BIBR")
    case e: Elem if e.label == "xref" && attr(e, "ref-type", "fig") => Text("XREF_FIG")
    case e: Elem if e.label == "xref" && attr(e, "ref-type", "table") => Text("XREF_TABLE")
    case e: Elem if e.label == "xref" && attr(e, "ref-type", "supplementary-material") => Text("XREF_SUPPLEMENTARY")
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
    e.attribute(name).map(_.text == value).getOrElse(false)
  }

  def attr(e: Elem, name: String, values: Set[String]): Boolean = {
    e.attribute(name).map(values contains _.text).getOrElse(false)
  }

}
