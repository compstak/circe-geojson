package compstak.geojson

import cats._
import cats.implicits._

import scala.{specialized => sp}
import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._
import GeoJsonCodec.{baseDecoder, baseEncoder}
import cats.data.NonEmptyList

/*
A base trait providing a by-name entity identity

All GeoJSON types support a bounding box; see [[https://tools.ietf.org/html/rfc7946#page-12 RFC 7946 5.x]]
 */
sealed abstract class GeoJson[@sp(Int, Long, Float, Double) A, P](val `type`: GeoJsonObjectType) {
  val bbox: Option[BoundingBox[A]]
}

object GeoJson extends GeoJsonLowPriorityImplicits {

  implicit def encoderForGeoJson[N: Encoder, P: Encoder]: Encoder[GeoJson[N, P]] = {
    case g: GeoJsonGeometry[_] =>
      g.asJson
    case gc: GeometryCollection[_] =>
      gc.asJson
    case f: Feature[N, _] =>
      f.asJson
    case fc: FeatureCollection[N, _] =>
      fc.asJson
  }

  implicit def decoderForGeoJsonUnit[N: Eq: Decoder]: Decoder[GeoJson[N, Unit]] = { cursor =>
    cursor
      .downField("type")
      .as[GeoJsonObjectType]
      .flatMap {
        case g: GeometryType                      => cursor.as[GeoJsonGeometry[N]]
        case GeoJsonObjectType.GeometryCollection => cursor.as[GeometryCollection[N]]
        case GeoJsonObjectType.Feature            => cursor.as[Feature[N, Unit]]
        case GeoJsonObjectType.FeatureCollection  => cursor.as[FeatureCollection[N, Unit]]
      }
  }

}

trait GeoJsonLowPriorityImplicits {
  implicit def decoderForGeoJson[N: Eq: Decoder, P: Decoder]: Decoder[GeoJson[N, P]] = { cursor =>
    cursor
      .downField("type")
      .as[GeoJsonObjectType]
      .flatMap {
        case GeoJsonObjectType.Feature           => cursor.as[Feature[N, P]]
        case GeoJsonObjectType.FeatureCollection => cursor.as[FeatureCollection[N, P]]
        case _                                   => Left(DecodingFailure("Should never happen, please open an issue in the GitHub repo", cursor.history))
      }
  }
}

/*
A base trait identifying the GeoJSON types

todo can we assert anything further about coordinate types; they follow (?) a recursive structure
 */
sealed abstract class GeoJsonGeometry[A](override val `type`: GeometryType) extends GeoJson[A, Unit](`type`) {
  type G <: Geometry[A]
  val coordinates: G
}

object GeoJsonGeometry {
  implicit def encoderForGeometry[N: Encoder]: Encoder[GeoJsonGeometry[N]] = {
    case p: Point[N] =>
      p.asJson
    case m: MultiPoint[N] =>
      m.asJson
    case l: LineString[N] =>
      l.asJson
    case m: MultiLineString[N] =>
      m.asJson
    case p: Polygon[N] =>
      p.asJson
    case m: MultiPolygon[N] =>
      m.asJson
  }

  implicit def decoderForGeometry[N: Eq: Decoder]: Decoder[GeoJsonGeometry[N]] = { cursor =>
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
      }
  }
}

case class BoundingBox[A](llb: Position[A], urt: Position[A])

object BoundingBox {
  implicit def eqForBoundingBox[A: Eq]: Eq[BoundingBox[A]] =
    Eq.instance((x, y) => x.llb === y.llb && x.urt === y.urt)

  implicit def encoder[A: Encoder]: Encoder[BoundingBox[A]] =
    Encoder[List[A]].contramap { bb =>
      (bb.llb.z, bb.urt.z).tupled match {
        case Some((llbZ, urtZ)) => List(bb.llb.x, bb.llb.y, llbZ, bb.urt.x, bb.urt.y, urtZ)
        case None               => List(bb.llb.x, bb.llb.y, bb.urt.x, bb.urt.y)
      }
    }

  implicit def decoder[A: Decoder]: Decoder[BoundingBox[A]] =
    Decoder[List[A]].emap {
      case llbX :: llbY :: llbZ :: urtX :: urtY :: urtZ :: Nil =>
        Right(BoundingBox(Pos3(llbX, llbY, llbZ), Pos3(urtX, urtY, urtZ)))
      case llbX :: llbY :: urtX :: urtY :: Nil =>
        Right(BoundingBox(Pos2(llbX, llbY), Pos2(urtX, urtY)))
      case _ => Left("Not a valid bounding box")
    }
}

