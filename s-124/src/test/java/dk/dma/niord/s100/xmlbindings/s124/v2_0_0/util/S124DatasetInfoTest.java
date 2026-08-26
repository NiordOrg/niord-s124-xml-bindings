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

    @Test
    void rejectsApplicationProfilesOutsideTable10b4() {
        S124DatasetInfo info = new S124DatasetInfo("NW-001", "DK00");

        assertThatThrownBy(() -> info.setApplicationProfile("NavigationalWarning"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("applicationProfile");
        assertThat(info.getApplicationProfile()).isEqualTo("1");

        info.setApplicationProfile(S124DatasetInfo.UPDATE_APPLICATION_PROFILE);
        assertThat(info.getApplicationProfile()).isEqualTo("2");
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

        String xml = S124Utils.marshalS124(dataset);
        assertThatCode(() -> S124XsdValidator.validate(xml))
                .as("XSD validation errors in:%n%s", xml)
                .doesNotThrowAnyException();
    }
}