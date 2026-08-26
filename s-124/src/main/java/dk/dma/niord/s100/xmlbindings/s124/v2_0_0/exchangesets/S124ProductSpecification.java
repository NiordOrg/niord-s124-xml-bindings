package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.exchangesets;

import java.math.BigInteger;
import java.time.LocalDate;

import dk.dma.niord.s100.catalog._5_2.S100CompliancyCategory;
import dk.dma.niord.s100.catalog._5_2.S100ProductSpecification;

/**
 * Default S-100 product specification metadata for S-124 v2.0.0. Used by
 * {@link S124ExchangeSetFactory} when the caller does not provide a custom
 * {@link S100ProductSpecification}.
 */
public final class S124ProductSpecification {

    /** S-124 Ed 2.0.0 clause 12.2.2.4: "Must be Navigational Warnings". */
    public static final String NAME = "Navigational Warnings";
    /** S-124 Ed 2.0.0 clause 12.2.2.4: "Must be S-124". */
    public static final String IDENTIFIER = "S-124";
    public static final int NUMBER = 124;
    /**
     * The version of the product specification these bindings encode, from the
     * specification's own metadata (S-124 Ed 2.0.0 clause 1.6, "S-124 Version: 2.0.0").
     * <p/>
     * Clause 12.2.2.4 remarks "Must be 1.0.0" against the version attribute, but that
     * remark is a leftover of Edition 1.0.0 - the document carrying it is Edition 2.0.0
     * throughout - and encoding it would contradict the productEdition ("2.0.0") of the
     * dataset headers in the same exchange set, telling a consumer that datasets built
     * against Ed 2.0.0 were produced against a specification that predates the features
     * they use. Callers who must reproduce the literal table value can pass their own
     * {@link S100ProductSpecification} to
     * {@code S124ExchangeSetFactory.Builder#productSpecification}.
     */
    public static final String VERSION = "2.0.0";
    /**
     * S-124 Ed 2.0.0 clause 12.2.2.4 fixes the compliancyCategory of the
     * S100_ProductSpecification carried by the exchange catalogue: "Must be category 3"
     * (IHO S-100 compliant with standard encoding). The product specification's own
     * front matter (clause 1.6) states category4, but that describes the document, not
     * the value to encode in the catalogue, so the 12.2.2.4 profile value wins here.
     */
    public static final S100CompliancyCategory COMPLIANCY_CATEGORY = S100CompliancyCategory.CATEGORY_3;

    /**
     * S-124 Ed 2.0.0 clause 12.2.2.4 defines the date attribute as the "Publication date
     * of this document", which is March 2025 (clause 1.6 and the title page; the document
     * history dates the first operational edition to IHO CL 31/2025). The specification
     * names no day, and S100_ProductSpecification.date is a full date, so the first of the
     * publication month is encoded.
     */
    public static final LocalDate DATE = LocalDate.of(2025, 3, 1);

    private S124ProductSpecification() {
    }

    public static S100ProductSpecification defaultSpec() {
        S100ProductSpecification spec = new S100ProductSpecification();
        spec.setName(NAME);
        spec.setProductIdentifier(IDENTIFIER);
        spec.setNumber(BigInteger.valueOf(NUMBER));
        spec.setVersion(VERSION);
        spec.setCompliancyCategory(COMPLIANCY_CATEGORY);
        spec.setDate(DATE);
        return spec;
    }
}
