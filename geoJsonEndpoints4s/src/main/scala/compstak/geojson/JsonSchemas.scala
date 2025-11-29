package compstak.geojson

import cats._
import cats.implicits._
import endpoints4s.algebra
import endpoints4s.{Invalid, Valid, Validated}

import compstak.geojson._

trait JsonSchemas extends algebra.JsonSchemas {

  implicit def jsonSchemaPosition[A: JsonSchema]: JsonSchema[Position[A]] =
    implicitly[JsonSchema[List[A]]].xmapPartial {
      case x :: y :: Nil => Valid(Pos2(x, y): Position[A])
      case x :: y :: z :: Nil => Valid(Pos3(x, y, z): Position[A])
      case _ => Invalid("Coordinates must be two- or three-dimensional geographic positions")
    } {
      case Pos2(x, y) => x :: y :: Nil
      case Pos3(x, y, z) => x :: y :: z :: Nil
    }
      .withTitle("Position")

  implicit def jsonSchemaPositionSet[A: JsonSchema]: JsonSchema[PositionSet[A]] =
    implicitly[JsonSchema[List[Position[A]]]]
      .xmap(PositionSet(_))(_.elements)
      .withTitle("PositionSet")

  implicit def jsonSchemaBoundingBox[A: JsonSchema]: JsonSchema[BoundingBox[A]] =
    implicitly[JsonSchema[List[A]]].xmapPartial {
      case llbX :: llbY :: llbZ :: urtX :: urtY :: urtZ :: Nil =>
        Valid(BoundingBox(Pos3(llbX, llbY, llbZ), Pos3(urtX, urtY, urtZ)))
      case llbX :: llbY :: urtX :: urtY :: Nil =>
        Valid(BoundingBox(Pos2(llbX, llbY), Pos2(urtX, urtY)))
      case _ => Invalid("Not a valid bounding box")
    } { bb =>
      (bb.llb.z, bb.urt.z).tupled match {
        case Some((llbZ, urtZ)) => List(bb.llb.x, bb.llb.y, llbZ, bb.urt.x, bb.urt.y, urtZ)
        case None => List(bb.llb.x, bb.llb.y, bb.urt.x, bb.urt.y)
      }
    }
      .withTitle("BoundingBox")

  implicit def jsonSchemaLine[A: JsonSchema]: JsonSchema[Line[A]] =
    implicitly[JsonSchema[PositionSet[A]]]
      .xmapPartial(x => Validated.fromOption(Line.fromFoldable(x.elements))("A line instance must be non-empty"))(x =>
        PositionSet(x.list)
      )
      .withTitle("Line")

  implicit def jsonSchemaLineSet[A: JsonSchema]: JsonSchema[LineSet[A]] =
    implicitly[JsonSchema[List[Line[A]]]]
      .xmap(LineSet(_))(_.elements)
      .withTitle("LineSet")

  implicit def jsonSchemaLinearRing[A: JsonSchema: Eq]: JsonSchema[LinearRing[A]] =
    implicitly[JsonSchema[List[Position[A]]]]
      .xmapPartial(xs => Validated.fromEither(LinearRing.fromFoldable(xs).leftMap(err => err.getMessage :: Nil)))(
        _.list
      )
      .withTitle("LinearRing")

  implicit def jsonSchemaRingSet[A: JsonSchema: Eq]: JsonSchema[RingSet[A]] =
    implicitly[JsonSchema[List[LinearRing[A]]]]
      .xmap(RingSet(_))(_.elements)
      .withTitle("RingSet")

  implicit def jsonSchemaPolygonSet[A: JsonSchema: Eq]: JsonSchema[PolygonSet[A]] =
    implicitly[JsonSchema[List[RingSet[A]]]]
      .xmap(PolygonSet(_))(_.elements)
      .withTitle("PolygonSet")

  implicit def pointTagged[A: JsonSchema]: Tagged[Point[A]] =
    field[Position[A]]("coordinates")
      .zip(optField[BoundingBox[A]]("bbox"))
      .xmap { case (pos, bb) => Point(pos, bb) }(p => p.coordinates -> p.bbox)
      .tagged(GeometryType.Point.tag)
      .withTitle("Point")

