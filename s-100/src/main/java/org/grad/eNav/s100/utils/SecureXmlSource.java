package org.grad.eNav.s100.utils;

import jakarta.xml.bind.JAXBException;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.Objects;

/**
 * Builds the XML source this library parses foreign documents through.
 * <p/>
 * The rule, rather than a list of the call sites that follow it today: every path in this library
 * that parses XML it did not itself produce goes through this class, without exemption. The legacy
 * {@code dk.baleen.s100.xmlbindings.s124.v1_0_0} utilities were the one place the rule did not
 * hold; they were deleted rather than hardened, since nothing consumed them.
 * <p/>
 * A JAXB {@code Unmarshaller} handed a raw {@code InputStream}, and a JAXP {@code Validator}
 * handed a raw {@code StreamSource}, parse with a default JAXP parser: document type declarations
 * are honoured and external general and parameter entities are dereferenced. Any caller who feeds
 * such a parser a document it did not produce itself is exposed to the three classic attacks on
 * XML entity handling:
 * <ul>
 *   <li><b>XXE file disclosure.</b> {@code <!ENTITY x SYSTEM "file:///etc/passwd">} referenced from
 *       a text node returns the file's content in a parsed field, from where it travels wherever
 *       that field travels - a log line, a database column, an exchange catalogue built from the
 *       parsed entry.</li>
 *   <li><b>SSRF and network probing.</b> The same declaration with an {@code http://} or
 *       {@code ftp://} system identifier makes the parsing host issue the request, from inside
 *       whatever network the reader runs in. A maritime service reading a SECOM peer's exchange
 *       set is exactly the position an attacker wants that request issued from.</li>
 *   <li><b>Entity expansion denial of service</b> ("billion laughs"): nested internal entities
 *       whose expansion is exponential in the size of the document.</li>
 * </ul>
 * The reader returned here refuses a document type declaration outright, which is what makes the
 * defence total rather than a list of holes plugged one at a time: no DOCTYPE means no entity
 * declarations at all, so there is nothing to dereference and nothing to expand, and the file
 * disclosure, the SSRF and the billion-laughs vectors close together. The external-entity features
 * and the no-op {@link EntityResolver} are set anyway, so that a parser that somehow honoured a
 * DOCTYPE would still resolve nothing.
 * <p/>
 * Refusing a DOCTYPE costs nothing for the document types this library reads. An S-100 exchange
 * catalogue (S-100 Part 17, clause 17-4.2), a single S-100 dataset discovery metadata entry and an
 * S-124 dataset are XML Schema instance documents: their structure, their defaults and their
 * content model come from the XSD, and a schema-valid instance of any of them never carries a
 * document type declaration. A DOCTYPE in one of these documents is therefore not a legitimate
 * producer's encoding choice that this hardening happens to break - it is either a mistake or an
 * attack, and rejecting it loses no conformant input.
 * <p/>
 * <b>Fail closed.</b> If the platform's parser cannot be told to refuse DOCTYPEs, this class
 * throws instead of returning a parser that would go on to read the document anyway. JAXP makes
 * {@code setFeature} throw for a feature that is unrecognised or unsupported, and the reader that
 * will actually do the parsing is asked once afterwards whether the refusal is in force - so an
 * implementation that accepted the setting without applying it is caught too. A parser that cannot
 * be instructed to refuse a DOCTYPE must not be handed a peer's document, so the failure surfaces
 * as the {@link JAXBException} the read paths already declare rather than being degraded into a
 * permissive parse.
 * <p/>
 * What this does <b>not</b> address: the size of the document itself. The reader is streamed a
 * byte array the caller already holds in memory, so a caller that decompressed those bytes from an
 * archive has already paid whatever that decompression cost - see
 * {@code S124ExchangeSetFactory.readDiscoveryMetadata}, which is explicit about it.
 */
public final class SecureXmlSource {

    /**
     * Xerces feature that rejects a document with a {@code <!DOCTYPE>} declaration outright. The
     * load-bearing control here: no DOCTYPE, no entity declarations, so neither an external entity
     * nor a recursive internal one can be declared in the first place.
     */
    private static final String DISALLOW_DOCTYPE_DECL =
            "http://apache.org/xml/features/disallow-doctype-decl";

    /** SAX feature governing the dereferencing of external general entities ({@code &x;}). */
    private static final String EXTERNAL_GENERAL_ENTITIES =
            "http://xml.org/sax/features/external-general-entities";

