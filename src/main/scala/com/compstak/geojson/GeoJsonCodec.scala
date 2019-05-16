package com.compstak.geojson

import cats._
import cats.data._
import cats.implicits._
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

sealed class GeoJsonCodec[N: Eq: Encoder: Decoder] {

  implicit val encoderForPosition: Encoder[Position[N]] = Encoder.instance {
    case Pos2(x, y)    => List[N](x, y).asJson
    case Pos3(x, y, z) => List[N](x, y, z).asJson
  }

  implicit val decoderForPosition: Decoder[Position[N]] = Decoder.instance {
    cursor =>
      cursor.as[List[N]] match {
        case Right(List(x, y))    => Right(Pos2(x, y))
        case Right(List(x, y, z)) => Right(Pos3(x, y, z))
        case Right(_: List[N]) =>
          Left(DecodingFailure(
            "Coordinates must be two- or three-dimensional geographic positions",
            cursor.history))
        case _ =>
          Left(
            DecodingFailure(
              "Expected coordinate set of Array[Double] but not found",
              cursor.history))
      }
  }

  implicit val encoderForPositionSet: Encoder[PositionSet[N]] =
    Encoder.instance(_.elements.asJson)

  implicit val decoderForPositionSet: Decoder[PositionSet[N]] =
    Decoder.instance { cursor =>
      cursor.as[List[Position[N]]] match {
        case Right(elements) => Right(PositionSet(elements))
        case _ =>
          Left(
            DecodingFailure("Failed to decoded position set", cursor.history))
      }
    }

  implicit def encoderForLine: Encoder[Line[N]] =
    Encoder.instance(_.list.asJson)

  implicit def decoderForLine: Decoder[Line[N]] = Decoder.instance { cursor =>
    cursor.as[PositionSet[N]] match {
      case Right(c: PositionSet[N]) =>
        NonEmptyList
          .fromList(c.elements)
          .map(pos => Line(pos.head, pos.tail))
          .toRight(DecodingFailure("A line instance must be non-empty",
                                   cursor.history))
      case _ =>
        Left(
          DecodingFailure(
            "A line should be constructed as an array of positions",
            cursor.history))
    }
  }

  implicit val encoderForLineSet: Encoder[LineSet[N]] =
    Encoder.instance(_.elements.asJson)

  implicit val decoderForLineSet: Decoder[LineSet[N]] = Decoder.instance {
    cursor =>
      cursor.as[List[Line[N]]] match {
        case Right(elements) => Right(LineSet(elements))
        case _ =>
          Left(
            DecodingFailure("Failed to decoded position set", cursor.history))
      }
  }

  implicit def encoderForLinearRing: Encoder[LinearRing[N]] = Encoder.instance {
    case lrn: LRingN[N] => lrn.list.asJson
    case _: LRing0[N]   => Json.Null
  }

  implicit def decoderForLinearRing: Decoder[LinearRing[N]] = Decoder.instance {
    cursor =>
      val isEmptyOr4Plus: LineSet[N] => Boolean =
        c => c.elements.isEmpty || c.elements.map(_.list.size).toList.sum >= 4

      cursor.as[LineSet[N]] match {
        case Right(c) if isEmptyOr4Plus(c) =>
          LinearRing
            .of(c.elements)
            .leftMap(t => DecodingFailure.fromThrowable(t, cursor.history))
        case Right(c) =>
          Left(DecodingFailure(
            s"A linear ring must have 0 or 4+ elements, has ${c.elements.size}",
            cursor.history))
        case Left(_) =>
          Left(
            DecodingFailure(
              "A linear ring should be constructed as an array of lines",
              cursor.history))
      }
  }

  implicit val encoderForRingSet: Encoder[RingSet[N]] =
    Encoder.instance(_.elements.asJson)

  implicit val decoderForRingSet: Decoder[RingSet[N]] = Decoder.instance {
    cursor =>
      cursor.as[List[LinearRing[N]]] match {
        case Right(elements) => Right(RingSet(elements))
        case _ =>
          Left(
            DecodingFailure("Failed to decoded position set", cursor.history))
      }
  }

  // todo add back properties
  private[this] def baseEncoder[G <: GeoJsonGeometry[N]](geometry: G)(
      implicit E: Encoder[geometry.G]): Json =
    Json.obj(("coordinates", geometry.coordinates.asJson))

  implicit val encoderForPoint: Encoder[Point[N]] =
    Encoder.instance(baseEncoder(_))
  implicit val decoderForPoint: Decoder[Point[N]] = deriveDecoder[Point[N]]

  implicit val encoderForMultiPoint: Encoder[MultiPoint[N]] =
    Encoder.instance(baseEncoder(_))
  implicit val decoderForMultiPoint: Decoder[MultiPoint[N]] =
    deriveDecoder[MultiPoint[N]]

  implicit val encoderForLineString: Encoder[LineString[N]] =
    Encoder.instance(baseEncoder(_))
  implicit val decoderForLineString: Decoder[LineString[N]] =
    deriveDecoder[LineString[N]]

  implicit val encoderForMultiLineString: Encoder[MultiLineString[N]] =
    Encoder.instance(baseEncoder(_))
  implicit val decoderForMultiLineString: Decoder[MultiLineString[N]] =
    deriveDecoder[MultiLineString[N]]

