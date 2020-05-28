package compstak.geojson

import org.scalatest.FlatSpec
import cats.kernel.Eq
import cats.implicits._
import org.scalacheck.{Arbitrary, Gen}
import cats.data.NonEmptyList
import compstak.geojson.arbitrary._
import compstak.geojson.implicits.simple._
import compstak.geojson.postgis._
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class PostGisGeoJsonCodecSuite extends FlatSpec with ScalaCheckPropertyChecks {
  it should "make a codec round trip" in {
    // PostGis doesn't have the notion of a bounding box associated with a geometry
    forAll(genGeoJsonGeometryNoBbox) { (g: GeoJsonGeometry[Int]) =>
      Eq.eqv(g.asPostGIS(_.toDouble).asGeoJson(_.toInt), g)
    }
  }

  it should "make a codec round trip for wkb" in {
    forAll(genGeoJsonGeometryNoBbox) { (g: GeoJsonGeometry[Int]) =>
      gis.decodeWkb
        .decodeJson(gis.encodeWkb(g.asPostGIS(_.toDouble)))
        .map(_.asGeoJson(_.toInt))
        .exists(Eq.eqv(_, g))
    }
  }
}
