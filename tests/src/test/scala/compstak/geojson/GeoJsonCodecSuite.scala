package compstak.geojson

import cats.kernel.Eq
import cats.implicits._
import io.circe.{Decoder, Encoder}
import io.circe.syntax._
import cats.data.NonEmptyList
import compstak.geojson.arbitrary._
import compstak.geojson.implicits.simple._
import org.scalacheck.{Arbitrary, Gen}
import org.scalatest.FlatSpec
import org.scalatestplus.scalacheck.Checkers

class GeoJsonCodecSuite extends FlatSpec with Checkers {

  it should "make a codec roundtrip" in {
    check((g: GeoJsonGeometry[Int]) => Eq.eqv(g.asJson.as[GeoJsonGeometry[Int]], Right(g)))
  }

  it should "make a GeoJson codec roundtrip" in {
    check((g: GeoJson[Int]) => g.asJson.as[GeoJson[Int]] == Right(g))
  }
}
