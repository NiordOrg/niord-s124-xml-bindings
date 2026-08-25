package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import org.locationtech.jts.algorithm.Orientation;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateArrays;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;

import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.CurveProperty;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.CurveType;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.PointProperty;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.PointType;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.S100SpatialAttributeType;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.SurfaceProperty;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.SurfaceType;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.CurvePropertyImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.CurveTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.PointPropertyImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.PointTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.SurfacePropertyImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.SurfaceTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.AbstractCurveSegmentType;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.AbstractRingPropertyType;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.AbstractRingType;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.BoundingShapeType;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.EnvelopeType;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.GeodesicStringType;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.LineStringSegmentType;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.LinearRingType;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.ObjectFactory;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.Patches;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.PolygonPatchType;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.Pos;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.PosList;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.Segments;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.AbstractRingPropertyTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.LineStringSegmentTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.LinearRingTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.PatchesImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.PolygonPatchTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.PosImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.PosListImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.SegmentsImpl;
import jakarta.xml.bind.JAXBElement;

/**
 * Converts JTS geometries to and from S-124 point / curve / surface property objects.
 *
 * <p>GML uses lat,lon coordinate order; JTS stores coordinates as lon,lat. The conversion
 * swaps order on every translation.</p>
 *
 * <p>The generation path enforces the S-100 GML profile geometry rules (S-100 5.2.0
 * Part 10b clause 10b-9 / S100_gmlProfile.xsd): a curve segment carries at least two
 * positions, a linear ring at least four with first = last, and every emitted GML object
 * carries the mandatory {@code gml:id}. Multi-geometries are decomposed into one spatial
 * property per member, polygon interior rings (holes) are preserved, and ring orientation
 * is normalised to geometry configuration level 3a (S-100 Part 7 clause 7-4.3.2, required
 * by S-124 Ed 2.0.0 clause 8.8): exterior rings clockwise, interior rings
 * counter-clockwise. Empty or degenerate geometries fail with
 * {@link IllegalArgumentException} instead of emitting non-conformant GML.</p>
 */
public final class GeometryS124Converter {

    private static final ObjectFactory PROFILE_FACTORY = new ObjectFactory();

    /** Backs the default gml:id sequence; ids must be unique within a dataset. */
    private static final AtomicLong DEFAULT_ID_SEQUENCE = new AtomicLong();

    private GeometryS124Converter() {
    }

    /**
     * Converts a JTS geometry to spatial properties, assigning each emitted GML object a
     * JVM-unique {@code gml:id} of the form {@code G.n}. Use
     * {@link #geometryToS124PointCurveSurfaceGeometry(Geometry, Supplier)} to control the
     * ids (they must be unique within the containing dataset).
     */
    public static List<S100SpatialAttributeType> geometryToS124PointCurveSurfaceGeometry(Geometry geometry) {
        return geometryToS124PointCurveSurfaceGeometry(geometry,
                () -> "G." + DEFAULT_ID_SEQUENCE.incrementAndGet());
    }

    /**
     * Converts a JTS geometry to spatial properties. {@code gmlIds} supplies the mandatory
     * {@code gml:id} for each emitted Point / Curve / Surface, in emission order.
     *
     * @throws IllegalArgumentException if the geometry (or a member of a collection) is
     *         empty or violates the S-100 GML profile minimum-position rules
     */
    public static List<S100SpatialAttributeType> geometryToS124PointCurveSurfaceGeometry(
            Geometry geometry, Supplier<String> gmlIds) {
        List<S100SpatialAttributeType> result = new ArrayList<>();
        populatePointCurveSurfaceToGeometry(geometry, result, gmlIds);
        return result;
    }

    /**
     * Convert a GML {@link BoundingShapeType} envelope (lat/lon ordered) to a JTS polygon
     * (lon/lat ordered) in EPSG:4326. Returns {@code null} if the envelope is absent or
     * malformed.
     */
    public static Geometry envelopeToJts(BoundingShapeType boundingShape) {
        EnvelopeType env = Optional.ofNullable(boundingShape).map(BoundingShapeType::getEnvelope).orElse(null);
        if (env == null || env.getLowerCorner() == null || env.getUpperCorner() == null) {
            return null;
        }
        Double[] lower = env.getLowerCorner().getValue();
        Double[] upper = env.getUpperCorner().getValue();
        if (lower == null || upper == null || lower.length < 2 || upper.length < 2) {
            return null;
        }
        double minLat = lower[0], minLon = lower[1];
        double maxLat = upper[0], maxLon = upper[1];
        GeometryFactory gf = new GeometryFactory(new PrecisionModel(), 4326);
        return gf.createPolygon(new Coordinate[] {
                new Coordinate(minLon, minLat),
                new Coordinate(maxLon, minLat),
                new Coordinate(maxLon, maxLat),
                new Coordinate(minLon, maxLat),
                new Coordinate(minLon, minLat),
        });
    }

