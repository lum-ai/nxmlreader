package ai.lum.common

@SerialVersionUID(1L)
sealed class Interval protected (val start: Int, val stop: Int)
extends IndexedSeq[Int] with Ordered[Interval] with Serializable {

  require(start < stop || (start == 0 && stop == 0), "invalid range")

  import Interval.empty

  override def toString: String = s"Interval($start, $stop)"

  def length: Int = stop - start

  def apply(index: Int): Int = {
    require(index >= 0 && index < length, "invalid index")
    start + index
  }

  def compare(that: Interval): Int = {
    if (this.start > that.start) {
      1
    } else if (this.start < that.start) {
      -1
    } else {
      this.length - that.length
    }
  }

  def contains(i: Int): Boolean = i >= start && i < stop

  def contains(that: Interval): Boolean = {
    this.start <= that.start && this.stop >= that.stop
  }

  def overlaps(that: Interval): Boolean = {
    if (this == empty) {
      false
    } else if (that == empty) {
      false
    } else if (this.start < that.start) {
      this.stop > that.start
    } else if (this.start > that.start) {
      this.start < that.stop
    } else {
      true
    }
  }

}

private[common] object Empty extends Interval(0, 0) {
  override def toString: String = "Empty"
}

object Interval {

  val empty: Interval = Empty

  def apply(i: Int): Interval = new Interval(i, i + 1)

  def apply(start: Int, stop: Int): Interval = {
    if (start == stop) empty else new Interval(start, stop)
  }

  def bySize(start: Int, size: Int): Interval = Interval(start, start + size)

}
