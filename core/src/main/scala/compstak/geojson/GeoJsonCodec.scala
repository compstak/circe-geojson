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
          bbox <- decodeBoundingBox[N](cursor)
          result <- f(coordinates, bbox)
        } yield result

    def make[G <: GeoJsonGeometry[N]](f: (G#G, Option[BoundingBox[N]]) => G)(implicit D: Decoder[G#G]): Decoder[G] =
      apply[G]((g, bbox) => f(g, bbox).asRight)
  }

  private[geojson] def mkType(t: GeometryType): Json =
    Json.obj(("type", t.tag.asJson))

  def decodeBoundingBox[N: Decoder](
    cursor: ACursor
  ): Decoder.Result[Option[BoundingBox[N]]] = {

    val bboxCursor = cursor.downField("bbox")

    val properBox = bboxCursor.as[Option[(Position[N], Position[N])]]

    val flattenedBox = bboxCursor
      .as[Option[List[N]]]
      .flatMap { maybeCoordinates =>
        Right(maybeCoordinates.flatMap {
          case List(a, b, c, d)          => Some((Pos2[N](a, b), Pos2[N](c, d)))
          case List(a, b, c, d, e, f, g) => Some((Pos3[N](a, b, c), Pos3[N](e, f, g)))
          case _                         => None
        })
      }

    properBox.orElse(flattenedBox).map(_.map((BoundingBox.apply[N] _).tupled))
  }
}
