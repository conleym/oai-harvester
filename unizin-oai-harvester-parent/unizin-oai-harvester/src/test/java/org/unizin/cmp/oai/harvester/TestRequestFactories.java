package org.unizin.cmp.oai.harvester;

import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;

import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClients;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.unizin.cmp.oai.OAI2Constants;
import org.unizin.cmp.oai.OAIRequestParameter;
import org.unizin.cmp.oai.OAIVerb;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ImmutableMap;


public final class TestRequestFactories {
    private static final int EXPECTED_GET_STATUS =
            HttpStatus.SC_PAYMENT_REQUIRED;
    private static final int EXPECTED_POST_STATUS =
            HttpStatus.SC_LOCKED;

    @Rule
    public final WireMockRule wireMock = WireMockUtils.newWireMockRule();
    @Rule
    public final ExpectedException exception = ExpectedException.none();


    private HttpUriRequest request(final OAIRequestFactory factory,
            final Map<String, String> params) {
        return factory.createRequest(WireMockUtils.MOCK_OAI_BASE_URI, params);
    }

    private HttpResponse execute(final HttpUriRequest request)
            throws Exception {
        return HttpClients.createMinimal().execute(request);
    }

    private void testGet(final Map<String, String> params) throws Exception {
        final HttpUriRequest req = request(GetOAIRequestFactory.getInstance(),
                params);
        final MappingBuilder mb = WireMockUtils.getAnyURL();
        WireMockUtils.returning(mb, EXPECTED_GET_STATUS);
        WireMockUtils.matchingQueryParams(mb, params);
        stubFor(mb);
        final HttpResponse resp = execute(req);
        Assert.assertEquals(EXPECTED_GET_STATUS,
                resp.getStatusLine().getStatusCode());
    }

    private void testPost(final Map<String, String> params) throws Exception {
        final HttpUriRequest req = request(PostOAIRequestFactory.getInstance(),
                params);
        final MappingBuilder mb = WireMockUtils.postAnyURL();
        WireMockUtils.returning(mb, EXPECTED_POST_STATUS);
        WireMockUtils.matchingPostParams(mb, params);
        stubFor(mb);
        final HttpResponse resp = execute(req);
        Assert.assertEquals(EXPECTED_POST_STATUS,
                resp.getStatusLine().getStatusCode());
    }

    private void testGetAndPost(final Map<String, String> params)
            throws Exception {
        testGet(params);
        testPost(params);
    }

    @Test
    public void test() throws Exception {
        testGetAndPost(ImmutableMap.of("x", "1 2 3 4"));
        testGetAndPost(ImmutableMap.of(
                OAI2Constants.VERB_PARAM_NAME, OAIVerb.LIST_RECORDS.localPart(),
                OAIRequestParameter.METADATA_PREFIX.paramName(), "madeup",
                OAIRequestParameter.SET.paramName(), "some set",
                "foo bar", "baz"
        ));
    }
}