package compstak.geojson

import org.scalatest.prop.Checkers
import org.scalatest.FlatSpec
import io.circe.{Decoder, Encoder}
import io.circe.syntax._
import cats.kernel.Eq
import cats.implicits._
import org.scalacheck.{Arbitrary, Gen}
import cats.data.NonEmptyList

class GeoJsonCodecSuite extends FlatSpec with Checkers {

  val minLattitude: Int = -90
  val maxLattitude: Int = 90
  val minLongitude: Int = -180
  val maxLongitude: Int = 180

  def genPos2: Gen[Pos2[Int]] =
    for {
      lat <- Gen.choose(minLattitude, maxLattitude)
      lon <- Gen.choose(minLongitude, maxLongitude)
    } yield Pos2(lon, lat)

  def genLinearRing: Gen[LinearRing[Int]] =
    for {
      a <- genPos2
      b <- genPos2
      c <- genPos2
      d <- genPos2
      rest <- Gen.listOf[Position[Int]](genPos2)
    } yield LinearRing.unsafeCreate(NonEmptyList(a, b :: c :: d :: (rest :+ a)))

  def genPoint: Gen[Point[Int]] =
    genPos2.map(Point(_))

  def genMultiPoint: Gen[MultiPoint[Int]] =
    Gen.listOf(genPos2).map(ps => MultiPoint(PositionSet(ps)))

  def genLineString: Gen[LineString[Int]] =
    for {
      a <- genPos2
      b <- genPos2
      rest <- Gen.listOf(genPos2)
    } yield LineString(Line(a, NonEmptyList(b, rest)))

  def genMultiLineString: Gen[MultiLineString[Int]] =
    Gen.listOf(genLineString).map(ls => MultiLineString(LineSet(ls.map(_.coordinates))))

  def genPolygon: Gen[Polygon[Int]] = Gen.listOf(genLinearRing).map(rs => Polygon(RingSet(rs)))

  def genMultiPolygon: Gen[MultiPolygon[Int]] =
    Gen.listOf(genPolygon).map(ps => MultiPolygon(PolygonSet(ps.map(_.coordinates))))

  def widen[B <: GeoJsonGeometry[Int]](g: Gen[B]): Gen[GeoJsonGeometry[Int]] = g.map(identity)

  implicit val arbitraryGeoJson: Arbitrary[GeoJsonGeometry[Int]] = Arbitrary(
    Gen.oneOf(widen(genPoint),
              widen(genMultiPoint),
              widen(genLineString),
              widen(genMultiLineString),
              widen(genPolygon),
              widen(genMultiPolygon))
  )

  it should "make a codec roundtrip" in {
    check((g: GeoJsonGeometry[Int]) => g.asJson.as[GeoJsonGeometry[Int]] == Right(g))
  }
}
