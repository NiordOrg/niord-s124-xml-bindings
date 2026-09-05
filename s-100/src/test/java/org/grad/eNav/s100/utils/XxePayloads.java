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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * The hostile document type declarations the read paths of this library must refuse, and the
 * machinery for proving they refused them without having read anything first.
 * <p/>
 * Every payload here is an internal subset to be spliced into an otherwise well-formed document of
 * the type under test, so that a rejection can only be attributed to the DOCTYPE: the body around
 * it is the same body the control case of each test parses successfully.
 * <p/>
 * The two entity payloads point at real files on disk holding a known canary string, which is what
 * makes a passing test meaningful. A test that only asserted "an exception was thrown" would pass
 * just as happily against a parser that read the file first and threw for some later reason, so the
 * assertions check the canary against everything the caller can observe - the parsed value and the
 * failure - and not merely that the call failed.
 */
final class XxePayloads {

    /** The content of the file an external general entity is pointed at. */
    static final String FILE_CANARY = "S100-XXE-FILE-CANARY-4f2a91c6";

    /** The value an external parameter entity would smuggle in through a DTD fragment. */
    static final String PARAMETER_CANARY = "S100-XXE-PARAMETER-CANARY-8b7d30ae";

    /**
     * The part of the message Xerces reports when {@code disallow-doctype-decl} stops a document.
     * Asserted on, rather than merely asserting that something was thrown, because the point of the
     * hardening is that the document is refused <em>at the declaration</em> - before any entity is
     * resolved and before any schema is consulted. A rejection carrying any other reason would mean
     * the parser got further into the document than it should have.
     * <p/>
     * The feature name is asserted rather than the English prose around it ("DOCTYPE is
     * disallowed"): the JDK ships {@code XMLMessages_de}, {@code _ja} and {@code _zh_CN} bundles
     * and Xerces resolves them through the default locale, so the prose is German on a German JVM
     * and this suite would report a working control as broken. Every translation of the message
     * interpolates the feature name verbatim, and no other Xerces message carries it.
     */
    static final String REFUSAL = "http://apache.org/xml/features/disallow-doctype-decl";

    private XxePayloads() {
    }

    /**
     * Writes the file an external general entity payload will try to read.
     *
     * @return the file's URI, for use as an entity system identifier
     */
    static String writeSecretFile(Path dir) throws IOException {
        final Path secret = dir.resolve("secret.txt");
        Files.writeString(secret, FILE_CANARY, StandardCharsets.UTF_8);
        return secret.toUri().toString();
    }

    /**
     * Writes the DTD fragment an external parameter entity payload will pull in. The fragment
     * declares a general entity, which is the standard way a parameter entity gets attacker
     * controlled text into the document body, so resolving it leaves the canary in a parsed field.
     *
     * @return the fragment's URI, for use as a parameter entity system identifier
     */
    static String writeSecretDtd(Path dir) throws IOException {
        final Path dtd = dir.resolve("evil.dtd");
        Files.writeString(dtd, "<!ENTITY smuggled \"" + PARAMETER_CANARY + "\">", StandardCharsets.UTF_8);
        return dtd.toUri().toString();
    }

    /** An external general entity reading a file off the parsing host: the classic XXE. */
    static String externalGeneralEntity(String rootElement, String secretFileUri) {
        return "<!DOCTYPE " + rootElement + " [ <!ENTITY leak SYSTEM \"" + secretFileUri + "\"> ]>";
    }

    /**
     * An external parameter entity, dereferenced while the internal subset is still being read.
     * The fragment it pulls in declares the general entity the body then references, so an
     * unhardened parser both fetches the URL and lands its content in the document.
     */
    static String externalParameterEntity(String rootElement, String secretDtdUri) {
        return "<!DOCTYPE " + rootElement + " [ <!ENTITY % remote SYSTEM \"" + secretDtdUri + "\"> %remote; ]>";
    }

    /**
     * The billion-laughs expansion: internal entities only, so it needs no network and no file, and
     * it is stopped by the same DOCTYPE refusal rather than by an expansion counter part way in.
     */
    static String billionLaughs(String rootElement) {
        final StringBuilder subset = new StringBuilder("<!DOCTYPE ").append(rootElement).append(" [\n")
                .append("<!ENTITY lol \"lol\">\n");
        for (int i = 1; i <= 9; i++) {
            subset.append("<!ENTITY lol").append(i).append(" \"");
            for (int j = 0; j < 10; j++) {
                subset.append("&lol").append(i == 1 ? "" : String.valueOf(i - 1)).append(";");
            }
            subset.append("\">\n");
        }
        return subset.append("]>").toString();
    }

    /** A DOCTYPE that declares nothing at all - harmless, and still refused. */
    static String bareDoctype(String rootElement) {
        return "<!DOCTYPE " + rootElement + ">";
    }

    /**
     * Runs a read and reports everything the caller could observe from it: the value it produced,
     * or the failure it produced. A leak is a leak whether the canary comes back in a parsed field
     * or inside an error message quoting it, so both are folded into one searchable string.
     */
    static Outcome outcomeOf(Callable<String> read) {
        try {
            return new Outcome(false, String.valueOf(read.call()));
        } catch (Throwable t) {
            return new Outcome(true, describe(t));
        }
    }

    /**
     * Renders a failure, following {@code getLinkedException} as well as {@code getCause}: JAXB
     * wraps a SAX parse failure in a JAXBException whose cause is null and whose linked exception
     * carries the real message, so a plain cause walk would lose the very text under assertion.
     */
    private static String describe(Throwable failure) {
        final StringWriter rendered = new StringWriter();
        final PrintWriter out = new PrintWriter(rendered);
        for (Throwable t = failure; t != null; ) {
            out.println(t);
            out.println(t.getMessage());
            Throwable next = t.getCause();
            if (next == null && t instanceof JAXBException jaxb) {
                next = jaxb.getLinkedException();
            }
            t = next == t ? null : next;
        }
        return rendered.toString();
    }

    /** The observable result of a read: whether it failed, and everything it said or returned. */
    record Outcome(boolean threw, String text) {
    }

}
