package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.exchangesets;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.grad.eNav.s100.enums.MaintenanceFrequency;
import org.grad.eNav.s100.enums.RoleCode;
import org.grad.eNav.s100.enums.SecurityClassification;
import org.grad.eNav.s100.enums.TelephoneType;
import org.grad.eNav.s100.utils.S100ExchangeCatalogueBuilder;
import org.grad.eNav.s100.utils.S100ExchangeSetUtils;
import org.locationtech.jts.geom.Geometry;

import dk.dma.niord.s100.catalog._5_2.S100EncodingFormat;
import dk.dma.niord.s100.catalog._5_2.S100NavigationPurpose;
import dk.dma.niord.s100.catalog._5_2.S100ProductSpecification;
import dk.dma.niord.s100.catalog._5_2.S100ProtectionScheme;
import dk.dma.niord.s100.catalog._5_2.S100Purpose;
import dk.dma.niord.s100.catalog._5_2.S100SEDigitalSignature;
import dk.dma.niord.s100.catalog._5_2.S100SEDigitalSignatureReference;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.DataSetIdentificationType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.Dataset;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util.GeometryS124Converter;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util.S124Utils;
import jakarta.xml.bind.JAXBException;

/**
 * Builds an S-100 Ed 5.0 Part 17 exchange set (ZIP) from one or more S-124
 * v2.0.0 datasets.
 *
 * <p>Usage:</p>
 * <pre>{@code
 * byte[] zip = S124ExchangeSetFactory.builder()
 *         .datasets(datasets)
 *         .organization("Danish Maritime Authority")
 *         .producerCode("DK00")
 *         .certificatePem(pem)
 *         .signer((alg, payload) -> ecdsaSign(privateKey, payload))
 *         .build()
 *         .toBytes();
 * }</pre>
 *
 * <p>The factory wraps {@link S100ExchangeCatalogueBuilder} and produces:</p>
 * <pre>
 * S_100/
 *  ├── S-124/
 *  │   ├── DATASET_FILES/   124&lt;producer&gt;&lt;uuid&gt;-&lt;idx&gt;.XML
 *  │   ├── CATALOGUES/      (empty)
 *  │   └── SUPPORT_FILES/   (empty)
 *  ├── CATALOG.XML
 *  └── CATALOG.SIGN
 * </pre>
 */
public final class S124ExchangeSetFactory {

    private static final String CERTIFICATE_REF = "cer1";
    private static final String DEFAULT_DATASET_MRN_PREFIX = "urn:mrn:iho:s124";
    private static final String DEFAULT_EXCHANGE_SET_MRN_PREFIX = "urn:mrn:iho:s124:exchangeset";

    private static final String ROOT_DIR = "S_100/";
    private static final String PRODUCT_DIR = ROOT_DIR + "S-124/";
    private static final String DATASET_FILES_DIR = PRODUCT_DIR + "DATASET_FILES/";
    private static final String CATALOGUES_DIR = PRODUCT_DIR + "CATALOGUES/";
    private static final String SUPPORT_FILES_DIR = PRODUCT_DIR + "SUPPORT_FILES/";
    private static final String CATALOG_XML = ROOT_DIR + "CATALOG.XML";
    private static final String CATALOG_SIGN = ROOT_DIR + "CATALOG.SIGN";

    private final Builder cfg;

