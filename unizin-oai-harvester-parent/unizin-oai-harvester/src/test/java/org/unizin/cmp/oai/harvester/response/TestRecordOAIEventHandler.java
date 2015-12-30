package org.unizin.cmp.oai.harvester.response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.unizin.cmp.oai.OAI2Constants;
import org.unizin.cmp.oai.harvester.Harvester;
import org.unizin.cmp.oai.harvester.ListResponses;
import org.unizin.cmp.oai.harvester.Tests;
import org.unizin.cmp.oai.harvester.WireMockUtils;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

public final class TestRecordOAIEventHandler {
    @Rule
    public final WireMockRule wireMock = WireMockUtils.newWireMockRule();

    @Rule
    public final ExpectedException expected = ExpectedException.none();


    /**
     * Test implementation of the abstract/empty protected methods of
     * {@link RecordOAIEventHandler}.
     * <p>
     * Mockito can't mock these for us.
     * </p>
     */
    private final class Handler
    extends RecordOAIEventHandler<Multimap<String, String>> {
        public Handler(final Consumer<Multimap<String, String>> consumer) {
            super(consumer);
        }

        @Override
        protected void onRecordEnd(final Multimap<String, String> currentRecord,
                final List<XMLEvent> recordEvents) {
            // Simple event verification.
            Assert.assertTrue(recordEvents.size() >= 2);
            final XMLEvent first = recordEvents.get(0);
            final XMLEvent last = recordEvents.get(recordEvents.size() - 1);
            Assert.assertTrue(first.isStartElement());
            Assert.assertEquals(OAI2Constants.RECORD,
                    first.asStartElement().getName());
            Assert.assertTrue(last.isEndElement());
            Assert.assertEquals(OAI2Constants.RECORD,
                    last.asEndElement().getName());
        }

        @Override
        protected void onDatestamp(final Multimap<String, String> currentRecord,
                final String datestamp) {
            currentRecord.put("datestamp", datestamp);
        }

        @Override
        protected void onIdentifier(
                final Multimap<String, String> currentRecord,
                final String identifier) {
            currentRecord.put("identifier", identifier);
        }

        @Override
        protected void onSet(final Multimap<String, String> currentRecord,
                final String set) {
            currentRecord.put("set", set);
        }

        @Override
        protected void onStatus(Multimap<String, String> currentRecord,
                final String status) {
            currentRecord.put("status", status);
        }

        @Override
        protected Multimap<String, String> createRecord(
                final StartElement recordStartElement) {
            return ArrayListMultimap.create();
        }
    }

    private void setupResponse() throws Exception {
        ListResponses.setupResponses(null, Collections.emptyMap());

    }

    private OAIResponseHandler setupHandlers(
            final Consumer<Multimap<String, String>> consumer) {
        final OAIEventHandler eventHandler = new Handler(consumer);
        final OAIResponseHandler responseHandler = Mockito.mock(
                OAIResponseHandler.class);
        Mockito.when(responseHandler.getEventHandler(Matchers.any()))
            .thenReturn(eventHandler);
        return responseHandler;
    }

//    @Test
    public void testStuff() throws Exception {
        final List<Multimap<String, String>> results = new ArrayList<>(2);
        final Consumer<Multimap<String, String>> consumer =
                new Consumer<Multimap<String, String>>() {
            @Override
            public void accept(final Multimap<String, String> t) {
                results.add(t);
            }
        };

        final OAIResponseHandler responseHandler = setupHandlers(consumer);
        setupResponse();
        new Harvester.Builder().build().start(Tests.newParams().build(),
                responseHandler);
        Assert.assertEquals(2, results.size());
    }
}
