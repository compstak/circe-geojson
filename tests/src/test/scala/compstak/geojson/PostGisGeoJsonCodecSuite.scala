package compstak.geojson

import org.scalatest.prop.Checkers
import org.scalatest.FlatSpec
import cats.kernel.Eq
import cats.implicits._
import org.scalacheck.{Arbitrary, Gen}
import cats.data.NonEmptyList
import compstak.geojson.arbitrary._
import compstak.geojson.implicits.simple._
import compstak.geojson.postgis._

class PostGisGeoJsonCodecSuite extends FlatSpec with Checkers {
  it should "make a codec roundtrip" in {
    check((g: GeoJsonGeometry[Int]) => Eq.eqv(g.asPostGIS(_.toDouble).asGeoJson(_.toInt), g))
  }
}
