package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util;

import java.math.BigInteger;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.DataSetIdentificationType;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.DatasetPurposeType;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.MDTopicCategoryCode;
import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.DataSetIdentificationTypeImpl;
import dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.ReferenceType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.Dataset;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.FixedDateRangeType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.GeneralAreaType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.InformationType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.LocalityType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.LocationNameType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.MessageSeriesIdentifierType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnPart;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnPreamble;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnTypeGeneralLabel;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.NavwarnTypeGeneralType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.ObjectFactory;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.WarningInformationType;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.WarningTypeLabel;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.WarningTypeType;

/**
 * Conformant S-124 datasets for tests to start from, so each test states only the one thing it is
 * about. Everything built here validates against the S-124 GML schema and passes
 * {@link S124DatasetValidator}; a test that wants a defect introduces it explicitly.
 */
final class S124TestDatasets {

    private S124TestDatasets() {
    }

    /** A dataset carrying one conformant NavwarnPreamble and no NavwarnPart. */
    static Dataset datasetWithPreamble() {
        ObjectFactory of = new ObjectFactory();
        Dataset dataset = of.createDataset();
        dataset.setId("DK.S124.test");
        dataset.setDatasetIdentificationInformation(identification());
        dataset.setMembers(of.createDatasetMembers());
        dataset.getMembers().getNavwarnPartsAndNavwarnAreaAffectedsAndTextPlacements().add(preamble());
        return dataset;
    }

    /** The dataset's single preamble. */
    static NavwarnPreamble preambleOf(Dataset dataset) {
        return dataset.getMembers().getNavwarnPartsAndNavwarnAreaAffectedsAndTextPlacements().stream()
                .filter(NavwarnPreamble.class::isInstance)
                .map(NavwarnPreamble.class::cast)
                .findFirst()
                .orElseThrow();
    }

    /** A second, equally conformant preamble - the one thing S-124 clause 4 forbids. */
    static void addSecondPreamble(Dataset dataset) {
        NavwarnPreamble second = preamble();
        second.setId("PR.2");
        dataset.getMembers().getNavwarnPartsAndNavwarnAreaAffectedsAndTextPlacements().add(second);
    }

    /** Adds a NavwarnPart whose fixedDateRange carries the given times of day. */
    static FixedDateRangeType addPartWithTimeOfDay(Dataset dataset, XMLGregorianCalendar start,
            XMLGregorianCalendar end) {
        ObjectFactory of = new ObjectFactory();
        NavwarnPart part = of.createNavwarnPart();
        part.setId("NW.1");
        InformationType information = of.createInformationType();
        information.setLanguage("eng");
        information.setText("Test warning information");
        WarningInformationType warningInformation = of.createWarningInformationType();
        warningInformation.getInformations().add(information);
        part.setWarningInformation(warningInformation);
        ReferenceType header = new dk.dma.niord.s100.xmlbindings.s100.gml.profiles._5_0.ObjectFactory()
                .createReferenceType();
        header.setHref("#PR.1");
        part.setHeader(header);
        FixedDateRangeType range = of.createFixedDateRangeType();
        range.setTimeOfDayStart(start);
        range.setTimeOfDayEnd(end);
        part.getFixedDateRanges().add(range);
        dataset.getMembers().getNavwarnPartsAndNavwarnAreaAffectedsAndTextPlacements().add(part);
        return range;
    }

    /** A time of day with a UTC designator, as S-124 clause 4.3.3 requires. */
    static XMLGregorianCalendar utcTime(int hour, int minute) {
        return time(hour, minute, 0);
    }

    /** A time of day with no timezone designator at all - the defect clause 4.3.3 forbids. */
    static XMLGregorianCalendar floatingTime(int hour, int minute) {
        return time(hour, minute, DatatypeConstants.FIELD_UNDEFINED);
    }

    /** A time of day at a non-UTC offset, given in minutes. */
    static XMLGregorianCalendar offsetTime(int hour, int minute, int offsetMinutes) {
        return time(hour, minute, offsetMinutes);
    }

    private static XMLGregorianCalendar time(int hour, int minute, int offsetMinutes) {
        try {
            return DatatypeFactory.newInstance()
                    .newXMLGregorianCalendarTime(hour, minute, 0, offsetMinutes);
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static NavwarnPreamble preamble() {
        ObjectFactory of = new ObjectFactory();
        NavwarnPreamble preamble = of.createNavwarnPreamble();
        preamble.setId("PR.1");
        preamble.getGeneralAreas().add(generalArea("The Sound", "Sundet"));
        preamble.getLocalities().add(locality("Drogden Channel", "Drogden"));
        preamble.setMessageSeriesIdentifier(messageSeriesIdentifier());
        preamble.setIntService(true);
        NavwarnTypeGeneralType typeGeneral = of.createNavwarnTypeGeneralType();
        typeGeneral.setValue(NavwarnTypeGeneralLabel.OTHER_HAZARDS);
        typeGeneral.setCode(BigInteger.valueOf(5));
        preamble.setNavwarnTypeGeneral(typeGeneral);
        preamble.setPublicationTime(OffsetDateTime.of(2026, 8, 20, 6, 45, 0, 0, ZoneOffset.UTC));
        return preamble;
    }

    private static MessageSeriesIdentifierType messageSeriesIdentifier() {
        ObjectFactory of = new ObjectFactory();
        WarningTypeType warningType = of.createWarningTypeType();
        warningType.setValue(WarningTypeLabel.COASTAL_NAVIGATIONAL_WARNING);
        warningType.setCode(BigInteger.TWO);
        MessageSeriesIdentifierType series = of.createMessageSeriesIdentifierType();
        series.setAgencyResponsibleForProduction("DK00");
        series.setNameOfSeries("Danish Nav. Warn.");
        series.setNationality("DK");
        series.setWarningNumber(11);
        series.setYear(2026);
        series.setWarningType(warningType);
        return series;
    }

    private static GeneralAreaType generalArea(String english, String danish) {
        GeneralAreaType area = new ObjectFactory().createGeneralAreaType();
        area.getLocationNames().add(locationName("eng", english));
        area.getLocationNames().add(locationName("dan", danish));
        return area;
    }

    private static LocalityType locality(String english, String danish) {
        LocalityType locality = new ObjectFactory().createLocalityType();
        locality.getLocationNames().add(locationName("eng", english));
        locality.getLocationNames().add(locationName("dan", danish));
        return locality;
    }

    private static LocationNameType locationName(String language, String text) {
        LocationNameType name = new ObjectFactory().createLocationNameType();
        name.setLanguage(language);
        name.setText(text);
        return name;
    }

    private static DataSetIdentificationType identification() {
        DataSetIdentificationTypeImpl ident = new DataSetIdentificationTypeImpl();
        ident.setEncodingSpecification("S-100 Part 10b");
        ident.setEncodingSpecificationEdition("1.0");
        ident.setProductIdentifier("S-124");
        ident.setProductEdition("2.0.0");
        ident.setApplicationProfile(S124DatasetInfo.BASE_APPLICATION_PROFILE);
        ident.setDatasetFileIdentifier("124DK00DKS124test.GML");
        ident.setDatasetTitle("Test S-124 Dataset");
        ident.setDatasetReferenceDate(LocalDate.of(2026, 8, 25));
        ident.setDatasetLanguage("eng");
        ident.setDatasetAbstract("Synthetic dataset used for unit tests");
        ident.getDatasetTopicCategories().add(MDTopicCategoryCode.OCEANS);
        ident.setDatasetPurpose(DatasetPurposeType.BASE);
        ident.setUpdateNumber(BigInteger.ZERO);
        return ident;
    }
}