/*
A geometry represented by a single position

See [[https://tools.ietf.org/html/rfc7946#page-8 RFC 7946 3.1.2]]
 */
final case class Point[A](coordinates: Position[A], bbox: Option[BoundingBox[A]] = None)
    extends GeoJsonGeometry[A](GeometryType.Point) {
  type G = Position[A]
}

object Point {
  implicit def catsStdEqForPoint[A: Eq]: Eq[Point[A]] =
    new Eq[Point[A]] {
      def eqv(x: Point[A], y: Point[A]): Boolean =
        x.coordinates === y.coordinates
    }

  implicit def encoderForPoint[N: Encoder]: Encoder[Point[N]] =
    baseEncoder[N].apply(_)
  implicit def decoderForPoint[N: Decoder]: Decoder[Point[N]] =
    baseDecoder[N].make[Position[N], Point[N]](Point.apply[N])
}

/*
A geometry represented by an array of positions

See [[https://tools.ietf.org/html/rfc7946#page-8 RFC 7946 3.1.3]]
 */
final case class MultiPoint[A](coordinates: PositionSet[A], bbox: Option[BoundingBox[A]] = None)
    extends GeoJsonGeometry[A](GeometryType.MultiPoint) {
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
    baseEncoder[N].apply(_)
  implicit def decoderForMultiPoint[N: Decoder]: Decoder[MultiPoint[N]] =
    baseDecoder[N].make[PositionSet[N], MultiPoint[N]](MultiPoint.apply[N])
}

/*
A geometry represented by a non-empty array of positions

See [[https://tools.ietf.org/html/rfc7946#page-8 RFC 7946 3.1.4]]
 */
final case class LineString[A](coordinates: Line[A], bbox: Option[BoundingBox[A]] = None)
    extends GeoJsonGeometry[A](GeometryType.LineString) {
  type G = Line[A]
}

object LineString {
  implicit def catsStdEqForLineString[A: Eq]: Eq[LineString[A]] =
    new Eq[LineString[A]] {
      def eqv(x: LineString[A], y: LineString[A]): Boolean =
        x.coordinates === y.coordinates && x.bbox === y.bbox
    }

  implicit def encoderForLineString[N: Encoder]: Encoder[LineString[N]] =
    baseEncoder[N].apply(_)
  implicit def decoderForLineString[N: Decoder]: Decoder[LineString[N]] =
    baseDecoder[N].make[Line[N], LineString[N]](LineString.apply[N])
}

/*
A geometry represented by an array of non-empty arrays of positions

See [[https://tools.ietf.org/html/rfc7946#page-8 RFC 7946 3.1.5]]
 */
final case class MultiLineString[A](coordinates: LineSet[A], bbox: Option[BoundingBox[A]] = None)
    extends GeoJsonGeometry[A](GeometryType.MultiLineString) {
  type G = LineSet[A]
}

object MultiLineString {
  implicit def catsStdEqForMultiLineString[A: Eq]: Eq[MultiLineString[A]] =
    new Eq[MultiLineString[A]] {
      def eqv(x: MultiLineString[A], y: MultiLineString[A]): Boolean =
        x.coordinates === y.coordinates && x.bbox === y.bbox
    }

  implicit def encoderForMultiLineString[N: Encoder]: Encoder[MultiLineString[N]] =
    baseEncoder[N].apply(_)
  implicit def decoderForMultiLineString[N: Decoder]: Decoder[MultiLineString[N]] =
    baseDecoder[N].make[LineSet[N], MultiLineString[N]](MultiLineString.apply[N])
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
final case class Polygon[A](coordinates: RingSet[A], bbox: Option[BoundingBox[A]] = None)
    extends GeoJsonGeometry[A](GeometryType.Polygon) {
  type G = RingSet[A]
}

object Polygon {
  implicit def encoderForPolygon[N: Encoder]: Encoder[Polygon[N]] =
    baseEncoder[N].apply(_)
  implicit def decoderForPolygon[N: Eq: Decoder]: Decoder[Polygon[N]] =
    baseDecoder[N]
      .make[RingSet[N], Polygon[N]](Polygon.apply[N])
      .or(Decoder.instance { cursor =>
        val isEmptyOr4Plus: List[Line[N]] => Boolean =
          c => c.isEmpty || c.map(_.list.size).toList.sum >= 4

        cursor
          .downField("coordinates")
          .as[List[Line[N]]]
          .flatMap(lines =>
            if (isEmptyOr4Plus(lines))
              fromLines(lines).leftMap(ex => DecodingFailure(ex.getMessage, cursor.history))
            else
              Left(DecodingFailure(s"A linear ring must have 0 or 4+ elements, has ${lines.size}", cursor.history))
          )
      })

