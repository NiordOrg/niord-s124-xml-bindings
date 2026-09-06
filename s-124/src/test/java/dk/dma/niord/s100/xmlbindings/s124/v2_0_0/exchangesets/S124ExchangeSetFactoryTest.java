package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.exchangesets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.grad.eNav.s100.utils.S100ExchangeSetUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import dk.dma.niord.s100.catalog._5_2.DataStatus;
import dk.dma.niord.s100.catalog._5_2.S100CompliancyCategory;
import dk.dma.niord.s100.catalog._5_2.S100DatasetDiscoveryMetadata;
import dk.dma.niord.s100.catalog._5_2.S100ExchangeCatalogue;
import org.grad.eNav.s100.enums.SecurityClassification;
import dk.dma.niord.s100.catalog._5_2.S100GeographicBoundingBoxType;
import dk.dma.niord.s100.catalog._5_2.S100ProductSpecification;
import dk.dma.niord.s100.catalog._5_2.S100Purpose;
import dk.dma.niord.s100.catalog._5_2.S100SECertificateContainerType;
import dk.dma.niord.s100.catalog._5_2.S100SEDigitalSignatureReference;
import dk.dma.niord.s100.catalog._5_2.S100SECertificateType;
import dk.dma.niord.s100.catalog._5_2.S100SEDigitalSignature;
import dk.dma.niord.s100.catalog._5_2.S100SESignatureOnData;
import dk.dma.niord.s100.catalog._5_2.S100SESignatureOnSignature;
import dk.dma.niord.s100.catalog._5_2.S100TemporalExtent;
import dk.dma.niord.s100.catalog._5_2.StandaloneDigitalSignature;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.CurveProperty;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.PointProperty;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.S100SpatialAttributeType;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.SurfaceProperty;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.DataSetIdentificationTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.AbstractGMLType;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.BoundingShapeTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.EnvelopeTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.PosImpl;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.Dataset;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.ReferenceType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.GeneralAreaType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.InformationType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.LocalityType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.LocationNameType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.MessageSeriesIdentifierType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnPart;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnTypeGeneralLabel;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnTypeGeneralType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnPreamble;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.ObjectFactory;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.WarningInformationType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.WarningTypeLabel;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.WarningTypeType;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.DatasetPurposeType;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.MDTopicCategoryCode;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util.GeometryS124Converter;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util.S124ConformanceException;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util.S124DatasetInfo;
import jakarta.xml.bind.JAXBContext;

class S124ExchangeSetFactoryTest {

    /** The content of a certificate element, whatever namespace prefix it is marshalled with. */
    private static final Pattern CERTIFICATE_ELEMENT =
            Pattern.compile("<(?:[\\w.-]+:)?certificate[^>]*>([^<]*)</(?:[\\w.-]+:)?certificate>");

    /**
     * A syntactically valid Part 15 signature value that no key made: the DER SEQUENCE of two
     * 48-byte INTEGERs, the shape a real P-384 signature has (S-100 Part 15, clause 15-8.4).
     * Tests that are not about cryptography sign with it, because the factory checks the shape
     * of every signature it embeds.
     */
    private static final byte[] DUMMY_SIGNATURE = dummySignature((byte) 0x11, (byte) 0x22);

    private static String testCertPem;

    /** Data Server certificate issued by the domain coordinator below, not by the SA. */
    private static String dataServerViaDcPem;
    /** Domain coordinator certificate, issued by the (separately installed) SA root. */
    private static String domainCoordinatorPem;
    /** Same subject name as the real domain coordinator, but a different key: it signed nothing here. */
    private static String domainCoordinatorRolloverPem;
    /** A previous Data Server certificate, issued by the same domain coordinator as the current one. */
    private static String dataServerPreviousPem;

    @BeforeAll
    static void loadCert() throws Exception {
        testCertPem = readResource("/test-cert.pem");
        dataServerViaDcPem = readResource("/test-cert-dataserver-via-dc.pem");
        domainCoordinatorPem = readResource("/test-cert-domaincoordinator.pem");
        domainCoordinatorRolloverPem = readResource("/test-cert-domaincoordinator-rollover.pem");
        dataServerPreviousPem = readResource("/test-cert-dataserver-previous.pem");
    }

    private static String readResource(String name) throws Exception {
        try (var in = S124ExchangeSetFactoryTest.class.getResourceAsStream(name)) {
            assertThat(in).as("%s must be on the test classpath", name).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
        }
    }

    @Test
    void buildsExchangeSetWithExpectedLayout() throws Exception {
        Dataset dataset = newDataset("DK.S124.test-1");

        AtomicInteger signCalls = new AtomicInteger();
        S124Signer signer = (algorithm, payload) -> {
            signCalls.incrementAndGet();
            return DUMMY_SIGNATURE;
        };

        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .datasets(List.of(dataset))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer(signer)
                .emails(List.of("nautinf@dma.dk"))
                .phone("+4572196000")
                .city("Korsoer")
                .postalCode("4220")
                .country("Denmark")
                .build()
                .toBytes();

        Map<String, byte[]> entries = unzip(zipBytes);

        assertThat(entries.keySet())
                .contains("S100_ROOT/CATALOG.XML", "S100_ROOT/CATALOG.SIGN")
                .contains("S100_ROOT/S-124/DATASET_FILES/" + datasetFileNameOf("DK.S124.test-1"));
        assertThat(entries.keySet())
                .contains("S100_ROOT/S-124/CATALOGUES/", "S100_ROOT/S-124/SUPPORT_FILES/");

        // signer called once per dataset file + once for CATALOG.XML
        assertThat(signCalls.get()).isEqualTo(2);

        String catalogXml = new String(entries.get("S100_ROOT/CATALOG.XML"), StandardCharsets.UTF_8);
        S100ExchangeCatalogue catalogue = S100ExchangeSetUtils.unmarshallS100ExchangeSetCatalogue(catalogXml);

        assertThat(catalogue.getIdentifier().getIdentifier()).startsWith("urn:mrn:iho:s124:exchangeset:");
        List<S100DatasetDiscoveryMetadata> meta = catalogue.getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas();
        assertThat(meta).hasSize(1);
        assertThat(meta.get(0).getFileName()).startsWith("file:/124DK00");
        assertThat(meta.get(0).getProducerCode()).isEqualTo("DK00");
        assertThat(meta.get(0).getDigitalSignatureValues()).hasSize(1);
        // S-124 clause 12.1 removes editionNumber and updateNumber from the dataset discovery
        // metadata, so an S-124 entry carries neither.
        assertThat(meta.get(0).getEditionNumber()).isNull();
        assertThat(meta.get(0).getUpdateNumber()).isNull();

        S100SECertificateContainerType certContainer = catalogue.getCertificates().get(0);
        assertThat(certContainer.getCertificates()).hasSize(1);
        assertThat(certContainer.getCertificates().get(0).getId()).isEqualTo("cer1");
    }

    /**
     * S-100 Part 15, clauses 15-8.7 and 15-8.11.2: the catalogue's signature file is a
     * self-contained StandaloneDigitalSignature XML document carrying the signed file name,
     * the certificates needed to authenticate the signature and the signature itself - not
     * the raw signature bytes.
     */
    @Test
    void catalogueSignatureIsAStandaloneDigitalSignatureDocument() throws Exception {
        byte[] signatureBytes = dummySignature((byte) 0x33, (byte) 0x44);

        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .datasets(List.of(newDataset("DK.S124.catalogue-signature")))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> signatureBytes)
                .phone("+4572196000")
                .build()
                .toBytes();

        String signXml = new String(unzip(zipBytes).get("S100_ROOT/CATALOG.SIGN"), StandardCharsets.UTF_8);
        assertThat(validateAgainstSignatureSchema(signXml))
                .as("XSD validation errors in CATALOG.SIGN:\n%s", signXml)
                .isEmpty();

