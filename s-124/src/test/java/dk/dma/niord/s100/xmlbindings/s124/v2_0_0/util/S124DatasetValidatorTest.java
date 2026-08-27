package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.Dataset;

/**
 * The S-124 rules that no XML schema can express, and which therefore have to be checked in code.
 * <p/>
 * Each is a "must" in the specification's own vocabulary (S-124 clause 1.4.1: "'Must' indicates a
 * mandatory requirement"), so each fails the marshal rather than being logged.
 */
class S124DatasetValidatorTest {

    @Test
    void acceptsAConformantDataset() {
        assertThat(S124DatasetValidator.violations(S124TestDatasets.datasetWithPreamble())).isEmpty();
        assertThatCode(() -> S124DatasetValidator.validate(S124TestDatasets.datasetWithPreamble()))
                .doesNotThrowAnyException();
    }

    /**
     * S-124 clause 4: "every compliant S-124 dataset must contain only one NavwarnPreamble". This
     * is the rule that silently corrupts output when broken - the discovery metadata derives one
     * temporal extent per dataset, so extra warnings' dates are simply dropped.
     */
    @Test
    void rejectsMoreThanOneNavwarnPreamble() {
        Dataset dataset = S124TestDatasets.datasetWithPreamble();
        S124TestDatasets.addSecondPreamble(dataset);

        assertThatThrownBy(() -> S124DatasetValidator.validate(dataset))
                .isInstanceOf(S124ConformanceException.class)
                .hasMessageContaining("2 NavwarnPreamble instances")
                .hasMessageContaining("S-124 clause 4");
    }

    /**
     * A dataset with no preamble is left alone: the library is equally the way a partial or
     * non-warning dataset is serialised, and the exchange set builder supports one deliberately.
     */
    @Test
    void acceptsADatasetWithNoPreamble() {
        Dataset dataset = S124TestDatasets.datasetWithPreamble();
        dataset.getMembers().getNavwarnPartsAndNavwarnAreaAffectedsAndTextPlacements().clear();

        assertThat(S124DatasetValidator.violations(dataset)).isEmpty();
    }

    /**
     * S-124 clause 4.3.3: agencyResponsibleForProduction "must be populated with a alpha code
     * value that corresponds with one of the valid values in the S-62 list". The failure that
     * actually happens is the agency's name written where its code belongs.
     */
    @Test
    void rejectsAnAgencyNameWhereAnS62CodeBelongs() {
        Dataset dataset = S124TestDatasets.datasetWithPreamble();
        S124TestDatasets.preambleOf(dataset).getMessageSeriesIdentifier()
                .setAgencyResponsibleForProduction("Danish Maritime Authority");

        assertThatThrownBy(() -> S124DatasetValidator.validate(dataset))
                .isInstanceOf(S124ConformanceException.class)
                .hasMessageContaining("Danish Maritime Authority")
                .hasMessageContaining("S-124 clause 4.3.3");
    }

    @Test
    void acceptsAnS62ProducerCode() {
        Dataset dataset = S124TestDatasets.datasetWithPreamble();
        S124TestDatasets.preambleOf(dataset).getMessageSeriesIdentifier()
                .setAgencyResponsibleForProduction("GB01");

        assertThat(S124DatasetValidator.violations(dataset)).isEmpty();
    }

    /**
     * S-124 clause 4.3.3: "Any instance of time ... such as timeOfDayEnd and timeOfDayStart in the
     * complex attribute fixedDateRange, must be populated with UTC time values." xs:time makes the
     * designator optional, so the schema cannot catch this.
     */
    @Test
    void rejectsATimeOfDayWithNoUtcDesignator() {
        Dataset dataset = S124TestDatasets.datasetWithPreamble();
        S124TestDatasets.addPartWithTimeOfDay(dataset,
                S124TestDatasets.floatingTime(8, 0), S124TestDatasets.floatingTime(16, 0));

        assertThatThrownBy(() -> S124DatasetValidator.validate(dataset))
                .isInstanceOf(S124ConformanceException.class)
                .hasMessageContaining("timeOfDayStart")
                .hasMessageContaining("timeOfDayEnd")
                .hasMessageContaining("no UTC designator");
    }

    /** A designator that is present but not UTC is just as wrong, and says so differently. */
    @Test
    void rejectsATimeOfDayAtANonUtcOffset() {
        Dataset dataset = S124TestDatasets.datasetWithPreamble();
        S124TestDatasets.addPartWithTimeOfDay(dataset,
                S124TestDatasets.offsetTime(8, 0, 120), null);

        assertThatThrownBy(() -> S124DatasetValidator.validate(dataset))
                .isInstanceOf(S124ConformanceException.class)
                .hasMessageContaining("offset +120 minutes from UTC");
    }

    @Test
    void acceptsUtcTimesOfDay() {
        Dataset dataset = S124TestDatasets.datasetWithPreamble();
        S124TestDatasets.addPartWithTimeOfDay(dataset,
                S124TestDatasets.utcTime(6, 0), S124TestDatasets.utcTime(14, 0));

        assertThat(S124DatasetValidator.violations(dataset)).isEmpty();
    }

    /** A code that contradicts its own label is a producer decision the library will not overrule. */
    @Test
    void rejectsACodeThatContradictsItsLabel() {
        Dataset dataset = S124TestDatasets.datasetWithPreamble();
        S124TestDatasets.preambleOf(dataset).getNavwarnTypeGeneral().setCode(BigInteger.valueOf(19));

        assertThatThrownBy(() -> S124DatasetValidator.validate(dataset))
                .isInstanceOf(S124ConformanceException.class)
                .hasMessageContaining("10b-8.2.4")
                .hasMessageContaining("navwarnTypeGeneral");
    }

    /** Every violation is reported at once, so a producer fixes the dataset in one pass. */
    @Test
    void reportsEveryViolationTogether() {
        Dataset dataset = S124TestDatasets.datasetWithPreamble();
        S124TestDatasets.addSecondPreamble(dataset);
        S124TestDatasets.preambleOf(dataset).getMessageSeriesIdentifier()
                .setAgencyResponsibleForProduction("Danish Maritime Authority");
        S124TestDatasets.addPartWithTimeOfDay(dataset, S124TestDatasets.floatingTime(8, 0), null);

        assertThatThrownBy(() -> S124DatasetValidator.validate(dataset))
                .isInstanceOf(S124ConformanceException.class)
                .satisfies(e -> assertThat(((S124ConformanceException) e).getViolations())
                        .extracting(S124DatasetValidator.Violation::clause)
                        .contains("S-124 clause 4 / clause 8.1.2", "S-124 clause 4.3.3",
                                "S-124 clause 4.3.3 / clause 6.2.2"));
    }

    @Test
    void toleratesANullDataset() {
        assertThat(S124DatasetValidator.violations(null)).isEmpty();
        assertThatCode(() -> S124DatasetValidator.validate(null)).doesNotThrowAnyException();
    }
}
