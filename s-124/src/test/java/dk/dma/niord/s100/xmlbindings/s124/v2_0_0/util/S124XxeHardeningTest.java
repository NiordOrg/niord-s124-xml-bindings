package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.Dataset;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.XxePayloads;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.XxePayloads.Outcome;

/**
 * The three S-124 document entry points - reading a dataset back, checking one against the schema,
 * and re-indenting one for display - must refuse a document type declaration rather than act on it.
 * <p/>
 * All three take a dataset whose origin is a foreign producer: that is what S-124 is for. Each case
 * here is the marshalled form of a real, schema-valid dataset with a DOCTYPE spliced in and one
 * element's text replaced by an entity reference, and each has a control that reads the identical
 * document without the DOCTYPE - which is what makes a rejection attributable to the declaration
 * rather than to a body the parser disliked anyway.
 */
class S124XxeHardeningTest {

    /** The element a resolved general entity lands in on the unmarshal path: a plain string. */
    private static final String TITLE = "datasetTitle";

    /**
     * The element it lands in on the validation path. An {@code xs:date}, chosen so that a parser
     * which did resolve the entity reports the resolved text back in its {@code cvc-} message -
     * the leak is then visible to the assertions rather than merely inferable.
     */
    private static final String REFERENCE_DATE = "datasetReferenceDate";

    private String secretFileUri;
    private String secretDtdUri;
    private String datasetXml;
    private String rootElement;

    @BeforeEach
    void writeTheFilesAnAttackerWouldRead(@TempDir Path tempDir) throws IOException {
        this.secretFileUri = XxePayloads.writeSecretFile(tempDir);
        this.secretDtdUri = XxePayloads.writeSecretDtd(tempDir);
    }

    @BeforeEach
    void marshalTheDatasetEveryPayloadIsBuiltFrom() throws Exception {
        // Marshalled with the conformance checks on, so the body of every payload below is a
        // dataset this library considers valid and the schema accepts.
        this.datasetXml = S124Utils.marshalS124(S124TestDatasets.datasetWithPreamble(), true, true);
        this.rootElement = rootElementOf(this.datasetXml);
    }

    @Test
    void unmarshalWithoutADoctypeStillParses() throws Exception {
        final Dataset dataset = S124Utils.unmarshallS124(withText(TITLE, "A harmless title"));

        assertThat(dataset.getDatasetIdentificationInformation().getDatasetTitle()).isEqualTo("A harmless title");
    }

    @Test
    void unmarshalRefusesAnExternalGeneralEntityAndDoesNotReadTheFile() {
        assertNothingLeaked(unmarshal(XxePayloads.externalGeneralEntity(this.rootElement, this.secretFileUri),
                TITLE, "&leak;"));
    }

    @Test
    void unmarshalRefusesAnExternalParameterEntityAndDoesNotFetchTheFragment() {
        assertNothingLeaked(unmarshal(XxePayloads.externalParameterEntity(this.rootElement, this.secretDtdUri),
                TITLE, "&smuggled;"));
    }

    @Test
    void unmarshalRefusesBillionLaughsAtTheDeclaration() {
        assertRefusedAtTheDoctype(unmarshal(XxePayloads.billionLaughs(this.rootElement), TITLE, "&lol9;"));
    }

    @Test
    void unmarshalRefusesABareDoctype() {
        assertRefusedAtTheDoctype(unmarshal(XxePayloads.bareDoctype(this.rootElement), TITLE, "A harmless title"));
    }

    @Test
    void validationWithoutADoctypeStillAcceptsAValidDataset() {
        final Outcome outcome = XxePayloads.outcomeOf(() -> {
            S124XsdValidator.validate(this.datasetXml);
            return "schema-valid";
        });

        assertThat(outcome.threw()).as(outcome.text()).isFalse();
    }

    @Test
    void validationRefusesAnExternalGeneralEntityAndDoesNotReadTheFile() {
        assertNothingLeaked(validate(XxePayloads.externalGeneralEntity(this.rootElement, this.secretFileUri),
                REFERENCE_DATE, "&leak;"));
    }

    @Test
    void validationRefusesAnExternalParameterEntityAndDoesNotFetchTheFragment() {
        assertNothingLeaked(validate(XxePayloads.externalParameterEntity(this.rootElement, this.secretDtdUri),
                REFERENCE_DATE, "&smuggled;"));
    }

    @Test
    void validationRefusesBillionLaughsAtTheDeclaration() {
        assertRefusedAtTheDoctype(validate(XxePayloads.billionLaughs(this.rootElement), REFERENCE_DATE, "&lol9;"));
    }

    @Test
    void validationRefusesABareDoctype() {
        assertRefusedAtTheDoctype(validate(XxePayloads.bareDoctype(this.rootElement), REFERENCE_DATE, "2026-08-25"));
    }

    @Test
    void prettyPrintWithoutADoctypeStillFormats() {
        final String formatted = S124Utils.prettyPrint(withText(TITLE, "A harmless title"));

        assertThat(formatted).contains(">A harmless title<");
    }

