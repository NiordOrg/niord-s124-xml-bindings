package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;

import org.grad.eNav.s100.adapters.DateAdapter;
import org.grad.eNav.s100.adapters.OffsetDateTimeAdapter;
import org.grad.eNav.s100.utils.SecureXmlSource;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.Dataset;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.impl.DatasetImpl;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.JAXBIntrospector;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;

/**
 * S-124 v2.0.0 JAXB marshal / unmarshal helpers.
 */
public final class S124Utils {

    private static final String SCHEMA_LOCATION =
            "http://www.iho.int/S124/gml/2.0 "
                    + "https://schemas.s100dev.net/schemas/S124/2.0.0/20250729/124_2.0.0.xsd";

    private static final JAXBContext JAXB_CONTEXT;
    static {
        try {
            JAXB_CONTEXT = JAXBContext.newInstance(DatasetImpl.class);
        } catch (JAXBException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private S124Utils() {
    }

    public static String marshalS124(Dataset dataset) throws JAXBException {
        return marshalS124(dataset, true);
    }

    public static String marshalS124(Dataset dataset, boolean format) throws JAXBException {
        return marshalS124(dataset, format, true);
    }

    /**
     * Marshals the dataset to XML, normalising and checking it first.
     * <p/>
     * This method is the one point every dataset of this library passes through on its way to XML,
     * so it is where the rules the GML application schema cannot express are applied. Two things
     * happen before the marshal:
     * <ol>
     *   <li>{@link S124CodedValues#fillMissingCodes} fills in the numeric code of any enumeration
     *       or closed code list element that carries only its label, which S-100 Part 10b, clause
     *       10b-8.2.4, requires and the schema leaves optional. The code is a pure function of the
     *       label, so this is a completion of the producer's intent rather than a change to it.</li>
     *   <li>{@link S124DatasetValidator#validate} rejects the dataset if it breaks a "must" of
     *       S-124 or S-100 that no schema can catch - not exactly one {@code NavwarnPreamble}, an
     *       agency name where an S-62 producer code belongs, a time with no UTC designator, an
     *       association carrying neither {@code xlink:role} nor {@code xlink:arcrole}, an
     *       {@code applicationProfile} outside Table 10b-4 or disagreeing with the dataset purpose,
     *       or a code that contradicts its own label.</li>
     * </ol>
     * Both steps mutate and inspect the dataset in place; neither invents a value the producer did
     * not already imply.
     *
     * @param dataset  the dataset to marshal
     * @param format   whether to indent the output
     * @param validate whether to normalise and conformance-check the dataset first. Pass
     *                 {@code false} only to serialise a dataset that is knowingly non-conformant -
     *                 to write it to disk for inspection, say, or in a test - because the resulting
     *                 XML is not a valid S-124 dataset.
     * @return the marshalled XML
     * @throws S124ConformanceException if {@code validate} is set and the dataset breaks a rule
     */
    public static String marshalS124(Dataset dataset, boolean format, boolean validate) throws JAXBException {
        requireNonNull(dataset, "dataset is null");
        if (validate) {
            S124CodedValues.fillMissingCodes(dataset);
            S124DatasetValidator.validate(dataset);
        }

        Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, format);
        marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, SCHEMA_LOCATION);
        // The bundled adapters emit yyyyMMdd / yyyyMMdd'T'HHmmssZ which are not valid
        // xs:date / xs:dateTime; override with ISO-conformant ones.
        marshaller.setAdapter(DateAdapter.class, new IsoDateAdapter());
        marshaller.setAdapter(OffsetDateTimeAdapter.class, new IsoOffsetDateTimeAdapter());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        marshaller.marshal(dataset, out);
        return out.toString(StandardCharsets.UTF_8);
    }

    /**
     * Reads an S-124 dataset back from its XML form.
     * <p/>
     * This is the library's public read path for a dataset GML, and the dataset it is handed is
     * routinely a foreign one - the whole point of S-124 is that warnings travel between
     * producers and consumers - so the document is parsed through {@link SecureXmlSource}, which
     * refuses a document type declaration and dereferences no external entity. A hostile dataset
     * would otherwise read a file off the parsing host into a warning's text, or make that host
     * issue a request of the attacker's choosing. An S-124 dataset is an XML Schema instance
     * document (S-124 Ed 2.0.0, clause 8.1.1) and never legitimately carries a DOCTYPE, so
     * nothing conformant is refused.
     * <p/>
     * No schema or conformance checking happens here: use {@link S124XsdValidator#validate} and
     * {@link S124DatasetValidator#validate} on a dataset whose conformance matters.
     *
     * @param xml the marshalled dataset
     * @return the unmarshalled dataset
     * @throws JAXBException if the document cannot be read, including when it declares a DOCTYPE
     *                       or an external entity
     */
    public static Dataset unmarshallS124(String xml) throws JAXBException {
        requireNonNull(xml, "xml is null");
        Unmarshaller unmarshaller = JAXB_CONTEXT.createUnmarshaller();
        SAXSource source = SecureXmlSource.of(xml.getBytes(StandardCharsets.UTF_8));
        Object value = JAXBIntrospector.getValue(unmarshaller.unmarshal(source));
        return (Dataset) value;
    }

