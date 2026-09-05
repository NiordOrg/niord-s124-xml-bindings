package dk.dma.niord.s100.xmlbindings.s124.v2_0_0;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

import jakarta.xml.bind.JAXBException;

/**
 * The hostile document type declarations the S-124 read paths must refuse, and the machinery for
 * proving they refused them without having read anything first.
 * <p/>
 * Every payload is an internal subset to be spliced into an otherwise well-formed document of the
 * type under test, so a rejection can only be attributed to the DOCTYPE: the body around it is the
 * same body the control case of each test reads successfully.
 * <p/>
 * The two entity payloads point at real files on disk holding a known canary string, which is what
 * makes a passing test meaningful. A test that only asserted "an exception was thrown" would pass
 * just as happily against a parser that read the file first and threw for some later reason, so the
 * assertions check the canary against everything the caller can observe - the value returned and
 * the failure raised - and not merely that the call failed.
 * <p/>
 * This mirrors the class of the same name in the s-100 test sources. The two are deliberately not
 * shared: publishing an s-100 test-jar purely to reuse a test fixture would add an artifact to
 * every release of this library.
 */
public final class XxePayloads {

    /** The content of the file an external general entity is pointed at. */
    public static final String FILE_CANARY = "S124-XXE-FILE-CANARY-6c02be71";

    /** The value an external parameter entity would smuggle in through a DTD fragment. */
    public static final String PARAMETER_CANARY = "S124-XXE-PARAMETER-CANARY-1d59fa38";

    /**
     * The part of the message Xerces reports when {@code disallow-doctype-decl} stops a document.
     * Asserted on, rather than merely asserting that something was thrown, because the point of the
     * hardening is that the document is refused <em>at the declaration</em> - before any entity is
     * resolved and before any schema is consulted. In particular it is what tells an attack apart
     * from the {@code cvc-} report of a document that merely failed to match the S-124 schema.
     * <p/>
     * The feature name is asserted rather than the English prose around it ("DOCTYPE is
     * disallowed"): the JDK ships {@code XMLMessages_de}, {@code _ja} and {@code _zh_CN} bundles
     * and Xerces resolves them through the default locale, so the prose is German on a German JVM
     * and this suite would report a working control as broken. Every translation of the message
     * interpolates the feature name verbatim, and no other Xerces message carries it.
     */
    public static final String REFUSAL = "http://apache.org/xml/features/disallow-doctype-decl";

    /** The prefix of every Xerces schema validation message, which a refusal must not be. */
    public static final String SCHEMA_FAILURE = "cvc-";

    private XxePayloads() {
    }

    /**
     * Writes the file an external general entity payload will try to read.
     *
     * @return the file's URI, for use as an entity system identifier
     */
    public static String writeSecretFile(Path dir) throws IOException {
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
    public static String writeSecretDtd(Path dir) throws IOException {
        final Path dtd = dir.resolve("evil.dtd");
        Files.writeString(dtd, "<!ENTITY smuggled \"" + PARAMETER_CANARY + "\">", StandardCharsets.UTF_8);
        return dtd.toUri().toString();
    }

    /** An external general entity reading a file off the parsing host: the classic XXE. */
    public static String externalGeneralEntity(String rootElement, String secretFileUri) {
        return "<!DOCTYPE " + rootElement + " [ <!ENTITY leak SYSTEM \"" + secretFileUri + "\"> ]>";
    }

    /**
     * An external parameter entity, dereferenced while the internal subset is still being read.
     * The fragment it pulls in declares the general entity the body then references, so an
     * unhardened parser both fetches the URL and lands its content in the document.
     */
    public static String externalParameterEntity(String rootElement, String secretDtdUri) {
        return "<!DOCTYPE " + rootElement + " [ <!ENTITY % remote SYSTEM \"" + secretDtdUri + "\"> %remote; ]>";
    }

    /**
     * The billion-laughs expansion: internal entities only, so it needs no network and no file, and
     * it is stopped by the same DOCTYPE refusal rather than by an expansion counter part way in.
     */
    public static String billionLaughs(String rootElement) {
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
    public static String bareDoctype(String rootElement) {
        return "<!DOCTYPE " + rootElement + ">";
    }

    /**
     * Runs a read and reports everything the caller could observe from it: the value it produced,
     * or the failure it produced. A leak is a leak whether the canary comes back in a parsed field
     * or inside an error message quoting it, so both are folded into one searchable string.
     */
    public static Outcome outcomeOf(Callable<String> read) {
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
    public record Outcome(boolean threw, String text) {
    }

}
