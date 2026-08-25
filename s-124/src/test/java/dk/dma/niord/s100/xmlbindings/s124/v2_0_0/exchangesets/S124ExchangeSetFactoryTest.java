package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.exchangesets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.grad.eNav.s100.enums.MaintenanceFrequency;
import org.grad.eNav.s100.utils.S100ExchangeSetUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;

import dk.dma.niord.s100.catalog._5_2.DataStatus;
import dk.dma.niord.s100.catalog._5_2.S100CompliancyCategory;
import dk.dma.niord.s100.catalog._5_2.S100DatasetDiscoveryMetadata;
import dk.dma.niord.s100.catalog._5_2.S100ExchangeCatalogue;
import dk.dma.niord.s100.catalog._5_2.S100Purpose;
import dk.dma.niord.s100.catalog._5_2.S100SECertificateContainerType;
import dk.dma.niord.s100.catalog._5_2.S100SEDigitalSignatureReference;
import dk.dma.niord.s100.catalog._5_2.S100SECertificateType;
import dk.dma.niord.s100.catalog._5_2.S100SESignatureOnData;
import dk.dma.niord.s100.catalog._5_2.StandaloneDigitalSignature;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.DataSetIdentificationTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.BoundingShapeTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.EnvelopeTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.PosImpl;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.Dataset;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.ObjectFactory;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util.GeometryS124Converter;
import jakarta.xml.bind.JAXBContext;

class S124ExchangeSetFactoryTest {

    private static String testCertPem;

    /** Data Server certificate issued by the domain coordinator below, not by the SA. */
    private static String dataServerViaDcPem;
    /** Domain coordinator certificate, issued by the (separately installed) SA root. */
    private static String domainCoordinatorPem;
    /** Same subject name as the real domain coordinator, but a different key: it signed nothing here. */
    private static String domainCoordinatorRolloverPem;

