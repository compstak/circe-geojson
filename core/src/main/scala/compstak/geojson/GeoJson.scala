package compstak.geojson

import cats._
import cats.implicits._

import scala.{specialized => sp}
import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._
import GeoJsonCodec.{baseEncoder, decodeBoundingBox, mkType}

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
sealed trait GeoJsonGeometry[@sp(Int, Long, Float, Double) A] extends GeoJson[A] {
  type G <: Geometry[A]
  val coordinates: G
}

object GeoJsonGeometry {
  implicit def encoderForGeometry[N: Encoder]: Encoder[GeoJsonGeometry[N]] =
    Encoder
      .instance {
        case p: Point[N] =>
          Encoder[Point[N]].apply(p).deepMerge(mkType(GeometryType.Point))
        case m: MultiPoint[N] =>
          Encoder[MultiPoint[N]].apply(m).deepMerge(mkType(GeometryType.MultiPoint))
        case l: LineString[N] =>
          Encoder[LineString[N]].apply(l).deepMerge(mkType(GeometryType.LineString))
        case m: MultiLineString[N] =>
          Encoder[MultiLineString[N]].apply(m).deepMerge(mkType(GeometryType.MultiLineString))
        case p: Polygon[N] =>
          Encoder[Polygon[N]].apply(p).deepMerge(mkType(GeometryType.Polygon))
        case m: MultiPolygon[N] =>
          Encoder[MultiPolygon[N]].apply(m).deepMerge(mkType(GeometryType.MultiPolygon))
      }

  implicit def decoderForGeometry[N: Eq: Decoder]: Decoder[GeoJsonGeometry[N]] =
    Decoder
      .instance { cursor =>
        cursor
          .downField("type")
          .as[GeometryType]
          .flatMap {
            case GeometryType.Point           => cursor.as[Point[N]]
            case GeometryType.MultiPoint      => cursor.as[MultiPoint[N]]
            case GeometryType.LineString      => cursor.as[LineString[N]]
            case GeometryType.MultiLineString => cursor.as[MultiLineString[N]]
            case GeometryType.Polygon         => cursor.as[Polygon[N]]
            case GeometryType.MultiPolygon    => cursor.as[MultiPolygon[N]]
            // todo case "GeometryCollection" => cursor.as[GeometryCollection[List, N]]
          }
      }
}

/*
A geometry represented by a single position

See [[https://tools.ietf.org/html/rfc7946#page-8 RFC 7946 3.1.2]]
 */
final case class Point[A](coordinates: Position[A], bbox: Option[List[Position[A]]] = None) extends GeoJsonGeometry[A] {
  type G = Position[A]
}

object Point {

  implicit def catsStdEqForPoint[A: Eq]: Eq[Point[A]] =
    new Eq[Point[A]] {
      def eqv(x: Point[A], y: Point[A]): Boolean =
        x.coordinates === y.coordinates
    }

  implicit def encoderForPoint[N: Encoder]: Encoder[Point[N]] =
    Encoder.instance(baseEncoder[N].apply(_))
  implicit def decoderForPoint[N: Decoder]: Decoder[Point[N]] = deriveDecoder[Point[N]]
}

/*
A geometry represented by an array of positions

See [[https://tools.ietf.org/html/rfc7946#page-8 RFC 7946 3.1.3]]
 */
final case class MultiPoint[A](coordinates: PositionSet[A], bbox: Option[List[Position[A]]] = None)
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

  implicit def encoderForMultiPoint[N: Encoder]: Encoder[MultiPoint[N]] =
    Encoder.instance(baseEncoder[N].apply(_))
  implicit def decoderForMultiPoint[N: Decoder]: Decoder[MultiPoint[N]] =
    deriveDecoder[MultiPoint[N]]
}

/*
A geometry represented by a non-empty array of positions

See [[https://tools.ietf.org/html/rfc7946#page-8 RFC 7946 3.1.4]]
 */
final case class LineString[A](coordinates: Line[A], bbox: Option[List[Position[A]]] = None)
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

  implicit def encoderForLineString[N: Encoder]: Encoder[LineString[N]] =
    Encoder.instance(baseEncoder[N].apply(_))
  implicit def decoderForLineString[N: Decoder]: Decoder[LineString[N]] =
    deriveDecoder[LineString[N]]
}

/*
A geometry represented by an array of non-empty arrays of positions

See [[https://tools.ietf.org/html/rfc7946#page-8 RFC 7946 3.1.5]]
 */
final case class MultiLineString[A](coordinates: LineSet[A], bbox: Option[List[Position[A]]] = None)
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

  implicit def encoderForMultiLineString[N: Encoder]: Encoder[MultiLineString[N]] =
    Encoder.instance(baseEncoder[N].apply(_))
  implicit def decoderForMultiLineString[N: Decoder]: Decoder[MultiLineString[N]] =
    deriveDecoder[MultiLineString[N]]
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
final case class Polygon[A](coordinates: LinearRing[A], bbox: Option[List[Position[A]]] = None)
    extends GeoJsonGeometry[A] {
  type G = LinearRing[A]
}

