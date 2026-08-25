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

import dk.dma.niord.s100.catalog._5_2.ObjectFactory;
import dk.dma.niord.s100.catalog._5_2.*;
import org.grad.eNav.s100.enums.CodeListValueTypeProvider;
import org.grad.eNav.s100.enums.MaintenanceFrequency;
import org.grad.eNav.s100.enums.RoleCode;
import org.grad.eNav.s100.enums.SecurityClassification;
import org.grad.eNav.s100.enums.TelephoneType;
import org.iso.standards.iso._19115.__3.cit._2.*;
import org.iso.standards.iso._19115.__3.gco._1.DatePropertyType;
import org.iso.standards.iso._19115.__3.gco._1.TMPeriodDurationPropertyType;
import org.iso.standards.iso._19115.__3.mcc._1.AbstractTypedDatePropertyType;
import org.iso.standards.iso._19115.__3.mmi._1.MDMaintenanceFrequencyCodePropertyType;
import org.iso.standards.iso._19115.__3.mmi._1.MDMaintenanceInformationPropertyType;
import org.iso.standards.iso._19115.__3.mmi._1.MDMaintenanceInformationType;
import org.iso.standards.iso._19115.__3.mri._1.MDUsageType;
import org.locationtech.jts.geom.Geometry;

import java.math.BigInteger;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The S100 Dataset Discovery Metadata Builder Class.
 * <p/>
 * This is a basic builder class that enables the generation of the S100
 * Dataset Discovery Metadata contents.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class S100DatasetDiscoveryMetadataBuilder {

    // The only MD_MaintenanceFrequencyCode values allowed by S-100 Part 17
    private static final Set<MaintenanceFrequency> ALLOWED_MAINTENANCE_FREQUENCIES =
            EnumSet.of(MaintenanceFrequency.AS_NEEDED, MaintenanceFrequency.IRREGULAR);

    // Class Variables
    protected String fileName;
    protected String datasetID;
    protected String description;
    protected boolean compressionFlag;
    protected boolean dataProtection;
    protected S100ProtectionScheme protectionScheme;
    protected S100SEDigitalSignatureReference digitalSignatureReference;
    protected List<S100DatasetDiscoveryMetadata.DigitalSignatureValue> digitalSignatureValues;
    protected boolean copyright;
    protected SecurityClassification classification;
    protected S100Purpose purpose;
    protected boolean notForNavigation;
    protected String specificUsage;
    protected BigInteger editionNumber;
    protected BigInteger updateNumber;
    protected LocalDate updateApplicationDate;
    protected LocalDate issueDate;
    protected LocalTime issueTime;
    protected Geometry boundingBox;
    protected LocalDateTime timeInstantBegin;
    protected LocalDateTime timeInstantEnd;
    protected S100ProductSpecification productSpecification;
    protected String producingAgency;
    protected RoleCode producingAgencyRole;
    protected String producingAgencyPhone;
    protected TelephoneType producingAgencyPhoneType;
    protected List<String> producingAgencyElectronicMailAddresses;
    protected String producingAgencyCity;
    protected String producingAgencyAdministrativeArea;
    protected String producingAgencyPostalCode;
    protected String producingAgencyCountry;
    protected String producingAgencyOnlineResource;
    protected String producingAgencyContactInstructions;
    protected String producerCode;
    protected S100EncodingFormat encodingFormat;
    protected Geometry dataCoverages;
    protected String comment;
    protected LocalDate metadataDateStamp;
    protected boolean replacedData;
    protected List<S100NavigationPurpose> navigationPurposes;
    protected MaintenanceFrequency maintenanceFrequency;
    protected LocalDate maintenanceDate;
    protected Duration maintenancePeriod;

    // Objects Factories
    private final ObjectFactory objectFactory;
    private final org.iso.standards.iso._19115.__3.cit._2.ObjectFactory citObjectFactory;

    // Signature Provider
    private final S100ExchangeSetSignatureProvider signatureProvider;

    /**
     * Class Constructor.
     *
     * @param signatureProvider the signature provider for the exchange set dataset discovery metadata
     */
    public S100DatasetDiscoveryMetadataBuilder(S100ExchangeSetSignatureProvider signatureProvider) {
        this.signatureProvider = signatureProvider;

        // Initialise the object factories
        this.objectFactory = new ObjectFactory();
        this.citObjectFactory = new org.iso.standards.iso._19115.__3.cit._2.ObjectFactory();
    }

    /**
     * Sets file name.
     *
     * @param fileName the file name
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    /**
     * Sets description.
     *
     * @param description the description
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Sets dataset id.
     *
     * @param datasetID the dataset id
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setDatasetID(String datasetID) {
        this.datasetID = datasetID;
        return this;
    }

    /**
     * Sets compression flag.
     *
     * @param compressionFlag the compression flag
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setCompressionFlag(boolean compressionFlag) {
        this.compressionFlag = compressionFlag;
        return this;
    }

    /**
     * Sets data protection.
     *
     * @param dataProtection the data protection
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setDataProtection(boolean dataProtection) {
        this.dataProtection = dataProtection;
        return this;
    }

    /**
     * Sets protection scheme.
     *
     * @param protectionScheme the protection scheme
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setProtectionScheme(S100ProtectionScheme protectionScheme) {
        this.protectionScheme = protectionScheme;
        return this;
    }

    /**
     * Sets digital signature reference.
     *
     * @param digitalSignatureReference the digital signature reference
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setDigitalSignatureReference(S100SEDigitalSignatureReference digitalSignatureReference) {
        this.digitalSignatureReference = digitalSignatureReference;
        return this;
    }

    /**
     * Sets digital signature values.
     *
     * @param digitalSignatureValues the digital signature values
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setDigitalSignatureValues(List<S100DatasetDiscoveryMetadata.DigitalSignatureValue> digitalSignatureValues) {
        this.digitalSignatureValues = digitalSignatureValues;
        return this;
    }

    /**
     * Sets copyright.
     *
     * @param copyright the copyright
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setCopyright(boolean copyright) {
        this.copyright = copyright;
        return this;
    }

    /**
     * Sets classification.
     *
     * @param classification the classification
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setClassification(SecurityClassification classification) {
        this.classification = classification;
        return this;
    }

    /**
     * Sets purpose.
     *
     * @param purpose the purpose
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setPurpose(S100Purpose purpose) {
        this.purpose = purpose;
        return this;
    }

    /**
     * Sets not for navigation.
     *
     * @param notForNavigation the not for navigation
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setNotForNavigation(boolean notForNavigation) {
        this.notForNavigation = notForNavigation;
        return this;
    }

    /**
     * Sets specific usage.
     *
     * @param specificUsage the specific usage
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setSpecificUsage(String specificUsage) {
        this.specificUsage = specificUsage;
        return this;
    }

    /**
     * Sets edition number.
     *
     * @param editionNumber the edition number
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setEditionNumber(BigInteger editionNumber) {
        this.editionNumber = editionNumber;
        return this;
    }

    /**
     * Sets update number.
     *
     * @param updateNumber the update number
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setUpdateNumber(BigInteger updateNumber) {
        this.updateNumber = updateNumber;
        return this;
        
    }

    /**
     * Sets update application date.
     *
     * @param updateApplicationDate the update application date
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setUpdateApplicationDate(LocalDate updateApplicationDate) {
        this.updateApplicationDate = updateApplicationDate;
        return this;
    }

    /**
     * Sets issue date.
     *
     * @param issueDate the issue date
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
        return this;
    }

    /**
     * Sets issue time.
     *
     * @param issueTime the issue time
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setIssueTime(LocalTime issueTime) {
        this.issueTime = issueTime;
        return this;
    }

    /**
     * Sets bounding box.
     *
     * @param boundingBox the bounding box
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setBoundingBox(Geometry boundingBox) {
        this.boundingBox = boundingBox;
        return this;
    }

    /**
     * Sets time instant begin. The temporal extent is only encoded if at least
     * one of the time instant begin and end is provided, and the value is
     * encoded as a UTC date-time.
     *
     * @param timeInstantBegin the time instant begin
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setTimeInstantBegin(LocalDateTime timeInstantBegin) {
        this.timeInstantBegin = timeInstantBegin;
        return this;
    }

    /**
     * Sets time instant end. The temporal extent is only encoded if at least
     * one of the time instant begin and end is provided, and the value is
     * encoded as a UTC date-time.
     *
     * @param timeInstantEnd the time instant end
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setTimeInstantEnd(LocalDateTime timeInstantEnd) {
        this.timeInstantEnd = timeInstantEnd;
        return this;
    }

    /**
     * Sets product specification.
     *
     * @param productSpecification the product specification
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setProductSpecification(S100ProductSpecification productSpecification) {
        this.productSpecification = productSpecification;
        return this;
    }

    /**
     * Sets producing agency.
     *
     * @param producingAgency the producing agency
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setProducingAgency(String producingAgency) {
        this.producingAgency = producingAgency;
        return this;
    }

    /**
     * Sets producing agency role.
     *
     * @param producingAgencyRole the producing agency role
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setProducingAgencyRole(RoleCode producingAgencyRole) {
        this.producingAgencyRole = producingAgencyRole;
        return this;
    }

    /**
     * Sets the producing agency phone number. This is one of the contact
     * details that can satisfy the mandatory producing agency contact
     * information.
     *
     * @param producingAgencyPhone the producing agency phone number
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setProducingAgencyPhone(String producingAgencyPhone) {
        this.producingAgencyPhone = producingAgencyPhone;
        return this;
    }

    /**
     * Sets the producing agency phone number type.
     *
     * @param producingAgencyPhoneType the producing agency phone number type
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setProducingAgencyPhoneType(TelephoneType producingAgencyPhoneType) {
        this.producingAgencyPhoneType = producingAgencyPhoneType;
        return this;
    }

    /**
     * Sets the producing agency electronic mail addresses. These are part of
     * the producing agency address, which is one of the contact details that
     * can satisfy the mandatory producing agency contact information.
     *
     * @param producingAgencyElectronicMailAddresses the producing agency electronic mail addresses
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setProducingAgencyElectronicMailAddresses(List<String> producingAgencyElectronicMailAddresses) {
        this.producingAgencyElectronicMailAddresses = producingAgencyElectronicMailAddresses;
        return this;
    }

    /**
     * Sets the producing agency city.
     *
     * @param producingAgencyCity the producing agency city
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setProducingAgencyCity(String producingAgencyCity) {
        this.producingAgencyCity = producingAgencyCity;
        return this;
    }

    /**
     * Sets the producing agency administrative area.
     *
     * @param producingAgencyAdministrativeArea the producing agency administrative area
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setProducingAgencyAdministrativeArea(String producingAgencyAdministrativeArea) {
        this.producingAgencyAdministrativeArea = producingAgencyAdministrativeArea;
        return this;
    }

    /**
     * Sets the producing agency postal code.
     *
     * @param producingAgencyPostalCode the producing agency postal code
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setProducingAgencyPostalCode(String producingAgencyPostalCode) {
        this.producingAgencyPostalCode = producingAgencyPostalCode;
        return this;
    }

    /**
     * Sets the producing agency country.
     *
     * @param producingAgencyCountry the producing agency country
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setProducingAgencyCountry(String producingAgencyCountry) {
        this.producingAgencyCountry = producingAgencyCountry;
        return this;
    }

    /**
     * Sets the producing agency online resource linkage. This is one of the
     * contact details that can satisfy the mandatory producing agency contact
     * information.
     *
     * @param producingAgencyOnlineResource the producing agency online resource linkage
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setProducingAgencyOnlineResource(String producingAgencyOnlineResource) {
        this.producingAgencyOnlineResource = producingAgencyOnlineResource;
        return this;
    }

    /**
     * Sets the producing agency contact instructions. This is one of the
     * contact details that can satisfy the mandatory producing agency contact
     * information.
     *
     * @param producingAgencyContactInstructions the producing agency contact instructions
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setProducingAgencyContactInstructions(String producingAgencyContactInstructions) {
        this.producingAgencyContactInstructions = producingAgencyContactInstructions;
        return this;
    }

    /**
     * Sets producer code.
     *
     * @param producerCode the producer code
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setProducerCode(String producerCode) {
        this.producerCode = producerCode;
        return this;
    }

    /**
     * Sets encoding format.
     *
     * @param encodingFormat the encoding format
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setEncodingFormat(S100EncodingFormat encodingFormat) {
        this.encodingFormat = encodingFormat;
        return this;
    }

    /**
     * Sets data coverages.
     *
     * @param dataCoverages the data coverages
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setDataCoverages(Geometry dataCoverages) {
        this.dataCoverages = dataCoverages;
        return this;
    }

    /**
     * Sets comment.
     *
     * @param comment the comment
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setComment(String comment) {
        this.comment = comment;
        return this;
    }

    /**
     * Sets metadata date stamp.
     *
     * @param metadataDateStamp the metadata date stamp
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setMetadataDateStamp(LocalDate metadataDateStamp) {
        this.metadataDateStamp = metadataDateStamp;
        return this;
    }

    /**
     * Sets replaced data.
     *
     * @param replacedData the replaced data
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setReplacedData(boolean replacedData) {
        this.replacedData = replacedData;
        return this;
    }

    /**
     * Sets navigation purposes.
     *
     * @param navigationPurposes the navigation purposes
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setNavigationPurposes(List<S100NavigationPurpose> navigationPurposes) {
        this.navigationPurposes = navigationPurposes;
        return this;
    }

    /**
     * Sets maintenance frequency. S-100 restricts the ISO 19115-1
     * MD_MaintenanceFrequencyCode codelist used in the discovery metadata to
     * the "asNeeded" and "irregular" values only, hence any other value will
     * be rejected.
     *
     * @param maintenanceFrequency the maintenance frequency
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setMaintenanceFrequency(MaintenanceFrequency maintenanceFrequency) {
        if(Objects.nonNull(maintenanceFrequency) && !ALLOWED_MAINTENANCE_FREQUENCIES.contains(maintenanceFrequency)) {
            throw new IllegalArgumentException(String.format("The maintenance frequency %s is not "
                    + "allowed in the S-100 discovery metadata (S-100 Part 17, "
                    + "MD_MaintenanceFrequencyCode: S-100 is restricted to only the asNeeded and "
                    + "irregular values from the ISO 19115-1 codelist)", maintenanceFrequency.getValue()));
        }
        this.maintenanceFrequency = maintenanceFrequency;
        return this;
    }

    /**
     * Sets maintenance date.
     *
     * @param maintenanceDate the maintenance date
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setMaintenanceDate(LocalDate maintenanceDate) {
        this.maintenanceDate = maintenanceDate;
        return this;
    }

    /**
     * Sets maintenance period.
     *
     * @param maintenancePeriod the maintenance period
     * @return the S-100 dataset discovery metadata builder
     */
    public S100DatasetDiscoveryMetadataBuilder setMaintenancePeriod(Duration maintenancePeriod) {
        this.maintenancePeriod = maintenancePeriod;
        return this;
    }

    /**
     * The main building function of the S-100 exchange set dataset discovery
     * metadata object.
     *
     * @return the built S-100 exchange set dataset discovery metadata object
     */
    public S100DatasetDiscoveryMetadata build(byte[] payload) {
        // Create the metadata object
        final S100DatasetDiscoveryMetadata metadata = new S100DatasetDiscoveryMetadata();

        // Assign the variables
        metadata.setFileName(this.fileName);
        metadata.setDatasetID(this.datasetID);
        metadata.setDescription(S100ExchangeSetUtils.createCharacterStringPropertyType(this.description));
        metadata.setCompressionFlag(this.compressionFlag);
        metadata.setDataProtection(this.dataProtection);
        // the protection scheme is optional (0..1) and only describes the
        // method used when the data is actually protected
        metadata.setProtectionScheme(this.protectionScheme);
        metadata.setCopyright(this.copyright);
        final S100ClassificationCodePropertyType s100ClassificationCodePropertyType = new S100ClassificationCodePropertyType();
        s100ClassificationCodePropertyType.setMDClassificationCode(Optional.ofNullable(this.classification)
                .map(CodeListValueTypeProvider::getCodeListValueType)
                .orElse(null));
        metadata.setClassification(s100ClassificationCodePropertyType);
        metadata.setPurpose(this.purpose);
        metadata.setNotForNavigation(this.notForNavigation);
        if(Objects.nonNull(this.specificUsage)) {
            final S100UsagePropertyType s100UsagePropertyType = new S100UsagePropertyType();
            final MDUsageType mdUsageType = new MDUsageType();
            mdUsageType.setSpecificUsage(S100ExchangeSetUtils.createCharacterStringPropertyType(this.specificUsage));
            s100UsagePropertyType.setMDUsage(mdUsageType);
            metadata.setSpecificUsage(s100UsagePropertyType);
        }
        metadata.setEditionNumber(this.editionNumber);
        metadata.setUpdateNumber(this.updateNumber);
        metadata.setUpdateApplicationDate(this.updateApplicationDate);
        metadata.setIssueDate(this.issueDate);
        metadata.setIssueTime(this.issueTime);
        metadata.setBoundingBox(S100ExchangeSetUtils.createS100GeographicBoundingBoxType(this.boundingBox));
        // the temporal extent is encoded if and only if at least one of the
        // start and the end of the extent is known
        if(Objects.nonNull(this.timeInstantBegin) || Objects.nonNull(this.timeInstantEnd)) {
            final S100TemporalExtent temporalExtent = new S100TemporalExtent();
            temporalExtent.setTimeInstantBegin(this.timeInstantBegin);
            temporalExtent.setTimeInstantEnd(this.timeInstantEnd);
            metadata.setTemporalExtent(temporalExtent);
        }
        metadata.setProductSpecification(this.productSpecification);
        metadata.setProducerCode(this.producerCode);
        S100EncodingFormatPropertyType s100EncodingFormatPropertyType = new S100EncodingFormatPropertyType();
        s100EncodingFormatPropertyType.setValue(this.encodingFormat);
        metadata.setDataCoverages(S100ExchangeSetUtils.createS100DataCoverages(this.dataCoverages));
        metadata.setEncodingFormat(s100EncodingFormatPropertyType);
        metadata.setComment(S100ExchangeSetUtils.createCharacterStringPropertyType(this.comment));
        metadata.setReplacedData(this.replacedData);
        metadata.setNavigationPurposes(this.navigationPurposes);

        // Responsible Authority - the producing agency is mandatory (S-100
        // Part 17, S100_DatasetDiscoveryMetadata: producingAgency Mult 1)
        if(Objects.isNull(this.producingAgency)) {
            throw new IllegalStateException("The producing agency of the dataset must be provided "
                    + "(S-100 Part 17, S100_DatasetDiscoveryMetadata: producingAgency multiplicity 1)");
        }
        final CIResponsibilityPropertyType ciResponsibilityPropertyType = new CIResponsibilityPropertyType();
        final CIResponsibilityType ciResponsibilityType = new CIResponsibilityType();
        final CIOrganisationType ciOrganisationType = new CIOrganisationType();
        ciOrganisationType.setName(S100ExchangeSetUtils.createCharacterStringPropertyType(this.producingAgency));
        // the organisation contact information is mandatory (S-100 Part 17,
        // Table 17-3) and NOTE 2 requires at least one of the phone, address,
        // onlineResource and contactInstructions attributes to be documented
        final CIContactType ciContactType = this.createProducingAgencyContact();
        if(Objects.isNull(ciContactType)) {
            throw new IllegalStateException("The contact information of the producing agency must be "
                    + "provided (S-100 Part 17, Table 17-3: CI_Organisation.contactInfo is mandatory "
                    + "and NOTE 2 requires at least one of the CI_Contact phone, address, "
                    + "onlineResource and contactInstructions attributes to be documented)");
        }
        final CIContactPropertyType ciContactPropertyType = new CIContactPropertyType();
        ciContactPropertyType.setCIContact(ciContactType);
        ciOrganisationType.setContactInfos(Collections.singletonList(ciContactPropertyType));
        final AbstractCIPartyPropertyType abstractCIPartyPropertyType = new AbstractCIPartyPropertyType();
        abstractCIPartyPropertyType.setAbstractCIParty(this.citObjectFactory.createCIOrganisation(ciOrganisationType));
        ciResponsibilityType.setParties(Collections.singletonList(abstractCIPartyPropertyType));
        if(Objects.nonNull(this.producingAgencyRole)) {
            final CIRoleCodePropertyType ciRoleCodePropertyType = new CIRoleCodePropertyType();
            ciRoleCodePropertyType.setCIRoleCode(this.producingAgencyRole.getCodeListValueType());
            ciResponsibilityType.setRole(ciRoleCodePropertyType);
        }
        ciResponsibilityPropertyType.setCIResponsibility(ciResponsibilityType);
        metadata.setProducingAgency(ciResponsibilityPropertyType);

        // Maintenance information - the resourceMaintenance role is optional
        // (0..1) so it is only encoded when maintenance information is provided
        if(Objects.nonNull(this.maintenanceFrequency) || Objects.nonNull(this.maintenanceDate) || Objects.nonNull(this.maintenancePeriod)) {
            // S-100 Part 17, MD_MaintenanceInformation: "Exactly one of
            // maintenanceDate and userDefinedMaintenanceFrequency must be
            // populated"
            if(Objects.nonNull(this.maintenanceDate) == Objects.nonNull(this.maintenancePeriod)) {
                throw new IllegalStateException("Exactly one of the maintenance date and the maintenance "
                        + "period must be provided for the resource maintenance information (S-100 Part 17, "
                        + "MD_MaintenanceInformation: exactly one of maintenanceDate and "
                        + "userDefinedMaintenanceFrequency must be populated)");
            }
            // S-100 Part 17, MD_MaintenanceInformation: the maintenance
            // frequency "must be populated if userDefinedMaintenanceFrequency
            // is not present", while asNeeded and irregular are "allowed if
            // and only if userDefinedMaintenanceFrequency is not populated"
            if(Objects.nonNull(this.maintenanceDate) && Objects.isNull(this.maintenanceFrequency)) {
                throw new IllegalStateException("The maintenance frequency must be provided alongside the "
                        + "maintenance date (S-100 Part 17, MD_MaintenanceInformation: "
                        + "maintenanceAndUpdateFrequency must be populated if "
                        + "userDefinedMaintenanceFrequency is not present)");
            }
            if(Objects.nonNull(this.maintenancePeriod) && Objects.nonNull(this.maintenanceFrequency)) {
                throw new IllegalStateException("The maintenance frequency must not be provided alongside "
                        + "the maintenance period (S-100 Part 17, MD_MaintenanceFrequencyCode: asNeeded "
                        + "and irregular are allowed if and only if userDefinedMaintenanceFrequency is "
                        + "not populated)");
            }

            final MDMaintenanceInformationPropertyType mdMaintenanceInformationPropertyType = new MDMaintenanceInformationPropertyType();
            final MDMaintenanceInformationType mdMaintenanceInformationType = new MDMaintenanceInformationType();
            if(Objects.nonNull(this.maintenanceFrequency)) {
                final MDMaintenanceFrequencyCodePropertyType maintenanceAndUpdateFrequency = new MDMaintenanceFrequencyCodePropertyType();
                maintenanceAndUpdateFrequency.setMDMaintenanceFrequencyCode(this.maintenanceFrequency.getCodeListValueType());
                mdMaintenanceInformationType.setMaintenanceAndUpdateFrequency(maintenanceAndUpdateFrequency);
            }
            if(Objects.nonNull(this.maintenanceDate)) {
                final AbstractTypedDatePropertyType abstractTypedDatePropertyType = new AbstractTypedDatePropertyType();
                final CIDateType ciDateType = new CIDateType();
                final DatePropertyType datePropertyType = new DatePropertyType();
                datePropertyType.setDate(this.maintenanceDate);
                ciDateType.setDate(datePropertyType);
                // the dateType child is mandatory for cit:CI_Date
                final CIDateTypeCodePropertyType ciDateTypeCodePropertyType = new CIDateTypeCodePropertyType();
                ciDateTypeCodePropertyType.setCIDateTypeCode(S100ExchangeSetUtils.createCodeListValueType(
                        "https://standards.iso.org/iso/19115/resources/Codelists/cat/codelists.xml",
                        null,
                        "nextUpdate",
                        "nextUpdate"));
                ciDateType.setDateType(ciDateTypeCodePropertyType);
                // use the concrete cit:CI_Date element - the substitution group
                // head mcc:Abstract_TypedDate is abstract and may not appear in
                // instance documents
                abstractTypedDatePropertyType.setAbstractTypedDate(
                        this.citObjectFactory.createCIDate(ciDateType)
                );
                mdMaintenanceInformationType.setMaintenanceDates(Collections.singletonList(abstractTypedDatePropertyType));
            }
            if(Objects.nonNull(this.maintenancePeriod)) {
                final TMPeriodDurationPropertyType userDefinedMaintenanceFrequency = new TMPeriodDurationPropertyType();
                userDefinedMaintenanceFrequency.setTMPeriodDuration(this.maintenancePeriod);
                mdMaintenanceInformationType.setUserDefinedMaintenanceFrequency(userDefinedMaintenanceFrequency);
            }
            mdMaintenanceInformationPropertyType.setMDMaintenanceInformation(mdMaintenanceInformationType);
            metadata.setResourceMaintenance(mdMaintenanceInformationPropertyType);
        }

        // Set the metadata date-stamp
        metadata.setMetadataDateStamp(Optional.ofNullable(this.metadataDateStamp)
                .orElse(this.issueDate));

        //====================================================================//
        //                        METADATA SIGNATURES                         //
        //====================================================================//
        // First choose the signature reference to be used - S-100 Part 15
        // clause 15-8.7 mandates the "ECDSA-384-SHA2" encoding
        final S100SEDigitalSignatureReference signatureReference = Optional.ofNullable(this.digitalSignatureReference)
                .orElse(S100SEDigitalSignatureReference.ECDSA_384_SHA_2);
        // And populate the metadata
        final S100SEDigitalSignatureReferencePropertyType digitalSignatureReferencePropertyType = new S100SEDigitalSignatureReferencePropertyType();
        digitalSignatureReferencePropertyType.setValue(signatureReference);
        metadata.setDigitalSignatureReference(digitalSignatureReferencePropertyType);

        // Sign the dataset file if a provider detected
        if(Objects.nonNull(this.signatureProvider) && Objects.nonNull(payload)) {
            // Generate the signature
            final S100SEDigitalSignature signature = this.signatureProvider.generateSignature(
                    this.fileName,
                    signatureReference,
                    payload);

            // And add it to the metadata
            final S100DatasetDiscoveryMetadata.DigitalSignatureValue digitalSignatureValue = new S100DatasetDiscoveryMetadata.DigitalSignatureValue();
            digitalSignatureValue.setS100SEDigitalSignature(this.objectFactory.createS100SEDigitalSignature(signature));
            metadata.getDigitalSignatureValues().add(digitalSignatureValue);
        }
        // Or use the existing signatures if provided
        else if(Objects.nonNull(this.digitalSignatureValues) && !this.digitalSignatureValues.isEmpty()) {
            metadata.getDigitalSignatureValues().addAll(this.digitalSignatureValues);
        }
        // Otherwise the mandatory signature information cannot be generated
        else {
            throw new IllegalStateException("The dataset discovery metadata requires at least one digital "
                    + "signature value (S-100 Part 17, S100_DatasetDiscoveryMetadata: digitalSignatureValue "
                    + "multiplicity 1..*), so either a signature provider along with the dataset payload, or "
                    + "the already generated signature values must be provided");
        }
        //====================================================================//

        // And return the constructed metadata object
        return metadata;
    }

    /**
     * Constructs the contact information of the producing agency organisation
     * based on the contact details provided to the builder. If none of the
     * S-100 Part 17 Table 17-3 NOTE 2 contact details (phone, address,
     * onlineResource and contactInstructions) is available, no contact
     * information can be generated and a null value will be returned.
     *
     * @return the producing agency contact information, or null if not enough details are available
     */
    protected CIContactType createProducingAgencyContact() {
        final CIContactType ciContactType = new CIContactType();
        boolean documented = false;

        // The phone details - the number is mandatory for cit:CI_Telephone
        if(Objects.nonNull(this.producingAgencyPhone)) {
            final CITelephoneType ciTelephoneType = new CITelephoneType();
            ciTelephoneType.setNumber(S100ExchangeSetUtils.createCharacterStringPropertyType(this.producingAgencyPhone));
            if(Objects.nonNull(this.producingAgencyPhoneType)) {
                final CITelephoneTypeCodePropertyType ciTelephoneTypeCodePropertyType = new CITelephoneTypeCodePropertyType();
                ciTelephoneTypeCodePropertyType.setCITelephoneTypeCode(this.producingAgencyPhoneType.getCodeListValueType());
                ciTelephoneType.setNumberType(ciTelephoneTypeCodePropertyType);
            }
            final CITelephonePropertyType ciTelephonePropertyType = new CITelephonePropertyType();
            ciTelephonePropertyType.setCITelephone(ciTelephoneType);
            ciContactType.getPhones().add(ciTelephonePropertyType);
            documented = true;
        }

        // The address details
        if((Objects.nonNull(this.producingAgencyElectronicMailAddresses) && !this.producingAgencyElectronicMailAddresses.isEmpty())
                || Objects.nonNull(this.producingAgencyCity)
                || Objects.nonNull(this.producingAgencyAdministrativeArea)
                || Objects.nonNull(this.producingAgencyPostalCode)
                || Objects.nonNull(this.producingAgencyCountry)) {
            final CIAddressType ciAddressType = new CIAddressType();
            ciAddressType.setElectronicMailAddresses(S100ExchangeSetUtils.createCharacterStringPropertyTypeList(this.producingAgencyElectronicMailAddresses));
            ciAddressType.setCity(S100ExchangeSetUtils.createCharacterStringPropertyType(this.producingAgencyCity));
            ciAddressType.setAdministrativeArea(S100ExchangeSetUtils.createCharacterStringPropertyType(this.producingAgencyAdministrativeArea));
            ciAddressType.setPostalCode(S100ExchangeSetUtils.createCharacterStringPropertyType(this.producingAgencyPostalCode));
            ciAddressType.setCountry(S100ExchangeSetUtils.createCharacterStringPropertyType(this.producingAgencyCountry));
            final CIAddressPropertyType ciAddressPropertyType = new CIAddressPropertyType();
            ciAddressPropertyType.setCIAddress(ciAddressType);
            ciContactType.getAddresses().add(ciAddressPropertyType);
            documented = true;
        }

        // The online resource details
        if(Objects.nonNull(this.producingAgencyOnlineResource)) {
            final CIOnlineResourceType ciOnlineResourceType = new CIOnlineResourceType();
            ciOnlineResourceType.setLinkage(S100ExchangeSetUtils.createCharacterStringPropertyType(this.producingAgencyOnlineResource));
            final CIOnlineResourcePropertyType ciOnlineResourcePropertyType = new CIOnlineResourcePropertyType();
            ciOnlineResourcePropertyType.setCIOnlineResource(ciOnlineResourceType);
            ciContactType.getOnlineResources().add(ciOnlineResourcePropertyType);
            documented = true;
        }

        // And the contact instructions
        if(Objects.nonNull(this.producingAgencyContactInstructions)) {
            ciContactType.setContactInstructions(S100ExchangeSetUtils.createCharacterStringPropertyType(this.producingAgencyContactInstructions));
            documented = true;
        }

        // Only return the contact if at least one of the details was documented
        return documented ? ciContactType : null;
    }
}
