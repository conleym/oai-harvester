package org.unizin.cmp.oai.harvester.service;

import java.util.Map;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.service.JIRAClient.JIRAClientException;

import com.google.common.collect.ImmutableMap;

public final class FailureListener implements Consumer<HarvestNotification> {
    private static final Logger LOGGER = LoggerFactory.getLogger(
            FailureListener.class);


    private final JIRAClient jiraClient;

    public FailureListener(final JIRAClient jiraClient) {
        this.jiraClient = jiraClient;
    }


    private String summary(final HarvestNotification hn) {
        return "";
    }

    private String description(final HarvestNotification hn) {
        return "";
    }

    private String institution(final HarvestNotification hn) {
        return "";
    }

    @Override
    public void accept(final HarvestNotification t) {
        final Map<String, Object> postData = ImmutableMap.of(
                "fields", ImmutableMap.of(
                            "project", ImmutableMap.of("key", "SD"),
                            "issuetype", ImmutableMap.of("name", "help"),
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
