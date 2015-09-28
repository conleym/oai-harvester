package org.unizin.catalog.importer;

import java.io.IOException;
import java.io.Serializable;
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

	private interface KeyFilter {
		boolean operation(String key);
	}
	
	private static <T> Map<String, T> filterMap(final Map<String, T> toFilter,
			KeyFilter f) {
		final Map<String, T> result = new HashMap<>();
		for (final Map.Entry<String, T> entry : toFilter.entrySet()) {
			if (f.operation(entry.getKey())) {
				result.put(entry.getKey(), entry.getValue());
			}
		}
		return result;
	}
	
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
		final Map<String, Serializable> props = 
				node.getBlobHolder().getProperties();

		final Map<String, Object> dc = new HashMap<>(filterMap(props,
				k -> k.startsWith("dc")));
		dm.setProperties("dublincore", dc);
		
		dm.addFacet("Harvested");
		final Map<String, Object> hrv = new HashMap<>(filterMap(props,
				k -> k.startsWith("hrv:")));
		dm.setProperties("HarvestedRecord", hrv);
		
		return dm;
	}

}
