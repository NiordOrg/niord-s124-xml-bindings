package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.Dataset;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.FixedDateRangeType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.MessageSeriesIdentifierType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnPreamble;

/**
 * Checks a dataset against the S-124 rules the GML application schema cannot express.
 * <p/>
 * S-124 Ed 2.0.0, clause 8.1.1, requires both halves of conformance: "Feature instances must
 * validate against the schema <em>and conform to all other requirements specified in this data
 * product specification including all constraints not captured in the XML Schema document</em>."
 * {@link S124XsdValidator} covers the schema; this class covers constraints of the second kind that
 * are decidable from the dataset alone.
 * <p/>
 * Every rule checked here is a "must" in the specification's own vocabulary - S-124 clause 1.4.1:
 * "'Must' indicates a mandatory requirement" - so a violation fails the marshal rather than being
 * logged. Violations are collected rather than thrown one at a time, so a producer fixing a dataset
 * sees the whole list at once. Rules that need more than the dataset - the agreement between the
 * catalogue's {@code datasetID} and the preamble's {@code interoperabilityIdentifier}, for instance -
 * belong to the exchange set builder, which is the only scope holding both artefacts.
 *
 * <h2>What is deliberately not checked</h2>
 * <ul>
 *   <li><strong>A dataset with no {@code NavwarnPreamble}.</strong> S-124 clause 4 says "every
 *       compliant S-124 dataset must contain only one NavwarnPreamble", which a dataset carrying
 *       none also breaks. It is not rejected here because this library is equally the way a partial
 *       or non-warning dataset is serialised, and the exchange set builder deliberately supports a
 *       dataset without a preamble (it simply carries no temporal extent). Carrying <em>more</em>
 *       than one is the case that silently corrupts output, and that is rejected.</li>
 *   <li><strong>{@code navwarnTypeDetails} codes.</strong> S-124 declares it an open
 *       {@code S100_CodeList} over a free string, so no code can be derived or required.</li>
 *   <li><strong>Whether {@code agencyResponsibleForProduction} is a <em>registered</em> S-62
 *       code.</strong> See {@link #PRODUCER_CODE}.</li>
 * </ul>
 */
public final class S124DatasetValidator {

    /**
     * The shape of an S-62 producer code.
     * <p/>
     * S-124 clause 4.3.3: "The agencyResponsibleForProduction attribute ... must be populated with
     * a alpha code value that corresponds with one of the valid values in the S-62 list of S-100
     * codes found in the Producer Code Register of the IHO GI Registry." S-100 Part 17, clause
     * 17-4.3, uses the same code as the {@code YYYY} field of a dataset file name, four characters
     * wide.
     * <p/>
     * The register itself is online and versioned independently of this library, so bundling a
     * snapshot of it would go stale and start rejecting newly registered producers. The check is
     * therefore a shape guard: it catches the failure that actually occurs - the agency's
     * <em>name</em> written where its code belongs, as in {@code "Danish Maritime Authority"} - and
     * leaves membership of the register to a validator with network access.
     */
    private static final Pattern PRODUCER_CODE = Pattern.compile("[A-Za-z0-9]{1,4}");

    private S124DatasetValidator() {
    }

    /**
     * Throws unless the dataset conforms to the rules of {@link #violations(Dataset)}.
     *
     * @param dataset the dataset to check
     * @throws S124ConformanceException listing every rule the dataset broke
     */
    public static void validate(Dataset dataset) {
        List<Violation> violations = violations(dataset);
        if (violations.isEmpty()) {
            return;
        }
        String detail = violations.stream()
                .map(v -> String.format("%n  - [%s] %s", v.clause(), v.message()))
                .collect(Collectors.joining());
        throw new S124ConformanceException(String.format(
                "The S-124 dataset breaks %d rule%s of the product specification that the GML schema "
                        + "cannot express:%s",
                violations.size(), violations.size() == 1 ? "" : "s", detail),
                violations);
    }

    /** Every rule the dataset breaks, empty when it conforms. */
    public static List<Violation> violations(Dataset dataset) {
        if (dataset == null) {
            return List.of();
        }
        List<Violation> violations = new ArrayList<>();
        checkSinglePreamble(dataset, violations);
        checkAgencyAndTimes(dataset, violations);
        for (String mismatch : S124CodedValues.codeMismatches(dataset)) {
            violations.add(new Violation("S-100 Part 10b, clause 10b-8.2.4", mismatch));
        }
        return violations;
    }

