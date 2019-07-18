package compstak.geojson

import cats.data.NonEmptyList
import cats.implicits._
import org.scalacheck.{Arbitrary, Gen}
import org.scalacheck.Cogen

object arbitrary {

  val minLattitude: Int = -90
  val maxLattitude: Int = 90
  val minLongitude: Int = -180
  val maxLongitude: Int = 180

  def genPos2: Gen[Pos2[Int]] =
    for {
      lat <- Gen.choose(minLattitude, maxLattitude)
      lon <- Gen.choose(minLongitude, maxLongitude)
    } yield Pos2(lon, lat)

  def genBoundingBox: Gen[BoundingBox[Int]] =
    for {
      llb <- genPos2
      urt <- genPos2
    } yield BoundingBox(llb, urt)

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
    Gen
      .listOf(genPolygon.filter(_.coordinates.elements.nonEmpty))
      .map(ps => MultiPolygon(PolygonSet(ps.map(_.coordinates))))

  def widen[B <: GeoJsonGeometry[Int]](g: Gen[B]): Gen[GeoJsonGeometry[Int]] = g.map(identity)

  implicit val cogenGeoJson: Cogen[GeoJsonGeometry[Int]] = Cogen { geojson =>
    geojson.coordinates.hashCode().toLong
  }

  def genGeoJsonGeometryNoBbox: Gen[GeoJsonGeometry[Int]] = Gen.oneOf(
    widen(genPoint),
    widen(genMultiPoint),
    widen(genLineString),
    widen(genMultiLineString),
    widen(genPolygon),
    widen(genMultiPolygon)
  )

  def genGeoJsonGeometry: Gen[GeoJsonGeometry[Int]] =
    Gen
      .oneOf(genBoundingBox.map(Option(_)), Gen.const(None))
      .flatMap(
        optionBbox =>
          Gen.oneOf(
            widen(genPoint.map(_.copy(bbox = optionBbox))),
            widen(genMultiPoint.map(_.copy(bbox = optionBbox))),
            widen(genLineString.map(_.copy(bbox = optionBbox))),
            widen(genMultiLineString.map(_.copy(bbox = optionBbox))),
            widen(genPolygon.map(_.copy(bbox = optionBbox))),
            widen(genMultiPolygon.map(_.copy(bbox = optionBbox)))
        )
      )

  implicit val arbitraryGeoJson: Arbitrary[GeoJsonGeometry[Int]] =
    Arbitrary(genGeoJsonGeometry)

}
