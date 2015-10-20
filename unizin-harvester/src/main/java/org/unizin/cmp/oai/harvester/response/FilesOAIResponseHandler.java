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
import org.unizin.cmp.oai.harvester.exception.HarvesterException;

/**
 * Output handler that writes each response to a separate file in a specified
 * directory.
 *
 */
public final class FilesOAIResponseHandler implements OAIResponseHandler {
	
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
	public OAIEventHandler getEventHandler(final HarvestNotification notification) {
		return eventHandler;
	}

	@Override
	public void onHarvestStart(final HarvestNotification notification) {		
	}

	@Override
	public void onHarvestEnd(final HarvestNotification notification) {
		
	}

	@Override
	public void onResponseStart(final HarvestNotification notification) {
		try {
			final long fileCount = notification.getStats()
					.get("partialListResponseCount");
			final URI dest = directory.toURI().resolve(fileCount + ".xml");
			final File destFile = new File(dest);
			outputStream = new FileOutputStream(destFile);
			eventWriter = outputFactory.createXMLEventWriter(outputStream);
			eventHandler = new FilteringOAIEventHandler(eventWriter);
		} catch (final XMLStreamException | IOException e) {
			throw new HarvesterException(e);
		}
	}

	@Override
	public void onResponseEnd(HarvestNotification notification) {
		OAIXMLUtils.closeQuietly(eventWriter);
		try (final OutputStream os = outputStream) {
			eventWriter = null;
			outputStream = null;
			if (os != null) {
				os.close();
			}
		} catch (final IOException e) {
			throw new HarvesterException(e);
		}
	}
}
