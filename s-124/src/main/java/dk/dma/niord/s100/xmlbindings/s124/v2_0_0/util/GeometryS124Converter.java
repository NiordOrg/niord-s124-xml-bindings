package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Lineal;
import org.locationtech.jts.geom.Polygonal;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.Puntal;

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
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.AbstractRingPropertyType;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.BoundingShapeType;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.EnvelopeType;
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
 */
public final class GeometryS124Converter {

    private static final ObjectFactory PROFILE_FACTORY = new ObjectFactory();

    private GeometryS124Converter() {
    }

    public static List<S100SpatialAttributeType> geometryToS124PointCurveSurfaceGeometry(Geometry geometry) {
        return populatePointCurveSurfaceToGeometry(geometry, new ArrayList<>());
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
                        .map(pos -> new Coordinate(pos.getValue()[0], pos.getValue()[1]))
                        .map(geometryFactory::createPoint)
                        .map(Geometry.class::cast)
                        .orElse(geometryFactory.createEmpty(0));
            } else if (pty instanceof CurveProperty) {
                return geometryFactory.createGeometryCollection(Optional.of(pty)
                        .map(CurveProperty.class::cast)
                        .map(CurveProperty::getCurve)
                        .map(CurveType::getSegments)
                        .map(Segments::getAbstractCurveSegments)
                        .orElse(Collections.emptyList())
                        .stream()
                        .map(JAXBElement::getValue)
                        .filter(LineStringSegmentType.class::isInstance)
                        .map(LineStringSegmentType.class::cast)
                        .map(LineStringSegmentType::getPosList)
                        .map(GeometryS124Converter::gmlPosListToCoordinates)
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
                        .map(PolygonPatchType::getExterior)
                        .map(AbstractRingPropertyType::getAbstractRing)
                        .map(JAXBElement::getValue)
                        .filter(LinearRingType.class::isInstance)
                        .map(LinearRingType.class::cast)
                        .map(LinearRingType::getPosList)
                        .map(GeometryS124Converter::gmlPosListToCoordinates)
                        .map(coords -> coords.length == 1
                                ? geometryFactory.createPoint(coords[0])
                                : geometryFactory.createPolygon(coords))
                        .toList()
                        .toArray(Geometry[]::new));
            } else {
                throw new UnsupportedOperationException("Don't know how to convert " + pty);
            }
        }).reduce(geometryFactory.createEmpty(-1), (un, el) -> un == null || un.isEmpty() ? el : un.union(el));
    }

    private static List<S100SpatialAttributeType> populatePointCurveSurfaceToGeometry(
            Geometry geometry, List<S100SpatialAttributeType> out) {
        List<S100SpatialAttributeType> result = out == null ? new ArrayList<>() : out;
        if (geometry == null) {
            return result;
        }

        if (geometry instanceof Puntal) {
            PointProperty pointProperty = initPointProperty();
            pointProperty.getPoint().setPos(
                    generatePointPropertyPosition(coordinatesToGmlPosList(geometry.getCoordinates()).getValue()));
            result.add(pointProperty);
        } else if (geometry instanceof Lineal) {
            CurveProperty curveProperty = initialiseCurveProperty();
            curveProperty.getCurve().getSegments().getAbstractCurveSegments().add(
                    PROFILE_FACTORY.createLineStringSegment(
                            generateCurvePropertySegment(coordinatesToGmlPosList(geometry.getCoordinates()).getValue())));
            result.add(curveProperty);
        } else if (geometry instanceof Polygonal) {
            SurfaceProperty surfaceProperty = initialiseSurfaceProperty();
            surfaceProperty.getSurface().getPatches().getAbstractSurfacePatches().add(
                    PROFILE_FACTORY.createPolygonPatch(
                            generateSurfacePropertyPatch(coordinatesToGmlPosList(geometry.getCoordinates()).getValue())));
            result.add(surfaceProperty);
        } else if (geometry instanceof GeometryCollection && geometry.getNumGeometries() > 0) {
            for (int i = 0; i < geometry.getNumGeometries(); i++) {
                populatePointCurveSurfaceToGeometry(geometry.getGeometryN(i), result);
            }
        }

        return result;
    }

    private static PolygonPatchType generateSurfacePropertyPatch(Double[] coords) {
        PolygonPatchType polygonPatchType = new PolygonPatchTypeImpl();
        AbstractRingPropertyType abstractRingPropertyType = new AbstractRingPropertyTypeImpl();
        LinearRingType linearRingType = new LinearRingTypeImpl();
        PosList posList = new PosListImpl();
        posList.setValue(coords);
        linearRingType.setPosList(posList);
        abstractRingPropertyType.setAbstractRing(PROFILE_FACTORY.createLinearRing(linearRingType));
        polygonPatchType.setExterior(abstractRingPropertyType);
        return polygonPatchType;
    }

    private static LineStringSegmentType generateCurvePropertySegment(Double[] coords) {
        LineStringSegmentType lineStringSegmentType = new LineStringSegmentTypeImpl();
        PosList posList = new PosListImpl();
        posList.setValue(coords);
        lineStringSegmentType.setPosList(posList);
        return lineStringSegmentType;
    }

    private static Pos generatePointPropertyPosition(Double[] coords) {
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
        List<Coordinate> result = new ArrayList<>();
        Double[] values = posList.getValue();
        for (int i = 0; i < values.length; i = i + 2) {
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
