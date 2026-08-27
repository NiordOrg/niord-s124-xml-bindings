package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.Dataset;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NameUsageLabel;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnPreamble;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnTypeGeneralLabel;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnTypeGeneralType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.ObjectFactory;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.QualityOfHorizontalMeasurementLabel;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.ReferenceCategoryLabel;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.RestrictionLabel;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.WarningTypeLabel;

/**
 * The label/code tables of S-124, and the normalisation that applies them.
 * <p/>
 * The code values asserted here are the ones printed in the S-124 Ed 2.0.0 UML, not merely the ones
 * the positional pairing happens to produce - that is the whole point of the test, since the pairing
 * is what would break silently if the bindings were regenerated from a revised schema.
 */
class S124CodedValuesTest {

    @Test
    void navwarnTypeGeneralCodesMatchTheSpecification() {
        assertThat(S124CodedValues.codeOf(NavwarnTypeGeneralLabel.AIDS_TO_NAVIGATION_CHANGES))
                .isEqualTo(BigInteger.ONE);
        assertThat(S124CodedValues.codeOf(NavwarnTypeGeneralLabel.DRIFTING_HAZARDS))
                .isEqualTo(BigInteger.valueOf(3));
        assertThat(S124CodedValues.codeOf(NavwarnTypeGeneralLabel.OTHER_HAZARDS))
                .isEqualTo(BigInteger.valueOf(5));
        assertThat(S124CodedValues.codeOf(NavwarnTypeGeneralLabel.IN_FORCE_BULLETIN))
                .isEqualTo(BigInteger.valueOf(8));
        assertThat(S124CodedValues.codeOf(NavwarnTypeGeneralLabel.DANGEROUS_NATURAL_PHENOMENA))
                .isEqualTo(BigInteger.valueOf(9));
        assertThat(S124CodedValues.codeOf(NavwarnTypeGeneralLabel.OFFSHORE_INFRASTRUCTURE))
                .isEqualTo(BigInteger.valueOf(11));
        assertThat(S124CodedValues.codeOf(NavwarnTypeGeneralLabel.COMMUNICATION_OR_BROADCAST_SERVICE_CHANGE))
                .isEqualTo(BigInteger.valueOf(13));
        assertThat(S124CodedValues.codeOf(NavwarnTypeGeneralLabel.RIG_LIST))
                .isEqualTo(BigInteger.valueOf(20));
    }

    /**
     * {@code restriction} is the one enumeration whose codes are not {@code 1..n} - they are drawn
     * from the wider S-100 restriction register - so a positional pairing that silently drifted
     * would show up here first.
     */
    @Test
    void restrictionCodesAreTheNonSequentialRegisterValues() {
        assertThat(S124CodedValues.codeOf(RestrictionLabel.ENTRY_PROHIBITED)).isEqualTo(BigInteger.valueOf(7));
        assertThat(S124CodedValues.codeOf(RestrictionLabel.ENTRY_RESTRICTED)).isEqualTo(BigInteger.valueOf(8));
        assertThat(S124CodedValues.codeOf(RestrictionLabel.AREA_TO_BE_AVOIDED)).isEqualTo(BigInteger.valueOf(14));
        assertThat(S124CodedValues.codeOf(RestrictionLabel.STOPPING_PROHIBITED)).isEqualTo(BigInteger.valueOf(25));
        assertThat(S124CodedValues.codeOf(RestrictionLabel.SPEED_RESTRICTED)).isEqualTo(BigInteger.valueOf(27));
    }

    @Test
    void otherEnumerationCodesMatchTheSpecification() {
        assertThat(S124CodedValues.codeOf(ReferenceCategoryLabel.WARNING_CANCELLATION)).isEqualTo(BigInteger.ONE);
        assertThat(S124CodedValues.codeOf(ReferenceCategoryLabel.IN_FORCE)).isEqualTo(BigInteger.valueOf(3));
        assertThat(S124CodedValues.codeOf(WarningTypeLabel.LOCAL_NAVIGATIONAL_WARNING)).isEqualTo(BigInteger.ONE);
        assertThat(S124CodedValues.codeOf(WarningTypeLabel.COASTAL_NAVIGATIONAL_WARNING)).isEqualTo(BigInteger.TWO);
        assertThat(S124CodedValues.codeOf(WarningTypeLabel.LOCAL_IN_FORCE_BULLETIN)).isEqualTo(BigInteger.valueOf(12));
        assertThat(S124CodedValues.codeOf(NameUsageLabel.DEFAULT_NAME_DISPLAY)).isEqualTo(BigInteger.ONE);
        assertThat(S124CodedValues.codeOf(QualityOfHorizontalMeasurementLabel.SURVEYED)).isEqualTo(BigInteger.ONE);
        assertThat(S124CodedValues.codeOf(QualityOfHorizontalMeasurementLabel.CALCULATED)).isEqualTo(BigInteger.valueOf(11));
    }

