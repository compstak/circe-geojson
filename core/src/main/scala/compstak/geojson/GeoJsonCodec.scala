package compstak.geojson

import cats.implicits.*
import io.circe.*
import io.circe.syntax.*

object GeoJsonCodec {
  // todo add back properties
  private[geojson] def baseEncoder[N: Encoder] = new BaseEncoderPartiallyApplied[N]

  private[geojson] class BaseEncoderPartiallyApplied[N: Encoder] {
    def apply[G <: GeoJsonGeometry[N]](geometry: G)(implicit E: Encoder[geometry.G]): Json =
      Json.obj(
        ("coordinates", geometry.coordinates.asJson),
        ("bbox", geometry.bbox.asJson),
        ("type", geometry.`type`.asJson)
      )
  }

  private[geojson] def baseDecoder[N: Decoder] =
    new BaseDecoderPartiallyApplied[N]

  private[geojson] class BaseDecoderPartiallyApplied[N: Decoder]() {

    // GG = coordinates type, G = geometry whose type member G == GG
    def apply[GG <: Geometry[N], G <: GeoJsonGeometry[N] { type G = GG }](
      f: (GG, Option[BoundingBox[N]]) => Decoder.Result[G]
    )(implicit D: Decoder[GG]): Decoder[G] =
      cursor =>
        for {
          coordinates <- cursor.downField("coordinates").as[GG]
          bbox <- cursor.downField("bbox").as[Option[BoundingBox[N]]]
          result <- f(coordinates, bbox)
        } yield result

    def make[GG <: Geometry[N], G <: GeoJsonGeometry[N] { type G = GG }](
      f: (GG, Option[BoundingBox[N]]) => G
    )(implicit D: Decoder[GG]): Decoder[G] =
      apply[GG, G]((g, bbox) => f(g, bbox).asRight)
  }

}
