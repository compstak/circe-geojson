package compstak.geojson

import java.nio.file._

import cats._
import cats.effect._
import cats.instances.double._
import fs2.io.file.readAll
import fs2.text._
import io.circe._
import org.scalatest._

import scala.concurrent.ExecutionContext

class GeoJsonCirceExampleSlowSuite extends FlatSpec with Matchers {
  implicit val CS: ContextShift[IO] = IO.contextShift(ExecutionContext.global)

  type FreeMap = Option[Json]

  buildFileAssertion[FeatureCollection[Double, FreeMap]](
    statement = "process homogeneous FeatureCollections",
    file = "HomogeneousPoint"
  ) { result =>
    assert(result.features.forall(_.geometry.isInstanceOf[Point[Double]]), "The feature type is Point")
    assert(result.features.nonEmpty, "The feature set will be non-empty")
  }

  buildFileAssertion[FeatureCollection[Double, FreeMap]](
    statement = "process homogeneous FeatureCollections",
    file = "HomogeneousMultiPoint"
  ) { result =>
    assert(result.features.forall(_.geometry.isInstanceOf[MultiPoint[Double]]), "The feature type is MultiPoint")
    assert(result.features.nonEmpty, "The feature set will be non-empty")
  }

  buildFileAssertion[FeatureCollection[Double, FreeMap]](
    statement = "process homogeneous FeatureCollections",
    file = "HomogeneousLineString"
  ) { result =>
    assert(result.features.forall(_.geometry.isInstanceOf[LineString[Double]]), "The feature type is LineString")
    assert(result.features.nonEmpty, "The feature set will be non-empty")
  }

  buildFileAssertion[FeatureCollection[Double, FreeMap]](
    statement = "process homogeneous FeatureCollections",
    file = "HomogeneousMultiLineString"
  ) { result =>
    assert(
      result.features.forall(_.geometry.isInstanceOf[MultiLineString[Double]]),
      "The feature type is MultiLineString"
    )
    assert(result.features.nonEmpty, "The feature set will be non-empty")
  }

  buildFileAssertion[FeatureCollection[Double, FreeMap]](
    statement = "process homogeneous FeatureCollections",
    file = "HomogeneousPolygon"
  ) { result =>
    assert(result.features.forall(_.geometry.isInstanceOf[Polygon[Double]]), "The feature type is Polygon")
    assert(result.features.nonEmpty, "The feature set will be non-empty")
  }

  buildFileAssertion[FeatureCollection[Double, FreeMap]](
    statement = "process homogeneous FeatureCollections",
    file = "HomogeneousMultiPolygon"
  ) { result =>
    assert(result.features.forall(_.geometry.isInstanceOf[MultiPolygon[Double]]), "The feature type is MultiPolygon")
    assert(result.features.nonEmpty, "The feature set will be non-empty")
  }

  buildFileAssertion[FeatureCollection[Double, FreeMap]](
    statement = "process heterogeneous FeatureCollections",
    file = "FCHeterogeneous"
  ) { result =>
    assert(result.features.nonEmpty, "The feature set will be non-empty")
    assert(
      result.features.map(_.geometry) match {
        case (h0: MultiPoint[Double]) :: (h1: Point[Double]) :: Nil => h0.coordinates.elements.contains(h1.coordinates)
        case _                                                      => false
      },
      "Heterogeneity does not impact deserialization"
    )
  }

  buildFileAssertion[MultiPolygon[Double]](statement = "process a submarket record", file = "SubmarketExample-001") {
    result =>
      assert(result.coordinates.elements.nonEmpty, "The coordinate set will be non-empty")
  }

  buildFileAssertion[FeatureCollection[Double, FreeMap]](
    statement = "process a level 1 administrative US districts file",
    file = "AdminLevel1"
  ) { result =>
    assert(result.features.nonEmpty, "The feature set will be non-empty")
  }

  private[this] def classLoader = getClass.getClassLoader

  private[this] def buildFileAssertion[A: Decoder](statement: String, file: String)(asserting: A => Unit): Unit =
    it should s"$statement - $file" in {
      val path: Path = Paths.get(classLoader.getResource(s"geojson/$file.geojson").toURI)

      val parseTestFile: String => IO[A] = parser
        .parse(_)
        .fold(IO.raiseError, _.as[A].fold(IO.raiseError, IO.pure))

      val json = readAll[IO](path, Blocker.liftExecutionContext(ExecutionContext.global), 100000)
        .through(utf8Decode[IO])
        .compile
        .lastOrError
        .unsafeRunSync()

      parseTestFile(json)
        .map(asserting)
        .unsafeRunSync()
    }
}
