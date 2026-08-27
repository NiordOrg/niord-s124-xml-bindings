package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util;

import java.util.List;

/**
 * Thrown when a dataset breaks a rule of S-124 Ed 2.0.0 or S-100 Ed 5.2.0 that the GML application
 * schema cannot express, so the document would marshal as schema-valid but specification-invalid.
 * <p/>
 * S-124 clause 8.1.1 makes the distinction explicit: "Feature instances must validate against the
 * schema and conform to all other requirements specified in this data product specification
 * including all constraints not captured in the XML Schema document." {@link S124XsdValidator}
 * covers the first half; {@link S124DatasetValidator}, which raises this exception, covers the
 * constraints of the second.
 * <p/>
 * All violations found are reported together rather than one per build-and-fail cycle, so
 * {@link #getViolations()} lists everything the validator objected to and {@link #getMessage()}
 * renders the same list.
 */
public class S124ConformanceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient List<S124DatasetValidator.Violation> violations;

    public S124ConformanceException(String message, List<S124DatasetValidator.Violation> violations) {
        super(message);
        this.violations = List.copyOf(violations);
    }

    /** Every rule the dataset broke, in the order the validator checked them. */
    public List<S124DatasetValidator.Violation> getViolations() {
        return violations;
    }
}
