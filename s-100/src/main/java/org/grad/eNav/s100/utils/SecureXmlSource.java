/*
 * Copyright (c) 2024 GLA Research and Development Directorate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
 * Builds the XML source the read paths of this library parse foreign documents through: the two
 * {@code S100ExchangeSetUtils} unmarshals of a catalogue and of a dataset discovery metadata entry
 * (and so {@code S124ExchangeSetFactory.readDiscoveryMetadata} behind them), and the S-124 v2.0.0
 * {@code S124Utils.unmarshallS124}, {@code S124Utils.prettyPrint} and
 * {@code S124XsdValidator.validate}. The legacy {@code dk.baleen.s100.xmlbindings.s124.v1_0_0}
 * package is not among them and parses with a default JAXP parser.
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
 * <b>Fail closed.</b> If the platform's parser cannot be told to refuse DOCTYPEs - the feature is
 * unrecognised, unsupported, or silently not applied - this class throws instead of returning a
 * parser that would go on to read the document anyway. A parser that cannot be instructed to
 * refuse a DOCTYPE must not be handed a peer's document, so the failure is surfaced as the
 * {@link JAXBException} the read paths already declare rather than degraded into a permissive
 * parse. Every feature is read back after it is set, because a factory is permitted to accept
 * {@code setFeature} and then not honour it.
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
     * <p/>
     * The reader is namespace aware, which JAXB requires and which every document this library
     * reads depends on, and is not XInclude aware, XInclude being a second way to pull a file or a
     * URL into a document. Secure processing is enabled, which caps the JDK's entity expansion and
     * name limits as a further backstop behind the DOCTYPE refusal.
     * <p/>
     * A fresh reader is built per call: a SAX {@code XMLReader} is stateful and not safe to share
     * between concurrent parses.
     *
     * @param xml the XML document bytes, in the encoding its own declaration names
     * @return a source that parses those bytes with entity resolution shut off
     * @throws JAXBException if the platform's parser cannot be configured to refuse DOCTYPEs and
     *                       external entities, in which case nothing is parsed
     */
    public static SAXSource of(byte[] xml) throws JAXBException {
        Objects.requireNonNull(xml, "xml is null");
        try {
            final SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setXIncludeAware(false);
            setFeatureOrFail(factory, XMLConstants.FEATURE_SECURE_PROCESSING, true);
            setFeatureOrFail(factory, DISALLOW_DOCTYPE_DECL, true);
            setFeatureOrFail(factory, EXTERNAL_GENERAL_ENTITIES, false);
            setFeatureOrFail(factory, EXTERNAL_PARAMETER_ENTITIES, false);

            final XMLReader reader = factory.newSAXParser().getXMLReader();
            reader.setEntityResolver(NO_EXTERNAL_ENTITIES);

            return new SAXSource(reader, new InputSource(new ByteArrayInputStream(xml)));
        } catch (ParserConfigurationException | SAXException e) {
            // Fail closed: report the misconfiguration as the checked exception the read paths
            // already declare, rather than falling back to a parser that would honour a DOCTYPE.
            throw new JAXBException(
                    "Refusing to parse XML: this platform's SAX parser cannot be configured to "
                            + "reject document type declarations and external entities", e);
        }
    }

    /**
     * Sets a parser feature and reads it back, because {@code setFeature} succeeding is not by
     * itself evidence that the feature took effect.
     *
     * @throws SAXException if the feature is unrecognised, unsupported, or did not take the value
     *                      asked for
     */
    private static void setFeatureOrFail(SAXParserFactory factory, String feature, boolean value)
            throws SAXException, ParserConfigurationException {
        factory.setFeature(feature, value);
        if (factory.getFeature(feature) != value) {
            throw new SAXException(String.format(
                    "The SAX parser factory accepted but did not apply the feature %s=%s", feature, value));
        }
    }

}
