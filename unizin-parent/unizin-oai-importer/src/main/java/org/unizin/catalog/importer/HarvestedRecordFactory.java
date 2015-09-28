package org.unizin.catalog.importer;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.importer.factories.AbstractDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.factories.DefaultDocumentModelFactory;
import org.nuxeo.ecm.platform.importer.source.SourceNode;


/**
 * Creates new documents with the {@code Harvested} facet.
 *
 */
public final class HarvestedRecordFactory extends AbstractDocumentModelFactory {

	private final DefaultDocumentModelFactory dmf = 
			new DefaultDocumentModelFactory();

	private static <T> Map<String, T> filterByKeys(
			final Map<String, T> toFilter, Predicate<String> f) {
		final Map<String, T> result = new HashMap<>();
		for (final Map.Entry<String, T> entry : toFilter.entrySet()) {
			if (f.test(entry.getKey())) {
				result.put(entry.getKey(), entry.getValue());
			}
		}
		return result;
	}
	
	@Override
	public DocumentModel createFolderishNode(final CoreSession session, 
			final DocumentModel parent, final SourceNode node)
			throws IOException {
		return dmf.createFolderishNode(session, parent, node);
	}

	@Override
	public DocumentModel createLeafNode(final CoreSession session, 
			final DocumentModel parent, final SourceNode node)
					throws IOException {
		final DocumentModel dm = dmf.createLeafNode(session, parent, node);
		final Map<String, Serializable> props = 
				node.getBlobHolder().getProperties();

		final Map<String, Object> dc = new HashMap<>(filterByKeys(props,
				k -> k.startsWith("dc:")));
		dm.setProperties("dublincore", dc);
		
		// Facets can be added an ecm: property on the BlobHolder, but I 
		// think it's better to be explicit.
		dm.addFacet("Harvested");
		
		final Map<String, Object> hrv = new HashMap<>(filterByKeys(props,
				k -> k.startsWith("hrv:")));
		dm.setProperties("HarvestedRecord", hrv);
		
		return dm;
	}

}
