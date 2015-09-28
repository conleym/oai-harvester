package org.unizin.catalog.importer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.importer.factories.AbstractDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.factories.DefaultDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.source.SourceNode;

public final class HarvestedRecordFactory extends AbstractDocumentModelFactory {

	private final DefaultDocumentModelFactory dfmf = 
			new DefaultDocumentModelFactory();
	
	@Override
	public DocumentModel createFolderishNode(final CoreSession session, 
			final DocumentModel parent, final SourceNode node)
			throws IOException {
		return dfmf.createFolderishNode(session, parent, node);
	}

	@Override
	public DocumentModel createLeafNode(final CoreSession session, 
			final DocumentModel parent, final SourceNode node)
					throws IOException {
		final DocumentModel dm = dfmf.createLeafNode(session, parent, node);
		dm.addFacet("Harvested");
		final Map<String, Object> m = new HashMap<>();
		m.putAll(node.getBlobHolder().getProperties());
		dm.setProperties("HarvestedRecord", m);
		
		return dm;
	}

}
