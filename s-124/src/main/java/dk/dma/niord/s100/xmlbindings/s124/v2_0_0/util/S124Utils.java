package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util;

import static java.util.Objects.requireNonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.grad.eNav.s100.adapters.DateAdapter;
import org.grad.eNav.s100.adapters.OffsetDateTimeAdapter;

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

    private static final TransformerFactory TRANSFORMER_FACTORY = TransformerFactory.newInstance();

    private S124Utils() {
    }

    public static String marshalS124(Dataset dataset) throws JAXBException {
        return marshalS124(dataset, true);
    }

    public static String marshalS124(Dataset dataset, boolean format) throws JAXBException {
        requireNonNull(dataset, "dataset is null");

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

    public static Dataset unmarshallS124(String xml) throws JAXBException {
        requireNonNull(xml, "xml is null");
        Unmarshaller unmarshaller = JAXB_CONTEXT.createUnmarshaller();
        ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
        Object value = JAXBIntrospector.getValue(unmarshaller.unmarshal(in));
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
        @Override
        public String marshal(OffsetDateTime value) {
            return value == null ? null : value.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        }

        @Override
        public OffsetDateTime unmarshal(String value) {
            return value == null || value.isBlank() ? null : OffsetDateTime.parse(value);
        }
    }

    public static String prettyPrint(String input) {
        try {
            Source xmlInput = new StreamSource(new StringReader(input));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            Transformer transformer = TRANSFORMER_FACTORY.newTransformer();
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
}
