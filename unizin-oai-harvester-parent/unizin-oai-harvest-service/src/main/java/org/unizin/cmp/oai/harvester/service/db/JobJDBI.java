package org.unizin.cmp.oai.harvester.service.db;

import static org.unizin.cmp.oai.harvester.service.Status.formatHeaders;
import static org.unizin.cmp.oai.harvester.service.Status.headerValue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.HashPrefixStatementRewriter;
import org.skife.jdbi.v2.sqlobject.Bind;
import org.skife.jdbi.v2.sqlobject.GetGeneratedKeys;
import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.SqlUpdate;
import org.skife.jdbi.v2.sqlobject.Transaction;
import org.skife.jdbi.v2.sqlobject.customizers.OverrideStatementRewriterWith;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapperFactory;
import org.skife.jdbi.v2.sqlobject.customizers.SingleValueResult;
import org.skife.jdbi.v2.sqlobject.mixins.GetHandle;
import org.slf4j.Logger;
import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.HarvestNotification.HarvestNotificationType;
import org.unizin.cmp.oai.harvester.HarvestNotification.HarvestStatistic;
import org.unizin.cmp.oai.harvester.exception.HarvesterHTTPStatusException;
import org.unizin.cmp.oai.harvester.exception.OAIProtocolException;

/**
* JDBI database access methods.
*/
/*
 * Hash prefix should make converting to PostgreSQL easier, should it ever be
 * necessary.
 */
@OverrideStatementRewriterWith(HashPrefixStatementRewriter.class)
public abstract class JobJDBI implements AutoCloseable, GetHandle {
    private static final ByteArrayInputStream EMPTY
        = new ByteArrayInputStream(new byte[]{});

    private static final String JOB_UPDATE = "update JOB " +
            "set JOB_START = #start," +
            "JOB_END = #end, " +
            "JOB_LAST_UPDATE = now(), " +
            "JOB_RECORDS_RECEIVED = #recordsReceived, " +
            "JOB_RECORD_BYTES_RECEIVED = #recordBytesReceived, " +
            "JOB_BATCHES_ATTEMPTED = #batchesAttempted " +
            "where JOB_ID = #id";

    private static final String HARVEST_UPDATE = "update HARVEST " +
            "set HARVEST_START = #start, " +
            "HARVEST_END = #end, " +
            "HARVEST_LAST_UPDATE = now(), " +
            "HARVEST_CANCELLED = #cancelled, " +
            "HARVEST_INTERRUPTED = #interrupted, " +
            "HARVEST_LAST_REQUEST_URI = #lastRequestURI," +
            "HARVEST_LAST_REQUEST_PARAMETERS = #lastRequestParameters, " +
            "HARVEST_STACK_TRACE = #stackTrace, " +
            "HARVEST_REQUEST_COUNT = #requestCount, " +
            "HARVEST_RESPONSE_COUNT = #responseCount, " +
            "HARVEST_XML_EVENT_COUNT = #eventCount " +
            "where HARVEST_ID = #id";

    private static final String INSERT_HARVEST_HTTP_ERROR = "insert into " +
            "HARVEST_HTTP_ERROR(HARVEST_ID, HARVEST_HTTP_ERROR_STATUS_CODE, " +
            "HARVEST_HTTP_ERROR_RESPONSE_BODY, " +
            "HARVEST_HTTP_ERROR_CONTENT_ENCODING, " +
            "HARVEST_HTTP_ERROR_CONTENT_TYPE, HARVEST_HTTP_ERROR_HEADERS) " +
            "values (#id, #statusCode, #responseBody, #contentEncoding, " +
            "#contentType, #headers)";

    private static final String JOB_QUERY = "select J.*, H.*, R.*, E.*, "
            + "OAI_ERRORS(H.HARVEST_ID) as HARVEST_PROTOCOL_ERRORS from " +
            "JOB J inner join HARVEST H on J.JOB_ID = H.JOB_ID " +
            "inner join REPOSITORY R on R.REPOSITORY_ID = H.REPOSITORY_ID " +
            "left outer join HARVEST_HTTP_ERROR E " +
            "on H.HARVEST_ID = E.HARVEST_ID " +
            "left outer join HARVEST_PROTOCOL_ERROR P " +
            "on H.HARVEST_ID = P.HARVEST_ID " +
            "where J.JOB_ID = #id";

    @SqlUpdate("insert into JOB() values()")
    @GetGeneratedKeys
    public abstract long createJob();

