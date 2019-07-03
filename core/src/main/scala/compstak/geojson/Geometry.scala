package compstak.geojson

import cats._
import cats.data._
import cats.implicits._

import scala.{specialized => sp}
import io.circe._
import io.circe.syntax._

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

  implicit def encoderForPosition[N: Encoder]: Encoder[Position[N]] = Encoder.instance {
    case Pos2(x, y)    => List[N](x, y).asJson
    case Pos3(x, y, z) => List[N](x, y, z).asJson
  }

  implicit def decoderForPosition[N: Decoder]: Decoder[Position[N]] = Decoder.instance { cursor =>
    cursor.as[List[N]] match {
      case Right(List(x, y))    => Right(Pos2(x, y))
      case Right(List(x, y, z)) => Right(Pos3(x, y, z))
      case Right(_: List[N]) =>
        Left(DecodingFailure("Coordinates must be two- or three-dimensional geographic positions", cursor.history))
      case _ =>
        Left(DecodingFailure("Expected coordinate set of Array[Double] but not found", cursor.history))
    }
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

  implicit def encoderForPositionSet[N: Encoder]: Encoder[PositionSet[N]] =
    Encoder.instance(_.elements.asJson)

  implicit def decoderForPositionSet[N: Decoder]: Decoder[PositionSet[N]] =
    Decoder.instance { cursor =>
      cursor.as[List[Position[N]]] match {
        case Right(elements) => Right(PositionSet(elements))
        case _ =>
          Left(DecodingFailure("Failed to decoded position set", cursor.history))
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

  implicit def encoderForLine[N: Encoder]: Encoder[Line[N]] =
    Encoder.instance(_.list.asJson)

  implicit def decoderForLine[N: Decoder]: Decoder[Line[N]] = Decoder.instance { cursor =>
    cursor.as[PositionSet[N]] match {
      case Right(c: PositionSet[N]) =>
        NonEmptyList
          .fromList(c.elements)
          .map(pos => Line(pos.head, pos.tail))
          .toRight(DecodingFailure("A line instance must be non-empty", cursor.history))
      case _ =>
        Left(DecodingFailure("A line should be constructed as an array of positions", cursor.history))
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

  implicit def encoderForLineSet[N: Encoder]: Encoder[LineSet[N]] =
    Encoder.instance(_.elements.asJson)

  implicit def decoderForLineSet[N: Decoder]: Decoder[LineSet[N]] = Decoder.instance { cursor =>
    cursor.as[List[Line[N]]] match {
      case Right(elements) => Right(LineSet(elements))
      case _ =>
        Left(DecodingFailure("Failed to decoded position set", cursor.history))
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

  implicit def encoderForLinearRing[N: Encoder]: Encoder[LinearRing[N]] = Encoder.instance {
    case lrn: LRingN[N] => lrn.list.asJson
    case _: LRing0[N]   => Json.Null
  }

  implicit def decoderForLinearRing[N: Eq: Decoder]: Decoder[LinearRing[N]] = Decoder.instance { cursor =>
    val isEmptyOr4Plus: LineSet[N] => Boolean =
      c => c.elements.isEmpty || c.elements.map(_.list.size).toList.sum >= 4

    cursor.as[LineSet[N]] match {
      case Right(c) if isEmptyOr4Plus(c) =>
        LinearRing
          .of(c.elements)
          .leftMap(t => DecodingFailure.fromThrowable(t, cursor.history))
      case Right(c) =>
        Left(DecodingFailure(s"A linear ring must have 0 or 4+ elements, has ${c.elements.size}", cursor.history))
      case Left(_) =>
        Left(DecodingFailure("A linear ring should be constructed as an array of lines", cursor.history))
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

  implicit def encoderForRingSet[N: Encoder]: Encoder[RingSet[N]] =
    Encoder.instance(_.elements.asJson)

  implicit def decoderForRingSet[N: Eq: Decoder]: Decoder[RingSet[N]] = Decoder.instance { cursor =>
    cursor.as[List[LinearRing[N]]] match {
      case Right(elements) => Right(RingSet(elements))
      case _ =>
        Left(DecodingFailure("Failed to decoded position set", cursor.history))
    }
  }
}
