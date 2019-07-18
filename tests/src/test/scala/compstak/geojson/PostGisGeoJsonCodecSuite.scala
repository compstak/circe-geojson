package compstak.geojson

import org.scalatest.prop.GeneratorDrivenPropertyChecks
import org.scalatest.FlatSpec
import cats.kernel.Eq
import cats.implicits._
import org.scalacheck.{Arbitrary, Gen}
import cats.data.NonEmptyList
import compstak.geojson.arbitrary._
import compstak.geojson.implicits.simple._
import compstak.geojson.postgis._

class PostGisGeoJsonCodecSuite extends FlatSpec with GeneratorDrivenPropertyChecks {
  it should "make a codec roundtrip" in {
    // PostGis doesn't have the notion of a bounding box associated with a geometry
    forAll(genGeoJsonGeometryNoBbox) { (g: GeoJsonGeometry[Int]) =>
      Eq.eqv(g.asPostGIS(_.toDouble).asGeoJson(_.toInt), g)
    }
  }
}
