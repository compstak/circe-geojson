package compstak.geojson

import org.scalatest.prop.Checkers
import org.scalatest.FlatSpec
import io.circe.{Decoder, Encoder}
import io.circe.syntax._
import cats.kernel.Eq
import cats.implicits._
import org.scalacheck.{Arbitrary, Gen}
import cats.data.NonEmptyList
import compstak.geojson.arbitrary._
import compstak.geojson.implicits.simple._

class GeoJsonCodecSuite extends FlatSpec with Checkers {

  it should "make a codec roundtrip" in {
    check((g: GeoJsonGeometry[Int]) => Eq.eqv(g.asJson.as[GeoJsonGeometry[Int]], Right(g)))
  }

  it should "make a GeoJson codec roundtrip" in {
    check((g: GeoJson[Int]) => g.asJson.as[GeoJson[Int]] == Right(g))
  }
}