  def fromLines[N: Eq](lines: List[Line[N]]): Either[IllegalArgumentException, Polygon[N]] =
    NonEmptyList.fromList(lines.flatMap(_.list)) match {
      case Some(nel) => LinearRing.of(nel).map(lr => Polygon(RingSet(lr :: Nil)))
      case None      => Right(Polygon(RingSet[N](List.empty)))
    }
}

/*
A geometry represented by an array of non-empty arrays of non-empty arrays of positions

See [[https://tools.ietf.org/html/rfc7946#page-8 RFC 7946 3.1.7]]
 */
final case class MultiPolygon[A](coordinates: PolygonSet[A], bbox: Option[BoundingBox[A]] = None)
    extends GeoJsonGeometry[A](GeometryType.MultiPolygon) {
  type G = PolygonSet[A]
}

object MultiPolygon {
  implicit def encoderForMultiPolygon[N: Encoder]: Encoder[MultiPolygon[N]] =
    baseEncoder[N].apply(_)
  implicit def decoderForMultiPolygon[N: Decoder: Eq]: Decoder[MultiPolygon[N]] =
    baseDecoder[N].make[PolygonSet[N], MultiPolygon[N]](MultiPolygon.apply[N])
}

/*
See [[https://tools.ietf.org/html/rfc7946#page-8 RFC 7946 3.1.8]]

todo docs
 */
final case class GeometryCollection[A](geometries: List[GeoJsonGeometry[A]], bbox: Option[BoundingBox[A]] = None)
    extends GeoJson[A, Unit](GeoJsonObjectType.GeometryCollection)

object GeometryCollection {
  implicit def encoderForGeometryCollection[N: Encoder]: Encoder[GeometryCollection[N]] = { coll =>
    Json.obj(
      ("geometries", coll.geometries.asJson),
      ("bbox", coll.bbox.asJson),
      ("type", coll.`type`.asJson)
    )
  }

  implicit def decoderForGeometryCollections[N: Eq: Decoder]: Decoder[GeometryCollection[N]] =
    Decoder
      .instance { cursor =>
        for {
          geometries <- cursor.downField("geometries").as[List[GeoJsonGeometry[N]]]
          bbox <- cursor.downField("bbox").as[Option[BoundingBox[N]]]
        } yield GeometryCollection[N](geometries, bbox)
      }
}

/*
See [[https://tools.ietf.org/html/rfc7946#page-11 RFC 7946 3.2]]

todo docs
todo per the RFC, the id can be either string or number
 */
final case class Feature[A, P](
  geometry: GeoJsonGeometry[A],
  properties: P,
  id: Option[String] = None,
  bbox: Option[BoundingBox[A]] = None
) extends GeoJson[A, P](GeoJsonObjectType.Feature)

object Feature {
  implicit def encoderForFeature[N: Encoder, P: Encoder]: Encoder[Feature[N, P]] =
    f =>
      Json.obj(
        ("id", f.id.asJson),
        ("geometry", f.geometry.asJson),
        ("type", f.`type`.asJson),
        ("properties", f.properties.asJson),
        ("bbox", f.bbox.asJson)
      )

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
          bbox <- cursor.downField("bbox").as[Option[BoundingBox[N]]]
        } yield Feature[N, P](geometry, properties, id, bbox)
      }
}

/*
See [[https://tools.ietf.org/html/rfc7946#page-12 RFC 7946 3.3]]

todo abstract over the collection type; I had issues deriving circe codecs
 */
final case class FeatureCollection[A, P](features: Seq[Feature[A, P]], bbox: Option[BoundingBox[A]] = None)
    extends GeoJson[A, P](GeoJsonObjectType.FeatureCollection) {}

object FeatureCollection {
  implicit def encoderForFeatureCollection[N: Encoder, P: Encoder]: Encoder[FeatureCollection[N, P]] =
    fc =>
      Json.obj(
        ("type", fc.`type`.asJson),
        ("bbox", fc.bbox.asJson),
        ("features", Json.fromValues(fc.features.map(_.asJson)))
      )

  implicit def decoderForFeatureCollection[N: Eq: Decoder, P: Decoder]: Decoder[FeatureCollection[N, P]] =
    Decoder
      .instance { cursor =>
        for {
          features <- cursor
            .downField("features")
            .as[Seq[Feature[N, P]]]
          bbox <- cursor.downField("bbox").as[Option[BoundingBox[N]]]
        } yield FeatureCollection[N, P](features, bbox)
      }
}
