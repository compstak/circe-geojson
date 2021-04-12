package compstak.geojson
package http4s

import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe._
import cats.effect.Sync
import cats.{Applicative, Eq}
import io.circe.{Decoder, Encoder}
import io.circe.Json

object instances {
  implicit def entityEncoderForGeoJson[F[_]: Applicative, N: Encoder, P: Encoder]: EntityEncoder[F, GeoJson[N, P]] =
    jsonEncoderOf[F, GeoJson[N, P]]

  implicit def entityDecoderForGeoJson[F[_]: Sync, N: Eq: Decoder, P: Decoder]: EntityDecoder[F, GeoJson[N, P]] =
    jsonOf[F, GeoJson[N, P]]

  implicit def entityEncoderForGeoJsonGeometry[F[_]: Applicative, N: Encoder]: EntityEncoder[F, GeoJsonGeometry[N]] =
    jsonEncoderOf[F, GeoJsonGeometry[N]]

  implicit def entityDecoderForGeoJsonGeometry[F[_]: Sync, N: Eq: Decoder]: EntityDecoder[F, GeoJsonGeometry[N]] =
    jsonOf[F, GeoJsonGeometry[N]]

  implicit def entityEncoderForPoint[F[_]: Applicative, N: Encoder]: EntityEncoder[F, Point[N]] =
    jsonEncoderOf[F, Point[N]]

  implicit def entityDecoderForPoint[F[_]: Sync, N: Decoder]: EntityDecoder[F, Point[N]] =
    jsonOf[F, Point[N]]

  implicit def entityEncoderForMultiPoint[F[_]: Applicative, N: Encoder]: EntityEncoder[F, MultiPoint[N]] =
    jsonEncoderOf[F, MultiPoint[N]]

  implicit def entityDecoderForMultiPoint[F[_]: Sync, N: Decoder]: EntityDecoder[F, MultiPoint[N]] =
    jsonOf[F, MultiPoint[N]]

  implicit def entityEncoderForLineString[F[_]: Applicative, N: Encoder]: EntityEncoder[F, LineString[N]] =
    jsonEncoderOf[F, LineString[N]]

  implicit def entityDecoderForLineString[F[_]: Sync, N: Decoder]: EntityDecoder[F, LineString[N]] =
    jsonOf[F, LineString[N]]

  implicit def entityEncoderForMultiLineString[F[_]: Applicative, N: Encoder]: EntityEncoder[F, MultiLineString[N]] =
    jsonEncoderOf[F, MultiLineString[N]]

  implicit def entityDecoderForMultiLineString[F[_]: Sync, N: Decoder]: EntityDecoder[F, MultiLineString[N]] =
    jsonOf[F, MultiLineString[N]]

  implicit def entityEncoderForPolygon[F[_]: Applicative, N: Encoder]: EntityEncoder[F, Polygon[N]] =
    jsonEncoderOf[F, Polygon[N]]

  implicit def entityDecoderForPolygon[F[_]: Sync, N: Decoder: Eq]: EntityDecoder[F, Polygon[N]] =
    jsonOf[F, Polygon[N]]

  implicit def entityEncoderForMultiPolygon[F[_]: Applicative, N: Encoder]: EntityEncoder[F, MultiPolygon[N]] =
    jsonEncoderOf[F, MultiPolygon[N]]

  implicit def entityDecoderForMultiPolygon[F[_]: Sync, N: Decoder: Eq]: EntityDecoder[F, MultiPolygon[N]] =
    jsonOf[F, MultiPolygon[N]]

  implicit def entityEncoderForGeometryCollection[F[_]: Applicative, N: Encoder]
    : EntityEncoder[F, GeometryCollection[N]] =
    jsonEncoderOf[F, GeometryCollection[N]]

  implicit def entityDecoderForGeometryCollection[F[_]: Sync, N: Decoder: Eq]: EntityDecoder[F, GeometryCollection[N]] =
    jsonOf[F, GeometryCollection[N]]

  implicit def entityEncoderForFeature[F[_]: Applicative, N: Encoder, P: Encoder]: EntityEncoder[F, Feature[N, P]] =
    jsonEncoderOf[F, Feature[N, P]]

  implicit def entityDecoderForFeature[F[_]: Sync, N: Decoder: Eq, P: Decoder: Encoder]
    : EntityDecoder[F, Feature[N, P]] =
    jsonOf[F, Feature[N, P]]

  implicit def entityEncoderForFeatureCollection[F[_]: Applicative, N: Encoder, P: Encoder]
    : EntityEncoder[F, FeatureCollection[N, P]] =
    jsonEncoderOf[F, FeatureCollection[N, P]]

  implicit def entityDecoderForFeatureCollection[F[_]: Sync, N: Decoder: Eq, P: Decoder: Encoder]
    : EntityDecoder[F, FeatureCollection[N, P]] =
    jsonOf[F, FeatureCollection[N, P]]
}
