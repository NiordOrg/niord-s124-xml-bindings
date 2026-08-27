package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.DatasetPurposeType;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.DataSetIdentificationTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.BoundingShapeTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.EnvelopeTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.impl.PosImpl;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.Dataset;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.ObjectFactory;

class S124DatasetInfoTest {

    /**
     * S-124 clause 9.7 requires dataset files to be named according to S-100 Part 17,
     * clause 17-4.3: product code (124), producer code, an alphanumeric unique code and the
     * encoding specific extension (.GML for the GML encoding). Part 10b Table 10b-4 requires
     * the dataset file identifier to include that extension.
     */
    @Test
    void defaultFileIdentifierFollowsTheS100NamingPattern() {
        S124DatasetInfo info = new S124DatasetInfo("NW-001", "DK00");

        assertThat(info.getFileIdentifier()).isEqualTo("124DK00NW001.GML");
        assertThat(Pattern.matches("124[A-Za-z0-9]+\\.GML", info.getFileIdentifier())).isTrue();
    }

    @Test
    void fileIdentifierCanBeOverridden() {
        S124DatasetInfo info = new S124DatasetInfo("NW-001", "DK00");

        info.setFileIdentifier("124DK00XYZ.GML");

        assertThat(info.getFileIdentifier()).isEqualTo("124DK00XYZ.GML");
    }

    /**
     * S-100 Part 10b Table 10b-4 admits two values for applicationProfile: "1" for base
     * datasets and "2" for update datasets. The XSD types the element as a plain string,
     * so nothing but this carrier keeps the value inside the standard.
     */
    @Test
    void defaultApplicationProfileIsTheBaseDatasetCode() {
        S124DatasetInfo info = new S124DatasetInfo("NW-001", "DK00");

        assertThat(info.getPurpose()).isEqualTo(DatasetPurposeType.BASE);
        assertThat(info.getApplicationProfile()).isEqualTo("1");
    }

    /** The profile follows the purpose: an update dataset is application profile "2". */
    @Test
    void applicationProfileFollowsTheDatasetPurpose() {
        S124DatasetInfo info = new S124DatasetInfo("NW-001", "DK00");

        info.setPurpose(DatasetPurposeType.UPDATE);
        assertThat(info.getApplicationProfile()).isEqualTo("2");

        info.setPurpose(DatasetPurposeType.BASE);
        assertThat(info.getApplicationProfile()).isEqualTo("1");
    }

    /**
     * The pairing holds from either side: Table 10b-4 leaves no combination of an update
     * purpose with the base profile, or the reverse, so setting the profile sets the
     * purpose it stands for rather than letting the header contradict itself.
     */
    @Test
    void datasetPurposeFollowsTheApplicationProfile() {
        S124DatasetInfo info = new S124DatasetInfo("NW-001", "DK00");

        info.setApplicationProfile(S124DatasetInfo.UPDATE_APPLICATION_PROFILE);
        assertThat(info.getPurpose()).isEqualTo(DatasetPurposeType.UPDATE);

        info.setApplicationProfile(S124DatasetInfo.BASE_APPLICATION_PROFILE);
        assertThat(info.getPurpose()).isEqualTo(DatasetPurposeType.BASE);

        // ... in either order: an update dataset cannot be talked back into profile "1"
        info.setPurpose(DatasetPurposeType.UPDATE);
        info.setApplicationProfile(S124DatasetInfo.BASE_APPLICATION_PROFILE);
        assertThat(info.getPurpose()).isEqualTo(DatasetPurposeType.BASE);
        assertThat(info.getApplicationProfile()).isEqualTo("1");
    }

