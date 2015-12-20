package org.unizin.cmp.harvester.job;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class Batch {
    private final int batchSize;
    private final Map<String, Set<String>> map = new HashMap<>();
    private final List<HarvestedOAIRecord> batch = new ArrayList<>();

    Batch(final int size) {
        this.batchSize = size;
    }

    boolean full() {
        return batch.size() >= batchSize;
    }

    boolean add(final HarvestedOAIRecord record) {
        final String uri = record.getBaseURL();
        final String id = record.getIdentifier();
        Set<String> ids = map.get(uri);
        if (ids == null) {
            ids = new HashSet<>();
            map.put(uri, ids);
        }
        if (! ids.contains(id)) {
            batch.add(record);
            ids.add(id);
            return true;
        }
        return false;
    }

    int size() {
        return batch.size();
    }

    boolean isEmpty() {
        return batch.isEmpty();
    }

    void clear() {
        map.clear();
        batch.clear();
    }

    List<HarvestedOAIRecord> toList() {
        return batch;
    }
}
