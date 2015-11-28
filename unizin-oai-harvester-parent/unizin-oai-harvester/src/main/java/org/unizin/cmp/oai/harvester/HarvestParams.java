package org.unizin.cmp.oai.harvester;

import java.net.URI;
import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.unizin.cmp.oai.OAI2Constants;
import org.unizin.cmp.oai.OAIDateGranularity;
import org.unizin.cmp.oai.OAIRequestParameter;
import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.ResumptionToken;


/**
 * Convenience class for assembling and optionally validating OAI harvest
 * request parameters.
 * <p>
 * Instances are immutable.
 * </p>
 */
public final class HarvestParams {

    public static final class Builder {
        private final URI baseURI;
        private final OAIVerb verb;
        private final Map<String, String> standardParameters = new HashMap<>();
        private final Map<String, String> nonstandardParameters = new HashMap<>();

        public Builder(final URI baseURI, final OAIVerb verb) {
            this.baseURI = baseURI;
            this.verb = verb;
            // Ensure reasonable default metadataPrefix if it's required.
            if (verb.requiresParameter(OAIRequestParameter.METADATA_PREFIX)) {
                put(OAIRequestParameter.METADATA_PREFIX,
                        OAI2Constants.DEFAULT_METADATA_PREFIX);
            }
        }

        private void put(final OAIRequestParameter param, final String value) {
            Objects.requireNonNull(value);
            standardParameters.put(param.paramName(), value);
        }

        public Builder withFrom(final TemporalAccessor from,
                final OAIDateGranularity granularity) {
            return withFrom(granularity.format(from));
        }

        public Builder withFrom(final String from) {
            put(OAIRequestParameter.FROM, from);
            return this;
        }

        public Builder withIdentifier(final String identifier) {
            put(OAIRequestParameter.IDENTIFIER, identifier);
            return this;
        }

        public Builder withMetadataPrefix(final String prefix) {
            put(OAIRequestParameter.METADATA_PREFIX, prefix);
            return this;
        }

        public Builder withResumptionToken(final String resumptionToken) {
            put(OAIRequestParameter.RESUMPTION_TOKEN, resumptionToken);
            return this;
        }

        public Builder withSet(final String setSpec) {
            put(OAIRequestParameter.SET, setSpec);
            return this;
        }

        public Builder withUntil(final TemporalAccessor until,
                final OAIDateGranularity granularity) {
            return withUntil(granularity.format(until));
        }

        public Builder withUntil(final String until) {
            put(OAIRequestParameter.UNTIL, until);
            return this;
        }

        /**
         * Add an arbitrary nonstandard parameter.
         * <p>
         * Nonstandard parameters are never validated.
         * <p>
         * Nonstandard parameters are added only to the initial request. To use
         * a nonstandard parameter in both the initial and subsequent requests,
         * add it to the {@code baseURI} instead.
         * @param name the parameter's name
         * @param value the parameter's value
         * @return this instance
         */
        public Builder withNonstandardParameter(final String name,
                final String value) {
            nonstandardParameters.put(name, value);
            return this;
        }

        /**
         * Validate the standard harvest parameters.
         * @return {@code true} iff this instance's standard parameters are valid
         * with this instance's verb.
         */
        public boolean isValid() {
            return verb.areValidParameters(standardParameters);
        }

        public HarvestParams build() {
            return new HarvestParams(baseURI, verb, standardParameters,
                    nonstandardParameters);
        }
    }

    private final URI baseURI;
    private final OAIVerb verb;
    private final Map<String, String> standardParameters;
    private final Map<String, String> nonstandardParameters;
    private final Map<String, String> parameters;


    private HarvestParams(final URI baseURI, final OAIVerb verb,
            final Map<String, String> standardParameters,
            final Map<String, String> nonstandardParameters) {
        Objects.requireNonNull(baseURI, "baseURI");
        Objects.requireNonNull(verb, "verb");
        Objects.requireNonNull(standardParameters, "standardParameters");
        Objects.requireNonNull(nonstandardParameters, "nonstandardParameters");

        this.baseURI = baseURI;
        this.verb = verb;
        this.standardParameters = Collections.unmodifiableMap(
                standardParameters);
        this.nonstandardParameters = Collections.unmodifiableMap(
                nonstandardParameters);

        final Map<String, String> p = new HashMap<>(standardParameters);
        p.putAll(nonstandardParameters);
        p.put(OAI2Constants.VERB_PARAM_NAME, verb.localPart());
        this.parameters = Collections.unmodifiableMap(p);
    }

    public URI getBaseURI() {
        return baseURI;
    }

    public OAIVerb getVerb() {
        return verb;
    }

    /**
     * Validate the standard harvest parameters.
     * @return {@code true} iff this instance's standard parameters are valid
     * with this instance's verb.
     */
    public boolean areValid() {
        return verb.areValidParameters(standardParameters);
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public String get(final OAIRequestParameter param) {
        return parameters.get(param.paramName());
    }

    public String get(final String param) {
        return parameters.get(param);
    }

    public HarvestParams getRetryParameters(
            final ResumptionToken resumptionToken) {
        if (resumptionToken != null) {
            final Map<String, String> standard = new HashMap<>(
                    standardParameters);
            standard.put(OAIRequestParameter.RESUMPTION_TOKEN.paramName(),
                    resumptionToken.getToken());
            return new HarvestParams(baseURI, verb, standard,
                    nonstandardParameters);
        }
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((baseURI == null) ? 0 : baseURI.hashCode());
        result = prime * result + ((nonstandardParameters == null) ? 0 : nonstandardParameters.hashCode());
        result = prime * result + ((standardParameters == null) ? 0 : standardParameters.hashCode());
        result = prime * result + ((verb == null) ? 0 : verb.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        HarvestParams other = (HarvestParams) obj;
        if (baseURI == null) {
            if (other.baseURI != null)
                return false;
        } else if (!baseURI.equals(other.baseURI))
            return false;
        if (nonstandardParameters == null) {
            if (other.nonstandardParameters != null)
                return false;
        } else if (!nonstandardParameters.equals(other.nonstandardParameters))
            return false;
        if (standardParameters == null) {
            if (other.standardParameters != null)
                return false;
        } else if (!standardParameters.equals(other.standardParameters))
            return false;
        if (verb != other.verb)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return parameters.toString();
    }
}
