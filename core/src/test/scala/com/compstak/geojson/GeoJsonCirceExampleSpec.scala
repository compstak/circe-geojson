package compstak.geojson

import cats._
import cats.data._
import cats.effect._
import cats.implicits._
import compstak.geojson.GeoJsonCodec.geoJsonCodecForDouble._
import io.circe._
import io.circe.literal._
import org.scalatest._

import scala.concurrent.ExecutionContext

class GeoJsonCirceExampleSuite extends FlatSpec with Matchers {

  implicit val CS: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

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

    point shouldBe a[Point[_]]
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

    multipoint shouldBe a[MultiPoint[_]]
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

    val coordinates: Line[Double] = NonEmptyList
      .fromList[Position[Double]](Pos2(0.0, 1.0) :: Pos2(1.0, 2.0) :: Nil)
      .map(pos => Line(pos.head, pos.tail))
      .get

    lineString shouldBe a[LineString[_]]
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
      Line[List, Double](Pos2(0.0, 0.0) :: Pos2(0.0, 1.0) :: Nil) ::
        Line[List, Double](Pos2(0.0, 1.0) :: Pos2(1.0, 2.0) :: Nil) ::
        Nil
    )

    multiLineString shouldBe a[MultiLineString[_]]
    Eq[LineSet[Double]].eqv(multiLineString.coordinates, coordinates) should equal(true)
  }

  it should "process a valid Polygon instance" in {

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

    val coordinates: LinearRing[Double] = LinearRing(
      Line[List, Double](Pos2(0.0, 0.0) :: Pos2(0.0, 2.0) :: Nil) ::
        Line[List, Double](Pos2(0.0, 2.0) :: Pos2(2.0, 2.0) :: Nil) ::
        Line[List, Double](Pos2(2.0, 2.0) :: Pos2(2.0, 0.0) :: Nil) ::
        Line[List, Double](Pos2(2.0, 0.0) :: Pos2(0.0, 0.0) :: Nil) ::
        Nil
    )

    polygon shouldBe a[Polygon[_]]
    Eq[LinearRing[Double]].eqv(polygon.coordinates, coordinates) should equal(true)
  }

  it should "process a valid MultiPolygon instance" in {

    val polygon = build[MultiPolygon[Double]] {
      json"""
      {
        "type": "Polygon",
        "coordinates": [
          [
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
        ]
      }
      """
    }

    val coordinates: List[LinearRing[Double]] =
      List(
        LinearRing(
          Line[List, Double](Pos2(0.0, 0.0) :: Pos2(0.0, 2.0) :: Nil) ::
            Line[List, Double](Pos2(0.0, 2.0) :: Pos2(2.0, 2.0) :: Nil) ::
            Line[List, Double](Pos2(2.0, 2.0) :: Pos2(2.0, 0.0) :: Nil) ::
            Line[List, Double](Pos2(2.0, 0.0) :: Pos2(0.0, 0.0) :: Nil) ::
            Nil
        )
      )

    polygon shouldBe a[MultiPolygon[_]]
    Eq[List[LinearRing[Double]]].eqv(polygon.coordinates.elements, coordinates) should equal(true)
  }

  private[this] def build[A: Decoder](json: Json): A = json.as[A].toOption.get
}
