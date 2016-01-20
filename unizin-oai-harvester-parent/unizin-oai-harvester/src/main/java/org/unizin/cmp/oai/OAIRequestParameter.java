package org.unizin.cmp.oai;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Enumeration of all valid OAI-PMH request parameters, regardless of verb.
 *
 */
public enum OAIRequestParameter {
    FROM("from"),
    IDENTIFIER("identifier"),
    METADATA_PREFIX("metadataPrefix"),
    RESUMPTION_TOKEN("resumptionToken"),
    SET("set"),
    UNTIL("until");


    private final String param;
    private OAIRequestParameter(final String param) {
        this.param = param;
    }

    private static final Map<String, OAIRequestParameter> PARAM_TO_ENUM;
    static {
        final OAIRequestParameter[] values = OAIRequestParameter.values();
        final Map<String, OAIRequestParameter> m = new HashMap<>(values.length);
        Arrays.stream(values).forEach(v -> {
            m.put(v.paramName(), v);
        });
        PARAM_TO_ENUM = Collections.unmodifiableMap(m);
    }

    public static OAIRequestParameter fromParam(final String param) {
        return PARAM_TO_ENUM.get(param);
    }

    public String paramName() {
        return this.param;
    }
}
