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

    public static final String NAME = "Navigational Warnings";
    public static final String IDENTIFIER = "S-124";
    public static final int NUMBER = 124;
    public static final String VERSION = "2.0.0";
    public static final S100CompliancyCategory COMPLIANCY_CATEGORY = S100CompliancyCategory.CATEGORY_1;
    public static final LocalDate DATE = LocalDate.of(2025, 7, 29);

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
