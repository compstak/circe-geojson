package compstak.geojson

import cats._
import cats.data._
import cats.implicits._

import scala.{specialized => sp}

sealed trait Geometry[@sp(Int, Long, Float, Double) A]

/*
A position in geodesics is a 2- or 3-dimensional coordinate representing a position on the Earth ellipsoid
 */
sealed abstract class Position[@sp(Int, Long, Float, Double) A] extends Geometry[A] {
  val x: A
  val y: A
  val z: Option[A]
}

final case class Pos2[A](x: A, y: A) extends Position[A] {
  val z: Option[A] = None
}

final case class Pos3[A](x: A, y: A, private val _z: A) extends Position[A] {
  val z: Option[A] = Some(_z)
}

object Position {
  /*
  NOTE
  It is inaccurate to describe any further kernel behavior here as geospatial operations involving binary operations and
  order are largely context-specific and should be constrained by another algebraic domain.
   */
  implicit def catsStdEqForPosition[A: Eq]: Eq[Position[A]] =
    new Eq[Position[A]] {
      def eqv(x: Position[A], y: Position[A]): Boolean =
        x.x === y.x && x.y === y.y && x.z === y.z
    }
}

// todo collection instances
final case class PositionSet[@sp(Int, Long, Float, Double) A](elements: List[Position[A]]) extends Geometry[A]

object PositionSet {

  implicit def catsStdEqForPositionSet[A: Eq]: Eq[PositionSet[A]] =
    new Eq[PositionSet[A]] {
      def eqv(x: PositionSet[A], y: PositionSet[A]): Boolean =
        x.elements.zip(y.elements).forall { e =>
          val (xx, yy) = e
          xx === yy
        }
    }
}

/*
A line in geodesics is any collection of positions which does not intersect with itself

todo intersection validation
 */
final case class Line[@sp(Int, Long, Float, Double) A](head: Position[A], tail: List[Position[A]]) extends Geometry[A] {

  def list: List[Position[A]] = head +: tail

  def nel: NonEmptyList[Position[A]] = NonEmptyList.of(head, tail: _*)

  // todo figure out how to implement these via type class
  def reverse: Line[A] = Line(nel.reverse.toList.head, nel.reverse.tail)
}

object Line {

  def apply[C[_], A](xs: C[Position[A]])(implicit T: Traverse[C]): Line[A] =
    NonEmptyList
      .fromList(xs.toList)
      .map(pos => Line(pos.head, pos.tail))
      .getOrElse(throw new RuntimeException("Line instance cannot be empty"))

  implicit def catsStdEqForLine[A: Eq]: Eq[Line[A]] =
    new Eq[Line[A]] {
      def eqv(x: Line[A], y: Line[A]): Boolean =
        x.list.zip(y.list).forall { e =>
          val (xx, yy) = e
          xx === yy
        }
    }
}

// todo collection instances
final case class LineSet[@sp(Int, Long, Float, Double) A](elements: List[Line[A]]) extends Geometry[A]

object LineSet {

  implicit def catsStdEqForLineSet[A: Eq]: Eq[LineSet[A]] =
    new Eq[LineSet[A]] {
      def eqv(x: LineSet[A], y: LineSet[A]): Boolean =
        x.elements.zip(y.elements).forall { e =>
          val (xx, yy) = e
          xx === yy
        }
    }
}

/*
A linear ring in geodesics is a special instance of a line string which is also closed. Additionally, a linear ring
must contain either 0 or 4 or more positions.

Closed in this context means that the initial and terminal positions are equal, e.g.:

[
  [  0, 0 ], [  1, 1 ],
  [  1, 1 ], [  0, 2 ],
  [  0, 2 ], [ -1, 1 ],
  [ -1, 1 ], [  0, 0 ]
]

Visualized in cartesian space, this would produce a square which both originates and terminates on [ 0, 0 ].

todo intersection validation
 */

sealed trait LinearRing[@sp(Int, Long, Float, Double) A] extends Geometry[A]

object LinearRing {

  def apply[C[_]: Traverse, A: Eq](xs: C[Line[A]]): LinearRing[A] =
    NonEmptyList
      .fromList(xs.toList)
      .fold[LinearRing[A]](LRing0[A]()) { xs =>
        if (xs.head.head === xs.reverse.head.reverse.head) { // the terminal position is the final element of the final line
          LRingN(xs.head, xs.tail)
        } else throw new RuntimeException("The line string is not closed")
      }

  def ofOption[C[_]: Traverse, A: Eq](xs: C[Line[A]]): Option[LinearRing[A]] =
    of(xs).toOption

  def of[C[_]: Traverse, A: Eq](xs: C[Line[A]]): Either[Throwable, LinearRing[A]] =
    Either.catchNonFatal(LinearRing(xs))

  implicit def catsStdEqForLinearRingSlowOptimistic[A: Eq]: Eq[LinearRing[A]] =
    new Eq[LinearRing[A]] {
      def eqv(x: LinearRing[A], y: LinearRing[A]): Boolean = (x, y) match {
        case (xs: LRingN[A], ys: LRingN[A]) => xs === ys
        case (_: LRing0[A], _: LRing0[A])   => true
        case _                              => false
      }
    }
}

/*
The empty closed ring
 */
final case class LRing0[A]() extends LinearRing[A]

/*
The standard closed ring

// todo fix the arity to ensure 4
 */
final case class LRingN[A](head: Line[A], tail: List[Line[A]]) extends LinearRing[A] {

  // todo move this to a ListOps type class
  def list: List[Line[A]] = head +: tail

  // todo move this to a ListOps type class
  def nel: NonEmptyList[Line[A]] = NonEmptyList.of(head, tail: _*)

  // todo figure out how to implement these via type class
  def reverse: LRingN[A] = LRingN(nel.reverse.head, nel.reverse.tail)
}

object LRingN {

  implicit def catsStdEqForLRingNSlowOptimistic[A: Eq]: Eq[LRingN[A]] =
    new Eq[LRingN[A]] {
      def eqv(x: LRingN[A], y: LRingN[A]): Boolean =
        x.list.zip(y.list).forall { e =>
          val (xx, yy) = e
          xx === yy
        }
    }
}

// todo collection instances
final case class RingSet[A](elements: List[LinearRing[A]]) extends Geometry[A]

object RingSet {

  implicit def catsStdEqForRingSet[A: Eq]: Eq[RingSet[A]] =
    new Eq[RingSet[A]] {
      def eqv(x: RingSet[A], y: RingSet[A]): Boolean =
        x.elements.zip(y.elements).forall { e =>
          val (xx, yy) = e
          xx === yy
        }
    }
}