    @SqlUpdate(JOB_UPDATE)
    public abstract void updateJob(@Bind("id") long id,
            @Bind("start") Instant start,
            @Bind("end") Optional<Instant> end,
            @Bind("stackTrace") Optional<String> stackTrace,
            @Bind("recordsReceived") long recordsReceived,
            @Bind("recordBytesReceived") long recordBytesReceived,
            @Bind("batchesAttempted") long batchesAttempted);

    @SqlUpdate(HARVEST_UPDATE)
    public abstract void updateHarvest(@Bind("id") long id,
            @Bind("start") Instant start,
            @Bind("end") Optional<Instant> end,
            @Bind("cancelled") boolean cancelled,
            @Bind("interrupted") boolean interrupted,
            @Bind("lastRequestURI") Optional<String> lastRequestURI,
            @Bind("lastRequestParameters")
    Optional<String> lastRequestParameters,
    @Bind("stackTrace") Optional<String> stackTrace,
    @Bind("requestCount") long requestCount,
    @Bind("responseCount") long responseCount,
    @Bind("eventCount") long eventCount);

    @SqlUpdate(INSERT_HARVEST_HTTP_ERROR)
    public abstract void insertHarvestHTTPError(@Bind("id") long harvestID,
            @Bind("statusCode") int statusCode,
            @Bind("headers") String[] headers,
            @Bind("contentType") Optional<String> contentType,
            @Bind("contentEncoding") Optional<String> contentEncoding,
            @Bind("responseBody") InputStream body);

    @SqlQuery(JOB_QUERY)
    @SingleValueResult(Map.class)
    @RegisterMapperFactory(CMPMapperFactory.class)
    public abstract List<Map<String, Object>> findJobByID(@Bind("id") long id);

    @Override
    public abstract void close();

    /**
     * Ensure that exceptions are written only once, as there's no facility
     * within the database or the application to constrain duplicate entries.
     *
     * @param notification
     *            the notification to check.
     * @return {@code true} iff the notification has an exception and is
     *         reporting the end of the harvest.
     */
    private static boolean writeExceptionInfo(
            final HarvestNotification notification) {
        return notification.getType() == HarvestNotificationType.HARVEST_ENDED
                && notification.getException().isPresent();
    }

    private static InputStream responseBody(
            final HarvesterHTTPStatusException ex, final Logger logger)
                    throws IOException {
        try {
            final PipedInputStream pis = new PipedInputStream();
            final PipedOutputStream pos = new PipedOutputStream(pis);
            new Thread(() -> {
                try (final OutputStream out = pos) {
                    ex.writeResponseBodyTo(out);
                } catch (final IOException e) {
                    logger.error("Error writing response body to database.", e);
                }
            }).start();
            return pis;
        } catch (final IOException e) {
            // Don't throw this so that the database can be updated anyway.
            logger.error("Error connecting pipe to read response body.", e);
            return EMPTY;
        }
    }

    @Transaction
    public void harvestDatabaseUpdate(final long harvestID,
            final Optional<String> lastRequestURI,
            final Optional<String> lastRequestParameters,
            final Optional<String> stackTrace,
            final HarvestNotification notification,
            final Logger logger) {
        updateHarvest(harvestID, notification.getStarted(),
                notification.getEnded(),
                notification.isCancelled(),
                notification.isInterrupted(),
                lastRequestURI,
                lastRequestParameters,
                stackTrace,
                notification.getStat(HarvestStatistic.REQUEST_COUNT),
                notification.getStat(HarvestStatistic.RESPONSE_COUNT),
                notification.getStat(HarvestStatistic.XML_EVENT_COUNT));
        if (writeExceptionInfo(notification)) {
            final Exception ex = notification.getException().get();
            if (ex instanceof HarvesterHTTPStatusException) {
                final HarvesterHTTPStatusException hhse =
                        (HarvesterHTTPStatusException)ex;
                try (final InputStream in = responseBody(hhse, logger)) {
                    insertHarvestHTTPError(harvestID,
                        hhse.getStatusLine().getStatusCode(),
                        formatHeaders(hhse.getHeaders()),
                        headerValue(hhse.getContentType()),
                        headerValue(hhse.getContentEncoding()),
                        in);
                } catch (final IOException e) {
                    final String msg =
                            "Error closing HTTP error response body stream.";
                    logger.warn(msg, e);
                }
            } else if (ex instanceof OAIProtocolException) {
                final OAIProtocolException ope = (OAIProtocolException)ex;
                final Handle h = getHandle();
                h.setStatementRewriter(new HashPrefixStatementRewriter());
                h.createCall("call INSERT_OAI_ERRORS(#id, #errors)")
                    .bind("id", harvestID)
                    .bind("errors", ope.getOAIErrors())
                    .invoke();
            }
        }
    }
}
