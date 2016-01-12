package org.unizin.cmp.oai.harvester;

import java.io.OutputStream;
import java.util.regex.Pattern;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;

import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.OAIXMLUtils;
import org.unizin.cmp.oai.harvester.response.EventWriterOAIEventHandler;
import org.unizin.cmp.oai.harvester.response.OAIEventHandler;

public class Tests {
    public static final OAIVerb DEFAULT_VERB = OAIVerb.LIST_RECORDS;

    private static final String LITERAL_QM = Pattern.quote("?");

    public static final String URL_PATTERN_WITHOUT_RESUMPTION_TOKEN =
            "^.*\\?(?:(?!resumptionToken).)*$";

    public static String urlResmptionTokenPattern(
            final String resumptionToken) {
        return "^.*" + LITERAL_QM + ".*resumptionToken=" +
                Pattern.quote(resumptionToken) + ".*$";
    }

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
        final XMLEventWriter eventWriter = OAIXMLUtils.createEventWriter(
                OAIXMLUtils.newOutputFactory(), out);
        return new EventWriterOAIEventHandler(eventWriter);
    }

    /** No instances allowed. */
    private Tests() { }
}