  implicit def multiPointTagged[A: JsonSchema]: Tagged[MultiPoint[A]] =
    field[PositionSet[A]]("coordinates")
      .zip(optField[BoundingBox[A]]("bbox"))
      .xmap { case (poss, bb) => MultiPoint(poss, bb) }(p => p.coordinates -> p.bbox)
      .tagged(GeometryType.MultiPoint.tag)
      .withTitle("MultiPoint")

  implicit def lineStringTagged[A: JsonSchema]: Tagged[LineString[A]] =
    field[Line[A]]("coordinates")
      .zip(optField[BoundingBox[A]]("bbox"))
      .xmap { case (pos, bb) => LineString(pos, bb) }(p => p.coordinates -> p.bbox)
      .tagged(GeometryType.LineString.tag)
      .withTitle("LineString")

  implicit def multiLineStringTagged[A: JsonSchema]: Tagged[MultiLineString[A]] =
    field[LineSet[A]]("coordinates")
      .zip(optField[BoundingBox[A]]("bbox"))
      .xmap { case (pos, bb) => MultiLineString(pos, bb) }(p => p.coordinates -> p.bbox)
      .tagged(GeometryType.MultiLineString.tag)
      .withTitle("MultiLineString")

  implicit def polygonTagged[A: JsonSchema: Eq]: Tagged[Polygon[A]] =
    field[RingSet[A]]("coordinates")
      .zip(optField[BoundingBox[A]]("bbox"))
      .xmap { case (pos, bb) => Polygon(pos, bb) }(p => p.coordinates -> p.bbox)
      .tagged(GeometryType.Polygon.tag)
      .withTitle("Polygon")

  implicit def multiPolygonTagged[A: JsonSchema: Eq]: Tagged[MultiPolygon[A]] =
    field[PolygonSet[A]]("coordinates")
      .zip(optField[BoundingBox[A]]("bbox"))
      .xmap { case (pos, bb) =>
        MultiPolygon(pos, bb)
      }(p => p.coordinates -> p.bbox)
      .tagged(GeometryType.MultiPolygon.tag)
      .withTitle("MultiPolygon")

  implicit def jsonSchemaGeoJsonGeometry[A: JsonSchema: Eq]: JsonSchema[GeoJsonGeometry[A]] =
    pointTagged[A]
      .orElseMerge(multiPointTagged[A])
      .orElseMerge(lineStringTagged[A])
      .orElseMerge(multiLineStringTagged[A])
      .orElseMerge(polygonTagged[A])
      .orElseMerge(multiPolygonTagged[A])

  implicit def jsonSchemaGeometryCollection[A: JsonSchema: Eq]: JsonSchema[GeometryCollection[A]] =
    field[List[GeoJsonGeometry[A]]]("geometries")
      .zip(optField[BoundingBox[A]]("bbox"))
      .zip(field("type")(literal(GeoJsonObjectType.GeometryCollection.tag)))
      .xmap { case (geometries, bbox) =>
        GeometryCollection(geometries, bbox)
      }(x => (x.geometries, x.bbox))

  implicit def jsonSchemaFeature[A: JsonSchema: Eq, P: JsonSchema]: JsonSchema[Feature[A, P]] =
    optField[String]("id")
      .zip(field[GeoJsonGeometry[A]]("geometry"))
      .zip(field("type")(literal(GeoJsonObjectType.Feature.tag)))
      .zip(field[P]("properties"))
      .zip(optField[BoundingBox[A]]("bbox"))
      .xmap { case (id, geometry, properties, bbox) =>
        Feature(geometry, properties, id, bbox)
      }(x => (x.id, x.geometry, x.properties, x.bbox))
      .withTitle("Feature")

  implicit def jsonSchemaFeatureCollection[A: JsonSchema: Eq, P: JsonSchema]: JsonSchema[FeatureCollection[A, P]] =
    field[String]("type")
      .zip(optField[BoundingBox[A]]("bbox"))
      .zip(field[Seq[Feature[A, P]]]("features"))
      .xmap { case (_, bbox, features) => FeatureCollection(features, bbox) }(fc =>
        (GeoJsonObjectType.FeatureCollection.tag, fc.bbox, fc.features)
      )
      .withTitle("FeatureCollection")
}
