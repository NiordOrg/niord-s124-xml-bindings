package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.exchangesets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.grad.eNav.s100.utils.S100ExchangeSetUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dk.dma.niord.s100.catalog._5_2.S100DatasetDiscoveryMetadata;
import dk.dma.niord.s100.catalog._5_2.S100ExchangeCatalogue;
import dk.dma.niord.s100.catalog._5_2.S100Purpose;
import dk.dma.niord.s100.catalog._5_2.S100SECertificateContainerType;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.DataSetIdentificationTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.BoundingShapeTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.EnvelopeTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.PosImpl;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.Dataset;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.ObjectFactory;

class S124ExchangeSetFactoryTest {

    private static String testCertPem;

    @BeforeAll
    static void loadCert() throws Exception {
        try (var in = S124ExchangeSetFactoryTest.class.getResourceAsStream("/test-cert.pem")) {
            assertThat(in).as("test-cert.pem must be on the test classpath").isNotNull();
            testCertPem = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
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
                .contains("S_100/CATALOG.XML", "S_100/CATALOG.SIGN")
                .anyMatch(name -> name.startsWith("S_100/S-124/DATASET_FILES/124DK00") && name.endsWith("-0.XML"));
        assertThat(entries.keySet())
                .contains("S_100/S-124/CATALOGUES/", "S_100/S-124/SUPPORT_FILES/");

        // signer called once per dataset file + once for CATALOG.XML
        assertThat(signCalls.get()).isEqualTo(2);
        assertThat(entries.get("S_100/CATALOG.SIGN")).hasSize(64);

        String catalogXml = new String(entries.get("S_100/CATALOG.XML"), StandardCharsets.UTF_8);
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
                .build()
                .toBytes();

        long datasetFiles = unzip(zipBytes).keySet().stream()
                .filter(n -> n.startsWith("S_100/S-124/DATASET_FILES/") && n.endsWith(".XML"))
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
                .build()
                .toBytes();

        S100DatasetDiscoveryMetadata originalMeta = catalogueOf(originalZip)
                .getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas().get(0);
        assertThat(originalMeta.getPurpose()).isEqualTo(S100Purpose.NEW_DATASET);

        // 2. Cancel it: reuse the original file name and signature, but ship no file.
        String originalFileName = originalMeta.getFileName().substring("file:/".length());
        S124ExchangeSetFactory.Cancellation cancellation = new S124ExchangeSetFactory.Cancellation(
                originalFileName,
                originalMeta.getDatasetID(),
                originalMeta.getEditionNumber(),
                originalMeta.getUpdateNumber(),
                originalMeta.getIssueDate(),
                null,
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
        assertThatThrownBy(() -> new S124ExchangeSetFactory.Cancellation(
                "124DK00x-0.XML", "urn:mrn:iho:s124:dk:1", BigInteger.ONE, BigInteger.ZERO,
                LocalDate.of(2026, 1, 15), null, null, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("original");
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
                .build()
                .toBytes();
        S100DatasetDiscoveryMetadata m = catalogueOf(zip)
                .getDatasetDiscoveryMetadata().getS100DatasetDiscoveryMetadatas().get(0);
        return new S124ExchangeSetFactory.Cancellation(
                m.getFileName().substring("file:/".length()),
                m.getDatasetID(),
                m.getEditionNumber(),
                m.getUpdateNumber(),
                m.getIssueDate(),
                null,
                m.getDigitalSignatureReference().getValue(),
                m.getDigitalSignatureValues());
    }

    private static S100ExchangeCatalogue catalogueOf(byte[] zipBytes) throws Exception {
        String catalogXml = new String(unzip(zipBytes).get("S_100/CATALOG.XML"), StandardCharsets.UTF_8);
        return S100ExchangeSetUtils.unmarshallS100ExchangeSetCatalogue(catalogXml);
    }

    private static long datasetFileCount(Map<String, byte[]> entries) {
        return entries.keySet().stream()
                .filter(n -> n.startsWith("S_100/S-124/DATASET_FILES/") && n.endsWith(".XML"))
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
        ident.setDatasetLanguage("en");
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
