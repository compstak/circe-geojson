package com.compstak.geojson.implicits

import cats._
import cats.implicits._
import com.compstak.geojson._

trait SimpleGeometryInstances {

  /*
  An equality for GeoJSON geometries.

  Note: The geometries MUST have already been aligned to the same coordinate reference system (CRS) prior to comparison.
        Ensure you normalize geometries prior to evaluation.

  todo This implementation is not robust enough for extensive data computation. An application requiring true will need to
  todo implement a true topology model such as DE-9IM.
   */
  // scalastyle:off method.length cyclomatic.complexity
  implicit def catsStdEqForGeometryOptimistic[A: Eq]: Eq[GeoJsonGeometry[A]] =
    new Eq[GeoJsonGeometry[A]] {

      def eqv(x: GeoJsonGeometry[A], y: GeoJsonGeometry[A]): Boolean =
        (x, y) match {

          /*
      Point geometries are equal if their coordinates are equal in all dimensions.

      todo This doesn't handle scenarios where overflow is permissible for equality, e.g. -160 may be considered equal
      todo to 200 in many common degree notations
           */
          case (xs: Point[A], ys: Point[A]) => xs === ys

          /*
      Any two multi-point geometries are equal if their intersection is equal to their identities.

      todo I'm pretty certain there is a lattice structure that describes this.
           */
          case (xs: MultiPoint[A], ys: MultiPoint[A]) => xs === ys

          /*
      Two line string geometries are equal if each element in series is equal.

      todo The above isn't entirely true for closed line strings (which are also linear rings) since equivalence
      todo should then be defined with consideration for the cyclic ordering of elements.
           */
          case (xs: LineString[A], ys: LineString[A]) => xs === ys

          /*
      Multi- line string geometries are equal if each element in series is equal.

      todo The above isn't entirely true for closed line strings (which are also linear rings) since equivalence
      todo should then be defined with consideration for the cyclic ordering of elements.
           */
          case (xs: MultiLineString[A], ys: MultiLineString[A]) => xs === ys

          /*
      Polygon geometries share their equality definition with their underlying linear rings.
           */
          case (xs: Polygon[A], ys: Polygon[A]) =>
            xs.coordinates === ys.coordinates

          /*
      Multi-polygon geometries are equal if their intersection is equal to their identities.

      todo I'm pretty certain there is a lattice structure that describes this.
           */
          case (xs: MultiPolygon[A], ys: MultiPolygon[A]) =>
            xs.coordinates.elements
              .forall(ys.coordinates.elements.contains) && ys.coordinates.elements
              .forall(xs.coordinates.elements.contains)

          /*
      Multi-point and point geometries may show equality if the multi-point set contains only one element.
           */
          case (xs: MultiPoint[A], ys: Point[A]) =>
            xs.coordinates.elements
              .forall(_ === ys.coordinates) && xs.coordinates.elements.length === 1
          case (xs: Point[A], ys: MultiPoint[A]) =>
            ys.coordinates.elements
              .forall(_ === xs.coordinates) && ys.coordinates.elements.length === 1

          /*
      Multi- line string and polygon geometries may be equal if the multi- line string is closed and there exists a
      cyclic equality on the resulting two linear rings.
           */
          case (xs: MultiLineString[A], ys: Polygon[A]) =>
            LinearRing
              .ofOption(xs.coordinates.elements)
              .contains_(ys.coordinates) && xs.coordinates.elements.length === 1
          case (xs: Polygon[A], ys: MultiLineString[A]) =>
            LinearRing
              .ofOption(ys.coordinates.elements)
              .contains_(xs.coordinates) && ys.coordinates.elements.length === 1

          case _ => false
        }
      // scalastyle:on
    }
}

object simple extends SimpleGeometryInstances
