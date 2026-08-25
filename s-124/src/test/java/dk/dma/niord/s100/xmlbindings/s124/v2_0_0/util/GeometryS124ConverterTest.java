package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;

import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.CurveProperty;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.PointProperty;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.S100SpatialAttributeType;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.SurfaceProperty;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.PointPropertyImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.PointTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.AbstractRingPropertyType;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.LineStringSegmentType;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.LinearRingType;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.PolygonPatchType;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.SurfacePropertyImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.SurfaceTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.AbstractRingPropertyTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.LineStringSegmentTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.LinearRingTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.PatchesImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.PolygonPatchTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.PosListImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.PosImpl;

/**
 * Pins the coordinate-order contract of {@link GeometryS124Converter}: GML positions are
 * (lat, lon), JTS coordinates are (x=lon, y=lat), and every translation swaps.
 *
 * <p>The point read path used to skip the swap, so JTS geometries for points carried
 * x=lat / y=lon and every envelope derived from them serialized axis-swapped.</p>
 */
class GeometryS124ConverterTest {

    // Drogden lighthouse area, Denmark: lat 55.5467, lon 12.72
    private static final double LAT = 55.5467;
    private static final double LON = 12.72;

    private static PointProperty gmlPoint(double lat, double lon) {
        PosImpl pos = new PosImpl();
        pos.setValue(new Double[] { lat, lon });
        PointTypeImpl point = new PointTypeImpl();
        point.setPos(pos);
        PointPropertyImpl property = new PointPropertyImpl();
        property.setPoint(point);
        return property;
    }

    @Test
    void pointReadSwapsGmlLatLonToJtsLonLat() {
        Geometry geometry = GeometryS124Converter
                .pointCurveSurfaceToGeometry(List.of(gmlPoint(LAT, LON)));

        assertThat(geometry.getCoordinate().x).isEqualTo(LON);
        assertThat(geometry.getCoordinate().y).isEqualTo(LAT);
    }

    @Test
    void pointRoundTripPreservesGmlLatLonOrder() {
        Geometry geometry = GeometryS124Converter
                .pointCurveSurfaceToGeometry(List.of(gmlPoint(LAT, LON)));
        List<S100SpatialAttributeType> out =
                GeometryS124Converter.geometryToS124PointCurveSurfaceGeometry(geometry);

        Double[] pos = ((PointProperty) out.get(0)).getPoint().getPos().getValue();
        assertThat(pos).containsExactly(LAT, LON);
    }

    /**
     * A point and the write-side of a JTS point must agree with the curve/surface paths:
     * a point written from JTS (lon, lat) must read back to the identical coordinate.
     */
    @Test
    void jtsPointRoundTripsThroughGml() {
        GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);
        Geometry original = factory.createPoint(new Coordinate(LON, LAT));

        List<S100SpatialAttributeType> gml =
                GeometryS124Converter.geometryToS124PointCurveSurfaceGeometry(original);
        Geometry roundTripped = GeometryS124Converter.pointCurveSurfaceToGeometry(gml);

