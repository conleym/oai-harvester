package org.unizin.cmp.oai.harvester.job;

import java.util.Collections;
import java.util.Map;

import org.unizin.cmp.oai.harvester.HarvestParams;

public final class JobHarvestSpec {

    private final HarvestParams params;
    private final Map<String, String> tags;

    public JobHarvestSpec(final HarvestParams params) {
        this(params, Collections.emptyMap());
    }

    public JobHarvestSpec(final HarvestParams params,
            final Map<String, String> tags) {
        this.params = params;
        this.tags = tags;
    }

    public Map<String, String> getTags() { return tags; }
    public HarvestParams getParams() { return params; }
}
