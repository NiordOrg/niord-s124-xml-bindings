package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.PointProperty;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.S100SpatialAttributeType;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.PointPropertyImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.PointTypeImpl;
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
}