    /** SAX feature governing the dereferencing of external parameter entities ({@code %x;}). */
    private static final String EXTERNAL_PARAMETER_ENTITIES =
            "http://xml.org/sax/features/external-parameter-entities";

    /**
     * Resolves every external reference to an empty document rather than fetching it. Unreachable
     * while {@link #DISALLOW_DOCTYPE_DECL} holds - a document with no DOCTYPE declares no entity
     * to resolve - and installed precisely for the case where that assumption is wrong.
     */
    private static final EntityResolver NO_EXTERNAL_ENTITIES =
            (publicId, systemId) -> new InputSource(new StringReader(""));

    private SecureXmlSource() {
    }

    /**
     * Wraps the given XML bytes in a source backed by a parser that refuses DOCTYPEs and resolves
     * nothing off the document.
     *
     * @param xml the XML document bytes, in the encoding its own declaration names
     * @return a source that parses those bytes with entity resolution shut off
     * @throws JAXBException if the platform's parser cannot be configured to refuse DOCTYPEs and
     *                       external entities, in which case nothing is parsed
     */
    public static SAXSource of(byte[] xml) throws JAXBException {
        Objects.requireNonNull(xml, "xml is null");
        // Bytes are undecoded, so the encoding declaration they carry is the parser's to honour.
        return new SAXSource(secureReader(), new InputSource(new ByteArrayInputStream(xml)));
    }

    /**
     * Wraps an XML document already decoded to a string, the form every read path in this library
     * holds one in.
     * <p/>
     * The string is handed to the parser as characters rather than re-encoded to bytes. A string
     * has already been decoded, so the encoding pseudo-attribute of its own declaration describes
     * bytes that no longer exist, and a parser given those characters back as UTF-8 bytes would
     * obey the declaration anyway: a document declaring {@code encoding="ISO-8859-1"} would be
     * decoded as Latin-1 and every non-ASCII character in it - every Danish place name in a
     * navigational warning - would come back as mojibake, and one declaring a UTF-16 encoding
     * would not parse at all. {@link InputSource} is explicit that a parser reads an available
     * character stream directly and disregards any encoding declaration found in it, which is the
     * guarantee this overload needs: the caller decoded the document, and that decoding stands.
     *
     * @param xml the XML document
     * @return a source that parses it with entity resolution shut off
     * @throws JAXBException if the platform's parser cannot be configured to refuse DOCTYPEs and
     *                       external entities, in which case nothing is parsed
     */
    public static SAXSource of(String xml) throws JAXBException {
        Objects.requireNonNull(xml, "xml is null");
        return new SAXSource(secureReader(), new InputSource(new StringReader(xml)));
    }

    /**
     * The reader both overloads parse with.
     * <p/>
     * It is namespace aware, which JAXB requires and which every document this library reads
     * depends on, and is not XInclude aware, XInclude being a second way to pull a file or a URL
     * into a document. Secure processing is enabled, which caps the JDK's entity expansion and
     * name limits as a further backstop behind the DOCTYPE refusal.
     * <p/>
     * A fresh reader is built per call: a SAX {@code XMLReader} is stateful and not safe to share
     * between concurrent parses.
     */
    private static XMLReader secureReader() throws JAXBException {
        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);
            // JAXP requires setFeature to throw for a feature it does not recognise or cannot
            // support, so an unhardenable parser lands in the catch below rather than here.
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature(DISALLOW_DOCTYPE_DECL, true);
            factory.setFeature(EXTERNAL_GENERAL_ENTITIES, false);
            factory.setFeature(EXTERNAL_PARAMETER_ENTITIES, false);

            final XMLReader reader = factory.newSAXParser().getXMLReader();
            reader.setEntityResolver(NO_EXTERNAL_ENTITIES);
            // Asked of the reader, not of the factory: a factory reports back whatever it was
            // told, so only the object that will do the parsing can say whether the refusal is
            // really in force.
            if (!reader.getFeature(DISALLOW_DOCTYPE_DECL)) {
                throw new SAXException(String.format(
                        "The parser accepted but did not apply %s", DISALLOW_DOCTYPE_DECL));
            }

            return reader;
        } catch (ParserConfigurationException | SAXException e) {
            // Fail closed: report the misconfiguration as the checked exception the read paths
            // already declare, rather than falling back to a parser that would honour a DOCTYPE.
            throw new JAXBException(
                    "Refusing to parse XML: this platform's SAX parser cannot be configured to "
                            + "reject document type declarations and external entities", e);
        }
    }

}
