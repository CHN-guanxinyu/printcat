package com.keene.core.implicits

import com.keene.core.parsers.{Arguments, ArgumentsParser}

import scala.math.{min, random}
import scala.reflect.ClassTag

/**
  *
  * @param t
  * @tparam T
  */
case class AnyImplicitor[T](@transient t : T)
  (implicit tag : ClassTag[T]) {
}

case class StringImplicitor(@transient str : String) {


  /**
    * Usage: "package.to.classA".as[classA].foo.bar
    *
    * @tparam T
    * @return
    */
  def as[T]: T = as()
  def as[T](args: Object*): T =
    Class.forName(str).getDeclaredConstructor(args.map(_.getClass) : _*).newInstance(args: _*).asInstanceOf[T]

  def headToUpper = str.head.toUpper + str.tail

  def removeBlanks = replaceBlanks("")

  def replaceBlanks() : String = replaceBlanks(" ")

  def replaceBlanks(replacement : String) : String = {
    str.replaceAll("\t|\r|\n", replacement).replaceAll(" {2,}", replacement).trim
  }
}

/**
  *
  * 未解决的问题:MapImplicitor
  */
case object TraversableImlicitor {

  def apply[T](t : Traversable[T]) = t match {
    case _ : Set[T] => SetImplicitor(t.toSet)
    case _ : Seq[T] => SeqImplicitor(t.toList)
    //TODO:MapImplicitor
  }
}

case class SetImplicitor[T](@transient set : Set[T])
  extends TraversableImlicitor[T] {
  override def sample(n : Int) = SeqImplicitor(set.toSeq) sample n toSet
}

case class SeqImplicitor[T](@transient seq : Seq[T])
  extends TraversableImlicitor[T] {
  /**
    * Seq的sample方法性能较高,所以使用的时候需要可
    * 能的使用Seq的sample方法,以消除多余的转换遍历
    *
    * @param n
    * @return
    */
  override def sample(n : Int) = {
    if (seq.size / n == 0) seq
    else {
      val groupSize = seq.size / n + (if (seq.size % n > n / 2) 1 else 0)
      val b = Seq.newBuilder[T]
      var i = 0
      while (i < n) {
        b += seq(i * groupSize + random * (min(seq.size, (i + 1) * groupSize) - i * groupSize) toInt)
        i += 1
      }
      b.result
    }
  }
}

case class MapImplicitor[T, U](@transient map : Map[T, U])
  extends TraversableImlicitor[(T, U)] {
  override def sample(n : Int) = SetImplicitor(map.keySet) sample n map (x => (x, map(x))) toMap
}

trait TraversableImlicitor[T] {
  def sample() : Traversable[T] = sample(20)

  def sample(n : Int) : Traversable[T]
}

case class ArrayImplicitor[T](@transient array : Array[T]) {
  /**
    * 针对参数解析
    *
    * @tparam U
    * @return
    */
  def as[U <: Arguments](implicit tag : ClassTag[U]) =
    ArgumentsParser[U](array map (_ toString))


}
