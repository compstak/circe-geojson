package com.compstak.geojson

import cats._
import cats.syntax.eq._

import scala.{specialized => sp}

/*
A base trait providing a by-name entity identity

All GeoJSON types support a bounding box; see [[https://tools.ietf.org/html/rfc7946#page-12 RFC 7946 5.x]]
 */
sealed trait GeoJson[@sp(Int, Long, Float, Double) A] {
  val bbox: Option[List[Position[A]]]
}

/*
A base trait identifying the GeoJSON types

todo can we assert anything further about coordinate types; they follow (?) a recursive structure
 */
sealed trait GeoJsonGeometry[@sp(Int, Long, Float, Double) A]
    extends GeoJson[A] {
  type G <: Geometry[A]
  val coordinates: G
}

/*
A geometry represented by a single position

See [[https://tools.ietf.org/html/rfc7946#page-8 RFC 7946 3.1.2]]
 */
final case class Point[A](coordinates: Position[A],
                          bbox: Option[List[Position[A]]] = None)
    extends GeoJsonGeometry[A] {
  type G = Position[A]
}

object Point {

  implicit def catsStdEqForPoint[A: Eq]: Eq[Point[A]] =
    new Eq[Point[A]] {
      def eqv(x: Point[A], y: Point[A]): Boolean =
        x.coordinates === y.coordinates
    }
}

/*
A geometry represented by an array of positions

See [[https://tools.ietf.org/html/rfc7946#page-8 RFC 7946 3.1.3]]
 */
final case class MultiPoint[A](coordinates: PositionSet[A],
                               bbox: Option[List[Position[A]]] = None)
    extends GeoJsonGeometry[A] {
  type G = PositionSet[A]
}

object MultiPoint {

  implicit def catsStdEqForMultiPoint[A: Eq]: Eq[MultiPoint[A]] =
    new Eq[MultiPoint[A]] {
      def eqv(x: MultiPoint[A], y: MultiPoint[A]): Boolean =
        x.coordinates.elements
          .forall(y.coordinates.elements.contains) && y.coordinates.elements
          .forall(x.coordinates.elements.contains)
    }
}

/*
A geometry represented by a non-empty array of positions

See [[https://tools.ietf.org/html/rfc7946#page-8 RFC 7946 3.1.4]]
 */
final case class LineString[A](coordinates: Line[A],
                               bbox: Option[List[Position[A]]] = None)
    extends GeoJsonGeometry[A] {
  type G = Line[A]
}

object LineString {

  implicit def catsStdEqForLineString[A: Eq]: Eq[LineString[A]] =
    new Eq[LineString[A]] {
      def eqv(x: LineString[A], y: LineString[A]): Boolean =
        (for {
          xi <- x.coordinates.list
          yi <- y.coordinates.list
        } yield xi === yi).forall(identity)
    }
}

/*
A geometry represented by an array of non-empty arrays of positions

See [[https://tools.ietf.org/html/rfc7946#page-8 RFC 7946 3.1.5]]
 */
final case class MultiLineString[A](coordinates: LineSet[A],
                                    bbox: Option[List[Position[A]]] = None)
    extends GeoJsonGeometry[A] {
  type G = LineSet[A]
}

object MultiLineString {

  implicit def catsStdEqForMultiLineString[A: Eq]: Eq[MultiLineString[A]] =
    new Eq[MultiLineString[A]] {
      def eqv(x: MultiLineString[A], y: MultiLineString[A]): Boolean =
        (for {
          xi <- x.coordinates.elements
          yi <- y.coordinates.elements
        } yield xi === yi).forall(identity)
    }
}

/*
A geometry represented by a non-empty array of non-empty arrays of positions

A polygon differs from a multi- line string in two properties:

(1) the collection of line strings composing the geometry must be non-empty
(2) the line strings must be closed in the strict interpretation context

Closed is more thoroughly defined by [[LRingN]]

Often it is desirable to defer #2 as many clients may not comply with this property.

See [[https://tools.ietf.org/html/rfc7946#page-8 RFC 7946 3.1.6]]
 */
final case class Polygon[A](coordinates: LinearRing[A],
                            bbox: Option[List[Position[A]]] = None)
    extends GeoJsonGeometry[A] {
  type G = LinearRing[A]
}

/*
A geometry represented by an array of non-empty arrays of non-empty arrays of positions

See [[https://tools.ietf.org/html/rfc7946#page-8 RFC 7946 3.1.7]]
 */
final case class MultiPolygon[A](coordinates: RingSet[A],
                                 bbox: Option[List[Position[A]]] = None)
    extends GeoJsonGeometry[A] {
  type G = RingSet[A]
}

/*
See [[https://tools.ietf.org/html/rfc7946#page-8 RFC 7946 3.1.8]]

todo docs
 */
final case class GeometryCollection[F[_]: Traverse, A](
    geometries: F[GeoJsonGeometry[A]],
    bbox: Option[List[Position[A]]] = None)
    extends GeoJson[A]

/*
See [[https://tools.ietf.org/html/rfc7946#page-11 RFC 7946 3.2]]

todo docs
todo per the RFC, the id can be either string or number
 */
final case class Feature[A, P](geometry: GeoJsonGeometry[A],
                               properties: P,
                               id: Option[String] = None,
                               bbox: Option[List[Position[A]]] = None)
    extends GeoJson[A]

/*
See [[https://tools.ietf.org/html/rfc7946#page-12 RFC 7946 3.3]]

todo abstract over the collection type; I had issues deriving circe codecs
 */
final case class FeatureCollection[A, P](features: Seq[Feature[A, P]],
                                         bbox: Option[List[Position[A]]] = None)
    extends GeoJson[A]
