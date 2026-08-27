package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.math.BigInteger;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.PrecisionModel;

import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.CurveProperty;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.DatasetPurposeType;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.MDTopicCategoryCode;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.PointProperty;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.S100SpatialAttributeType;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.SurfaceProperty;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.DataSetIdentificationTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.BoundingShapeTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.EnvelopeTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.PosImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.ReferenceTypeImpl;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.Dataset;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnPart;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.ObjectFactory;

/**
 * End-to-end proof that converter-generated geometry marshals to schema-valid S-124:
 * a dataset holding a point, a curve and a surface with a hole must validate against
 * the bundled 124_2.0.0.xsd (which requires gml:id on every GML object).
 */
class S124GeometryXsdValidationTest {

    private static final GeometryFactory FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Test
    void converterGeometryMarshalsToSchemaValidDataset() throws Exception {
        Geometry geometries = FACTORY.createGeometryCollection(new Geometry[] {
                FACTORY.createPoint(new Coordinate(12.72, 55.5467)),
                FACTORY.createLineString(new Coordinate[] {
                        new Coordinate(12.0, 55.0), new Coordinate(12.5, 55.2) }),
                FACTORY.createPolygon(
                        FACTORY.createLinearRing(new Coordinate[] {
                                new Coordinate(8.0, 54.0), new Coordinate(14.0, 54.0),
                                new Coordinate(14.0, 58.0), new Coordinate(8.0, 58.0),
                                new Coordinate(8.0, 54.0) }),
                        new LinearRing[] { FACTORY.createLinearRing(new Coordinate[] {
                                new Coordinate(10.0, 55.0), new Coordinate(10.0, 56.0),
                                new Coordinate(11.0, 56.0), new Coordinate(11.0, 55.0),
                                new Coordinate(10.0, 55.0) }) }),
        });

        AtomicInteger sequence = new AtomicInteger();
        List<S100SpatialAttributeType> spatial = GeometryS124Converter
                .geometryToS124PointCurveSurfaceGeometry(geometries, () -> "DK.G." + sequence.incrementAndGet());

        // Validation off: the subject is whether converter geometry marshals to schema-valid XML,
        // and the fixture carries geometry without the NavwarnPreamble a real warning would have.
        String xml = S124Utils.marshalS124(dataset(spatial), true, false);

        assertThatCode(() -> S124XsdValidator.validate(xml))
                .as("marshalled dataset should be schema-valid, was:%n%s", xml)
                .doesNotThrowAnyException();
    }

    private static Dataset dataset(List<S100SpatialAttributeType> spatial) {
        ObjectFactory of = new ObjectFactory();
        Dataset dataset = of.createDataset();
        dataset.setId("DK.S124.test");

        DataSetIdentificationTypeImpl ident = new DataSetIdentificationTypeImpl();
        ident.setEncodingSpecification("S-100 Part 10b");
        ident.setEncodingSpecificationEdition("1.0");
        ident.setProductIdentifier("S-124");
        ident.setProductEdition("2.0.0");
        ident.setApplicationProfile(S124DatasetInfo.BASE_APPLICATION_PROFILE);
        ident.setDatasetFileIdentifier("DK.S124.test");
        ident.setDatasetTitle("Test");
        ident.setDatasetReferenceDate(LocalDate.of(2026, 1, 15));
        ident.setDatasetLanguage("eng");
        ident.setDatasetAbstract("Test");
        ident.getDatasetTopicCategories().add(MDTopicCategoryCode.OCEANS);
        ident.setDatasetPurpose(DatasetPurposeType.BASE);
        ident.setUpdateNumber(BigInteger.ZERO);
        dataset.setDatasetIdentificationInformation(ident);

        PosImpl lower = new PosImpl();
        lower.setValue(new Double[] { 54.0, 8.0 });
        PosImpl upper = new PosImpl();
        upper.setValue(new Double[] { 58.0, 14.0 });
        EnvelopeTypeImpl env = new EnvelopeTypeImpl();
        env.setSrsName("EPSG:4326");
        env.setLowerCorner(lower);
        env.setUpperCorner(upper);
        BoundingShapeTypeImpl bbox = new BoundingShapeTypeImpl();
        bbox.setEnvelope(env);
        dataset.setBoundedBy(bbox);

        NavwarnPart part = of.createNavwarnPart();
        part.setId("DK.NW.part.1");
        part.setWarningInformation(of.createWarningInformationType());
        ReferenceTypeImpl header = new ReferenceTypeImpl();
        header.setHref("#DK.NW.preamble.1");
        part.setHeader(header);
        for (S100SpatialAttributeType attribute : spatial) {
            NavwarnPart.Geometry geometry = of.createNavwarnPartGeometry();
            if (attribute instanceof PointProperty pointProperty) {
                geometry.setPointProperty(pointProperty);
            } else if (attribute instanceof CurveProperty curveProperty) {
                geometry.setCurveProperty(curveProperty);
            } else {
                geometry.setSurfaceProperty((SurfaceProperty) attribute);
            }
            part.getGeometries().add(geometry);
        }

        Dataset.Members members = of.createDatasetMembers();
        members.getNavwarnPartsAndNavwarnAreaAffectedsAndTextPlacements().add(part);
        dataset.setMembers(members);
        return dataset;
    }
}