    /**
     * S-124 clause 4: "A general principle of one navigational warning per dataset applies
     * throughout ... every compliant S-124 dataset must contain only one NavwarnPreamble", restated
     * by clause 8.1.2: "a dataset must contain only one Navigational Warning or In-force Bulletin".
     * <p/>
     * The rule is unconditional. An in-force bulletin is not an exception to it - Table 8-1 says the
     * bulletin "will include only one NavwarnPreamble instance and must include one References
     * instance with referenceCategory set to 3 (in-force)" - so a bulletin is one preamble that
     * references many warnings, not many preambles in one dataset.
     * <p/>
     * Beyond conformance, more than one preamble silently corrupts the exchange set: the discovery
     * metadata derives one temporal extent per dataset from the preamble, so the extra warnings'
     * publication and cancellation dates are dropped.
     */
    private static void checkSinglePreamble(Dataset dataset, List<Violation> violations) {
        long preambles = 0;
        for (Object member : membersOf(dataset)) {
            if (member instanceof NavwarnPreamble) {
                preambles++;
            }
        }
        if (preambles > 1) {
            violations.add(new Violation("S-124 clause 4 / clause 8.1.2", String.format(
                    "the dataset carries %d NavwarnPreamble instances, but S-124 allows only one "
                            + "navigational warning per dataset; split the warnings into one dataset "
                            + "each, or encode an in-force bulletin as a single preamble whose "
                            + "References instance has referenceCategory 3 (in-force)",
                    preambles)));
        }
    }

    /** The two attribute-level "must" rules of S-124 clause 4.3.3, wherever they occur. */
    private static void checkAgencyAndTimes(Dataset dataset, List<Violation> violations) {
        BindingWalk.forEach(dataset, node -> {
            if (node instanceof MessageSeriesIdentifierType series) {
                checkAgency(series.getAgencyResponsibleForProduction(), violations);
            } else if (node instanceof FixedDateRangeType range) {
                checkUtc("timeOfDayStart", range.getTimeOfDayStart(), violations);
                checkUtc("timeOfDayEnd", range.getTimeOfDayEnd(), violations);
            }
        });
    }

    private static void checkAgency(String agency, List<Violation> violations) {
        if (agency == null || PRODUCER_CODE.matcher(agency).matches()) {
            return;
        }
        violations.add(new Violation("S-124 clause 4.3.3", String.format(
                "agencyResponsibleForProduction is \"%s\", which is not an S-62 producer code; the "
                        + "attribute takes the agency's code from the IHO GI Registry Producer Code "
                        + "Register (\"DK00\"), not its name",
                agency)));
    }

    /**
     * S-124 clause 4.3.3: "Any instance of time, either in text or in attributes, such as
     * timeOfDayEnd and timeOfDayStart in the complex attribute fixedDateRange, must be populated
     * with UTC time values", restated by clause 6.2.2: "All instances of time in datasets
     * conforming to S-124 must be expressed in UTC."
     * <p/>
     * The schema types both attributes {@code xs:time}, whose lexical space makes the timezone
     * designator optional, so {@code 08:00:00} is schema-valid and says nothing about which
     * timezone it is in. A reader has no way to recover the producer's intent, which is exactly
     * what the clause forbids.
     */
    private static void checkUtc(String attribute, XMLGregorianCalendar time, List<Violation> violations) {
        if (time == null) {
            return;
        }
        int offsetMinutes = time.getTimezone();
        if (offsetMinutes == DatatypeConstants.FIELD_UNDEFINED) {
            violations.add(new Violation("S-124 clause 4.3.3 / clause 6.2.2", String.format(
                    "%s is \"%s\", which carries no UTC designator, so the time it denotes is "
                            + "undefined; S-124 times must be UTC, encoded with a trailing \"Z\"",
                    attribute, time)));
        } else if (offsetMinutes != 0) {
            violations.add(new Violation("S-124 clause 4.3.3 / clause 6.2.2", String.format(
                    "%s is \"%s\", which is offset %+d minutes from UTC; S-124 times must be "
                            + "expressed in UTC",
                    attribute, time, offsetMinutes)));
        }
    }

    private static List<?> membersOf(Dataset dataset) {
        if (dataset.getMembers() == null) {
            return List.of();
        }
        List<?> members = dataset.getMembers().getNavwarnPartsAndNavwarnAreaAffectedsAndTextPlacements();
        return members == null ? List.of() : members;
    }

    /**
     * One broken rule: the clause that states it and what the dataset did instead.
     *
     * @param clause  the specification clause the rule comes from
     * @param message what the dataset does, and what it should do instead
     */
    public record Violation(String clause, String message) {
        @Override
        public String toString() {
            return "[" + clause + "] " + message;
        }
    }
}
