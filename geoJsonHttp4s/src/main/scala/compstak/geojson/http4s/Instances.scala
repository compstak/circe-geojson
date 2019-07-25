package compstak.geojson
package http4s

import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe._
import cats.effect.Sync
import cats.kernel.Eq
import io.circe.Encoder
import io.circe.Decoder

object instances {
  implicit def entityEncoderForGeoJsonGeometry[F[_]: Sync, N: Encoder]: EntityEncoder[F, GeoJsonGeometry[N]] =
    jsonEncoderOf[F, GeoJsonGeometry[N]]

  implicit def entityDecoderForGeoJsonGeometry[F[_]: Sync, N: Eq: Decoder]: EntityDecoder[F, GeoJsonGeometry[N]] =
    jsonOf[F, GeoJsonGeometry[N]]
}
