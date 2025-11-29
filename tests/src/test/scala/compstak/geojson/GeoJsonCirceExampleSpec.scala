package compstak.geojson

import cats.*
import cats.data.*
import cats.implicits.*
import io.circe.*
import io.circe.literal.*
import org.scalatest.flatspec.*
import org.scalatest.matchers.should.*

class GeoJsonCirceExampleSuite extends AnyFlatSpec with Matchers {

  // todo fix this when we reenable properties
  implicit val nothingDecoder: Decoder[Any] = Decoder.const(null)
  type FreeMap = Option[Map[String, Any]]

  it should "process a valid Point instance" in {
    val point = build[Point[Double]] {
      json"""
      {
        "type": "Point",
        "coordinates": [ 0, 0 ]
      }
      """
    }

    val coordinates = Pos2(0.0, 0.0)

    point shouldBe a[Point[?]]
    Eq[Position[Double]].eqv(point.coordinates, coordinates) should equal(true)
  }

  it should "process a valid MultiPoint instance" in {
    val multipoint = build[MultiPoint[Double]] {
      json"""
      {
        "type": "MultiPoint",
        "coordinates": [
          [ 0, 0 ],
          [ 0, 1 ]
        ]
      }
      """
    }

    val coordinates = PositionSet(Pos2(0.0, 0.0) :: Pos2(0.0, 1.0) :: Nil)

    multipoint shouldBe a[MultiPoint[?]]
    Eq[PositionSet[Double]].eqv(multipoint.coordinates, coordinates) should equal(true)
  }

  it should "process a valid LineString instance" in {
    val lineString = build[LineString[Double]] {
      json"""
      {
        "type": "LineString",
        "coordinates": [
          [ 0, 1 ],
          [ 1, 2 ]
        ]
      }
      """
    }

    val coordinates: Line[Double] =
      Line(Pos2(0.0, 1.0), NonEmptyList.one(Pos2(1.0, 2.0)))

    lineString shouldBe a[LineString[?]]
    Eq[Line[Double]].eqv(lineString.coordinates, coordinates) should equal(true)
  }

  it should "process a valid MultiLineString instance" in {
    val multiLineString = build[MultiLineString[Double]] {
      json"""
      {
        "type": "MultiLineString",
        "coordinates": [
          [
            [ 0, 0 ],
            [ 0, 1 ]
          ],
          [
            [ 0, 1 ],
            [ 1, 2 ]
          ]
        ]
      }
      """
    }

    val coordinates: LineSet[Double] = LineSet(
      Line.unsafeFromFoldable[List, Double](Pos2(0.0, 0.0) :: Pos2(0.0, 1.0) :: Nil) ::
        Line.unsafeFromFoldable[List, Double](Pos2(0.0, 1.0) :: Pos2(1.0, 2.0) :: Nil) ::
        Nil
    )

    multiLineString shouldBe a[MultiLineString[?]]
    Eq[LineSet[Double]].eqv(multiLineString.coordinates, coordinates) should equal(true)
  }

  it should "process a 2D bounding box" in {
    val bbox = build[BoundingBox[Double]] {
      json"""
        [100.0, 0.0, 105.0, 1.0]
      """
    }

    bbox shouldBe a[BoundingBox[?]]
  }

  it should "process a 3D bounding box" in {
    val bbox = build[BoundingBox[Double]] {
      json"""
        [100.0, 10.0, 0.0, 105.0, 1.0, 0.0]
      """
    }

    bbox shouldBe a[BoundingBox[?]]
  }

  it should "process a geometry with 2D bounding box" in {
    val multiLineString = build[GeoJsonGeometry[Double]] {
      json"""
      {
        "type": "MultiLineString",
        "bbox": [100.0, 0.0, 105.0, 1.0],
        "coordinates": [
          [
            [ 0, 0 ],
            [ 0, 1 ]
          ],
          [
            [ 0, 1 ],
            [ 1, 2 ]
          ]
        ]
      }
      """
    }

    multiLineString shouldBe a[MultiLineString[?]]
    multiLineString.bbox shouldBe a[Some[?]]
  }

  it should "process a geometry with 3D bounding box" in {
    val point = build[GeoJsonGeometry[Double]] {
      json"""
      {
        "type": "Point",
        "bbox": [100.0, 10.0, 0.0, 105.0, 1.0, 0.0],
        "coordinates": [ 0, 0, 0 ]
      }
      """
    }

    point shouldBe a[Point[?]]
    point.bbox shouldBe a[Some[?]]
  }

