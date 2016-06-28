package org.unizin.cmp.oai.harvester.response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Objects;

import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

import org.unizin.cmp.oai.OAIXMLUtils;
import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.HarvestNotification.HarvestStatistic;
import org.unizin.cmp.oai.harvester.exception.HarvesterException;

/**
 * Output handler that writes each response to a separate file in a specified
 * directory.
 * <p>
 * Each file is given by an instance of {@link EventHandlerProvider}, which
 * provides a new {@code OAIEventHandler} for each response. Each event handler
 * is closed once the response has been processed.
 * </p>
 * <p>
 * A {@link DirectoryEventHandlerProvider handler} and
 * {@link #FilesOAIResponseHandler(File, XMLOutputFactory)} constructor are
 * provided for convenience.
 * </p>
 *
 */
public final class FilesOAIResponseHandler extends AbstractOAIResponseHandler {

    /**
     * Implementations produce an event handler given a harvest notification.
     * <p>
     * Instances will be called once per request/response cycle when the request
     * is received, and must produce a <em>new</em> event handler for each
     * response.
     * </p>
     */
    @FunctionalInterface
    public static interface EventHandlerProvider {
        OAIEventHandler get(HarvestNotification notification)
                throws XMLStreamException, IOException;
    }

    /**
     * Event handler provider that creates files in a given directory.
     * <p>
     * Each event handler returned by this provider will write events to a file
     * in the given directory named X.xml, where X is the current response count
     * of the harvest. The given output factory will be used to create the event
     * writers to which received events will be written.
     * </p>
     * <p>
     * Output will be UTF-8 encoded, regardless of the encoding of the
     * repository's responses.
     * </p>
     */
    public static final class DirectoryEventHandlerProvider
        implements EventHandlerProvider {

        private final File directory;
        private final XMLOutputFactory outputFactory;

        public DirectoryEventHandlerProvider(final File directory,
                final XMLOutputFactory outputFactory) {
            Objects.requireNonNull(directory, "directory");
            Objects.requireNonNull(outputFactory, "outputFactory");
            if (! directory.isDirectory()) {
                throw new IllegalArgumentException(String.format(
                        "Not a directory: %s.", directory));
            }
            this.directory = directory;
            this.outputFactory = outputFactory;
        }

        @Override
        public OAIEventHandler get(final HarvestNotification notification)
                throws XMLStreamException, IOException {
            final long fileCount = notification.getStats()
                    .get(HarvestStatistic.RESPONSE_COUNT);
            final URI dest = directory.toURI().resolve(fileCount + ".xml");
            final File destFile = new File(dest);
            final OutputStream out = new FileOutputStream(destFile);
            final XMLEventWriter eventWriter = OAIXMLUtils.createEventWriter(
                    outputFactory, out);
            return new OAIEventHandler() {
                @Override
                public void onEvent(final XMLEvent e)
                        throws XMLStreamException {
                    eventWriter.add(e);
                }

                @Override
                public void close() throws XMLStreamException {
                    try (final OutputStream os = out) {
                        eventWriter.close();
                    } catch (final IOException e) {
                        throw new HarvesterException(e);
                    }
                }

            };
        }
    }


    private final EventHandlerProvider provider;
    private OAIEventHandler eventHandler;


    public FilesOAIResponseHandler(final EventHandlerProvider provider) {
        Objects.requireNonNull(provider, "provider");
        this.provider = provider;
    }

    /**
     * Create an instance with a {@link DirectoryEventHandlerProvider} provider
     *
     * @param directory
     *            the directory where harvest response files should be stored.
     */
    public FilesOAIResponseHandler(final File directory) {
        this(directory, OAIXMLUtils.newOutputFactory());
    }

    /**
     * Create an instance with a {@link DirectoryEventHandlerProvider} provider
     * using the given directory and output factory.
     *
     * @param directory
     *            directory the directory where harvest response files should be
     *            stored.
     * @param outputFactory
     *            the XML output factory to use to write XML events to files.
     */
    public FilesOAIResponseHandler(final File directory,
            final XMLOutputFactory outputFactory) {
        this(new DirectoryEventHandlerProvider(directory, outputFactory));
    }

    @Override
    public OAIEventHandler getEventHandler(
            final HarvestNotification notification) {
        return eventHandler;
    }

    @Override
    public void onResponseReceived(final HarvestNotification notification) {
        try {
            eventHandler = provider.get(notification);
        } catch (final XMLStreamException | IOException e) {
            throw new HarvesterException(e);
        }
    }

    @Override
    public void onResponseProcessed(final HarvestNotification notification) {
        if (eventHandler != null) {
            try {
                eventHandler.close();
            } catch (final XMLStreamException e) {
                throw new HarvesterException(e);
            }
        }
    }
}
