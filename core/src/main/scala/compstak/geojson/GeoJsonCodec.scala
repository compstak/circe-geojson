package compstak.geojson

import cats._
import cats.data._
import cats.implicits._
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

object GeoJsonCodec {

  // todo add back properties
  private[geojson] def baseEncoder[N: Encoder] = new BaseEncoderPartiallyApplied[N]

  private[geojson] class BaseEncoderPartiallyApplied[N: Encoder]() {
    def apply[G <: GeoJsonGeometry[N]](geometry: G)(implicit E: Encoder[geometry.G]): Json =
      Json.obj(("coordinates", geometry.coordinates.asJson))
  }

  private[geojson] def mkType(t: GeometryType): Json =
    Json.obj(("type", t.tag.asJson))

  def decodeBoundingBox[N: Decoder](
    cursor: ACursor
  ): Decoder.Result[Option[(Position[N], Position[N])]] = {

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

    properBox.orElse(flattenedBox)
  }
}