        StandaloneDigitalSignature signature = unmarshalSignature(signXml);
        assertThat(signature.getFilename()).isEqualTo("CATALOG.XML");
        assertThat(signature.getCertificates().getCertificates()).hasSize(1);
        // S-100 Part 15, clauses 15-8.6 and 15-8.11.1: the schemeAdministrator id is the SA
        // identity (IHO by default) whose root certificate the OEM installs separately, and a
        // certificate's issuer is the id of the issuing element, not an X.500 distinguished
        // name - otherwise the OEM cannot resolve the chain and verification fails.
        assertThat(signature.getCertificates().getSchemeAdministrator().getId()).isEqualTo("IHO");
        assertThat(signature.getCertificates().getCertificates().get(0).getIssuer())
                .isEqualTo(signature.getCertificates().getSchemeAdministrator().getId())
                .doesNotContain("CN=", "O=");
        assertThat(signature.getDigitalSignature().getValue()).isEqualTo(signatureBytes);
        assertThat(signature.getDigitalSignature().getCertificateRef())
                .isEqualTo(signature.getCertificates().getCertificates().get(0).getId());
        // Part 15 realizes a signature over a resource as S100_SE_SignatureOnData, whose
        // mandatory dataStatus records that S-124 data is neither compressed nor encrypted.
        assertThat(signature.getDigitalSignature())
                .isInstanceOfSatisfying(S100SESignatureOnData.class,
                        s -> assertThat(s.getDataStatus()).isEqualTo(DataStatus.UNENCRYPTED));
    }

    /**
     * S-100 Part 15, clauses 15-8.6 and 15-8.11.1: an embedded certificate is "a signed Public
     * Key certificate ... Base 64 encoded", written with "the header and footer lines omitted".
     * An OEM therefore decodes the element content once and must hold the X.509 certificate in
     * DER form - a second Base64 layer would leave it with text it cannot parse, so neither
     * CATALOG.XML nor CATALOG.SIGN may encode the certificate twice.
     */
    @Test
    void embeddedCertificatesAreBase64EncodedExactlyOnce() throws Exception {
        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .datasets(List.of(newDataset("DK.S124.single-base64")))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build()
                .toBytes();

        Map<String, byte[]> entries = unzip(zipBytes);
        X509Certificate configured = S100ExchangeSetUtils.getCertFromPem(testCertPem);

        for (String file : List.of("S100_ROOT/CATALOG.XML", "S100_ROOT/CATALOG.SIGN")) {
            String xml = new String(entries.get(file), StandardCharsets.UTF_8);
            Matcher matcher = CERTIFICATE_ELEMENT.matcher(xml);
            assertThat(matcher.find()).as("no certificate element in %s:%n%s", file, xml).isTrue();
            String elementContent = matcher.group(1);

            // The element content is the PEM body of the configured certificate ...
            assertThat(elementContent).as("certificate content of %s", file).isEqualTo(testCertPem);

            // ... so a single Base64 decode yields parseable DER, not Base64 text again.
            byte[] der = Base64.getDecoder().decode(elementContent);
            X509Certificate decoded = (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new ByteArrayInputStream(der));
            assertThat(decoded).as("certificate of %s", file).isEqualTo(configured);
        }

        // The unmarshalled values - what JAXB hands a consumer after that one decode - are the
        // certificate DER content in both files as well.
        assertThat(catalogueOf(zipBytes).getCertificates().get(0).getCertificates().get(0).getValue())
                .isEqualTo(configured.getEncoded());
        StandaloneDigitalSignature signature = unmarshalSignature(
                new String(entries.get("S100_ROOT/CATALOG.SIGN"), StandardCharsets.UTF_8));
        assertThat(signature.getCertificates().getCertificates().get(0).getValue())
                .isEqualTo(configured.getEncoded());
    }

    /**
     * The SA identity must be the same in CATALOG.XML and CATALOG.SIGN, or the certificate
     * references of one file cannot be resolved with the chain declared by the other.
     */
    @Test
    void schemeAdministratorIsConsistentAcrossCatalogueAndSignature() throws Exception {
        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .datasets(List.of(newDataset("DK.S124.scheme-admin")))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .schemeAdministrator("IHO-TEST")
                .build()
                .toBytes();

        Map<String, byte[]> entries = unzip(zipBytes);
        String catalogXml = new String(entries.get("S100_ROOT/CATALOG.XML"), StandardCharsets.UTF_8);
        StandaloneDigitalSignature signature = unmarshalSignature(
                new String(entries.get("S100_ROOT/CATALOG.SIGN"), StandardCharsets.UTF_8));

        assertThat(catalogXml)
                .as("CATALOG.XML:%n%s", catalogXml)
                .contains("id=\"IHO-TEST\"")
                .contains("issuer=\"IHO-TEST\"");
        assertThat(signature.getCertificates().getSchemeAdministrator().getId()).isEqualTo("IHO-TEST");
        assertThat(signature.getCertificates().getCertificates().get(0).getIssuer()).isEqualTo("IHO-TEST");
    }

    /**
     * S-100 Part 15, clause 15-8.7: when a domain coordinator issued the Data Server
     * certificate, its certificate must travel with the exchange set and the issuer
     * references must form a path the OEM can walk up to the separately installed SA root.
     */
    @Test
    void domainCoordinatorChainIsIncludedAndReferenced() throws Exception {
        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .datasets(List.of(newDataset("DK.S124.dc-chain")))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(dataServerViaDcPem)
                .intermediateCertificatePems(List.of(domainCoordinatorPem))
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build()
                .toBytes();

        Map<String, byte[]> entries = unzip(zipBytes);
        StandaloneDigitalSignature signature = unmarshalSignature(
                new String(entries.get("S100_ROOT/CATALOG.SIGN"), StandardCharsets.UTF_8));

        // Both certificates travel with the exchange set; the SA root never does.
        assertThat(signature.getCertificates().getCertificates()).hasSize(2);
        Map<String, String> issuerById = signature.getCertificates().getCertificates().stream()
                .collect(Collectors.toMap(S100SECertificateType::getId, S100SECertificateType::getIssuer));

        // The signature is made with the Data Server certificate, which the domain
        // coordinator issued; the domain coordinator in turn resolves to the SA.
        String dataServerId = signature.getDigitalSignature().getCertificateRef();
        String domainCoordinatorId = issuerById.get(dataServerId);
        assertThat(domainCoordinatorId).isNotEqualTo("IHO");
        assertThat(issuerById).containsKey(domainCoordinatorId);
        assertThat(issuerById.get(domainCoordinatorId)).isEqualTo("IHO");
        assertThat(signature.getCertificates().getSchemeAdministrator().getId()).isEqualTo("IHO");

        // CATALOG.XML must declare the same chain, or the two files disagree.
        String catalogXml = new String(entries.get("S100_ROOT/CATALOG.XML"), StandardCharsets.UTF_8);
        assertThat(catalogXml)
                .as("CATALOG.XML:%n%s", catalogXml)
                .contains("id=\"" + domainCoordinatorId + "\" issuer=\"IHO\"")
                .contains("id=\"" + dataServerId + "\" issuer=\"" + domainCoordinatorId + "\"");
    }

    /**
     * A certificate that issues nothing else in the set leaves the OEM with a path it cannot
     * resolve, so it is rejected rather than shipped.
     */
    @Test
    void unrelatedIntermediateCertificateIsRejected() {
        S124ExchangeSetFactory factory = S124ExchangeSetFactory.builder()
                .datasets(List.of(newDataset("DK.S124.bad-chain")))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .intermediateCertificatePems(List.of(domainCoordinatorPem))
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build();

        assertThatThrownBy(factory::toBytes)
                .isInstanceOf(S124ExchangeSetFactory.ExchangeSetException.class)
                .hasMessageContaining("does not issue any other configured certificate");
    }

    /**
     * A certificate can carry the issuer's name without being the certificate that signed it -
     * the usual cause is an intermediate that has been rolled over. The OEM verifies the
     * signature, so matching on the name alone would ship a chain that fails on board.
     */
    @Test
    void intermediateThatDidNotSignTheDataServerCertificateIsRejected() {
        S124ExchangeSetFactory factory = S124ExchangeSetFactory.builder()
                .datasets(List.of(newDataset("DK.S124.rollover")))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(dataServerViaDcPem)
                // Same subject name as the domain coordinator that really issued it, other key.
                .intermediateCertificatePems(List.of(domainCoordinatorRolloverPem))
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build();

        assertThatThrownBy(factory::toBytes)
                .isInstanceOf(S124ExchangeSetFactory.ExchangeSetException.class)
                .hasMessageContaining("actually signed it");
    }

    /** The id anchors every issuer reference, so an absent one makes both catalogue files unusable. */
    @Test
    void blankSchemeAdministratorIsRejected() {
        assertThatThrownBy(() -> S124ExchangeSetFactory.builder().schemeAdministrator(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be blank");
        assertThatThrownBy(() -> S124ExchangeSetFactory.builder().schemeAdministrator("  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * A fileless cancellation reuses the original dataset's signature (S-100 Part 17, clause
     * 17-4.4.1). If the producer's certificate has been replaced since, the certificate that
     * made that signature must still travel with the exchange set, or the reused signature
     * resolves to the current key and cannot be verified.
     */
    @Test
    void cancellationCarriesTheCertificateThatMadeItsOriginalSignature() throws Exception {
        // Original signed under testCertPem ...
        S124ExchangeSetFactory.Cancellation cancellation =
                cancellationOf(newDataset("DK.S124.rotated"), List.of(testCertPem));

        // ... but the exchange set is now produced under a different certificate.
        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .cancellations(List.of(cancellation))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(dataServerViaDcPem)
                .intermediateCertificatePems(List.of(domainCoordinatorPem))
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build()
                .toBytes();

        String catalogXml = new String(unzip(zipBytes).get("S100_ROOT/CATALOG.XML"), StandardCharsets.UTF_8);
        assertThat(validateAgainstCatalogueSchema(catalogXml))
                .as("XSD validation errors in CATALOG.XML:\n%s", catalogXml)
                .isEmpty();

        S100ExchangeCatalogue catalogue = catalogueOf(zipBytes);
        Map<String, String> certificatePemById = catalogue.getCertificates().get(0).getCertificates()
                .stream()
                .collect(Collectors.toMap(S100SECertificateType::getId,
                        S124ExchangeSetFactoryTest::pemOf));

        String ref = catalogue.getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas().get(0)
                .getDigitalSignatureValues().get(0).getS100SEDigitalSignature().getValue()
                .getCertificateRef();

        // The reference must resolve, and to the certificate that actually made the signature.
        assertThat(certificatePemById).containsKey(ref);
        assertThat(certificatePemById.get(ref)).isEqualTo(testCertPem);
        assertThat(certificatePemById.get(ref)).isNotEqualTo(dataServerViaDcPem);
        // The current Data Server certificate is still carried, for the catalogue's own signature.
        assertThat(certificatePemById.get("cer1")).isEqualTo(dataServerViaDcPem);
    }

    /**
     * When the replaced certificate was issued by the same domain coordinator as the current
     * one, that coordinator is carried once. Every issuer reference must still name a
     * certificate the exchange set actually contains, or the chain dead-ends.
     */
    @Test
    void cancellationSharingAnIntermediateStillResolves() throws Exception {
        // Previous leaf, issued by the same domain coordinator as the current leaf.
        S124ExchangeSetFactory.Cancellation cancellation = cancellationOf(
                newDataset("DK.S124.shared-ca"), List.of(dataServerPreviousPem, domainCoordinatorPem));

        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .cancellations(List.of(cancellation))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(dataServerViaDcPem)
                .intermediateCertificatePems(List.of(domainCoordinatorPem))
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build()
                .toBytes();

        S100ExchangeCatalogue catalogue = catalogueOf(zipBytes);
        List<S100SECertificateType> certificates = catalogue.getCertificates().get(0).getCertificates();
        Map<String, String> pemById = certificates.stream()
                .collect(Collectors.toMap(S100SECertificateType::getId,
                        S124ExchangeSetFactoryTest::pemOf));

        // The shared domain coordinator is carried once, not twice.
        assertThat(pemById.values().stream().filter(domainCoordinatorPem::equals)).hasSize(1);

        // Every issuer reference resolves to a carried certificate or to the scheme administrator.
        String schemeAdministrator = catalogue.getCertificates().get(0).getSchemeAdministrator().getId();
        assertThat(certificates).allSatisfy(c ->
                assertThat(c.getIssuer())
                        .as("issuer of certificate %s must resolve", c.getId())
                        .isIn(union(pemById.keySet(), schemeAdministrator)));

        // ... including the one the cancellation's reused signature points at.
        String ref = catalogue.getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas().get(0)
                .getDigitalSignatureValues().get(0).getS100SEDigitalSignature().getValue()
                .getCertificateRef();
        assertThat(pemById).containsKey(ref);
        assertThat(pemById.get(ref)).isEqualTo(dataServerPreviousPem);
    }

    /**
     * The regression the certificate-aware cancellation API exists for, with real cryptography:
     * a dataset signed under certificate A is cancelled after the producer has rotated to B. The
     * cancellation reuses the original signature byte for byte (S-100 Part 17, clause 17-4.4.1),
     * and that signature must verify against the original dataset bytes with the certificate the
     * cancellation entry references - A, carried alongside B - while the new catalogue's own
     * signature verifies with B. The encoding is asserted separately from the round trips: a
     * signer returning the raw r||s form would sign and verify here just as well, and only a
     * Part 15 reader would notice.
     */
    @Test
    void cancellationSignatureVerifiesWithTheRotatedOutCertificateThatMadeIt() throws Exception {
        SigningIdentityFixture a = SigningIdentityFixture.selfSigned("Data Server A");
        SigningIdentityFixture b = SigningIdentityFixture.selfSigned("Data Server B");
        Dataset dataset = newDataset("DK.S124.real-rotation");
        String datasetFile = "S100_ROOT/S-124/DATASET_FILES/" + datasetFileNameOf("DK.S124.real-rotation");

        // 1. Publish under A.
        byte[] originalZip = S124ExchangeSetFactory.builder()
                .datasets(List.of(dataset))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(a.certificatePem())
                .signer(a.signer())
                .phone("+4572196000")
                .build()
                .toBytes();
        byte[] datasetBytes = unzip(originalZip).get(datasetFile);
        S100DatasetDiscoveryMetadata originalEntry = firstEntryOf(originalZip);
        byte[] originalSignature = dataSignatureOf(originalEntry).getValue();
        assertDerEcdsaSequence(originalSignature);
        assertThat(SigningIdentityFixture.verify(a.publicKey(), datasetBytes, originalSignature))
                .as("the published signature verifies against the dataset file with A")
                .isTrue();

        // 2. Rotate to B and cancel, handing over the retained entry and the chain that signed it.
        byte[] cancellationZip = S124ExchangeSetFactory.builder()
                .cancellations(List.of(new S124ExchangeSetFactory.Cancellation(
                        originalEntry, originalEntry.getIssueDate().plusDays(7), List.of(a.certificatePem()))))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(b.certificatePem())
                .signer(b.signer())
                .phone("+4572196000")
                .build()
                .toBytes();
        Map<String, byte[]> entries = unzip(cancellationZip);
        assertThat(datasetFileCount(entries)).as("a fileless cancellation ships no dataset file").isZero();
        String catalogXml = new String(entries.get("S100_ROOT/CATALOG.XML"), StandardCharsets.UTF_8);
        assertThat(validateAgainstCatalogueSchema(catalogXml))
                .as("XSD validation errors in CATALOG.XML:\n%s", catalogXml)
                .isEmpty();

        S100ExchangeCatalogue catalogue = catalogueOf(cancellationZip);
        S100DatasetDiscoveryMetadata cancellation =
                catalogue.getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas().get(0);
        assertThat(cancellation.getPurpose()).isEqualTo(S100Purpose.CANCELLATION);
        S100SESignatureOnData reused = dataSignatureOf(cancellation);
        assertThat(reused.getValue()).as("the reused signature is the original, byte for byte")
                .isEqualTo(originalSignature);

        // 3. The reused signature verifies with the certificate the entry references - decoded
        //    from the catalogue as an OEM would, not taken from the test - and that is A.
        X509Certificate referenced = certificateIn(catalogue.getCertificates().get(0), reused.getCertificateRef());
        assertThat(referenced).isEqualTo(a.certificate());
        assertThat(SigningIdentityFixture.verify(referenced.getPublicKey(), datasetBytes, reused.getValue())).isTrue();
        assertThat(SigningIdentityFixture.verify(b.publicKey(), datasetBytes, reused.getValue()))
                .as("B did not make the reused signature")
                .isFalse();

        // 4. The new catalogue's own signature verifies with B, over CATALOG.XML as shipped.
        StandaloneDigitalSignature catalogSign =
                unmarshalSignature(new String(entries.get("S100_ROOT/CATALOG.SIGN"), StandardCharsets.UTF_8));
        X509Certificate catalogueSigner =
                certificateIn(catalogSign.getCertificates(), catalogSign.getDigitalSignature().getCertificateRef());
        assertThat(catalogueSigner).isEqualTo(b.certificate());
        assertDerEcdsaSequence(catalogSign.getDigitalSignature().getValue());
        assertThat(SigningIdentityFixture.verify(catalogueSigner.getPublicKey(),
                entries.get("S100_ROOT/CATALOG.XML"), catalogSign.getDigitalSignature().getValue())).isTrue();

        // B is the current certificate; A is carried under a historical id of the catalogue's own.
        assertThat(certificateIn(catalogue.getCertificates().get(0), "cer1")).isEqualTo(b.certificate());
        assertThat(reused.getCertificateRef()).isNotEqualTo("cer1");
    }

    /**
     * S-100 Part 15, clause 15-8.4, embeds "a Base64 ASN.1 byte sequence" of R and S. A signer
     * returning the raw r||s concatenation instead - the output of the JCA algorithm
     * "SHA384withECDSAinP1363Format" - produces signatures that verify in the producer's own
     * code and that no Part 15 reader can decode, so the factory refuses them rather than
     * signing and shipping them.
     */
    @Test
    void rejectsASignerThatReturnsTheRawRSPairInsteadOfTheDerSequence() {
        SigningIdentityFixture identity = SigningIdentityFixture.selfSigned("Data Server");
        S124Signer p1363 = (algorithm, payload) -> {
            try {
                Signature signature = Signature.getInstance("SHA384withECDSAinP1363Format");
                signature.initSign(identity.privateKey());
                signature.update(payload);
                return signature.sign();
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException(e);
            }
        };
        S124ExchangeSetFactory factory = S124ExchangeSetFactory.builder()
                .datasets(List.of(newDataset("DK.S124.p1363")))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(identity.certificatePem())
                .signer(p1363)
                .phone("+4572196000")
                .build();

        assertThatThrownBy(factory::toBytes)
                .isInstanceOf(S124ExchangeSetFactory.ExchangeSetException.class)
                .hasMessageContaining("15-8.4")
                .hasMessageContaining("96 bytes")
                .hasMessageContaining("SHA384withECDSA");
    }

    /**
     * DER admits exactly one encoding of each integer: a leading 0x00 byte is allowed only as
     * the sign byte before a magnitude whose high bit is set. A signer that pads r or s beyond
     * that produces well-formed BER that Java's verifier refuses outright ("Invalid encoding for
     * signature") instead of reading the same number, so the factory must refuse it too rather
     * than package a signature nothing can check.
     */
    @Test
    void rejectsASignatureWhoseIntegerCarriesRedundantLeadingZeroPadding() {
        // The premise, with real cryptography: the padded form of a valid signature does not verify.
        SigningIdentityFixture identity = SigningIdentityFixture.selfSigned("Data Server");
        byte[] payload = "payload".getBytes(StandardCharsets.UTF_8);
        assertThat(SigningIdentityFixture.verify(identity.publicKey(), payload, identity.sign(payload))).isTrue();
        assertThat(SigningIdentityFixture.verify(identity.publicKey(), payload, withPaddedR(identity.sign(payload))))
                .as("Java refuses the padded encoding").isFalse();

        // A deterministic padded value: r is 48 bytes whose first is 0x00 and whose second has no high bit.
        byte[] padded = dummySignature((byte) 0x11, (byte) 0x22);
        padded[4] = 0x00;
        assertThatThrownBy(() -> exchangeSetSignedWith((alg, p) -> padded))
                .isInstanceOf(S124ExchangeSetFactory.ExchangeSetException.class)
                .hasMessageContaining("INTEGER r")
                .hasMessageContaining("redundant leading zero");

        // And a real signer that pads its output is caught the same way.
        assertThatThrownBy(() -> exchangeSetSignedWith((alg, p) -> withPaddedR(identity.sign(p))))
                .isInstanceOf(S124ExchangeSetFactory.ExchangeSetException.class)
                .hasMessageContaining("INTEGER r");
    }

    /**
     * The one leading zero DER does require - the sign byte before a magnitude with its high bit
     * set, which about half of all P-384 signatures need - is not padding and must pass.
     */
    @Test
    void acceptsTheSignByteDerRequiresBeforeAHighBitMagnitude() throws Exception {
        byte[] signature = new byte[2 + 51 + 50];
        signature[0] = 0x30;
        signature[1] = (byte) 101;
        signature[2] = 0x02;
        signature[3] = 49;
        signature[4] = 0x00;
        Arrays.fill(signature, 5, 53, (byte) 0x9A);
        signature[53] = 0x02;
        signature[54] = 48;
        Arrays.fill(signature, 55, 103, (byte) 0x22);

        byte[] zip = exchangeSetSignedWith((alg, payload) -> signature);

        assertThat(dataSignatureOf(firstEntryOf(zip)).getValue()).isEqualTo(signature);
    }

    /** A one-dataset exchange set signed by {@code signer} under the test certificate. */
    private static byte[] exchangeSetSignedWith(S124Signer signer) {
        return S124ExchangeSetFactory.builder()
                .datasets(List.of(newDataset("DK.S124.encoding")))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer(signer)
                .phone("+4572196000")
                .build()
                .toBytes();
    }

    /** {@code signature} with one redundant 0x00 prepended to the content of INTEGER r: well-formed BER, not DER. */
    private static byte[] withPaddedR(byte[] signature) {
        int rLength = signature[3] & 0xFF;
        byte[] padded = new byte[signature.length + 1];
        padded[0] = 0x30;
        padded[1] = (byte) (signature[1] + 1);
        padded[2] = 0x02;
        padded[3] = (byte) (rLength + 1);
        padded[4] = 0x00;
        System.arraycopy(signature, 4, padded, 5, signature.length - 4);
        return padded;
    }

    /** Bytes that are not a well-formed SEQUENCE at all are rejected too, naming the signed resource. */
    @Test
    void rejectsSignatureBytesThatAreNotADerSequence() {
        S124ExchangeSetFactory factory = S124ExchangeSetFactory.builder()
                .datasets(List.of(newDataset("DK.S124.garbage-signature")))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> "not a signature".getBytes(StandardCharsets.UTF_8))
                .phone("+4572196000")
                .build();

        assertThatThrownBy(factory::toBytes)
                .isInstanceOf(S124ExchangeSetFactory.ExchangeSetException.class)
                .hasMessageContaining(datasetFileNameOf("DK.S124.garbage-signature"))
                .hasMessageContaining("SEQUENCE tag 0x30");
    }

    /**
     * A producer that retains certificate chains with its signatures will, until its first
     * rotation, hand the factory the very certificate it is signing with. That is not a second
     * certificate: it is deduplicated by X.509 identity onto the current chain's entry, so the
     * catalogue carries each certificate once and the reused signature references the current id.
     */
    @Test
    void supplyingTheCurrentCertificateAsTheHistoricalChainReusesItsEntry() throws Exception {
        S124ExchangeSetFactory.Cancellation cancellation = cancellationOf(
                newDataset("DK.S124.same-cert"), List.of(dataServerViaDcPem, domainCoordinatorPem));

        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .datasets(List.of(newDataset("DK.S124.same-cert-active")))
                .cancellations(List.of(cancellation))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(dataServerViaDcPem)
                .intermediateCertificatePems(List.of(domainCoordinatorPem))
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build()
                .toBytes();

        S100ExchangeCatalogue catalogue = catalogueOf(zipBytes);
        List<S100SECertificateType> certificates = catalogue.getCertificates().get(0).getCertificates();
        assertThat(certificates).extracting(S100SECertificateType::getId).containsExactlyInAnyOrder("cer1", "ca1");
        assertThat(certificates).extracting(S124ExchangeSetFactoryTest::pemOf)
                .containsExactlyInAnyOrder(dataServerViaDcPem, domainCoordinatorPem);
        // The new dataset's signature and the reused one both reference the one Data Server entry.
        assertThat(catalogue.getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas())
                .hasSize(2)
                .allSatisfy(entry -> assertThat(dataSignatureOf(entry).getCertificateRef()).isEqualTo("cer1"));
    }

    /**
     * A producer keeps one record of each published dataset and may cancel it in several
     * exchange sets - for different consumers, or after a failed delivery - under whatever
     * certificate is current each time. The record it hands over is its evidence of what was
     * published, so no build may change it: not the entry, not the signature bytes and not the
     * certificate reference the original catalogue gave the signature.
     */
    @Test
    void repeatedBuildsFromTheSameCancellationLeaveItsInputUntouched() throws Exception {
        S124ExchangeSetFactory.Cancellation cancellation =
                cancellationOf(newDataset("DK.S124.reused-record"), List.of(testCertPem));
        S100SESignatureOnData originalSignature = dataSignatureOf(cancellation.original());
        String entryBefore = marshal(cancellation.original());
        byte[] bytesBefore = originalSignature.getValue().clone();
        String refBefore = originalSignature.getCertificateRef();

        // First under the certificate that signed it, then after a rotation to a chained one.
        byte[] first = cancellationExchangeSet(cancellation, testCertPem, List.of());
        byte[] second = cancellationExchangeSet(cancellation, dataServerViaDcPem, List.of(domainCoordinatorPem));

        assertThat(marshal(cancellation.original())).isEqualTo(entryBefore);
        assertThat(originalSignature.getValue()).isEqualTo(bytesBefore);
        assertThat(originalSignature.getCertificateRef()).isEqualTo(refBefore);
        assertThat(cancellation.original().getPurpose()).isEqualTo(S100Purpose.NEW_DATASET);

        // Each catalogue carries the signing certificate under an id of its own, and both resolve to it.
        S100SESignatureOnData inFirst = dataSignatureOf(firstEntryOf(first));
        S100SESignatureOnData inSecond = dataSignatureOf(firstEntryOf(second));
        assertThat(inFirst.getValue()).isEqualTo(bytesBefore);
        assertThat(inSecond.getValue()).isEqualTo(bytesBefore);
        assertThat(inFirst.getCertificateRef()).isEqualTo("cer1");
        assertThat(inSecond.getCertificateRef()).isNotEqualTo("cer1");
        assertThat(pemOf(certificateEntry(catalogueOf(first), inFirst.getCertificateRef()))).isEqualTo(testCertPem);
        assertThat(pemOf(certificateEntry(catalogueOf(second), inSecond.getCertificateRef()))).isEqualTo(testCertPem);
    }

    private static S100SECertificateType certificateEntry(S100ExchangeCatalogue catalogue, String id) {
        return catalogue.getCertificates().get(0).getCertificates().stream()
                .filter(c -> id.equals(c.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no certificate with id " + id + " is carried"));
    }

    /**
     * S-100 Part 15, clause 15-8.8: "Chains of digital signatures are implemented by use of a
     * signatureRef attribute", and clause 15-8.11.5 makes that attribute mandatory (Mult 1).
     * A cancelled dataset's entry may carry such a counter-signature, so reproducing the entry
     * must keep it an S100_SE_SignatureOnSignature with its signatureRef, and must leave its
     * certificateRef pointing at the certificate of the party that made it - the counter-signer
     * is by definition a different certified identity than the data signer. That certificate
     * travels with the exchange set (clause 15-8.7) under an id of this catalogue's own, which
     * is what the reproduced signature has to reference.
     */
    @Test
    void reusedSignatureOnSignatureKeepsItsSubtypeAndReferences() throws Exception {
        S124ExchangeSetFactory.Cancellation seed = cancellationOf(newDataset("DK.S124.countersigned"));
        S100DatasetDiscoveryMetadata original = seed.original();
        String dataSignatureId = original.getDigitalSignatureValues().get(0)
                .getS100SEDigitalSignature().getValue().getId();
        original.getDigitalSignatureValues().add(
                counterSignature("s3", dataSignatureId, "cerCounterSigner"));

        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .cancellations(List.of(new S124ExchangeSetFactory.Cancellation(
                        original, seed.issueDate(), List.of(),
                        Map.of("cerCounterSigner", List.of(dataServerPreviousPem, domainCoordinatorPem)))))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build()
                .toBytes();

        S100ExchangeCatalogue catalogue = catalogueOf(zipBytes);
        S100DatasetDiscoveryMetadata emitted = catalogue
                .getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas().get(0);
        assertThat(emitted.getPurpose()).isEqualTo(S100Purpose.CANCELLATION);
        assertThat(emitted.getDigitalSignatureValues()).hasSize(2);

        // The dataset signature still resolves to the certificate carried for the cancellation.
        S100SEDigitalSignature onData = emitted.getDigitalSignatureValues().get(0)
                .getS100SEDigitalSignature().getValue();
        assertThat(onData).isInstanceOf(S100SESignatureOnData.class);
        assertThat(onData.getCertificateRef()).isEqualTo("cer1");

        // The chained signature keeps its subtype and its signatureRef ...
        S100SEDigitalSignature chained = emitted.getDigitalSignatureValues().get(1)
                .getS100SEDigitalSignature().getValue();
        assertThat(chained).isInstanceOfSatisfying(S100SESignatureOnSignature.class, s -> {
            assertThat(s.getId()).isEqualTo("s3");
            assertThat(s.getSignatureRef()).isEqualTo(dataSignatureId);
            assertThat(s.getValue()).isEqualTo("counter-signature".getBytes(StandardCharsets.UTF_8));
        });

        // ... and its certificateRef resolves, to the counter-signer's certificate rather than
        // to the data signer's (clause 15-8.11.5: "Identifier of the certificate against which
        // the digital signature validates").
        Map<String, String> pemById = catalogue.getCertificates().get(0).getCertificates().stream()
                .collect(Collectors.toMap(S100SECertificateType::getId,
                        S124ExchangeSetFactoryTest::pemOf));
        String counterSignerRef = ((S100SESignatureOnSignature) chained).getCertificateRef();
        assertThat(pemById).containsKey(counterSignerRef);
        assertThat(pemById.get(counterSignerRef)).isEqualTo(dataServerPreviousPem);
        assertThat(counterSignerRef).isNotEqualTo(onData.getCertificateRef());

        // Every issuer reference of the enlarged certificate container still resolves too.
        String schemeAdministrator = catalogue.getCertificates().get(0).getSchemeAdministrator().getId();
        assertThat(catalogue.getCertificates().get(0).getCertificates()).allSatisfy(c ->
                assertThat(c.getIssuer())
                        .as("issuer of certificate %s must resolve", c.getId())
                        .isIn(union(pemById.keySet(), schemeAdministrator)));

        // ... in the marshalled catalogue too, as the S100_SE_SignatureOnSignature element.
        String catalogXml = new String(unzip(zipBytes).get("S100_ROOT/CATALOG.XML"), StandardCharsets.UTF_8);
        assertThat(catalogXml)
                .as("CATALOG.XML:%n%s", catalogXml)
                .contains("S100_SE_SignatureOnSignature")
                .contains("signatureRef=\"" + dataSignatureId + "\"");
        assertThat(validateAgainstCatalogueSchema(catalogXml))
                .as("XSD validation errors in CATALOG.XML:\n%s", catalogXml)
                .isEmpty();

        // Supplying the original must not modify it.
        assertThat(original.getDigitalSignatureValues()).hasSize(2);
        assertThat(original.getDigitalSignatureValues().get(1).getS100SEDigitalSignature().getValue())
                .isInstanceOfSatisfying(S100SESignatureOnSignature.class, s ->
                        assertThat(s.getCertificateRef()).isEqualTo("cerCounterSigner"));
    }

    /**
     * The counter-signer's certificate cannot be conjured up: S-100 Part 15, clause 15-8.7,
     * requires the exchange set to carry "all the certificates required to perform a full
     * certificate path validation without any external access", so a chained signature whose
     * certificate is not supplied is rejected rather than emitted with a certificateRef that
     * resolves to nothing in the catalogue.
     */
    @Test
    void reusedSignatureOnSignatureWithoutTheCounterSignersCertificateIsRejected() throws Exception {
        S124ExchangeSetFactory.Cancellation seed = cancellationOf(newDataset("DK.S124.unknown-signer"));
        S100DatasetDiscoveryMetadata original = seed.original();
        String dataSignatureId = original.getDigitalSignatureValues().get(0)
                .getS100SEDigitalSignature().getValue().getId();
        original.getDigitalSignatureValues().add(
                counterSignature("s3", dataSignatureId, "cerCounterSigner"));

        S124ExchangeSetFactory factory = S124ExchangeSetFactory.builder()
                .cancellations(List.of(new S124ExchangeSetFactory.Cancellation(original, seed.issueDate())))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build();

        assertThatThrownBy(factory::toBytes)
                .isInstanceOf(S124ExchangeSetFactory.ExchangeSetException.class)
                .hasMessageContaining("cerCounterSigner")
                .hasMessageContaining("does not carry");
    }

    /**
     * A chained signature without the signatureRef that clause 15-8.11.5 makes mandatory
     * cannot be reproduced into a conformant cancellation entry, so it is rejected instead of
     * being emitted with a severed chain.
     */
    @Test
    void reusedSignatureOnSignatureWithoutSignatureRefIsRejected() throws Exception {
        S124ExchangeSetFactory.Cancellation seed = cancellationOf(newDataset("DK.S124.no-signature-ref"));
        S100DatasetDiscoveryMetadata original = seed.original();
        original.getDigitalSignatureValues().add(counterSignature("s3", null, "cerCounterSigner"));

        S124ExchangeSetFactory factory = S124ExchangeSetFactory.builder()
                .cancellations(List.of(new S124ExchangeSetFactory.Cancellation(original, seed.issueDate())))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build();

        assertThatThrownBy(factory::toBytes)
                .isInstanceOf(S124ExchangeSetFactory.ExchangeSetException.class)
                .hasMessageContaining("signatureRef");
    }

    /**
     * The certificate of the counter-signing party cannot be guessed either: silently pointing
     * the chained signature at the data signer's certificate would break the verification of
     * every signature in the chain (clause 15-8.8).
     */
    @Test
    void reusedSignatureOnSignatureWithoutCertificateRefIsRejected() throws Exception {
        S124ExchangeSetFactory.Cancellation seed = cancellationOf(newDataset("DK.S124.no-certificate-ref"));
        S100DatasetDiscoveryMetadata original = seed.original();
        String dataSignatureId = original.getDigitalSignatureValues().get(0)
                .getS100SEDigitalSignature().getValue().getId();
        original.getDigitalSignatureValues().add(counterSignature("s3", dataSignatureId, null));

        S124ExchangeSetFactory factory = S124ExchangeSetFactory.builder()
                .cancellations(List.of(new S124ExchangeSetFactory.Cancellation(original, seed.issueDate())))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build();

        assertThatThrownBy(factory::toBytes)
                .isInstanceOf(S124ExchangeSetFactory.ExchangeSetException.class)
                .hasMessageContaining("certificateRef");
    }

    /** A signature made by another party on the signature {@code signatureRef} (clause 15-8.8). */
    private static S100DatasetDiscoveryMetadata.DigitalSignatureValue counterSignature(
            String id, String signatureRef, String certificateRef) {
        S100SESignatureOnSignature signature = new S100SESignatureOnSignature();
        signature.setId(id);
        signature.setSignatureRef(signatureRef);
        signature.setCertificateRef(certificateRef);
        signature.setValue("counter-signature".getBytes(StandardCharsets.UTF_8));
        S100DatasetDiscoveryMetadata.DigitalSignatureValue value =
                new S100DatasetDiscoveryMetadata.DigitalSignatureValue();
        value.setS100SEDigitalSignature(new dk.dma.niord.s100.catalog._5_2.ObjectFactory()
                .createS100SESignatureOnSignature(signature));
        return value;
    }

    /**
     * The PEM body of a carried certificate: the element is typed xs:base64Binary, so its
     * unmarshalled value is the certificate DER content and Base64 encoding it once - exactly
     * what S-100 Part 15, clause 15-8.6, puts on the wire - reproduces the PEM text.
     */
    private static String pemOf(S100SECertificateType certificate) {
        return Base64.getEncoder().encodeToString(certificate.getValue());
    }

    private static List<String> union(java.util.Set<String> ids, String extra) {
        List<String> all = new java.util.ArrayList<>(ids);
        all.add(extra);
        return all;
    }

    /** A DER SEQUENCE of two 48-byte INTEGERs filled with the given bytes; see {@link #DUMMY_SIGNATURE}. */
    private static byte[] dummySignature(byte rFill, byte sFill) {
        byte[] signature = new byte[2 + 50 + 50];
        signature[0] = 0x30;
        signature[1] = (byte) 100;
        signature[2] = 0x02;
        signature[3] = 48;
        Arrays.fill(signature, 4, 52, rFill);
        signature[52] = 0x02;
        signature[53] = 48;
        Arrays.fill(signature, 54, 102, sFill);
        return signature;
    }

    /**
     * S-100 Part 15, clause 15-8.4: the embedded value is "a Base64 ASN.1 byte sequence" of the
     * integers R and S - once JAXB has removed the Base64 layer on unmarshalling, a DER SEQUENCE
     * (tag 0x30) of two INTEGERs (tag 0x02) that fill it exactly. Asserted independently of any
     * sign-and-verify round trip, because such a round trip succeeds for the raw r||s form too.
     */
    private static void assertDerEcdsaSequence(byte[] signature) {
        assertThat(signature[0]).as("SEQUENCE tag").isEqualTo((byte) 0x30);
        assertThat(signature[1] & 0xFF).as("SEQUENCE length").isEqualTo(signature.length - 2);
        assertThat(signature[2]).as("INTEGER r tag").isEqualTo((byte) 0x02);
        int rLength = signature[3] & 0xFF;
        assertThat(signature[4 + rLength]).as("INTEGER s tag").isEqualTo((byte) 0x02);
        int sLength = signature[5 + rLength] & 0xFF;
        assertThat(6 + rLength + sLength).as("the two INTEGERs fill the SEQUENCE").isEqualTo(signature.length);
    }

    /** The certificate {@code id} refers to, decoded from the container exactly as an OEM would decode it. */
    private static X509Certificate certificateIn(S100SECertificateContainerType container, String id) throws Exception {
        S100SECertificateType carried = container.getCertificates().stream()
                .filter(c -> id.equals(c.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no certificate with id " + id + " is carried"));
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(carried.getValue()));
    }

    /** The first (for a dataset entry, the mandatory S100_SE_SignatureOnData) signature of an entry. */
    private static S100SESignatureOnData dataSignatureOf(S100DatasetDiscoveryMetadata entry) {
        return (S100SESignatureOnData) entry.getDigitalSignatureValues().get(0)
                .getS100SEDigitalSignature().getValue();
    }

    private static S100DatasetDiscoveryMetadata firstEntryOf(byte[] zip) throws Exception {
        return catalogueOf(zip).getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas().get(0);
    }

    /** A cancellation-only exchange set built under the given current chain, with dummy signatures. */
    private static byte[] cancellationExchangeSet(S124ExchangeSetFactory.Cancellation cancellation,
            String certificatePem, List<String> intermediatePems) {
        return S124ExchangeSetFactory.builder()
                .cancellations(List.of(cancellation))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(certificatePem)
                .intermediateCertificatePems(intermediatePems)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build()
                .toBytes();
    }

    /**
     * Clause 17-4.4.1: the cancellation entry keeps "all other mandatory metadata fields also
     * set to the same values as the original, with the exception of the issueDate". They must
     * therefore come from the cancelled dataset, not from a configuration that has since moved
     * on.
     */
    @Test
    void cancellationReproducesTheOriginalMetadataNotTheCurrentConfiguration() throws Exception {
        S124ExchangeSetFactory.Cancellation cancellation = cancellationOf(newDataset("DK.S124.drift"));
        S100DatasetDiscoveryMetadata original = cancellation.original();

        // Everything about the producer has changed since the dataset was published.
        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .cancellations(List.of(cancellation))
                .organization("Some Other Authority")
                .producerCode("XX99")
                .classification(SecurityClassification.RESTRICTED)
                .datasetComment("a comment that did not exist back then")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+9900000000")
                .build()
                .toBytes();

        S100DatasetDiscoveryMetadata emitted = catalogueOf(zipBytes)
                .getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas().get(0);

        // The two fields that legitimately differ.
        assertThat(emitted.getPurpose()).isEqualTo(S100Purpose.CANCELLATION);
        assertThat(emitted.getIssueDate()).isEqualTo(cancellation.issueDate());

        // Everything else reproduces the original, despite the changed configuration.
        assertThat(emitted.getFileName()).isEqualTo(original.getFileName());
        assertThat(emitted.getDatasetID()).isEqualTo(original.getDatasetID());
        assertThat(emitted.getEditionNumber()).isEqualTo(original.getEditionNumber());
        assertThat(emitted.getUpdateNumber()).isEqualTo(original.getUpdateNumber());
        assertThat(emitted.getComment()).isEqualTo(original.getComment());
        assertThat(emitted.isNotForNavigation()).isEqualTo(original.isNotForNavigation());
        assertThat(emitted.getProducerCode()).isEqualTo(original.getProducerCode());

        // The catalogue's own contact legitimately names the current producer, so compare the
        // dataset entry itself: it must still describe the producer as of the original.
        String emittedXml = marshal(emitted);
        assertThat(emittedXml)
                .as("cancellation entry:%n%s", emittedXml)
                .contains("DMA")
                .doesNotContain("Some Other Authority")
                .doesNotContain("XX99")
                .doesNotContain("a comment that did not exist back then")
                // the original was unclassified; the current configuration says restricted
                .doesNotContain("restricted");

        // Supplying the original must not modify it.
        assertThat(original.getPurpose()).isEqualTo(S100Purpose.NEW_DATASET);
    }

    /**
     * The dataset entries must follow the fixed values of the S-124 clause 12.2.2 discovery
     * metadata profile and the algorithm/codelist restrictions of S-100 Parts 15 and 17.
     */
    @Test
    void datasetEntriesFollowTheS124MetadataProfile() throws Exception {
        LocalDateTime beforeBuild = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1);

        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .datasets(List.of(newDataset("DK.S124.profile")))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build()
                .toBytes();

        String catalogXml = new String(unzip(zipBytes).get("S100_ROOT/CATALOG.XML"), StandardCharsets.UTF_8);
        assertThat(catalogXml)
                .as("CATALOG.XML:%n%s", catalogXml)
                // no navigationPurpose: the S-124 profile has no such attribute
                .doesNotContain("navigationPurpose")
                // S-124 clause 12.2.2 fixes specificUsage
                .contains("Navigational Warning Service")
                // Part 15 clause 15-8.7 mandates the ECDSA-384-SHA2 encoding
                .contains("ECDSA-384-SHA2")
                .doesNotContain("ECDSA-384-SHA3")
                // Part 15 clause 15-8.11.4: signatures on data carry dataStatus
                .contains("unencrypted")
                // S-124 clause 12.2.2 makes the bounding box mandatory
                .contains("westBoundLongitude");

        S100ExchangeCatalogue catalogue = S100ExchangeSetUtils.unmarshallS100ExchangeSetCatalogue(catalogXml);
        S100ProductSpecification productSpecification = catalogue.getProductSpecifications().get(0);
        assertThat(productSpecification.getCompliancyCategory()).isEqualTo(S100CompliancyCategory.CATEGORY_3);
        // S-124 clause 12.2.2.4 fixes the name and the product identifier, and defines the date
        // as the publication date of the product specification (March 2025 for Edition 2.0.0)
        assertThat(productSpecification.getName()).isEqualTo("Navigational Warnings");
        assertThat(productSpecification.getProductIdentifier()).isEqualTo("S-124");
        assertThat(productSpecification.getNumber()).isEqualTo(BigInteger.valueOf(124));
        assertThat(productSpecification.getVersion()).isEqualTo("2.0.0");
        assertThat(productSpecification.getDate()).isEqualTo(LocalDate.of(2025, 3, 28));
        // Part 17 mandates the catalogue creation date and time in UTC
        assertThat(catalogue.getIdentifier().getDateTime())
                .isBetween(beforeBuild, LocalDateTime.now(ZoneOffset.UTC).plusMinutes(1));

        S100DatasetDiscoveryMetadata meta = catalogue.getDatasetDiscoveryMetadata()
                .getS100DatasetDiscoveryMetadatas().get(0);
        assertThat(meta.getDigitalSignatureReference().getValue())
                .isEqualTo(S100SEDigitalSignatureReference.ECDSA_384_SHA_2);
    }

    /**
     * S-124 clause 12.1: "the S100_DatasetDiscoveryMetadata is further restricted to remove
     * attributes that are not relevant to a Navigational Warning service." Neither the clause
     * 12.2.2 encoding table nor the metadata model figure carries editionNumber, updateNumber,
     * dataCoverage, replacedData or resourceMaintenance, so no S-124 dataset entry may.
     */
    @Test
    void datasetEntriesOmitTheAttributesTheS124ProfileRemoves() throws Exception {
        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .datasets(List.of(newDataset("DK.S124.profile-removals")))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build()
                .toBytes();

        S100DatasetDiscoveryMetadata meta = catalogueOf(zipBytes)
                .getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas().get(0);

        assertThat(meta.getEditionNumber()).isNull();
        assertThat(meta.getUpdateNumber()).isNull();
        assertThat(meta.getDataCoverages()).isEmpty();
        assertThat(meta.isReplacedData()).isNull();
        assertThat(meta.getResourceMaintenance()).isNull();

        String entryXml = marshal(meta);
        assertThat(entryXml)
                .as("dataset entry:%n%s", entryXml)
                .doesNotContain("editionNumber")
                .doesNotContain("updateNumber")
                .doesNotContain("dataCoverage")
                .doesNotContain("replacedData")
                .doesNotContain("resourceMaintenance")
                .doesNotContain("maintenanceAndUpdateFrequency");
    }

    /**
     * S-124 clause 12.2.2 makes the bounding box mandatory: "boundingBox | The extent of the
     * dataset limits | 1 | EX_GeographicBoundingBox". The dataset's own gml:boundedBy is used
     * when it declares one.
     */
    @Test
    void boundingBoxComesFromTheDatasetEnvelopeWhenItDeclaresOne() throws Exception {
        Dataset dataset = newDataset("DK.S124.declared-extent");

        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .datasets(List.of(dataset))
                .organization("DMA")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build()
                .toBytes();

        assertBoundingBox(catalogueOf(zipBytes), 8.0, 14.0, 54.0, 58.0);
    }

    /**
     * gml:boundedBy is optional in the S-100 GML profile, but S-124 clause 12.2.2 makes the
     * discovery metadata's boundingBox mandatory all the same, so the extent of a dataset that
     * declares no envelope is derived from the geometry its members carry - the union of it,
     * not the geometry of a single member.
     */
    @Test
    void boundingBoxIsDerivedFromMemberGeometryWhenTheDatasetDeclaresNoEnvelope() throws Exception {
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        Dataset dataset = newDataset("DK.S124.derived-extent");
        dataset.setBoundedBy(null);
        addMemberGeometry(dataset, geometryFactory.createPoint(new Coordinate(8.0, 54.0)));
        addMemberGeometry(dataset, geometryFactory.createLineString(new Coordinate[] {
                new Coordinate(11.0, 56.0), new Coordinate(14.0, 58.0) }));

        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .datasets(List.of(dataset))
                .organization("DMA")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build()
                .toBytes();

        assertBoundingBox(catalogueOf(zipBytes), 8.0, 14.0, 54.0, 58.0);
    }

    /**
     * The most common NAVWARN extent is a single position, and the S-100 Part 17 catalogue
     * Schematron (S100_XC.sch, pattern S100_ValidBBoxPattern) asserts at error level that
     * "northBoundLatitude ... must be greater than southBoundLatitude" - and warns unless west
     * is less than east - so the point extent is padded to a strictly positive span that still
     * contains the position.
     */
    @Test
    void boundingBoxOfAPointDatasetIsPaddedToAStrictlyPositiveSpan() throws Exception {
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        Dataset dataset = newDataset("DK.S124.point-extent");
        dataset.setBoundedBy(null);
        addMemberGeometry(dataset, geometryFactory.createPoint(new Coordinate(12.5, 55.5)));

        assertPaddedAround(emittedBoundingBox(exchangeSetOf(dataset)), 12.5, 55.5);
    }

    /**
     * The same holds for an extent that is degenerate in one dimension only, such as the curve
     * of constant latitude a coastal warning is often drawn as: the latitude span is padded,
     * the longitude span is left as it is.
     */
    @Test
    void boundingBoxOfAConstantLatitudeCurveIsPaddedInLatitudeOnly() throws Exception {
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        Dataset dataset = newDataset("DK.S124.flat-extent");
        dataset.setBoundedBy(null);
        addMemberGeometry(dataset, geometryFactory.createLineString(new Coordinate[] {
                new Coordinate(8.0, 55.5), new Coordinate(14.0, 55.5) }));

        S100GeographicBoundingBoxType box = emittedBoundingBox(exchangeSetOf(dataset));
        assertThat(box.getWestBoundLongitude().getDecimal()).isEqualByComparingTo(BigDecimal.valueOf(8.0));
        assertThat(box.getEastBoundLongitude().getDecimal()).isEqualByComparingTo(BigDecimal.valueOf(14.0));
        assertThat(box.getSouthBoundLatitude().getDecimal().doubleValue())
                .isLessThan(55.5)
                .isCloseTo(55.5, within(0.0001));
        assertThat(box.getNorthBoundLatitude().getDecimal().doubleValue())
                .isGreaterThan(55.5)
                .isCloseTo(55.5, within(0.0001));
    }

    /**
     * A dataset that declares a degenerate {@code gml:boundedBy} - a "point envelope" - is
     * padded just the same: the catalogue cannot encode a bounding box of zero height either
     * way.
     */
    @Test
    void boundingBoxOfADegenerateDeclaredEnvelopeIsPadded() throws Exception {
        Dataset dataset = newDataset("DK.S124.point-envelope");
        // GML positions use lat,lon order.
        PosImpl corner = new PosImpl();
        corner.setValue(new Double[] { 55.5, 12.5 });
        dataset.getBoundedBy().getEnvelope().setLowerCorner(corner);
        dataset.getBoundedBy().getEnvelope().setUpperCorner(corner);

        assertPaddedAround(emittedBoundingBox(exchangeSetOf(dataset)), 12.5, 55.5);
    }

    /**
     * The padding stays inside the coordinate domain the same Schematron pattern asserts
     * ("latitude and longitude in decimal degrees in +/-90 or +/-180 range"): a position on a
     * pole or on the antimeridian is padded inwards instead of past the limit.
     */
    @Test
    void boundingBoxPaddingStaysWithinTheCoordinateDomain() throws Exception {
        GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        Dataset dataset = newDataset("DK.S124.pole-extent");
        dataset.setBoundedBy(null);
        addMemberGeometry(dataset, geometryFactory.createPoint(new Coordinate(180.0, 90.0)));

        S100GeographicBoundingBoxType box = emittedBoundingBox(exchangeSetOf(dataset));
        assertThat(box.getEastBoundLongitude().getDecimal().doubleValue()).isEqualTo(180.0);
        assertThat(box.getNorthBoundLatitude().getDecimal().doubleValue()).isEqualTo(90.0);
        assertThat(box.getWestBoundLongitude().getDecimal().doubleValue())
                .isCloseTo(179.9999, within(1e-9));
        assertThat(box.getSouthBoundLatitude().getDecimal().doubleValue())
                .isCloseTo(89.9999, within(1e-9));
    }

    /** Asserts a padded bounding box: strictly ordered, tight, and containing the position. */
    private static void assertPaddedAround(S100GeographicBoundingBoxType box,
            double longitude, double latitude) {
        double west = box.getWestBoundLongitude().getDecimal().doubleValue();
        double east = box.getEastBoundLongitude().getDecimal().doubleValue();
        double south = box.getSouthBoundLatitude().getDecimal().doubleValue();
        double north = box.getNorthBoundLatitude().getDecimal().doubleValue();
        assertThat(west).as("westBoundLongitude < eastBoundLongitude").isLessThan(east);
        assertThat(south).as("southBoundLatitude < northBoundLatitude").isLessThan(north);
        assertThat(longitude).as("the box contains the position").isBetween(west, east);
        assertThat(latitude).as("the box contains the position").isBetween(south, north);
        assertThat(east - west).isCloseTo(0.0001, within(1e-9));
        assertThat(north - south).isCloseTo(0.0001, within(1e-9));
    }

    /** The bounding box of the first dataset entry of the exchange set's catalogue. */
    private static S100GeographicBoundingBoxType emittedBoundingBox(byte[] zipBytes) throws Exception {
        return catalogueOf(zipBytes).getDatasetDiscoveryMetadata()
                .getS100DatasetDiscoveryMetadatas().get(0).getBoundingBox();
    }

    /** A minimal exchange set carrying a dataset that is knowingly not schema-valid. */
    private static byte[] exchangeSetOfUnvalidated(Dataset dataset) {
        return S124ExchangeSetFactory.builder()
                .datasets(List.of(dataset))
                .organization("DMA")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .validateAgainstSchema(false)
                .build()
                .toBytes();
    }

    /** A minimal exchange set carrying the given dataset. */
    private static byte[] exchangeSetOf(Dataset dataset) {
        return S124ExchangeSetFactory.builder()
                .datasets(List.of(dataset))
                .organization("DMA")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build()
                .toBytes();
    }

    /**
     * A dataset with neither an envelope nor any member geometry has no extent to describe, so
     * it is rejected rather than packaged behind a catalogue entry that lacks the element S-124
     * clause 12.2.2 makes mandatory.
     */
    @Test
    void rejectsDatasetsWithNeitherAnEnvelopeNorMemberGeometry() {
        Dataset dataset = newDataset("DK.S124.no-extent");
        dataset.setBoundedBy(null);

        S124ExchangeSetFactory factory = S124ExchangeSetFactory.builder()
                .datasets(List.of(dataset))
                .organization("DMA")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build();

        assertThatThrownBy(factory::toBytes)
                .isInstanceOf(S124ExchangeSetFactory.ExchangeSetException.class)
                .hasMessageContaining(datasetFileNameOf("DK.S124.no-extent"))
                .hasMessageContaining("boundingBox");
    }

    /**
     * S-124 clause 9.3 cancels a dataset by "Populating the cancellationDate attribute in the
     * dataset and the temporalExtent in the metadata (see 12.2.2), and that date has passed",
     * and clause 12.2.2 requires the metadata values to "align with the publicationTime and
     * cancellationDate attributes of the dataset NavwarnPreamble".
     */
    @Test
    void temporalExtentReproducesThePreambleOfASelfCancellingDataset() throws Exception {
        Dataset dataset = newDataset("DK.S124.self-cancelling");
        addPreamble(dataset,
                OffsetDateTime.parse("2026-01-15T06:00:00Z"),
                // a non-UTC offset, which the catalogue must carry as the same instant in UTC
                OffsetDateTime.parse("2026-01-20T18:00:00+02:00"));

        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .datasets(List.of(dataset))
                .organization("DMA")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build()
                .toBytes();

        S100TemporalExtent temporalExtent = catalogueOf(zipBytes)
                .getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas().get(0)
                .getTemporalExtent();

        assertThat(temporalExtent).isNotNull();
        assertThat(temporalExtent.getTimeInstantBegin())
                .isEqualTo(LocalDateTime.of(2026, 1, 15, 6, 0, 0));
        assertThat(temporalExtent.getTimeInstantEnd())
                .isEqualTo(LocalDateTime.of(2026, 1, 20, 16, 0, 0));
    }

    /**
     * S-124 clause 12.2.2: the temporal extent "is only used when a NAVWARN have a known expiry
     * date and time", so a warning whose preamble carries no cancellation date - and a dataset
     * with no preamble at all - must ship without one.
     */
    @Test
    void noTemporalExtentWithoutAKnownExpiry() throws Exception {
        Dataset withoutCancellation = newDataset("DK.S124.in-force");
        addPreamble(withoutCancellation, OffsetDateTime.parse("2026-01-15T06:00:00Z"), null);

        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .datasets(List.of(withoutCancellation, newDataset("DK.S124.no-preamble")))
                .organization("DMA")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build()
                .toBytes();

        assertThat(catalogueOf(zipBytes).getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas())
                .hasSize(2)
                .allSatisfy(meta -> assertThat(meta.getTemporalExtent()).isNull());
    }

    /** S-124 clause 9.6: "S-124 datasets must not exceed 50KB." */
    @Test
    void rejectsDatasetsExceedingTheS124SizeLimit() {
        Dataset oversized = newDataset("DK.S124.oversized");
        oversized.getDatasetIdentificationInformation().setDatasetAbstract("x".repeat(60 * 1024));

        S124ExchangeSetFactory factory = S124ExchangeSetFactory.builder()
                .datasets(List.of(oversized))
                .organization("DMA")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build();

        assertThatThrownBy(factory::toBytes)
                .isInstanceOf(S124ExchangeSetFactory.ExchangeSetException.class)
                .hasMessageContaining(datasetFileNameOf("DK.S124.oversized"))
                .hasMessageContaining("51200");
    }

    /**
     * S-100 Part 17, clause 17-4.3 (mandated for S-124 by clause 9.7), names dataset files
     * 124&lt;producer code&gt;&lt;unique code&gt;.GML, the unique code being "an arbitrary length
     * unique code in alphanumeric characters"; S-100 Part 10b Table 10b-4 makes the dataset's
     * own datasetFileIdentifier that very name. The gml:id's dots and hyphens therefore reach
     * neither the packaged file name nor the catalogue entry.
     */
    @Test
    void packagesDatasetsUnderTheirDeclaredPart17FileName() throws Exception {
        Dataset dataset = newDataset("DK.S124.naming-1");

        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .datasets(List.of(dataset))
                .organization("DMA")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build()
                .toBytes();

        String fileName = "124DK00DKS124naming1.GML";
        assertThat(dataset.getDatasetIdentificationInformation().getDatasetFileIdentifier())
                .isEqualTo(fileName);
        assertThat(unzip(zipBytes)).containsKey("S100_ROOT/S-124/DATASET_FILES/" + fileName);
        assertThat(catalogueOf(zipBytes).getDatasetDiscoveryMetadata()
                .getS100DatasetDiscoveryMetadatas().get(0).getFileName())
                .isEqualTo("file:/" + fileName);
    }

    /**
     * Without a datasetFileIdentifier there is nothing to keep the file name consistent with,
     * so it is derived from the dataset identifier - reduced to the alphanumeric unique code
     * clause 17-4.3 allows.
     * <p/>
     * Schema validation is switched off for this one case: S-100 Part 10b Table 10b-4 makes
     * datasetFileIdentifier mandatory, so a dataset omitting it is not schema-valid and the
     * derivation this test covers only ever applies to such a dataset.
     */
    @Test
    void derivesAlphanumericFileNamesForDatasetsWithoutAFileIdentifier() throws Exception {
        Dataset dataset = newDataset("DK.S124.no-identifier");
        dataset.getDatasetIdentificationInformation().setDatasetFileIdentifier(null);

        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .datasets(List.of(dataset))
                .organization("DMA")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .validateAgainstSchema(false)
                .build()
                .toBytes();

        assertThat(unzip(zipBytes)).containsKey("S100_ROOT/S-124/DATASET_FILES/124DK00DKS124noidentifier.GML");
    }

    /**
     * A declared file identifier that is not a clause 17-4.3 file name cannot be repaired by
     * renaming the packaged file: the identifier travels inside the dataset. It is rejected.
     */
    @Test
    void rejectsDatasetFileIdentifiersThatAreNotPart17FileNames() {
        Dataset dataset = newDataset("DK.S124.bad-name");
        dataset.getDatasetIdentificationInformation().setDatasetFileIdentifier("DK.S124.bad-name");

        S124ExchangeSetFactory factory = S124ExchangeSetFactory.builder()
                .datasets(List.of(dataset))
                .organization("DMA")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build();

        assertThatThrownBy(factory::toBytes)
                .isInstanceOf(S124ExchangeSetFactory.ExchangeSetException.class)
                .hasMessageContaining("DK.S124.bad-name")
                .hasMessageContaining("124DK00");
    }

    /**
     * S-100 Part 17, clause 17-4.3: "all base dataset filenames must be unique", which is what
     * makes the file URIs of the catalogue resolvable. Datasets that would collide are rejected
     * rather than silently packaged over one another.
     */
    @Test
    void rejectsDatasetsThatWouldShareAFileName() {
        S124ExchangeSetFactory factory = S124ExchangeSetFactory.builder()
                .datasets(List.of(newDataset("DK.S124.twin"), newDataset("DK.S124.twin")))
                .organization("DMA")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build();

        assertThatThrownBy(factory::toBytes)
                .isInstanceOf(S124ExchangeSetFactory.ExchangeSetException.class)
                .hasMessageContaining("124DK00DKS124twin.GML")
                .hasMessageContaining("unique");
    }

    /**
     * The producer code is the fixed width YYYY field of the clause 17-4.3 file name, issued
     * as a four character code by the IHO Producer Code Register. A code of any other length
     * or with other characters leaves the file name structure unreadable.
     */
    @Test
    void rejectsProducerCodesThatAreNotFourAlphanumericCharacters() {
        for (String producerCode : List.of("DK-00", "DK0", "DK000", "")) {
            S124ExchangeSetFactory.Builder builder = S124ExchangeSetFactory.builder()
                    .datasets(List.of(newDataset("DK.S124.producer-code")))
                    .organization("DMA")
                    .producerCode(producerCode)
                    .certificatePem(testCertPem)
                    .signer((alg, payload) -> DUMMY_SIGNATURE);

            assertThatThrownBy(builder::build)
                    .as("producerCode \"%s\"", producerCode)
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("producerCode");
        }
    }

    /**
     * S-100 Part 15, clause 15-8.7: "The digitalSignatureReference field must be encoded
     * 'ECDSA-384-SHA2'." No other algorithm may be configured.
     */
    @Test
    void rejectsSignatureAlgorithmsOtherThanTheFixedValue() {
        S124ExchangeSetFactory.Builder builder = S124ExchangeSetFactory.builder();

        assertThatThrownBy(() -> builder.signatureAlgorithm(S100SEDigitalSignatureReference.DSA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ECDSA-384-SHA2");
        assertThat(builder.signatureAlgorithm(S100SEDigitalSignatureReference.ECDSA_384_SHA_2))
                .isSameAs(builder);
    }

    @Test
    void rejectsSpecificUsageOtherThanTheFixedValue() {
        S124ExchangeSetFactory.Builder builder = S124ExchangeSetFactory.builder();

        assertThatThrownBy(() -> builder.specificUsage("testing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Navigational Warning Service");
        assertThat(builder.specificUsage(S124ExchangeSetFactory.SPECIFIC_USAGE)).isSameAs(builder);
    }

    /**
     * The generated CATALOG.XML must validate against the S-100 5.2.0 exchange
     * catalogue XSD. Guards against regressions of the basic-form ISO dates,
     * empty gco:CharacterStrings, missing codeList attributes and abstract
     * gml:AbstractRing elements that used to make every catalogue schema-invalid.
     */
    @Test
    void catalogueIsSchemaValid() throws Exception {
        Dataset dataset = newDataset("DK.S124.xsd-validity");

        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .datasets(List.of(dataset))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .emails(List.of("nautinf@dma.dk"))
                .phone("+4572196000")
                .city("Korsoer")
                .postalCode("4220")
                .country("Denmark")
                .build()
                .toBytes();

        String catalogXml = new String(unzip(zipBytes).get("S100_ROOT/CATALOG.XML"), StandardCharsets.UTF_8);

        assertThat(validateAgainstCatalogueSchema(catalogXml))
                .as("XSD validation errors in CATALOG.XML:\n%s", catalogXml)
                .isEmpty();
    }

    /**
     * A catalogue built from the bare mandatory factory configuration (no phone,
     * address, emails or dataset abstract) must be schema-valid too: absent
     * optional details must be omitted rather than emitted as empty elements.
     */
    @Test
    void catalogueWithMinimalContactDetailsIsSchemaValid() throws Exception {
        Dataset dataset = newDataset("DK.S124.xsd-validity-minimal");
        dataset.getDatasetIdentificationInformation().setDatasetAbstract(null);

        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .datasets(List.of(dataset))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build()
                .toBytes();

        String catalogXml = new String(unzip(zipBytes).get("S100_ROOT/CATALOG.XML"), StandardCharsets.UTF_8);

        assertThat(validateAgainstCatalogueSchema(catalogXml))
                .as("XSD validation errors in CATALOG.XML:\n%s", catalogXml)
                .isEmpty();
    }

    /**
     * A catalogue that carries a fileless cancellation entry alongside an
     * active dataset must be schema-valid as well.
     */
    @Test
    void catalogueWithCancellationIsSchemaValid() throws Exception {
        S124ExchangeSetFactory.Cancellation cancellation =
                cancellationOf(newDataset("DK.S124.xsd-validity-cancelled"));

        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .datasets(List.of(newDataset("DK.S124.xsd-validity-active")))
                .cancellations(List.of(cancellation))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build()
                .toBytes();

        String catalogXml = new String(unzip(zipBytes).get("S100_ROOT/CATALOG.XML"), StandardCharsets.UTF_8);

        assertThat(validateAgainstCatalogueSchema(catalogXml))
                .as("XSD validation errors in CATALOG.XML:\n%s", catalogXml)
                .isEmpty();
    }

    /**
     * Validates against the exchange catalogue XSD in the sibling s-100 module.
     * Loading the schema from the filesystem lets its relative ISO 19115-3
     * imports resolve naturally; the GML/xlink imports resolve over the network,
     * exactly as the xjc code generation for these modules already does.
     */
    private static List<String> validateAgainstCatalogueSchema(String xml) throws Exception {
        return validateAgainst(CatalogueSchemaHolder.SCHEMA, xml);
    }

    /** Validates against the Part 15 signature/encryption schema in the sibling s-100 module. */
    private static List<String> validateAgainstSignatureSchema(String xml) throws Exception {
        return validateAgainst(SignatureSchemaHolder.SCHEMA, xml);
    }

    private static List<String> validateAgainst(Schema schema, String xml) throws Exception {
        List<String> errors = new ArrayList<>();
        Validator validator = schema.newValidator();
        validator.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException e) {
            }

            @Override
            public void error(SAXParseException e) {
                errors.add("line " + e.getLineNumber() + ": " + e.getMessage());
            }

            @Override
            public void fatalError(SAXParseException e) {
                errors.add("fatal, line " + e.getLineNumber() + ": " + e.getMessage());
            }
        });
        validator.validate(new StreamSource(new StringReader(xml)));
        return errors;
    }

    /** Lazily compiles the exchange catalogue schema once for all tests. */
    private static final class CatalogueSchemaHolder {
        static final Schema SCHEMA = compile("xsd/S100Catalog/20240415/S100_ExchangeCatalogue.xsd");
    }

    /** Lazily compiles the Part 15 signature schema (CATALOG.SIGN) once for all tests. */
    private static final class SignatureSchemaHolder {
        static final Schema SCHEMA = compile("xsd/S100SE/20240415/Part15.xsd");
    }

    private static Schema compile(String schemaResource) {
        Path schemaPath = Path.of("../s-100/src/main/resources/" + schemaResource);
        if (!Files.exists(schemaPath)) {
            // running with the repository root as working directory (e.g. from an IDE)
            schemaPath = Path.of("s-100/src/main/resources/" + schemaResource);
        }
        assertThat(schemaPath).as("schema %s must be present", schemaResource).exists();
        try {
            return SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                    .newSchema(schemaPath.toFile());
        } catch (org.xml.sax.SAXException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Test
    void buildsMultipleDatasets() throws Exception {
        Dataset a = newDataset("DK.S124.a");
        Dataset b = newDataset("DK.S124.b");

        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .datasets(List.of(a, b))
                .organization("DMA")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build()
                .toBytes();

        long datasetFiles = unzip(zipBytes).keySet().stream()
                .filter(n -> n.startsWith("S100_ROOT/S-124/DATASET_FILES/") && n.endsWith(".GML"))
                .count();
        assertThat(datasetFiles).isEqualTo(2);
    }

    @Test
    void requiresAllMandatoryFields() {
        S124ExchangeSetFactory.Builder b = S124ExchangeSetFactory.builder()
                .datasets(List.of(newDataset("DK.S124.req")))
                .organization("DMA")
                .producerCode("DK00")
                .certificatePem(testCertPem);

        assertThatThrownBy(b::build)
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("signer");
    }

    @Test
    void emitsFilelessCancellationReusingOriginalSignature() throws Exception {
        // 1. Build an "original" exchange set for a dataset and read back its discovery metadata.
        Dataset original = newDataset("DK.S124.cancel-me");
        byte[] originalZip = S124ExchangeSetFactory.builder()
                .datasets(List.of(original))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build()
                .toBytes();

        S100DatasetDiscoveryMetadata originalMeta = catalogueOf(originalZip)
                .getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas().get(0);
        assertThat(originalMeta.getPurpose()).isEqualTo(S100Purpose.NEW_DATASET);

        // 2. Cancel it: reuse the original file name and signature, but ship no file. Per
        // S-100 clause 17-4.4.1 the entry carries the issue date of the cancellation itself.
        String originalFileName = originalMeta.getFileName().substring("file:/".length());
        LocalDate cancellationDate = originalMeta.getIssueDate().plusDays(3);
        S124ExchangeSetFactory.Cancellation cancellation =
                new S124ExchangeSetFactory.Cancellation(originalMeta, cancellationDate);

        Dataset stillActive = newDataset("DK.S124.still-active");
        AtomicInteger signCalls = new AtomicInteger();
        byte[] zip = S124ExchangeSetFactory.builder()
                .datasets(List.of(stillActive))
                .cancellations(List.of(cancellation))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> { signCalls.incrementAndGet(); return DUMMY_SIGNATURE; })
                .phone("+4572196000")
                .build()
                .toBytes();

        Map<String, byte[]> entries = unzip(zip);

        // Fileless: only the still-active dataset yields a DATASET_FILES entry — the cancellation does not.
        assertThat(datasetFileCount(entries)).isEqualTo(1);

        // Signer runs once for the active dataset file + once for CATALOG.XML — NOT for the
        // cancellation, which reuses the supplied original signature.
        assertThat(signCalls.get()).isEqualTo(2);

        List<S100DatasetDiscoveryMetadata> meta = catalogueOf(zip)
                .getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas();
        assertThat(meta).hasSize(2);

        S100DatasetDiscoveryMetadata cancelEntry = meta.stream()
                .filter(m -> m.getPurpose() == S100Purpose.CANCELLATION)
                .findFirst()
                .orElseThrow(() -> new AssertionError("no cancellation entry in catalogue"));
        assertThat(cancelEntry.getFileName()).isEqualTo("file:/" + originalFileName);
        assertThat(cancelEntry.getDatasetID()).isEqualTo(originalMeta.getDatasetID());
        assertThat(cancelEntry.getEditionNumber()).isEqualTo(originalMeta.getEditionNumber());
        assertThat(cancelEntry.getUpdateNumber()).isEqualTo(originalMeta.getUpdateNumber());
        // S-100 clause 17-4.4.1 excepts issueDate from "same values as the original": it is the
        // issue date of the fileless cancellation itself.
        assertThat(cancelEntry.getIssueDate()).isEqualTo(cancellationDate);
        // boundingBox is mandatory in the S-124 profile, also for a cancellation entry.
        assertThat(cancelEntry.getBoundingBox()).isNotNull();
        // At least one digital signature is present (the catalogue schema mandates it) and it is the reused one.
        assertThat(cancelEntry.getDigitalSignatureValues()).hasSize(1);

        // The other entry is the still-active dataset, marked newDataset.
        assertThat(meta.stream().filter(m -> m.getPurpose() == S100Purpose.NEW_DATASET).count()).isEqualTo(1);
    }

    @Test
    void buildsCancellationOnlyExchangeSet() throws Exception {
        S124ExchangeSetFactory.Cancellation cancellation = cancellationOf(newDataset("DK.S124.seed"));

        byte[] zip = S124ExchangeSetFactory.builder()
                .cancellations(List.of(cancellation)) // no datasets at all
                .organization("DMA")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .build()
                .toBytes();

        Map<String, byte[]> entries = unzip(zip);
        assertThat(datasetFileCount(entries)).isZero();

        List<S100DatasetDiscoveryMetadata> meta = catalogueOf(zip)
                .getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas();
        assertThat(meta).hasSize(1);
        assertThat(meta.get(0).getPurpose()).isEqualTo(S100Purpose.CANCELLATION);
    }

    @Test
    void cancellationRequiresOriginalSignature() {
        S100DatasetDiscoveryMetadata withoutSignature = new S100DatasetDiscoveryMetadata();
        withoutSignature.setFileName("file:/124DK00x-0.GML");

        assertThatThrownBy(() -> new S124ExchangeSetFactory.Cancellation(
                withoutSignature, LocalDate.of(2026, 1, 15)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("digital signature");
    }

    /** The entry identifies the resource being cancelled by its file name (clause 17-4.4.1). */
    @Test
    void cancellationRequiresOriginalFileName() throws Exception {
        S100DatasetDiscoveryMetadata.DigitalSignatureValue signature =
                cancellationOf(newDataset("DK.S124.name-check")).original().getDigitalSignatureValues().get(0);
        S100DatasetDiscoveryMetadata withoutFileName = new S100DatasetDiscoveryMetadata();
        withoutFileName.getDigitalSignatureValues().add(signature);

        assertThatThrownBy(() -> new S124ExchangeSetFactory.Cancellation(
                withoutFileName, LocalDate.of(2026, 1, 15)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("file name");
    }

    @Test
    void buildRequiresAtLeastOneDatasetOrCancellation() {
        S124ExchangeSetFactory.Builder b = S124ExchangeSetFactory.builder()
                .organization("DMA")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE);

        assertThatThrownBy(b::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one dataset or cancellation");
    }

    // ---------------------------------------------------------------------------------------
    // What a producer keeps in order to withdraw a dataset later (S-100 Part 17, clause 17-4.4.1)
    // ---------------------------------------------------------------------------------------

    /**
     * The entry handed back is the entry that was marshalled, not one equivalent to it. This is
     * the permanent guard on the contract S100ExchangeCatalogueBuilder.build() keeps but does not
     * document: it invokes each registered metadata provider once and adds what the provider
     * returns to the catalogue unmutated, so the object captured inside the provider is the object
     * CATALOG.XML carries. That invariant is also what makes handing the entries back from the
     * build correct at all - a producer persisting an entry the exchange set does not contain
     * would later cancel a dataset nobody received. Do not delete this test.
     */
    @Test
    void toExchangeSetReturnsTheEntryThatWasMarshalledIntoTheCatalogue() throws Exception {
        S124ExchangeSetFactory.ExchangeSet set = publisher()
                .datasets(List.of(newDataset("DK.S124.captured")))
                .build()
                .toExchangeSet();

        assertThat(set.datasets()).hasSize(1);
        assertThat(marshal(set.datasets().get(0).discoveryMetadata()))
                .isEqualTo(marshal(firstEntryOf(set.bytes())));
    }

    /**
     * The published datasets come back at the index their dataset has in the builder, and each
     * one carries the very object the caller handed over - the only join that always works,
     * since a dataset declaring neither a gml:id nor a datasetFileIdentifier is packaged under a
     * randomly drawn clause 17-4.3 unique code the caller could not have predicted.
     */
    @Test
    void publishedDatasetsAreInBuilderOrderAndJoinBackByIdentity() throws Exception {
        Dataset first = newDataset("DK.S124.first");
        Dataset unnamed = newDataset("DK.S124.middle");
        unnamed.setId(null);
        unnamed.getDatasetIdentificationInformation().setDatasetFileIdentifier(null);
        Dataset last = newDataset("DK.S124.last");
        List<Dataset> input = List.of(first, unnamed, last);

        S124ExchangeSetFactory.ExchangeSet set = publisher()
                .datasets(input)
                // the middle dataset carries no gml:id, which the GML schema requires
                .validateAgainstSchema(false)
                .build()
                .toExchangeSet();

        assertThat(set.datasets()).hasSize(3);
        Map<String, byte[]> entries = unzip(set.bytes());
        for (int i = 0; i < input.size(); i++) {
            S124ExchangeSetFactory.PublishedDataset published = set.datasets().get(i);
            assertThat(published.dataset()).isSameAs(input.get(i));
            assertThat(entries).containsKey("S100_ROOT/S-124/DATASET_FILES/" + published.fileName());
        }
        assertThat(set.datasets().get(1).fileName())
                .as("the randomly coded file name is readable nowhere else")
                .isNotEqualTo(datasetFileNameOf("DK.S124.middle"));
    }

    /**
     * The name is the bare clause 17-4.3 file name, while the catalogue's fileName is typed
     * xs:anyURI and carries the file:/ form that clause 17-4.4.1 has a cancellation reproduce
     * verbatim. Both are needed, so the relation between them is fixed.
     */
    @Test
    void publishedFileNameIsTheBareNameTheCatalogueCarriesAsAUri() throws Exception {
        S124ExchangeSetFactory.PublishedDataset published = publisher()
                .datasets(List.of(newDataset("DK.S124.bare-name")))
                .build()
                .toExchangeSet()
                .datasets()
                .get(0);

        assertThat(published.fileName())
                .isEqualTo(datasetFileNameOf("DK.S124.bare-name"))
                .doesNotContain("/")
                .doesNotStartWith("file:");
        assertThat(published.discoveryMetadata().getFileName())
                .isEqualTo("file:/" + published.fileName());
    }

    /**
     * A fileless cancellation publishes nothing, so there is nothing new to record. The signing
     * chain still comes back: the exchange set is signed like any other.
     */
    @Test
    void cancellationOnlyExchangeSetPublishesNoDatasets() throws Exception {
        S124ExchangeSetFactory.ExchangeSet set = publisher()
                .cancellations(List.of(cancellationOf(newDataset("DK.S124.only-cancel"))))
                .build()
                .toExchangeSet();

        assertThat(set.datasets()).isEmpty();
        assertThat(set.signingCertificatePems()).containsExactly(testCertPem);
        assertThat(datasetFileCount(unzip(set.bytes()))).isZero();
        List<S100DatasetDiscoveryMetadata> meta = catalogueOf(set.bytes())
                .getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas();
        assertThat(meta).hasSize(1);
        assertThat(meta.get(0).getPurpose()).isEqualTo(S100Purpose.CANCELLATION);
    }

    /**
     * toBytes() now delegates to toExchangeSet(), and must still build what it always built. The
     * two artefacts cannot be compared byte for byte - each build stamps a fresh time and asks
     * the signer again - so the comparison is of everything the layout fixes.
     */
    @Test
    void toBytesProducesTheSameArtefactShapeAsToExchangeSet() throws Exception {
        S124ExchangeSetFactory factory = publisher()
                .datasets(List.of(newDataset("DK.S124.shape-a"), newDataset("DK.S124.shape-b")))
                .build();

        Map<String, byte[]> fromBytes = unzip(factory.toBytes());
        S124ExchangeSetFactory.ExchangeSet set = factory.toExchangeSet();

        assertThat(unzip(set.bytes()).keySet()).isEqualTo(fromBytes.keySet());
        assertThat(set.datasets().stream().map(S124ExchangeSetFactory.PublishedDataset::fileName))
                .containsExactly(datasetFileNameOf("DK.S124.shape-a"), datasetFileNameOf("DK.S124.shape-b"));
    }

    /**
     * The chain is the one the Data Server signed with, leaf first: exactly the form
     * {@link S124ExchangeSetFactory.Cancellation} takes it in, so a producer can persist it as it
     * comes and hand it back unaltered.
     */
    @Test
    void signingCertificatePemsAreTheDataServerChainSigningCertificateFirst() throws Exception {
        S124ExchangeSetFactory.ExchangeSet set = publisher()
                .datasets(List.of(newDataset("DK.S124.chain")))
                .certificatePem(dataServerViaDcPem)
                .intermediateCertificatePems(List.of(domainCoordinatorPem))
                .build()
                .toExchangeSet();

        assertThat(set.signingCertificatePems())
                .containsExactly(dataServerViaDcPem, domainCoordinatorPem);
    }

    /**
     * The reason the chain is handed back at all: the entry records only the id its own catalogue
     * gave the certificate, so a producer holding just the entry cannot satisfy S-100 Part 15,
     * clause 15-8.7 - "all the certificates required to perform a full certificate path validation
     * without any external access" - once the key has rotated. Everything this cancellation needs
     * comes out of the earlier build result.
     */
    @Test
    void theReturnedChainCancelsAfterTheCertificateHasRotated() throws Exception {
        S124ExchangeSetFactory.ExchangeSet published = publisher()
                .datasets(List.of(newDataset("DK.S124.rotate-from-result")))
                .certificatePem(dataServerViaDcPem)
                .intermediateCertificatePems(List.of(domainCoordinatorPem))
                .build()
                .toExchangeSet();
        S100DatasetDiscoveryMetadata retainedEntry = published.datasets().get(0).discoveryMetadata();
        List<String> retainedChain = published.signingCertificatePems();

        // The producer has since rotated to an unrelated Data Server certificate.
        byte[] zip = cancellationExchangeSet(new S124ExchangeSetFactory.Cancellation(
                retainedEntry, retainedEntry.getIssueDate().plusDays(30), retainedChain),
                testCertPem, List.of());

        S100ExchangeCatalogue catalogue = catalogueOf(zip);
        Map<String, String> pemById = catalogue.getCertificates().get(0).getCertificates().stream()
                .collect(Collectors.toMap(S100SECertificateType::getId,
                        S124ExchangeSetFactoryTest::pemOf));
        String ref = dataSignatureOf(catalogue.getDatasetDiscoveryMetadata()
                .getS100DatasetDiscoveryMetadatas().get(0)).getCertificateRef();

        assertThat(pemById.get(ref)).isEqualTo(dataServerViaDcPem);
        assertThat(pemById.get("cer1")).as("the current certificate signs the new catalogue")
                .isEqualTo(testCertPem);
        assertThat(pemById).as("the rotated-out leaf's own issuer travels with it")
                .containsValue(domainCoordinatorPem);
    }

    /**
     * The whole point, end to end: publish, persist the entry as a producer's database column
     * holds it, and cancel from that column alone - without unzipping anything or knowing that
     * an exchange catalogue exists. Clause 17-4.4.1 excepts the issue date and the purpose from
     * "the same values as the original"; everything else must be reproduced, and is.
     */
    @Test
    void publishThenPersistThenCancelWithoutTouchingTheZip() throws Exception {
        S124ExchangeSetFactory.ExchangeSet published = publisher()
                .datasets(List.of(newDataset("DK.S124.end-to-end")))
                .build()
                .toExchangeSet();
        S124ExchangeSetFactory.PublishedDataset row = published.datasets().get(0);
        String storedMetadata = S124ExchangeSetFactory.discoveryMetadataToXml(row.discoveryMetadata());
        List<String> storedChain = published.signingCertificatePems();

        S100DatasetDiscoveryMetadata original =
                S124ExchangeSetFactory.discoveryMetadataFromXml(storedMetadata);
        LocalDate cancelledOn = original.getIssueDate().plusMonths(4);
        byte[] cancellationZip = publisher()
                .cancellations(List.of(new S124ExchangeSetFactory.Cancellation(
                        original, cancelledOn, storedChain)))
                .build()
                .toBytes();

        // Only the assertions read the shipped artefact; the producer flow above never did.
        S100DatasetDiscoveryMetadata emitted = firstEntryOf(cancellationZip);
        assertThat(emitted.getPurpose()).isEqualTo(S100Purpose.CANCELLATION);
        assertThat(emitted.getIssueDate()).isEqualTo(cancelledOn);
        assertThat(emitted.getFileName()).isEqualTo("file:/" + row.fileName());
        S100SESignatureOnData reused = dataSignatureOf(emitted);
        S100SESignatureOnData publishedSignature = dataSignatureOf(row.discoveryMetadata());
        assertThat(reused.getValue())
                .as("a fileless cancellation reuses the original signature")
                .isEqualTo(publishedSignature.getValue());
        assertThat(reused.getId()).isEqualTo(publishedSignature.getId());
        assertThat(reused.getDataStatus()).isEqualTo(publishedSignature.getDataStatus());
        assertThat(reused.getCertificateRef())
                .as("the signing certificate has not changed, so the entry keeps its id")
                .isEqualTo(publishedSignature.getCertificateRef());

        // Normalise the two fields the clause excepts, and the rest of the entry is the same
        // document. The signature is compared above rather than as text: the published entry
        // writes the substitution group head with an xsi:type and the reproduced one the
        // S100_SE_SignatureOnData member element, which is the same signature to a reader.
        S100DatasetDiscoveryMetadata expected =
                S124ExchangeSetFactory.discoveryMetadataFromXml(storedMetadata);
        expected.setPurpose(S100Purpose.CANCELLATION);
        expected.setIssueDate(cancelledOn);
        assertThat(withoutSignature(marshal(emitted))).isEqualTo(withoutSignature(marshal(expected)));
    }

    /** A marshalled discovery metadata entry with its signature elements removed. */
    private static String withoutSignature(String entryXml) {
        return entryXml.replaceAll("(?s)<digitalSignatureValue>.*?</digitalSignatureValue>", "");
    }

    /**
     * The persisted form has to survive the database round trip whole: the signature is what
     * clause 17-4.4.1 reuses, and its certificateRef, dataStatus and concrete
     * S100_SE_SignatureOnData type are what a Part 15 reader authenticates it by.
     */
    @Test
    void discoveryMetadataXmlRoundTripPreservesTheSignature() throws Exception {
        S100DatasetDiscoveryMetadata entry = publisher()
                .datasets(List.of(newDataset("DK.S124.persisted")))
                .build()
                .toExchangeSet()
                .datasets()
                .get(0)
                .discoveryMetadata();
        String xml = S124ExchangeSetFactory.discoveryMetadataToXml(entry);

        S100DatasetDiscoveryMetadata restored = S124ExchangeSetFactory.discoveryMetadataFromXml(xml);

        S100SEDigitalSignature signature = restored.getDigitalSignatureValues().get(0)
                .getS100SEDigitalSignature().getValue();
        assertThat(signature).isInstanceOf(S100SESignatureOnData.class);
        S100SESignatureOnData onData = (S100SESignatureOnData) signature;
        assertThat(onData.getValue()).isEqualTo(dataSignatureOf(entry).getValue());
        assertThat(onData.getId()).isEqualTo(dataSignatureOf(entry).getId());
        assertThat(onData.getCertificateRef()).isEqualTo(dataSignatureOf(entry).getCertificateRef());
        assertThat(onData.getDataStatus()).isEqualTo(DataStatus.UNENCRYPTED);
        assertThat(S124ExchangeSetFactory.discoveryMetadataToXml(restored)).isEqualTo(xml);
    }

    /**
     * The persisted form is XML text and a Danish producer's free text is not ASCII, so the
     * entry is written and read back as the UTF-8 the marshaller declares rather than through
     * whatever the JVM's default charset happens to be. Clause 17-4.4.1 requires the
     * cancellation to reproduce the original's metadata fields, and a producing agency name or
     * dataset comment that came back mangled from the producer's database would be re-signed
     * into the cancelling catalogue in that mangled form.
     */
    @Test
    void discoveryMetadataXmlRoundTripPreservesNonAsciiFreeText() throws Exception {
        String agency = "S\u00f8fartsstyrelsen";
        String comment = "\u00d8resund og L\u00e6s\u00f8 Rende - grundst\u00f8dning ved \u00c6r\u00f8";
        S100DatasetDiscoveryMetadata entry = publisher()
                .organization(agency)
                .datasetComment(comment)
                .datasets(List.of(newDataset("DK.S124.non-ascii")))
                .build()
                .toExchangeSet()
                .datasets()
                .get(0)
                .discoveryMetadata();

        S100DatasetDiscoveryMetadata restored = S124ExchangeSetFactory.discoveryMetadataFromXml(
                S124ExchangeSetFactory.discoveryMetadataToXml(entry));

        assertThat(organisationNameOf(restored)).isEqualTo(agency);
        assertThat(restored.getComment().getCharacterString().getValue()).isEqualTo(comment);
    }

    /** The name of the organisation the entry's mandatory producingAgency block names. */
    private static String organisationNameOf(S100DatasetDiscoveryMetadata metadata) {
        org.iso.standards.iso._19115.__3.cit._2.CIOrganisationType organisation =
                (org.iso.standards.iso._19115.__3.cit._2.CIOrganisationType) metadata.getProducingAgency()
                        .getCIResponsibility().getParties().get(0).getAbstractCIParty().getValue();
        return (String) organisation.getName().getCharacterString().getValue();
    }

    /**
     * Reading the entry back is a step in building the next exchange set, so a half-written
     * column raises the exception every caller of this factory already handles rather than a
     * checked JAXBException a transactional service method would have to catch.
     */
    @Test
    void discoveryMetadataFromXmlRejectsATruncatedEntry() throws Exception {
        String xml = S124ExchangeSetFactory.discoveryMetadataToXml(publisher()
                .datasets(List.of(newDataset("DK.S124.truncated")))
                .build()
                .toExchangeSet()
                .datasets()
                .get(0)
                .discoveryMetadata());
        String half = xml.substring(0, xml.length() / 2);

        assertThatThrownBy(() -> S124ExchangeSetFactory.discoveryMetadataFromXml(half))
                .isInstanceOf(S124ExchangeSetFactory.ExchangeSetException.class)
                .hasMessageContaining("discovery metadata")
                .hasCauseInstanceOf(jakarta.xml.bind.JAXBException.class);
    }

    /**
     * The back-fill path for datasets published before the entries were handed back: the shipped
     * catalogue is the only faithful record of what clause 17-4.4.1 requires a cancellation to
     * reproduce, and the file name is the only key a producer still holds.
     */
    @Test
    void readDiscoveryMetadataRecoversTheEntriesOfABuiltExchangeSet() throws Exception {
        S124ExchangeSetFactory.ExchangeSet set = publisher()
                .datasets(List.of(newDataset("DK.S124.archived-a"), newDataset("DK.S124.archived-b")))
                .build()
                .toExchangeSet();

        Map<String, S100DatasetDiscoveryMetadata> recovered =
                S124ExchangeSetFactory.readDiscoveryMetadata(set.bytes());

        assertThat(recovered.keySet()).containsExactly(
                datasetFileNameOf("DK.S124.archived-a"), datasetFileNameOf("DK.S124.archived-b"));
        for (S124ExchangeSetFactory.PublishedDataset published : set.datasets()) {
            assertThat(marshal(recovered.get(published.fileName())))
                    .isEqualTo(marshal(published.discoveryMetadata()));
        }
    }

    /**
     * Only the newDataset entries come back. A cancellation entry withdraws a dataset rather than
     * publishing one, can never be the original of a further cancellation, and carries the file
     * name of the entry it cancels - so it would also collide on the key.
     */
    @Test
    void readDiscoveryMetadataSkipsCancellationEntries() throws Exception {
        Dataset active = newDataset("DK.S124.still-published");
        byte[] withUnrelatedCancellation = publisher()
                .datasets(List.of(active))
                .cancellations(List.of(cancellationOf(newDataset("DK.S124.withdrawn"))))
                .build()
                .toBytes();

        assertThat(S124ExchangeSetFactory.readDiscoveryMetadata(withUnrelatedCancellation).keySet())
                .containsExactly(datasetFileNameOf("DK.S124.still-published"));

        // The same file name published and cancelled in one build: still one key, the live entry.
        Dataset republished = newDataset("DK.S124.same-name");
        byte[] withCollidingCancellation = publisher()
                .datasets(List.of(republished))
                .cancellations(List.of(cancellationOf(newDataset("DK.S124.same-name"))))
                .build()
                .toBytes();

        Map<String, S100DatasetDiscoveryMetadata> recovered =
                S124ExchangeSetFactory.readDiscoveryMetadata(withCollidingCancellation);
        assertThat(recovered).hasSize(1);
        assertThat(recovered.get(datasetFileNameOf("DK.S124.same-name")).getPurpose())
                .isEqualTo(S100Purpose.NEW_DATASET);
    }

    @Test
    void readDiscoveryMetadataRejectsAZipWithoutACatalogue() throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(out)) {
            zos.putNextEntry(new ZipEntry("readme.txt"));
            zos.write("not an exchange set".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        byte[] zip = out.toByteArray();

        assertThatThrownBy(() -> S124ExchangeSetFactory.readDiscoveryMetadata(zip))
                .isInstanceOf(S124ExchangeSetFactory.ExchangeSetException.class)
                .hasMessageContaining("S100_ROOT/CATALOG.XML");
    }

    /**
     * ExchangeSet.datasets() promises an entry at the index each dataset has in
     * Builder.datasets(List), so what the factory packages is fixed when it is built rather than
     * read off a list the caller may since have added to.
     */
    @Test
    void datasetsAreSnapshotWhenTheFactoryIsBuilt() throws Exception {
        List<Dataset> datasets = new ArrayList<>(List.of(newDataset("DK.S124.snapshot")));
        S124ExchangeSetFactory factory = publisher().datasets(datasets).build();

        datasets.add(newDataset("DK.S124.added-after-build"));

        S124ExchangeSetFactory.ExchangeSet set = factory.toExchangeSet();
        assertThat(set.datasets()).hasSize(1);
        assertThat(set.datasets().get(0).fileName()).isEqualTo(datasetFileNameOf("DK.S124.snapshot"));
        assertThat(datasetFileCount(unzip(set.bytes()))).isEqualTo(1);
    }

    /** Builds a real exchange set for the given dataset and derives a fileless {@link S124ExchangeSetFactory.Cancellation} of it. */

    private static S124ExchangeSetFactory.Cancellation cancellationOf(Dataset dataset) throws Exception {
        return cancellationOf(dataset, List.of());
    }

    /**
     * Publishes {@code dataset}, then builds a cancellation reproducing its catalogue entry -
     * taking that entry from the build result, the way a producer does, rather than unzipping
     * the exchange set to recover it.
     */
    private static S124ExchangeSetFactory.Cancellation cancellationOf(
            Dataset dataset, List<String> certificatePems) throws Exception {
        S100DatasetDiscoveryMetadata original = publisher()
                .datasets(List.of(dataset))
                .build()
                .toExchangeSet()
                .datasets()
                .get(0)
                .discoveryMetadata();
        return new S124ExchangeSetFactory.Cancellation(
                original, original.getIssueDate().plusDays(1), certificatePems);
    }

    /**
     * A builder kept for a second exchange set - the only reason a fluent builder is reusable at
     * all - must not change an exchange set already built from it. The factory reads its
     * configuration when the ZIP is packaged rather than when {@code build()} returns, so before
     * the snapshot this published the wrong dataset: the caller asked for the first and got the
     * second, packaged and signed under the first exchange set's identity.
     */
    @Test
    void reusingTheBuilderDoesNotChangeAnAlreadyBuiltFactory() throws Exception {
        Dataset first = newDataset("DK.S124.builder-reuse-first");
        Dataset second = newDataset("DK.S124.builder-reuse-second");
        S124ExchangeSetFactory.Builder builder = publisher().datasets(List.of(first));
        S124ExchangeSetFactory factory = builder.build();

        builder.datasets(List.of(second));

        assertThat(factory.toExchangeSet().datasets())
                .extracting(S124ExchangeSetFactory.PublishedDataset::dataset)
                .containsExactly(first);
    }

    /** The same guarantee one level down: the list itself is copied, not aliased. */
    @Test
    void mutatingTheDatasetListAfterBuildDoesNotChangeWhatIsPackaged() throws Exception {
        List<Dataset> datasets = new ArrayList<>(List.of(newDataset("DK.S124.list-copy")));
        S124ExchangeSetFactory factory = publisher().datasets(datasets).build();

        datasets.add(newDataset("DK.S124.list-copy-added"));

        assertThat(factory.toExchangeSet().datasets()).hasSize(1);
    }

    /**
     * Each {@code build()} is a distinct exchange set and gets a distinct identifier. The generated
     * default used to be written back onto the builder, so a second exchange set built from it
     * inherited the first one's MRN - two artefacts claiming one identity.
     */
    @Test
    void eachBuildFromOneBuilderGetsItsOwnExchangeSetIdentifier() throws Exception {
        S124ExchangeSetFactory.Builder builder =
                publisher().datasets(List.of(newDataset("DK.S124.identifier")));

        String first = catalogueOf(builder.build().toBytes()).getIdentifier().getIdentifier();
        String second = catalogueOf(builder.build().toBytes()).getIdentifier().getIdentifier();

        assertThat(first).startsWith("urn:mrn:iho:s124:exchangeset:").isNotEqualTo(second);
    }

    /**
     * The snapshot is written field by field, so a field added to the builder and forgotten there
     * would go back to being read live off the caller's builder - silently, and only for callers
     * who set it. This walks the declared fields rather than listing them: each is given a value
     * the copy cannot produce by accident, and the snapshot has to carry it.
     */
    @Test
    void everyBuilderFieldIsCarriedIntoTheSnapshot() throws Exception {
        S124ExchangeSetFactory.Builder builder = S124ExchangeSetFactory.builder();
        List<Field> fields = new ArrayList<>();
        for (Field field : S124ExchangeSetFactory.Builder.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }
            field.setAccessible(true);
            field.set(builder, sentinelFor(field, field.get(builder)));
            fields.add(field);
        }
        Constructor<S124ExchangeSetFactory.Builder> copyConstructor = S124ExchangeSetFactory.Builder.class
                .getDeclaredConstructor(S124ExchangeSetFactory.Builder.class);
        copyConstructor.setAccessible(true);

        S124ExchangeSetFactory.Builder snapshot = copyConstructor.newInstance(builder);

        assertThat(fields).as("the builder declares fields to copy").isNotEmpty();
        for (Field field : fields) {
            assertThat(field.get(snapshot))
                    .as("Builder.%s is not carried into the snapshot build() hands the factory",
                            field.getName())
                    .isSameAs(field.get(builder));
        }
    }

    /**
     * A value a forgotten copy cannot reproduce: null where the field declares a non-null default,
     * a distinct object where it declares none, the other value for a boolean.
     */
    private static Object sentinelFor(Field field, Object current) {
        Class<?> type = field.getType();
        if (type == boolean.class) {
            return !((boolean) current);
        }
        if (current != null) {
            return null;
        }
        if (type == String.class) {
            return "sentinel:" + field.getName();
        }
        if (type.isInterface()) {
            return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "toString" -> "sentinel:" + field.getName();
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> null;
                    });
        }
        throw new AssertionError(String.format(
                "extend sentinelFor to give %s %s a value the copy cannot produce by accident",
                type.getName(), field.getName()));
    }

    /** A factory builder carrying the mandatory details of the test producer. */
    private static S124ExchangeSetFactory.Builder publisher() {
        return S124ExchangeSetFactory.builder()
                .organization("DMA")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000");
    }

    private static String marshal(S100DatasetDiscoveryMetadata metadata) throws Exception {
        jakarta.xml.bind.JAXBContext context =
                jakarta.xml.bind.JAXBContext.newInstance(S100DatasetDiscoveryMetadata.class);
        java.io.StringWriter out = new java.io.StringWriter();
        context.createMarshaller().marshal(metadata, out);
        return out.toString();
    }

    private static Geometry boundingBoxOf(Dataset dataset) {
        return GeometryS124Converter.envelopeToJts(dataset.getBoundedBy());
    }

    /** Asserts the extent of the catalogue's first dataset entry, in degrees. */
    private static void assertBoundingBox(S100ExchangeCatalogue catalogue,
            double west, double east, double south, double north) {
        S100GeographicBoundingBoxType boundingBox = catalogue.getDatasetDiscoveryMetadata()
                .getS100DatasetDiscoveryMetadatas().get(0).getBoundingBox();
        assertThat(boundingBox)
                .as("boundingBox has multiplicity 1 in S-124 clause 12.2.2")
                .isNotNull();
        assertThat(boundingBox.getWestBoundLongitude().getDecimal())
                .isEqualByComparingTo(BigDecimal.valueOf(west));
        assertThat(boundingBox.getEastBoundLongitude().getDecimal())
                .isEqualByComparingTo(BigDecimal.valueOf(east));
        assertThat(boundingBox.getSouthBoundLatitude().getDecimal())
                .isEqualByComparingTo(BigDecimal.valueOf(south));
        assertThat(boundingBox.getNorthBoundLatitude().getDecimal())
                .isEqualByComparingTo(BigDecimal.valueOf(north));
    }

    /** Adds a NavwarnPart carrying {@code geometry} to the dataset's members. */
    private static void addMemberGeometry(Dataset dataset, Geometry geometry) {
        ObjectFactory of = new ObjectFactory();
        List<AbstractGMLType> members = membersOf(dataset)
                .getNavwarnPartsAndNavwarnAreaAffectedsAndTextPlacements();
        NavwarnPart part = of.createNavwarnPart();
        part.setId("NW." + (members.size() + 1));
        // warningInformation and header are both mandatory in the S-124 schema, so even a part
        // that exists only to carry geometry has to state them.
        InformationType information = of.createInformationType();
        information.setLanguage("eng");
        information.setText("Test warning information");
        WarningInformationType warningInformation = of.createWarningInformationType();
        warningInformation.getInformations().add(information);
        part.setWarningInformation(warningInformation);
        ReferenceType header = new dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.ObjectFactory()
                .createReferenceType();
        header.setHref("#PR.1");
        // S-100 Part 10b clause 10b-9: an association must carry role or arcrole, so that a reader
        // can tell it is an association role rather than an attribute (clause 10b-10 item 3).
        header.setRole("http://www.iho.int/S124/gml/2.0/roles/header");
        part.setHeader(header);
        for (S100SpatialAttributeType property :
                GeometryS124Converter.geometryToS124PointCurveSurfaceGeometry(geometry)) {
            NavwarnPart.Geometry memberGeometry = of.createNavwarnPartGeometry();
            if (property instanceof PointProperty pointProperty) {
                memberGeometry.setPointProperty(pointProperty);
            } else if (property instanceof CurveProperty curveProperty) {
                memberGeometry.setCurveProperty(curveProperty);
            } else {
                memberGeometry.setSurfaceProperty((SurfaceProperty) property);
            }
            part.getGeometries().add(memberGeometry);
        }
        members.add(part);
    }

    /**
     * Sets the times S-124 clause 12.2.2 aligns the temporal extent with on the dataset's preamble.
     * <p/>
     * S-124 clause 4 allows exactly one NavwarnPreamble per dataset, so this configures the one
     * {@link #newDataset(String)} already supplied rather than adding another.
     */
    private static void addPreamble(Dataset dataset, OffsetDateTime publicationTime,
            OffsetDateTime cancellationDate) {
        NavwarnPreamble preamble = preambleOf(dataset);
        preamble.setPublicationTime(publicationTime);
        preamble.setCancellationDate(cancellationDate);
    }

    /** The dataset's single NavwarnPreamble. */
    private static NavwarnPreamble preambleOf(Dataset dataset) {
        return membersOf(dataset).getNavwarnPartsAndNavwarnAreaAffectedsAndTextPlacements().stream()
                .filter(NavwarnPreamble.class::isInstance)
                .map(NavwarnPreamble.class::cast)
                .findFirst()
                .orElseThrow();
    }

    /** A conformant NavwarnPreamble, as every S-124 dataset must carry exactly one. */
    private static NavwarnPreamble newPreamble(String id) {
        ObjectFactory of = new ObjectFactory();
        NavwarnPreamble preamble = of.createNavwarnPreamble();
        preamble.setId(id);
        preamble.setPublicationTime(OffsetDateTime.parse("2026-01-15T06:00:00Z"));
        // generalArea, messageSeriesIdentifier, intService and navwarnTypeGeneral are all
        // mandatory in the S-124 schema.
        LocationNameType locationName = of.createLocationNameType();
        locationName.setLanguage("eng");
        locationName.setText("Test Area");
        GeneralAreaType generalArea = of.createGeneralAreaType();
        generalArea.getLocationNames().add(locationName);
        preamble.getGeneralAreas().add(generalArea);
        WarningTypeType warningType = of.createWarningTypeType();
        warningType.setValue(WarningTypeLabel.COASTAL_NAVIGATIONAL_WARNING);
        MessageSeriesIdentifierType series = of.createMessageSeriesIdentifierType();
        // An S-62 producer code, not the agency's name (S-124 clause 4.3.3).
        series.setAgencyResponsibleForProduction("DK00");
        series.setNameOfSeries("Test Nav. Warn.");
        series.setWarningNumber(1);
        series.setYear(2026);
        series.setWarningType(warningType);
        preamble.setMessageSeriesIdentifier(series);
        preamble.setIntService(true);
        NavwarnTypeGeneralType typeGeneral = of.createNavwarnTypeGeneralType();
        typeGeneral.setValue(NavwarnTypeGeneralLabel.OTHER_HAZARDS);
        preamble.setNavwarnTypeGeneral(typeGeneral);
        return preamble;
    }

    /** A second preamble - the one thing S-124 clause 4 forbids. */
    private static void addSecondPreamble(Dataset dataset) {
        membersOf(dataset).getNavwarnPartsAndNavwarnAreaAffectedsAndTextPlacements()
                .add(newPreamble("PR.2"));
    }

    private static Dataset.Members membersOf(Dataset dataset) {
        if (dataset.getMembers() == null) {
            dataset.setMembers(new ObjectFactory().createDatasetMembers());
        }
        return dataset.getMembers();
    }

    private static StandaloneDigitalSignature unmarshalSignature(String xml) throws Exception {
        JAXBContext jaxbContext = JAXBContext.newInstance(
                StandaloneDigitalSignature.class.getPackageName(),
                StandaloneDigitalSignature.class.getClassLoader());
        return (StandaloneDigitalSignature) jaxbContext.createUnmarshaller()
                .unmarshal(new StringReader(xml));
    }

    private static S100ExchangeCatalogue catalogueOf(byte[] zipBytes) throws Exception {
        String catalogXml = new String(unzip(zipBytes).get("S100_ROOT/CATALOG.XML"), StandardCharsets.UTF_8);
        return S100ExchangeSetUtils.unmarshallS100ExchangeSetCatalogue(catalogXml);
    }

    private static long datasetFileCount(Map<String, byte[]> entries) {
        return entries.keySet().stream()
                .filter(n -> n.startsWith("S100_ROOT/S-124/DATASET_FILES/") && n.endsWith(".GML"))
                .count();
    }

    /** The clause 17-4.3 file name the test producer DK00 packages the dataset {@code id} under. */
    private static String datasetFileNameOf(String id) {
        return "124DK00" + id.replaceAll("[^A-Za-z0-9]", "") + ".GML";
    }

    private static Dataset newDataset(String id) {
        ObjectFactory of = new ObjectFactory();
        Dataset dataset = of.createDataset();
        dataset.setId(id);

        DataSetIdentificationTypeImpl ident = new DataSetIdentificationTypeImpl();
        ident.setEncodingSpecification("S-100 Part 10b");
        ident.setEncodingSpecificationEdition("1.0");
        ident.setProductIdentifier("S-124");
        ident.setProductEdition("2.0.0");
        // Mandatory in the S-124 GML schema and fixed by S-100 Part 10b Table 10b-4 to "1"
        // for a base dataset, the purpose set below.
        ident.setApplicationProfile(S124DatasetInfo.BASE_APPLICATION_PROFILE);
        // S-100 Part 10b Table 10b-4 defines the dataset file identifier as the name of the
        // packaged file, which S-100 Part 17, clause 17-4.3, builds from the product code,
        // the producer code and an alphanumeric unique code.
        ident.setDatasetFileIdentifier(datasetFileNameOf(id));
        ident.setDatasetTitle("Test S-124 Dataset");
        ident.setDatasetReferenceDate(LocalDate.of(2026, 1, 15));
        ident.setDatasetLanguage("eng");
        ident.setDatasetAbstract("Synthetic dataset used for unit tests");
        // datasetTopicCategory and datasetPurpose are both mandatory in the S-124 GML schema.
        ident.getDatasetTopicCategories().add(MDTopicCategoryCode.OCEANS);
        ident.setDatasetPurpose(DatasetPurposeType.BASE);
        ident.setUpdateNumber(BigInteger.ZERO);
        dataset.setDatasetIdentificationInformation(ident);

        // GML positions use lat,lon order.
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
        // The S-124 schema declares <members> mandatory, and clause 4 requires exactly one
        // NavwarnPreamble in it.
        membersOf(dataset).getNavwarnPartsAndNavwarnAreaAffectedsAndTextPlacements()
                .add(newPreamble("PR.1"));

        return dataset;
    }

    private static Map<String, byte[]> unzip(byte[] data) throws Exception {
        Map<String, byte[]> out = new LinkedHashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(data))) {
            ZipEntry entry;
            byte[] buf = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int n;
                while ((n = zis.read(buf)) > 0) {
                    baos.write(buf, 0, n);
                }
                out.put(entry.getName(), baos.toByteArray());
            }
        }
        return out;
    }

    // ---------------------------------------------------------------------------------------
    // Discovery metadata derived from the dataset (S-124 clause 12.2.2)
    // ---------------------------------------------------------------------------------------

    /**
     * S-124 clause 12.2.2, description row: "If used, content of this attribute must match the
     * content of the generalArea and locality attributes of the dataset NavwarnPreamble" - not the
     * dataset abstract, which is written for a different purpose and typically opens with the
     * issuing authority and the warning number.
     */
    @Test
    void descriptionComesFromThePreamblesGeneralAreaAndLocality() throws Exception {
        Dataset dataset = newDataset("DK.S124.description");
        dataset.getDatasetIdentificationInformation()
                .setDatasetAbstract("Danish navigational warning DK-NW-011-26 issued by the DMA.");
        namePreamble(dataset, "The Sound", "Drogden Channel");

        S100DatasetDiscoveryMetadata entry = firstEntry(exchangeSetOf(dataset));

        assertThat(entry.getDescription().getCharacterString().getValue())
                .isEqualTo("The Sound, Drogden Channel");
    }

    /** The localised name matching the catalogue's language wins; the others are ignored. */
    @Test
    void descriptionUsesTheCatalogueLanguage() throws Exception {
        Dataset dataset = newDataset("DK.S124.descriptionlang");
        NavwarnPreamble preamble = namePreamble(dataset, "The Sound", "Drogden Channel");
        preamble.getGeneralAreas().get(0).getLocationNames().add(locationName("dan", "Sundet"));
        preamble.getLocalities().get(0).getLocationNames().add(locationName("dan", "Drogden"));

        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .datasets(List.of(dataset))
                .organization("DMA")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .locales(List.of(Locale.forLanguageTag("da")))
                .build()
                .toBytes();

        assertThat(firstEntry(zipBytes).getDescription().getCharacterString().getValue())
                .isEqualTo("Sundet, Drogden");
    }

    /**
     * The attribute is optional ("If used"), so a dataset whose preamble names no place omits it.
     * Falling back to the abstract would break the very rule the derivation exists to satisfy.
     */
    @Test
    void descriptionIsOmittedWhenThePreambleNamesNoPlace() throws Exception {
        // generalArea is mandatory and its locationName text is an unrestricted xs:string
        // (124_2.0.0.xsd textType), so naming no place means an empty or whitespace-only name -
        // both of which are schema-valid, hence full validation below.
        for (String blank : List.of("", "   ")) {
            Dataset dataset = newDataset("DK.S124.nodescription");
            dataset.getDatasetIdentificationInformation()
                    .setDatasetAbstract("An abstract, but no place");
            preambleOf(dataset).getGeneralAreas().get(0).getLocationNames().get(0).setText(blank);
            preambleOf(dataset).getLocalities().clear();

            assertThat(firstEntry(exchangeSetOf(dataset)).getDescription())
                    .as("description for a location name of \"%s\"", blank)
                    .isNull();
        }
    }

    /**
     * S-124 clause 12.2.2 defines issueTime as the "Time of day at which the data was made
     * available by the Data Producer", which is the preamble's publicationTime - converted to UTC,
     * not taken at face value.
     */
    @Test
    void issueTimeComesFromThePreamblesPublicationTime() throws Exception {
        Dataset dataset = newDataset("DK.S124.issuetime");
        dataset.getDatasetIdentificationInformation().setDatasetReferenceDate(LocalDate.of(2026, 8, 20));
        // 08:45+02:00 is 06:45 UTC, the form the catalogue carries.
        addPreamble(dataset, OffsetDateTime.of(2026, 8, 20, 8, 45, 0, 0, ZoneOffset.ofHours(2)), null);

        assertThat(firstEntry(exchangeSetOf(dataset)).getIssueTime()).isEqualTo(LocalTime.of(6, 45));
    }

    /** With no publication time there is nothing to state, and the attribute is optional. */
    @Test
    void issueTimeIsOmittedWhenTheDatasetStatesNoPublicationTime() throws Exception {
        Dataset dataset = newDataset("DK.S124.noissuetime");
        // publicationTime is mandatory in the schema, so a dataset without one is not schema-valid;
        // the catalogue must still describe it rather than invent a time.
        preambleOf(dataset).setPublicationTime(null);

        assertThat(firstEntry(exchangeSetOfUnvalidated(dataset)).getIssueTime()).isNull();
    }

    /**
     * issueDate and issueTime are two halves of one instant. issueDate comes from the dataset's own
     * datasetReferenceDate while the publication time is normalised to UTC, so near midnight the two
     * can fall on different days; the time is then dropped rather than made to contradict the date.
     */
    @Test
    void issueTimeIsOmittedWhenThePublicationTimeFallsOnAnotherDay() throws Exception {
        Dataset dataset = newDataset("DK.S124.midnight");
        dataset.getDatasetIdentificationInformation().setDatasetReferenceDate(LocalDate.of(2026, 6, 10));
        // 2026-06-10T01:00+02:00 is 2026-06-09T23:00Z - the day before the declared issue date.
        addPreamble(dataset, OffsetDateTime.of(2026, 6, 10, 1, 0, 0, 0, ZoneOffset.ofHours(2)), null);

        S100DatasetDiscoveryMetadata entry = firstEntry(exchangeSetOf(dataset));
        assertThat(entry.getIssueDate()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(entry.getIssueTime()).isNull();
    }

    /**
     * S-124 clause 6.2.2: "All instances of time in datasets conforming to S-124 must be expressed
     * in UTC". A producer working in local time has its publicationTime converted, not rejected -
     * the offset makes the instant unambiguous, so there is nothing for the producer to decide.
     */
    @Test
    void datasetTimesAreMarshalledInUtc() throws Exception {
        Dataset dataset = newDataset("DK.S124.utctimes");
        addPreamble(dataset, OffsetDateTime.of(2026, 8, 20, 8, 45, 0, 0, ZoneOffset.ofHours(2)), null);

        String gml = new String(unzip(exchangeSetOf(dataset))
                .get("S100_ROOT/S-124/DATASET_FILES/124DK00DKS124utctimes.GML"), StandardCharsets.UTF_8);

        assertThat(gml).as("dataset GML:%n%s", gml)
                .contains("2026-08-20T06:45:00Z")
                .doesNotContain("+02:00");
    }

    /**
     * S-124 clause 12.2.2, datasetID row: "The URN must be an MRN and if used match the value of
     * interoperabilityIdentifier in the messageSeriesIdentifier". Using it verbatim makes the two
     * artefacts agree by construction rather than by the producer's care.
     */
    @Test
    void datasetIdIsTheInteroperabilityIdentifierWhenTheDatasetStatesOne() throws Exception {
        Dataset dataset = newDataset("DK.S124.interop");
        NavwarnPreamble preamble = namePreamble(dataset, "The Sound", null);
        preamble.getMessageSeriesIdentifier().setInteroperabilityIdentifier("urn:mrn:iho:s124:dk:nw-011-26");

        assertThat(firstEntry(exchangeSetOf(dataset)).getDatasetID())
                .isEqualTo("urn:mrn:iho:s124:dk:nw-011-26");
    }

    @Test
    void datasetIdFallsBackToThePrefixWhenTheDatasetStatesNone() throws Exception {
        assertThat(firstEntry(exchangeSetOf(newDataset("DK.S124.nointerop"))).getDatasetID())
                .isEqualTo("urn:mrn:iho:s124:DK.S124.nointerop");
    }

    /**
     * When the dataset states an identifier that is not an MRN, no datasetID can be both an MRN and
     * a match for it, so the optional attribute is omitted. S-124 clause 4.3.3 only says
     * interoperabilityIdentifier "should" follow the MRN concept, so such a dataset is conformant
     * and must not fail the build.
     */
    @Test
    void datasetIdIsOmittedWhenTheInteroperabilityIdentifierIsNotAnMrn() throws Exception {
        Dataset dataset = newDataset("DK.S124.badinterop");
        NavwarnPreamble preamble = namePreamble(dataset, "The Sound", null);
        preamble.getMessageSeriesIdentifier().setInteroperabilityIdentifier("DK-NW-011-26");

        assertThat(firstEntry(exchangeSetOf(dataset)).getDatasetID()).isNull();
    }

    /**
     * The catalogue schema types datasetID as MRNType, whose pattern "urn:mrn:.+" is case-sensitive,
     * so an upper-case scheme must not be accepted as an MRN - it would produce a signed but
     * schema-invalid CATALOG.XML.
     */
    @Test
    void anUpperCaseUrnSchemeIsNotTreatedAsAnMrn() throws Exception {
        Dataset dataset = newDataset("DK.S124.upperinterop");
        NavwarnPreamble preamble = namePreamble(dataset, "The Sound", null);
        preamble.getMessageSeriesIdentifier().setInteroperabilityIdentifier("URN:MRN:iho:s124:dk:1");

        assertThat(firstEntry(exchangeSetOf(dataset)).getDatasetID()).isNull();
    }

    /** A synthesised datasetID that cannot be an MRN is the caller's to fix, so it is reported. */
    @Test
    void rejectsADatasetIdThatCannotFormAnMrn() {
        Dataset dataset = newDataset("DK.S124.badid");
        dataset.setId("DK.S124.\u00d81");

        assertThatThrownBy(() -> exchangeSetOf(dataset))
                .isInstanceOf(S124ExchangeSetFactory.ExchangeSetException.class)
                .hasMessageContaining("Marine Resource Name");
    }

    @Test
    void rejectsADatasetMrnPrefixThatIsNotAnMrn() {
        assertThatThrownBy(() -> S124ExchangeSetFactory.builder().datasetMrnPrefix("s124"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Marine Resource Name");
        assertThatThrownBy(() -> S124ExchangeSetFactory.builder().datasetMrnPrefix(" "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * An administrative area is a subdivision of a country, not a synonym for one. With none
     * configured the element is simply left out, as the dataset-level producing agency already did.
     */
    @Test
    void administrativeAreaIsNotDefaultedToTheCountry() throws Exception {
        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .datasets(List.of(newDataset("DK.S124.adminarea")))
                .organization("DMA")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .country("Denmark")
                .build()
                .toBytes();

        String catalogXml = new String(unzip(zipBytes).get("S100_ROOT/CATALOG.XML"), StandardCharsets.UTF_8);
        assertThat(catalogXml).as("CATALOG.XML:%n%s", catalogXml).doesNotContain("administrativeArea");
    }

    // ---------------------------------------------------------------------------------------
    // Fail-fast on non-conformant datasets
    // ---------------------------------------------------------------------------------------

    /**
     * S-124 clause 8.1.1 requires schema validity of every dataset. The check runs before the
     * dataset is signed, so an invalid one never ships vouched for.
     */
    @Test
    void rejectsADatasetThatIsNotSchemaValidBeforeSigningIt() {
        Dataset dataset = newDataset("DK.S124.invalid");
        dataset.getDatasetIdentificationInformation().setApplicationProfile(null);

        assertThatThrownBy(() -> exchangeSetOf(dataset))
                .isInstanceOf(S124ExchangeSetFactory.ExchangeSetException.class)
                .hasMessageContaining("not valid against its XML schema")
                .hasMessageContaining("applicationProfile");
    }

    /** The same dataset builds when the caller knowingly opts out. */
    @Test
    void schemaValidationCanBeTurnedOff() {
        Dataset dataset = newDataset("DK.S124.invalidallowed");
        dataset.getDatasetIdentificationInformation().setApplicationProfile(null);

        assertThatCode(() -> S124ExchangeSetFactory.builder()
                .datasets(List.of(dataset))
                .organization("DMA")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> DUMMY_SIGNATURE)
                .phone("+4572196000")
                .validateAgainstSchema(false)
                .build()
                .toBytes()).doesNotThrowAnyException();
    }

    /**
     * S-124 clause 4 allows one navigational warning per dataset. Two preambles leave the entry's
     * temporal extent undefined, so the dataset is rejected rather than resolved by taking the
     * first - which would silently drop the second warning's dates.
     */
    @Test
    void rejectsADatasetCarryingMoreThanOneNavwarnPreamble() {
        Dataset dataset = newDataset("DK.S124.twopreambles");
        addSecondPreamble(dataset);

        assertThatThrownBy(() -> exchangeSetOf(dataset))
                .isInstanceOf(S124ConformanceException.class)
                .hasMessageContaining("2 NavwarnPreamble instances");
    }

    /** The conformance failure names the dataset and the clause, so it is not wrapped away. */
    @Test
    void conformanceFailuresAreNotWrappedInAGenericBuildFailure() {
        Dataset dataset = newDataset("DK.S124.agencyname");
        NavwarnPreamble preamble = namePreamble(dataset, "The Sound", null);
        preamble.getMessageSeriesIdentifier().setAgencyResponsibleForProduction("Danish Maritime Authority");

        assertThatThrownBy(() -> exchangeSetOf(dataset))
                .isInstanceOf(S124ConformanceException.class)
                .hasMessageContaining("S-124 clause 4.3.3");
    }

    /**
     * S-100 Part 10b, clause 10b-8.2.4, requires "the code and label of the listed value". The code
     * is a pure function of the label, so it is filled in rather than demanded of the producer.
     */
    @Test
    void fillsInMissingEnumerationCodesBeforeMarshalling() throws Exception {
        Dataset dataset = newDataset("DK.S124.codes");
        NavwarnPreamble preamble = namePreamble(dataset, "The Sound", null);
        preamble.getNavwarnTypeGeneral().setCode(null);
        preamble.getMessageSeriesIdentifier().getWarningType().setCode(null);

        String gml = new String(unzip(exchangeSetOf(dataset))
                .get("S100_ROOT/S-124/DATASET_FILES/124DK00DKS124codes.GML"), StandardCharsets.UTF_8);

        assertThat(gml).as("dataset GML:%n%s", gml)
                // Other Hazards = 5, Coastal Navigational Warning = 2 (S-124 clause 4.3.1, Figure 4-4)
                .contains("navwarnTypeGeneral code=\"5\"")
                .contains("warningType code=\"2\"");
    }

    // ---------------------------------------------------------------------------------------
    // Bounding box precision (S-124 clause 8.2)
    // ---------------------------------------------------------------------------------------

    /**
     * S-124 clause 8.2: coordinates are "coded as decimal numbers with 7 or fewer digits after the
     * decimal". Passing a raw double through BigDecimal.valueOf leaks the IEEE-754 representation
     * error into the XML, so the catalogue disagreed with the very GML it describes.
     */
    @Test
    void boundingBoxCoordinatesAreQuantisedToSevenDecimals() throws Exception {
        Dataset dataset = newDataset("DK.S124.precision");
        setEnvelope(dataset, 55.4967, 12.67, 55.5967, 12.77);

        S100GeographicBoundingBoxType box = firstEntry(exchangeSetOf(dataset)).getBoundingBox();

        // Quantising the double rather than the BigDecimal also drops the non-significant trailing
        // zeros S-124 clause 8.3 forbids; clause 8.2 asks for "7 or fewer" digits, not exactly 7.
        assertThat(box.getWestBoundLongitude().getDecimal().toPlainString()).isEqualTo("12.67");
        assertThat(box.getEastBoundLongitude().getDecimal().toPlainString()).isEqualTo("12.77");
        assertThat(box.getSouthBoundLatitude().getDecimal().toPlainString()).isEqualTo("55.4967");
        assertThat(box.getNorthBoundLatitude().getDecimal().toPlainString()).isEqualTo("55.5967");
    }

    /**
     * The catalogue bounding box and the geometry the dataset actually publishes must denote the
     * same numbers. Both are quantised to 7 decimals half-up - the catalogue here, every GML
     * coordinate by DoubleListAdapter's {@code %.7f} - and rounding is monotonic, so the declared
     * box is exactly the extent of the encoded geometry rather than merely containing it.
     */
    @Test
    void boundingBoxAgreesWithTheDatasetEnvelopeItDescribes() throws Exception {
        Dataset dataset = newDataset("DK.S124.agreement");
        setEnvelope(dataset, 54.82, 14.8, 55.045, 15.1);

        byte[] zipBytes = exchangeSetOf(dataset);
        String catalogXml = new String(unzip(zipBytes).get("S100_ROOT/CATALOG.XML"), StandardCharsets.UTF_8);
        String gml = new String(unzip(zipBytes)
                .get("S100_ROOT/S-124/DATASET_FILES/124DK00DKS124agreement.GML"), StandardCharsets.UTF_8);

        assertThat(catalogXml).as("CATALOG.XML:%n%s", catalogXml)
                .contains(">14.8<", ">15.1<", ">54.82<", ">55.045<")
                .doesNotContain("14.799999999999999")
                .doesNotContain("15.100000000000001")
                .doesNotContain("55.044999999999995");
        assertThat(gml).contains("54.8200000 14.8000000", "55.0450000 15.1000000");
    }

    // ---------------------------------------------------------------------------------------
    // Helpers for the tests above
    // ---------------------------------------------------------------------------------------

    /** The catalogue's first dataset discovery metadata entry. */
    private static S100DatasetDiscoveryMetadata firstEntry(byte[] zipBytes) throws Exception {
        return catalogueOf(zipBytes).getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas().get(0);
    }

    /** Gives the dataset a preamble naming a general area and, optionally, a locality. */
    private static NavwarnPreamble namePreamble(Dataset dataset, String generalArea, String locality) {
        addPreamble(dataset, OffsetDateTime.of(2026, 8, 20, 6, 45, 0, 0, ZoneOffset.UTC), null);
        NavwarnPreamble preamble = preambleOf(dataset);
        preamble.getGeneralAreas().get(0).getLocationNames().clear();
        preamble.getGeneralAreas().get(0).getLocationNames().add(locationName("eng", generalArea));
        if (locality != null) {
            LocalityType localityType = new ObjectFactory().createLocalityType();
            localityType.getLocationNames().add(locationName("eng", locality));
            preamble.getLocalities().add(localityType);
        }
        return preamble;
    }

    private static LocationNameType locationName(String language, String text) {
        LocationNameType name = new ObjectFactory().createLocationNameType();
        name.setLanguage(language);
        name.setText(text);
        return name;
    }

    /** Replaces the dataset's declared envelope, in GML lat,lon order. */
    private static void setEnvelope(Dataset dataset, double south, double west, double north, double east) {
        PosImpl lower = new PosImpl();
        lower.setValue(new Double[] { south, west });
        PosImpl upper = new PosImpl();
        upper.setValue(new Double[] { north, east });
        EnvelopeTypeImpl env = new EnvelopeTypeImpl();
        env.setSrsName("EPSG:4326");
        env.setLowerCorner(lower);
        env.setUpperCorner(upper);
        BoundingShapeTypeImpl bbox = new BoundingShapeTypeImpl();
        bbox.setEnvelope(env);
        dataset.setBoundedBy(bbox);
    }
}
