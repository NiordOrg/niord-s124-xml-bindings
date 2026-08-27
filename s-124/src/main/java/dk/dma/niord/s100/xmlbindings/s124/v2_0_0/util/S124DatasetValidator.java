package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.DataSetIdentificationType;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.DatasetPurposeType;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.ReferenceType;
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
        checkApplicationProfile(dataset, violations);
        checkWalkedRules(dataset, violations);
        for (String mismatch : S124CodedValues.codeMismatches(dataset)) {
            violations.add(new Violation("S-100 Part 10b, clause 10b-8.2.4", mismatch));
        }
        return violations;
    }

    /**
     * S-100 Part 10b, Table 10b-4, gives {@code applicationProfile} exactly two values - "1" for
     * base datasets and "2" for update datasets - and pairs each with a {@code datasetPurpose}.
     * <p/>
     * The XSD types the element as a plain {@code CharacterString}, so anything at all passes
     * schema validation; every generated example carried the descriptive
     * {@code "NavigationalWarning"}, which names no profile the standard defines. The two elements
     * are one fact written twice, so a header combining profile "2" with purpose "base"
     * contradicts itself - and that, unlike the value itself, no reader can resolve.
     */
    private static void checkApplicationProfile(Dataset dataset, List<Violation> violations) {
        DataSetIdentificationType identification = dataset.getDatasetIdentificationInformation();
        if (identification == null) {
            return;
        }
        String profile = identification.getApplicationProfile();
        DatasetPurposeType purpose = identification.getDatasetPurpose();
        if (profile == null) {
            // Absence is a schema matter - Table 10b-4 gives it multiplicity 1 and the XSD
            // enforces that - so S124XsdValidator reports it with better context than this could.
            return;
        }
        if (!S124DatasetInfo.BASE_APPLICATION_PROFILE.equals(profile)
                && !S124DatasetInfo.UPDATE_APPLICATION_PROFILE.equals(profile)) {
            violations.add(new Violation("S-100 Part 10b, Table 10b-4", String.format(
                    "applicationProfile is \"%s\"; S-100 Part 10b Table 10b-4 defines only \"%s\" "
                            + "(base datasets) and \"%s\" (update datasets)",
                    profile, S124DatasetInfo.BASE_APPLICATION_PROFILE,
                    S124DatasetInfo.UPDATE_APPLICATION_PROFILE)));
            return;
        }
        DatasetPurposeType implied = S124DatasetInfo.BASE_APPLICATION_PROFILE.equals(profile)
                ? DatasetPurposeType.BASE
                : DatasetPurposeType.UPDATE;
        if (purpose != null && purpose != implied) {
            violations.add(new Violation("S-100 Part 10b, Table 10b-4", String.format(
                    "applicationProfile \"%s\" stands for a %s dataset but datasetPurpose is %s; "
                            + "Table 10b-4 pairs the two, so the header contradicts itself",
                    profile, implied.value(), purpose.value())));
        }
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
     * <p/>
     * Exactly one, so a dataset carrying none is rejected too. Clause 8.1.2 admits no dataset type
     * that lacks a preamble, and the preamble is where a warning states what it is - without one
     * the discovery metadata has no temporal extent, no place name and no series identifier to
     * describe. A half-built dataset can still be serialised by turning validation off.
     */
    private static void checkSinglePreamble(Dataset dataset, List<Violation> violations) {
        long preambles = 0;
        for (Object member : membersOf(dataset)) {
            if (member instanceof NavwarnPreamble) {
                preambles++;
            }
        }
        if (preambles == 1) {
            return;
        }
        if (preambles == 0) {
            violations.add(new Violation("S-124 clause 4 / clause 8.1.2",
                    "the dataset carries no NavwarnPreamble; S-124 clause 4 requires every "
                            + "compliant dataset to contain exactly one, and clause 8.1.2 admits "
                            + "no dataset type without one"));
            return;
        }
        violations.add(new Violation("S-124 clause 4 / clause 8.1.2", String.format(
                "the dataset carries %d NavwarnPreamble instances, but S-124 allows only one "
                        + "navigational warning per dataset; split the warnings into one dataset "
                        + "each, or encode an in-force bulletin as a single preamble whose "
                        + "References instance has referenceCategory 3 (in-force)",
                preambles)));
    }

    /**
     * The property names under which S-124 and the S-100 GML profile encode a feature or
     * information association, all typed {@code gml:ReferenceType}.
     * <p/>
     * Listed rather than matched by type, because the same type also encodes
     * {@code maskReference} inside {@code S100_SpatialAttributeType} - a spatial mask, not an
     * association, and so outside clause 10b-9. The S-124 associations are {@code theWarning},
     * {@code theReferences}, {@code header}, {@code affects}, {@code thePositionProvider},
     * {@code impacts} and {@code theCartographicText} (124_2.0.0.xsd); {@code informationAssociation}
     * is the S-100 GML profile's own, carried by every geometry type. Names are the JAXB property
     * names, so repeated elements appear in their plural form.
     */
    private static final Set<String> ASSOCIATION_ROLES = Set.of(
            "theWarning", "theReferences", "header", "affects", "thePositionProviders",
            "impacts", "theCartographicText", "informationAssociations");

    /** The rules that apply to an attribute wherever in the dataset it occurs. */
    private static void checkWalkedRules(Dataset dataset, List<Violation> violations) {
        BindingWalk.forEachProperty(dataset, (property, node) -> {
            if (node instanceof MessageSeriesIdentifierType series) {
                checkAgency(series.getAgencyResponsibleForProduction(), violations);
            } else if (node instanceof FixedDateRangeType range) {
                checkUtc("timeOfDayStart", range.getTimeOfDayStart(), violations);
                checkUtc("timeOfDayEnd", range.getTimeOfDayEnd(), violations);
            } else if (node instanceof ReferenceType reference && ASSOCIATION_ROLES.contains(property)) {
                checkAssociation(property, reference, violations);
            }
        });
    }

    /**
     * S-100 Ed 5.2.0 Part 10b, clause 10b-9: "Feature and information associations must encode at
     * least one of the role or arcrole attributes of the reference."
     * <p/>
     * The rule is not decoration. Clause 10b-10 item 3 makes those attributes the way a processor
     * tells an association apart from an attribute at all: "If X2 has XML attributes xlink:href and
     * xlink:role and/or xlink:arcrole it is an association role." A reference carrying only
     * {@code xlink:href} is therefore not merely terse - a generic S-100 reader cannot classify it.
     * <p/>
     * S-124 nowhere defines role or arcrole values, so the producer chooses them; the library
     * cannot fill them in the way it fills an enumeration code, because there is no listed value to
     * derive. Which elements are associations is decided by {@link #ASSOCIATION_ROLES}, not by the
     * type, so that {@code maskReference} - the same type in a non-association position - is left
     * alone.
     * <p/>
     * Every association that is present is checked, whether or not it carries an {@code href}: the
     * clause conditions on the reference existing, not on it resolving, and an association element
     * with no target is in any case not something a producer meant to write.
     */
    private static void checkAssociation(String property, ReferenceType reference,
            List<Violation> violations) {
        boolean hasRole = reference.getRole() != null && !reference.getRole().isBlank();
        boolean hasArcrole = reference.getArcrole() != null && !reference.getArcrole().isBlank();
        if (hasRole || hasArcrole) {
            return;
        }
        String target = reference.getHref() == null || reference.getHref().isBlank()
                ? "nothing"
                : "\"" + reference.getHref() + "\"";
        violations.add(new Violation("S-100 Part 10b, clause 10b-9", String.format(
                "the %s association, referencing %s, carries neither xlink:role nor xlink:arcrole, "
                        + "so a reader cannot tell it is an association role rather than an "
                        + "attribute (clause 10b-10 item 3); set one of them on the reference",
                property, target)));
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
