package compstak.geojson

import cats._
import cats.implicits._
import org.{postgis => pg}

object postgis {

  implicit class GeoJsonGeometryOps[N](val g: GeoJsonGeometry[N]) extends AnyVal {

    def asPostGIS(fa: N => Double)(implicit E: Eq[N]) =
      g match {
        case p: Point[N]             => gis.fromPoint(fa)(p)
        case mp: MultiPoint[N]       => gis.fromMultiPoint(fa)(mp)
        case ls: LineString[N]       => gis.fromLine(fa)(ls)
        case mls: MultiLineString[N] => gis.fromMultiLine(fa)(mls)
        case p: Polygon[N]           => gis.fromPolygon(fa)(p)
        case mp: MultiPolygon[N]     => gis.fromMultiPolygon(fa)(mp)
      }
  }

  implicit class PostGISGeometryOps(val g: pg.Geometry) extends AnyVal {

    def asGeoJson[N: Eq](fa: Double => N): GeoJsonGeometry[N] =
      g match {
        case p: pg.Point             => json.fromPoint(fa)(p)
        case mp: pg.MultiPoint       => json.fromMultiPoint(fa)(mp)
        case ls: pg.LineString       => json.fromLineString(fa)(ls)
        case mls: pg.MultiLineString => json.fromMultiLineString(fa)(mls)
        case p: pg.Polygon           => json.fromPolygon(fa)(p)
        case mp: pg.MultiPolygon     => json.fromMultiPolygon(fa)(mp)
        case lr: pg.LinearRing       => json.fromLinearRing(fa)(lr)
      }
  }

  object gis {

    def fromPoint[N](fa: N => Double)(p: Point[N]): pg.Point =
      new pg.Point(
        fa(p.coordinates.x),
        fa(p.coordinates.y)
      )

    def fromMultiPoint[N](fa: N => Double)(mp: MultiPoint[N]): pg.MultiPoint =
      new pg.MultiPoint(
        mp.coordinates.elements
          .map({ position =>
            new pg.Point(fa(position.x), fa(position.y))
          })
          .toArray
      )

    def fromLine[N](fa: N => Double)(l: LineString[N]): pg.LineString =
      new pg.LineString(
        l.coordinates.list
          .map({ position =>
            new pg.Point(fa(position.x), fa(position.y))
          })
          .toArray
      )

    def fromMultiLine[N](fa: N => Double)(mls: MultiLineString[N]): pg.MultiLineString =
      new pg.MultiLineString(
        mls.coordinates.elements.map { line =>
          new pg.LineString(
            line.list
              .map({ position =>
                new pg.Point(fa(position.x), fa(position.y))
              })
              .toArray
          )
        }.toArray
      )

    def makePolygon[N](fa: N => Double)(lr: LinearRing[N]): pg.Polygon = lr match {
      case _: LRing0[N] => new pg.Polygon()
      case r: LRingN[N] =>
        new pg.Polygon(
          (new pg.LinearRing(
            r.list.flatMap { line =>
              new pg.LineString(
                line.list
                  .map({ position =>
                    new pg.Point(fa(position.x), fa(position.y))
                  })
                  .toArray
              ).getPoints()
            }.toArray
          ) :: Nil).toArray
        )
    }

    def fromPolygon[N](fa: N => Double)(p: Polygon[N]): pg.Polygon =
      makePolygon(fa)(p.coordinates)

    def fromMultiPolygon[N](fa: N => Double)(p: MultiPolygon[N]): pg.MultiPolygon =
      new pg.MultiPolygon(
        p.coordinates.elements.map(makePolygon(fa)).toArray
      )
  }

  object json {

    def pointToPosition[N](fa: Double => N)(p: pg.Point): Position[N] =
      Option(p.getZ).fold[Position[N]](
        Pos2[N](fa(p.getX), fa(p.getY))
      ) { z =>
        Pos3[N](fa(p.getX), fa(p.getY), fa(z))
      }

    def makeLine[N](fa: Double => N)(points: Array[pg.Point]): Line[N] = {
      val positions = points.toList.map(pointToPosition(fa))
      Line(positions)
    }

    def makeLinearRing[N: Eq](fa: Double => N)(polygon: pg.Polygon): LinearRing[N] =
      LinearRing(
        Range(0, polygon.numRings())
          .map(polygon.getRing(_))
          .map(_.getPoints())
          .map(makeLine(fa))
          .toList
      )

    def fromPoint[N](fa: Double => N)(p: pg.Point): Point[N] =
      Point[N](
        coordinates = pointToPosition(fa)(p),
        bbox = None
      )

    def fromMultiPoint[N](fa: Double => N)(mp: pg.MultiPoint): MultiPoint[N] =
      MultiPoint[N](
        coordinates = PositionSet(mp.getPoints.toList.map(pointToPosition(fa))),
        bbox = None
      )

    def fromLineString[N](fa: Double => N)(ls: pg.LineString): LineString[N] = {
      val positions = ls.getPoints.toList.map(pointToPosition(fa))
      LineString[N](
        coordinates = makeLine(fa)(ls.getPoints),
        bbox = None
      )
    }

    def fromMultiLineString[N](fa: Double => N)(mls: pg.MultiLineString): MultiLineString[N] =
      MultiLineString[N](
        coordinates = LineSet[N](mls.getLines().toList.map(_.getPoints()).map(makeLine(fa))),
        bbox = None
      )

    def fromPolygon[N: Eq](fa: Double => N)(ls: pg.Polygon): Polygon[N] =
      Polygon[N](
        coordinates = makeLinearRing(fa)(ls),
        bbox = None
      )

    def fromMultiPolygon[N: Eq](fa: Double => N)(ls: pg.MultiPolygon): MultiPolygon[N] =
      MultiPolygon[N](
        coordinates = RingSet[N](
          ls.getPolygons().toList.map(makeLinearRing(fa))
        ),
        bbox = None
      )

    def fromLinearRing[N: Eq](fa: Double => N)(ls: pg.LinearRing): Polygon[N] =
      Polygon[N](
        coordinates = LinearRing(List.empty[Line[N]]),
        bbox = None
      )
  }
}
