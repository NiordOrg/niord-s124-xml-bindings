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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.XMLConstants;
import javax.xml.transform.sax.SAXSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The reader {@link SecureXmlSource} hands out, tested on its own rather than through a caller, so
 * that a regression points at the parser configuration instead of at whichever read path noticed.
 */
class SecureXmlSourceTest {

    private static final String NAMESPACES = "http://xml.org/sax/features/namespaces";
    private static final String DISALLOW_DOCTYPE_DECL = "http://apache.org/xml/features/disallow-doctype-decl";
    private static final String EXTERNAL_GENERAL_ENTITIES = "http://xml.org/sax/features/external-general-entities";
    private static final String EXTERNAL_PARAMETER_ENTITIES = "http://xml.org/sax/features/external-parameter-entities";

    /**
     * A well-formed document parses normally, namespaces intact. The hardening is only worth having
     * if it is invisible to a conformant producer, and every read path in this library depends on
     * namespace-aware parsing to bind an element to a class at all.
     */
    @Test
    void parsesAWellFormedNamespacedDocument() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<c:root xmlns:c=\"http://www.iho.int/s100/xc/5.2\"><c:child>text</c:child></c:root>";

        final Recorder recorded = parse(xml);

        assertEquals(List.of("http://www.iho.int/s100/xc/5.2:root", "http://www.iho.int/s100/xc/5.2:child"),
                recorded.elements);
        assertEquals("text", recorded.text.toString());
    }

    /** A UTF-8 document is decoded as UTF-8 and not as the platform default. */
    @Test
    void parsesNonAsciiContentAsUtf8() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>Rødbyhavn Sønderborg</root>";

        assertEquals("Rødbyhavn Sønderborg", parse(xml).text.toString());
    }

    /**
     * The configuration itself, asserted on the reader that will do the parsing. The four features
     * are read back rather than assumed, which is the same check {@code SecureXmlSource} makes
     * before it hands the reader out and the reason it can fail closed.
     */
    @Test
    void theReaderIsConfiguredToResolveNothing() throws Exception {
        final XMLReader reader = ((SAXSource) SecureXmlSource.of("<root/>".getBytes(StandardCharsets.UTF_8)))
                .getXMLReader();

        assertTrue(reader.getFeature(NAMESPACES), "JAXB binds elements by namespace");
        assertTrue(reader.getFeature(XMLConstants.FEATURE_SECURE_PROCESSING), "secure processing");
        assertTrue(reader.getFeature(DISALLOW_DOCTYPE_DECL), "the load-bearing control");
        assertFalse(reader.getFeature(EXTERNAL_GENERAL_ENTITIES), "external general entities");
        assertFalse(reader.getFeature(EXTERNAL_PARAMETER_ENTITIES), "external parameter entities");
    }

    /**
     * The four hostile declarations, refused at the declaration itself. The external entity ones
     * point at real files, and the parse is asserted to have produced neither of their canaries.
     */
    @Test
    void refusesEveryHostileDoctype(@TempDir Path tempDir) throws IOException {
        final String secretFileUri = XxePayloads.writeSecretFile(tempDir);
        final String secretDtdUri = XxePayloads.writeSecretDtd(tempDir);

        assertRefused(XxePayloads.externalGeneralEntity("root", secretFileUri), "&leak;");
        assertRefused(XxePayloads.externalParameterEntity("root", secretDtdUri), "&smuggled;");
        assertRefused(XxePayloads.billionLaughs("root"), "&lol9;");
        assertRefused(XxePayloads.bareDoctype("root"), "harmless");
    }

    /** Nothing is parsed on a null input, so the misuse surfaces as a misuse, on either overload. */
    @Test
    void rejectsANullDocument() {
        assertThrows(NullPointerException.class, () -> SecureXmlSource.of((byte[]) null));
        assertThrows(NullPointerException.class, () -> SecureXmlSource.of((String) null));
    }

    /**
     * A string has already been decoded, so its own encoding declaration describes bytes that no
     * longer exist and must not decide how the parser reads it. This overload used to re-encode the
     * string as UTF-8 and hand the parser those bytes, which obeyed the declaration below and
     * returned mojibake for every non-ASCII character - silently, and only for the text a Danish
     * navigational warning is most likely to carry.
     */
    @Test
    void ignoresTheEncodingDeclarationOfADocumentGivenAsAString() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><root>R\u00f8dbyhavn S\u00f8nderborg</root>";

        assertEquals("R\u00f8dbyhavn S\u00f8nderborg", parseString(xml).text.toString());
    }

    /**
     * The same for a declaration that no re-encoding could satisfy: the characters handed over as
     * UTF-8 bytes are not a well-formed UTF-16 document at all, so the document did not parse.
     */
    @Test
    void parsesAStringDeclaringAnEncodingItsCharactersAreNotIn() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-16\"?><root>R\u00f8dbyhavn</root>";

        assertEquals("R\u00f8dbyhavn", parseString(xml).text.toString());
    }

    /**
     * The refusal belongs to the reader, not to one overload: a document arriving as a string is
     * hardened exactly as one arriving as bytes.
     */
    @Test
    void refusesAHostileDoctypeGivenAsAString(@TempDir Path tempDir) throws IOException {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + XxePayloads.externalGeneralEntity("root", XxePayloads.writeSecretFile(tempDir))
                + "\n<root>&leak;</root>";

        final SAXParseException refusal = assertThrows(SAXParseException.class, () -> parseString(xml));

        assertTrue(refusal.getMessage().contains(XxePayloads.REFUSAL),
                () -> "refused, but not for its DOCTYPE: " + refusal.getMessage());
        assertFalse(refusal.getMessage().contains(XxePayloads.FILE_CANARY), "the file's content leaked");
    }

    private static void assertRefused(String doctype, String body) {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + doctype + "\n<root>" + body + "</root>";

        final SAXParseException refusal = assertThrows(SAXParseException.class, () -> parse(xml));

        assertTrue(refusal.getMessage().contains(XxePayloads.REFUSAL),
                () -> "refused, but not for its DOCTYPE: " + refusal.getMessage());
        assertFalse(refusal.getMessage().contains(XxePayloads.FILE_CANARY), "the file's content leaked");
        assertFalse(refusal.getMessage().contains(XxePayloads.PARAMETER_CANARY), "the DTD fragment's content leaked");
    }

    /** Parses the document as bytes, the form a caller reading an archive or a stream holds. */
    private static Recorder parse(String xml) throws Exception {
        return parse(SecureXmlSource.of(xml.getBytes(StandardCharsets.UTF_8)));
    }

    /** Parses the document as characters, the form a caller holding a decoded string holds. */
    private static Recorder parseString(String xml) throws Exception {
        return parse(SecureXmlSource.of(xml));
    }

    private static Recorder parse(SAXSource source) throws Exception {
        final Recorder recorder = new Recorder();
        source.getXMLReader().setContentHandler(recorder);
        // Rethrow rather than let Xerces' default handler print the refusal to stderr: the test
        // asserts on the exception, and a passing build should not look like a failing one.
        source.getXMLReader().setErrorHandler(recorder);
        source.getXMLReader().parse(source.getInputSource());
        return recorder;
    }

    /** Collects what the parse saw, so a test can assert on the document rather than on a mock. */
    private static final class Recorder extends DefaultHandler {
        private final List<String> elements = new ArrayList<>();
        private final StringBuilder text = new StringBuilder();

        @Override
        public void error(SAXParseException e) throws SAXException {
            throw e;
        }

        @Override
        public void fatalError(SAXParseException e) throws SAXException {
            throw e;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            this.elements.add(uri + ":" + localName);
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            this.text.append(ch, start, length);
        }
    }

}
