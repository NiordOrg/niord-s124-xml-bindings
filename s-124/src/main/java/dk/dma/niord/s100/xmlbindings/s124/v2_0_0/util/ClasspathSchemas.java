package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXException;

/**
 * Compiles the XML schemas bundled in this library from the classpath.
 * <p/>
 * The schemas cross-reference each other by relative path and by the {@code schemas.s100dev.net}
 * URLs they were published under, neither of which a classloader can resolve on its own, so every
 * schema is compiled with a resolver that maps both forms back onto the {@code /xsd/} classpath
 * root. Compiling from the classpath rather than from a file keeps validation working when the
 * library is used as a jar, which is the only way it is ever used.
 */
final class ClasspathSchemas {

    /** The classpath root the s-100 and s-124 modules both publish their schemas under. */
    private static final String SCHEMA_BASE_PATH = "/xsd/";

    private ClasspathSchemas() {
    }

    /**
     * Compiles the schema at the given absolute classpath resource path.
     *
     * @param resourcePath the schema resource, e.g. {@code /xsd/124_2.0.0.xsd}
     * @return the compiled, thread-safe schema
     */
    static Schema compile(String resourcePath) {
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setResourceResolver(new ClasspathResourceResolver(resourcePath));
            URL schemaUrl = ClasspathSchemas.class.getResource(resourcePath);
            if (schemaUrl == null) {
                throw new IOException("Schema not found on classpath: " + resourcePath);
            }
            return factory.newSchema(schemaUrl);
        } catch (SAXException | IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static class ClasspathResourceResolver implements LSResourceResolver {

        private final String schemaResourcePath;

        ClasspathResourceResolver(String schemaResourcePath) {
            this.schemaResourcePath = schemaResourcePath;
        }

        @Override
        public LSInput resolveResource(String type, String namespaceURI, String publicId,
                String systemId, String baseURI) {
            if (systemId == null) return null;

            String resourcePath = resolveResourcePath(systemId, baseURI);
            InputStream is = ClasspathSchemas.class.getResourceAsStream(resourcePath);

            if (is == null) {
                resourcePath = SCHEMA_BASE_PATH + systemId;
                is = ClasspathSchemas.class.getResourceAsStream(resourcePath);
            }

            if (is == null) return null;
            // The resolved classpath path is handed back as the system id, not the relative one
            // that was asked for. Schemas that are themselves reached by a relative path - the
            // ISO 19115-3 tree the exchange catalogue imports is several levels deep - are the
            // base URI for their own imports, so returning the relative id would leave the next
            // resolution with nothing to resolve against.
            return new LSInputImpl(publicId, resourcePath, is);
        }

        private String resolveResourcePath(String systemId, String baseURI) {
            if (systemId.startsWith("http://") || systemId.startsWith("https://")) {
                String fileName = systemId.substring(systemId.lastIndexOf('/') + 1);
                return SCHEMA_BASE_PATH + fileName;
            }

            if (baseURI != null) {
                String basePath = baseURI;
                if (basePath.startsWith("file:") || basePath.startsWith("jar:")) {
                    int schemasIdx = basePath.indexOf(SCHEMA_BASE_PATH);
                    if (schemasIdx >= 0) basePath = basePath.substring(schemasIdx);
                }
                int lastSlash = basePath.lastIndexOf('/');
                if (lastSlash >= 0) {
                    return normalizePath(basePath.substring(0, lastSlash + 1) + systemId);
                }
            }

            int lastSlash = schemaResourcePath.lastIndexOf('/');
            return normalizePath(schemaResourcePath.substring(0, lastSlash + 1) + systemId);
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
