package compstak.geojson

import cats.kernel.Eq
import cats.implicits._
import io.circe.syntax._
import compstak.geojson.arbitrary._
import compstak.geojson.implicits.simple._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.prop.Configuration
import org.scalatestplus.scalacheck.Checkers
import org.scalactic.anyvals.PosZInt

class GeoJsonCodecSuite extends AnyFlatSpec with Checkers with Configuration {

  implicit override val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(sizeRange = PosZInt(20))

  it should "make a codec roundtrip" in {
    check((g: GeoJsonGeometry[Int]) => Eq.eqv(g.asJson.as[GeoJsonGeometry[Int]], Right(g)))
  }

  it should "make a GeoJson codec roundtrip" in {
    check((g: GeoJson[Int, Unit]) => g.asJson.as[GeoJson[Int, Unit]] == Right(g))
  }

  it should "make a GeoJson Feature codec roundtrip" in {
    check((g: Feature[Int, String]) => g.asJson.as[Feature[Int, String]] == Right(g))
  }
}
