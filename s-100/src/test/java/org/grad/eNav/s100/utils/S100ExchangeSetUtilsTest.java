/*
 * Copyright (c) 2024 GLA Research and Development Directorate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grad.eNav.s100.utils;

import dk.dma.niord.s100.catalog._5_2.*;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import net.opengis.gml._3.*;
import org.grad.eNav.s100.enums.MaintenanceFrequency;
import org.grad.eNav.s100.enums.RoleCode;
import org.grad.eNav.s100.enums.SecurityClassification;
import org.grad.eNav.s100.enums.TelephoneType;
import org.iso.standards.iso._19115.__3.gco._1.CharacterStringPropertyType;
import org.iso.standards.iso._19115.__3.gco._1.CodeListValueType;
import org.iso.standards.iso._19115.__3.gco._1.DecimalPropertyType;
import org.iso.standards.iso._19115.__3.lan._1.PTLocaleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class S100ExchangeSetUtilsTest {

    // Test Variables
    private S100ExchangeCatalogueBuilder s100ExchangeCatalogueBuilder;
    private S100DatasetDiscoveryMetadataBuilder s100DatasetDiscoveryMetadataBuilder;
    private S100ExchangeCatalogue s100ExchangeCatalogue;
    private String s100ExchangeSetXml;
    private Geometry geometry;

    // Fixed Variables
    private String isoType = "ISO 19103:2015";
    private DateTimeFormatter timeFormat = DateTimeFormatter.ISO_TIME;
    private DateTimeFormatter dateFormat = DateTimeFormatter.ISO_DATE;
    private DateTimeFormatter dateTimeFormat = DateTimeFormatter.ISO_DATE_TIME;

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setup() throws IOException, CertificateException, JAXBException {
        // Create a geometry first
        final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        this.geometry = geometryFactory.createPolygon(new Coordinate[]{
                new Coordinate(-10, -10),
                new Coordinate(-11, 10),
                new Coordinate(10, 11),
                new Coordinate(11, -11),
                new Coordinate(-10, -10),
        });

        // Create an exchange set catalogue builder
        this.s100ExchangeCatalogueBuilder = new S100ExchangeCatalogueBuilder((id, algorithm, payload) -> {
            S100SEDigitalSignature s100SEDigitalSignature = new S100SEDigitalSignature();
            s100SEDigitalSignature.setId("sig");
            s100SEDigitalSignature.setCertificateRef("ref");
            s100SEDigitalSignature.setValue("signature".getBytes());
            return s100SEDigitalSignature;
        });

        // Create the medatata file builders
        this.s100DatasetDiscoveryMetadataBuilder = new S100DatasetDiscoveryMetadataBuilder((id, algorithm, payload) -> {
            S100SEDigitalSignature s100SEDigitalSignature = new S100SEDigitalSignature();
            s100SEDigitalSignature.setId("sig");
            s100SEDigitalSignature.setCertificateRef("ref");
            s100SEDigitalSignature.setValue("signature".getBytes());
            return s100SEDigitalSignature;
        });

        //====================================================================//
        //              Load a test Exchange Set Catalogue XML                //
        //====================================================================//
        final InputStream inES = ClassLoader.getSystemResourceAsStream("s100-exchange-set.xml");
        assertNotNull(inES);
        this.s100ExchangeSetXml = new String(inES.readAllBytes(), StandardCharsets.UTF_8);
        //====================================================================//

        //====================================================================//
        //                   Load a test X.509 Certificate                    //
        //====================================================================//
        final InputStream inCert = ClassLoader.getSystemResourceAsStream("test.pem");
        assertNotNull(inCert);
        final String inCertPem = new String(inCert.readAllBytes(), StandardCharsets.UTF_8)
                .replaceAll("-----BEGIN CERTIFICATE-----","")
                .replaceAll("-----END CERTIFICATE-----","")
                .replaceAll(System.lineSeparator(),"");
        //====================================================================//

        //====================================================================//
        //         The S100 Exchange Catalogue product Specification          //
        //====================================================================//
        final S100ProductSpecification s100ProductSpecification = new S100ProductSpecification();
        s100ProductSpecification.setName("S-125");
        s100ProductSpecification.setProductIdentifier("S-125");
        s100ProductSpecification.setNumber(BigInteger.ONE);
        s100ProductSpecification.setDate(LocalDate.parse("2022-10-22", this.dateFormat));
        s100ProductSpecification.setCompliancyCategory(S100CompliancyCategory.CATEGORY_1);
        //====================================================================//

        // Initialise the Dataset
        this.s100ExchangeCatalogue = this.s100ExchangeCatalogueBuilder
                .setIdentifier("Test Exchange Set")
                .setDateTime(LocalDateTime.parse("2023-01-01T00:00:00.000", this.dateTimeFormat))
                .setDataServerIdentifier("2d7c8116-75a9-4fb8-b1b3-7a698d416b97")
                .setOrganization("GRAD")
                .setElectronicMailAddresses(Collections.singletonList("test@gla-rad.org"))
                .setPhone("+44 1255 245000")
                .setPhoneType(TelephoneType.VOICE)
                .setCity("Harwich")
                .setPostalCode("postalCode")
                .setCountry("UK and Ireland")
                .setLocales(Collections.singletonList(Locale.UK))
                .setAdministrativeArea("England, Wales, Scotland and the whole of Ireland")
                .setDescription("Test Exchange Set Description.")
                .setComment("Test Exchange Set Comment.")
                .setProductSpecification(Collections.singletonList(s100ProductSpecification))
                .setCertificatesByPem(Collections.singletonMap("CRT1", inCertPem))
                .addDatasetMetadata(builder -> builder
                        .setFileName("file:/dataset.XML")
                        .setDatasetID("urn:mrn:gla:grad:s125:datasets:XXXX")
                        .setDescription("description")
                        .setCompressionFlag(false)
                        .setDataProtection(false)
                        .setProtectionScheme(S100ProtectionScheme.S_100_P_15)
                        .setCopyright(true)
                        .setClassification(SecurityClassification.UNCLASSIFIED)
                        .setPurpose(S100Purpose.NEW_DATASET)
                        .setNotForNavigation(true)
                        .setSpecificUsage("testing")
                        .setEditionNumber(BigInteger.ONE)
                        .setUpdateNumber(BigInteger.ZERO)
                        .setUpdateApplicationDate(LocalDate.parse("2023-01-01", this.dateFormat))
                        .setIssueDate(LocalDate.parse("2023-01-02", this.dateFormat))
                        .setIssueTime(LocalTime.parse("00:00:00", this.timeFormat))
                        // the dataset discovery metadata product specification is
                        // mandatory (minOccurs=1) in the exchange catalogue schema
                        .setProductSpecification(s100ProductSpecification)
                        .setProducingAgency("producingAgency")
                        .setProducingAgencyRole(RoleCode.ORIGINATOR)
                        // the producing agency contact information is mandatory
                        // (S-100 Part 17, Table 17-3)
                        .setProducingAgencyPhone("+44 1255 245000")
                        .setProducingAgencyElectronicMailAddresses(Collections.singletonList("test@gla-rad.org"))
                        .setProducerCode("producerCode")
                        .setEncodingFormat(S100EncodingFormat.GML)
                        .setDataCoverages(this.geometry)
                        .setComment("comment")
                        .setMetadataDateStamp(LocalDate.parse("2023-01-03", this.dateFormat))
                        .setReplacedData(false)
                        .setNavigationPurposes(Collections.singletonList(S100NavigationPurpose.OVERVIEW))
                        // S-100 allows only the asNeeded and irregular
                        // frequencies, and exactly one of the maintenance date
                        // and the maintenance period
                        .setMaintenanceFrequency(MaintenanceFrequency.AS_NEEDED)
                        .setMaintenanceDate(LocalDate.parse("2023-01-04", this.dateFormat))
                        .build("dataset".getBytes()))
                .build();
    }

    /**
     * Test that we can successfully create a character string from a simple
     * Java string.
     */
    @Test
    void testCreateCharacterString() {
        final CharacterStringPropertyType cspt = S100ExchangeSetUtils.createCharacterStringPropertyType("test");

        // Assert that the CharacterStringPropertyType is not empty and seems valid
        assertNotNull(cspt);
        assertNotNull(cspt.getCharacterString());
        assertEquals("test", cspt.getCharacterString().getValue());
    }

    /**
     * Test that for a null string the CharacterStringPropertyType generation
     * method will return null, so that the enclosing optional element is
     * omitted instead of being marshalled as a nil (schema-invalid)
     * gco:CharacterString.
     */
    @Test
    void testCreateCharacterStringNull() {
        final CharacterStringPropertyType cspt = S100ExchangeSetUtils.createCharacterStringPropertyType(null);

        // Assert that no CharacterStringPropertyType was generated
        assertNull(cspt);
    }

    /**
     * Test that we can successfully create a list of character strings from a
     * simple list of Java strings.
     */
    @Test
    void testCreateCharacterStringList() {
        final List<CharacterStringPropertyType> cspts = S100ExchangeSetUtils.createCharacterStringPropertyTypeList(Collections.singletonList("test"));

        // Assert that the CharacterStringPropertyType is not empty and seems valid
        assertNotNull(cspts);
        assertNotNull(cspts.get(0));
        assertEquals(1, cspts.size());
        assertNotNull(cspts.get(0).getCharacterString());
        assertEquals("test", cspts.get(0).getCharacterString().getValue());
    }

    /**
     * The that for a null list, the CharacterStringPropertyType list generation
     * method will return an empty list respectively.
     */
    @Test
    void testCreateCharacterStringListNull() {
        final List<CharacterStringPropertyType> cspts = S100ExchangeSetUtils.createCharacterStringPropertyTypeList(null);

        // Assert that the CharacterStringPropertyType is not empty and seems valid
        assertNotNull(cspts);
        assertEquals(0, cspts.size());
    }

    /**
     * Test that for a valid decimal input, the DecimalPropertyType generation
     * method will return a valid and populated S-100 object.
     */
    @Test
    void testCreateDecimalPropertyType() {
        final DecimalPropertyType decimalPropertyType = S100ExchangeSetUtils.createDecimalPropertyType(BigDecimal.ONE);

        // Assert that the DecimalPropertyType is not empty and seems valid
        assertNotNull(decimalPropertyType);
        assertEquals(BigDecimal.ONE, decimalPropertyType.getDecimal());
    }

    /**
     * Test that for a null decimal input, the DecimalPropertyType generation
     * method will return a valid but empty S-100 object.
     */
    @Test
    void testCreateDecimalPropertyTypeNull() {
        final DecimalPropertyType decimalPropertyType = S100ExchangeSetUtils.createDecimalPropertyType(null);

        // Assert that the DecimalPropertyType is not empty and seems valid
        assertNotNull(decimalPropertyType);
        assertNull(decimalPropertyType.getDecimal());
    }

    /**
     * Test that we can successfully generate the S-100 Code List Value type
     * objects provided that we pass the correct parameters.
     */
    @Test
    void testCreateCodeListValueType() {
        // Create the S-100 Code List Value Type object
        final CodeListValueType codeListValueType = S100ExchangeSetUtils.createCodeListValueType(
                "list",
                "space",
                "code",
                "value"
        );

        // Assess the result
        assertNotNull(codeListValueType);
        assertEquals("list", codeListValueType.getCodeList());
        assertEquals("space", codeListValueType.getCodeSpace());
        assertEquals("code", codeListValueType.getCodeListValue());
        assertEquals("value", codeListValueType.getValue());
    }

    /**
     * Test that provided a valid Java local we can successfully generate an
     * S-100 PTLocaleType object.
     */
    @Test
    void testCreatePTLocaleType() {
        // Generate the S-100 PT Locale Type
        final PTLocaleType ptLocaleType = S100ExchangeSetUtils.createPTLocaleType(Locale.UK);

        // Assess the result
        assertNotNull(ptLocaleType);
        assertNotNull(ptLocaleType.getLanguage());
        assertNotNull(ptLocaleType.getLanguage().getLanguageCode());
        assertEquals(Locale.UK.getDisplayLanguage(), ptLocaleType.getLanguage().getLanguageCode().getValue());
        assertEquals(Locale.UK.getISO3Language(), ptLocaleType.getLanguage().getLanguageCode().getCodeListValue());
        assertNotNull(ptLocaleType.getCountry());
        assertNotNull(ptLocaleType.getCountry().getCountryCode());
        assertEquals(Locale.UK.getDisplayCountry(), ptLocaleType.getCountry().getCountryCode().getValue());
        // S-100 Part 17 PT_Locale requires the ISO 3166-1 2-letter country code
        assertEquals("GB", ptLocaleType.getCountry().getCountryCode().getCodeListValue());
        assertNotNull(ptLocaleType.getCharacterEncoding());
        assertNotNull(ptLocaleType.getCharacterEncoding().getMDCharacterSetCode());
        assertEquals(StandardCharsets.UTF_8.displayName(), ptLocaleType.getCharacterEncoding().getMDCharacterSetCode().getValue());
    }

    /**
     * Test that for a Java Locale without a country, the optional PT_Locale
     * country element is omitted, rather than being populated with an empty
     * country code.
     */
    @Test
    void testCreatePTLocaleTypeWithoutCountry() {
        // Generate the S-100 PT Locale Type
        final PTLocaleType ptLocaleType = S100ExchangeSetUtils.createPTLocaleType(Locale.ENGLISH);

        // Assess the result
        assertNotNull(ptLocaleType);
        assertNotNull(ptLocaleType.getLanguage());
        assertNull(ptLocaleType.getCountry());
    }

    /**
     * Test that provided a null Java Locale this the S-100 Utils function will
     * return a null output.
     */
    @Test
    void testCreatePTLocaleTypeNull() {
        // Generate the S-100 PT Locale Type
        PTLocaleType ptLocaleType = S100ExchangeSetUtils.createPTLocaleType(null);

        assertNull(ptLocaleType);
    }

    /**
     * Test that provided a valid geometry, the S100GeographicBoundingBoxType
     * generation method will generate and populate the respective S-100
     * boundary object correctly.
     * <p/>
     * Note that for this test the geometry is not a perfect square, so that
     * we make sure we can calculate correctly the bounding boxes for generic
     * shapes.
     */
    @Test
    void testCreateS100GeographicBoundingBoxType() {
        // Generate the S-100 bounding box
        S100GeographicBoundingBoxType s100GeographicBoundingBoxType = S100ExchangeSetUtils.createS100GeographicBoundingBoxType(this.geometry);

        // Assert that the bounding box is not empty and seems valid
        assertNotNull(s100GeographicBoundingBoxType);
        assertNotNull(s100GeographicBoundingBoxType.getWestBoundLongitude());
        assertNotNull(s100GeographicBoundingBoxType.getEastBoundLongitude());
        assertNotNull(s100GeographicBoundingBoxType.getSouthBoundLatitude());
        assertNotNull(s100GeographicBoundingBoxType.getNorthBoundLatitude());
        assertNotNull(s100GeographicBoundingBoxType.getWestBoundLongitude().getDecimal());
        assertNotNull(s100GeographicBoundingBoxType.getEastBoundLongitude().getDecimal());
        assertNotNull(s100GeographicBoundingBoxType.getSouthBoundLatitude().getDecimal());
        assertNotNull(s100GeographicBoundingBoxType.getNorthBoundLatitude().getDecimal());
        assertEquals(-11.0, s100GeographicBoundingBoxType.getWestBoundLongitude().getDecimal().doubleValue());
        assertEquals(11.0, s100GeographicBoundingBoxType.getEastBoundLongitude().getDecimal().doubleValue());
        assertEquals(-11.0, s100GeographicBoundingBoxType.getSouthBoundLatitude().getDecimal().doubleValue());
        assertEquals(11.0,s100GeographicBoundingBoxType.getNorthBoundLatitude().getDecimal().doubleValue());
    }

    /**
     * Test that for a null geometry input, the S100GeographicBoundingBoxType
     * generation method will return a simple null output.
     */
    @Test
    void testCreateS100GeographicBoundingBoxTypeNull() {
        assertNull(S100ExchangeSetUtils.createS100GeographicBoundingBoxType(null));
    }

    /**
     * Test that we can successfully generate a geographical data coverage
     * description in S-100 if we provide a valid geometry.
     * <p/>
     * S-100 Part 17, S100_DataCoverage NOTE 1 requires a single GML polygon
     * with an identifier, whose exterior is a linear ring of a closed sequence
     * of at least 4 EPSG:4326 (latitude, longitude) positions.
     */
    @Test
    void testCreateS100DataCoverages() {
        // Generate the data coverage
        List<S100DataCoverage> dataCoverage = S100ExchangeSetUtils.createS100DataCoverages(this.geometry);

        // Assert that the data coverage is not empty and seems valid
        assertNotNull(dataCoverage);
        assertEquals(1, dataCoverage.size());
        assertNotNull(dataCoverage.get(0));
        assertNotNull(dataCoverage.get(0).getBoundingPolygon());
        assertNotNull(dataCoverage.get(0).getBoundingPolygon().getPolygons());
        assertEquals(1, dataCoverage.get(0).getBoundingPolygon().getPolygons().size());
        assertNotNull(dataCoverage.get(0).getBoundingPolygon().getPolygons().get(0));
        assertNotNull(dataCoverage.get(0).getBoundingPolygon().getPolygons().get(0).getAbstractGeometry());
        assertNotNull(dataCoverage.get(0).getBoundingPolygon().getPolygons().get(0).getAbstractGeometry().getValue());
        assertTrue(dataCoverage.get(0).getBoundingPolygon().getPolygons().get(0).getAbstractGeometry().getValue() instanceof PolygonType);

        // Now investigate the polygon itself
        PolygonType polygonType = (PolygonType) dataCoverage.get(0).getBoundingPolygon().getPolygons().get(0).getAbstractGeometry().getValue();
        assertNotNull(polygonType.getId());
        assertTrue(polygonType.getId().startsWith(S100ExchangeSetUtils.BOUNDING_POLYGON_ID_PREFIX));
        // NOTE 1 also fixes the SRS of the bounding polygon to EPSG:4326
        assertEquals(S100ExchangeSetUtils.BOUNDING_POLYGON_SRS_NAME, polygonType.getSrsName());
        assertEquals(BigInteger.TWO, polygonType.getSrsDimension());
        assertNotNull(polygonType.getExterior());
        assertNotNull(polygonType.getExterior().getAbstractRing());
        assertNotNull(polygonType.getExterior().getAbstractRing().getValue());
        assertTrue(polygonType.getExterior().getAbstractRing().getValue() instanceof LinearRingType);
        assertTrue(polygonType.getInteriors().isEmpty());

        // Then investigate the exterior linear ring, which should contain all
        // the geometry coordinates in a single latitude/longitude position list
        LinearRingType linearRingType = (LinearRingType) polygonType.getExterior().getAbstractRing().getValue();
        assertNotNull(linearRingType.getPosList());
        assertNotNull(linearRingType.getPosList().getValue());
        assertEquals(2 * this.geometry.getCoordinates().length, linearRingType.getPosList().getValue().length);
        for(int i=0; i < this.geometry.getCoordinates().length; i++) {
            assertEquals(this.geometry.getCoordinates()[i].getY(), linearRingType.getPosList().getValue()[2*i]);
            assertEquals(this.geometry.getCoordinates()[i].getX(), linearRingType.getPosList().getValue()[2*i + 1]);
        }
    }

    /**
     * Test that the holes of a polygon are encoded as separate interior linear
     * rings and are never fused into the exterior one.
     */
    @Test
    void testCreateS100DataCoveragesWithInteriorRings() {
        // Create a polygon with a hole in it
        final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        final LinearRing shell = geometryFactory.createLinearRing(new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(10, 0),
                new Coordinate(10, 10),
                new Coordinate(0, 10),
                new Coordinate(0, 0),
        });
        final LinearRing hole = geometryFactory.createLinearRing(new Coordinate[]{
                new Coordinate(2, 2),
                new Coordinate(4, 2),
                new Coordinate(4, 4),
                new Coordinate(2, 4),
                new Coordinate(2, 2),
        });
        final Polygon polygon = geometryFactory.createPolygon(shell, new LinearRing[]{hole});

        // Generate the data coverage
        final List<S100DataCoverage> dataCoverage = S100ExchangeSetUtils.createS100DataCoverages(polygon);

        // Assess the result
        assertNotNull(dataCoverage);
        assertEquals(1, dataCoverage.size());
        final PolygonType polygonType = (PolygonType) dataCoverage.get(0).getBoundingPolygon().getPolygons().get(0).getAbstractGeometry().getValue();

        // The exterior should only contain the shell coordinates
        final LinearRingType exterior = (LinearRingType) polygonType.getExterior().getAbstractRing().getValue();
        assertArrayEquals(new Double[]{0.0, 0.0, 0.0, 10.0, 10.0, 10.0, 10.0, 0.0, 0.0, 0.0},
                exterior.getPosList().getValue());

        // And the hole should be provided as a separate interior linear ring
        assertEquals(1, polygonType.getInteriors().size());
        assertTrue(polygonType.getInteriors().get(0).getAbstractRing().getValue() instanceof LinearRingType);
        final LinearRingType interior = (LinearRingType) polygonType.getInteriors().get(0).getAbstractRing().getValue();
        assertArrayEquals(new Double[]{2.0, 2.0, 2.0, 4.0, 4.0, 4.0, 4.0, 2.0, 2.0, 2.0},
                interior.getPosList().getValue());
    }

    /**
     * Test that non-polygonal geometries, which cannot provide a closed
     * sequence of at least 4 positions on their own, are described by their
     * envelope.
     */
    @Test
    void testCreateS100DataCoveragesForNonPolygons() {
        // Create a simple line string
        final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        final LineString lineString = geometryFactory.createLineString(new Coordinate[]{
                new Coordinate(0, 0),
                new Coordinate(10, 20),
        });

        // Generate the data coverage
        final List<S100DataCoverage> dataCoverage = S100ExchangeSetUtils.createS100DataCoverages(lineString);

        // Assess the result - the envelope rectangle in latitude/longitude order
        assertNotNull(dataCoverage);
        assertEquals(1, dataCoverage.size());
        final PolygonType polygonType = (PolygonType) dataCoverage.get(0).getBoundingPolygon().getPolygons().get(0).getAbstractGeometry().getValue();
        final LinearRingType exterior = (LinearRingType) polygonType.getExterior().getAbstractRing().getValue();
        assertArrayEquals(new Double[]{0.0, 0.0, 0.0, 10.0, 20.0, 10.0, 20.0, 0.0, 0.0, 0.0},
                exterior.getPosList().getValue());
    }

    /**
     * Test that each component of a geometry collection generates its own data
     * coverage entry, with its own unique GML polygon identifier.
     */
    @Test
    void testCreateS100DataCoveragesForCollections() {
        // Create a multi-polygon out of two copies of the test geometry
        final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        final MultiPolygon multiPolygon = geometryFactory.createMultiPolygon(new Polygon[]{
                (Polygon) this.geometry,
                (Polygon) this.geometry.copy()
        });

        // Generate the data coverage
        final List<S100DataCoverage> dataCoverage = S100ExchangeSetUtils.createS100DataCoverages(multiPolygon);

        // Assess the result
        assertNotNull(dataCoverage);
        assertEquals(2, dataCoverage.size());
        final PolygonType first = (PolygonType) dataCoverage.get(0).getBoundingPolygon().getPolygons().get(0).getAbstractGeometry().getValue();
        final PolygonType second = (PolygonType) dataCoverage.get(1).getBoundingPolygon().getPolygons().get(0).getAbstractGeometry().getValue();
        assertNotNull(first.getId());
        assertNotNull(second.getId());
        assertNotEquals(first.getId(), second.getId());
    }

    /**
     * Test that geometries which cannot provide a conformant bounding polygon
     * are rejected, instead of generating a non-conformant ring.
     */
    @Test
    void testCreateS100DataCoveragesForEmptyGeometries() {
        final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);

        // An empty polygon cannot provide the required exterior ring
        assertThrows(IllegalArgumentException.class, () ->
                S100ExchangeSetUtils.createS100DataCoverages(geometryFactory.createPolygon()));

        // And neither can an empty geometry provide an envelope
        assertThrows(IllegalArgumentException.class, () ->
                S100ExchangeSetUtils.createS100DataCoverages(geometryFactory.createPoint()));
    }

    /**
     * Test that for a null geometry input, the S100DataCoverage list
     * generation method will return a simple null output.
     */
    @Test
    void testCreateS100DataCoveragesNull() {
        assertNull(S100ExchangeSetUtils.createS100DataCoverages(null));
    }

    /**
     * Test that we can create (marshall) and XML based on an S-125 Dataset type
     * object.
     *
     * @throws JAXBException a JAXB exception thrown during the marshalling operation
     */
    @Test
    void testMarchallS125() throws JAXBException {
        Locale l = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            String xml = S100ExchangeSetUtils.marshalS100ExchangeSetCatalogue(this.s100ExchangeCatalogue);

            // Assert the XML is not empty and seems valid
            assertNotNull(xml);

            // The marshalling operation ofter messes up the namespace order so
            // we might as well remove it before we continue with the one-to-one
            // matching. The bounding polygon GML identifiers are also unique
            // per JVM, so they are normalised as well.
            String s100ExchangeSetXmlWithoutNamespaces = this.s100ExchangeSetXml
                    .replaceAll("S100_ExchangeCatalogue .+>","S100_ExchangeCatalogue>")
                    .replaceAll("ns\\d+:","")
                    .replaceAll("id=\"BP\\.\\d+\"","id=\"BP\"");
            String xmlWithoutNamespaces = xml
                    .replaceAll("S100_ExchangeCatalogue .+>","S100_ExchangeCatalogue>")
                    .replaceAll("ns\\d+:","")
                    .replaceAll("id=\"BP\\.\\d+\"","id=\"BP\"");
            assertEquals(s100ExchangeSetXmlWithoutNamespaces, xmlWithoutNamespaces);
        } finally {
            Locale.setDefault(l);
        }
    }

    /**
     * Test that the marshalled exchange set catalogue validates against the
     * S-100 5.2.0 exchange catalogue schema. The schema is loaded from the
     * filesystem so that its relative ISO 19115-3 imports resolve naturally;
     * the GML/xlink imports resolve over the network, exactly as the xjc code
     * generation for this module already does.
     */
    @Test
    void testMarshalledCatalogueIsSchemaValid() throws Exception {
        final String xml = S100ExchangeSetUtils.marshalS100ExchangeSetCatalogue(this.s100ExchangeCatalogue);

        java.nio.file.Path schemaPath = java.nio.file.Path.of("src/main/resources/xsd/S100Catalog/20240415/S100_ExchangeCatalogue.xsd");
        if (!java.nio.file.Files.exists(schemaPath)) {
            // running with the repository root as working directory (e.g. from an IDE)
            schemaPath = java.nio.file.Path.of("s-100/src/main/resources/xsd/S100Catalog/20240415/S100_ExchangeCatalogue.xsd");
        }
        assertTrue(java.nio.file.Files.exists(schemaPath), "exchange catalogue schema must be present");

        final javax.xml.validation.Validator validator = javax.xml.validation.SchemaFactory
                .newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI)
                .newSchema(schemaPath.toFile())
                .newValidator();
        final List<String> errors = new java.util.ArrayList<>();
        validator.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
            @Override
            public void error(org.xml.sax.SAXParseException e) {
                errors.add("line " + e.getLineNumber() + ": " + e.getMessage());
            }

            @Override
            public void fatalError(org.xml.sax.SAXParseException e) {
                errors.add("fatal, line " + e.getLineNumber() + ": " + e.getMessage());
            }
        });
        validator.validate(new javax.xml.transform.stream.StreamSource(new java.io.StringReader(xml)));

        assertEquals(Collections.emptyList(), errors);
    }

    /**
     * Test that we can generate (unmarshall) an S-125 POJO based on a valid
     * XML S-125 Dataset.
     *
     * @throws JAXBException a JAXB exception thrown during the unmarshalling operation
     */
    @Test
    void testUnmarshalS125() throws JAXBException {
        // Unmarshall it to a G1128 service instance object
        S100ExchangeCatalogue result = S100ExchangeSetUtils.unmarshallS100ExchangeSetCatalogue(this.s100ExchangeSetXml);

        // Assert all information is correct
        assertNotNull(result);
        assertEquals(this.s100ExchangeCatalogue.getDataServerIdentifier(), result.getDataServerIdentifier());
        assertNotNull( result.getExchangeCatalogueDescription());
        assertNotNull( result.getExchangeCatalogueDescription().getCharacterString());
        assertEquals(this.s100ExchangeCatalogue.getExchangeCatalogueDescription().getCharacterString().getValue(), result.getExchangeCatalogueDescription().getCharacterString().getValue());
        assertNotNull( result.getExchangeCatalogueComment());
        assertNotNull( result.getExchangeCatalogueComment().getCharacterString());
        assertEquals(this.s100ExchangeCatalogue.getExchangeCatalogueComment().getCharacterString().getValue(), result.getExchangeCatalogueComment().getCharacterString().getValue());
        assertNotNull(result.getDatasetDiscoveryMetadata());
        assertNotNull(result.getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas());
        assertEquals(1, result.getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas().size());

        // Now verify the dataset discovery metadata
        final S100DatasetDiscoveryMetadata metadata = result.getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas().get(0);
        assertEquals(this.s100ExchangeCatalogue.getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas().get(0).getFileName(), metadata.getFileName());
        assertEquals(this.s100ExchangeCatalogue.getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas().get(0).getDatasetID(), metadata.getDatasetID());
        assertNotNull(metadata.getDescription().getCharacterString());
        assertNotNull(metadata.getClassification());
        assertEquals(this.s100ExchangeCatalogue.getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas().get(0).getPurpose(), metadata.getPurpose());
        assertTrue(metadata.isNotForNavigation());
        assertEquals(this.s100ExchangeCatalogue.getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas().get(0).getEditionNumber(), metadata.getEditionNumber());
        assertEquals(this.s100ExchangeCatalogue.getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas().get(0).getUpdateNumber(), metadata.getUpdateNumber());
        assertEquals(this.s100ExchangeCatalogue.getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas().get(0).getUpdateApplicationDate(), metadata.getUpdateApplicationDate());
        assertEquals(this.s100ExchangeCatalogue.getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas().get(0).getIssueDate(), metadata.getIssueDate());
        assertNull(metadata.getBoundingBox());
        assertNull(metadata.getTemporalExtent());
        assertNotNull(metadata.getProducingAgency());
        assertEquals(this.s100ExchangeCatalogue.getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas().get(0).getProducerCode(), metadata.getProducerCode());
        assertEquals(this.s100ExchangeCatalogue.getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas().get(0).getMetadataDateStamp(), metadata.getMetadataDateStamp());
        assertEquals(this.s100ExchangeCatalogue.getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas().get(0).isReplacedData(), metadata.isReplacedData());
    }

    /**
     * Test the translation operations between the certificate and the
     * respective PEM representation performed by the utility.
     *
     * @throws IOException for IO Exceptions
     * @throws CertificateException for issues with the certificate loading
     */
    @Test
    void testCertPemOperations() throws IOException, CertificateException {
        final InputStream in = ClassLoader.getSystemResourceAsStream("test.pem");
        assertNotNull(in);
        final String inString = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                .replaceAll("-----BEGIN CERTIFICATE-----","")
                .replaceAll("-----END CERTIFICATE-----","")
                .replaceAll("\\s","");

        // Perform the translations
        X509Certificate certificate = S100ExchangeSetUtils.getCertFromPem(inString);
        byte[] pem = S100ExchangeSetUtils.getPemFromCert(certificate);

        // Make sure the translation operations worked correctly
        assertNotNull(certificate);
        assertNotNull(pem);
        // The PEM representation is the textual one, i.e. the ASCII characters
        // of the certificate Base64 encoded once, without the header and footer
        // lines
        assertEquals(inString, new String(pem, StandardCharsets.UTF_8));
    }

    /**
     * Test that the DER translation returns the certificate content that the
     * xs:base64Binary-typed certificate elements of the S-100 protection scheme
     * expect - S-100 Part 15, clauses 15-8.6 and 15-8.11.1 require a single
     * Base64 decode of such an element to yield the X.509 certificate, so the
     * value handed to JAXB must not already be Base64 text.
     *
     * @throws IOException for IO Exceptions
     * @throws CertificateException for issues with the certificate loading
     */
    @Test
    void testCertDerOperations() throws IOException, CertificateException {
        final InputStream in = ClassLoader.getSystemResourceAsStream("test.pem");
        assertNotNull(in);
        final String inString = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                .replaceAll("-----BEGIN CERTIFICATE-----","")
                .replaceAll("-----END CERTIFICATE-----","")
                .replaceAll("\\s","");
        final X509Certificate certificate = S100ExchangeSetUtils.getCertFromPem(inString);

        // Perform the translation
        final byte[] der = S100ExchangeSetUtils.getDerFromCert(certificate);

        // The DER content is the certificate encoding, i.e. Base64 encoding it
        // once produces the PEM body and nothing more
        assertNotNull(der);
        assertArrayEquals(certificate.getEncoded(), der);
        assertEquals(inString, Base64.getEncoder().encodeToString(der));
    }

    /**
     * The bounding box helper is shared by every S-100 product, so it must hand back the caller's
     * coordinates unaltered.
     * <p/>
     * S-100 Part 17 states no precision for boundingBox - its Remarks column is empty - and the
     * 7-decimal limit of S-124 clause 8.2 is a rule of that product specification alone. Rounding
     * here would silently coarsen the extent of any product whose datasets carry finer geometry,
     * and, because half-up rounding can move a bound inward, could leave a catalogue declaring a
     * box that no longer contains its own data. S-124 applies its own quantisation before it calls
     * this helper.
     */
    @Test
    void boundingBoxKeepsTheFullPrecisionOfTheSuppliedGeometry() {
        // The east and north bounds are chosen so that rounding to 7 decimals would move them
        // INWARD, which is the case that makes the declared box stop containing its own data.
        double west = 12.6712345678;
        double east = 12.77234561;
        double south = 55.4967123456;
        double north = 55.59672341;
        Geometry geometry = new GeometryFactory().toGeometry(new Envelope(west, east, south, north));

        S100GeographicBoundingBoxType box = S100ExchangeSetUtils.createS100GeographicBoundingBoxType(geometry);

        assertEquals(BigDecimal.valueOf(west), box.getWestBoundLongitude().getDecimal());
        assertEquals(BigDecimal.valueOf(east), box.getEastBoundLongitude().getDecimal());
        assertEquals(BigDecimal.valueOf(south), box.getSouthBoundLatitude().getDecimal());
        assertEquals(BigDecimal.valueOf(north), box.getNorthBoundLatitude().getDecimal());
        // The declared box still contains the geometry it was built from, to the last digit.
        assertTrue(box.getEastBoundLongitude().getDecimal().doubleValue() >= east,
                "eastBoundLongitude must not round inward");
        assertTrue(box.getNorthBoundLatitude().getDecimal().doubleValue() >= north,
                "northBoundLatitude must not round inward");
    }

    /**
     * A single dataset discovery metadata entry is an XML root element in its own right, so it
     * can be stored and read back separately from the exchange catalogue it was delivered in -
     * which is what a producer has to do between publishing a dataset and cancelling it
     * (S-100 Part 17, clause 17-4.4.1). The signature it carries, and the concrete
     * S100_SE_SignatureOnData type of that signature, must survive the round trip: they are
     * what the cancellation reuses.
     *
     * @throws JAXBException a JAXB exception thrown during the marshalling operations
     */
    @Test
    void datasetDiscoveryMetadataRoundTripsOnItsOwn() throws JAXBException {
        final S100DatasetDiscoveryMetadata metadata = this.signedDatasetDiscoveryMetadata();
        final String xml = S100ExchangeSetUtils.marshalS100DatasetDiscoveryMetadata(metadata);

        final S100DatasetDiscoveryMetadata result = S100ExchangeSetUtils.unmarshallS100DatasetDiscoveryMetadata(xml);

        assertNotNull(result);
        assertEquals(metadata.getFileName(), result.getFileName());
        assertEquals(metadata.getDatasetID(), result.getDatasetID());
        assertEquals(1, result.getDigitalSignatureValues().size());
        final S100SEDigitalSignature signature = result.getDigitalSignatureValues().get(0)
                .getS100SEDigitalSignature().getValue();
        assertInstanceOf(S100SESignatureOnData.class, signature);
        final S100SESignatureOnData signatureOnData = (S100SESignatureOnData) signature;
        assertEquals("sig1", signatureOnData.getId());
        assertEquals("CRT1", signatureOnData.getCertificateRef());
        assertEquals(DataStatus.UNENCRYPTED, signatureOnData.getDataStatus());
        assertArrayEquals("signature".getBytes(StandardCharsets.UTF_8), signatureOnData.getValue());
        assertEquals(xml, S100ExchangeSetUtils.marshalS100DatasetDiscoveryMetadata(result));
    }

    /**
     * The single argument overload formats its output, as the exchange catalogue one does. The
     * unformatted form is the one to store, and the two parse to equivalent entries - so the two
     * spellings must not be compared as strings.
     *
     * @throws JAXBException a JAXB exception thrown during the marshalling operations
     */
    @Test
    void datasetDiscoveryMetadataMarshallingDefaultsToFormattedOutput() throws JAXBException {
        final S100DatasetDiscoveryMetadata metadata = this.signedDatasetDiscoveryMetadata();

        final String formatted = S100ExchangeSetUtils.marshalS100DatasetDiscoveryMetadata(metadata);
        final String unformatted = S100ExchangeSetUtils.marshalS100DatasetDiscoveryMetadata(metadata, java.lang.Boolean.FALSE);

        assertTrue(formatted.contains("\n"));
        assertFalse(unformatted.contains("\n"));
        assertTrue(unformatted.length() < formatted.length());
        assertEquals(
                S100ExchangeSetUtils.marshalS100DatasetDiscoveryMetadata(
                        S100ExchangeSetUtils.unmarshallS100DatasetDiscoveryMetadata(formatted), java.lang.Boolean.FALSE),
                S100ExchangeSetUtils.marshalS100DatasetDiscoveryMetadata(
                        S100ExchangeSetUtils.unmarshallS100DatasetDiscoveryMetadata(unformatted), java.lang.Boolean.FALSE));
    }

    /**
     * The catalogue's dataset entry, carrying the kind of signature a real exchange set does:
     * an S100_SE_SignatureOnData, the realization S-100 Part 15, clause 15-8.11.3, gives a
     * signature over a data resource.
     */
    private S100DatasetDiscoveryMetadata signedDatasetDiscoveryMetadata() {
        final S100DatasetDiscoveryMetadata metadata = this.s100ExchangeCatalogue
                .getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas().get(0);
        final S100SESignatureOnData signature = new S100SESignatureOnData();
        signature.setId("sig1");
        signature.setCertificateRef("CRT1");
        signature.setDataStatus(DataStatus.UNENCRYPTED);
        signature.setValue("signature".getBytes(StandardCharsets.UTF_8));
        final S100DatasetDiscoveryMetadata.DigitalSignatureValue value =
                new S100DatasetDiscoveryMetadata.DigitalSignatureValue();
        value.setS100SEDigitalSignature(
                new dk.dma.niord.s100.catalog._5_2.ObjectFactory().createS100SESignatureOnData(signature));
        metadata.getDigitalSignatureValues().clear();
        metadata.getDigitalSignatureValues().add(value);
        return metadata;
    }
    /**
     * The marshalling operations return the marshaller's own UTF-8 output decoded as UTF-8,
     * not decoded with the platform default charset.
     * <p/>
     * This is what makes the marshal/unmarshal pair self inverse: the unmarshalling side
     * re-encodes the string as UTF-8, and S124ExchangeSetFactory writes the catalogue string
     * into CATALOG.XML as UTF-8, so a platform default decode would corrupt every non-ASCII
     * character on a JVM whose default charset is not UTF-8. S-100 Part 17, clause 17-4.4.1,
     * requires a cancellation to reproduce the cancelled dataset's metadata fields, so a
     * stored entry that comes back mangled is re-signed into the cancelling catalogue.
     * <p/>
     * The comparison is against the bytes an independent marshaller produces, which is the
     * assertion that actually detects a platform default decode; the round trip assertions
     * only detect it when the suite itself runs on a non-UTF-8 default charset.
     *
     * @throws JAXBException a JAXB exception thrown during the marshalling operations
     */
    @Test
    void marshallingDecodesTheMarshallersUtf8Output() throws JAXBException {
        // Free text a Danish producer really does put in a warning
        final String danish = "Søfartsstyrelsen - Øresund, Læsø og Ærø";
        final S100DatasetDiscoveryMetadata metadata = this.signedDatasetDiscoveryMetadata();
        metadata.setDatasetID(danish);
        metadata.setComment(S100ExchangeSetUtils.createCharacterStringPropertyType(danish));

        // The single entry form - what a producer stores between publishing and cancelling
        final String entryXml = S100ExchangeSetUtils.marshalS100DatasetDiscoveryMetadata(metadata);
        assertEquals(new String(marshalToBytes(metadata, java.lang.Boolean.TRUE), StandardCharsets.UTF_8), entryXml);
        final S100DatasetDiscoveryMetadata entry = S100ExchangeSetUtils.unmarshallS100DatasetDiscoveryMetadata(entryXml);
        assertEquals(danish, entry.getDatasetID());
        assertEquals(danish, entry.getComment().getCharacterString().getValue());

        // And the whole catalogue, which carries the same entry
        final String catalogueXml = S100ExchangeSetUtils.marshalS100ExchangeSetCatalogue(this.s100ExchangeCatalogue);
        assertEquals(new String(marshalToBytes(this.s100ExchangeCatalogue, java.lang.Boolean.TRUE), StandardCharsets.UTF_8), catalogueXml);
        final S100ExchangeCatalogue catalogue = S100ExchangeSetUtils.unmarshallS100ExchangeSetCatalogue(catalogueXml);
        assertEquals(danish, catalogue.getDatasetDiscoveryMetadata()
                .getS100DatasetDiscoveryMetadatas().get(0).getDatasetID());
        assertEquals(danish, catalogue.getDatasetDiscoveryMetadata()
                .getS100DatasetDiscoveryMetadatas().get(0).getComment().getCharacterString().getValue());
    }

    /**
     * Marshals through a JAXB context built independently of the one the utilities cache, so
     * that the assertion above compares against bytes rather than against the same code path.
     */
    private static byte[] marshalToBytes(Object object, java.lang.Boolean format) throws JAXBException {
        final JAXBContext context = JAXBContext.newInstance(
                S100ExchangeCatalogue.class.getPackageName(),
                S100ExchangeCatalogue.class.getClassLoader());
        final Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, format);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshaller.marshal(object, out);
        return out.toByteArray();
    }
}
