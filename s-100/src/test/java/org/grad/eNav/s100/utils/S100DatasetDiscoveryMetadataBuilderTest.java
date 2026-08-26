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

package org.grad.eNav.s100.utils;

import dk.dma.niord.s100.catalog._5_2.*;
import org.grad.eNav.s100.enums.MaintenanceFrequency;
import org.grad.eNav.s100.enums.RoleCode;
import org.grad.eNav.s100.enums.SecurityClassification;
import org.grad.eNav.s100.enums.TelephoneType;
import org.iso.standards.iso._19115.__3.cit._2.CIContactType;
import org.iso.standards.iso._19115.__3.cit._2.CIOrganisationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class S100DatasetDiscoveryMetadataBuilderTest {

    // Test Variables
    private S100DatasetDiscoveryMetadataBuilder s100DatasetDiscoveryMetadataBuilder;

    // Fixed Variables
    private DateTimeFormatter timeFormat = DateTimeFormatter.ISO_TIME;
    private DateTimeFormatter dateFormat = DateTimeFormatter.ISO_DATE;
    private DateTimeFormatter dateTimeFormat = DateTimeFormatter.ISO_DATE_TIME;
    private static final S100ExchangeSetSignatureProvider SIGNATURE_PROVIDER = (id, algorithm, payload) -> {
        S100SEDigitalSignature s100SEDigitalSignature = new S100SEDigitalSignature();
        s100SEDigitalSignature.setId("sig");
        s100SEDigitalSignature.setCertificateRef("ref");
        s100SEDigitalSignature.setValue("signature".getBytes());
        return s100SEDigitalSignature;
    };

    /**
     * Common setup for all the tests.
     */
    @BeforeEach
    void setup() throws IOException {
        this.s100DatasetDiscoveryMetadataBuilder = new S100DatasetDiscoveryMetadataBuilder(SIGNATURE_PROVIDER);
    }

    /**
     * Test that the builder will get constructed correctly with all its
     * elements null.
     */
    @Test
    void testConstructor() {
        assertNotNull(this.s100DatasetDiscoveryMetadataBuilder);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.fileName);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.datasetID);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.description);
        assertFalse(this.s100DatasetDiscoveryMetadataBuilder.compressionFlag);
        assertFalse(this.s100DatasetDiscoveryMetadataBuilder.dataProtection);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.protectionScheme);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.digitalSignatureReference);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.digitalSignatureValues);
        assertFalse(this.s100DatasetDiscoveryMetadataBuilder.copyright);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.classification);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.purpose);
        assertFalse(this.s100DatasetDiscoveryMetadataBuilder.notForNavigation);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.specificUsage);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.editionNumber);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.updateNumber);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.updateApplicationDate);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.referenceID);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.issueDate);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.issueTime);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.boundingBox);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.timeInstantBegin);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.timeInstantEnd);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.productSpecification);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.producingAgency);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.producingAgencyRole);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.producingAgencyPhone);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.producingAgencyPhoneType);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.producingAgencyElectronicMailAddresses);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.producingAgencyCity);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.producingAgencyAdministrativeArea);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.producingAgencyPostalCode);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.producingAgencyCountry);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.producingAgencyOnlineResource);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.producingAgencyContactInstructions);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.producerCode);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.encodingFormat);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.dataCoverages);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.metadataDateStamp);
        assertFalse(this.s100DatasetDiscoveryMetadataBuilder.replacedData);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.navigationPurposes);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.maintenanceFrequency);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.maintenanceDate);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.maintenancePeriod);
    }

    /**
     * Test that if provided the builder arguments will be correctly set to
     * the builder.
     */
    @Test
    void testSetters() {
        // Perform the setting operations
        this.s100DatasetDiscoveryMetadataBuilder
                .setFileName("file:/dataset.XML")
                .setDatasetID("urn:mrn:gla:grad:s125:datasets:XXXX")
                .setDescription("description")
                .setCompressionFlag(false)
                .setDataProtection(false)
                .setProtectionScheme(S100ProtectionScheme.S_100_P_15)
                .setCopyright(true)
                .setClassification(SecurityClassification.UNCLASSIFIED)
                .setPurpose(S100Purpose.NEW_DATASET)
                .setNotForNavigation(true)
                .setSpecificUsage("testing")
                .setEditionNumber(BigInteger.ONE)
                .setUpdateNumber(BigInteger.ZERO)
                .setUpdateApplicationDate(LocalDate.parse("2023-01-01", this.dateFormat))
                .setIssueDate(LocalDate.parse("2023-01-02", this.dateFormat))
                .setIssueTime(LocalTime.parse("00:00:00", this.timeFormat))
                .setTimeInstantBegin(LocalDateTime.parse("2023-01-02T00:00:00", this.dateTimeFormat))
                .setTimeInstantEnd(LocalDateTime.parse("2023-01-05T00:00:00", this.dateTimeFormat))
                .setProductSpecification(new S100ProductSpecification())
                .setProducingAgency("producingAgency")
                .setProducingAgencyRole(RoleCode.ORIGINATOR)
                .setProducingAgencyPhone("+44 1255 245000")
                .setProducingAgencyPhoneType(TelephoneType.VOICE)
                .setProducingAgencyElectronicMailAddresses(Collections.singletonList("test@gla-rad.org"))
                .setProducingAgencyCity("Harwich")
                .setProducingAgencyAdministrativeArea("England")
                .setProducingAgencyPostalCode("postalCode")
                .setProducingAgencyCountry("UK")
                .setProducingAgencyOnlineResource("https://www.gla-rad.org")
                .setProducingAgencyContactInstructions("contactInstructions")
                .setProducerCode("producerCode")
                .setEncodingFormat(S100EncodingFormat.GML)
                .setDataCoverages(null)
                .setComment("comment")
                .setMetadataDateStamp(LocalDate.parse("2023-01-03", this.dateFormat))
                .setReplacedData(true)
                .setNavigationPurposes(Collections.singletonList(S100NavigationPurpose.OVERVIEW))
                .setMaintenanceFrequency(MaintenanceFrequency.AS_NEEDED);

        assertNotNull(this.s100DatasetDiscoveryMetadataBuilder);
        assertEquals("file:/dataset.XML", this.s100DatasetDiscoveryMetadataBuilder.fileName);
        assertEquals("urn:mrn:gla:grad:s125:datasets:XXXX", this.s100DatasetDiscoveryMetadataBuilder.datasetID);
        assertEquals("description", this.s100DatasetDiscoveryMetadataBuilder.description);
        assertFalse(this.s100DatasetDiscoveryMetadataBuilder.compressionFlag);
        assertFalse(this.s100DatasetDiscoveryMetadataBuilder.dataProtection);
        assertEquals(S100ProtectionScheme.S_100_P_15, this.s100DatasetDiscoveryMetadataBuilder.protectionScheme);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.digitalSignatureReference);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.digitalSignatureValues);
        assertTrue(this.s100DatasetDiscoveryMetadataBuilder.copyright);
        assertEquals(SecurityClassification.UNCLASSIFIED, this.s100DatasetDiscoveryMetadataBuilder.classification);
        assertEquals(S100Purpose.NEW_DATASET, this.s100DatasetDiscoveryMetadataBuilder.purpose);
        assertTrue(this.s100DatasetDiscoveryMetadataBuilder.notForNavigation);
        assertEquals("testing", this.s100DatasetDiscoveryMetadataBuilder.specificUsage);
        assertEquals(BigInteger.ONE, this.s100DatasetDiscoveryMetadataBuilder.editionNumber);
        assertEquals(BigInteger.ZERO, this.s100DatasetDiscoveryMetadataBuilder.updateNumber);
        assertEquals(LocalDate.parse("2023-01-01", this.dateFormat), this.s100DatasetDiscoveryMetadataBuilder.updateApplicationDate);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.referenceID);
        assertEquals(LocalDate.parse("2023-01-02", this.dateFormat),this.s100DatasetDiscoveryMetadataBuilder.issueDate);
        assertEquals(LocalTime.parse("00:00:00", this.timeFormat), this.s100DatasetDiscoveryMetadataBuilder.issueTime);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.boundingBox);
        assertEquals(LocalDateTime.parse("2023-01-02T00:00:00", this.dateTimeFormat), this.s100DatasetDiscoveryMetadataBuilder.timeInstantBegin);
        assertEquals(LocalDateTime.parse("2023-01-05T00:00:00", this.dateTimeFormat), this.s100DatasetDiscoveryMetadataBuilder.timeInstantEnd);
        assertNotNull(this.s100DatasetDiscoveryMetadataBuilder.productSpecification);
        assertEquals("producingAgency", this.s100DatasetDiscoveryMetadataBuilder.producingAgency);
        assertEquals(RoleCode.ORIGINATOR,this.s100DatasetDiscoveryMetadataBuilder.producingAgencyRole);
        assertEquals("+44 1255 245000", this.s100DatasetDiscoveryMetadataBuilder.producingAgencyPhone);
        assertEquals(TelephoneType.VOICE, this.s100DatasetDiscoveryMetadataBuilder.producingAgencyPhoneType);
        assertNotNull(this.s100DatasetDiscoveryMetadataBuilder.producingAgencyElectronicMailAddresses);
        assertEquals(1, this.s100DatasetDiscoveryMetadataBuilder.producingAgencyElectronicMailAddresses.size());
        assertEquals("test@gla-rad.org", this.s100DatasetDiscoveryMetadataBuilder.producingAgencyElectronicMailAddresses.get(0));
        assertEquals("Harwich", this.s100DatasetDiscoveryMetadataBuilder.producingAgencyCity);
        assertEquals("England", this.s100DatasetDiscoveryMetadataBuilder.producingAgencyAdministrativeArea);
        assertEquals("postalCode", this.s100DatasetDiscoveryMetadataBuilder.producingAgencyPostalCode);
        assertEquals("UK", this.s100DatasetDiscoveryMetadataBuilder.producingAgencyCountry);
        assertEquals("https://www.gla-rad.org", this.s100DatasetDiscoveryMetadataBuilder.producingAgencyOnlineResource);
        assertEquals("contactInstructions", this.s100DatasetDiscoveryMetadataBuilder.producingAgencyContactInstructions);
        assertEquals("producerCode", this.s100DatasetDiscoveryMetadataBuilder.producerCode);
        assertEquals(S100EncodingFormat.GML, this.s100DatasetDiscoveryMetadataBuilder.encodingFormat);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.dataCoverages);
        assertEquals(LocalDate.parse("2023-01-03", this.dateFormat), this.s100DatasetDiscoveryMetadataBuilder.metadataDateStamp);
        assertTrue(this.s100DatasetDiscoveryMetadataBuilder.replacedData);
        assertNotNull(this.s100DatasetDiscoveryMetadataBuilder.navigationPurposes);
        assertTrue(this.s100DatasetDiscoveryMetadataBuilder.navigationPurposes.contains(S100NavigationPurpose.OVERVIEW));
        assertEquals(MaintenanceFrequency.AS_NEEDED, this.s100DatasetDiscoveryMetadataBuilder.maintenanceFrequency);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.maintenanceDate);
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.maintenancePeriod);
    }

    /**
     * Test that the maintenance frequency values not included in the S-100
     * restricted subset of the ISO 19115-1 MD_MaintenanceFrequencyCode
     * codelist will be rejected.
     */
    @Test
    void testSetMaintenanceFrequencyRestrictedToS100Subset() {
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> this.s100DatasetDiscoveryMetadataBuilder.setMaintenanceFrequency(MaintenanceFrequency.CONTINUAL));

        assertTrue(exception.getMessage().contains("continual"));
        assertTrue(exception.getMessage().contains("asNeeded and irregular"));

        // While the allowed values are accepted
        assertDoesNotThrow(() -> this.s100DatasetDiscoveryMetadataBuilder.setMaintenanceFrequency(MaintenanceFrequency.AS_NEEDED));
        assertDoesNotThrow(() -> this.s100DatasetDiscoveryMetadataBuilder.setMaintenanceFrequency(MaintenanceFrequency.IRREGULAR));
        assertDoesNotThrow(() -> this.s100DatasetDiscoveryMetadataBuilder.setMaintenanceFrequency(null));
    }

    /**
     * Test that the maintenance periods that are not positive durations will
     * be rejected, since S-100 prohibits zero and negative durations in the
     * userDefinedMaintenanceFrequency.
     */
    @Test
    void testSetMaintenancePeriodRequiresPositiveDuration() {
        final IllegalArgumentException zero = assertThrows(IllegalArgumentException.class,
                () -> this.s100DatasetDiscoveryMetadataBuilder.setMaintenancePeriod(Duration.ZERO));
        assertTrue(zero.getMessage().contains("prohibiting zero or negative values of duration"));

        final IllegalArgumentException negative = assertThrows(IllegalArgumentException.class,
                () -> this.s100DatasetDiscoveryMetadataBuilder.setMaintenancePeriod(Duration.ofHours(-48)));
        assertTrue(negative.getMessage().contains("prohibiting zero or negative values of duration"));

        // While the positive durations are accepted
        assertDoesNotThrow(() -> this.s100DatasetDiscoveryMetadataBuilder.setMaintenancePeriod(Duration.ofSeconds(1)));
        assertEquals(Duration.ofSeconds(1), this.s100DatasetDiscoveryMetadataBuilder.maintenancePeriod);
        assertDoesNotThrow(() -> this.s100DatasetDiscoveryMetadataBuilder.setMaintenancePeriod(null));
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.maintenancePeriod);
    }

    /**
     * Test that the reference ID values that are not Marine Resource Names will
     * be rejected.
     */
    @Test
    void testSetReferenceIDRequiresMarineResourceName() {
        final IllegalArgumentException notAnMrn = assertThrows(IllegalArgumentException.class,
                () -> this.s100DatasetDiscoveryMetadataBuilder.setReferenceID("gla:grad:s125:datasets:XXXX"));
        assertTrue(notAnMrn.getMessage().contains("must be an MRN"));

        // The MRN namespace on its own does not identify anything either
        final IllegalArgumentException empty = assertThrows(IllegalArgumentException.class,
                () -> this.s100DatasetDiscoveryMetadataBuilder.setReferenceID("urn:mrn:"));
        assertTrue(empty.getMessage().contains("must be an MRN"));

        // While the MRNs are accepted
        assertDoesNotThrow(() -> this.s100DatasetDiscoveryMetadataBuilder.setReferenceID("urn:mrn:gla:grad:s125:datasets:XXXX"));
        assertEquals("urn:mrn:gla:grad:s125:datasets:XXXX", this.s100DatasetDiscoveryMetadataBuilder.referenceID);
        assertDoesNotThrow(() -> this.s100DatasetDiscoveryMetadataBuilder.setReferenceID(null));
        assertNull(this.s100DatasetDiscoveryMetadataBuilder.referenceID);
    }

    /**
     * Test that the reference ID back to the dataset ID of the updated dataset
     * is emitted if and only if the dataset is an update.
     */
    @Test
    void testBuildReferenceIDOnlyForUpdates() {
        // An update dataset without a reference back to the updated dataset
        final IllegalStateException missing = assertThrows(IllegalStateException.class,
                () -> this.minimalBuilder()
                        .setPurpose(S100Purpose.UPDATE)
                        .build("dataset".getBytes()));
        assertTrue(missing.getMessage().contains("if and only if the dataset is an update"));

        // A reference back to an updated dataset for a dataset that is not an update
        final IllegalStateException unexpected = assertThrows(IllegalStateException.class,
                () -> this.minimalBuilder()
                        .setPurpose(S100Purpose.NEW_DATASET)
                        .setReferenceID("urn:mrn:gla:grad:s125:datasets:XXXX")
                        .build("dataset".getBytes()));
        assertTrue(unexpected.getMessage().contains("if and only if the dataset is an update"));

        // While an update dataset carrying the reference is valid
        final S100DatasetDiscoveryMetadata metadata = this.minimalBuilder()
                .setPurpose(S100Purpose.UPDATE)
                .setReferenceID("urn:mrn:gla:grad:s125:datasets:XXXX")
                .build("dataset".getBytes());
        assertEquals("urn:mrn:gla:grad:s125:datasets:XXXX", metadata.getReferenceID());

        // And so is a dataset that is not an update and carries no reference
        assertNull(this.minimalBuilder()
                .setPurpose(S100Purpose.NEW_DATASET)
                .build("dataset".getBytes())
                .getReferenceID());
    }

    /**
     * Test that the S-100 Exchange Set Catalogue Discovery Metadata builder can
     * correctly build a discovery metadata XML if the appropriate parameters
     * have been provided.
     */
    @Test
    void testBuild() {
        // Perform the setting operations and build
        final S100DatasetDiscoveryMetadata metadata = this.s100DatasetDiscoveryMetadataBuilder
                .setFileName("file:/dataset.XML")
                .setDatasetID("urn:mrn:gla:grad:s125:datasets:XXXX")
                .setDescription("description")
                .setCompressionFlag(false)
                .setDataProtection(false)
                .setProtectionScheme(S100ProtectionScheme.S_100_P_15)
                .setCopyright(true)
                .setClassification(SecurityClassification.UNCLASSIFIED)
                .setPurpose(S100Purpose.NEW_DATASET)
                .setNotForNavigation(true)
                .setSpecificUsage("testing")
                .setEditionNumber(BigInteger.ONE)
                .setUpdateNumber(BigInteger.ZERO)
                .setUpdateApplicationDate(LocalDate.parse("2023-01-01", this.dateFormat))
                .setIssueDate(LocalDate.parse("2023-01-02", this.dateFormat))
                .setIssueTime(LocalTime.parse("00:00:00", this.timeFormat))
                .setTimeInstantBegin(LocalDateTime.parse("2023-01-02T00:00:00", this.dateTimeFormat))
                .setTimeInstantEnd(LocalDateTime.parse("2023-01-05T00:00:00", this.dateTimeFormat))
                .setProductSpecification(new S100ProductSpecification())
                .setProducingAgency("producingAgency")
                .setProducingAgencyRole(RoleCode.ORIGINATOR)
                .setProducingAgencyPhone("+44 1255 245000")
                .setProducingAgencyPhoneType(TelephoneType.VOICE)
                .setProducingAgencyElectronicMailAddresses(Collections.singletonList("test@gla-rad.org"))
                .setProducingAgencyCity("Harwich")
                .setProducingAgencyAdministrativeArea("England")
                .setProducingAgencyPostalCode("postalCode")
                .setProducingAgencyCountry("UK")
                .setProducingAgencyOnlineResource("https://www.gla-rad.org")
                .setProducingAgencyContactInstructions("contactInstructions")
                .setProducerCode("producerCode")
                .setEncodingFormat(S100EncodingFormat.GML)
                .setDataCoverages(null)
                .setComment("comment")
                .setMetadataDateStamp(LocalDate.parse("2023-01-03", this.dateFormat))
                .setReplacedData(true)
                .setNavigationPurposes(Collections.singletonList(S100NavigationPurpose.OVERVIEW))
                .setMaintenanceFrequency(MaintenanceFrequency.AS_NEEDED)
                .setMaintenanceDate(LocalDate.parse("2023-01-04", this.dateFormat))
                .build("dataset".getBytes());

        // Assess the main information
        assertNotNull(metadata);
        assertEquals("file:/dataset.XML", metadata.getFileName());
        assertEquals("urn:mrn:gla:grad:s125:datasets:XXXX", metadata.getDatasetID());
        assertNotNull(metadata.getDescription());
        assertNotNull(metadata.getDescription().getCharacterString());
        assertEquals("description", metadata.getDescription().getCharacterString().getValue());
        assertFalse(metadata.isCompressionFlag());
        assertFalse(metadata.isDataProtection());
        assertEquals(S100ProtectionScheme.S_100_P_15, metadata.getProtectionScheme());
        assertTrue(metadata.isCopyright());
        assertNotNull(metadata.getClassification());
        assertNotNull(metadata.getClassification().getIsoType());
        assertNotNull(metadata.getClassification().getMDClassificationCode());
        assertEquals(SecurityClassification.UNCLASSIFIED.getValue(), metadata.getClassification().getMDClassificationCode().getValue());
        // the codeListValue identifies an entry of the referenced code list
        assertEquals("unclassified", metadata.getClassification().getMDClassificationCode().getCodeListValue());
        assertEquals(S100Purpose.NEW_DATASET, metadata.getPurpose());
        assertTrue(metadata.isNotForNavigation());
        assertNotNull(metadata.getSpecificUsage());
        assertNotNull(metadata.getSpecificUsage().getMDUsage());
        assertNotNull(metadata.getSpecificUsage().getMDUsage().getSpecificUsage());
        assertNotNull(metadata.getSpecificUsage().getMDUsage().getSpecificUsage().getCharacterString());
        assertEquals("testing", metadata.getSpecificUsage().getMDUsage().getSpecificUsage().getCharacterString().getValue());
        assertEquals(BigInteger.ONE, metadata.getEditionNumber());
        assertEquals(BigInteger.ZERO, metadata.getUpdateNumber());
        assertEquals(LocalDate.parse("2023-01-01", this.dateFormat), metadata.getUpdateApplicationDate());
        assertNull(metadata.getReferenceID());
        assertEquals(LocalDate.parse("2023-01-02", this.dateFormat), metadata.getIssueDate());
        assertEquals(LocalTime.parse("00:00:00", this.timeFormat), metadata.getIssueTime());
        assertNotNull(metadata.getTemporalExtent());
        assertEquals(LocalDateTime.parse("2023-01-02T00:00:00", this.dateTimeFormat), metadata.getTemporalExtent().getTimeInstantBegin());
        assertEquals(LocalDateTime.parse("2023-01-05T00:00:00", this.dateTimeFormat), metadata.getTemporalExtent().getTimeInstantEnd());
        assertNotNull(metadata.getProductSpecification());

        // Assess the producing agency information
        assertNotNull(metadata.getProducingAgency());
        assertNotNull(metadata.getProducingAgency().getCIResponsibility());
        assertNotNull(metadata.getProducingAgency().getCIResponsibility().getParties());
        assertEquals(1, metadata.getProducingAgency().getCIResponsibility().getParties().size());
        assertNotNull(metadata.getProducingAgency().getCIResponsibility().getParties().get(0));
        assertNotNull(metadata.getProducingAgency().getCIResponsibility().getParties().get(0).getAbstractCIParty());
        assertNotNull(metadata.getProducingAgency().getCIResponsibility().getParties().get(0).getAbstractCIParty().getValue());
        final CIOrganisationType organisation = (CIOrganisationType) metadata.getProducingAgency()
                .getCIResponsibility().getParties().get(0).getAbstractCIParty().getValue();
        assertNotNull(organisation.getName());
        assertNotNull(organisation.getName().getCharacterString());
        assertEquals("producingAgency", organisation.getName().getCharacterString().getValue());

        // Assess the mandatory producing agency contact information
        assertNotNull(organisation.getContactInfos());
        assertEquals(1, organisation.getContactInfos().size());
        final CIContactType contact = organisation.getContactInfos().get(0).getCIContact();
        assertNotNull(contact);
        assertEquals(1, contact.getPhones().size());
        assertNotNull(contact.getPhones().get(0).getCITelephone());
        assertEquals("+44 1255 245000", contact.getPhones().get(0).getCITelephone().getNumber().getCharacterString().getValue());
        assertNotNull(contact.getPhones().get(0).getCITelephone().getNumberType());
        assertEquals(TelephoneType.VOICE.getValue(), contact.getPhones().get(0).getCITelephone().getNumberType().getCITelephoneTypeCode().getValue());
        assertEquals(1, contact.getAddresses().size());
        assertNotNull(contact.getAddresses().get(0).getCIAddress());
        assertEquals(1, contact.getAddresses().get(0).getCIAddress().getElectronicMailAddresses().size());
        assertEquals("test@gla-rad.org", contact.getAddresses().get(0).getCIAddress().getElectronicMailAddresses().get(0).getCharacterString().getValue());
        assertEquals("Harwich", contact.getAddresses().get(0).getCIAddress().getCity().getCharacterString().getValue());
        assertEquals("England", contact.getAddresses().get(0).getCIAddress().getAdministrativeArea().getCharacterString().getValue());
        assertEquals("postalCode", contact.getAddresses().get(0).getCIAddress().getPostalCode().getCharacterString().getValue());
        assertEquals("UK", contact.getAddresses().get(0).getCIAddress().getCountry().getCharacterString().getValue());
        assertEquals(1, contact.getOnlineResources().size());
        assertNotNull(contact.getOnlineResources().get(0).getCIOnlineResource());
        assertEquals("https://www.gla-rad.org", contact.getOnlineResources().get(0).getCIOnlineResource().getLinkage().getCharacterString().getValue());
        assertNotNull(contact.getContactInstructions());
        assertEquals("contactInstructions", contact.getContactInstructions().getCharacterString().getValue());
        assertNotNull(metadata.getProducingAgency().getCIResponsibility().getRole());
        assertNotNull(metadata.getProducingAgency().getCIResponsibility().getRole().getCIRoleCode());
        assertEquals(RoleCode.ORIGINATOR.getValue(), metadata.getProducingAgency().getCIResponsibility().getRole().getCIRoleCode().getValue());

        // Assess the maintenance information
        assertEquals("producerCode", metadata.getProducerCode());
        assertNotNull(metadata.getEncodingFormat());
        assertEquals(S100EncodingFormat.GML, metadata.getEncodingFormat().getValue());
        assertNotNull(metadata.getDataCoverages());
        assertTrue(metadata.getDataCoverages().isEmpty());
        assertNotNull(metadata.getComment());
        assertNotNull(metadata.getComment().getCharacterString());
        assertEquals("comment", metadata.getComment().getCharacterString().getValue());
        assertEquals(LocalDate.parse("2023-01-03", this.dateFormat), metadata.getMetadataDateStamp());
        assertTrue(metadata.isReplacedData());
        assertNotNull(metadata.getNavigationPurposes());
        assertEquals(1, metadata.getNavigationPurposes().size());
        assertTrue(metadata.getNavigationPurposes().contains(S100NavigationPurpose.OVERVIEW));
        assertNotNull(metadata.getResourceMaintenance());
        assertNotNull(metadata.getResourceMaintenance().getMDMaintenanceInformation());
        assertNotNull(metadata.getResourceMaintenance().getMDMaintenanceInformation().getMaintenanceAndUpdateFrequency());
        assertNotNull(metadata.getResourceMaintenance().getMDMaintenanceInformation().getMaintenanceDates());
        assertEquals(1, metadata.getResourceMaintenance().getMDMaintenanceInformation().getMaintenanceDates().size());
        assertNull(metadata.getResourceMaintenance().getMDMaintenanceInformation().getUserDefinedMaintenanceFrequency());
        assertNotNull(metadata.getResourceMaintenance().getMDMaintenanceInformation().getMaintenanceAndUpdateFrequency().getMDMaintenanceFrequencyCode());
        assertEquals(MaintenanceFrequency.AS_NEEDED.getValue(), metadata.getResourceMaintenance().getMDMaintenanceInformation().getMaintenanceAndUpdateFrequency().getMDMaintenanceFrequencyCode().getValue());

        // Assess the signature
        assertNotNull(metadata.getDigitalSignatureReference());
        assertNotNull(metadata.getDigitalSignatureReference().getValue());
        assertEquals(S100SEDigitalSignatureReference.ECDSA_384_SHA_2, metadata.getDigitalSignatureReference().getValue());
        assertNotNull(metadata.getDigitalSignatureValues());
        assertEquals(1, metadata.getDigitalSignatureValues().size());
        assertNotNull(metadata.getDigitalSignatureValues().get(0));
        assertNotNull(metadata.getDigitalSignatureValues().get(0).getS100SEDigitalSignature());
        assertNotNull(metadata.getDigitalSignatureValues().get(0).getS100SEDigitalSignature().getValue());
        assertNotNull(metadata.getDigitalSignatureValues().get(0).getS100SEDigitalSignature().getValue().getValue());
        assertEquals("signature".getBytes().length, metadata.getDigitalSignatureValues().get(0).getS100SEDigitalSignature().getValue().getValue().length);
    }

    /**
     * Test that the protection scheme provided to the builder is the one
     * emitted, and that no protection scheme is emitted for the unprotected
     * datasets that do not define one.
     */
    @Test
    void testBuildProtectionScheme() {
        assertEquals(S100ProtectionScheme.S_100_P_15, this.minimalBuilder()
                .setProtectionScheme(S100ProtectionScheme.S_100_P_15)
                .build("dataset".getBytes())
                .getProtectionScheme());
        assertNull(this.minimalBuilder()
                .setDataProtection(false)
                .build("dataset".getBytes())
                .getProtectionScheme());
    }

    /**
     * Test that the temporal extent is encoded if and only if at least one of
     * the start and the end of the extent is known.
     */
    @Test
    void testBuildTemporalExtent() {
        // No temporal extent information at all
        assertNull(this.minimalBuilder()
                .build("dataset".getBytes())
                .getTemporalExtent());

        // Only the beginning of the extent is known
        final S100TemporalExtent beginOnly = this.minimalBuilder()
                .setTimeInstantBegin(LocalDateTime.parse("2023-01-02T00:00:00", this.dateTimeFormat))
                .build("dataset".getBytes())
                .getTemporalExtent();
        assertNotNull(beginOnly);
        assertEquals(LocalDateTime.parse("2023-01-02T00:00:00", this.dateTimeFormat), beginOnly.getTimeInstantBegin());
        assertNull(beginOnly.getTimeInstantEnd());

        // Only the end of the extent is known
        final S100TemporalExtent endOnly = this.minimalBuilder()
                .setTimeInstantEnd(LocalDateTime.parse("2023-01-05T00:00:00", this.dateTimeFormat))
                .build("dataset".getBytes())
                .getTemporalExtent();
        assertNotNull(endOnly);
        assertNull(endOnly.getTimeInstantBegin());
        assertEquals(LocalDateTime.parse("2023-01-05T00:00:00", this.dateTimeFormat), endOnly.getTimeInstantEnd());
    }

    /**
     * Test that the replaced data flag provided to the builder is honoured.
     */
    @Test
    void testBuildReplacedData() {
        assertTrue(this.minimalBuilder()
                .setReplacedData(true)
                .build("dataset".getBytes())
                .isReplacedData());
        assertFalse(this.minimalBuilder()
                .build("dataset".getBytes())
                .isReplacedData());
    }

    /**
     * Test that the mandatory producing agency information will not be
     * silently omitted.
     */
    @Test
    void testBuildRequiresProducingAgency() {
        final IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> this.s100DatasetDiscoveryMetadataBuilder
                        .setFileName("file:/dataset.XML")
                        .build("dataset".getBytes()));

        assertTrue(exception.getMessage().contains("producingAgency multiplicity 1"));
    }

    /**
     * Test that the mandatory producing agency contact information will not be
     * silently omitted.
     */
    @Test
    void testBuildRequiresProducingAgencyContact() {
        final IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> this.s100DatasetDiscoveryMetadataBuilder
                        .setFileName("file:/dataset.XML")
                        .setProducingAgency("producingAgency")
                        .build("dataset".getBytes()));

        assertTrue(exception.getMessage().contains("CI_Organisation.contactInfo is mandatory"));

        // An empty list of electronic mail addresses documents nothing either
        final IllegalStateException empty = assertThrows(IllegalStateException.class,
                () -> new S100DatasetDiscoveryMetadataBuilder(SIGNATURE_PROVIDER)
                        .setFileName("file:/dataset.XML")
                        .setProducingAgency("producingAgency")
                        .setProducingAgencyElectronicMailAddresses(Collections.emptyList())
                        .build("dataset".getBytes()));
        assertTrue(empty.getMessage().contains("CI_Organisation.contactInfo is mandatory"));

        // While any single one of the allowed contact details is sufficient
        assertDoesNotThrow(() -> this.minimalBuilder()
                .setProducingAgencyContactInstructions("contactInstructions")
                .build("dataset".getBytes()));
        assertDoesNotThrow(() -> this.minimalBuilder()
                .setProducingAgencyOnlineResource("https://www.gla-rad.org")
                .build("dataset".getBytes()));
        assertDoesNotThrow(() -> this.minimalBuilder()
                .setProducingAgencyElectronicMailAddresses(Collections.singletonList("test@gla-rad.org"))
                .build("dataset".getBytes()));
    }

    /**
     * Test that the mandatory role of the producing agency responsibility will
     * not be silently omitted.
     */
    @Test
    void testBuildRequiresProducingAgencyRole() {
        final IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> new S100DatasetDiscoveryMetadataBuilder(SIGNATURE_PROVIDER)
                        .setFileName("file:/dataset.XML")
                        .setProducingAgency("producingAgency")
                        .setProducingAgencyPhone("+44 1255 245000")
                        .build("dataset".getBytes()));

        assertTrue(exception.getMessage().contains("cit:role"));

        // While the provided role is emitted in the producing agency responsibility
        final S100DatasetDiscoveryMetadata metadata = this.minimalBuilder()
                .setProducingAgencyRole(RoleCode.CUSTODIAN)
                .build("dataset".getBytes());
        assertNotNull(metadata.getProducingAgency().getCIResponsibility().getRole());
        assertNotNull(metadata.getProducingAgency().getCIResponsibility().getRole().getCIRoleCode());
        assertEquals(RoleCode.CUSTODIAN.getValue(), metadata.getProducingAgency().getCIResponsibility().getRole().getCIRoleCode().getValue());
    }

    /**
     * Test that the optional resource maintenance information is only emitted
     * when it has actually been provided.
     */
    @Test
    void testBuildOmitsEmptyResourceMaintenance() {
        assertNull(this.minimalBuilder()
                .build("dataset".getBytes())
                .getResourceMaintenance());
    }

    /**
     * Test that exactly one of the maintenance date and the maintenance period
     * is allowed in the resource maintenance information.
     */
    @Test
    void testBuildRequiresExactlyOneMaintenanceValue() {
        // Neither of the two provided
        final IllegalStateException neither = assertThrows(IllegalStateException.class,
                () -> this.minimalBuilder()
                        .setMaintenanceFrequency(MaintenanceFrequency.AS_NEEDED)
                        .build("dataset".getBytes()));
        assertTrue(neither.getMessage().contains("exactly one of maintenanceDate and userDefinedMaintenanceFrequency"));

        // Both of the two provided
        final IllegalStateException both = assertThrows(IllegalStateException.class,
                () -> this.minimalBuilder()
                        .setMaintenanceDate(LocalDate.parse("2023-01-04", this.dateFormat))
                        .setMaintenancePeriod(Duration.ofDays(100))
                        .build("dataset".getBytes()));
        assertTrue(both.getMessage().contains("exactly one of maintenanceDate and userDefinedMaintenanceFrequency"));
    }

    /**
     * Test that the maintenance frequency accompanies the maintenance date but
     * never the maintenance period.
     */
    @Test
    void testBuildMaintenanceFrequencyCombinations() {
        // A maintenance date without a maintenance frequency
        final IllegalStateException missing = assertThrows(IllegalStateException.class,
                () -> this.minimalBuilder()
                        .setMaintenanceDate(LocalDate.parse("2023-01-04", this.dateFormat))
                        .build("dataset".getBytes()));
        assertTrue(missing.getMessage().contains("maintenanceAndUpdateFrequency must be populated"));

        // A maintenance period along with a maintenance frequency
        final IllegalStateException extra = assertThrows(IllegalStateException.class,
                () -> this.minimalBuilder()
                        .setMaintenanceFrequency(MaintenanceFrequency.IRREGULAR)
                        .setMaintenancePeriod(Duration.ofDays(100))
                        .build("dataset".getBytes()));
        assertTrue(extra.getMessage().contains("if and only if userDefinedMaintenanceFrequency is not populated"));

        // While a maintenance period on its own is valid
        final S100DatasetDiscoveryMetadata metadata = this.minimalBuilder()
                .setMaintenancePeriod(Duration.ofDays(100))
                .build("dataset".getBytes());
        assertNotNull(metadata.getResourceMaintenance());
        assertNull(metadata.getResourceMaintenance().getMDMaintenanceInformation().getMaintenanceAndUpdateFrequency());
        assertNotNull(metadata.getResourceMaintenance().getMDMaintenanceInformation().getUserDefinedMaintenanceFrequency());
        assertEquals(Duration.ofDays(100), metadata.getResourceMaintenance().getMDMaintenanceInformation().getUserDefinedMaintenanceFrequency().getTMPeriodDuration());
    }

    /**
     * Test that the mandatory digital signature values will not be silently
     * omitted when neither a signature provider with a payload nor the already
     * generated signature values are available.
     */
    @Test
    void testBuildRequiresDigitalSignatureValues() {
        // No signature provider at all
        final IllegalStateException noProvider = assertThrows(IllegalStateException.class,
                () -> this.minimalBuilder(null).build("dataset".getBytes()));
        assertTrue(noProvider.getMessage().contains("digitalSignatureValue multiplicity 1..*"));

        // A signature provider but no payload to sign
        final IllegalStateException noPayload = assertThrows(IllegalStateException.class,
                () -> this.minimalBuilder().build(null));
        assertTrue(noPayload.getMessage().contains("digitalSignatureValue multiplicity 1..*"));

        // An empty list of already generated signature values
        final IllegalStateException noValues = assertThrows(IllegalStateException.class,
                () -> this.minimalBuilder()
                        .setDigitalSignatureValues(Collections.emptyList())
                        .build(null));
        assertTrue(noValues.getMessage().contains("digitalSignatureValue multiplicity 1..*"));
    }

    /**
     * Test that a fileless entry, i.e. one without a payload to be signed, can
     * still reuse the already generated digital signature values.
     */
    @Test
    void testBuildReusesProvidedDigitalSignatureValues() {
        final S100DatasetDiscoveryMetadata.DigitalSignatureValue digitalSignatureValue = new S100DatasetDiscoveryMetadata.DigitalSignatureValue();
        final S100DatasetDiscoveryMetadata metadata = this.minimalBuilder()
                .setDigitalSignatureValues(Collections.singletonList(digitalSignatureValue))
                .build(null);

        assertEquals(1, metadata.getDigitalSignatureValues().size());
        assertEquals(digitalSignatureValue, metadata.getDigitalSignatureValues().get(0));
    }

    /**
     * Creates a new builder populated with the minimum mandatory information,
     * i.e. the producing agency along with its role and contact details, and
     * using the provided signature provider.
     *
     * @param signatureProvider the signature provider to be used, if any
     * @return the minimally populated S-100 dataset discovery metadata builder
     */
    private S100DatasetDiscoveryMetadataBuilder minimalBuilder(S100ExchangeSetSignatureProvider signatureProvider) {
        return new S100DatasetDiscoveryMetadataBuilder(signatureProvider)
                .setFileName("file:/dataset.XML")
                .setProducingAgency("producingAgency")
                .setProducingAgencyRole(RoleCode.ORIGINATOR)
                .setProducingAgencyPhone("+44 1255 245000");
    }

    /**
     * Creates a new builder populated with the minimum mandatory information
     * and using the test signature provider.
     *
     * @return the minimally populated S-100 dataset discovery metadata builder
     */
    private S100DatasetDiscoveryMetadataBuilder minimalBuilder() {
        return this.minimalBuilder(SIGNATURE_PROVIDER);
    }

}