    /**
     * Replaces {@link DateAdapter} with XSD-conformant ISO date formatting.
     * The shipped adapter uses {@code yyyyMMdd}, which is not a valid {@code xs:date}.
     */
    private static final class IsoDateAdapter extends DateAdapter {
        @Override
        public String marshal(LocalDate value) {
            return value == null ? null : value.format(DateTimeFormatter.ISO_LOCAL_DATE);
        }

        @Override
        public LocalDate unmarshal(String value) {
            return value == null || value.isBlank() ? null : LocalDate.parse(value);
        }
    }

    /**
     * Replaces {@link OffsetDateTimeAdapter} with XSD-conformant ISO date-time formatting.
     */
    private static final class IsoOffsetDateTimeAdapter extends OffsetDateTimeAdapter {
        /**
         * Emits the instant in UTC, whatever offset the caller expressed it in.
         * <p/>
         * S-124 clause 6.2.2: "All instances of time in datasets conforming to S-124 must be
         * expressed in UTC", restated by clause 4.3.3 for every time "either in text or in
         * attributes". A producer working in local time therefore has its {@code publicationTime}
         * and {@code cancellationDate} converted rather than rejected: the offset is enough to
         * recover the instant unambiguously, so there is nothing for the producer to decide and
         * nothing lost by normalising. Times that carry no offset at all - {@code xs:time}
         * attributes such as {@code timeOfDayStart} - cannot be converted, and those
         * {@code S124DatasetValidator} rejects.
         */
        @Override
        public String marshal(OffsetDateTime value) {
            return value == null ? null : value.withOffsetSameInstant(ZoneOffset.UTC)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }

        @Override
        public OffsetDateTime unmarshal(String value) {
            return value == null || value.isBlank() ? null : OffsetDateTime.parse(value);
        }
    }

    /**
     * Re-indents an XML document, for logging it or putting it in front of a person.
     * <p/>
     * The document is parsed through {@link SecureXmlSource}, exactly as {@link #unmarshallS124}
     * is, because the reason to pretty-print a dataset on this class is normally that it just
     * arrived from another producer. An identity transform reading a raw {@code StreamSource}
     * parses with a default JAXP parser, which honours a document type declaration: an
     * {@code <!ENTITY x SYSTEM "file:///...">} referenced from the body is resolved and copied
     * into the string this method returns, so an unhardened pretty-printer hands the caller the
     * content of a file on the parsing host, and the {@code http://} form of the same declaration
     * makes that host issue a request of the attacker's choosing. Refusing the DOCTYPE costs
     * nothing here: an S-124 dataset is an XML Schema instance document (S-124 Ed 2.0.0, clause
     * 8.1.1) and never legitimately carries one.
     *
     * @param input the XML document to re-indent
     * @return the same document, indented by four spaces
     * @throws RuntimeException if the document cannot be parsed or written, including when it
     *                          declares a DOCTYPE or an external entity
     */
    public static String prettyPrint(String input) {
        requireNonNull(input, "input is null");
        try {
            SAXSource xmlInput = SecureXmlSource.of(input.getBytes(StandardCharsets.UTF_8));
            // Xerces' default handler writes a fatal parse error to stderr before the parse
            // unwinds. The refusal is already reported to the caller as the exception below, and a
            // library has no business printing a caller's document problem to the console.
            xmlInput.getXMLReader().setErrorHandler(RETHROW_PARSE_FAILURES);
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            Transformer transformer = secureTransformerFactory().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.transform(xmlInput, xmlOutput);
            return stringWriter.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Reports a parse failure by throwing it, rather than by printing it and throwing it. */
    private static final ErrorHandler RETHROW_PARSE_FAILURES = new ErrorHandler() {
        @Override
        public void warning(SAXParseException e) {
            // A warning is not a failure and there is nothing here to report it to.
        }

        @Override
        public void error(SAXParseException e) throws SAXException {
            throw e;
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            throw e;
        }
    };

    /**
     * A transformer factory that dereferences nothing, built per call because a
     * {@code TransformerFactory} is not documented as safe to share between threads.
     * <p/>
     * The two access properties are defence in depth rather than the control that stops an attack:
     * the source handed to the transform is a {@link SecureXmlSource}, which carries its own reader
     * and refuses the DOCTYPE before either property could apply. They matter if that ever changes
     * - a raw {@code StreamSource} would be parsed by the factory's own parser instead. Empty
     * string means "no protocol is allowed", per the JAXP javadoc for these properties.
     *
     * @throws TransformerConfigurationException if the platform's factory cannot be told to refuse
     *                                           external references, in which case nothing is
     *                                           transformed
     */
    private static TransformerFactory secureTransformerFactory() throws TransformerConfigurationException {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        return factory;
    }
}
