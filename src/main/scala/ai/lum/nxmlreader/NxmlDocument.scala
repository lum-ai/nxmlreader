package ai.lum.nxmlreader

import scala.xml._
import ai.lum.common.Interval
import ai.lum.nxmlreader.standoff._

import scala.annotation.tailrec


class NxmlDocument(val root: Node, val preprocessor: Preprocessor) {

  def articleMeta: Node = (root \\ "front" \ "article-meta").head

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

  def inTextCitations: Seq[Tree] = {
    val xrefs = findXrefs(Seq(standoff), Nil)
    xrefs.filter(xref => xref.attributes("ref-type") == "bibr")
  }

  @tailrec
  private def findXrefs(remaining: Seq[Tree], results: Seq[Tree]): Seq[Tree] = remaining match {
    case Seq() => results
    case (n:NonTerminal) +: rest => findXrefs(n.children ++ rest, results)
    case (t:Terminal) +: rest if t.label == "xref" => findXrefs(rest, t +: results)
    case (t:Terminal) +: rest => findXrefs(rest, results)
  }

  def references: Seq[Reference] = for {
    ref <- root \ "back" \ "ref-list" \ "ref"
    id = ref \@ "id"
    label = ref \ "label"
    title = ref \ "mixed-citation" \ "article-title"
    pubId = ref \ "mixed-citation" \ "pub-id"
    authors = (ref \ "mixed-citation" \ "person-group" \ "name").map(n => Author((n \ "surname").text, (n \ "given-names").text))
  } yield Reference(id, label.text, title.text, authors, PubId(pubId \@ "pub-id-type", pubId.text))

  def getTextFrom(node: Node): String = {
    preprocessor(node).text
  }

  def paperBody: String = {
    val body = root \\ "body"
    if (body.length == 1) getTextFrom(body.head)
    else ""
  }

  private def mkStandoff(): Tree = {
    def mkTree(node: Node, index: Int): Option[Tree] = node match {
      case n @ Text(string) =>
        Some(new Terminal(n.label, string, Interval.ofLength(index, string.length)))
      case n if n.label == "title" =>
        val string = n.text
        Some(new Terminal(n.label, string, Interval.ofLength(index, string.length)))
      case n if n.label == "xref" =>
        val string = n.text
        val attributes = n.attributes.map(b => b.key -> b.value.text).toMap
        Some(new Terminal(n.label, string, Interval.ofLength(index, string.length), attributes))
      case n if n.child.isEmpty =>
        // if nonterminal has no children, don't make a tree node
        None
      case n =>
        var idx = index
        val children = n.child.toList.flatMap { c =>
          val t = mkTree(c, idx)
          if (t.isDefined) {
            idx = t.get.characterInterval.end
          }
          t
        }
        if (children.isEmpty) return None
        // Keep track of the tag's attributes as a Map[String, String]
        val attributes = n.attributes.map(b => (b.key -> b.value.text)).toMap
        Some(new NonTerminal(n.label, children, attributes))
    }
    val newRoot = preprocessor(root)
    // here i am assuming that papers *always* have a title
    val paperTitle = mkTree((newRoot \\ "article-title").head, 0).get
    // some papers don't have an abstract
    // also, mkTree returns an Option, that is why we use flatMap
    val paperAbstractOption = (newRoot \\ "abstract").headOption.flatMap(mkTree(_, paperTitle.characterInterval.end))
    // next start is the end of the abstract if there is one, or the end of the title
    val nextStart = paperAbstractOption.map(_.characterInterval.end).getOrElse(paperTitle.characterInterval.end)
    // sometimes the body is missing
    // sometimes the body is empty, this is handled by mkTree returning None
    val paperBodyOption = (newRoot \\ "body").headOption.flatMap(mkTree(_, nextStart))
    val children = paperTitle :: paperAbstractOption.map(List(_)).getOrElse(Nil) ::: paperBodyOption.map(List(_)).getOrElse(Nil)
    new NonTerminal("doc", children, Map())
  }

  val standoff: Tree = mkStandoff()
  def text = standoff.text

}

case class PubDate(day: Option[Int], month: Option[Int], year: Int, pubType: String)

case class Reference(id: String, label: String, title: String, authors: Seq[Author], pubId: PubId)

case class PubId(idType: String, id: String)

case class Author(surname: String, givenNames: String)

case class Figure(id: String, label: String, caption: String)

case class Table(id: String, label: String, caption: String, xhtml: Node)