    @Test
    void rejectsApplicationProfilesOutsideTable10b4() {
        S124DatasetInfo info = new S124DatasetInfo("NW-001", "DK00");

        assertThatThrownBy(() -> info.setApplicationProfile("NavigationalWarning"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("applicationProfile");
        assertThat(info.getApplicationProfile()).isEqualTo("1");
        assertThat(info.getPurpose()).isEqualTo(DatasetPurposeType.BASE);

        info.setApplicationProfile(S124DatasetInfo.UPDATE_APPLICATION_PROFILE);
        assertThat(info.getApplicationProfile()).isEqualTo("2");
    }

    /** datasetPurpose is mandatory, so a null purpose would only surface at XSD validation. */
    @Test
    void rejectsANullDatasetPurpose() {
        S124DatasetInfo info = new S124DatasetInfo("NW-001", "DK00");

        assertThatThrownBy(() -> info.setPurpose(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("purpose");
    }

    /**
     * S-100 Part 10b Table 10b-4 binds datasetLanguage to ISO 639-2/T alpha-3 codes; the
     * bound XSD type enforces a three character pattern, which "en" violates.
     */
    @Test
    void defaultLanguageIsTheThreeLetterIso6392Code() {
        S124DatasetInfo info = new S124DatasetInfo("NW-001", "DK00");

        assertThat(info.getLanguage()).isEqualTo("eng");
    }

    /**
     * Every element of DatasetIdentificationType is mandatory, so a dataset built purely
     * from the carrier's defaults must marshal to schema-valid S-124: this is what pins
     * applicationProfile, datasetReferenceDate, datasetTopicCategory, datasetPurpose and
     * updateNumber as defaults rather than caller obligations.
     */
    @Test
    void defaultsProduceASchemaValidDatasetIdentification() throws Exception {
        S124DatasetInfo info = new S124DatasetInfo("NW-001", "DK00");

        ObjectFactory of = new ObjectFactory();
        Dataset dataset = of.createDataset();
        dataset.setId("DK.S124.NW001");

        DataSetIdentificationTypeImpl ident = new DataSetIdentificationTypeImpl();
        ident.setEncodingSpecification(info.getEncodingSpecification());
        ident.setEncodingSpecificationEdition(info.getEncodingSpecificationEdition());
        ident.setProductIdentifier(info.getProductionIdentifier());
        ident.setProductEdition(info.getProductionEdition());
        ident.setApplicationProfile(info.getApplicationProfile());
        ident.setDatasetFileIdentifier(info.getFileIdentifier());
        ident.setDatasetTitle(info.getTitle());
        ident.setDatasetReferenceDate(info.getReferenceDate());
        ident.setDatasetLanguage(info.getLanguage());
        ident.setDatasetAbstract(info.getAbstractText());
        ident.getDatasetTopicCategories().add(info.getTopicCategory());
        ident.setDatasetPurpose(info.getPurpose());
        ident.setUpdateNumber(info.getUpdateNumber());
        dataset.setDatasetIdentificationInformation(ident);

        PosImpl lower = new PosImpl();
        lower.setValue(new Double[] { 54.0, 8.0 });
        PosImpl upper = new PosImpl();
        upper.setValue(new Double[] { 58.0, 14.0 });
        EnvelopeTypeImpl envelope = new EnvelopeTypeImpl();
        envelope.setSrsName("EPSG:4326");
        envelope.setLowerCorner(lower);
        envelope.setUpperCorner(upper);
        BoundingShapeTypeImpl boundedBy = new BoundingShapeTypeImpl();
        boundedBy.setEnvelope(envelope);
        dataset.setBoundedBy(boundedBy);
        dataset.setMembers(of.createDatasetMembers());

        // Validation off: the subject is the identification block S124DatasetInfo produces, and
        // the fixture carries no NavwarnPreamble to make it a conformant warning.
        String xml = S124Utils.marshalS124(dataset, true, false);
        assertThatCode(() -> S124XsdValidator.validate(xml))
                .as("XSD validation errors in:%n%s", xml)
                .doesNotThrowAnyException();
    }

    /**
     * Without a way to project the carrier onto the type a dataset actually holds, the Table 10b-4
     * checks in this class guarded nothing - hand-populating DataSetIdentificationType was the only
     * path, and it bypasses every one of them.
     */
    @Test
    void applyToWritesTheValidatedFieldsOntoTheDataset() {
        Dataset dataset = new ObjectFactory().createDataset();
        dataset.setId("DK.S124.apply");
        dataset.setDatasetIdentificationInformation(new DataSetIdentificationTypeImpl());

        S124DatasetInfo info = new S124DatasetInfo("DK.S124.apply", "DK00");
        info.setTitle("Drogden Channel. Light buoy unlit.");
        info.applyTo(dataset);

        var ident = dataset.getDatasetIdentificationInformation();
        assertThat(ident.getEncodingSpecification()).isEqualTo("S-100 Part 10b");
        assertThat(ident.getProductIdentifier()).isEqualTo("S-124");
        assertThat(ident.getProductEdition()).isEqualTo("2.0.0");
        assertThat(ident.getDatasetTitle()).isEqualTo("Drogden Channel. Light buoy unlit.");
        assertThat(ident.getDatasetFileIdentifier()).isEqualTo("124DK00DKS124apply.GML");
        assertThat(ident.getDatasetTopicCategories()).hasSize(1);
        assertThat(ident.getDatasetPurpose()).isEqualTo(DatasetPurposeType.BASE);
        // The pairing this class exists to enforce now actually reaches the dataset.
        assertThat(ident.getApplicationProfile()).isEqualTo(S124DatasetInfo.BASE_APPLICATION_PROFILE);
    }

    /** Applying twice must not accumulate topic categories. */
    @Test
    void applyToIsIdempotent() {
        Dataset dataset = new ObjectFactory().createDataset();
        dataset.setDatasetIdentificationInformation(new DataSetIdentificationTypeImpl());
        S124DatasetInfo info = new S124DatasetInfo("DK.S124.twice", "DK00");

        info.applyTo(dataset);
        info.applyTo(dataset);

        assertThat(dataset.getDatasetIdentificationInformation().getDatasetTopicCategories()).hasSize(1);
    }
}
