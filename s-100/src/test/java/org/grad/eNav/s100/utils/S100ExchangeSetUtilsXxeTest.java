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

import dk.dma.niord.s100.catalog._5_2.S100DatasetDiscoveryMetadata;
import dk.dma.niord.s100.catalog._5_2.S100ExchangeCatalogue;
import org.grad.eNav.s100.utils.XxePayloads.Outcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The two catalogue read paths must refuse a document type declaration rather than act on it.
 * <p/>
 * {@code readDiscoveryMetadata} on a peer's exchange set reaches
 * {@link S100ExchangeSetUtils#unmarshallS100ExchangeSetCatalogue}, and a cancellation's stored
 * {@code original} reaches {@link S100ExchangeSetUtils#unmarshallS100DatasetDiscoveryMetadata}, so
 * both take XML this library did not write. Each test here has a control that parses the identical
 * document without the DOCTYPE, which is what makes the rejection attributable to the declaration
 * and not to a body the parser disliked anyway.
 */
class S100ExchangeSetUtilsXxeTest {

    private static final String CATALOGUE_ROOT = "S100_ExchangeCatalogue";
    private static final String METADATA_ROOT = "S100_DatasetDiscoveryMetadata";

    /** The URI of a real file holding {@link XxePayloads#FILE_CANARY}. */
    private String secretFileUri;

    /** The URI of a real DTD fragment declaring an entity holding {@link XxePayloads#PARAMETER_CANARY}. */
    private String secretDtdUri;

    @BeforeEach
    void writeTheFilesAnAttackerWouldRead(@TempDir Path tempDir) throws IOException {
        this.secretFileUri = XxePayloads.writeSecretFile(tempDir);
        this.secretDtdUri = XxePayloads.writeSecretDtd(tempDir);
    }

    /**
     * The control for every catalogue case below: the same document, minus the DOCTYPE, parses and
     * yields its identifier. Without this, a green DOCTYPE test would prove only that the payload
     * was unparseable.
     */
    @Test
    void catalogueWithoutADoctypeStillParses() throws Exception {
        final S100ExchangeCatalogue catalogue =
                S100ExchangeSetUtils.unmarshallS100ExchangeSetCatalogue(catalogue(null, "A harmless identifier"));

        assertEquals("A harmless identifier", catalogue.getIdentifier().getIdentifier());
    }

    @Test
    void catalogueExternalGeneralEntityIsRefusedAndTheFileIsNotRead() {
        final Outcome outcome = readCatalogue(
                XxePayloads.externalGeneralEntity(CATALOGUE_ROOT, this.secretFileUri), "&leak;");

        assertNothingLeaked(outcome);
    }

    @Test
    void catalogueExternalParameterEntityIsRefusedAndTheFragmentIsNotFetched() {
        final Outcome outcome = readCatalogue(
                XxePayloads.externalParameterEntity(CATALOGUE_ROOT, this.secretDtdUri), "&smuggled;");

        assertNothingLeaked(outcome);
    }

    @Test
    void catalogueBillionLaughsIsRefusedAtTheDeclaration() {
        final Outcome outcome = readCatalogue(XxePayloads.billionLaughs(CATALOGUE_ROOT), "&lol9;");

        assertRefusedAtTheDoctype(outcome);
    }

    @Test
    void catalogueBareDoctypeIsRefused() {
        final Outcome outcome = readCatalogue(XxePayloads.bareDoctype(CATALOGUE_ROOT), "A harmless identifier");

        assertRefusedAtTheDoctype(outcome);
    }

    /** The control for the discovery metadata cases: the same document, minus the DOCTYPE. */
    @Test
    void discoveryMetadataWithoutADoctypeStillParses() throws Exception {
        final S100DatasetDiscoveryMetadata metadata =
                S100ExchangeSetUtils.unmarshallS100DatasetDiscoveryMetadata(metadata(null, "124DK00.GML"));

        assertEquals("124DK00.GML", metadata.getFileName());
    }

    @Test
    void discoveryMetadataExternalGeneralEntityIsRefusedAndTheFileIsNotRead() {
        final Outcome outcome = readMetadata(
                XxePayloads.externalGeneralEntity(METADATA_ROOT, this.secretFileUri), "&leak;");

        assertNothingLeaked(outcome);
    }

    @Test
    void discoveryMetadataExternalParameterEntityIsRefusedAndTheFragmentIsNotFetched() {
        final Outcome outcome = readMetadata(
                XxePayloads.externalParameterEntity(METADATA_ROOT, this.secretDtdUri), "&smuggled;");

        assertNothingLeaked(outcome);
    }

    @Test
    void discoveryMetadataBillionLaughsIsRefusedAtTheDeclaration() {
        final Outcome outcome = readMetadata(XxePayloads.billionLaughs(METADATA_ROOT), "&lol9;");

        assertRefusedAtTheDoctype(outcome);
    }

    @Test
    void discoveryMetadataBareDoctypeIsRefused() {
        final Outcome outcome = readMetadata(XxePayloads.bareDoctype(METADATA_ROOT), "124DK00.GML");

        assertRefusedAtTheDoctype(outcome);
    }

    /**
     * The read failed, said so because of the declaration, and neither canary appears anywhere the
     * caller can see it. All three are asserted together so that a regression reports the leak as
     * well as the missing rejection, rather than stopping at whichever fails first.
     */
    private static void assertNothingLeaked(Outcome outcome) {
        assertAll(
                () -> assertFalse(outcome.text().contains(XxePayloads.FILE_CANARY),
                        () -> "the file's content reached the caller:\n" + outcome.text()),
                () -> assertFalse(outcome.text().contains(XxePayloads.PARAMETER_CANARY),
                        () -> "the DTD fragment's content reached the caller:\n" + outcome.text()),
                () -> assertRefusedAtTheDoctype(outcome));
    }

    /**
     * The document was refused for its DOCTYPE. Asserting the reason, not just the failure, is what
     * distinguishes hardening from a parser that resolved the entity and then tripped over
     * something else - and it is what a caller needs to tell an attack from a malformed document.
     */
    private static void assertRefusedAtTheDoctype(Outcome outcome) {
        assertTrue(outcome.threw(), () -> "the hostile document was accepted, and returned: " + outcome.text());
        assertTrue(outcome.text().contains(XxePayloads.REFUSAL),
                () -> "the document was rejected, but not for its DOCTYPE:\n" + outcome.text());
    }

    private static Outcome readCatalogue(String doctype, String identifier) {
        return XxePayloads.outcomeOf(() -> S100ExchangeSetUtils
                .unmarshallS100ExchangeSetCatalogue(catalogue(doctype, identifier))
                .getIdentifier()
                .getIdentifier());
    }

    private static Outcome readMetadata(String doctype, String fileName) {
        return XxePayloads.outcomeOf(() -> S100ExchangeSetUtils
                .unmarshallS100DatasetDiscoveryMetadata(metadata(doctype, fileName))
                .getFileName());
    }

    /** A minimal catalogue whose identifier is the field a resolved entity would land in. */
    private static String catalogue(String doctype, String identifier) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + (doctype == null ? "" : doctype + "\n")
                + "<S100_ExchangeCatalogue xmlns=\"http://www.iho.int/s100/xc/5.2\">\n"
                + "    <identifier>\n"
                + "        <identifier>" + identifier + "</identifier>\n"
                + "    </identifier>\n"
                + "</S100_ExchangeCatalogue>\n";
    }

    /** A minimal discovery metadata entry whose fileName is that field. */
    private static String metadata(String doctype, String fileName) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + (doctype == null ? "" : doctype + "\n")
                + "<S100_DatasetDiscoveryMetadata xmlns=\"http://www.iho.int/s100/xc/5.2\">\n"
                + "    <fileName>" + fileName + "</fileName>\n"
                + "</S100_DatasetDiscoveryMetadata>\n";
    }

}
