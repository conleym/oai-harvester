package org.unizin.cmp.oai.harvester.service;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.service.client.JIRAClient;
import org.unizin.cmp.oai.harvester.service.client.JIRAClient.JIRAClientException;

import com.google.common.collect.ImmutableMap;


public final class HarvestFailureListener implements Consumer<HarvestNotification> {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            HarvestFailureListener.class);


    private final JIRAClient jiraClient;
    private final Optional<UriBuilder> uriBuilder;

    public HarvestFailureListener(final JIRAClient jiraClient,
            final Optional<UriBuilder> uriBuilder) {
        Objects.requireNonNull(jiraClient, "jiraClient");
        Objects.requireNonNull(uriBuilder, "uriBuilder");
        this.jiraClient = jiraClient;
        this.uriBuilder = uriBuilder;
    }


    private Optional<String> link(final HarvestNotification hn) {
        if (uriBuilder.isPresent()) {
            final URI uri = uriBuilder.get().build(hn.getTag(
                    JobManager.JOB_NAME));
            return Optional.of(uri.toString());
        }
        return Optional.empty();
    }

    private String summary(final HarvestNotification hn) {
        return String.format("Failed harvest of %s",
                hn.getTag(JobManager.JOB_NAME));
    }

    private String description(final HarvestNotification hn) {
        final String fmt = "The harvest of repository %s (owned by " +
                "institution %s) failed.";
        final String prefix = String.format(fmt,
                hn.getTag(JobManager.REPOSITORY_NAME), institution(hn));
        final Optional<String> link = link(hn);
        if (link.isPresent()) {
            return prefix + "  Details available at " + link.get();
        }
        return prefix;
    }

    private String institution(final HarvestNotification hn) {
        final String institution = hn.getTag(JobManager.REPOSITORY_INSTITUTION);
        return institution == null ? "" : institution;
    }

    @Override
    public void accept(final HarvestNotification t) {
        final Map<String, Object> postData = ImmutableMap.of(
                "fields", ImmutableMap.of(
                            "project", ImmutableMap.of("key", "SD"),
                            "issuetype", ImmutableMap.of("name", "Help"),
                            "summary", summary(t),
                            "description", description(t),
                            "customfield_10104", institution(t)));
        try {
            jiraClient.createIssue(postData);
        } catch (final JIRAClientException e) {
            if (LOGGER.isErrorEnabled()) {
                final String msg = String.format(
                        "Error creating JIRA issue for failed harvest %s",
                        t);
                LOGGER.error(msg, e);
            }
        }
    }
}
