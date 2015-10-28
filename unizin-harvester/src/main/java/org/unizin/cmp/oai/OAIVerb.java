package org.unizin.cmp.oai;

import static org.unizin.cmp.oai.OAIRequestParameter.IDENTIFIER;
import static org.unizin.cmp.oai.OAIRequestParameter.METADATA_PREFIX;
import static org.unizin.cmp.oai.OAIRequestParameter.RESUMPTION_TOKEN;
import static org.unizin.cmp.oai.OAIVerbConstants.GET_RECORD_PARAMS;
import static org.unizin.cmp.oai.OAIVerbConstants.LIST_PARAMS;
import static org.unizin.cmp.oai.OAIVerbConstants.EMPTY_LIST;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

/**
 * Enumeration of OAI-PMH verbs.
 *
 */
public enum OAIVerb {
    GET_RECORD(OAI2Constants.GET_RECORD,
            GET_RECORD_PARAMS,
            GET_RECORD_PARAMS),
    IDENTIFY(OAI2Constants.IDENTIFY,
            EMPTY_LIST,
            EMPTY_LIST),
    LIST_IDENTIFIERS(OAI2Constants.LIST_IDENTIFIERS,
            LIST_PARAMS,
            Collections.singletonList(METADATA_PREFIX)),
    LIST_METADATA_FORMATS(OAI2Constants.LIST_METADATA_FORMATS,
            Collections.singletonList(IDENTIFIER),
            EMPTY_LIST),
    LIST_RECORDS(OAI2Constants.LIST_RECORDS,
            LIST_PARAMS,
            Collections.singletonList(METADATA_PREFIX)),
    LIST_SETS(OAI2Constants.LIST_SETS,
            Collections.singletonList(RESUMPTION_TOKEN),
            EMPTY_LIST);

    private static final Collection<QName> ALL_QNAMES =
            Collections.unmodifiableCollection(Arrays.asList(values())
                    .stream()
                    .map(verb -> verb.qname())
                    .collect(Collectors.toList()));

    private final QName qname;
    private final Set<OAIRequestParameter> legalParameters;
    private final Set<OAIRequestParameter> requiredParameters;

    private OAIVerb(final QName qname,
            final List<OAIRequestParameter> legalParameters,
            final List<OAIRequestParameter> requiredParameters) {
        this.qname = qname;
        this.legalParameters = Collections.unmodifiableSet(
                new HashSet<>(legalParameters));
        this.requiredParameters = Collections.unmodifiableSet(
                new HashSet<>(requiredParameters));
    }

    /**
    * @return the QName of the corresponding XML element in the OAI-PMH
    * response for this verb.
    */
    public QName qname() {
        return this.qname;
    }

    /**
    * @return the local name of the corresponding XML element in the OAI-PMH
    * response for this verb.
    */
    public String localPart() {
        return this.qname.getLocalPart();
    }

    public boolean areValidParameters(final Map<String, String> parameters) {
        // Make a copy to avoid altering the map.
        final Set<String> keys = new HashSet<>(parameters.keySet());
        // resumptionToken is an exclusive argument with all verbs where it is
        // allowed.
        if (keys.contains(RESUMPTION_TOKEN.paramName())) {
            if (acceptsParameter(RESUMPTION_TOKEN)) {
                keys.remove(RESUMPTION_TOKEN.paramName());
                return keys.isEmpty();
            }
            return false;
        }
        final Set<String> requiredParamKeys = requiredParameters.stream()
                .map((rp) -> rp.paramName())
                .collect(Collectors.toSet());
        if (keys.containsAll(requiredParamKeys)) {
            legalParameters.stream()
                .forEach((lp) -> keys.remove(lp.paramName()));
            return keys.isEmpty();
        }
        return false;
    }

    public boolean acceptsParameter(final OAIRequestParameter param) {
        return legalParameters.contains(param);
    }

    public boolean requiresParameter(final OAIRequestParameter param) {
        return requiredParameters.contains(param);
    }

    public static boolean isVerb(final QName qname) {
        return ALL_QNAMES.contains(qname);
    }
}
