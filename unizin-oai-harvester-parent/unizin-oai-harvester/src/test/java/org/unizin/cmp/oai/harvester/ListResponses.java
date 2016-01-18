package org.unizin.cmp.oai.harvester;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.http.HttpStatus;
import org.unizin.cmp.oai.ResumptionToken;
import org.unizin.cmp.oai.mocks.WireMockUtils;
import org.unizin.cmp.oai.templates.ListRecordsTemplate;
import org.unizin.cmp.oai.templates.RecordMetadataTemplate;

import freemarker.template.TemplateException;

public final class ListResponses {
    public static final long DEFAULT_RESPONSE_COUNT = 2;

    public static final ResumptionToken FIRST_TOKEN =
            new ResumptionToken("the_first_token", 2L, 1L, null);

    public static final ResumptionToken LAST_TOKEN =
            new ResumptionToken("", DEFAULT_RESPONSE_COUNT,
                    DEFAULT_RESPONSE_COUNT, null);

    public static final List<ResumptionToken> RESUMPTION_TOKENS =
            Collections.unmodifiableList(Arrays.asList(
                    FIRST_TOKEN, LAST_TOKEN));

    public static Map<String, Object> toMap(final ResumptionToken token) {
        final Map<String, Object> m = new HashMap<>();
        m.put("token", token.getToken());
        if (token.getCursor().isPresent()) {
            m.put("cursor", token.getCursor().get());
        }
        if (token.getCompleteListSize().isPresent()) {
            m.put("completeListSize",
                    token.getCompleteListSize().get());
        }
        if (token.getExpirationDate().isPresent()) {
            m.put("expirationDate", token.getExpirationDate().get());
        }
        return m;
    }

    private static void addRecord(final ListRecordsTemplate listRecordsTemplate,
            final String identifier,
            final RecordMetadataTemplate recordMetadataTemplate)
                    throws TemplateException, IOException {
        final Map<String, Object> record = new HashMap<>(2);
        record.put("identifier", identifier);
        record.put("metadata", recordMetadataTemplate.process());
        listRecordsTemplate.addRecord(record);
    }


    /**
     *
     * @param responseBody
     *            the response body
     * @param resumptionToken
     *            the token which should trigger the given response, or empty if
     *            the given response should be returned in response to requests
     *            with no token.
     */
    private static void setupResponse(final String responseBody,
            final Optional<String> resumptionToken) {
        final String pattern = resumptionToken.isPresent() ?
                Tests.urlResmptionTokenPattern(resumptionToken.get()) :
                    Tests.URL_PATTERN_WITHOUT_RESUMPTION_TOKEN;
        WireMockUtils.getStub(HttpStatus.SC_OK,
                responseBody, pattern);
    }

    public static void setupResponses(final String firstResponse,
            final Map<String, String> subsequentResponses) {
        setupResponse(firstResponse, Optional.empty());
        for (final Map.Entry<String, String> entry :
            subsequentResponses.entrySet()) {
            final String token = entry.getKey();
            final String body = entry.getValue();
            setupResponse(body, Optional.of(token));
        }
    }

    /**
     * Set up with two incomplete lists. First has two records, second has one.
     *
     * @param sendFinalResumptionToken
     *            should we follow the standard and send an empty resumption
     *            token in the last incomplete list? If {@code false}, do what
     *            many repositories actually do, and send no token at all.
     */
    public static List<String> setupWithDefaultListRecordsResponse(
            final boolean sendFinalResumptionToken)
                    throws TemplateException, IOException {
        ListRecordsTemplate listRecordsTemplate = new ListRecordsTemplate()
                .withResumptionToken(toMap(FIRST_TOKEN));
        RecordMetadataTemplate recordMetadataTemplate =
                new RecordMetadataTemplate()
                .addTitle("A Title")
                .addCreator("Some Creator")
                .addCreator("Another Creator");
        addRecord(listRecordsTemplate, "1", recordMetadataTemplate);

        recordMetadataTemplate = new RecordMetadataTemplate()
                .addTitle("Another Title")
                .addTitle("Yet More Title")
                .addDate("2015-10-31")
                .addDate("1900-01-01");
        addRecord(listRecordsTemplate, "2", recordMetadataTemplate);
        final String firstResponse = listRecordsTemplate.process();

        listRecordsTemplate = new ListRecordsTemplate();
        if (sendFinalResumptionToken) {
            listRecordsTemplate.withResumptionToken(toMap(LAST_TOKEN));
        }
        recordMetadataTemplate = new RecordMetadataTemplate()
                .addTitle("Such Title Wow");
        addRecord(listRecordsTemplate, "3", recordMetadataTemplate);
        final Map<String, String> subsequentResponses = new HashMap<>(1);
        subsequentResponses.put(FIRST_TOKEN.getToken(),
                listRecordsTemplate.process());

        setupResponses(firstResponse, subsequentResponses);
        final List<String> list = new ArrayList<>();
        list.add(firstResponse);
        list.addAll(subsequentResponses.values());
        return list;
    }


    /** No instances allowed. */
    private ListResponses() { }
}