    public static Geometry pointCurveSurfaceToGeometry(List<S100SpatialAttributeType> properties) {
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        return properties.stream().map(pty -> {
            if (pty instanceof PointProperty) {
                return Optional.of(pty)
                        .map(PointProperty.class::cast)
                        .map(PointProperty::getPoint)
                        .map(PointType::getPos)
                        // GML order is lat,lon; JTS expects lon,lat.
                        .map(pos -> new Coordinate(pos.getValue()[1], pos.getValue()[0]))
                        .map(geometryFactory::createPoint)
                        .map(Geometry.class::cast)
                        .orElse(geometryFactory.createEmpty(0));
            } else if (pty instanceof CurveProperty curveProperty) {
                // compositeCurve / orientableCurve carry curve members by reference, which
                // cannot be resolved here; failing beats silently losing the geometry.
                if (curveProperty.getCompositeCurve() != null || curveProperty.getOrientableCurve() != null) {
                    throw new UnsupportedOperationException("Unsupported curve property encoding"
                            + " (compositeCurve/orientableCurve): only inline curves are supported");
                }
                return geometryFactory.createGeometryCollection(Optional.of(curveProperty)
                        .map(CurveProperty::getCurve)
                        .map(CurveType::getSegments)
                        .map(Segments::getAbstractCurveSegments)
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(JAXBElement::getValue)
                        .map(GeometryS124Converter::segmentToCoordinates)
                        .map(coords -> coords.length == 1
                                ? geometryFactory.createPoint(coords[0])
                                : geometryFactory.createLineString(coords))
                        .toList()
                        .toArray(Geometry[]::new));
            } else if (pty instanceof SurfaceProperty) {
                return geometryFactory.createGeometryCollection(Optional.of(pty)
                        .map(SurfaceProperty.class::cast)
                        .map(SurfaceProperty::getSurface)
                        .map(SurfaceType::getPatches)
                        .map(Patches::getAbstractSurfacePatches)
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(JAXBElement::getValue)
                        .filter(PolygonPatchType.class::isInstance)
                        .map(PolygonPatchType.class::cast)
                        .map(patch -> patchToGeometry(patch, geometryFactory))
                        .filter(Objects::nonNull)
                        .toList()
                        .toArray(Geometry[]::new));
            } else {
                throw new UnsupportedOperationException("Don't know how to convert " + pty);
            }
        }).reduce(geometryFactory.createEmpty(-1), (un, el) -> un == null || un.isEmpty() ? el : un.union(el));
    }

    private static Geometry patchToGeometry(PolygonPatchType patch, GeometryFactory geometryFactory) {
        Coordinate[] shell = ringCoordinates(patch.getExterior());
        if (shell == null) {
            return null;
        }
        if (shell.length == 1) {
            return geometryFactory.createPoint(shell[0]);
        }
        LinearRing[] holes = patch.getInteriors().stream()
                .map(GeometryS124Converter::ringCoordinates)
                .filter(Objects::nonNull)
                .filter(coords -> coords.length >= 4)
                .map(geometryFactory::createLinearRing)
                .toArray(LinearRing[]::new);
        return geometryFactory.createPolygon(geometryFactory.createLinearRing(shell), holes);
    }

    private static Coordinate[] ringCoordinates(AbstractRingPropertyType ringProperty) {
        AbstractRingType ring = Optional.ofNullable(ringProperty)
                .map(AbstractRingPropertyType::getAbstractRing)
                .map(JAXBElement::getValue)
                .orElse(null);
        if (ring == null) {
            return null;
        }
        if (!(ring instanceof LinearRingType linearRing)) {
            // gml:Ring rings are built from curve members (possibly by reference); failing
            // beats silently dropping the patch.
            throw new UnsupportedOperationException("Unsupported ring type "
                    + ring.getClass().getSimpleName() + ": only gml:LinearRing is supported");
        }
        return positionsToCoordinates(linearRing.getPosList(), linearRing.getPosAndPointProperties(),
                "LinearRing");
    }

