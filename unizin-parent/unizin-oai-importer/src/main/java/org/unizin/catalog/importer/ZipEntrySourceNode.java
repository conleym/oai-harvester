package org.unizin.catalog.importer;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.stream.XMLStreamException;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.core.api.blobholder.SimpleBlobHolder;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;


/** Each of these is assumed to be an OAI-PMH ListRecords response. */
public final class ZipEntrySourceNode implements SourceNode {
	
	private static final Logger LOGGER = 
			LoggerFactory.getLogger(ZipEntrySourceNode.class);

	private final ZipFile zipFile;
	private final ZipEntry zipEntry;	
	
	
	public ZipEntrySourceNode(final ZipFile zipFile, final ZipEntry zipEntry) {
		this.zipFile = zipFile;
		this.zipEntry = zipEntry;
	}
	
	private InputStream getInputStream() throws IOException {
		return this.zipFile.getInputStream(this.zipEntry);
	}
	
	@Override
	public boolean isFolderish() {
		return true;
	}

	@Override
	public BlobHolder getBlobHolder() throws IOException {
		final Blob blob = Blobs.createBlob(zipFile.getInputStream(zipEntry));
		return new SimpleBlobHolder(blob);
	}

	@Override
	public List<SourceNode> getChildren() throws IOException {
		try {
			final InputStream in = getInputStream();
			final ListRecordsResponseSplitter splitter = 
					new ListRecordsResponseSplitter(in);
			final List<SourceNode> children = new ArrayList<>();
			Iterables.addAll(children, splitter);
			return children;
		} catch (final XMLStreamException e) {
			throw new XMLStreamRuntimeException(e);
		} catch (final URISyntaxException e) {
			throw new IOException(e);
		}
	}

	@Override
	public String getName() {
		return this.zipEntry.getName();
	}

	@Override
	public String getSourcePath() {
		return this.zipFile.getName();
	}
}
