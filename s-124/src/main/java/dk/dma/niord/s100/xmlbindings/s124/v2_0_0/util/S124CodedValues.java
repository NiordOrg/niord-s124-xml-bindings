package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.Dataset;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NameUsageLabel;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NameUsageType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnTypeGeneralLabel;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnTypeGeneralType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.QualityOfHorizontalMeasurementLabel;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.QualityOfHorizontalMeasurementType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.ReferenceCategoryLabel;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.ReferenceCategoryType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.RestrictionLabel;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.RestrictionType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.WarningTypeLabel;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.WarningTypeType;

/**
 * The numeric codes of the S-124 enumerations and closed code lists, and the normalisation that
 * puts them on a dataset that carries only labels.
 * <p/>
 * S-100 Ed 5.2.0 Part 10b, clause 10b-8.2.4: "For S-100 enumeration or S-100 codelist attributes,
 * datasets must use the code and label of the listed value as encoded in the Feature Catalogue."
 * S-124 Ed 2.0.0, clause 4.3.1, says the same of its own attributes: "Enumeration and code list
 * values are represented in the data by their numerical value and label."
 * <p/>
 * The rule is enforceable only in prose. The S-124 GML application schema types every {@code code}
 * attribute as optional - {@code <xs:attribute name="code" type="navwarnTypeGeneralCode"/>} carries
 * no {@code use="required"} - because Part 10b, clause 10b-8.2.4, deliberately builds enumerations
 * as "a combination of restricted simple types for the code and label" and leaves the pairing to
 * the producer. A label-only element is therefore schema-valid and specification-invalid, which is
 * exactly the class of defect {@link S124XsdValidator} cannot see.
 * <p/>
 * Because the code is a pure function of the label, this class fills it in rather than rejecting
 * the dataset: a producer that selected a label has already made the only choice there is to make,
 * and failing the marshal would force every caller to restate a mapping the schema already fixes.
 * A code that is present but disagrees with its label is a different matter - that is a genuine
 * contradiction only the producer can resolve - and is reported by {@link S124DatasetValidator}.
 *
 * <h2>Where the codes come from</h2>
 * Each table below pairs the generated {@code *Label} enum with the {@code *Code} simple type of
 * {@code /xsd/124_2.0.0.xsd} positionally, which is how the schema declares them: JAXB preserves
 * the schema's enumeration order in {@code values()}, and the two restrictions list the same values
 * in the same order. The pairing is not merely assumed - it reproduces the code values printed in
 * the S-124 Ed 2.0.0 UML (clause 4.3.1, Figures 4-3 and 4-4), including the non-sequential
 * {@code restriction} codes, which is why {@link #RESTRICTION_CODES} is spelled out rather than
 * generated.
 */
public final class S124CodedValues {

    /**
     * {@code restriction} is the one S-124 enumeration whose codes are not {@code 1..n}: the values
     * are drawn from the wider S-100 restriction register, so the product specification keeps the
     * register's numbering. S-124 clause 4.3.1, Figure 4-3: "Entry Prohibited = 7", "Entry Restricted = 8",
     * "Area To Be Avoided = 14", "Stopping Prohibited = 25", "Speed Restricted = 27".
     */
    private static final int[] RESTRICTION_CODES = {7, 8, 14, 25, 27};

    private static final Map<NavwarnTypeGeneralLabel, BigInteger> NAVWARN_TYPE_GENERAL =
            sequential(NavwarnTypeGeneralLabel.class);
    private static final Map<ReferenceCategoryLabel, BigInteger> REFERENCE_CATEGORY =
            sequential(ReferenceCategoryLabel.class);
    private static final Map<WarningTypeLabel, BigInteger> WARNING_TYPE =
            sequential(WarningTypeLabel.class);
    private static final Map<NameUsageLabel, BigInteger> NAME_USAGE =
            sequential(NameUsageLabel.class);
    private static final Map<QualityOfHorizontalMeasurementLabel, BigInteger> QUALITY_OF_HORIZONTAL_MEASUREMENT =
            sequential(QualityOfHorizontalMeasurementLabel.class);
    private static final Map<RestrictionLabel, BigInteger> RESTRICTION =
            explicit(RestrictionLabel.class, RESTRICTION_CODES);

    private S124CodedValues() {
    }

    /** The code S-124 pairs with the label, never {@code null} for a schema-valid label. */
    public static BigInteger codeOf(NavwarnTypeGeneralLabel label) { return NAVWARN_TYPE_GENERAL.get(label); }

    /** The code S-124 pairs with the label, never {@code null} for a schema-valid label. */
    public static BigInteger codeOf(ReferenceCategoryLabel label) { return REFERENCE_CATEGORY.get(label); }

    /** The code S-124 pairs with the label, never {@code null} for a schema-valid label. */
    public static BigInteger codeOf(WarningTypeLabel label) { return WARNING_TYPE.get(label); }

    /** The code S-124 pairs with the label, never {@code null} for a schema-valid label. */
    public static BigInteger codeOf(NameUsageLabel label) { return NAME_USAGE.get(label); }

    /** The code S-124 pairs with the label, never {@code null} for a schema-valid label. */
    public static BigInteger codeOf(QualityOfHorizontalMeasurementLabel label) {
        return QUALITY_OF_HORIZONTAL_MEASUREMENT.get(label);
    }

    /** The code S-124 pairs with the label, never {@code null} for a schema-valid label. */
    public static BigInteger codeOf(RestrictionLabel label) { return RESTRICTION.get(label); }