  it should "process a valid Polygon instance" in {
    val polygon = build[Polygon[Double]] {
      json"""
      {
        "type": "Polygon",
        "coordinates": [
          [
            [100.0, 0.0],
            [101.0, 0.0],
            [101.0, 1.0],
            [100.0, 1.0],
            [100.0, 0.0]
          ]
        ]
      }
      """
    }

    val coordinates: RingSet[Double] = RingSet(
      LinearRing(
        NonEmptyList.of[Position[Double]](
          Pos2(100.0, 0.0),
          Pos2(101.0, 0.0),
          Pos2(101.0, 1.0),
          Pos2(100.0, 1.0),
          Pos2(100.0, 0.0)
        )
      ).toList
    )

    polygon shouldBe a[Polygon[?]]
    Eq[RingSet[Double]].eqv(polygon.coordinates, coordinates) should equal(true)
  }

  it should "process a valid Polygon instance with holes " in {
    val polygon = build[Polygon[Double]] {
      json"""
      {
        "type": "Polygon",
        "coordinates": [
          [
            [100.0, 0.0],
            [101.0, 0.0],
            [101.0, 1.0],
            [100.0, 1.0],
            [100.0, 0.0]
          ],
          [
            [100.8, 0.8],
            [100.8, 0.2],
            [100.2, 0.2],
            [100.2, 0.8],
            [100.8, 0.8]
          ]
        ]
      }
      """
    }

    val coordinates: RingSet[Double] = RingSet(
      LinearRing(
        NonEmptyList.of[Position[Double]](
          Pos2(100.0, 0.0),
          Pos2(101.0, 0.0),
          Pos2(101.0, 1.0),
          Pos2(100.0, 1.0),
          Pos2(100.0, 0.0)
        )
      ).toList ::: LinearRing(
        NonEmptyList.of[Position[Double]](
          Pos2(100.8, 0.8),
          Pos2(100.8, 0.2),
          Pos2(100.2, 0.2),
          Pos2(100.2, 0.8),
          Pos2(100.8, 0.8)
        )
      ).toList
    )

    polygon shouldBe a[Polygon[?]]
    Eq[RingSet[Double]].eqv(polygon.coordinates, coordinates) should equal(true)
  }

  it should "process a valid MultiPolygon instance" in {
    val polygon = build[MultiPolygon[Double]] {
      json"""
      {
        "type": "MultiPolygon",
        "coordinates": [
          [
            [
              [102.0, 2.0],
              [103.0, 2.0],
              [103.0, 3.0],
              [102.0, 3.0],
              [102.0, 2.0]
            ]
          ],
          [
            [
              [100.0, 0.0],
              [101.0, 0.0],
              [101.0, 1.0],
              [100.0, 1.0],
              [100.0, 0.0]
            ]
          ]
        ]
      }
      """
    }

    val coordinates: PolygonSet[Double] =
      PolygonSet(
        List(
          RingSet(
            LinearRing(
              NonEmptyList.of[Position[Double]](
                Pos2(102.0, 2.0),
                Pos2(103.0, 2.0),
                Pos2(103.0, 3.0),
                Pos2(102.0, 3.0),
                Pos2(102.0, 2.0)
              )
            ).toList
          ),
          RingSet(
            LinearRing(
              NonEmptyList.of[Position[Double]](
                Pos2(100.0, 0.0),
                Pos2(101.0, 0.0),
                Pos2(101.0, 1.0),
                Pos2(100.0, 1.0),
                Pos2(100.0, 0.0)
              )
            ).toList
          )
        )
      )

    polygon shouldBe a[MultiPolygon[?]]
    Eq[PolygonSet[Double]].eqv(polygon.coordinates, coordinates) should equal(true)
  }

  it should "process a Polygon instance made up of lines" in {
    val polygon = build[Polygon[Double]] {
      json"""
      {
        "type": "Polygon",
        "coordinates": [
          [
            [ 0, 0 ],
            [ 0, 2 ]
          ],
          [
            [ 0, 2 ],
            [ 2, 2 ]
          ],
          [
            [ 2, 2 ],
            [ 2, 0 ]
          ],
          [
            [ 2, 0 ],
            [ 0, 0 ]
          ]
        ]
      }
      """
    }

    val coordinates: RingSet[Double] = RingSet(
      LinearRing(
        NonEmptyList.of[Position[Double]](
          Pos2(0.0, 0.0),
          Pos2(0.0, 2.0),
          Pos2(0.0, 2.0),
          Pos2(2.0, 2.0),
          Pos2(2.0, 2.0),
          Pos2(2.0, 0.0),
          Pos2(2.0, 0.0),
          Pos2(0.0, 0.0)
        )
      ).toList
    )

    polygon shouldBe a[Polygon[?]]
    Eq[RingSet[Double]].eqv(polygon.coordinates, coordinates) should equal(true)
  }

  it should "be able to have instances for different numeric types at the same time" in {
    Decoder[Point[Long]].decodeJson(Encoder[Point[Int]].apply(Point(Pos2(0, 0)))) shouldBe a[Right[?, ?]]
  }

  it should "allow to create a feature without a property encoder" in {
    case class Foo(n: Int)
    Feature(Point(Pos2(1.0, 2.0)), Foo(42))
  }

  private def build[A: Decoder](json: Json): A = json.as[A].toOption.get
}