    @BeforeAll
    static void loadCert() throws Exception {
        testCertPem = readResource("/test-cert.pem");
        dataServerViaDcPem = readResource("/test-cert-dataserver-via-dc.pem");
        domainCoordinatorPem = readResource("/test-cert-domaincoordinator.pem");
        domainCoordinatorRolloverPem = readResource("/test-cert-domaincoordinator-rollover.pem");
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
            return new byte[64];
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
                .anyMatch(name -> name.startsWith("S100_ROOT/S-124/DATASET_FILES/124DK00") && name.endsWith("-0.GML"));
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
        // S-124 datasets are always new: edition 1, update number 0 (never null/absent).
        assertThat(meta.get(0).getEditionNumber()).isEqualTo(BigInteger.ONE);
        assertThat(meta.get(0).getUpdateNumber()).isEqualTo(BigInteger.ZERO);

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
        byte[] signatureBytes = "s-124-catalogue-signature".getBytes(StandardCharsets.UTF_8);

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
     * The dataset entries must follow the fixed values of the S-124 clause 12.2.2 discovery
     * metadata profile and the algorithm/codelist restrictions of S-100 Parts 15 and 17.
     */
    /**
     * S-100 Part 17 MD_MaintenanceInformation requires the frequency and a maintenance date
     * together, and restricts MD_MaintenanceFrequencyCode to asNeeded / irregular.
     */
    @Test
    void configuredMaintenanceDateEmitsAllowedFrequency() throws Exception {
        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .datasets(List.of(newDataset("DK.S124.maintenance")))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> new byte[64])
                .phone("+4572196000")
                .maintenanceDate(LocalDate.of(2026, 1, 15))
                .build()
                .toBytes();

        String catalogXml = new String(unzip(zipBytes).get("S100_ROOT/CATALOG.XML"), StandardCharsets.UTF_8);
        assertThat(catalogXml)
                .as("CATALOG.XML:%n%s", catalogXml)
                .contains("asNeeded")
                .contains("2026-01-15")
                .doesNotContain("continual");
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
                .signer((alg, payload) -> new byte[64])
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
                .signer((alg, payload) -> new byte[64])
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
                .signer((alg, payload) -> new byte[64])
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
                .signer((alg, payload) -> new byte[64])
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
        S124ExchangeSetFactory.Cancellation original = cancellationOf(newDataset("DK.S124.rotated"));
        S124ExchangeSetFactory.Cancellation cancellation = new S124ExchangeSetFactory.Cancellation(
                original.fileName(), original.datasetId(), original.editionNumber(),
                original.updateNumber(), original.issueDate(), original.boundingBox(),
                original.signatureReference(), original.signatureValues(),
                List.of(testCertPem));

        // ... but the exchange set is now produced under a different certificate.
        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .cancellations(List.of(cancellation))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(dataServerViaDcPem)
                .intermediateCertificatePems(List.of(domainCoordinatorPem))
                .signer((alg, payload) -> new byte[64])
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
                        c -> new String(c.getValue(), StandardCharsets.UTF_8)));

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

    @Test
    void datasetEntriesFollowTheS124MetadataProfile() throws Exception {
        LocalDateTime beforeBuild = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1);

        byte[] zipBytes = S124ExchangeSetFactory.builder()
                .datasets(List.of(newDataset("DK.S124.profile")))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> new byte[64])
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
                // MD_MaintenanceInformation needs a maintenance date alongside the frequency,
                // so resourceMaintenance is omitted (0..1) unless a date is configured
                .doesNotContain("maintenanceAndUpdateFrequency")
                .doesNotContain("continual")
                // Part 15 clause 15-8.7 mandates the ECDSA-384-SHA2 encoding
                .contains("ECDSA-384-SHA2")
                .doesNotContain("ECDSA-384-SHA3")
                // Part 15 clause 15-8.11.4: signatures on data carry dataStatus
                .contains("unencrypted");

        S100ExchangeCatalogue catalogue = S100ExchangeSetUtils.unmarshallS100ExchangeSetCatalogue(catalogXml);
        assertThat(catalogue.getProductSpecifications().get(0).getCompliancyCategory())
                .isEqualTo(S100CompliancyCategory.CATEGORY_3);
        // Part 17 mandates the catalogue creation date and time in UTC
        assertThat(catalogue.getIdentifier().getDateTime())
                .isBetween(beforeBuild, LocalDateTime.now(ZoneOffset.UTC).plusMinutes(1));

        S100DatasetDiscoveryMetadata meta = catalogue.getDatasetDiscoveryMetadata()
                .getS100DatasetDiscoveryMetadatas().get(0);
        assertThat(meta.getDigitalSignatureReference().getValue())
                .isEqualTo(S100SEDigitalSignatureReference.ECDSA_384_SHA_2);
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
                .signer((alg, payload) -> new byte[64])
                .phone("+4572196000")
                .build();

        assertThatThrownBy(factory::toBytes)
                .isInstanceOf(S124ExchangeSetFactory.ExchangeSetException.class)
                .hasMessageContaining("124DK00DK.S124.oversized-0.GML")
                .hasMessageContaining("51200");
    }

    @Test
    void rejectsMaintenanceFrequenciesOutsideTheS100Subset() {
        S124ExchangeSetFactory.Builder builder = S124ExchangeSetFactory.builder();

        assertThatThrownBy(() -> builder.maintenanceFrequency(MaintenanceFrequency.CONTINUAL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("asNeeded");
        assertThat(builder.maintenanceFrequency(MaintenanceFrequency.IRREGULAR)).isSameAs(builder);
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
                .signer((alg, payload) -> new byte[64])
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
                .signer((alg, payload) -> new byte[64])
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
                .signer((alg, payload) -> new byte[64])
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
                .signer((alg, payload) -> new byte[32])
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
                .signer((alg, payload) -> new byte[64])
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
        S124ExchangeSetFactory.Cancellation cancellation = new S124ExchangeSetFactory.Cancellation(
                originalFileName,
                originalMeta.getDatasetID(),
                originalMeta.getEditionNumber(),
                originalMeta.getUpdateNumber(),
                cancellationDate,
                boundingBoxOf(original),
                originalMeta.getDigitalSignatureReference().getValue(),
                originalMeta.getDigitalSignatureValues());

        Dataset stillActive = newDataset("DK.S124.still-active");
        AtomicInteger signCalls = new AtomicInteger();
        byte[] zip = S124ExchangeSetFactory.builder()
                .datasets(List.of(stillActive))
                .cancellations(List.of(cancellation))
                .organization("Danish Maritime Authority")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> { signCalls.incrementAndGet(); return new byte[64]; })
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
                .signer((alg, payload) -> new byte[64])
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
        Geometry bbox = boundingBoxOf(newDataset("DK.S124.sig-check"));
        assertThatThrownBy(() -> new S124ExchangeSetFactory.Cancellation(
                "124DK00x-0.GML", "urn:mrn:iho:s124:dk:1", BigInteger.ONE, BigInteger.ZERO,
                LocalDate.of(2026, 1, 15), bbox, null, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("original");
    }

    /** boundingBox is mandatory (Mult 1) in the S-124 clause 12.2.2 discovery metadata profile. */
    @Test
    void cancellationRequiresBoundingBox() throws Exception {
        S100DatasetDiscoveryMetadata.DigitalSignatureValue signature =
                cancellationOf(newDataset("DK.S124.bbox-check")).signatureValues().get(0);
        assertThatThrownBy(() -> new S124ExchangeSetFactory.Cancellation(
                "124DK00x-0.GML", "urn:mrn:iho:s124:dk:1", BigInteger.ONE, BigInteger.ZERO,
                LocalDate.of(2026, 1, 15), null, null, List.of(signature)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("boundingBox");
    }

    @Test
    void buildRequiresAtLeastOneDatasetOrCancellation() {
        S124ExchangeSetFactory.Builder b = S124ExchangeSetFactory.builder()
                .organization("DMA")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> new byte[64]);

        assertThatThrownBy(b::build)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one dataset or cancellation");
    }

    /** Builds a real exchange set for the given dataset and derives a fileless {@link S124ExchangeSetFactory.Cancellation} of it. */

    private static S124ExchangeSetFactory.Cancellation cancellationOf(Dataset dataset) throws Exception {
        byte[] zip = S124ExchangeSetFactory.builder()
                .datasets(List.of(dataset))
                .organization("DMA")
                .producerCode("DK00")
                .certificatePem(testCertPem)
                .signer((alg, payload) -> new byte[64])
                .phone("+4572196000")
                .build()
                .toBytes();
        S100DatasetDiscoveryMetadata m = catalogueOf(zip)
                .getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas().get(0);
        return new S124ExchangeSetFactory.Cancellation(
                m.getFileName().substring("file:/".length()),
                m.getDatasetID(),
                m.getEditionNumber(),
                m.getUpdateNumber(),
                m.getIssueDate().plusDays(1),
                boundingBoxOf(dataset),
                m.getDigitalSignatureReference().getValue(),
                m.getDigitalSignatureValues());
    }

    private static Geometry boundingBoxOf(Dataset dataset) {
        return GeometryS124Converter.envelopeToJts(dataset.getBoundedBy());
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

    private static Dataset newDataset(String id) {
        ObjectFactory of = new ObjectFactory();
        Dataset dataset = of.createDataset();
        dataset.setId(id);

        DataSetIdentificationTypeImpl ident = new DataSetIdentificationTypeImpl();
        ident.setEncodingSpecification("S-100 Part 10b");
        ident.setEncodingSpecificationEdition("1.0");
        ident.setProductIdentifier("S-124");
        ident.setProductEdition("2.0.0");
        ident.setDatasetFileIdentifier(id);
        ident.setDatasetTitle("Test S-124 Dataset");
        ident.setDatasetReferenceDate(LocalDate.of(2026, 1, 15));
        ident.setDatasetLanguage("eng");
        ident.setDatasetAbstract("Synthetic dataset used for unit tests");
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
}
