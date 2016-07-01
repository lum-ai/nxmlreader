package ai.lum.nxmlreader.standoff

import ai.lum.common.Interval

sealed trait Tree {

  private[standoff] var _parent: Option[Tree] = None
  def parent: Option[Tree] = _parent

  def label: String
  def text: String
  def interval: Interval
  def children: List[Tree]
  def copy(): Tree
  def getTerminals(i: Interval): List[Terminal]

  def getTerminals(i: Int): List[Terminal] = {
    getTerminals(Interval.singleton(i))
  }

  def getTerminals(start: Int, end: Int): List[Terminal] = {
    getTerminals(Interval.open(start, end))
  }

  def root: Tree = parent match {
    case None => this
    case Some(p) => p.root
  }

  def ancestors: List[Tree] = parent match {
    case None => Nil
    case Some(p) => p :: p.ancestors
  }

  def path: String = ancestors.map(_.label).mkString(" ")

}

class NonTerminal(
    val label: String,
    val children: List[Tree]
) extends Tree {

  require(children.forall(_.parent.isEmpty), "children already have a parent")

  children.foreach(_._parent = Some(this))

  val interval: Interval = Interval.union(children.map(_.interval))

  def text: String = children.map(_.text).mkString

  def copy(): Tree = new NonTerminal(label, children.map(_.copy()))

  def getTerminals(i: Interval): List[Terminal] = {
    if (i intersects interval) {
      for {
        c <- children
        t <- c.getTerminals(i)
      } yield t
    } else {
      Nil
    }
  }

}

class Terminal(
    val label: String,
    val text: String,
    val interval: Interval
) extends Tree {

  val children: List[Tree] = Nil

  def copy(): Tree = new Terminal(label, text, interval)

  def getTerminals(i: Interval): List[Terminal] = {
    if (i intersects interval) List(this) else Nil
  }

}
