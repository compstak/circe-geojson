package compstak.geojson

import cats.implicits._
import io.circe._
import io.circe.syntax._

sealed abstract class GeoJsonObjectType(val tag: String) extends Product with Serializable

sealed abstract class GeometryType(
  override val tag: String
) extends GeoJsonObjectType(tag)

object GeometryType {
  case object Point extends GeometryType("Point")
  case object MultiPoint extends GeometryType("MultiPoint")
  case object LineString extends GeometryType("LineString")
  case object MultiLineString extends GeometryType("MultiLineString")
  case object Polygon extends GeometryType("Polygon")
  case object MultiPolygon extends GeometryType("MultiPolygon")

  val all: List[GeometryType] = Point :: MultiPoint ::
    LineString :: MultiLineString ::
    Polygon :: MultiPolygon ::
    Nil

  def tag(g: GeometryType): String = g.tag

  implicit val encoderForGeometryType: Encoder[GeometryType] = Encoder
    .instance(_.tag.asJson)

  implicit val decoderForGeometryType: Decoder[GeometryType] = Decoder.instance { cursor =>
    for {
      s <- cursor.as[String]
      t <- all
        .find(_.tag === s)
        .toRight(DecodingFailure(s"Invalid geometry type: $s", List.empty))
    } yield t
  }
}

object GeoJsonObjectType {
  case object GeometryCollection extends GeoJsonObjectType("GeometryCollection")
  case object Feature extends GeoJsonObjectType("Feature")
  case object FeatureCollection extends GeoJsonObjectType("FeatureCollection")

  val all: List[GeoJsonObjectType] = GeometryCollection :: Feature ::
    FeatureCollection :: GeometryType.all

  def tag(g: GeometryType): String = g.tag

  implicit val encoderForGeoJsonObjectType: Encoder[GeoJsonObjectType] = Encoder
    .instance(_.tag.asJson)

  implicit val decoderForGeoJsonObjectType: Decoder[GeoJsonObjectType] = Decoder.instance { cursor =>
    for {
      s <- cursor.as[String]
      t <- all
        .find(_.tag === s)
        .toRight(DecodingFailure(s"Invalid GeoJSON object type: $s", List.empty))
    } yield t
  }
}
