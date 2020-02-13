package compstak.geojson

import cats.implicits._
import compstak.geojson.arbitrary._
import compstak.geojson.implicits.simple._
import cats.kernel.laws.discipline.EqTests
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.prop.Configuration
import org.typelevel.discipline.scalatest.FunSpecDiscipline

class GeoJsonLawSuite extends AnyFunSpec with FunSpecDiscipline with Configuration {
  checkAll("GeoJson.EqLaws", EqTests[GeoJsonGeometry[Int]].eqv)
}