    /** Every label of every table has a code; no gaps, whatever the schema grows. */
    @Test
    void everyLabelHasACode() {
        for (NavwarnTypeGeneralLabel label : NavwarnTypeGeneralLabel.values()) {
            assertThat(S124CodedValues.codeOf(label)).as("code of %s", label).isNotNull();
        }
        for (RestrictionLabel label : RestrictionLabel.values()) {
            assertThat(S124CodedValues.codeOf(label)).as("code of %s", label).isNotNull();
        }
        for (WarningTypeLabel label : WarningTypeLabel.values()) {
            assertThat(S124CodedValues.codeOf(label)).as("code of %s", label).isNotNull();
        }
        for (ReferenceCategoryLabel label : ReferenceCategoryLabel.values()) {
            assertThat(S124CodedValues.codeOf(label)).as("code of %s", label).isNotNull();
        }
        for (NameUsageLabel label : NameUsageLabel.values()) {
            assertThat(S124CodedValues.codeOf(label)).as("code of %s", label).isNotNull();
        }
        for (QualityOfHorizontalMeasurementLabel label : QualityOfHorizontalMeasurementLabel.values()) {
            assertThat(S124CodedValues.codeOf(label)).as("code of %s", label).isNotNull();
        }
    }

    /**
     * The defect S-100 Part 10b, clause 10b-8.2.4, forbids: a label with no code. The reflective
     * walk has to reach the preamble's navwarnTypeGeneral and the warningType nested one level
     * further inside the messageSeriesIdentifier.
     */
    @Test
    void fillsMissingCodesThroughoutTheDataset() {
        Dataset dataset = S124TestDatasets.datasetWithPreamble();
        NavwarnPreamble preamble = S124TestDatasets.preambleOf(dataset);
        preamble.getNavwarnTypeGeneral().setCode(null);
        preamble.getMessageSeriesIdentifier().getWarningType().setCode(null);

        assertThat(S124CodedValues.fillMissingCodes(dataset)).isEqualTo(2);

        assertThat(preamble.getNavwarnTypeGeneral().getCode()).isEqualTo(BigInteger.valueOf(5));
        assertThat(preamble.getMessageSeriesIdentifier().getWarningType().getCode()).isEqualTo(BigInteger.TWO);
    }

    /** A code the producer set explicitly is never rewritten, even when it is wrong. */
    @Test
    void leavesAnExplicitCodeAloneAndReportsItWhenItContradictsTheLabel() {
        Dataset dataset = S124TestDatasets.datasetWithPreamble();
        NavwarnTypeGeneralType typeGeneral = S124TestDatasets.preambleOf(dataset).getNavwarnTypeGeneral();
        typeGeneral.setCode(BigInteger.valueOf(19));

        assertThat(S124CodedValues.fillMissingCodes(dataset)).isZero();
        assertThat(typeGeneral.getCode()).isEqualTo(BigInteger.valueOf(19));
        assertThat(S124CodedValues.codeMismatches(dataset))
                .singleElement().asString()
                .contains("navwarnTypeGeneral", "code 19", "Other Hazards", "code 5");
    }

    @Test
    void reportsNoMismatchWhenCodesAgreeWithTheirLabels() {
        assertThat(S124CodedValues.codeMismatches(S124TestDatasets.datasetWithPreamble())).isEmpty();
    }

    @Test
    void toleratesANullDataset() {
        assertThat(S124CodedValues.fillMissingCodes(null)).isZero();
        assertThat(S124CodedValues.codeMismatches(null)).isEmpty();
    }

    /** The walk must not choke on a dataset whose members list is absent. */
    @Test
    void toleratesADatasetWithoutMembers() {
        Dataset empty = new ObjectFactory().createDataset();
        empty.setId("EMPTY");
        assertThat(S124CodedValues.fillMissingCodes(empty)).isZero();
        assertThat(S124CodedValues.codeMismatches(empty)).isEmpty();
    }

    /**
     * Normalisation must add codes and nothing else. The generated impls create their backing list
     * on first call, and eleven of them back a list-valued XML *attribute* that way
     * ({@code @XmlAttribute List<String> nilReasons}); JAXB omits a null attribute list but marshals
     * a materialised empty one as {@code nilReason=""}. Merely walking those getters therefore used
     * to add attributes to the document - and those bytes are what the exchange set signs.
     */
    @Test
    void walkingTheDatasetDoesNotChangeTheMarshalledBytes() throws Exception {
        Dataset untouched = S124TestDatasets.datasetWithPreamble();
        S124TestDatasets.addPartWithTimeOfDay(untouched,
                S124TestDatasets.utcTime(6, 0), S124TestDatasets.utcTime(14, 0));
        Dataset walked = S124TestDatasets.datasetWithPreamble();
        S124TestDatasets.addPartWithTimeOfDay(walked,
                S124TestDatasets.utcTime(6, 0), S124TestDatasets.utcTime(14, 0));

        String before = S124Utils.marshalS124(untouched, true, false);
        S124CodedValues.fillMissingCodes(walked);
        String after = S124Utils.marshalS124(walked, true, false);

        assertThat(after).as("walking the dataset must not alter its XML").isEqualTo(before);
        assertThat(after).doesNotContain("nilReason=\"\"");
    }
}
