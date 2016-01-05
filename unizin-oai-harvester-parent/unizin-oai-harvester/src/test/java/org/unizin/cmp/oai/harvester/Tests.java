package org.unizin.cmp.oai.harvester;

import java.util.regex.Pattern;

import org.unizin.cmp.oai.OAIVerb;

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

    /** No instances allowed. */
    private Tests() { }
}