    /**
     * Sets the numeric code of every coded element of the dataset that carries a label but no
     * code, so the marshalled document satisfies S-100 Part 10b, clause 10b-8.2.4.
     * <p/>
     * Elements that already carry a code are left untouched, including ones whose code contradicts
     * their label: silently rewriting a producer's explicit value would hide the disagreement
     * instead of surfacing it. {@link S124DatasetValidator#validate} reports those.
     * <p/>
     * {@code navwarnTypeDetails} is deliberately not covered. S-124 declares it an open
     * {@code S100_CodeList} whose value is a free {@code String} rather than an enumeration, so
     * there is no closed set of labels to derive a code from.
     *
     * @param dataset the dataset to normalise in place; {@code null} is a no-op
     * @return the number of codes filled in
     */
    public static int fillMissingCodes(Dataset dataset) {
        if (dataset == null) {
            return 0;
        }
        int[] filled = {0};
        BindingWalk.forEach(dataset, node -> {
            if (node instanceof NavwarnTypeGeneralType e && e.getCode() == null && e.getValue() != null) {
                e.setCode(codeOf(e.getValue()));
                filled[0]++;
            } else if (node instanceof ReferenceCategoryType e && e.getCode() == null && e.getValue() != null) {
                e.setCode(codeOf(e.getValue()));
                filled[0]++;
            } else if (node instanceof WarningTypeType e && e.getCode() == null && e.getValue() != null) {
                e.setCode(codeOf(e.getValue()));
                filled[0]++;
            } else if (node instanceof NameUsageType e && e.getCode() == null && e.getValue() != null) {
                e.setCode(codeOf(e.getValue()));
                filled[0]++;
            } else if (node instanceof QualityOfHorizontalMeasurementType e && e.getCode() == null && e.getValue() != null) {
                e.setCode(codeOf(e.getValue()));
                filled[0]++;
            } else if (node instanceof RestrictionType e && e.getCode() == null && e.getValue() != null) {
                e.setCode(codeOf(e.getValue()));
                filled[0]++;
            }
        });
        return filled[0];
    }

    /**
     * The coded elements of the dataset whose code contradicts the code S-124 pairs with their
     * label, described one per entry. Empty for a conformant dataset.
     */
    public static List<String> codeMismatches(Dataset dataset) {
        if (dataset == null) {
            return List.of();
        }
        List<String> mismatches = new ArrayList<>();
        BindingWalk.forEach(dataset, node -> {
            if (node instanceof NavwarnTypeGeneralType e) {
                check("navwarnTypeGeneral", e.getValue() == null ? null : e.getValue().value(),
                        codeOf(e.getValue()), e.getCode(), mismatches);
            } else if (node instanceof ReferenceCategoryType e) {
                check("referenceCategory", e.getValue() == null ? null : e.getValue().value(),
                        codeOf(e.getValue()), e.getCode(), mismatches);
            } else if (node instanceof WarningTypeType e) {
                check("warningType", e.getValue() == null ? null : e.getValue().value(),
                        codeOf(e.getValue()), e.getCode(), mismatches);
            } else if (node instanceof NameUsageType e) {
                check("nameUsage", e.getValue() == null ? null : e.getValue().value(),
                        codeOf(e.getValue()), e.getCode(), mismatches);
            } else if (node instanceof QualityOfHorizontalMeasurementType e) {
                check("qualityOfHorizontalMeasurement", e.getValue() == null ? null : e.getValue().value(),
                        codeOf(e.getValue()), e.getCode(), mismatches);
            } else if (node instanceof RestrictionType e) {
                check("restriction", e.getValue() == null ? null : e.getValue().value(),
                        codeOf(e.getValue()), e.getCode(), mismatches);
            }
        });
        return mismatches;
    }

    private static void check(String element, String label, BigInteger expected, BigInteger actual,
            List<String> mismatches) {
        if (label == null || actual == null || expected == null || expected.equals(actual)) {
            return;
        }
        mismatches.add(String.format(
                "%s carries code %s but S-124 pairs the label \"%s\" with code %s",
                element, actual, label, expected));
    }

    /** Pairs the labels with codes {@code 1..n}, the numbering S-124 gives most of its enumerations. */
    private static <E extends Enum<E>> Map<E, BigInteger> sequential(Class<E> labels) {
        E[] values = labels.getEnumConstants();
        int[] codes = new int[values.length];
        for (int i = 0; i < codes.length; i++) {
            codes[i] = i + 1;
        }
        return explicit(labels, codes);
    }

    /** Pairs the labels positionally with the codes of the matching {@code *Code} simple type. */
    private static <E extends Enum<E>> Map<E, BigInteger> explicit(Class<E> labels, int[] codes) {
        E[] values = labels.getEnumConstants();
        if (values.length != codes.length) {
            // The bindings have been regenerated from a schema whose label and code restrictions no
            // longer line up, so the positional pairing this class rests on no longer holds.
            throw new IllegalStateException(String.format(
                    "%s declares %d labels but %d codes are configured; the S-124 label/code tables "
                            + "in S124CodedValues need updating for the current schema",
                    labels.getSimpleName(), values.length, codes.length));
        }
        Map<E, BigInteger> map = new EnumMap<>(labels);
        for (int i = 0; i < values.length; i++) {
            map.put(values[i], BigInteger.valueOf(codes[i]));
        }
        return Collections.unmodifiableMap(map);
    }
}
