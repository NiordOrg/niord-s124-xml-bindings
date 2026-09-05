package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.exchangesets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.XxePayloads;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.XxePayloads.Outcome;

/**
 * {@link S124ExchangeSetFactory#readDiscoveryMetadata(byte[])} is the entry point the hardening was
 * asked for: its signature takes a ZIP, and an exchange set received from a SECOM peer is a
 * realistic thing to hand it, so the catalogue inside is foreign XML however carefully the archive
 * was described as "the producer's own".
 * <p/>
 * The attack is therefore staged the way it would actually arrive - as a real ZIP whose
 * {@code S100_ROOT/CATALOG.XML} is the hostile document - rather than by calling the unmarshaller
 * underneath. Nothing but a test of this method proves that the bytes coming out of the archive
 * reach a hardened parser.
 */
class S124ExchangeSetFactoryXxeTest {

    private static final String CATALOG_XML = "S100_ROOT/CATALOG.XML";
    private static final String CATALOGUE_ROOT = "S100_ExchangeCatalogue";
    private static final String PUBLISHED_FILE = "124DK00DKS124test.GML";

    private String secretFileUri;
    private String secretDtdUri;

    @BeforeEach
    void writeTheFilesAnAttackerWouldRead(@TempDir Path tempDir) throws IOException {
        this.secretFileUri = XxePayloads.writeSecretFile(tempDir);
        this.secretDtdUri = XxePayloads.writeSecretDtd(tempDir);
    }

    /**
     * The control: the same archive, the same catalogue, no DOCTYPE. Without it a green rejection
     * test would prove only that the hand-built catalogue was unreadable.
     */
    @Test
    void anArchiveWithoutADoctypeStillYieldsItsEntries() throws Exception {
        final byte[] zip = exchangeSetZip(catalogue(null, PUBLISHED_FILE));

        assertThat(S124ExchangeSetFactory.readDiscoveryMetadata(zip)).containsOnlyKeys(PUBLISHED_FILE);
    }

    @Test
    void anArchiveCarryingAnExternalGeneralEntityIsRefusedAndTheFileIsNotRead() {
        assertNothingLeaked(read(XxePayloads.externalGeneralEntity(CATALOGUE_ROOT, this.secretFileUri), "&leak;"));
    }

    @Test
    void anArchiveCarryingAnExternalParameterEntityIsRefusedAndTheFragmentIsNotFetched() {
        assertNothingLeaked(read(XxePayloads.externalParameterEntity(CATALOGUE_ROOT, this.secretDtdUri), "&smuggled;"));
    }

    @Test
    void anArchiveCarryingABillionLaughsCatalogueIsRefusedAtTheDeclaration() {
        assertRefusedAtTheDoctype(read(XxePayloads.billionLaughs(CATALOGUE_ROOT), "&lol9;"));
    }

    @Test
    void anArchiveCarryingABareDoctypeIsRefused() {
        assertRefusedAtTheDoctype(read(XxePayloads.bareDoctype(CATALOGUE_ROOT), PUBLISHED_FILE));
    }

    /** Everything the caller can see - the keys it got back, or the failure - free of both canaries. */
    private static void assertNothingLeaked(Outcome outcome) {
        assertSoftly(softly -> {
            softly.assertThat(outcome.text())
                    .as("the file's content must not reach the caller")
                    .doesNotContain(XxePayloads.FILE_CANARY);
            softly.assertThat(outcome.text())
                    .as("the DTD fragment's content must not reach the caller")
                    .doesNotContain(XxePayloads.PARAMETER_CANARY);
            softly.assertThat(outcome.threw())
                    .as("the hostile archive must be refused, it returned: %s", outcome.text())
                    .isTrue();
            softly.assertThat(outcome.text())
                    .as("the refusal must name the DOCTYPE")
                    .contains(XxePayloads.REFUSAL);
        });
    }

    private static void assertRefusedAtTheDoctype(Outcome outcome) {
        assertSoftly(softly -> {
            softly.assertThat(outcome.threw())
                    .as("the hostile archive must be refused, it returned: %s", outcome.text())
                    .isTrue();
            softly.assertThat(outcome.text())
                    .as("the refusal must name the DOCTYPE")
                    .contains(XxePayloads.REFUSAL);
        });
    }

    /** Reads the archive and reports the file names it published, which is where a leak would show. */
    private static Outcome read(String doctype, String fileName) {
        return XxePayloads.outcomeOf(() -> String.join(
                ",", S124ExchangeSetFactory.readDiscoveryMetadata(exchangeSetZip(catalogue(doctype, fileName)))
                        .keySet()));
    }

    /** An exchange set laid out as S-100 Part 17, clause 17-4.2, requires: the catalogue at the root. */
    private static byte[] exchangeSetZip(String catalogueXml) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(bytes)) {
            zos.putNextEntry(new ZipEntry("S100_ROOT/"));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry(CATALOG_XML));
            zos.write(catalogueXml.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        return bytes.toByteArray();
    }

    /**
     * A catalogue publishing one dataset. The file name is the field a resolved entity lands in,
     * and it is also the key the method returns, so a leak arrives in the caller's map.
     */
    private static String catalogue(String doctype, String fileName) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + (doctype == null ? "" : doctype + "\n")
                + "<S100_ExchangeCatalogue xmlns=\"http://www.iho.int/s100/xc/5.2\">\n"
                + "    <identifier>\n"
                + "        <identifier>Hostile exchange set</identifier>\n"
                + "    </identifier>\n"
                + "    <datasetDiscoveryMetadata>\n"
                + "        <S100_DatasetDiscoveryMetadata>\n"
                + "            <fileName>" + fileName + "</fileName>\n"
                + "            <purpose>newDataset</purpose>\n"
                + "        </S100_DatasetDiscoveryMetadata>\n"
                + "    </datasetDiscoveryMetadata>\n"
                + "</S100_ExchangeCatalogue>\n";
    }

}
