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

import org.unizin.cmp.oai.OAIXMLUtils;
import org.unizin.cmp.oai.harvester.HarvestNotification;
import org.unizin.cmp.oai.harvester.HarvestNotification.HarvestStatistic;
import org.unizin.cmp.oai.harvester.exception.HarvesterException;

/**
 * Output handler that writes each response to a separate file in a specified
 * directory.
 * <p>
 * Each file's name is the current number of responses received so far for the
 * harvest with a '.xml' extension.
 * </p>
 *
 */
public final class FilesOAIResponseHandler extends AbstractOAIResponseHandler {

    private final File directory;
    private final XMLOutputFactory outputFactory;
    private OutputStream outputStream;
    private XMLEventWriter eventWriter;
    private OAIEventHandler eventHandler;


    public FilesOAIResponseHandler(final File directory) {
        this(directory, OAIXMLUtils.newOutputFactory());
    }

    public FilesOAIResponseHandler(final File directory,
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
    public OAIEventHandler getEventHandler(
            final HarvestNotification notification) {
        return eventHandler;
    }

    @Override
    public void onResponseReceived(final HarvestNotification notification) {
        try {
            final long fileCount = notification.getStats()
                    .get(HarvestStatistic.RESPONSE_COUNT);
            final URI dest = directory.toURI().resolve(fileCount + ".xml");
            final File destFile = new File(dest);
            outputStream = new FileOutputStream(destFile);
            eventWriter = outputFactory.createXMLEventWriter(outputStream);
            eventHandler = new FilteringOAIEventHandler(
                    new EventWriterOAIEventHandler(eventWriter));
        } catch (final XMLStreamException | IOException e) {
            throw new HarvesterException(e);
        }
    }

    @Override
    public void onResponseProcessed(final HarvestNotification notification) {
        OAIXMLUtils.closeQuietly(eventWriter);
        try (final OutputStream os = outputStream) {
            eventWriter = null;
            outputStream = null;
        } catch (final IOException e) {
            throw new HarvesterException(e);
        }
    }
}
