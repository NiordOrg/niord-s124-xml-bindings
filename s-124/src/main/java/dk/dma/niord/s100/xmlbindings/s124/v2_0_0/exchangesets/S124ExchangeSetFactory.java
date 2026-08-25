package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.exchangesets;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.security.auth.x500.X500Principal;

import org.grad.eNav.s100.enums.MaintenanceFrequency;
import org.grad.eNav.s100.enums.RoleCode;
import org.grad.eNav.s100.enums.SecurityClassification;
import org.grad.eNav.s100.enums.TelephoneType;
import org.grad.eNav.s100.utils.S100ExchangeCatalogueBuilder;
import org.grad.eNav.s100.utils.S100ExchangeSetUtils;
import org.locationtech.jts.geom.Geometry;

import dk.dma.niord.s100.catalog._5_2.DataStatus;
import dk.dma.niord.s100.catalog._5_2.S100DatasetDiscoveryMetadata;
import dk.dma.niord.s100.catalog._5_2.S100EncodingFormat;
import dk.dma.niord.s100.catalog._5_2.S100ProductSpecification;
import dk.dma.niord.s100.catalog._5_2.S100Purpose;
import dk.dma.niord.s100.catalog._5_2.S100SECertificateContainerType;
import dk.dma.niord.s100.catalog._5_2.S100SECertificateType;
import dk.dma.niord.s100.catalog._5_2.S100SEDigitalSignatureReference;
import dk.dma.niord.s100.catalog._5_2.ObjectFactory;
import dk.dma.niord.s100.catalog._5_2.S100SEDigitalSignature;
import dk.dma.niord.s100.catalog._5_2.S100SESignatureOnData;
import dk.dma.niord.s100.catalog._5_2.StandaloneDigitalSignature;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.DataSetIdentificationType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.Dataset;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util.GeometryS124Converter;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util.S124Utils;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;

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
 * <p>The factory wraps {@link S100ExchangeCatalogueBuilder} and produces the folder
 * structure of S-100 Ed 5.x Part 17, clause 17-4.2:</p>
 * <pre>
 * S100_ROOT/
 *  ├── S-124/
 *  │   ├── DATASET_FILES/   124&lt;producer&gt;&lt;uuid&gt;-&lt;idx&gt;.GML
 *  │   ├── CATALOGUES/      (empty)
 *  │   └── SUPPORT_FILES/   (empty)
 *  ├── CATALOG.XML
 *  └── CATALOG.SIGN         (a Part 15 StandaloneDigitalSignature document)
 * </pre>
 *
 * <p>In addition to datasets (written as files and listed in the catalogue with
 * {@code purpose=newDataset}), the exchange set may carry <em>fileless</em> cancellations
 * via {@link Builder#cancellations(List)}. Per S-100 Ed 5.x Part 17, clause 17-4.4.1, a
 * fileless cancellation is a dataset discovery-metadata entry with {@code purpose=cancellation}
 * that reproduces the cancelled dataset's file name, its <em>original</em> digital signature
 * and all other mandatory metadata, but ships no dataset file. Consumers use it to remove the
 * referenced dataset. See {@link Cancellation}.</p>
 *
 * <p><strong>Producer responsibilities the factory cannot check.</strong> Two entries of the
 * S-124 clause 12.2.2 discovery-metadata profile depend on the content of the dataset's
 * {@code NavwarnPreamble}, which this factory does not parse:</p>
 * <ul>
 *   <li>{@code datasetID} is synthesised as {@code <datasetMrnPrefix>:<dataset gml id>};
 *       S-124 requires it to match the {@code interoperabilityIdentifier} of the dataset's
 *       {@code messageSeriesIdentifier} when that is present, so producers must choose the
 *       dataset id and {@link Builder#datasetMrnPrefix(String) MRN prefix} accordingly.</li>
 *   <li>{@code description} is taken from the dataset's {@code datasetAbstract}; S-124
 *       requires its content to match the preamble's {@code generalArea} and {@code locality}.</li>
 * </ul>
 */
public final class S124ExchangeSetFactory {

    private static final String CERTIFICATE_REF = "cer1";
    private static final String DEFAULT_DATASET_MRN_PREFIX = "urn:mrn:iho:s124";
    private static final String DEFAULT_EXCHANGE_SET_MRN_PREFIX = "urn:mrn:iho:s124:exchangeset";
    /** S-124 clause 12.2.2 fixes specificUsage: "Must always be 'Navigational Warning Service'". */
    public static final String SPECIFIC_USAGE = "Navigational Warning Service";
    /** S-124 clause 9.6: "S-124 datasets must not exceed 50KB." */
    private static final int MAX_DATASET_SIZE_BYTES = 50 * 1024;

    // S-100 Part 17, clause 17-4.2: all S-100 content lives in the single top level root
    // folder S100_ROOT, which also holds CATALOG.XML and its signature CATALOG.SIGN.
    private static final String ROOT_DIR = "S100_ROOT/";
    private static final String PRODUCT_DIR = ROOT_DIR + "S-124/";
    private static final String DATASET_FILES_DIR = PRODUCT_DIR + "DATASET_FILES/";
    private static final String CATALOGUES_DIR = PRODUCT_DIR + "CATALOGUES/";
    private static final String SUPPORT_FILES_DIR = PRODUCT_DIR + "SUPPORT_FILES/";
    private static final String CATALOG_FILE_NAME = "CATALOG.XML";
    private static final String CATALOG_XML = ROOT_DIR + CATALOG_FILE_NAME;
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
            byte[] catalogSig = buildCatalogueSignature(catalogBytes);

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
        } catch (ExchangeSetException e) {
            throw e;
        } catch (Exception e) {
            throw new ExchangeSetException("Failed to build S-124 exchange set", e);
        }
    }

    private List<DatasetFile> marshalDatasets() throws JAXBException {
        List<DatasetFile> result = new ArrayList<>(cfg.datasets.size());
        AtomicInteger idx = new AtomicInteger();
        for (Dataset dataset : cfg.datasets) {
            String uuid = datasetUuid(dataset);
            // S-100 Part 17, clause 17-4.3 (mandated for S-124 by clause 9.7): the file name is
            // the product code, the producer code, a unique code and the encoding specific file
            // extension - .GML for the GML encoding (.XML is reserved for metadata files).
            String fileName = String.format("124%s%s-%d.GML", cfg.producerCode, uuid, idx.getAndIncrement());
            byte[] bytes = S124Utils.marshalS124(dataset).getBytes(StandardCharsets.UTF_8);
            if (bytes.length > MAX_DATASET_SIZE_BYTES) {
                throw new ExchangeSetException(String.format(
                        "S-124 dataset %s is %d bytes, exceeding the %d byte limit of S-124 clause 9.6",
                        fileName, bytes.length, MAX_DATASET_SIZE_BYTES));
            }
            result.add(new DatasetFile(fileName, bytes, dataset, uuid));
        }
        return result;
    }

    /**
     * Builds the CATALOG.SIGN content. S-100 Part 15, clauses 15-8.7 and 15-8.11.2, require
     * auxiliary files that are not covered by the catalogue metadata - the catalogue itself
     * above all - to be signed with a self-contained StandaloneDigitalSignature document that
     * carries the signed file name, all certificates needed to authenticate the signature and
     * the signature itself, rather than the bare signature value.
     */
    private byte[] buildCatalogueSignature(byte[] catalogBytes) throws CertificateException, JAXBException {
        S100SECertificateContainerType.SchemeAdministrator schemeAdministrator =
                new S100SECertificateContainerType.SchemeAdministrator();
        schemeAdministrator.setId(cfg.schemeAdministrator);
        S100SECertificateContainerType certificates = new S100SECertificateContainerType();
        certificates.setSchemeAdministrator(schemeAdministrator);
        for (ChainedCertificate link : certificateChain()) {
            S100SECertificateType certificateType = new S100SECertificateType();
            certificateType.setId(link.id());
            // S-100 Part 15, clause 15-8.6: issuer holds the id of the issuing ELEMENT - the
            // schemeAdministrator id, or the id of an included domain coordinator certificate -
            // not the issuer's X.500 distinguished name, which an OEM cannot resolve against the
            // separately installed SA root certificate.
            certificateType.setIssuer(link.issuerId());
            certificateType.setValue(S100ExchangeSetUtils.getPemFromCert(link.certificate()));
            certificates.getCertificates().add(certificateType);
        }

        StandaloneDigitalSignature standaloneSignature = new StandaloneDigitalSignature();
        standaloneSignature.setFilename(CATALOG_FILE_NAME);
        standaloneSignature.setCertificates(certificates);
        standaloneSignature.setDigitalSignature(
                signatureOnData("catalogueSig", cfg.signatureAlgorithm, catalogBytes));

        JAXBContext jaxbContext = JAXBContext.newInstance(
                StandaloneDigitalSignature.class.getPackageName(),
                StandaloneDigitalSignature.class.getClassLoader());
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshaller.marshal(standaloneSignature, out);
        return out.toByteArray();
    }

    /** A certificate together with the id of the element that issued it. */
    private record ChainedCertificate(String id, X509Certificate certificate, String issuerId) {}

    /**
     * Orders the configured certificates into the path the OEM has to walk, from the Data
     * Server certificate up to the scheme administrator.
     * <p/>
     * S-100 Part 15, clause 15-8.7: "The Data Server must always include the digital
     * certificate of its Domain Coordinator to ensure the Data Client OEM has all the
     * certificates required to perform a full certificate path validation without any
     * external access", and "All certificates in the Exchange Set shall be authenticated by
     * the SA, either directly or through indirect authentication by one or more Domain
     * Coordinators." Each certificate therefore references the id of the element above it,
     * and the topmost references the scheme administrator, whose root certificate is
     * installed separately by the OEM and is never included here.
     * <p/>
     * The order is derived from the certificates themselves by matching each issuer name to
     * the subject name of the certificate above it, so callers cannot silently ship an
     * unverifiable chain by listing the intermediates in the wrong order.
     */
    private List<ChainedCertificate> certificateChain() throws CertificateException {
        return certificateChain(cfg.certificatePem, cfg.intermediateCertificatePems, CERTIFICATE_REF, "ca");
    }

    /**
     * Orders {@code leafPem} and its intermediates into a resolvable path and labels them:
     * the leaf takes {@code leafId} and each intermediate {@code <intermediatePrefix>N}.
     */
    private List<ChainedCertificate> certificateChain(String leafPem, List<String> intermediatePems,
            String leafId, String intermediatePrefix) throws CertificateException {
        X509Certificate dataServer = S100ExchangeSetUtils.getCertFromPem(leafPem);
        List<X509Certificate> remaining = new ArrayList<>();
        for (String pem : intermediatePems) {
            remaining.add(S100ExchangeSetUtils.getCertFromPem(pem));
        }

        List<X509Certificate> ordered = new ArrayList<>();
        ordered.add(dataServer);
        while (!remaining.isEmpty()) {
            X509Certificate subject = ordered.get(ordered.size() - 1);
            X500Principal issuerName = subject.getIssuerX500Principal();
            List<X509Certificate> namedIssuers = remaining.stream()
                    .filter(c -> c.getSubjectX500Principal().equals(issuerName))
                    .toList();
            // The name only says which certificate claims to be the issuer; the signature says
            // which one actually is. They diverge when an intermediate is rolled over or
            // replaced, and the OEM checks the signature, so the chain is built on it too.
            Optional<X509Certificate> issuer = namedIssuers.stream()
                    .filter(candidate -> issued(candidate, subject))
                    .findFirst();
            if (issuer.isEmpty()) {
                if (!namedIssuers.isEmpty()) {
                    throw new ExchangeSetException(String.format(
                            "Certificate %s names %s as its issuer but no configured certificate "
                                    + "with that subject actually signed it, so the OEM would "
                                    + "reject the chain (S-100 Part 15, clause 15-8.7)",
                            subject.getSubjectX500Principal().getName(), issuerName.getName()));
                }
                break;
            }
            remaining.remove(issuer.get());
            ordered.add(issuer.get());
        }
        if (!remaining.isEmpty()) {
            throw new ExchangeSetException(String.format(
                    "Intermediate certificate %s does not issue any other configured certificate, "
                            + "so the exchange set would carry a chain the OEM cannot resolve "
                            + "(S-100 Part 15, clause 15-8.7)",
                    remaining.get(0).getSubjectX500Principal().getName()));
        }

        List<ChainedCertificate> chain = new ArrayList<>(ordered.size());
        for (int i = 0; i < ordered.size(); i++) {
            // The topmost certificate is issued by the scheme administrator; every other one
            // is issued by the certificate above it, referenced by its id.
            String issuerId = i == ordered.size() - 1
                    ? cfg.schemeAdministrator
                    : intermediateId(intermediatePrefix, i + 1);
            chain.add(new ChainedCertificate(i == 0 ? leafId : intermediateId(intermediatePrefix, i),
                    ordered.get(i), issuerId));
        }
        return chain;
    }

    private static String intermediateId(String prefix, int index) {
        return prefix + index;
    }

    /** The id an equal certificate already has in the container, or {@code null}. */
    private static String idOf(Map<String, X509Certificate> certificatesById, X509Certificate certificate) {
        return certificatesById.entrySet().stream()
                .filter(e -> e.getValue().equals(certificate))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    /**
     * Re-labels reused signatures to reference the certificate this catalogue carries for
     * them. The signature bytes are untouched - only the document scoped id changes - and
     * the caller's objects are left alone.
     */
    private static List<S100DatasetDiscoveryMetadata.DigitalSignatureValue> withCertificateRef(
            List<S100DatasetDiscoveryMetadata.DigitalSignatureValue> values, String certificateRef) {
        ObjectFactory objectFactory = new ObjectFactory();
        List<S100DatasetDiscoveryMetadata.DigitalSignatureValue> result = new ArrayList<>(values.size());
        for (S100DatasetDiscoveryMetadata.DigitalSignatureValue value : values) {
            S100SEDigitalSignature original = value.getS100SEDigitalSignature() == null
                    ? null
                    : value.getS100SEDigitalSignature().getValue();
            if (original == null) {
                result.add(value);
                continue;
            }
            S100DatasetDiscoveryMetadata.DigitalSignatureValue copy =
                    new S100DatasetDiscoveryMetadata.DigitalSignatureValue();
            if (original instanceof S100SESignatureOnData onData) {
                S100SESignatureOnData signature = new S100SESignatureOnData();
                signature.setId(onData.getId());
                signature.setValue(onData.getValue());
                signature.setDataStatus(onData.getDataStatus());
                signature.setCertificateRef(certificateRef);
                copy.setS100SEDigitalSignature(objectFactory.createS100SESignatureOnData(signature));
            } else {
                S100SEDigitalSignature signature = new S100SEDigitalSignature();
                signature.setId(original.getId());
                signature.setValue(original.getValue());
                signature.setCertificateRef(certificateRef);
                copy.setS100SEDigitalSignature(objectFactory.createS100SEDigitalSignature(signature));
            }
            result.add(copy);
        }
        return result;
    }

    /** Whether {@code issuer} really signed {@code subject}, as the OEM will check. */
    private static boolean issued(X509Certificate issuer, X509Certificate subject) {
        try {
            subject.verify(issuer.getPublicKey());
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    /**
     * Signs a resource with the configured signer. S-100 Part 15, clauses 15-8.11.3 and
     * 15-8.11.4, realize a signature over a data resource as S100_SE_SignatureOnData, which
     * carries the mandatory dataStatus; S-124 data is never compressed or encrypted, so the
     * status is always unencrypted.
     */
    private S100SESignatureOnData signatureOnData(String id, S100SEDigitalSignatureReference algorithm, byte[] payload) {
        S100SESignatureOnData signature = new S100SESignatureOnData();
        signature.setId(id);
        signature.setCertificateRef(CERTIFICATE_REF);
        signature.setDataStatus(DataStatus.UNENCRYPTED);
        signature.setValue(cfg.signer.sign(algorithm, payload));
        return signature;
    }

    private String buildCatalogueXml(List<DatasetFile> datasetFiles) throws JAXBException, CertificateException {
        AtomicInteger signatureCounter = new AtomicInteger(1);

        S100ExchangeCatalogueBuilder catBuilder = new S100ExchangeCatalogueBuilder(
                (objectId, algorithm, payload) -> signatureOnData(
                        String.format("sig%d", signatureCounter.getAndIncrement()), algorithm, payload))
                .setIdentifier(cfg.identifier)
                // Part 17 mandates the format yyyy-mm-ddThh:mm:ssZ, i.e. UTC, for the
                // exchange catalogue identifier's creation date and time.
                .setDateTime(LocalDateTime.now(ZoneOffset.UTC))
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
                .setSchemeAdministrator(cfg.schemeAdministrator);
        Map<String, X509Certificate> certificatesById = new LinkedHashMap<>();
        Map<String, String> certificateIssuers = new LinkedHashMap<>();
        for (ChainedCertificate link : certificateChain()) {
            certificatesById.put(link.id(), link.certificate());
            certificateIssuers.put(link.id(), link.issuerId());
        }
        // A fileless cancellation reuses the original dataset's signature, which was made with
        // whichever certificate was current then. If that certificate has since been replaced,
        // it has to travel with the exchange set too, otherwise the reused signature resolves
        // to the wrong key and cannot be verified (S-100 Part 15, clause 15-8.7).
        Map<Cancellation, String> cancellationCertificateRefs = new LinkedHashMap<>();
        int cancellationIndex = 0;
        for (Cancellation cancellation : cfg.cancellations) {
            if (cancellation.certificatePems().isEmpty()) {
                // No certificate supplied: the signature was made with the current one.
                cancellationCertificateRefs.put(cancellation, CERTIFICATE_REF);
                continue;
            }
            cancellationIndex++;
            String leafId = "cerC" + cancellationIndex;
            List<ChainedCertificate> cancellationChain = certificateChain(
                    cancellation.certificatePems().get(0),
                    cancellation.certificatePems().subList(1, cancellation.certificatePems().size()),
                    leafId, "caC" + cancellationIndex + ".");
            for (ChainedCertificate link : cancellationChain) {
                String existingId = idOf(certificatesById, link.certificate());
                if (existingId != null) {
                    // Already carried for the current chain or an earlier cancellation.
                    if (link.id().equals(leafId)) {
                        leafId = existingId;
                    }
                    continue;
                }
                certificatesById.put(link.id(), link.certificate());
                certificateIssuers.put(link.id(), link.issuerId());
            }
            cancellationCertificateRefs.put(cancellation, leafId);
        }
        catBuilder.setCertificates(certificatesById).setCertificateIssuers(certificateIssuers);

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
                    // No protectionScheme: S-124 data is unprotected (dataProtection=false) and
                    // the S-124 clause 12.2.2 profile has no protectionScheme attribute.
                    .setDataProtection(false)
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
                    .setProducingAgencyPhone(cfg.phone)
                    .setProducingAgencyPhoneType(cfg.phoneType)
                    .setProducingAgencyElectronicMailAddresses(cfg.emails)
                    .setProducingAgencyCity(cfg.city)
                    .setProducingAgencyAdministrativeArea(cfg.administrativeArea)
                    .setProducingAgencyPostalCode(cfg.postalCode)
                    .setProducingAgencyCountry(cfg.country)
                    .setProducingAgencyOnlineResource(cfg.onlineResource)
                    .setProducingAgencyContactInstructions(cfg.contactInstructions)
                    .setProducerCode(cfg.producerCode)
                    .setEncodingFormat(S100EncodingFormat.GML)
                    .setDataCoverages(bbox)
                    .setComment(cfg.datasetComment)
                    .setMetadataDateStamp(LocalDate.now(ZoneOffset.UTC))
                    .setReplacedData(false)
                    // No navigationPurpose: the S-124 clause 12.2.2 profile has no such attribute.
                    .setMaintenanceFrequency(cfg.maintenanceDate == null ? null : cfg.maintenanceFrequency)
                    .setMaintenanceDate(cfg.maintenanceDate)
                    .setDigitalSignatureReference(cfg.signatureAlgorithm)
                    .build(df.bytes));
        }

        // Fileless cancellations (S-100 Part 17, clause 17-4.4.1): a discovery-metadata entry
        // with purpose=cancellation that reuses the cancelled dataset's file name, original
        // digital signature and mandatory metadata, but WITHOUT shipping a dataset file. The
        // build(null) call reuses the supplied original signature instead of signing a payload.
        for (Cancellation cancellation : cfg.cancellations) {
            S100SEDigitalSignatureReference signatureReference =
                    Optional.ofNullable(cancellation.signatureReference()).orElse(cfg.signatureAlgorithm);
            catBuilder.addDatasetMetadata(builder -> builder
                    .setFileName("file:/" + cancellation.fileName())
                    .setDatasetID(cancellation.datasetId())
                    .setCompressionFlag(false)
                    .setDataProtection(false)
                    .setCopyright(true)
                    .setClassification(cfg.classification)
                    .setPurpose(S100Purpose.CANCELLATION)
                    .setNotForNavigation(cfg.notForNavigation)
                    .setSpecificUsage(cfg.specificUsage)
                    .setEditionNumber(cancellation.editionNumber())
                    .setUpdateNumber(cancellation.updateNumber())
                    .setIssueDate(cancellation.issueDate())
                    .setBoundingBox(cancellation.boundingBox())
                    .setProductSpecification(cfg.productSpecification)
                    .setProducingAgency(cfg.organization)
                    .setProducingAgencyRole(cfg.producingAgencyRole)
                    .setProducingAgencyPhone(cfg.phone)
                    .setProducingAgencyPhoneType(cfg.phoneType)
                    .setProducingAgencyElectronicMailAddresses(cfg.emails)
                    .setProducingAgencyCity(cfg.city)
                    .setProducingAgencyAdministrativeArea(cfg.administrativeArea)
                    .setProducingAgencyPostalCode(cfg.postalCode)
                    .setProducingAgencyCountry(cfg.country)
                    .setProducingAgencyOnlineResource(cfg.onlineResource)
                    .setProducingAgencyContactInstructions(cfg.contactInstructions)
                    .setProducerCode(cfg.producerCode)
                    .setEncodingFormat(S100EncodingFormat.GML)
                    .setDataCoverages(cancellation.boundingBox())
                    .setComment(cfg.datasetComment)
                    .setMetadataDateStamp(LocalDate.now(ZoneOffset.UTC))
                    .setReplacedData(false)
                    .setMaintenanceFrequency(cfg.maintenanceDate == null ? null : cfg.maintenanceFrequency)
                    .setMaintenanceDate(cfg.maintenanceDate)
                    .setDigitalSignatureReference(signatureReference)
                    .setDigitalSignatureValues(withCertificateRef(cancellation.signatureValues(),
                            cancellationCertificateRefs.get(cancellation)))
                    .build(null));
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

    /**
     * A fileless dataset cancellation, per S-100 Ed 5.x Part 17, clause 17-4.4.1.
     *
     * <p>The cancelled dataset's file is <em>not</em> shipped in the exchange set; instead the
     * catalogue carries a discovery-metadata entry with {@code purpose=cancellation} that
     * reproduces the cancelled dataset's identifying metadata and reuses its <em>original</em>
     * digital signature, so a consumer can match and remove exactly the dataset it received.</p>
     *
     * @param fileName          the cancelled dataset's file name (without the {@code file:/} prefix,
     *                          which the factory adds); required
     * @param datasetId         the cancelled dataset's identifier (MRN); may be {@code null}
     * @param editionNumber     the cancelled dataset's edition number
     * @param updateNumber      the cancelled dataset's update number
     * @param issueDate         the issue date of the fileless cancellation <em>itself</em>, not the
     *                          cancelled dataset's; required. S-100 5.2.0 clause 17-4.4.1 requires
     *                          "all other mandatory metadata fields also set to the same values as
     *                          the original, with the exception of the issueDate, which must be set
     *                          to the issue date of the fileless cancellation itself"
     * @param boundingBox       the cancelled dataset's bounding box; required, since boundingBox is
     *                          mandatory (Mult 1) in the S-124 clause 12.2.2 profile and a fileless
     *                          cancellation reproduces the original's mandatory metadata
     * @param signatureReference the algorithm of the original signature; {@code null} falls back to
     *                          the exchange set's {@code signatureAlgorithm}
     * @param signatureValues   the cancelled dataset's <em>original</em> digital signature value(s);
     *                          required and non-empty (the catalogue schema mandates at least one)
     */
    public record Cancellation(
            String fileName,
            String datasetId,
            BigInteger editionNumber,
            BigInteger updateNumber,
            LocalDate issueDate,
            Geometry boundingBox,
            S100SEDigitalSignatureReference signatureReference,
            List<S100DatasetDiscoveryMetadata.DigitalSignatureValue> signatureValues,
            List<String> certificatePems) {

        /**
         * A cancellation signed with the exchange set's current Data Server certificate.
         * Use the canonical constructor instead when the original dataset was signed with a
         * certificate that has since been replaced.
         */
        public Cancellation(String fileName, String datasetId, BigInteger editionNumber,
                BigInteger updateNumber, LocalDate issueDate, Geometry boundingBox,
                S100SEDigitalSignatureReference signatureReference,
                List<S100DatasetDiscoveryMetadata.DigitalSignatureValue> signatureValues) {
            this(fileName, datasetId, editionNumber, updateNumber, issueDate, boundingBox,
                    signatureReference, signatureValues, List.of());
        }

        public Cancellation {
            certificatePems = certificatePems == null ? List.of() : List.copyOf(certificatePems);
            Objects.requireNonNull(fileName, "cancellation fileName must be set");
            Objects.requireNonNull(issueDate, "cancellation issueDate must be set");
            Objects.requireNonNull(boundingBox, "cancellation boundingBox must be set (S-124 "
                    + "clause 12.2.2: boundingBox is mandatory in dataset discovery metadata)");
            if (signatureValues == null || signatureValues.isEmpty()) {
                throw new IllegalArgumentException("cancellation must carry the original dataset's "
                        + "digital signature (S-100 Part 17 clause 17-4.4.1: a fileless cancellation "
                        + "reuses the original signature)");
            }
        }
    }

    /** Fluent builder for {@link S124ExchangeSetFactory}. */
    public static final class Builder {
        private List<Dataset> datasets = Collections.emptyList();
        private List<Cancellation> cancellations = Collections.emptyList();
        private String organization;
        private String producerCode;
        private String certificatePem;
        private List<String> intermediateCertificatePems = Collections.emptyList();
        private S124Signer signer;

        private String identifier;
        private String dataServerIdentifier;
        private String datasetMrnPrefix = DEFAULT_DATASET_MRN_PREFIX;
        private S100ProductSpecification productSpecification = S124ProductSpecification.defaultSpec();
        private String description;
        private String comment;
        private String datasetComment;
        private String specificUsage = SPECIFIC_USAGE;
        private List<String> emails = Collections.emptyList();
        private String phone;
        private TelephoneType phoneType = TelephoneType.VOICE;
        private String city;
        private String postalCode;
        private String country;
        private String administrativeArea;
        private List<Locale> locales = Collections.singletonList(Locale.ENGLISH);
        // S-100 Part 15, clause 15-8.11.1: the Scheme Administrator identity, whose root
        // certificate the OEM installs separately; "The encoding of IHO as schemeAdministrator
        // is <S100SE:schemeAdministrator id="IHO"/>".
        private String schemeAdministrator = "IHO";
        // S-100 Part 15, clause 15-8.7: "The digitalSignatureReference field must be encoded
        // 'ECDSA-384-SHA2'."
        private S100SEDigitalSignatureReference signatureAlgorithm = S100SEDigitalSignatureReference.ECDSA_384_SHA_2;
        private boolean notForNavigation = true;
        private SecurityClassification classification = SecurityClassification.UNCLASSIFIED;
        private RoleCode producingAgencyRole = RoleCode.CUSTODIAN;
        // S-100 Part 17 restricts MD_MaintenanceFrequencyCode to asNeeded and irregular.
        private MaintenanceFrequency maintenanceFrequency = MaintenanceFrequency.AS_NEEDED;
        // resourceMaintenance is optional (0..1) but MD_MaintenanceInformation requires the
        // frequency and a maintenance date together, so it is only encoded when a date is given.
        private LocalDate maintenanceDate;
        private String onlineResource;
        private String contactInstructions;

        private Builder() {}

        public Builder datasets(List<Dataset> datasets) { this.datasets = datasets; return this; }
        /** Fileless dataset cancellations (S-100 Part 17, clause 17-4.4.1); see {@link Cancellation}. */
        public Builder cancellations(List<Cancellation> cancellations) { this.cancellations = cancellations; return this; }
        public Builder organization(String organization) { this.organization = organization; return this; }
        public Builder producerCode(String producerCode) { this.producerCode = producerCode; return this; }
        public Builder certificatePem(String certificatePem) { this.certificatePem = certificatePem; return this; }

        /**
         * Certificates of the domain coordinators between the Data Server certificate and the
         * scheme administrator, in any order. Required when the Data Server certificate was
         * not issued by the scheme administrator directly: S-100 Part 15, clause 15-8.7,
         * obliges the Data Server to include its Domain Coordinator's certificate so the OEM
         * can perform a full path validation without external access. The scheme
         * administrator's own root certificate must NOT be included; the OEM installs it
         * separately.
         */
        public Builder intermediateCertificatePems(List<String> pems) {
            this.intermediateCertificatePems = pems == null ? Collections.emptyList() : List.copyOf(pems);
            return this;
        }
        public Builder signer(S124Signer signer) { this.signer = signer; return this; }
        public Builder identifier(String identifier) { this.identifier = identifier; return this; }
        public Builder dataServerIdentifier(String id) { this.dataServerIdentifier = id; return this; }
        public Builder datasetMrnPrefix(String prefix) { this.datasetMrnPrefix = prefix; return this; }
        public Builder productSpecification(S100ProductSpecification spec) { this.productSpecification = spec; return this; }
        public Builder description(String description) { this.description = description; return this; }
        public Builder comment(String comment) { this.comment = comment; return this; }
        public Builder datasetComment(String comment) { this.datasetComment = comment; return this; }
        /**
         * Overrides the dataset entries' specific usage. S-124 clause 12.2.2 fixes the value to
         * "Navigational Warning Service", so only that value (or {@code null}, which omits the
         * optional attribute) is accepted.
         */
        public Builder specificUsage(String usage) {
            if (usage != null && !SPECIFIC_USAGE.equals(usage)) {
                throw new IllegalArgumentException(String.format(
                        "specificUsage must be \"%s\" (S-124 clause 12.2.2)", SPECIFIC_USAGE));
            }
            this.specificUsage = usage;
            return this;
        }
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
        /**
         * Overrides the dataset entries' maintenance frequency. S-100 Part 17 restricts
         * MD_MaintenanceFrequencyCode in discovery metadata to {@code asNeeded} and
         * {@code irregular}; all other ISO 19115-1 values are rejected.
         */
        public Builder maintenanceFrequency(MaintenanceFrequency f) {
            if (f != MaintenanceFrequency.AS_NEEDED && f != MaintenanceFrequency.IRREGULAR) {
                throw new IllegalArgumentException("maintenanceFrequency must be asNeeded or "
                        + "irregular (S-100 Part 17 restricts MD_MaintenanceFrequencyCode to these values)");
            }
            this.maintenanceFrequency = f;
            return this;
        }

        /**
         * Date of the resource maintenance. Encoding {@code resourceMaintenance} at all is
         * optional, but S-100 Part 17 MD_MaintenanceInformation requires the maintenance
         * frequency and this date together, so no maintenance information is encoded unless
         * this is set.
         */
        public Builder maintenanceDate(LocalDate d) { this.maintenanceDate = d; return this; }

        /**
         * Identity of the S-100 Scheme Administrator that issued the Data Server certificate,
         * used as the {@code schemeAdministrator} id and as the certificate's {@code issuer}
         * reference in both CATALOG.XML and CATALOG.SIGN. Defaults to {@code IHO}; override
         * only when the certificate chains to a different scheme administrator.
         */
        public Builder schemeAdministrator(String id) {
            // The id is a required XML attribute and the anchor every certificate's issuer
            // reference resolves to, so an absent one makes both catalogue files unusable.
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("schemeAdministrator id must not be blank "
                        + "(S-100 Part 15, clause 15-8.11.1: the Scheme Administrator identity "
                        + "is carried in the id attribute of the schemeAdministrator element)");
            }
            this.schemeAdministrator = id;
            return this;
        }

        /** Producing agency online resource; one of the CI_Contact attributes of Table 17-3 NOTE 2. */
        public Builder onlineResource(String url) { this.onlineResource = url; return this; }

        /** Producing agency contact instructions; one of the CI_Contact attributes of Table 17-3 NOTE 2. */
        public Builder contactInstructions(String instructions) { this.contactInstructions = instructions; return this; }

        public S124ExchangeSetFactory build() {
            Objects.requireNonNull(datasets, "datasets must be set");
            Objects.requireNonNull(cancellations, "cancellations must be set");
            if (datasets.isEmpty() && cancellations.isEmpty()) {
                throw new IllegalArgumentException("at least one dataset or cancellation must be provided");
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
                description = String.format("S-124 Exchange Set generated by %s at %sZ",
                        organization,
                        LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            return new S124ExchangeSetFactory(this);
        }
    }

    /**
     * Thrown when assembling the exchange set fails for IO, JAXB or certificate reasons, or
     * when the content to package violates a hard S-124 limit.
     */
    public static final class ExchangeSetException extends RuntimeException {
        public ExchangeSetException(String message) {
            super(message);
        }

        public ExchangeSetException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
