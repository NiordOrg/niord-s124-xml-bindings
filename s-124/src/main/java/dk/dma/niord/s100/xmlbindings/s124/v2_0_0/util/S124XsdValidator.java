package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util;

import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.grad.eNav.s100.utils.SecureXmlSource;
import org.xml.sax.SAXException;

import jakarta.xml.bind.JAXBException;

/**
 * XSD validation utility for S-124 v2.0.0 GML documents. The schema is loaded from the
 * {@code /xsd/} classpath root (the layout used by the niord-xml-bindings jars), so validation
 * needs no network access.
 * <p/>
 * Exchange catalogues are not covered. The S-100 Part 17 catalogue schema imports the ISO 19115-3
 * schemas, which this library does not vendor, so compiling it would depend on reaching
 * schemas.isotc211.org - a dependency an exchange set build must not have. The catalogue is checked
 * against that schema in this project's own test suite instead.
 * <p/>
 * Schema validity is one half of conformance. S-124 Ed 2.0.0, clause 8.1.1: "Feature instances must
 * validate against the schema and conform to all other requirements specified in this data product
 * specification including all constraints not captured in the XML Schema document." The constraints
 * of the second half are checked by {@link S124DatasetValidator}.
 */
public final class S124XsdValidator {

    private static final String DATASET_SCHEMA_RESOURCE_PATH = "/xsd/124_2.0.0.xsd";

    private S124XsdValidator() {}

    /**
     * Validates an S-124 dataset document against the S-124 v2.0.0 GML application schema.
     * <p/>
     * The document being checked is by definition one whose conformance is not yet established,
     * which usually means a foreign one, so it is parsed through {@link SecureXmlSource} rather
     * than handed to the validator as a raw stream - otherwise a hostile dataset would read a file
     * off this host, or issue a request from it, before the validator ever reported whether the
     * document was schema-valid. A document that declares a DOCTYPE fails here as a
     * {@link SAXException} whose message names the declaration, distinct from the {@code cvc-}
     * messages a schema failure carries.
     *
     * @param xml the marshalled dataset
     * @throws SAXException if the document is not schema-valid, or declares a DOCTYPE or an
     *                      external entity
     */
    public static void validate(String xml) throws SAXException, IOException {
        validate(DatasetSchema.INSTANCE, xml);
    }

    private static void validate(Schema schema, String xml) throws SAXException, IOException {
        final Validator validator = schema.newValidator();
        // Defence in depth, not the control that closes an attack today: DatasetSchema.INSTANCE is
        // a fully composed, precompiled Schema over a read-only grammar pool, so Xerces never
        // consults an xsi:schemaLocation or xsi:noNamespaceSchemaLocation hint in the instance
        // document - a hint naming the attacker's URL is simply ignored, with or without these two
        // lines. They are set because that is a property of how the schema is supplied here, not of
        // the API: a validator built from a schema composed at validation time would follow the
        // hint. The control that actually stops entity resolution on this path is the reader
        // SecureXmlSource carries below; the validator's properties do not reach a caller-supplied
        // reader. Empty string means "no protocol is allowed", per the JAXP javadoc.
        validator.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        validator.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        try {
            validator.validate(SecureXmlSource.of(xml));
        } catch (JAXBException e) {
            // SecureXmlSource reports a parser it cannot harden as the JAXBException its JAXB
            // callers declare; this caller declares SAXException, and a parser that cannot be
            // told to refuse a DOCTYPE is a validation failure here just the same.
            throw new SAXException(e.getMessage(), e);
        }
    }

    /** Compiled on first use, so merely loading this class costs nothing. */
    private static final class DatasetSchema {
        static final Schema INSTANCE = ClasspathSchemas.compile(DATASET_SCHEMA_RESOURCE_PATH);
    }

}