    /**
     * Reads the control points of a curve segment. Geodesic strings are read as their control
     * points (interpolation between them is approximated as straight lines). Arc, circle and
     * spline segments cannot be represented without densification, so they fail loudly
     * instead of being silently dropped.
     */
    private static Coordinate[] segmentToCoordinates(AbstractCurveSegmentType segment) {
        if (segment instanceof LineStringSegmentType lineString) {
            return positionsToCoordinates(lineString.getPosList(), lineString.getPosAndPointProperties(),
                    "LineStringSegment");
        }
        if (segment instanceof GeodesicStringType geodesic) {
            return positionsToCoordinates(geodesic.getPosList(), geodesic.getPosAndPointProperties(),
                    "GeodesicString");
        }
        throw new UnsupportedOperationException("Unsupported curve segment type "
                + segment.getClass().getSimpleName()
                + ": arc/circle/spline interpolations are not supported");
    }

    /** Reads either a {@code gml:posList} or a sequence of {@code gml:pos} elements. */
    private static Coordinate[] positionsToCoordinates(PosList posList, List<Object> posAndPointProperties,
            String context) {
        if (posList != null) {
            return gmlPosListToCoordinates(posList);
        }
        List<Coordinate> result = new ArrayList<>();
        for (Object item : posAndPointProperties == null ? Collections.emptyList() : posAndPointProperties) {
            if (item instanceof Pos pos && pos.getValue() != null && pos.getValue().length >= 2) {
                // GML order is lat,lon; JTS expects lon,lat.
                result.add(new Coordinate(pos.getValue()[1], pos.getValue()[0]));
            } else {
                throw new UnsupportedOperationException("Unsupported position encoding in " + context
                        + ": " + (item == null ? "null" : item.getClass().getSimpleName()));
            }
        }
        return result.toArray(new Coordinate[0]);
    }

    private static void populatePointCurveSurfaceToGeometry(
            Geometry geometry, List<S100SpatialAttributeType> out, Supplier<String> gmlIds) {
        if (geometry == null) {
            return;
        }

        // MultiPoint / MultiLineString / MultiPolygon implement Puntal / Lineal / Polygonal
        // as well, so collections must be decomposed first: flattening their members into a
        // single pos / posList fuses disjoint geometries into one (GM_Point holds exactly one
        // position; GM_Curve segments are connected; a patch has one exterior ring).
        if (geometry instanceof GeometryCollection) {
            for (int i = 0; i < geometry.getNumGeometries(); i++) {
                populatePointCurveSurfaceToGeometry(geometry.getGeometryN(i), out, gmlIds);
            }
            return;
        }

        if (geometry.isEmpty()) {
            throw new IllegalArgumentException("Cannot encode an empty " + geometry.getGeometryType()
                    + " as S-124 geometry: the S-100 GML profile requires at least one position for a"
                    + " point, two for a curve segment and four for a linear ring");
        }

        if (geometry instanceof Point point) {
            PointProperty pointProperty = initPointProperty();
            pointProperty.getPoint().setId(gmlIds.get());
            pointProperty.getPoint().setPos(
                    generatePointPropertyPosition(coordinatesToGmlPosList(point.getCoordinates()).getValue()));
            out.add(pointProperty);
        } else if (geometry instanceof LineString line) {
            CurveProperty curveProperty = initialiseCurveProperty();
            curveProperty.getCurve().setId(gmlIds.get());
            curveProperty.getCurve().getSegments().getAbstractCurveSegments().add(
                    PROFILE_FACTORY.createLineStringSegment(
                            generateCurvePropertySegment(coordinatesToGmlPosList(line.getCoordinates()).getValue())));
            out.add(curveProperty);
        } else if (geometry instanceof Polygon polygon) {
            SurfaceProperty surfaceProperty = initialiseSurfaceProperty();
            surfaceProperty.getSurface().setId(gmlIds.get());
            surfaceProperty.getSurface().getPatches().getAbstractSurfacePatches().add(
                    PROFILE_FACTORY.createPolygonPatch(generateSurfacePropertyPatch(polygon)));
            out.add(surfaceProperty);
        } else {
            throw new UnsupportedOperationException("Don't know how to convert " + geometry.getGeometryType());
        }
    }

