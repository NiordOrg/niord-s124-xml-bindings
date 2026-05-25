package dk.dma.niord.s100.xmlbindings.s124.v2_0_0.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;

import org.junit.jupiter.api.Test;

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
}
