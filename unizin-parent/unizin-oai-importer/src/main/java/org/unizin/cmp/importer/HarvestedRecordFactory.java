package org.unizin.cmp.importer;

import java.io.IOException;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.blobholder.BlobHolder;
import org.nuxeo.ecm.platform.importer.factories.DefaultDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.source.SourceNode;


/**
 * A {@code DocumentModelFactory} that doesn't assume we have content to
 * attach to imported files.
 */
public final class HarvestedRecordFactory extends DefaultDocumentModelFactory {
	
	public HarvestedRecordFactory() {
	} 
	
	public HarvestedRecordFactory(final String folderishType,
			final String leafType) {
		super(folderishType, leafType);
	}

	@Override
	protected DocumentModel defaultCreateLeafNode(final CoreSession session,
			final DocumentModel parent, final SourceNode node)
					throws IOException {
		// Copy and paste, from defaultCreateLeafNode, except that we _don't_
		// assume the SourceNode's BlobHolder has a non-null blob.
		//
		// This lets us import without empty file attachments
		// or other junk we don't want if we don't have the actual
		// files on hand.
		BlobHolder bh = node.getBlobHolder();
		String leafTypeToUse = getDocTypeToUse(bh);
		if (leafTypeToUse == null) {
			leafTypeToUse = leafType;
		}
		List<String> facets = getFacetsToUse(bh);
		// Also mimeType wasn't used, so we won't bother with it.
		String name = getValidNameFromFileName(node.getName());
		String title = node.getName();
		DocumentModel doc = session.createDocumentModel(
				parent.getPathAsString(), name, leafTypeToUse);
		for (String facet : facets) {
			doc.addFacet(facet);
		}
		doc.setProperty("dublincore", "title", title);
		final Blob b = bh.getBlob();
		if (b != null) {
			doc.setProperty("file", "filename", title);
			doc.setProperty("file", "content", bh.getBlob());
		}
		doc = session.createDocument(doc);
		if (bh != null) {
			doc = setDocumentProperties(session,
					bh.getProperties(), doc);
		}
		return doc;
	}
}
