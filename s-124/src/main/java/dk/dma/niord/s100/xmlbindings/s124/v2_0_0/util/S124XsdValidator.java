package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util;

import java.io.IOException;
import java.io.StringReader;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.xml.sax.SAXException;

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
     *
     * @param xml the marshalled dataset
     * @throws SAXException if the document is not schema-valid
     */
    public static void validate(String xml) throws SAXException, IOException {
        validate(DatasetSchema.INSTANCE, xml);
    }

    private static void validate(Schema schema, String xml) throws SAXException, IOException {
        Validator validator = schema.newValidator();
        validator.validate(new StreamSource(new StringReader(xml)));
    }

    /** Compiled on first use, so merely loading this class costs nothing. */
    private static final class DatasetSchema {
        static final Schema INSTANCE = ClasspathSchemas.compile(DATASET_SCHEMA_RESOURCE_PATH);
    }

}