    private S124ExchangeSetFactory(Builder cfg) {
        this.cfg = cfg;
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Build the exchange set and return it as a ZIP byte array. */
    public byte[] toBytes() {
        try {
            List<DatasetFile> datasetFiles = marshalDatasets();
            String catalogXml = buildCatalogueXml(datasetFiles);
            byte[] catalogBytes = catalogXml.getBytes(StandardCharsets.UTF_8);
            byte[] catalogSig = cfg.signer.sign(cfg.signatureAlgorithm, catalogBytes);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                putDirectoryEntry(zos, ROOT_DIR);
                putDirectoryEntry(zos, PRODUCT_DIR);
                putDirectoryEntry(zos, DATASET_FILES_DIR);
                putDirectoryEntry(zos, CATALOGUES_DIR);
                putDirectoryEntry(zos, SUPPORT_FILES_DIR);
                for (DatasetFile df : datasetFiles) {
                    putFileEntry(zos, DATASET_FILES_DIR + df.fileName, df.bytes);
                }
                putFileEntry(zos, CATALOG_XML, catalogBytes);
                putFileEntry(zos, CATALOG_SIGN, catalogSig);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            throw new ExchangeSetException("Failed to build S-124 exchange set", e);
        }
    }

    private List<DatasetFile> marshalDatasets() throws JAXBException {
        List<DatasetFile> result = new ArrayList<>(cfg.datasets.size());
        AtomicInteger idx = new AtomicInteger();
        for (Dataset dataset : cfg.datasets) {
            String uuid = datasetUuid(dataset);
            String fileName = String.format("124%s%s-%d.XML", cfg.producerCode, uuid, idx.getAndIncrement());
            byte[] bytes = S124Utils.marshalS124(dataset).getBytes(StandardCharsets.UTF_8);
            result.add(new DatasetFile(fileName, bytes, dataset, uuid));
        }
        return result;
    }

    private String buildCatalogueXml(List<DatasetFile> datasetFiles) throws JAXBException, CertificateException {
        AtomicInteger signatureCounter = new AtomicInteger(1);

        S100ExchangeCatalogueBuilder catBuilder = new S100ExchangeCatalogueBuilder(
                (objectId, algorithm, payload) -> {
                    S100SEDigitalSignature sig = new S100SEDigitalSignature();
                    sig.setId(String.format("sig%d", signatureCounter.getAndIncrement()));
                    sig.setCertificateRef(CERTIFICATE_REF);
                    sig.setValue(cfg.signer.sign(algorithm, payload));
                    return sig;
                })
                .setIdentifier(cfg.identifier)
                .setDateTime(LocalDateTime.now())
                .setDataServerIdentifier(cfg.dataServerIdentifier)
                .setOrganization(cfg.organization)
                .setElectronicMailAddresses(cfg.emails)
                .setPhone(cfg.phone)
                .setPhoneType(cfg.phoneType)
                .setCity(cfg.city)
                .setPostalCode(cfg.postalCode)
                .setCountry(cfg.country)
                .setAdministrativeArea(Optional.ofNullable(cfg.administrativeArea).orElse(cfg.country))
                .setLocales(cfg.locales)
                .setDescription(cfg.description)
                .setComment(cfg.comment)
                .setProductSpecification(Collections.singletonList(cfg.productSpecification))
                .setCertificatesByPem(Collections.singletonMap(CERTIFICATE_REF, cfg.certificatePem));

        for (DatasetFile df : datasetFiles) {
            Geometry bbox = GeometryS124Converter.envelopeToJts(df.dataset.getBoundedBy());
            DataSetIdentificationType ident = df.dataset.getDatasetIdentificationInformation();
            LocalDate issueDate = Optional.ofNullable(ident)
                    .map(DataSetIdentificationType::getDatasetReferenceDate)
                    .orElseGet(LocalDate::now);

            catBuilder.addDatasetMetadata(builder -> builder
                    .setFileName("file:/" + df.fileName)
                    .setDatasetID(cfg.datasetMrnPrefix + ":" + df.uuid)
                    .setDescription(Optional.ofNullable(ident).map(DataSetIdentificationType::getDatasetAbstract).orElse(null))
                    .setCompressionFlag(false)
                    .setDataProtection(false)
                    .setProtectionScheme(S100ProtectionScheme.S_100_P_15)
                    .setCopyright(true)
                    .setClassification(cfg.classification)
                    .setPurpose(S100Purpose.NEW_DATASET)
                    .setNotForNavigation(cfg.notForNavigation)
                    .setSpecificUsage(cfg.specificUsage)
                    .setEditionNumber(BigInteger.ONE)
                    .setUpdateNumber(BigInteger.ZERO)
                    .setIssueDate(issueDate)
                    .setIssueTime(LocalTime.MIDNIGHT)
                    .setBoundingBox(bbox)
                    .setProductSpecification(cfg.productSpecification)
                    .setProducingAgency(cfg.organization)
                    .setProducingAgencyRole(cfg.producingAgencyRole)
                    .setProducerCode(cfg.producerCode)
                    .setEncodingFormat(S100EncodingFormat.GML)
                    .setDataCoverages(bbox)
                    .setComment(cfg.datasetComment)
                    .setMetadataDateStamp(LocalDate.now())
                    .setReplacedData(false)
                    .setNavigationPurposes(Collections.singletonList(S100NavigationPurpose.OVERVIEW))
                    .setMaintenanceFrequency(cfg.maintenanceFrequency)
                    .setDigitalSignatureReference(cfg.signatureAlgorithm)
                    .build(df.bytes));
        }

        try {
            return S100ExchangeSetUtils.marshalS100ExchangeSetCatalogue(catBuilder.build());
        } catch (java.security.cert.CertificateEncodingException e) {
            throw new CertificateException(e);
        }
    }

    private String datasetUuid(Dataset dataset) {
        String id = Optional.ofNullable(dataset.getId()).filter(s -> !s.isBlank()).orElse(null);
        if (id != null) {
            return id;
        }
        return Optional.ofNullable(dataset.getDatasetIdentificationInformation())
                .map(DataSetIdentificationType::getDatasetFileIdentifier)
                .filter(s -> !s.isBlank())
                .orElseGet(() -> UUID.randomUUID().toString());
    }

    private static void putDirectoryEntry(ZipOutputStream zos, String path) throws java.io.IOException {
        zos.putNextEntry(new ZipEntry(path));
        zos.closeEntry();
    }

    private static void putFileEntry(ZipOutputStream zos, String path, byte[] data) throws java.io.IOException {
        zos.putNextEntry(new ZipEntry(path));
        zos.write(data);
        zos.closeEntry();
    }

    private record DatasetFile(String fileName, byte[] bytes, Dataset dataset, String uuid) {}

    /** Fluent builder for {@link S124ExchangeSetFactory}. */
    public static final class Builder {
        private List<Dataset> datasets;
        private String organization;
        private String producerCode;
        private String certificatePem;
        private S124Signer signer;

        private String identifier;
        private String dataServerIdentifier;
        private String datasetMrnPrefix = DEFAULT_DATASET_MRN_PREFIX;
        private S100ProductSpecification productSpecification = S124ProductSpecification.defaultSpec();
        private String description;
        private String comment;
        private String datasetComment;
        private String specificUsage;
        private List<String> emails = Collections.emptyList();
        private String phone;
        private TelephoneType phoneType = TelephoneType.VOICE;
        private String city;
        private String postalCode;
        private String country;
        private String administrativeArea;
        private List<Locale> locales = Collections.singletonList(Locale.ENGLISH);
        private S100SEDigitalSignatureReference signatureAlgorithm = S100SEDigitalSignatureReference.ECDSA_384_SHA_3;
        private boolean notForNavigation = true;
        private SecurityClassification classification = SecurityClassification.UNCLASSIFIED;
        private RoleCode producingAgencyRole = RoleCode.CUSTODIAN;
        private MaintenanceFrequency maintenanceFrequency = MaintenanceFrequency.CONTINUAL;

        private Builder() {}

        public Builder datasets(List<Dataset> datasets) { this.datasets = datasets; return this; }
        public Builder organization(String organization) { this.organization = organization; return this; }
        public Builder producerCode(String producerCode) { this.producerCode = producerCode; return this; }
        public Builder certificatePem(String certificatePem) { this.certificatePem = certificatePem; return this; }
        public Builder signer(S124Signer signer) { this.signer = signer; return this; }
        public Builder identifier(String identifier) { this.identifier = identifier; return this; }
        public Builder dataServerIdentifier(String id) { this.dataServerIdentifier = id; return this; }
        public Builder datasetMrnPrefix(String prefix) { this.datasetMrnPrefix = prefix; return this; }
        public Builder productSpecification(S100ProductSpecification spec) { this.productSpecification = spec; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder comment(String comment) { this.comment = comment; return this; }
        public Builder datasetComment(String comment) { this.datasetComment = comment; return this; }
        public Builder specificUsage(String usage) { this.specificUsage = usage; return this; }
        public Builder emails(List<String> emails) { this.emails = emails; return this; }
        public Builder phone(String phone) { this.phone = phone; return this; }
        public Builder phoneType(TelephoneType phoneType) { this.phoneType = phoneType; return this; }
        public Builder city(String city) { this.city = city; return this; }
        public Builder postalCode(String postalCode) { this.postalCode = postalCode; return this; }
        public Builder country(String country) { this.country = country; return this; }
        public Builder administrativeArea(String area) { this.administrativeArea = area; return this; }
        public Builder locales(List<Locale> locales) { this.locales = locales; return this; }
        public Builder signatureAlgorithm(S100SEDigitalSignatureReference algorithm) { this.signatureAlgorithm = algorithm; return this; }
        public Builder notForNavigation(boolean v) { this.notForNavigation = v; return this; }
        public Builder classification(SecurityClassification c) { this.classification = c; return this; }
        public Builder producingAgencyRole(RoleCode role) { this.producingAgencyRole = role; return this; }
        public Builder maintenanceFrequency(MaintenanceFrequency f) { this.maintenanceFrequency = f; return this; }

        public S124ExchangeSetFactory build() {
            Objects.requireNonNull(datasets, "datasets must be set");
            if (datasets.isEmpty()) {
                throw new IllegalArgumentException("datasets must not be empty");
            }
            Objects.requireNonNull(organization, "organization must be set");
            Objects.requireNonNull(producerCode, "producerCode must be set");
            Objects.requireNonNull(certificatePem, "certificatePem must be set");
            Objects.requireNonNull(signer, "signer must be set");
            Objects.requireNonNull(productSpecification, "productSpecification must be set");
            Objects.requireNonNull(signatureAlgorithm, "signatureAlgorithm must be set");
            Objects.requireNonNull(locales, "locales must be set");
            Objects.requireNonNull(emails, "emails must be set");

            if (identifier == null) {
                identifier = DEFAULT_EXCHANGE_SET_MRN_PREFIX + ":" + UUID.randomUUID();
            }
            if (dataServerIdentifier == null) {
                dataServerIdentifier = UUID.nameUUIDFromBytes(organization.getBytes(StandardCharsets.UTF_8)).toString();
            }
            if (description == null) {
                description = String.format("S-124 Exchange Set generated by %s at %s",
                        organization,
                        LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            return new S124ExchangeSetFactory(this);
        }
    }

    /** Thrown when assembling the exchange set fails for IO, JAXB or certificate reasons. */
    public static final class ExchangeSetException extends RuntimeException {
        public ExchangeSetException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
