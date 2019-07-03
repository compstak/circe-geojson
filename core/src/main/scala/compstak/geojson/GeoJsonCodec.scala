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

  private[geojson] def decodeBoundingBox[N: Decoder](cursor: ACursor): Decoder.Result[Option[List[Position[N]]]] = {

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

    properBox.orElse(flattenedBox)
  }
}
