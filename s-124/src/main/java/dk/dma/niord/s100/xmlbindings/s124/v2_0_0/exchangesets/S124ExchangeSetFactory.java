package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.exchangesets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.security.auth.x500.X500Principal;

import org.grad.eNav.s100.enums.RoleCode;
import org.grad.eNav.s100.enums.SecurityClassification;
import org.grad.eNav.s100.enums.TelephoneType;
import org.grad.eNav.s100.utils.S100ExchangeCatalogueBuilder;
import org.grad.eNav.s100.utils.S100ExchangeSetUtils;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;

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
import dk.dma.niord.s100.catalog._5_2.S100SESignatureOnSignature;
import dk.dma.niord.s100.catalog._5_2.StandaloneDigitalSignature;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.DataSetIdentificationType;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.S100SpatialAttributeType;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.AbstractGMLType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.Dataset;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.GeneralAreaType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.LocalityType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.LocationNameType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.MessageSeriesIdentifierType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnAreaAffected;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnPart;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnPreamble;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.TextPlacement;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util.GeometryS124Converter;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util.S124ConformanceException;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util.S124Utils;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util.S124XsdValidator;
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
 *  │   ├── DATASET_FILES/   124&lt;producer&gt;&lt;unique code&gt;.GML
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
 * <p>The dataset entries follow the S-124 clause 12.2.2 profile of
 * {@code S100_DatasetDiscoveryMetadata}, which clause 12.1 restricts "to remove attributes that
 * are not relevant to a Navigational Warning service": none of {@code editionNumber},
 * {@code updateNumber}, {@code dataCoverage}, {@code replacedData}, {@code resourceMaintenance},
 * {@code protectionScheme} and {@code navigationPurpose} is encoded. Four entries are read off
 * the dataset itself:</p>
 * <ul>
 *   <li>{@code boundingBox}, which clause 12.2.2 makes mandatory, is the dataset's
 *       {@code gml:boundedBy} envelope or, for datasets that declare none, the extent of the
 *       geometry their members carry; a dataset with neither is rejected. An extent that is a
 *       point or a line is padded to the strictly positive span the catalogue Schematron
 *       requires of a bounding box.</li>
 *   <li>{@code temporalExtent} is emitted only for datasets whose {@code NavwarnPreamble}
 *       carries a {@code cancellationDate} - clause 12.2.2 uses it only "when a NAVWARN have a
 *       known expiry date and time" and clause 9.3 cancels such a dataset by the pairing of that
 *       date with this metadata - and then carries the preamble's {@code publicationTime} and
 *       {@code cancellationDate}, the values clause 12.2.2 requires it to align with.</li>
 *   <li>{@code description} is the preamble's {@code generalArea} and {@code locality}, joined -
 *       clause 12.2.2 requires that "If used, content of this attribute must match the content of
 *       the generalArea and locality attributes of the dataset NavwarnPreamble". The name in the
 *       catalogue's first configured {@link Builder#locales(List) locale} is used, and a preamble
 *       naming no place leaves the attribute out, which its 0..1 multiplicity allows.</li>
 *   <li>{@code datasetID} is the {@code interoperabilityIdentifier} of the preamble's
 *       {@code messageSeriesIdentifier} when the dataset states one, which clause 12.2.2 requires
 *       it to match; otherwise it is synthesised as
 *       {@code <}{@link Builder#datasetMrnPrefix(String) datasetMrnPrefix}{@code >:<dataset gml id>}.
 *       Either way it is a Marine Resource Name, as the same clause requires.</li>
 * </ul>
 *
 * <p><strong>Conformance checking.</strong> Every dataset is checked before it is signed and
 * packaged, because a non-conformant dataset that ships with a valid signature over it reads as
 * authoritative and can only be withdrawn by re-signing the whole exchange set. Two layers apply:
 * {@code S124Utils.marshalS124} normalises and checks the rules the GML schema cannot express (see
 * {@code S124DatasetValidator}), and this factory then validates the marshalled document against
 * the S-124 application schema itself, which S-124 clause 8.1.1 requires of every dataset. The
 * latter can be turned off with {@link Builder#validateAgainstSchema(boolean)}.</p>
 *
 * <p><strong>Producer responsibilities the factory cannot check.</strong> S-100 Part 10b, clause
 * 10b-9, requires that "Feature and information associations must encode at least one of the role
 * or arcrole attributes of the reference". The factory never constructs those references - the
 * {@code header}, {@code theWarning} and {@code theReferences} associations come from the caller -
 * and S-124 defines no role or arcrole values to check them against, so populating them remains
 * the producer's responsibility.</p>
 */
public final class S124ExchangeSetFactory {

    private static final String CERTIFICATE_REF = "cer1";
    private static final String DEFAULT_DATASET_MRN_PREFIX = "urn:mrn:iho:s124";
    private static final String DEFAULT_EXCHANGE_SET_MRN_PREFIX = "urn:mrn:iho:s124:exchangeset";
    /** S-124 clause 12.2.2 fixes specificUsage: "Must always be 'Navigational Warning Service'". */
    public static final String SPECIFIC_USAGE = "Navigational Warning Service";
    /**
     * Joins the {@code generalArea} and {@code locality} names of a discovery metadata
     * description. S-124 clause 12.2.2 requires the description to "match the content of" both
     * attributes without fixing a punctuation, so the two are read as the successively narrower
     * place names they are - "The Sound, Drogden Channel".
     */
    private static final String DESCRIPTION_SEPARATOR = ", ";

    /**
     * The syntax of a Marine Resource Name, which S-124 clause 12.2.2 requires of {@code datasetID}
     * ("The URN must be an MRN"). S-100 Part 3, clause 3-10, gives the grammar as
     * {@code <URN> ::= "urn:mrn:" <OID> ":" <OSS>} - the namespace, an organisation identifier and
     * at least one namespace specific string.
     * <p/>
     * The {@code urn:mrn:} namespace is matched case-sensitively even though RFC 2141 treats URN
     * schemes as case-insensitive, because the catalogue schema does: {@code MRNType} restricts
     * {@code xs:anyURI} with {@code <xs:pattern value="urn:mrn:.+"/>}
     * (S100_ExchangeCatalogue.xsd), and XSD patterns are case-sensitive. Accepting
     * {@code URN:MRN:...} here would let a signed but schema-invalid CATALOG.XML through. The
     * organisation and namespace specific parts stay case-insensitive, which the schema allows.
     */
    private static final Pattern MRN_PATTERN =
            Pattern.compile("urn:mrn:[A-Za-z0-9][A-Za-z0-9-]*(:[A-Za-z0-9()+,\\-.:=@;$_!*'%/?#]+)+");

    /** S-124 clause 9.6: "S-124 datasets must not exceed 50KB." */
    private static final int MAX_DATASET_SIZE_BYTES = 50 * 1024;
    /**
     * The span a degenerate dataset extent - a point or a line - is padded to before it is
     * encoded as a bounding box; see {@link #withMinimumSpan(Geometry)}.
     */
    private static final double MIN_EXTENT_SPAN_DEGREES = 0.0001;

    /**
     * The number of fraction digits a latitude or longitude is encoded with in the discovery
     * metadata; see {@link #quantized(Geometry)}. S-124 clause 8.2 allows "7 or fewer".
     */
    private static final int COORDINATE_SCALE = 7;

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
        } catch (ExchangeSetException | S124ConformanceException e) {
            // Both already name the artefact and the clause they failed on; wrapping them
            // in a generic "failed to build" would bury that behind a cause chain.
            throw e;
        } catch (Exception e) {
            throw new ExchangeSetException("Failed to build S-124 exchange set", e);
        }
    }

    private List<DatasetFile> marshalDatasets() throws JAXBException {
        List<DatasetFile> result = new ArrayList<>(cfg.datasets.size());
        Set<String> fileNames = new LinkedHashSet<>();
        for (Dataset dataset : cfg.datasets) {
            String uuid = datasetUuid(dataset);
            String fileName = datasetFileName(dataset, uuid);
            if (!fileNames.add(fileName)) {
                throw new ExchangeSetException(String.format(
                        "Two S-124 datasets would both be packaged as %s, but S-100 Part 17, clause "
                                + "17-4.3, requires all base dataset file names to be unique; give the "
                                + "datasets different unique codes",
                        fileName));
            }
            String xml = S124Utils.marshalS124(dataset);
            // S-124 clause 8.1.1: "Feature instances must validate against the schema and conform
            // to all other requirements specified in this data product specification". marshalS124
            // has just applied the requirements no schema can express; this is the other half. It
            // runs before the dataset is signed and packaged, because a schema-invalid dataset that
            // ships with a valid signature over it reads as authoritative and is expensive to
            // withdraw - every correction re-signs the whole exchange set.
            if (cfg.validateAgainstSchema) {
                validateAgainstSchema(fileName, xml);
            }
            byte[] bytes = xml.getBytes(StandardCharsets.UTF_8);
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
     * The name of the file a dataset is packaged in.
     * <p/>
     * S-100 Part 17, clause 17-4.3 (mandated for S-124 by clause 9.7), names dataset files
     * XXXYYYYØØØØ.[EXT]: the product code 124, the producer code, "an arbitrary length unique
     * code in alphanumeric characters" and the encoding specific file extension - .GML for the
     * GML encoding, .XML being reserved for metadata files. Anything the dataset identifier
     * carries beyond those characters, such as the dots and hyphens of a typical gml:id, is
     * therefore dropped from the unique code rather than written into the file name.
     * <p/>
     * When the dataset header declares a {@code datasetFileIdentifier}, that name wins: S-100
     * Part 10b Table 10b-4 defines it as "The file name including the extension but excluding
     * any path information", so a packaged file under any other name would contradict the
     * dataset it contains. A declared identifier which is not a conformant file name is
     * rejected instead of being repaired, because renaming it here would leave the dataset's
     * own - signed - header pointing at a file the exchange set does not contain.
     */
    private String datasetFileName(Dataset dataset, String uuid) {
        String declared = Optional.ofNullable(dataset.getDatasetIdentificationInformation())
                .map(DataSetIdentificationType::getDatasetFileIdentifier)
                .filter(s -> !s.isBlank())
                .orElse(null);
        if (declared == null) {
            return String.format("124%s%s.GML", cfg.producerCode, uniqueCode(uuid));
        }
        if (!datasetFileNamePattern(cfg.producerCode).matcher(declared).matches()) {
            throw new ExchangeSetException(String.format(
                    "The dataset declares datasetFileIdentifier \"%s\", which is not a valid S-100 "
                            + "Part 17, clause 17-4.3, dataset file name: expected 124%s followed by "
                            + "an alphanumeric unique code and \".GML\" (S-100 Part 10b Table 10b-4 "
                            + "requires the identifier to be the name of the packaged file)",
                    declared, cfg.producerCode));
        }
        return declared;
    }

    /**
     * Validates a marshalled dataset against the S-124 GML application schema, naming the file in
     * the failure so a producer knows which of them to look at.
     * <p/>
     * The S-124 schemas ship with this library, so the check needs no network access and cannot
     * drift from the bindings that produced the document. {@code CATALOG.XML} is deliberately not
     * checked here: the S-100 Part 17 exchange catalogue schema imports the ISO 19115-3 schemas,
     * which are not vendored, so compiling it would reach out to schemas.isotc211.org - and an
     * exchange set that cannot be built without internet access is worse than one whose catalogue
     * is checked only in this project's own test suite, where that access exists.
     */
    private static void validateAgainstSchema(String fileName, String xml) {
        try {
            S124XsdValidator.validate(xml);
        } catch (org.xml.sax.SAXException e) {
            throw new ExchangeSetException(String.format(
                    "%s is not valid against its XML schema, which S-124 clause 8.1.1 requires of "
                            + "every exchange set document: %s",
                    fileName, e.getMessage()), e);
        } catch (java.io.IOException e) {
            throw new ExchangeSetException(String.format("Failed to validate %s", fileName), e);
        }
    }

    /** The Part 17, clause 17-4.3 file name pattern for S-124 datasets of one producer. */
    private static Pattern datasetFileNamePattern(String producerCode) {
        return Pattern.compile("124" + Pattern.quote(producerCode) + "[A-Za-z0-9]+\\.GML");
    }

    /** The alphanumeric unique code clause 17-4.3 builds a file name around. */
    private static String uniqueCode(String datasetId) {
        String code = datasetId.replaceAll("[^A-Za-z0-9]", "");
        if (code.isEmpty()) {
            throw new ExchangeSetException(String.format(
                    "The dataset identifier \"%s\" holds no alphanumeric character, so it yields no "
                            + "unique code for the file name required by S-100 Part 17, clause 17-4.3",
                    datasetId));
        }
        return code;
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
            // The certificate element is typed xs:base64Binary, which JAXB Base64-encodes
            // itself, so it carries the certificate DER content: clauses 15-8.6 and
            // 15-8.11.1 embed the certificate "with the header and footer lines omitted",
            // i.e. one Base64 decode of the element must yield the X.509 certificate.
            certificateType.setValue(S100ExchangeSetUtils.getDerFromCert(link.certificate()));
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
     * Orders a chain given signing certificate first - the form the public API takes one in -
     * into a resolvable path, labelling the leaf {@code leafId} and each certificate above it
     * {@code <intermediatePrefix>N}.
     */
    private List<ChainedCertificate> certificateChain(List<String> pems, String leafId,
            String intermediatePrefix) throws CertificateException {
        return certificateChain(pems.get(0), pems.subList(1, pems.size()), leafId, intermediatePrefix);
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

    /**
     * Deep copies a discovery-metadata entry through JAXB, so reproducing an original in a
     * cancellation cannot modify the caller's object.
     */
    private static S100DatasetDiscoveryMetadata copyOf(S100DatasetDiscoveryMetadata original) {
        try {
            JAXBContext context = JAXBContext.newInstance(S100DatasetDiscoveryMetadata.class);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            context.createMarshaller().marshal(original, out);
            return (S100DatasetDiscoveryMetadata) context.createUnmarshaller()
                    .unmarshal(new ByteArrayInputStream(out.toByteArray()));
        } catch (JAXBException e) {
            throw new ExchangeSetException("Failed to copy the cancelled dataset's metadata", e);
        }
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
     * Adds a certificate chain to what the catalogue carries and returns the id its leaf ended
     * up with - the id a signature made with that certificate has to reference.
     * <p/>
     * Certificates already carried - typically the domain coordinator, shared with the current
     * chain - keep the id they were first given. Every id is settled before anything is added,
     * so issuer references point at the id actually emitted rather than at the temporary one of
     * a link that turned out to be a duplicate.
     */
    private static String carry(List<ChainedCertificate> chain,
            Map<String, X509Certificate> certificatesById, Map<String, String> certificateIssuers) {
        Map<String, String> settledIds = new LinkedHashMap<>();
        for (ChainedCertificate link : chain) {
            String existingId = idOf(certificatesById, link.certificate());
            settledIds.put(link.id(), existingId != null ? existingId : link.id());
        }
        for (ChainedCertificate link : chain) {
            String id = settledIds.get(link.id());
            if (certificatesById.containsKey(id)) {
                continue;
            }
            certificatesById.put(id, link.certificate());
            // The topmost link's issuer is the scheme administrator, which is not a certificate
            // id and so passes through unchanged.
            certificateIssuers.put(id, settledIds.getOrDefault(link.issuerId(), link.issuerId()));
        }
        // The chain is ordered leaf first.
        return settledIds.get(chain.get(0).id());
    }

    /**
     * Re-labels reused signatures to reference the certificate this catalogue carries for
     * them. The signature bytes are untouched - only the document scoped id changes - and
     * the caller's objects are left alone.
     * <p/>
     * A chained signature is reproduced as the S100_SE_SignatureOnSignature it is, keeping
     * both of its references. S-100 Part 15, clause 15-8.8, implements signature chains "by
     * use of a signatureRef attribute", which clause 15-8.11.5 makes mandatory (Mult 1), so
     * demoting such a signature to a plain S100_SE_DigitalSignature would sever the chain. Its
     * certificateRef is not redirected to the data signer's certificate either: a chained
     * signature counter-signs the signature of another party, so it was made by a different
     * certified identity, and clause 15-8.8 requires each signature in the chain to reference
     * the certificate that made it. It is translated instead - from the id the original
     * catalogue gave the counter-signer's certificate, in {@code counterSignerRefs}, to the id
     * this catalogue carries that certificate under - because certificate ids are scoped to the
     * document that declares them.
     */
    private static List<S100DatasetDiscoveryMetadata.DigitalSignatureValue> withCertificateRef(
            List<S100DatasetDiscoveryMetadata.DigitalSignatureValue> values, String certificateRef,
            Map<String, String> counterSignerRefs) {
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
            } else if (original instanceof S100SESignatureOnSignature onSignature) {
                if (onSignature.getSignatureRef() == null || onSignature.getSignatureRef().isBlank()) {
                    throw new ExchangeSetException(String.format(
                            "The reused signature %s of the cancelled dataset is an "
                                    + "S100_SE_SignatureOnSignature but references no signature, "
                                    + "and S-100 Part 15, clause 15-8.11.5, makes its signatureRef "
                                    + "mandatory - clause 15-8.8 chains signatures by nothing else",
                            onSignature.getId()));
                }
                if (onSignature.getCertificateRef() == null || onSignature.getCertificateRef().isBlank()) {
                    throw new ExchangeSetException(String.format(
                            "The reused signature %s of the cancelled dataset counter-signs "
                                    + "signature %s but references no certificate; the certificate "
                                    + "of the party that made it cannot be inferred, and S-100 Part "
                                    + "15, clause 15-8.8, requires each signature in the chain to "
                                    + "have a valid certificateRef",
                            onSignature.getId(), onSignature.getSignatureRef()));
                }
                String counterSignerRef = counterSignerRefs.get(onSignature.getCertificateRef());
                if (counterSignerRef == null) {
                    throw new ExchangeSetException(String.format(
                            "The reused signature %s of the cancelled dataset counter-signs "
                                    + "signature %s with the certificate its original catalogue "
                                    + "called \"%s\", which this exchange set does not carry; supply "
                                    + "that certificate chain as the counterSignerCertificatePems "
                                    + "entry \"%s\" of the cancellation. S-100 Part 15, clause "
                                    + "15-8.7, requires every "
                                    + "certificate needed to authenticate a signature to travel with "
                                    + "the exchange set, and clause 15-8.11.5 defines certificateRef "
                                    + "as the \"Identifier of the certificate against which the "
                                    + "digital signature validates\", so a reference to a certificate "
                                    + "no catalogue element declares leaves the chain unverifiable",
                            onSignature.getId(), onSignature.getSignatureRef(),
                            onSignature.getCertificateRef(), onSignature.getCertificateRef()));
                }
                S100SESignatureOnSignature signature = new S100SESignatureOnSignature();
                signature.setId(onSignature.getId());
                signature.setValue(onSignature.getValue());
                signature.setSignatureRef(onSignature.getSignatureRef());
                // Not the data signer's certificate: the counter-signer is a different certified
                // identity, carried under an id of this catalogue's own.
                signature.setCertificateRef(counterSignerRef);
                copy.setS100SEDigitalSignature(objectFactory.createS100SESignatureOnSignature(signature));
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
                // No fallback to the country: an administrative area is a subdivision of a
                // country, not a synonym for one, and the attribute is optional. Substituting
                // the country would state something the caller never said - and something the
                // dataset-level producing agency below does not say either.
                .setAdministrativeArea(cfg.administrativeArea)
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
        // The same holds for the certificate of every party that counter-signed that signature:
        // clause 15-8.7 requires the exchange set to carry "all the certificates required to
        // perform a full certificate path validation without any external access", and clause
        // 15-8.11.5 defines certificateRef as the "Identifier of the certificate against which
        // the digital signature validates" - which an id no carried certificate has is not. The
        // ids of this catalogue are its own, so the ref the original catalogue used is
        // translated to the id the certificate is carried under here.
        Map<Cancellation, Map<String, String>> counterSignerRefs = new LinkedHashMap<>();
        int cancellationIndex = 0;
        int counterSignerIndex = 0;
        for (Cancellation cancellation : cfg.cancellations) {
            Map<String, String> refs = new LinkedHashMap<>();
            for (Map.Entry<String, List<String>> counterSigner
                    : cancellation.counterSignerCertificatePems().entrySet()) {
                counterSignerIndex++;
                refs.put(counterSigner.getKey(), carry(
                        certificateChain(counterSigner.getValue(), "cerS" + counterSignerIndex,
                                "caS" + counterSignerIndex + "."),
                        certificatesById, certificateIssuers));
            }
            counterSignerRefs.put(cancellation, refs);
            if (cancellation.certificatePems().isEmpty()) {
                // No certificate supplied: the signature was made with the current one.
                cancellationCertificateRefs.put(cancellation, CERTIFICATE_REF);
                continue;
            }
            cancellationIndex++;
            cancellationCertificateRefs.put(cancellation, carry(
                    certificateChain(cancellation.certificatePems(), "cerC" + cancellationIndex,
                            "caC" + cancellationIndex + "."),
                    certificatesById, certificateIssuers));
        }
        catBuilder.setCertificates(certificatesById).setCertificateIssuers(certificateIssuers);

        for (DatasetFile df : datasetFiles) {
            Geometry bbox = datasetExtent(df);
            DataSetIdentificationType ident = df.dataset.getDatasetIdentificationInformation();
            // The fallback is UTC rather than the JVM default zone, like every other date this
            // catalogue stamps: a producer west of Greenwich building an exchange set late in the
            // day would otherwise date it a day early.
            LocalDate issueDate = Optional.ofNullable(ident)
                    .map(DataSetIdentificationType::getDatasetReferenceDate)
                    .orElseGet(() -> LocalDate.now(ZoneOffset.UTC));
            // S-124 clause 12.2.2: the temporal extent "is only used when a NAVWARN have a
            // known expiry date and time. When used the values must align with the
            // publicationTime and cancellationDate attributes of the dataset NavwarnPreamble".
            // Clause 9.3 cancels a dataset by that pairing - "Populating the cancellationDate
            // attribute in the dataset and the temporalExtent in the metadata (see 12.2.2), and
            // that date has passed" - so the extent is read off the preamble rather than
            // configured: a caller supplied value could only disagree with the dataset.
            NavwarnPreamble preamble = preambleOf(df.dataset);
            LocalDateTime cancellationDate = preamble == null ? null : utc(preamble.getCancellationDate());
            // Clause 12.2.2.2: "if both are known, both must be populated". Without a known
            // expiry there is no temporal extent at all, so the publication time is only
            // carried alongside a cancellation date.
            LocalDateTime publicationTime = cancellationDate == null ? null : utc(preamble.getPublicationTime());
            // S-124 clause 12.2.2 defines issueTime as the "Time of day at which the data was made
            // available by the Data Producer", the same moment issueDate above gives the date of.
            // The preamble's publicationTime is that moment, and it is the only record of it the
            // factory holds; a dataset that does not state one leaves the attribute out, which its
            // 0..1 multiplicity allows. Stamping a constant would assert a time never given.
            //
            // The two are only emitted together when they agree on the day. issueDate comes from
            // the dataset's datasetReferenceDate, which the producer states in its own frame, while
            // publicationTime is normalised to UTC - so near midnight the pair could otherwise name
            // an instant up to a day from the real one. When they disagree the date is kept, being
            // the dataset's own declared issue date, and the time is dropped rather than made to
            // contradict it.
            LocalDateTime publication = preamble == null ? null : utc(preamble.getPublicationTime());
            LocalTime issueTime = publication != null && publication.toLocalDate().equals(issueDate)
                    ? publication.toLocalTime()
                    : null;

            catBuilder.addDatasetMetadata(builder -> s124Profile(builder
                    .setFileName("file:/" + df.fileName)
                    .setDatasetID(datasetId(preamble, df))
                    .setDescription(datasetDescription(preamble))
                    .setCompressionFlag(false)
                    // No protectionScheme: S-124 data is unprotected (dataProtection=false) and
                    // the S-124 clause 12.2.2 profile has no protectionScheme attribute.
                    .setDataProtection(false)
                    .setCopyright(true)
                    .setClassification(cfg.classification)
                    .setPurpose(S100Purpose.NEW_DATASET)
                    .setNotForNavigation(cfg.notForNavigation)
                    .setSpecificUsage(cfg.specificUsage)
                    // No editionNumber and no updateNumber: the S-124 clause 12.2.2 profile has
                    // no such attributes (see s124Profile).
                    .setIssueDate(issueDate)
                    .setIssueTime(issueTime)
                    .setBoundingBox(bbox)
                    .setTimeInstantBegin(publicationTime)
                    .setTimeInstantEnd(cancellationDate)
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
                    .setComment(cfg.datasetComment)
                    .setMetadataDateStamp(LocalDate.now(ZoneOffset.UTC))
                    // No dataCoverage, no replacedData, no resourceMaintenance and no
                    // navigationPurpose: the S-124 clause 12.2.2 profile has no such attributes
                    // (see s124Profile).
                    .setDigitalSignatureReference(cfg.signatureAlgorithm)
                    .build(df.bytes)));
        }

        // Fileless cancellations (S-100 Part 17, clause 17-4.4.1): a discovery-metadata entry
        // with purpose=cancellation that reuses the cancelled dataset's file name, original
        // digital signature and mandatory metadata, but WITHOUT shipping a dataset file. The
        // build(null) call reuses the supplied original signature instead of signing a payload.
        for (Cancellation cancellation : cfg.cancellations) {
            // Clause 17-4.4.1 requires every other mandatory field to keep the value it had in
            // the original, so the original entry is reproduced rather than rebuilt from the
            // current configuration, which may have moved on since the dataset was issued.
            S100DatasetDiscoveryMetadata entry = copyOf(cancellation.original());
            entry.setPurpose(S100Purpose.CANCELLATION);
            entry.setIssueDate(cancellation.issueDate());
            List<S100DatasetDiscoveryMetadata.DigitalSignatureValue> signatures = withCertificateRef(
                    entry.getDigitalSignatureValues(), cancellationCertificateRefs.get(cancellation),
                    counterSignerRefs.get(cancellation));
            entry.getDigitalSignatureValues().clear();
            entry.getDigitalSignatureValues().addAll(signatures);
            catBuilder.addDatasetMetadata(builder -> entry);
        }

        try {
            return S100ExchangeSetUtils.marshalS100ExchangeSetCatalogue(catBuilder.build());
        } catch (java.security.cert.CertificateEncodingException e) {
            throw new CertificateException(e);
        }
    }

    /**
     * Applies the restriction of S-124 clause 12.1 to a finished dataset entry: "the
     * S100_DatasetDiscoveryMetadata is further restricted to remove attributes that are not
     * relevant to a Navigational Warning service."
     * <p/>
     * Neither the clause 12.2.2 encoding table nor the metadata model figure of clause 12.1
     * carries editionNumber, updateNumber, dataCoverage, replacedData or resourceMaintenance -
     * consistent with clause 9.4, "S-124 does not support delta changes to issued S-124
     * datasets" - so none of them is encoded here. Four of the five are simply never handed to
     * the shared S-100 builder; replacedData is dropped afterwards instead, because the builder
     * takes it as a primitive boolean and so always encodes it.
     */
    private static S100DatasetDiscoveryMetadata s124Profile(S100DatasetDiscoveryMetadata metadata) {
        metadata.setReplacedData(null);
        return metadata;
    }

    /**
     * The extent of a dataset, as the mandatory bounding box of its discovery metadata.
     * <p/>
     * S-124 clause 12.2.2 tightens the multiplicity S-100 Part 17 gives boundingBox to 1:
     * "boundingBox | The extent of the dataset limits | 1 | EX_GeographicBoundingBox". The
     * dataset's own {@code gml:boundedBy} is used whenever it declares one; that element is
     * optional in the S-100 GML profile, so for the datasets which omit it the extent is
     * derived from the geometry of the dataset's members instead. A dataset that carries
     * neither has no extent to describe and is rejected, rather than packaged behind a
     * catalogue entry missing a mandatory element. Either way the extent is padded to a
     * strictly positive span, the only form the catalogue can encode (see
     * {@link #withMinimumSpan(Geometry)}).
     */
    private static Geometry datasetExtent(DatasetFile df) {
        Geometry declared = GeometryS124Converter.envelopeToJts(df.dataset.getBoundedBy());
        if (declared != null) {
            return quantized(withMinimumSpan(declared));
        }
        Geometry derived;
        try {
            derived = memberExtent(df.dataset);
        } catch (RuntimeException e) {
            throw new ExchangeSetException(String.format(
                    "The S-124 dataset packaged as %s declares no gml:boundedBy and its member "
                            + "geometry cannot be read, so the extent required by S-124 clause "
                            + "12.2.2 (boundingBox, multiplicity 1) cannot be derived; declare "
                            + "the dataset's gml:boundedBy envelope",
                    df.fileName), e);
        }
        if (derived == null) {
            throw new ExchangeSetException(String.format(
                    "The S-124 dataset packaged as %s declares no gml:boundedBy and carries no "
                            + "member geometry, so it has no extent; S-124 clause 12.2.2 makes "
                            + "boundingBox mandatory (multiplicity 1) in the dataset discovery "
                            + "metadata",
                    df.fileName));
        }
        return quantized(withMinimumSpan(derived));
    }

    /**
     * The extent padded to a strictly positive span in both dimensions, the only shape the
     * exchange catalogue can encode as a bounding box.
     * <p/>
     * The most common NAVWARN extent is a single position - a wreck or an obstruction - and a
     * curve of constant latitude is degenerate in one dimension too, so the geographic extent
     * of a dataset is regularly a point or a line. The S-100 Part 17 catalogue Schematron
     * (S100_XC.sch, pattern S100_ValidBBoxPattern) rejects such a bounding box: it asserts,
     * at error level, that "northBoundLatitude ... must be greater than southBoundLatitude",
     * and warns unless westBoundLongitude is less than eastBoundLongitude. A degenerate extent
     * is therefore padded outwards to {@value #MIN_EXTENT_SPAN_DEGREES} degrees - about 11 m,
     * far below the positional accuracy any NAVWARN position is stated with, and the box still
     * contains the geometry it describes. The padding never leaves the coordinate domain the
     * same pattern asserts ("values are latitude and longitude in decimal degrees in +/-90 or
     * +/-180 range"): a box that would cross a pole or the antimeridian is shifted back inside
     * it instead of being widened past it.
     */
    /**
     * Quantises an extent to the coordinate precision of S-124 clause 8.2: "Values of latitude and
     * longitude can be accurate up to 7 decimal places. Coordinate values should be coded as decimal
     * numbers with 7 or fewer digits after the decimal."
     * <p/>
     * Without this the raw {@code double} reaches {@code gco:Decimal} through
     * {@link BigDecimal#valueOf(double)}, which takes the shortest round-trip decimal expansion and
     * applies no scale, so the IEEE-754 representation error is published verbatim - a catalogue
     * built from the same envelope as a dataset's {@code gml:boundedBy} printed
     * {@code 12.770000000000001} where the dataset itself printed {@code 12.7700000}. Rounding the
     * {@code double} rather than the {@code BigDecimal} also drops the trailing zeros, which S-124
     * clause 8.3 asks for: "Floating point attribute values must not contain non-significant
     * trailing zeros exceeding the attribute's precision".
     * <p/>
     * This belongs here and not in {@code S100ExchangeSetUtils}, which builds bounding boxes for
     * every S-100 product: the 7-digit limit is an S-124 rule, and a product whose datasets carry
     * finer geometry would have its extent silently altered - and, since half-up rounding can move
     * a bound inward, could end up declaring a box that no longer contains its own data.
     * <p/>
     * Half-up is safe for S-124 specifically, because the dataset's own coordinates are published at
     * exactly this precision and rounding mode: {@code DoubleListAdapter} formats every GML
     * coordinate with {@code %.7f}. Rounding is monotonic, so quantising the envelope of the raw
     * coordinates gives precisely the envelope of the quantised coordinates - the declared box
     * equals the extent of the geometry as encoded, rather than merely containing it. Padding is
     * applied before this, and a rounding step of at most 5e-8 per bound cannot close the 1e-4 span
     * {@link #MIN_EXTENT_SPAN_DEGREES} guarantees.
     */
    private static Geometry quantized(Geometry extent) {
        Envelope envelope = extent.getEnvelopeInternal();
        return new GeometryFactory(new PrecisionModel(), 4326)
                .toGeometry(new Envelope(
                        quantized(envelope.getMinX()), quantized(envelope.getMaxX()),
                        quantized(envelope.getMinY()), quantized(envelope.getMaxY())));
    }

    private static double quantized(double coordinate) {
        return BigDecimal.valueOf(coordinate)
                .setScale(COORDINATE_SCALE, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static Geometry withMinimumSpan(Geometry extent) {
        Envelope envelope = extent.getEnvelopeInternal();
        if (envelope.getWidth() >= MIN_EXTENT_SPAN_DEGREES
                && envelope.getHeight() >= MIN_EXTENT_SPAN_DEGREES) {
            return extent;
        }
        double[] longitudes = paddedRange(envelope.getMinX(), envelope.getMaxX(), -180.0, 180.0);
        double[] latitudes = paddedRange(envelope.getMinY(), envelope.getMaxY(), -90.0, 90.0);
        return new GeometryFactory(new PrecisionModel(), 4326)
                .toGeometry(new Envelope(longitudes[0], longitudes[1], latitudes[0], latitudes[1]));
    }

    /**
     * The range {@code [min, max]} padded to the minimum span, staying within
     * {@code [floor, ceiling]} and still containing the original range.
     */
    private static double[] paddedRange(double min, double max, double floor, double ceiling) {
        double missing = MIN_EXTENT_SPAN_DEGREES - (max - min);
        if (missing <= 0) {
            return new double[] {min, max};
        }
        double paddedMin = min - missing / 2;
        double paddedMax = max + missing / 2;
        // At most one of the two corrections is ever non-zero: the domain is orders of
        // magnitude wider than the padding.
        double shift = Math.max(0, floor - paddedMin) - Math.max(0, paddedMax - ceiling);
        return new double[] {paddedMin + shift, paddedMax + shift};
    }

    /**
     * The union of the extents of the geometry the dataset's members carry, as a JTS geometry
     * in EPSG:4326, or {@code null} when no member carries any geometry.
     */
    private static Geometry memberExtent(Dataset dataset) {
        Envelope extent = new Envelope();
        for (S100SpatialAttributeType property : spatialProperties(dataset)) {
            // One property at a time: the converter unions the list it is handed, and a union
            // across the mixed geometry types of a warning is neither needed for an extent nor
            // always defined.
            extent.expandToInclude(GeometryS124Converter
                    .pointCurveSurfaceToGeometry(Collections.singletonList(property))
                    .getEnvelopeInternal());
        }
        if (extent.isNull()) {
            return null;
        }
        return new GeometryFactory(new PrecisionModel(), 4326).toGeometry(extent);
    }

    /** Every point, curve and surface property carried by the dataset's members. */
    private static List<S100SpatialAttributeType> spatialProperties(Dataset dataset) {
        List<S100SpatialAttributeType> properties = new ArrayList<>();
        for (AbstractGMLType member : members(dataset)) {
            if (member instanceof NavwarnPart part) {
                for (NavwarnPart.Geometry geometry : part.getGeometries()) {
                    addAll(properties, geometry.getPointProperty(), geometry.getCurveProperty(),
                            geometry.getSurfaceProperty());
                }
            } else if (member instanceof NavwarnAreaAffected area) {
                for (NavwarnAreaAffected.Geometry geometry : area.getGeometries()) {
                    addAll(properties, geometry.getPointProperty(), geometry.getCurveProperty(),
                            geometry.getSurfaceProperty());
                }
            } else if (member instanceof TextPlacement textPlacement
                    && textPlacement.getGeometry() != null) {
                addAll(properties, textPlacement.getGeometry().getPointProperty());
            }
        }
        return properties;
    }

    private static void addAll(List<S100SpatialAttributeType> target, S100SpatialAttributeType... properties) {
        for (S100SpatialAttributeType property : properties) {
            if (property != null) {
                target.add(property);
            }
        }
    }

    /**
     * The dataset's NavwarnPreamble, the feature S-124 clause 12.2.2 aligns the temporal extent
     * with, or {@code null} when the dataset carries none.
     * <p/>
     * A dataset carrying more than one is rejected rather than resolved by taking the first.
     * S-124 clause 4 allows only one navigational warning per dataset, so there is no correct
     * choice to make between several preambles - and picking one silently would publish a
     * catalogue entry whose temporal extent describes one arbitrary warning as if it were the
     * expiry of the whole dataset. {@code S124DatasetValidator} rejects the same dataset on the
     * marshal path; this guard also covers a caller who assembled the catalogue by another route.
     */
    private static NavwarnPreamble preambleOf(Dataset dataset) {
        List<NavwarnPreamble> preambles = members(dataset).stream()
                .filter(NavwarnPreamble.class::isInstance)
                .map(NavwarnPreamble.class::cast)
                .toList();
        if (preambles.size() > 1) {
            throw new ExchangeSetException(String.format(
                    "The S-124 dataset carries %d NavwarnPreamble instances, but S-124 clause 4 "
                            + "allows only one navigational warning per dataset, so its discovery "
                            + "metadata has no single temporal extent to describe; package one "
                            + "warning per dataset",
                    preambles.size()));
        }
        return preambles.stream()
                .findFirst()
                .orElse(null);
    }

    /**
     * The {@code datasetID} of a dataset's discovery metadata entry.
     * <p/>
     * S-124 clause 12.2.2, datasetID row: "The URN must be an MRN and if used match the value of
     * interoperabilityIdentifier in the messageSeriesIdentifier". When the dataset states that
     * identifier there is nothing to reconcile - it <em>is</em> the value the catalogue must carry,
     * so it is used verbatim and the two artefacts agree by construction rather than by the
     * producer's care. Only a dataset that states none falls back to the synthesised
     * {@code <prefix>:<dataset id>}.
     * <p/>
     * When the dataset states an identifier that is <em>not</em> an MRN, the attribute is omitted
     * rather than the build failed. S-124 clause 4.3.3 only says interoperabilityIdentifier
     * "should follow the MRN concept", and clause 1.4.1 defines "should" as explicitly
     * non-mandatory, so such a dataset is conformant; but no {@code datasetID} could then satisfy
     * both halves of the clause 12.2.2 sentence at once - it would either not be an MRN or not
     * match the dataset. Since the attribute is optional (0..1), leaving it out is the one
     * conformant option, and it is the same policy the description and administrativeArea
     * attributes already follow.
     */
    private String datasetId(NavwarnPreamble preamble, DatasetFile df) {
        String interoperabilityIdentifier = Optional.ofNullable(preamble)
                .map(NavwarnPreamble::getMessageSeriesIdentifier)
                .map(MessageSeriesIdentifierType::getInteroperabilityIdentifier)
                .filter(s -> !s.isBlank())
                .orElse(null);
        if (interoperabilityIdentifier != null) {
            return MRN_PATTERN.matcher(interoperabilityIdentifier).matches()
                    ? interoperabilityIdentifier
                    : null;
        }
        // The synthesised form is the library's own construction, so an invalid one is a
        // misconfiguration the producer can act on rather than a property of the dataset. The
        // prefix is validated when it is set; the dataset's gml:id is not, and a gml:id may hold
        // characters - it is an NCName, so non-ASCII letters are legal - that a URN may not.
        String synthesised = cfg.datasetMrnPrefix + ":" + df.uuid;
        if (!MRN_PATTERN.matcher(synthesised).matches()) {
            throw new ExchangeSetException(String.format(
                    "The dataset id \"%s\" yields datasetID \"%s\", which is not a Marine Resource "
                            + "Name as S-124 clause 12.2.2 requires; give the dataset an id made of "
                            + "URN characters, or set the interoperabilityIdentifier of its "
                            + "messageSeriesIdentifier to the MRN the catalogue should carry",
                    df.uuid, synthesised));
        }
        return synthesised;
    }

    /**
     * The {@code description} of a dataset's discovery metadata entry.
     * <p/>
     * S-124 clause 12.2.2, description row: "If used, content of this attribute must match the
     * content of the generalArea and locality attributes of the dataset NavwarnPreamble" - a
     * stricter rule than the S-100 Part 17, clause 17-4.5, definition it profiles ("Short
     * description giving the area or location covered by the dataset"). The value is therefore
     * derived from the preamble rather than from the dataset abstract, which is a different field
     * written for a different purpose and which typically opens with the issuing authority and the
     * warning number - content that appears in neither {@code generalArea} nor {@code locality}.
     * <p/>
     * Both attributes are lists of localised {@code locationName}s, while {@code description} is a
     * single unlocalised {@code CharacterString}, so one language has to be chosen: the catalogue's
     * first configured locale, falling back to whichever name the dataset lists first. A dataset
     * with neither attribute has nothing the rule can be satisfied from, and since the attribute is
     * optional ("If used") it is then omitted - which conforms, where re-using the abstract would
     * not.
     */
    private String datasetDescription(NavwarnPreamble preamble) {
        if (preamble == null) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        for (GeneralAreaType generalArea : preamble.getGeneralAreas()) {
            addLocationName(parts, generalArea.getLocationNames());
        }
        for (LocalityType locality : preamble.getLocalities()) {
            addLocationName(parts, locality.getLocationNames());
        }
        return parts.isEmpty() ? null : String.join(DESCRIPTION_SEPARATOR, parts);
    }

    /** Appends the localised name matching the catalogue's language, or the first one given. */
    private void addLocationName(List<String> parts, List<LocationNameType> names) {
        String language = cfg.locales.isEmpty() ? null : cfg.locales.get(0).getISO3Language();
        names.stream()
                .filter(n -> n.getText() != null && !n.getText().isBlank())
                .filter(n -> language == null || language.equalsIgnoreCase(n.getLanguage()))
                .findFirst()
                .or(() -> names.stream().filter(n -> n.getText() != null && !n.getText().isBlank()).findFirst())
                .map(LocationNameType::getText)
                .ifPresent(parts::add);
    }

    private static List<AbstractGMLType> members(Dataset dataset) {
        return Optional.ofNullable(dataset.getMembers())
                .map(Dataset.Members::getNavwarnPartsAndNavwarnAreaAffectedsAndTextPlacements)
                .orElseGet(Collections::emptyList);
    }

    /** The instant as a UTC date and time, the form the catalogue encodes it in. */
    private static LocalDateTime utc(OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
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
     * A fileless dataset cancellation (S-100 Part 17, clause 17-4.4.1): a discovery-metadata
     * entry that withdraws a previously published dataset without shipping a file.
     *
     * <p>The clause requires the entry to reproduce the cancelled dataset's metadata - "with
     * all other mandatory metadata fields also set to the same values as the original, with
     * the exception of the issueDate" - so the original entry is supplied whole rather than
     * rebuilt field by field from the current configuration, which may have moved on since
     * the dataset was issued. Take it from the catalogue that published the dataset.</p>
     *
     * <p>An entry whose signature was counter-signed - S-100 Part 15, clause 15-8.8, chains
     * signatures with S100_SE_SignatureOnSignature - reproduces that chained signature too, so
     * the counter-signer's certificate has to travel with this exchange set as well: clause
     * 15-8.7 requires it to hold "all the certificates required to perform a full certificate
     * path validation without any external access". Supply it keyed by the certificateRef the
     * chained signature carries in the original entry; the reproduced entry references the
     * certificate under the id this catalogue gives it. A chained signature whose certificate
     * is not supplied is rejected rather than emitted with a reference nothing resolves.</p>
     *
     * @param original                     the cancelled dataset's discovery metadata,
     *                                     reproduced verbatim apart from the issue date and
     *                                     purpose; copied, never modified
     * @param issueDate                    the issue date of the cancellation itself, the one
     *                                     field the clause excepts from reproduction
     * @param certificatePems              the chain that verifies the reused signature, signing
     *                                     certificate first; empty means the exchange set's
     *                                     current Data Server certificate signed it
     * @param counterSignerCertificatePems the chain of each party that counter-signed the
     *                                     reused signature, signing certificate first, keyed by
     *                                     the certificateRef the original entry's chained
     *                                     signature carries
     */
    public record Cancellation(
            S100DatasetDiscoveryMetadata original,
            LocalDate issueDate,
            List<String> certificatePems,
            Map<String, List<String>> counterSignerCertificatePems) {

        /** A cancellation whose original was signed with the current Data Server certificate. */
        public Cancellation(S100DatasetDiscoveryMetadata original, LocalDate issueDate) {
            this(original, issueDate, List.of(), Map.of());
        }

        /** A cancellation whose reused signature carries no counter-signature. */
        public Cancellation(S100DatasetDiscoveryMetadata original, LocalDate issueDate,
                List<String> certificatePems) {
            this(original, issueDate, certificatePems, Map.of());
        }

        public Cancellation {
            Objects.requireNonNull(original, "cancellation original metadata must be set");
            Objects.requireNonNull(issueDate, "cancellation issueDate must be set");
            certificatePems = certificatePems == null ? List.of() : List.copyOf(certificatePems);
            counterSignerCertificatePems = copyOfChains(counterSignerCertificatePems);
            if (original.getFileName() == null || original.getFileName().isBlank()) {
                throw new IllegalArgumentException("cancellation original must carry the file name "
                        + "of the dataset being cancelled (S-100 Part 17, clause 17-4.4.1)");
            }
            if (original.getDigitalSignatureValues().isEmpty()) {
                throw new IllegalArgumentException("cancellation original must carry the dataset's "
                        + "digital signature (S-100 Part 17 clause 17-4.4.1: a fileless cancellation "
                        + "reuses the original signature)");
            }
        }

        /** An unmodifiable copy of the counter-signer chains, each of which must be usable. */
        private static Map<String, List<String>> copyOfChains(Map<String, List<String>> chains) {
            if (chains == null) {
                return Map.of();
            }
            Map<String, List<String>> copy = new LinkedHashMap<>();
            chains.forEach((certificateRef, pems) -> {
                if (certificateRef == null || certificateRef.isBlank()) {
                    throw new IllegalArgumentException("counter-signer certificate chains must be "
                            + "keyed by the certificateRef the original entry's chained signature "
                            + "carries");
                }
                if (pems == null || pems.isEmpty()) {
                    throw new IllegalArgumentException(String.format(
                            "no certificate supplied for the counter-signer of \"%s\", so the reused "
                                    + "chained signature could not be authenticated (S-100 Part 15, "
                                    + "clause 15-8.7)",
                            certificateRef));
                }
                copy.put(certificateRef, List.copyOf(pems));
            });
            return Collections.unmodifiableMap(copy);
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
        private boolean validateAgainstSchema = true;
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
        /**
         * The MRN the {@code datasetID} of each dataset is built from, as
         * {@code <prefix>:<dataset id>}.
         * <p/>
         * Rejected here rather than at build time, because S-124 clause 12.2.2 requires the
         * resulting {@code datasetID} to be an MRN and a prefix is the only part of it the caller
         * controls; a bad one would otherwise surface as a signed, schema-invalid catalogue.
         * The value is completed and re-checked in {@code datasetId} before it is emitted.
         */
        public Builder datasetMrnPrefix(String prefix) {
            if (prefix == null || prefix.isBlank()) {
                throw new IllegalArgumentException("datasetMrnPrefix must be set; S-124 clause "
                        + "12.2.2 requires each datasetID to be a Marine Resource Name");
            }
            // The prefix is completed by ":<dataset id>", so it needs one namespace specific
            // string fewer than a whole MRN does.
            if (!MRN_PATTERN.matcher(prefix + ":x").matches()) {
                throw new IllegalArgumentException(String.format(
                        "datasetMrnPrefix \"%s\" does not yield a Marine Resource Name; S-124 clause "
                                + "12.2.2 requires each datasetID to be an MRN, so the prefix must "
                                + "read urn:mrn:<organisation>[:<namespace>] (\"%s\")",
                        prefix, DEFAULT_DATASET_MRN_PREFIX));
            }
            this.datasetMrnPrefix = prefix;
            return this;
        }
        public Builder productSpecification(S100ProductSpecification spec) { this.productSpecification = spec; return this; }
        /**
         * Whether each dataset is validated against the S-124 GML application schema before
         * being signed and packaged. On by default: S-124 clause 8.1.1 requires schema validity,
         * and an invalid dataset is far cheaper to catch here than after it has been signed and
         * distributed.
         * <p/>
         * Turn it off only to build an exchange set from datasets that are knowingly invalid,
         * such as a fixture reproducing a consumer bug.
         */
        public Builder validateAgainstSchema(boolean validate) {
            this.validateAgainstSchema = validate;
            return this;
        }
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
        /**
         * Overrides the signature algorithm. S-100 Part 15, clause 15-8.7, admits a single
         * value - "The digitalSignatureReference field must be encoded 'ECDSA-384-SHA2'" -
         * so every other enumeration value is rejected rather than written into a catalogue
         * an OEM would refuse to authenticate.
         */
        public Builder signatureAlgorithm(S100SEDigitalSignatureReference algorithm) {
            if (algorithm != S100SEDigitalSignatureReference.ECDSA_384_SHA_2) {
                throw new IllegalArgumentException("signatureAlgorithm must be ECDSA-384-SHA2 "
                        + "(S-100 Part 15, clause 15-8.7: \"The digitalSignatureReference field "
                        + "must be encoded 'ECDSA-384-SHA2'\")");
            }
            this.signatureAlgorithm = algorithm;
            return this;
        }
        public Builder notForNavigation(boolean v) { this.notForNavigation = v; return this; }
        public Builder classification(SecurityClassification c) { this.classification = c; return this; }
        public Builder producingAgencyRole(RoleCode role) { this.producingAgencyRole = role; return this; }

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
            // The producer code is the fixed width YYYY field of the XXXYYYYØØØØ dataset file
            // name of S-100 Part 17, clause 17-4.3, and the IHO Producer Code Register issues
            // it as a four character code. Any other length or character both misnames every
            // dataset file and leaves a reader unable to tell where the unique code begins.
            if (!producerCode.matches("[A-Za-z0-9]{4}")) {
                throw new IllegalArgumentException(String.format(
                        "producerCode \"%s\" must be four alphanumeric characters: it is the YYYY "
                                + "field of the dataset file names of S-100 Part 17, clause 17-4.3, "
                                + "and the IHO Producer Code Register issues four character codes",
                        producerCode));
            }
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
