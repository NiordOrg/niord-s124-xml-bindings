/*
 * Copyright (c) 2024 GLA Research and Development Directorate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grad.eNav.s100.enums;

/**
 * This Enum implements the S-100 Security Classification codes.
 * <p/>
 * The S-100 Part 17 classification numbering (1..9) is retained through
 * {@link #getNumericCode()} for reference, but the value published as the
 * {@code codeListValue} attribute is the identifier of the corresponding
 * MD_ClassificationCode entry of the referenced ISO 19115-3 codelist
 * catalogue (e.g. {@code topSecret}), since S-100 Part 17 clause 17-4.9
 * requires the {@code codeListValue} to identify an entry of the codelist
 * named by the {@code codeList} attribute.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public enum SecurityClassification implements CodeListValueTypeProvider {
    UNCLASSIFIED(1, "unclassified", "unclassified"),
    RESTRICTED(2, "restricted", "restricted"),
    CONFIDENTIAL(3, "confidential", "confidential"),
    SECRET(4, "secret", "secret"),
    TOP_SECRET(5, "topSecret", "top secret"),
    SENSITIVE_BUT_UNCLASSIFIED(6, "SBU", "SBU"),
    FOR_OFFICIAL_USE_ONLY(7, "forOfficialUseOnly", "for official use only"),
    PROTECTED(8, "protected", "protected"),
    LIMITED_DISTRIBUTION(9, "limitedDistribution", "limited distribution");

    // Enum Variables
    private final int code;
    private final String codeListValue;
    private final String value;

    /**
     * Security Classification Constructor.
     *
     * @param code the S-100 Part 17 security classification code number
     * @param codeListValue the MD_ClassificationCode codelist entry identifier
     * @param value the security classification value
     */
    SecurityClassification(int code, String codeListValue, String value) {
        this.code = code;
        this.codeListValue = codeListValue;
        this.value = value;
    }

    /**
     * Gets code.
     * <p/>
     * This is the identifier of the MD_ClassificationCode entry in the
     * referenced codelist catalogue, which is what the S-100 encoding
     * requires in the {@code codeListValue} attribute.
     *
     * @return the code
     */
    @Override
    public String getCode() {
        return codeListValue;
    }

    /**
     * Gets the S-100 Part 17 numeric code of the classification (1..9), as
     * listed in the S100_DatasetDiscoveryMetadata classification remarks.
     * <p/>
     * Note that this number is not a codelist entry identifier and must not
     * be encoded as the {@code codeListValue} attribute.
     *
     * @return the numeric code
     */
    public int getNumericCode() {
        return code;
    }

    /**
     * Gets value.
     *
     * @return the value
     */
    @Override
    public String getValue() {
        return value;
    }

    /**
     * Gets the list of the code list.
     *
     * @return the list of the code list
     */
    @Override
    public String getList() {
        return CODELIST;
    }

    /**
     * Gets the code space of the code list.
     *
     * @return the code space of the code list
     */
    @Override
    public String getSpace() {
        return CODESPACE;
    }

    /**
     * The Enum Codespace.
     */
    public static final String CODESPACE = "https://schemas.isotc211.org/19115/-3/mco/1.0";

    /**
     * The Enum Codelist.
     */
    public static final String CODELIST = "https://standards.iso.org/iso/19115/resources/Codelists/cat/codelists.xml";
}

