package compstak.geojson

import compstak.geojson.arbitrary._
import compstak.geojson.implicits.simple._
import cats.tests.CatsSuite
import cats.kernel.laws.discipline.EqTests

class GeoJsonLawSuite extends CatsSuite {
  checkAll("GeoJson.EqLaws", EqTests[GeoJsonGeometry[Int]].eqv)
}