object Polygon {
  implicit def encoderForPolygon[N: Encoder]: Encoder[Polygon[N]] =
    Encoder.instance(baseEncoder[N].apply(_))
  implicit def decoderForPolygon[N: Eq: Decoder]: Decoder[Polygon[N]] =
    deriveDecoder[Polygon[N]]
}

/*
A geometry represented by an array of non-empty arrays of non-empty arrays of positions

See [[https://tools.ietf.org/html/rfc7946#page-8 RFC 7946 3.1.7]]
 */
final case class MultiPolygon[A](coordinates: RingSet[A], bbox: Option[List[Position[A]]] = None)
    extends GeoJsonGeometry[A] {
  type G = RingSet[A]
}

object MultiPolygon {
  implicit def encoderForMultiPolygon[N: Encoder]: Encoder[MultiPolygon[N]] =
    Encoder.instance(baseEncoder[N].apply(_))
  implicit def decoderForMultiPolygon[N: Decoder: Eq]: Decoder[MultiPolygon[N]] =
    deriveDecoder[MultiPolygon[N]]
}

/*
See [[https://tools.ietf.org/html/rfc7946#page-8 RFC 7946 3.1.8]]

todo docs
 */
final case class GeometryCollection[F[_]: Traverse, A](geometries: F[GeoJsonGeometry[A]],
                                                       bbox: Option[List[Position[A]]] = None)
    extends GeoJson[A]

object GeometryCollection {
  implicit def encoderForGeometryCollection[F[_], N: Encoder](
    implicit E: Encoder[F[GeoJsonGeometry[N]]]
  ): Encoder[GeometryCollection[F, N]] =
    Encoder
      .instance { coll =>
        Json.obj(("geometries", coll.geometries.asJson)).deepMerge(mkType(GeometryType.Point))
      }

  implicit def decoderForGeometryCollections[F[_]: Traverse, N: Eq: Decoder](
    implicit D: Decoder[F[GeoJsonGeometry[N]]]
  ): Decoder[GeometryCollection[F, N]] =
    Decoder
      .instance { cursor =>
        for {
          geometries <- cursor.downField("geometries").as[F[GeoJsonGeometry[N]]]
          bbox <- decodeBoundingBox[N](cursor)
        } yield GeometryCollection[F, N](geometries, bbox)
      }
}

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

object Feature {
  implicit def encoderForFeature[N: Encoder, P: Encoder]: Encoder[Feature[N, P]] =
    Encoder
      .instance { feature =>
        Json.obj(
          ("id", feature.id.asJson),
          ("geometry", feature.geometry.asJson),
          ("type", "Feature".asJson),
          ("properties", feature.properties.asJson),
          ("bbox", feature.bbox.asJson)
        )
      }

  implicit def decoderForFeature[N: Eq: Decoder, P: Decoder]: Decoder[Feature[N, P]] =
    Decoder
      .instance { cursor =>
        for {
          t <- cursor
            .downField("type")
            .as[String]
          _ <- Either.cond(t === "Feature", t, DecodingFailure("The element is not a feature", cursor.history))
          geometry <- cursor.downField("geometry").as[GeoJsonGeometry[N]]
          properties <- cursor.downField("properties").as[P]
          id <- cursor.downField("id").as[Option[String]]
          bbox <- decodeBoundingBox[N](cursor)
        } yield Feature[N, P](geometry, properties, id, bbox)
      }
}

/*
See [[https://tools.ietf.org/html/rfc7946#page-12 RFC 7946 3.3]]

todo abstract over the collection type; I had issues deriving circe codecs
 */
final case class FeatureCollection[A, P](features: Seq[Feature[A, P]], bbox: Option[List[Position[A]]] = None)
    extends GeoJson[A]

object FeatureCollection {
  implicit def encoderForFeatureCollection[N: Encoder, P: Encoder]: Encoder[FeatureCollection[N, P]] =
    Encoder
      .instance { coll: FeatureCollection[N, P] =>
        Json.obj(
          ("type", "FeatureCollection".asJson),
          ("features", coll.features.asJson)
        )
      }

  implicit def decoderForFeatureCollection[N: Eq: Decoder, P: Decoder]: Decoder[FeatureCollection[N, P]] =
    Decoder
      .instance { cursor =>
        for {
          features <- cursor
            .downField("features")
            .as[Seq[Feature[N, P]]]
          bbox <- decodeBoundingBox[N](cursor)
        } yield FeatureCollection[N, P](features, bbox)
      }
}