  implicit val encoderForPolygon: Encoder[Polygon[N]] =
    Encoder.instance(baseEncoder(_))
  implicit val decoderForPolygon: Decoder[Polygon[N]] =
    deriveDecoder[Polygon[N]]

  implicit val encoderForMultiPolygon: Encoder[MultiPolygon[N]] =
    Encoder.instance(baseEncoder(_))
  implicit val decoderForMultiPolygon: Decoder[MultiPolygon[N]] =
    deriveDecoder[MultiPolygon[N]]

  private[this] def mkType(t: GeometryType): Json =
    Json.obj(("type", t.tag.asJson))

  implicit def encoderForGeometry: Encoder[GeoJsonGeometry[N]] =
    Encoder
      .instance {
        case p: Point[N] =>
          encoderForPoint(p) deepMerge mkType(GeometryType.Point)
        case m: MultiPoint[N] =>
          encoderForMultiPoint(m) deepMerge mkType(GeometryType.MultiPoint)
        case l: LineString[N] =>
          encoderForLineString(l) deepMerge mkType(GeometryType.LineString)
        case m: MultiLineString[N] =>
          encoderForMultiLineString(m) deepMerge mkType(
            GeometryType.MultiLineString)
        case p: Polygon[N] =>
          encoderForPolygon(p) deepMerge mkType(GeometryType.Polygon)
        case m: MultiPolygon[N] =>
          encoderForMultiPolygon(m) deepMerge mkType(GeometryType.MultiPolygon)
      }

  implicit def decoderForGeometry: Decoder[GeoJsonGeometry[N]] =
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

  implicit def encoderForGeometryCollection[F[_]](
      implicit E: Encoder[F[GeoJsonGeometry[N]]])
    : Encoder[GeometryCollection[F, N]] =
    Encoder
      .instance { coll =>
        Json.obj(("geometries", coll.geometries.asJson)) deepMerge mkType(
          GeometryType.Point)
      }

  implicit def decoderForGeometryCollections[F[_]: Traverse](
      implicit D: Decoder[F[GeoJsonGeometry[N]]])
    : Decoder[GeometryCollection[F, N]] =
    Decoder
      .instance { cursor =>
        for {
          geometries <- cursor.downField("geometries").as[F[GeoJsonGeometry[N]]]
          bbox <- decodeBoundingBox(cursor)
        } yield GeometryCollection[F, N](geometries, bbox)
      }

  implicit def encoderForFeature[P: Encoder]: Encoder[Feature[N, P]] =
    Encoder
      .instance { feature =>
        Json.obj(
          ("id", feature.id.asJson),
          ("geometry", feature.geometry.asJson),
          ("type", "Feature".asJson),
          ("properties", feature.properties.asJson)
        )
      }

  implicit def decoderForFeature[P: Decoder]: Decoder[Feature[N, P]] =
    Decoder
      .instance { cursor =>
        for {
          _ <- cursor
            .downField("type")
            .as[String]
            .fold(_.asLeft,
                  t =>
                    Either.cond(t === "Feature",
                                t,
                                DecodingFailure("The element is not a feature",
                                                cursor.history)))
          geometry <- cursor.downField("geometry").as[GeoJsonGeometry[N]]
          properties <- cursor.downField("properties").as[P]
          id <- cursor.downField("id").as[Option[String]]
          bbox <- decodeBoundingBox(cursor)
        } yield Feature[N, P](geometry, properties, id, bbox)
      }

  implicit def encoderForFeatureCollection[P: Encoder]
    : Encoder[FeatureCollection[N, P]] =
    Encoder
      .instance { coll: FeatureCollection[N, P] =>
        Json.obj(
          ("type", "FeatureCollection".asJson),
          ("features", coll.features.asJson)
        )
      }

  implicit def decoderForFeatureCollection[P: Decoder]
    : Decoder[FeatureCollection[N, P]] =
    Decoder
      .instance { cursor =>
        for {
          features <- cursor
            .downField("features")
            .as[Seq[Feature[N, P]]]
          bbox <- decodeBoundingBox(cursor)
        } yield FeatureCollection[N, P](features, bbox)
      }

  private[this] def decodeBoundingBox(
      cursor: ACursor): Decoder.Result[Option[List[Position[N]]]] = {

    val bboxCursor = cursor.downField("bbox")

    val properBox = bboxCursor.as[Option[List[Position[N]]]]

    val flattenedBox = bboxCursor
      .as[Option[List[N]]]
      .flatMap { maybeCoordinates =>
        Right(maybeCoordinates.flatMap { coordinates =>
          if (coordinates.size === 4)
            coordinates
              .grouped(2)
              .map[Position[N]] { pos2 =>
                Pos2[N](pos2.head, pos2.tail.head) // todo improve this
              }
              .toList
              .some
          else None
        })
      }

    properBox orElse flattenedBox
  }
}

object GeoJsonCodec {
  implicit val geoJsonCodecForDouble: GeoJsonCodec[Double] =
    new GeoJsonCodec[Double]
  implicit val geoJsonCodecForFloat: GeoJsonCodec[Float] =
    new GeoJsonCodec[Float]
}