    private static PolygonPatchType generateSurfacePropertyPatch(Polygon polygon) {
        PolygonPatchType polygonPatchType = new PolygonPatchTypeImpl();
        polygonPatchType.setExterior(generateRingProperty(polygon.getExteriorRing(), false));
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            polygonPatchType.getInteriors().add(generateRingProperty(polygon.getInteriorRingN(i), true));
        }
        return polygonPatchType;
    }

    /**
     * S-100 Part 7 clause 7-4.3.2 (level 3a, mandated by S-124 clause 8.8) requires the outer
     * boundary of a surface clockwise (surface to the right of the curve) and inner boundaries
     * counter-clockwise. Orientation is evaluated on the JTS lon/lat axes, i.e. as drawn on a
     * north-up map; the lat/lon swap during serialisation does not change the traversal
     * direction of the boundary on the earth's surface.
     */
    private static AbstractRingPropertyType generateRingProperty(LinearRing ring, boolean counterClockwise) {
        Coordinate[] coordinates = ring.getCoordinates();
        if (Orientation.isCCW(coordinates) != counterClockwise) {
            coordinates = coordinates.clone();
            CoordinateArrays.reverse(coordinates);
        }
        Double[] coords = coordinatesToGmlPosList(coordinates).getValue();
        if (coords.length < 8) {
            throw new IllegalArgumentException("An S-100 linear ring requires at least four positions"
                    + " (S-100 GML profile), got " + coords.length / 2);
        }
        if (!coords[0].equals(coords[coords.length - 2]) || !coords[1].equals(coords[coords.length - 1])) {
            throw new IllegalArgumentException("An S-100 linear ring must be closed (first position"
                    + " equal to last position)");
        }
        AbstractRingPropertyType abstractRingPropertyType = new AbstractRingPropertyTypeImpl();
        LinearRingType linearRingType = new LinearRingTypeImpl();
        PosList posList = new PosListImpl();
        posList.setValue(coords);
        linearRingType.setPosList(posList);
        abstractRingPropertyType.setAbstractRing(PROFILE_FACTORY.createLinearRing(linearRingType));
        return abstractRingPropertyType;
    }

    private static LineStringSegmentType generateCurvePropertySegment(Double[] coords) {
        if (coords.length < 4) {
            throw new IllegalArgumentException("An S-100 curve segment requires at least two positions"
                    + " (S-100 GML profile), got " + coords.length / 2);
        }
        LineStringSegmentType lineStringSegmentType = new LineStringSegmentTypeImpl();
        PosList posList = new PosListImpl();
        posList.setValue(coords);
        lineStringSegmentType.setPosList(posList);
        return lineStringSegmentType;
    }

    private static Pos generatePointPropertyPosition(Double[] coords) {
        if (coords.length != 2) {
            throw new IllegalArgumentException("An S-100 point requires exactly one position"
                    + " (gml:pos), got " + coords.length / 2);
        }
        Pos pos = new PosImpl();
        pos.setValue(coords);
        return pos;
    }

    private static SurfaceProperty initialiseSurfaceProperty() {
        SurfaceProperty surfaceProperty = new SurfacePropertyImpl();
        SurfaceType surfaceType = new SurfaceTypeImpl();
        Patches patches = new PatchesImpl();
        surfaceType.setPatches(patches);
        surfaceProperty.setSurface(surfaceType);
        return surfaceProperty;
    }

    private static CurveProperty initialiseCurveProperty() {
        CurveProperty curveProperty = new CurvePropertyImpl();
        CurveType curveType = new CurveTypeImpl();
        Segments segments = new SegmentsImpl();
        curveType.setSegments(segments);
        curveProperty.setCurve(curveType);
        return curveProperty;
    }

    private static PointProperty initPointProperty() {
        PointProperty pointProperty = new PointPropertyImpl();
        PointType pointType = new PointTypeImpl();
        pointProperty.setPoint(pointType);
        return pointProperty;
    }

    private static Coordinate[] gmlPosListToCoordinates(PosList posList) {
        Double[] values = posList.getValue();
        if (values == null) {
            return new Coordinate[0];
        }
        List<Coordinate> result = new ArrayList<>();
        for (int i = 0; i + 1 < values.length; i = i + 2) {
            // GML order is lat,lon; JTS expects lon,lat.
            result.add(new Coordinate(values[i + 1], values[i]));
        }
        return result.toArray(new Coordinate[0]);
    }

    private static PosList coordinatesToGmlPosList(Coordinate[] coordinates) {
        // JTS stores (lon,lat); GML expects (lat,lon).
        List<Double> coords = Optional.ofNullable(coordinates)
                .map(Arrays::asList)
                .orElse(Collections.emptyList())
                .stream()
                .map(c -> Arrays.asList(c.getY(), c.getX()))
                .flatMap(List::stream)
                .toList();

        PosList posList = new PosListImpl();
        posList.setValue(coords.toArray(Double[]::new));
        return posList;
    }
}
