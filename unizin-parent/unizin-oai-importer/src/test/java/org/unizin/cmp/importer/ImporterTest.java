package org.unizin.cmp.importer;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.platform.importer.base.GenericMultiThreadedImporter;
import org.nuxeo.ecm.platform.importer.base.ImporterRunnerConfiguration.Builder;
import org.nuxeo.ecm.platform.importer.log.BufferredLogger;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.service.DefaultImporterService;
import org.nuxeo.ecm.platform.importer.service.DefaultImporterServiceImpl;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({TransactionalFeature.class, PlatformFeature.class})
@Deploy({"org.unizin.cmp.schemas", "org.unizin.cmp.importer"})
public final class ImporterTest {
	private static final String ZIP_NAME = "/harvester-test.zip";
	private static final URL TEST_ZIP = ImporterTest.class.getResource(ZIP_NAME);

	@Inject
	protected CoreSession session;

	private static List<String> getHrvProperty(final String prop,
			final DocumentModel dm) {
		// Most hrv: properties are arrays, but lists print out nicely, making
		// it easy to see why a test failed.
		final Object obj = dm.getProperty("HarvestedRecord", prop);
		if (obj.getClass().isArray()) {
			return Arrays.asList((String[])obj);
		}
		// Some are not arrays, though.
		return Arrays.asList((String)obj);
	}
	
	@Test
	public void testImport() throws Exception {
		final File test = new File(TEST_ZIP.getFile());
		final SourceNode sourceNode = new HarvestedZipFileSourceNode(test);
		final String importWritePath = "/";
		final ImporterLogger importerLogger = new BufferredLogger(LogFactory.getLog("import"));
		final Builder builder = new Builder(sourceNode, importWritePath, importerLogger)
				.skipRootContainerCreation(true)
				;
		GenericMultiThreadedImporter imp = new GenericMultiThreadedImporter(builder.build());
		imp.setFactory(new HarvestedRecordFactory());
		imp.run();
		
		// Count to make sure all the documents were loaded from test data.
		DocumentModelList docs = session.query("select * from File");
		Assert.assertEquals(231, docs.totalSize());
		
		// Pick out one document to check more thoroughly.
		final String testDocTitle = "Trucks involved in fatal accidents factbook 2002";
		final String testDocIdentifier = "oai:deepblue.lib.umich.edu:2027.42/1561";
		
		docs = session.query(String.format("select * from File where hrv:oaiIdentifier = '%s'",
				testDocIdentifier));
		Assert.assertEquals(1, docs.totalSize());
		
		final DocumentModel testDoc = docs.get(0);
		Assert.assertEquals(Collections.singletonList(testDocIdentifier), 
				getHrvProperty("oaiIdentifier", testDoc));
		Assert.assertEquals(testDocTitle, testDoc.getTitle());
		Assert.assertEquals(testDocTitle, 
				testDoc.getProperty("dublincore", "title"));
		Assert.assertEquals(Collections.singletonList(testDocTitle),
				getHrvProperty("title", testDoc));
	}
	
	
	@Deploy("org.nuxeo.ecm.platform.importer.core")
	@Test
	public void testServiceConfigurationContribution() throws Exception {
		final DefaultImporterServiceImpl dis = 
				(DefaultImporterServiceImpl)Framework.getService(DefaultImporterService.class);
		Assert.assertEquals(HarvestedZipFileSourceNode.class,
				dis.getSourceNodeClass());
		Assert.assertEquals(HarvestedRecordFactory.class,
				dis.getDocModelFactoryClass());
	}
}
