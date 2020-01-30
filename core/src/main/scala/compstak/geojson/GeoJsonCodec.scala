package compstak.geojson

import cats._
import cats.data._
import cats.implicits._
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

object GeoJsonCodec {
  // todo add back properties
  private[geojson] def baseEncoder[N: Encoder](t: GeometryType) = new BaseEncoderPartiallyApplied[N](t)

  private[geojson] class BaseEncoderPartiallyApplied[N: Encoder](t: GeometryType) {
    def apply[G <: GeoJsonGeometry[N]](geometry: G)(implicit E: Encoder[geometry.G]): Json =
      Json.obj(("coordinates", geometry.coordinates.asJson), ("bbox", geometry.bbox.asJson), ("type", t.asJson))
  }

  private[geojson] def baseDecoder[N: Decoder] = new BaseDecoderPartiallyApplied[N]

  private[geojson] class BaseDecoderPartiallyApplied[N: Decoder]() {
    def apply[G <: GeoJsonGeometry[N]](
      f: (G#G, Option[BoundingBox[N]]) => Decoder.Result[G]
    )(implicit D: Decoder[G#G]): Decoder[G] =
      cursor =>
        for {
          coordinates <- cursor.downField("coordinates").as[G#G]
          bbox <- cursor.downField("bbox").as[Option[BoundingBox[N]]]
          result <- f(coordinates, bbox)
        } yield result

    def make[G <: GeoJsonGeometry[N]](f: (G#G, Option[BoundingBox[N]]) => G)(implicit D: Decoder[G#G]): Decoder[G] =
      apply[G]((g, bbox) => f(g, bbox).asRight)
  }

}
