package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.ReferenceType;
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
     * Exactly one, so none is as wrong as two: clause 8.1.2 admits no S-124 dataset type without a
     * preamble, and without one there is no warning to describe.
     */
    @Test
    void rejectsADatasetWithNoNavwarnPreamble() {
        Dataset dataset = S124TestDatasets.datasetWithPreamble();
        dataset.getMembers().getNavwarnPartsAndNavwarnAreaAffectedsAndTextPlacements().clear();

        assertThatThrownBy(() -> S124DatasetValidator.validate(dataset))
                .isInstanceOf(S124ConformanceException.class)
                .hasMessageContaining("carries no NavwarnPreamble")
                .hasMessageContaining("S-124 clause 4");
    }

    /**
     * S-100 Part 10b clause 10b-9: "Feature and information associations must encode at least one
     * of the role or arcrole attributes of the reference" - without it, clause 10b-10 item 3 says a
     * reader cannot tell an association role from an attribute.
     */
    @Test
    void rejectsAnAssociationWithNeitherRoleNorArcrole() {
        Dataset dataset = S124TestDatasets.datasetWithPreamble();
        S124TestDatasets.addPartWithTimeOfDay(dataset, null, null);
        S124TestDatasets.headerOfFirstPart(dataset).setRole(null);

        assertThatThrownBy(() -> S124DatasetValidator.validate(dataset))
                .isInstanceOf(S124ConformanceException.class)
                .hasMessageContaining("neither xlink:role nor xlink:arcrole")
                .hasMessageContaining("10b-9");
    }

    /** An arcrole alone satisfies the clause; it asks for at least one of the two. */
    @Test
    void acceptsAnAssociationCarryingOnlyArcrole() {
        Dataset dataset = S124TestDatasets.datasetWithPreamble();
        S124TestDatasets.addPartWithTimeOfDay(dataset, null, null);
        ReferenceType header = S124TestDatasets.headerOfFirstPart(dataset);
        header.setRole(null);
        header.setArcrole("http://www.iho.int/S124/gml/2.0/arcroles/header");

        assertThat(S124DatasetValidator.violations(dataset)).isEmpty();
    }

    /**
     * {@code maskReference} is a {@code gml:ReferenceType} too, but it lives in
     * {@code S100_SpatialAttributeType} and is a spatial mask, not a feature or information
     * association - clause 10b-9 does not reach it, so a role must not be demanded there.
     */
    @Test
    void doesNotDemandARoleOnASpatialMaskReference() {
        Dataset dataset = S124TestDatasets.datasetWithPreamble();
        S124TestDatasets.addPartWithMaskReference(dataset);

        assertThat(S124DatasetValidator.violations(dataset)).isEmpty();
    }

    /**
     * S-100 Part 10b Table 10b-4 defines only "1" (base) and "2" (update). Every generated example
     * carried the descriptive "NavigationalWarning", which names no profile the standard defines.
     */
    @Test
    void rejectsAnApplicationProfileTable10b4DoesNotDefine() {
        Dataset dataset = S124TestDatasets.datasetWithPreamble();
        dataset.getDatasetIdentificationInformation().setApplicationProfile("NavigationalWarning");

        assertThatThrownBy(() -> S124DatasetValidator.validate(dataset))
                .isInstanceOf(S124ConformanceException.class)
                .hasMessageContaining("NavigationalWarning")
                .hasMessageContaining("Table 10b-4");
    }

    /** The profile and the purpose are one fact written twice, so they cannot disagree. */
    @Test
    void rejectsAnApplicationProfileThatContradictsTheDatasetPurpose() {
        Dataset dataset = S124TestDatasets.datasetWithPreamble();
        dataset.getDatasetIdentificationInformation()
                .setApplicationProfile(S124DatasetInfo.UPDATE_APPLICATION_PROFILE);

        assertThatThrownBy(() -> S124DatasetValidator.validate(dataset))
                .isInstanceOf(S124ConformanceException.class)
                .hasMessageContaining("contradicts itself");
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
