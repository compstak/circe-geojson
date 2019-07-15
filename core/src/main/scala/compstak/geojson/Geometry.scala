package compstak.geojson

import cats._
import cats.data._
import cats.implicits._

import scala.{specialized => sp}
import io.circe._
import io.circe.syntax._
import scala.util.control.NonFatal

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
final case class Line[@sp(Int, Long, Float, Double) A](head: Position[A], tail: NonEmptyList[Position[A]])
    extends Geometry[A] {

  def list: List[Position[A]] = head +: tail.toList

  def nel: NonEmptyList[Position[A]] = head :: tail

  // todo figure out how to implement these via type class
  def reverse: Line[A] = Line(tail.last, NonEmptyList.ofInitLast(nel.reverse.tail, head))
}

object Line {

  def apply[C[_]: Reducible, A](start: A, xs: C[Position[A]]): Line[A] =
    Line(start, xs.toNonEmptyList)

  def unsafeFromFoldable[C[_]: Foldable, A](xs: C[Position[A]]): Line[A] = xs.toList match {
    case a :: b :: tail => Line(a, NonEmptyList(b, tail))
    case _              => throw new RuntimeException("Line instance cannot be empty")
  }

  def fromFoldable[C[_]: Foldable, A](xs: C[Position[A]]): Option[Line[A]] =
    try { Some(unsafeFromFoldable(xs)) } catch { case NonFatal(_) => None }

  implicit def catsStdEqForLine[A: Eq]: Eq[Line[A]] =
    new Eq[Line[A]] {
      def eqv(x: Line[A], y: Line[A]): Boolean = {
        val xl = x.list
        val yl = y.list

        xl.size === yl.size && xl.zip(yl).forall { e =>
          val (xx, yy) = e
          xx === yy
        }
      }
    }

  implicit def encoderForLine[N: Encoder]: Encoder[Line[N]] =
    Encoder.instance(_.list.asJson)

  implicit def decoderForLine[N: Decoder]: Decoder[Line[N]] = Decoder.instance { cursor =>
    cursor.as[PositionSet[N]] match {
      case Right(c: PositionSet[N]) =>
        fromFoldable(c.elements)
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
      def eqv(x: LineSet[A], y: LineSet[A]): Boolean = {
        val xl = x.elements
        val yl = y.elements

        xl.size === yl.size && xl.zip(yl).forall { e =>
          val (xx, yy) = e
          xx === yy
        }
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

sealed abstract case class LinearRing[@sp(Int, Long, Float, Double) A](a: Position[A],
                                                                       b: Position[A],
                                                                       c: Position[A],
                                                                       rest: NonEmptyList[Position[A]])
    extends Geometry[A] {
  def list: List[Position[A]] = a :: b :: c :: rest.toList

  // todo move this to a ListOps type class
  def nel: NonEmptyList[Position[A]] = NonEmptyList.of(a, b, c) ::: rest

  // todo figure out how to implement these via type class
  //def reverse: LinearRing[A] = LRingN(nel.reverse.head, nel.reverse.tail)
}

object LinearRing {

  def apply[C[_]: Reducible, A: Eq](xs: C[Position[A]]): Option[LinearRing[A]] =
    of(xs).toOption

  def unsafeCreate[C[_]: Reducible, A: Eq](xs: C[Position[A]]): LinearRing[A] =
    xs.toNonEmptyList match {
      case NonEmptyList(a, b :: c :: d :: tail) if (a === tail.lastOption.getOrElse(d)) =>
        new LinearRing[A](a, b, c, NonEmptyList(d, tail)) {}
      case _ => throw new IllegalArgumentException("Not a valid closed Linear Ring")
    }

  def fromFoldable[C[_]: Foldable, A: Eq](xs: C[Position[A]]): Either[Throwable, LinearRing[A]] =
    Either.catchNonFatal(unsafeFromFoldable(xs))

  def unsafeFromFoldable[C[_]: Foldable, A: Eq](xs: C[Position[A]]): LinearRing[A] =
    unsafeCreate(NonEmptyList.fromFoldable(xs).get)

  def of[C[_]: Reducible, A: Eq](xs: C[Position[A]]): Either[IllegalArgumentException, LinearRing[A]] =
    Either.catchOnly[IllegalArgumentException](unsafeCreate(xs))

  implicit def catsStdEqForLinearRingSlowOptimistic[A: Eq]: Eq[LinearRing[A]] =
    new Eq[LinearRing[A]] {
      def eqv(x: LinearRing[A], y: LinearRing[A]): Boolean =
        x.a === y.a && x.b === y.b && x.c === y.c && x.rest === y.rest
    }

  implicit def encoderForLinearRing[N: Encoder]: Encoder[LinearRing[N]] =
    Encoder[List[Position[N]]].contramap(_.list)

  implicit def decoderForLinearRing[N: Eq: Decoder]: Decoder[LinearRing[N]] = Decoder.instance { cursor =>
    cursor
      .as[List[Position[N]]]
      .flatMap(list => fromFoldable(list).leftMap(ex => DecodingFailure(ex.getMessage, cursor.history)))
  }
}

// todo collection instances
final case class RingSet[A](elements: List[LinearRing[A]]) extends Geometry[A]

object RingSet {

  implicit def catsStdEqForRingSet[A: Eq]: Eq[RingSet[A]] =
    new Eq[RingSet[A]] {
      def eqv(x: RingSet[A], y: RingSet[A]): Boolean = {
        val xl = x.elements
        val yl = y.elements

        xl.size === yl.size && xl.zip(yl).forall { e =>
          val (xx, yy) = e
          xx === yy
        }
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

final case class PolygonSet[A](elements: List[RingSet[A]]) extends Geometry[A]

object PolygonSet {

  implicit def catsStdEqForRingSet[A: Eq]: Eq[PolygonSet[A]] =
    new Eq[PolygonSet[A]] {
      def eqv(x: PolygonSet[A], y: PolygonSet[A]): Boolean = {
        val xl = x.elements
        val yl = y.elements

        xl.size === yl.size && xl.zip(yl).forall { e =>
          val (xx, yy) = e
          xx === yy
        }
      }
    }

  implicit def encoderForRingSet[N: Encoder]: Encoder[PolygonSet[N]] =
    Encoder.instance(_.elements.asJson)

  implicit def decoderForRingSet[N: Eq: Decoder]: Decoder[PolygonSet[N]] = Decoder.instance { cursor =>
    cursor.as[List[RingSet[N]]] match {
      case Right(elements) => Right(PolygonSet(elements))
      case _ =>
        Left(DecodingFailure("Failed to decoded position set", cursor.history))
    }
  }
}