    @Test
    void prettyPrintRefusesAnExternalGeneralEntityAndDoesNotReadTheFile() {
        assertNothingLeaked(prettyPrint(XxePayloads.externalGeneralEntity(this.rootElement, this.secretFileUri),
                TITLE, "&leak;"));
    }

    @Test
    void prettyPrintRefusesAnExternalParameterEntityAndDoesNotFetchTheFragment() {
        assertNothingLeaked(prettyPrint(XxePayloads.externalParameterEntity(this.rootElement, this.secretDtdUri),
                TITLE, "&smuggled;"));
    }

    @Test
    void prettyPrintRefusesBillionLaughsAtTheDeclaration() {
        assertRefusedAtTheDoctype(prettyPrint(XxePayloads.billionLaughs(this.rootElement), TITLE, "&lol9;"));
    }

    @Test
    void prettyPrintRefusesABareDoctype() {
        assertRefusedAtTheDoctype(prettyPrint(XxePayloads.bareDoctype(this.rootElement), TITLE, "A harmless title"));
    }

    /**
     * The read failed, said so because of the declaration, and neither canary appears anywhere the
     * caller can see it. Asserted softly so that a regression reports the leak as well as the
     * missing rejection, instead of stopping at whichever fails first.
     */
    private static void assertNothingLeaked(Outcome outcome) {
        assertSoftly(softly -> {
            softly.assertThat(outcome.text())
                    .as("the file's content must not reach the caller")
                    .doesNotContain(XxePayloads.FILE_CANARY);
            softly.assertThat(outcome.text())
                    .as("the DTD fragment's content must not reach the caller")
                    .doesNotContain(XxePayloads.PARAMETER_CANARY);
            softly.assertThat(outcome.threw())
                    .as("the hostile document must be refused, it returned: %s", outcome.text())
                    .isTrue();
            softly.assertThat(outcome.text())
                    .as("the refusal must name the DOCTYPE, not a schema rule")
                    .contains(XxePayloads.REFUSAL)
                    .doesNotContain(XxePayloads.SCHEMA_FAILURE);
        });
    }

    /**
     * The document was refused for its DOCTYPE. Asserting the reason, not just the failure, is what
     * separates hardening from a parser that resolved the entity and then tripped over something
     * else - and, on the validation path, what tells a caller an attack from a non-conformant
     * dataset.
     */
    private static void assertRefusedAtTheDoctype(Outcome outcome) {
        assertSoftly(softly -> {
            softly.assertThat(outcome.threw())
                    .as("the hostile document must be refused, it returned: %s", outcome.text())
                    .isTrue();
            softly.assertThat(outcome.text())
                    .as("the refusal must name the DOCTYPE, not a schema rule")
                    .contains(XxePayloads.REFUSAL)
                    .doesNotContain(XxePayloads.SCHEMA_FAILURE);
        });
    }

    private Outcome unmarshal(String doctype, String element, String text) {
        final String xml = withDoctype(doctype, withText(element, text));
        return XxePayloads.outcomeOf(() -> S124Utils.unmarshallS124(xml)
                .getDatasetIdentificationInformation()
                .getDatasetTitle());
    }

    private Outcome prettyPrint(String doctype, String element, String text) {
        final String xml = withDoctype(doctype, withText(element, text));
        return XxePayloads.outcomeOf(() -> S124Utils.prettyPrint(xml));
    }

    private Outcome validate(String doctype, String element, String text) {
        final String xml = withDoctype(doctype, withText(element, text));
        return XxePayloads.outcomeOf(() -> {
            S124XsdValidator.validate(xml);
            return "schema-valid";
        });
    }

    /** The same dataset with one element's text replaced, and no DOCTYPE: the control document. */
    private String withText(String localName, String text) {
        final Pattern element = Pattern.compile("<((?:\\w+:)?" + localName + ")>[^<]*</\\1>");
        final Matcher matcher = element.matcher(this.datasetXml);
        assertThat(matcher.find()).as("the fixture must carry a <%s> to inject into", localName).isTrue();
        return matcher.replaceFirst(
                Matcher.quoteReplacement("<" + matcher.group(1) + ">" + text + "</" + matcher.group(1) + ">"));
    }

    /**
     * Splices the declaration in after the XML declaration, which is rewritten without its
     * {@code standalone="yes"}: a standalone document promises that no external markup declaration
     * affects its content, and the payloads exist precisely to break that promise.
     */
    private static String withDoctype(String doctype, String xml) {
        final int endOfDeclaration = xml.indexOf("?>") + 2;
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + doctype + xml.substring(endOfDeclaration);
    }

    /** The document element's qualified name, which a DOCTYPE declaration has to repeat exactly. */
    private static String rootElementOf(String xml) {
        final Matcher root = Pattern.compile("<([\\w.:-]+)[\\s>]").matcher(xml.substring(xml.indexOf("?>") + 2));
        assertThat(root.find()).as("the fixture must have a document element").isTrue();
        return root.group(1);
    }

}
