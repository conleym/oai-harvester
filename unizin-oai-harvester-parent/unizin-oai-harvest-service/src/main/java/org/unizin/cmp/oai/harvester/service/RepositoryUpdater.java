package org.unizin.cmp.oai.harvester.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.skife.jdbi.v2.Call;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unizin.cmp.oai.harvester.service.db.DBIUtils;

public final class RepositoryUpdater implements Runnable {
    private final Logger LOGGER = LoggerFactory.getLogger(
            RepositoryUpdater.class);

    private final NuxeoClient nuxeoClient;
    private final DBI dbi;

    public RepositoryUpdater(final NuxeoClient nuxeoClient, final DBI dbi) {
        this.nuxeoClient = nuxeoClient;
        this.dbi = dbi;
    }

    private void addRepo(final DBI dbi, final Map<String, Object> entry,
            final List<String> names, final List<String> baseURIs,
            final List<String> institutions) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> props =
        (Map<String, Object>)entry.get("properties");
        names.add((String)props.get("dc:title"));
        baseURIs.add((String)props.get("repo:baseUrl"));
        institutions.add((String)props.get("repo:owner"));
    }

    @Override
    public void run() {
        try {
            LOGGER.info("Getting repositories from Nuxeo.");
            // Accumulate all the info we're interested in, then call the
            // update function.
            final List<String> names = new ArrayList<>();
            final List<String> uris = new ArrayList<>();
            final List<String> institutions = new ArrayList<>();
            nuxeoClient.repositories().forEach(page -> {
                @SuppressWarnings("unchecked")
                final List<Map<String, Object>> entries =
                (List<Map<String, Object>>)page.get("entries");
                entries.forEach(entry -> {
                    addRepo(dbi, entry, names, uris, institutions);
                });
            });
            try (final Handle h = DBIUtils.handle(dbi)) {
                final Call c = h.createCall(
                        "call UPDATE_REPOSITORIES(#names, #uris, " +
                        "#institutions)");
                c.bind("names", names)
                 .bind("uris", uris)
                 .bind("institutions", institutions)
                 .invoke();
            }
            LOGGER.info("Done updating repositories.");
        } catch (final Exception e) {
            /* Uncaught exceptions will cause the scheduler to stop running
             * this task, so catch them all. */
            LOGGER.error("Error updating repositories from Nuxeo.", e);
        }
    }
}
