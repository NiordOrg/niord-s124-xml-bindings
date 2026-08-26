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

import org.iso.standards.iso._19115.__3.gco._1.CodeListValueType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that the code list enums publish values that can actually be resolved
 * against the ISO 19115-3 code list catalogue their codeList attribute points
 * to. S-100 Part 17 clause 17-4.9 requires the codeListValue attributes to
 * identify entries of that catalogue, whose checked-in authoritative copy is
 * used here as the reference.
 */
class CodeListValueTypeProviderTest {

    // The ISO 19115-3 code list catalogue namespaces
    private static final String CAT_NAMESPACE = "http://standards.iso.org/iso/19115/-3/cat/1.0";
    private static final String GCO_NAMESPACE = "http://standards.iso.org/iso/19115/-3/gco/1.0";

    // Test Variables
    private static Document codelists;

    /**
     * Loads the checked-in ISO 19115-3 code list catalogue from the
     * filesystem, since it is a schema resource rather than a classpath one.
     *
     * @throws Exception if the catalogue cannot be read or parsed
     */
    @BeforeAll
    static void loadCodelistCatalogue() throws Exception {
        Path catalogue = Path.of("src/main/schemas.isotc211.org/19115/resources/Codelist/cat/codelists.xml");
        if (!Files.exists(catalogue)) {
            // running with the repository root as working directory (e.g. from an IDE)
            catalogue = Path.of("s-100/src/main/schemas.isotc211.org/19115/resources/Codelist/cat/codelists.xml");
        }
        assertTrue(Files.exists(catalogue), "the ISO 19115-3 code list catalogue must be present");

        final DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        codelists = documentBuilderFactory.newDocumentBuilder().parse(catalogue.toFile());
    }

    /**
     * Test that the security classification code list values are the
     * identifiers of the MD_ClassificationCode entries of the catalogue, and
     * not the S-100 Part 17 classification numbers which identify no entry.
     */
    @Test
    void testSecurityClassificationCodesAreCatalogueEntries() {
        final Set<String> entries = codelistEntries("MD_ClassificationCode");

        for (SecurityClassification classification : SecurityClassification.values()) {
            assertTrue(entries.contains(classification.getCode()),
                    "the MD_ClassificationCode catalogue has no '" + classification.getCode() + "' entry");
        }

        // the camel-cased entries are the ones the numeric codes used to break
        assertEquals("unclassified", SecurityClassification.UNCLASSIFIED.getCode());
        assertEquals("topSecret", SecurityClassification.TOP_SECRET.getCode());
        assertEquals("SBU", SecurityClassification.SENSITIVE_BUT_UNCLASSIFIED.getCode());
        assertEquals("forOfficialUseOnly", SecurityClassification.FOR_OFFICIAL_USE_ONLY.getCode());
        assertEquals("limitedDistribution", SecurityClassification.LIMITED_DISTRIBUTION.getCode());
    }

    /**
     * Test that the S-100 Part 17 classification numbering is still available,
     * separately from the code list value.
     */
    @Test
    void testSecurityClassificationNumericCodes() {
        assertEquals(1, SecurityClassification.UNCLASSIFIED.getNumericCode());
        assertEquals(5, SecurityClassification.TOP_SECRET.getNumericCode());
        assertEquals(9, SecurityClassification.LIMITED_DISTRIBUTION.getNumericCode());
    }

    /**
     * Test that the generated code list value type carries the catalogue entry
     * identifier in its codeListValue attribute, along with the human readable
     * value and the code list URL.
     */
    @Test
    void testSecurityClassificationCodeListValueType() {
        final CodeListValueType codeListValueType = SecurityClassification.TOP_SECRET.getCodeListValueType();

        assertNotNull(codeListValueType);
        assertEquals(SecurityClassification.CODELIST, codeListValueType.getCodeList());
        assertEquals("topSecret", codeListValueType.getCodeListValue());
        assertEquals("top secret", codeListValueType.getValue());
    }

    /**
     * Test that the point of contact code list values are the identifiers of
     * the CI_RoleCode entries of the catalogue.
     */
    @Test
    void testPointOfContactCodesAreCatalogueEntries() {
        final Set<String> entries = codelistEntries("CI_RoleCode");

        for (PointOfContact pointOfContact : PointOfContact.values()) {
            assertTrue(entries.contains(pointOfContact.getCode()),
                    "the CI_RoleCode catalogue has no '" + pointOfContact.getCode() + "' entry");
        }

        // the catalogue entry is camel-cased
        assertEquals("coAuthor", PointOfContact.CO_AUTHOR.getCode());
        assertEquals("coAuthor", PointOfContact.CO_AUTHOR.getCodeListValueType().getCodeListValue());
    }

