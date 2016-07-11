package ai.lum.nxmlreader

import scala.xml._
import ai.lum.common.Interval
import ai.lum.nxmlreader.standoff._


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

  def mkStandoff(): Tree = {
    def mkTree(node: Node, index: Int): Tree = node match {
      case n @ Text(string) =>
        new Terminal(n.label, string, Interval.ofLength(index, string.length))
      case n if n.label == "title" | n.label == "p" =>
        val string = n.text
        new Terminal(n.label, string, Interval.ofLength(index, string.length))
      case n =>
        var idx = index
        val children = for (c <- n.child.toList) yield {
          val t = mkTree(c, idx)
          idx = t.interval.end
          t
        }
        new NonTerminal(n.label, children)
    }
    val newRoot = preprocessor(root)
    val paperTitle = mkTree((newRoot \\ "article-title").head, 0)
    val paperAbstract = mkTree((newRoot \\ "abstract").head, paperTitle.interval.end)
    val paperBody = mkTree((newRoot \\ "body").head, paperAbstract.interval.end)
    new NonTerminal("doc", List(paperTitle, paperAbstract, paperBody))
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

