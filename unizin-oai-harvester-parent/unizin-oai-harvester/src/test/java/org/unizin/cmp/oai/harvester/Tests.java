package org.unizin.cmp.oai.harvester;

import java.io.OutputStream;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;

import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.OAIXMLUtils;
import org.unizin.cmp.oai.harvester.response.EventWriterOAIEventHandler;
import org.unizin.cmp.oai.harvester.response.OAIEventHandler;

public class Tests {
    public static StAXImplementation STAX =
            StAXImplementation.getImplementation();

    public static final OAIVerb DEFAULT_VERB = OAIVerb.LIST_RECORDS;


    public static HarvestParams.Builder newParams() {
        return new HarvestParams.Builder(WireMockUtils.MOCK_OAI_BASE_URI,
                DEFAULT_VERB);
    }

    public static HarvestParams.Builder newParams(final OAIVerb verb) {
        return new HarvestParams.Builder(WireMockUtils.MOCK_OAI_BASE_URI, verb);
    }

    public static final OAIEventHandler simpleMergingHandler(
            final OutputStream out)
            throws XMLStreamException {
        final XMLEventWriter eventWriter = OAIXMLUtils.newOutputFactory()
                .createXMLEventWriter(out);
        return new EventWriterOAIEventHandler(eventWriter);
    }


    /** No instances allowed. */
    private Tests() { }
}