    /**
     * Test that the maintenance frequency code list values are the identifiers
     * of the MD_MaintenanceFrequencyCode entries of the catalogue.
     */
    @Test
    void testMaintenanceFrequencyCodesAreCatalogueEntries() {
        final Set<String> entries = codelistEntries("MD_MaintenanceFrequencyCode");

        for (MaintenanceFrequency maintenanceFrequency : MaintenanceFrequency.values()) {
            assertTrue(entries.contains(maintenanceFrequency.getCode()),
                    "the MD_MaintenanceFrequencyCode catalogue has no '" + maintenanceFrequency.getCode() + "' entry");
        }
    }

    /**
     * Test that the code spaces of the enums are the namespaces the catalogue
     * assigns to the respective code lists.
     */
    @Test
    void testCodeSpacesMatchTheCatalogue() {
        assertEquals(codelistCodeSpace("MD_ClassificationCode"), SecurityClassification.CODESPACE);
        assertEquals(codelistCodeSpace("CI_RoleCode"), PointOfContact.CODESPACE);
        assertEquals(codelistCodeSpace("MD_MaintenanceFrequencyCode"), MaintenanceFrequency.CODESPACE);

        // and, explicitly, the namespaces the code lists are declared in
        assertEquals("https://schemas.isotc211.org/19115/-3/mco/1.0", SecurityClassification.CODESPACE);
        assertEquals("https://schemas.isotc211.org/19115/-3/mmi/1.0", MaintenanceFrequency.CODESPACE);
    }

    /**
     * Returns the identifiers of all the code entries of the requested code
     * list of the catalogue.
     *
     * @param codelistId the identifier of the code list
     * @return the identifiers of the code entries of the code list
     */
    private static Set<String> codelistEntries(String codelistId) {
        final Element codelist = codelist(codelistId);
        final Set<String> entries = new LinkedHashSet<>();
        final NodeList codeEntries = codelist.getElementsByTagNameNS(CAT_NAMESPACE, "CT_CodelistValue");
        for (int i = 0; i < codeEntries.getLength(); i++) {
            entries.add(scopedName((Element) codeEntries.item(i)).getTextContent().trim());
        }
        assertFalse(entries.isEmpty(), "the " + codelistId + " code list must have code entries");
        return entries;
    }

    /**
     * Returns the code space the catalogue assigns to the requested code list.
     *
     * @param codelistId the identifier of the code list
     * @return the code space of the code list
     */
    private static String codelistCodeSpace(String codelistId) {
        return scopedName(codelist(codelistId)).getAttribute("codeSpace");
    }

    /**
     * Returns the catalogue element of the requested code list.
     *
     * @param codelistId the identifier of the code list
     * @return the catalogue element of the code list
     */
    private static Element codelist(String codelistId) {
        final NodeList codelistElements = codelists.getElementsByTagNameNS(CAT_NAMESPACE, "CT_Codelist");
        for (int i = 0; i < codelistElements.getLength(); i++) {
            final Element codelist = (Element) codelistElements.item(i);
            if (codelistId.equals(codelist.getAttribute("id"))) {
                return codelist;
            }
        }
        throw new AssertionError("the catalogue does not contain the " + codelistId + " code list");
    }

    /**
     * Returns the gco:ScopedName of the identifier of the provided catalogue
     * element, which carries both the entry identifier and its code space.
     *
     * @param element the catalogue code list or code entry element
     * @return the scoped name of the identifier of the element
     */
    private static Element scopedName(Element element) {
        final NodeList identifiers = element.getElementsByTagNameNS(CAT_NAMESPACE, "identifier");
        assertTrue(identifiers.getLength() > 0, "every catalogue element carries an identifier");
        final NodeList scopedNames = ((Element) identifiers.item(0)).getElementsByTagNameNS(GCO_NAMESPACE, "ScopedName");
        assertTrue(scopedNames.getLength() > 0, "every catalogue identifier carries a scoped name");
        return (Element) scopedNames.item(0);
    }

}
