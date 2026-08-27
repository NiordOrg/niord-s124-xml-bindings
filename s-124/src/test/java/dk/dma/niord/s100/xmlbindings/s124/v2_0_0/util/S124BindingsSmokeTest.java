package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import dk.dma.niord.s100.xmlbindings.s100.gml.base._5_0.impl.DataSetIdentificationTypeImpl;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.Dataset;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.ObjectFactory;
import dk.dma.niord.s100.xmlbindings.s124.v2_0_0.impl.DatasetImpl;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;

/**
 * Confirms the generated bindings are on the classpath and that a {@link Dataset} can be
 * marshalled with Jakarta XML Bind.
 */
class S124BindingsSmokeTest {

    @Test
    void marshalEmptyDataset() throws Exception {
        ObjectFactory factory = new ObjectFactory();
        Dataset dataset = factory.createDataset();
        dataset.setId("DK.S124.smoke");

        JAXBContext ctx = JAXBContext.newInstance(DatasetImpl.class);
        Marshaller marshaller = ctx.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        StringWriter out = new StringWriter();
        marshaller.marshal(dataset, out);

        String xml = out.toString();
        assertThat(xml).contains("DK.S124.smoke");
        assertThat(xml).contains("Dataset");
    }

    /**
     * A dataset marshalled by {@link S124Utils#marshalS124} (which emits
     * XSD-conformant extended ISO dates) must unmarshal back through
     * {@link S124Utils#unmarshallS124}. This used to fail because the default
     * {@code DateAdapter} only accepted the legacy basic date form.
     */
    @Test
    void marshalledDatasetRoundTrips() throws Exception {
        ObjectFactory factory = new ObjectFactory();
        Dataset dataset = factory.createDataset();
        dataset.setId("DK.S124.round-trip");

        DataSetIdentificationTypeImpl ident = new DataSetIdentificationTypeImpl();
        ident.setDatasetFileIdentifier("DK.S124.round-trip");
        ident.setDatasetReferenceDate(LocalDate.of(2026, 1, 15));
        dataset.setDatasetIdentificationInformation(ident);

        // Validation off: this fixture exists to exercise the date adapters, not to be a
        // conformant warning, and it deliberately carries nothing but the identification block.
        String xml = S124Utils.marshalS124(dataset, true, false);
        assertThat(xml).contains("datasetReferenceDate>2026-01-15</");

        Dataset roundTripped = S124Utils.unmarshallS124(xml);
        assertThat(roundTripped.getDatasetIdentificationInformation().getDatasetReferenceDate())
                .isEqualTo(LocalDate.of(2026, 1, 15));
    }
}
