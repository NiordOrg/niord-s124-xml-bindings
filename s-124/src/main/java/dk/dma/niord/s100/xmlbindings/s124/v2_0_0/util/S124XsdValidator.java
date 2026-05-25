package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;

/**
 * XSD validation utility for S-124 v2.0.0 GML documents. Schemas are loaded from the
 * {@code /xsd/} classpath root (the layout used by the niord-xml-bindings jar).
 */
public final class S124XsdValidator {

    private static final String SCHEMA_RESOURCE_PATH = "/xsd/124_2.0.0.xsd";
    private static final String SCHEMA_BASE_PATH = "/xsd/";

    private static final Schema SCHEMA;
    static {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setResourceResolver(new ClasspathResourceResolver());
            URL schemaUrl = S124XsdValidator.class.getResource(SCHEMA_RESOURCE_PATH);
            if (schemaUrl == null) {
                throw new IOException("Schema not found on classpath: " + SCHEMA_RESOURCE_PATH);
            }
            SCHEMA = factory.newSchema(schemaUrl);
        } catch (SAXException | IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private S124XsdValidator() {}

    public static void validate(String xml) throws SAXException, IOException {
        Validator validator = SCHEMA.newValidator();
        validator.validate(new StreamSource(new StringReader(xml)));
    }

    private static class ClasspathResourceResolver implements LSResourceResolver {

        @Override
        public LSInput resolveResource(String type, String namespaceURI, String publicId,
                String systemId, String baseURI) {
            if (systemId == null) return null;

            String resourcePath = resolveResourcePath(systemId, baseURI);
            InputStream is = S124XsdValidator.class.getResourceAsStream(resourcePath);

            if (is == null) {
                resourcePath = SCHEMA_BASE_PATH + systemId;
                is = S124XsdValidator.class.getResourceAsStream(resourcePath);
            }

            if (is == null) return null;
            return new LSInputImpl(publicId, systemId, is);
        }

        private static String resolveResourcePath(String systemId, String baseURI) {
            if (systemId.startsWith("http://") || systemId.startsWith("https://")) {
                String fileName = systemId.substring(systemId.lastIndexOf('/') + 1);
                return SCHEMA_BASE_PATH + fileName;
            }

            if (baseURI != null) {
                String basePath = baseURI;
                if (basePath.startsWith("file:")) {
                    int schemasIdx = basePath.indexOf(SCHEMA_BASE_PATH);
                    if (schemasIdx >= 0) basePath = basePath.substring(schemasIdx);
                }
                int lastSlash = basePath.lastIndexOf('/');
                if (lastSlash >= 0) {
                    return normalizePath(basePath.substring(0, lastSlash + 1) + systemId);
                }
            }

            int lastSlash = SCHEMA_RESOURCE_PATH.lastIndexOf('/');
            return SCHEMA_RESOURCE_PATH.substring(0, lastSlash + 1) + systemId;
        }

        private static String normalizePath(String path) {
            String p = path;
            while (p.contains("/../")) {
                int dotdot = p.indexOf("/../");
                int prevSlash = p.lastIndexOf('/', dotdot - 1);
                if (prevSlash >= 0) {
                    p = p.substring(0, prevSlash) + p.substring(dotdot + 3);
                } else {
                    break;
                }
            }
            return p.replace("/./", "/");
        }
    }

    private static class LSInputImpl implements LSInput {
        private final String publicId;
        private final String systemId;
        private final InputStream inputStream;

        LSInputImpl(String publicId, String systemId, InputStream inputStream) {
            this.publicId = publicId;
            this.systemId = systemId;
            this.inputStream = inputStream;
        }

        @Override public java.io.Reader getCharacterStream() { return null; }
        @Override public void setCharacterStream(java.io.Reader cs) {}
        @Override public InputStream getByteStream() { return inputStream; }
        @Override public void setByteStream(InputStream bs) {}
        @Override public String getStringData() { return null; }
        @Override public void setStringData(String sd) {}
        @Override public String getSystemId() { return systemId; }
        @Override public void setSystemId(String sid) {}
        @Override public String getPublicId() { return publicId; }
        @Override public void setPublicId(String pid) {}
        @Override public String getBaseURI() { return null; }
        @Override public void setBaseURI(String buri) {}
        @Override public String getEncoding() { return "UTF-8"; }
        @Override public void setEncoding(String enc) {}
        @Override public boolean getCertifiedText() { return false; }
        @Override public void setCertifiedText(boolean ct) {}
    }
}