        assertThat(roundTripped.getCoordinate()).isEqualTo(original.getCoordinate());
    }

    private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    /** GM_Point holds exactly one position: a MultiPoint must become one PointProperty per member. */
    @Test
    void multiPointBecomesOnePointPropertyPerMember() {
        Geometry multiPoint = FACTORY.createMultiPointFromCoords(new Coordinate[] {
                new Coordinate(LON, LAT), new Coordinate(13.0, 56.0) });

        List<S100SpatialAttributeType> out =
                GeometryS124Converter.geometryToS124PointCurveSurfaceGeometry(multiPoint);

        assertThat(out).hasSize(2).allMatch(PointProperty.class::isInstance);
        assertThat(((PointProperty) out.get(0)).getPoint().getPos().getValue()).containsExactly(LAT, LON);
        assertThat(((PointProperty) out.get(1)).getPoint().getPos().getValue()).containsExactly(56.0, 13.0);
    }

    /** Disjoint lines must not be fused into a single segment with a phantom connecting leg. */
    @Test
    void multiLineStringBecomesOneCurvePropertyPerMember() {
        LineString first = FACTORY.createLineString(new Coordinate[] {
                new Coordinate(0, 0), new Coordinate(1, 1) });
        LineString second = FACTORY.createLineString(new Coordinate[] {
                new Coordinate(10, 10), new Coordinate(11, 11) });
        Geometry multiLine = FACTORY.createMultiLineString(new LineString[] { first, second });

        List<S100SpatialAttributeType> out =
                GeometryS124Converter.geometryToS124PointCurveSurfaceGeometry(multiLine);

        assertThat(out).hasSize(2).allMatch(CurveProperty.class::isInstance);
        LineStringSegmentType segment = (LineStringSegmentType) ((CurveProperty) out.get(0))
                .getCurve().getSegments().getAbstractCurveSegments().get(0).getValue();
        assertThat(segment.getPosList().getValue()).containsExactly(0.0, 0.0, 1.0, 1.0);
    }

    /** A polygon's holes must be written as interior rings, not appended to the exterior. */
    @Test
    void polygonWithHoleKeepsInteriorRing() {
        Polygon polygon = polygonWithHole();

        List<S100SpatialAttributeType> out =
                GeometryS124Converter.geometryToS124PointCurveSurfaceGeometry(polygon);

        assertThat(out).hasSize(1);
        PolygonPatchType patch = (PolygonPatchType) ((SurfaceProperty) out.get(0))
                .getSurface().getPatches().getAbstractSurfacePatches().get(0).getValue();
        assertThat(ringCoordinates(patch.getExterior())).hasSize(5 * 2);
        assertThat(patch.getInteriors()).hasSize(1);
        assertThat(ringCoordinates(patch.getInteriors().get(0))).hasSize(5 * 2);
    }

    /**
     * S-100 Part 7 clause 7-4.3.2 (level 3a, S-124 clause 8.8): exterior rings clockwise,
     * interior rings counter-clockwise, evaluated on a north-up map (JTS lon/lat axes).
     */
    @Test
    void ringOrientationIsNormalisedToLevel3a() {
        Polygon polygon = polygonWithHole(); // shell CCW, hole CW - both must be flipped

        List<S100SpatialAttributeType> out =
                GeometryS124Converter.geometryToS124PointCurveSurfaceGeometry(polygon);

        PolygonPatchType patch = (PolygonPatchType) ((SurfaceProperty) out.get(0))
                .getSurface().getPatches().getAbstractSurfacePatches().get(0).getValue();
        assertThat(Orientation.isCCW(toJtsCoordinates(ringCoordinates(patch.getExterior())))).isFalse();
        assertThat(Orientation.isCCW(toJtsCoordinates(ringCoordinates(patch.getInteriors().get(0))))).isTrue();
    }

    @Test
    void polygonWithHoleRoundTripsThroughGml() {
        Polygon polygon = polygonWithHole();

        Geometry roundTripped = GeometryS124Converter.pointCurveSurfaceToGeometry(
                GeometryS124Converter.geometryToS124PointCurveSurfaceGeometry(polygon));

        // The read path wraps each surface's patches in a GeometryCollection.
        assertThat(roundTripped.getNumGeometries()).isEqualTo(1);
        assertThat(roundTripped.getGeometryN(0)).isInstanceOf(Polygon.class);
        assertThat(((Polygon) roundTripped.getGeometryN(0)).getNumInteriorRing()).isEqualTo(1);
        assertThat(roundTripped.getArea()).isEqualTo(polygon.getArea());
    }

    /** The S-100 GML profile requires >= 2 positions per curve segment and >= 4 per ring. */
    @Test
    void emptyGeometriesFailInsteadOfEmittingInvalidGml() {
        assertThatThrownBy(() -> GeometryS124Converter
                .geometryToS124PointCurveSurfaceGeometry(FACTORY.createPoint()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GeometryS124Converter
                .geometryToS124PointCurveSurfaceGeometry(FACTORY.createLineString()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GeometryS124Converter
                .geometryToS124PointCurveSurfaceGeometry(FACTORY.createPolygon()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** gml:id is mandatory on every GML object (S100_gmlProfile AbstractGMLType). */
    @Test
    void emittedGeometriesCarryUniqueGmlIds() {
        Geometry collection = FACTORY.createGeometryCollection(new Geometry[] {
                FACTORY.createPoint(new Coordinate(LON, LAT)),
                FACTORY.createLineString(new Coordinate[] { new Coordinate(0, 0), new Coordinate(1, 1) }),
                polygonWithHole(),
        });

        AtomicInteger sequence = new AtomicInteger();
        List<S100SpatialAttributeType> out = GeometryS124Converter
                .geometryToS124PointCurveSurfaceGeometry(collection, () -> "DK.G." + sequence.incrementAndGet());

        assertThat(((PointProperty) out.get(0)).getPoint().getId()).isEqualTo("DK.G.1");
        assertThat(((CurveProperty) out.get(1)).getCurve().getId()).isEqualTo("DK.G.2");
        assertThat(((SurfaceProperty) out.get(2)).getSurface().getId()).isEqualTo("DK.G.3");

        List<S100SpatialAttributeType> defaults = GeometryS124Converter
                .geometryToS124PointCurveSurfaceGeometry(FACTORY.createPoint(new Coordinate(LON, LAT)));
        assertThat(((PointProperty) defaults.get(0)).getPoint().getId()).isNotBlank();
    }

    /** A profile-valid segment encoded as a sequence of gml:pos (no posList) must parse. */
    @Test
    void curveSegmentEncodedAsPosSequenceParses() {
        dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.LineStringSegmentTypeImpl segment =
                new dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.LineStringSegmentTypeImpl();
        segment.getPosAndPointProperties().add(pos(55.5, 12.7));
        segment.getPosAndPointProperties().add(pos(56.0, 13.0));

        Geometry geometry = GeometryS124Converter.pointCurveSurfaceToGeometry(
                List.of(curveProperty(new dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.ObjectFactory()
                        .createLineStringSegment(segment))));

        assertThat(geometry.getCoordinates()).containsExactly(
                new Coordinate(12.7, 55.5), new Coordinate(13.0, 56.0));
    }

    /** Arc segments cannot be represented without densification: fail, never drop silently. */
    @Test
    void unsupportedCurveSegmentTypeFailsInsteadOfSilentlyDropping() {
        dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.S100ArcByCenterPointTypeImpl arc =
                new dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.S100ArcByCenterPointTypeImpl();
        List<S100SpatialAttributeType> properties = List.of(curveProperty(
                new dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.ObjectFactory()
                        .createS100ArcByCenterPoint(arc)));

        assertThatThrownBy(() -> GeometryS124Converter.pointCurveSurfaceToGeometry(properties))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("S100ArcByCenterPoint");
    }

    @Test
    void compositeCurvePropertyFailsInsteadOfSilentlyDropping() {
        dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.CurvePropertyImpl property =
                new dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.CurvePropertyImpl();
        property.setCompositeCurve(
                new dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.CompositeCurveTypeImpl());
        List<S100SpatialAttributeType> properties = List.of(property);

        assertThatThrownBy(() -> GeometryS124Converter.pointCurveSurfaceToGeometry(properties))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("compositeCurve");
    }

    /**
     * S-100 Part 10b restricts positions to two dimensions, so an odd ordinate count is a
     * malformed list. Truncating it would drop a latitude and move the geometry.
     */
    @Test
    void oddLengthPosListIsRejected() {
        LineStringSegmentTypeImpl segment = new LineStringSegmentTypeImpl();
        PosListImpl posList = new PosListImpl();
        posList.setValue(new Double[] { 55.0, 12.0, 56.0 });
        segment.setPosList(posList);
        List<S100SpatialAttributeType> properties = List.of(curveProperty(
                new dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.ObjectFactory()
                        .createLineStringSegment(segment)));

        assertThatThrownBy(() -> GeometryS124Converter.pointCurveSurfaceToGeometry(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not a whole number of two dimensional positions");
    }

    /**
     * Dropping a malformed hole would turn an area with an excluded region into one that
     * covers it - the dangerous direction for a navigational warning.
     */
    @Test
    void malformedInteriorRingIsRejectedRatherThanDropped() {
        PolygonPatchTypeImpl patch = new PolygonPatchTypeImpl();
        patch.setExterior(ring(new Double[] {
                55.0, 12.0, 55.0, 13.0, 56.0, 13.0, 56.0, 12.0, 55.0, 12.0 }));
        // Two positions cannot close a ring.
        patch.getInteriors().add(ring(new Double[] { 55.4, 12.4, 55.6, 12.6 }));

        SurfaceTypeImpl surface = new SurfaceTypeImpl();
        PatchesImpl patches = new PatchesImpl();
        patches.getAbstractSurfacePatches().add(
                new dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.ObjectFactory()
                        .createPolygonPatch(patch));
        surface.setPatches(patches);
        SurfacePropertyImpl property = new SurfacePropertyImpl();
        property.setSurface(surface);
        List<S100SpatialAttributeType> properties = List.of(property);

        assertThatThrownBy(() -> GeometryS124Converter.pointCurveSurfaceToGeometry(properties))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Malformed interior ring");
    }

    private static AbstractRingPropertyType ring(Double[] latLonValues) {
        PosListImpl posList = new PosListImpl();
        posList.setValue(latLonValues);
        LinearRingTypeImpl linearRing = new LinearRingTypeImpl();
        linearRing.setPosList(posList);
        AbstractRingPropertyTypeImpl ringProperty = new AbstractRingPropertyTypeImpl();
        ringProperty.setAbstractRing(
                new dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.ObjectFactory()
                        .createLinearRing(linearRing));
        return ringProperty;
    }

    private static PosImpl pos(double lat, double lon) {
        PosImpl pos = new PosImpl();
        pos.setValue(new Double[] { lat, lon });
        return pos;
    }

    private static S100SpatialAttributeType curveProperty(
            jakarta.xml.bind.JAXBElement<? extends dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.AbstractCurveSegmentType> segment) {
        dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.SegmentsImpl segments =
                new dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.SegmentsImpl();
        segments.getAbstractCurveSegments().add(segment);
        dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.CurveTypeImpl curve =
                new dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.CurveTypeImpl();
        curve.setSegments(segments);
        dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.CurvePropertyImpl property =
                new dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.CurvePropertyImpl();
        property.setCurve(curve);
        return property;
    }

    /** Shell counter-clockwise, hole clockwise - the conventions GeoJSON sources produce. */
    private static Polygon polygonWithHole() {
        LinearRing shell = FACTORY.createLinearRing(new Coordinate[] {
                new Coordinate(0, 0), new Coordinate(10, 0), new Coordinate(10, 10),
                new Coordinate(0, 10), new Coordinate(0, 0) });
        LinearRing hole = FACTORY.createLinearRing(new Coordinate[] {
                new Coordinate(4, 4), new Coordinate(4, 6), new Coordinate(6, 6),
                new Coordinate(6, 4), new Coordinate(4, 4) });
        return FACTORY.createPolygon(shell, new LinearRing[] { hole });
    }

    private static Double[] ringCoordinates(AbstractRingPropertyType ring) {
        return ((LinearRingType) ring.getAbstractRing().getValue()).getPosList().getValue();
    }

    /** Swap serialized (lat, lon) pairs back to JTS (x=lon, y=lat) coordinates. */
    private static Coordinate[] toJtsCoordinates(Double[] posList) {
        Coordinate[] coordinates = new Coordinate[posList.length / 2];
        for (int i = 0; i < coordinates.length; i++) {
            coordinates[i] = new Coordinate(posList[2 * i + 1], posList[2 * i]);
        }
        return coordinates;
    }
}
