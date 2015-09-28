package org.unizin.catalog.importer;

import java.io.File;
import java.net.URL;

import org.apache.commons.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.TransactionalFeature;
import org.nuxeo.ecm.platform.importer.base.GenericMultiThreadedImporter;
import org.nuxeo.ecm.platform.importer.base.ImporterRunnerConfiguration.Builder;
import org.nuxeo.ecm.platform.importer.log.BufferredLogger;
import org.nuxeo.ecm.platform.importer.log.ImporterLogger;
import org.nuxeo.ecm.platform.importer.source.SourceNode;
import org.nuxeo.ecm.platform.test.PlatformFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.RuntimeFeature;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features({TransactionalFeature.class, RuntimeFeature.class, CoreFeature.class,
	PlatformFeature.class})
@Deploy({"org.unizin.catalog.schemas"})
public final class ImporterTest {
	private static final String ZF_NAME = "/harvester-test.zip";
	private static final URL TEST_ZIP = ImporterTest.class.getResource(ZF_NAME);
	
    @Inject
    protected CoreSession session;
	
	@Test
	public void doImport() throws Exception {
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
		
		final DocumentModelList docs = session.query("select * from File");
		Assert.assertEquals(231, docs.totalSize());
		for (final DocumentModel doc : docs) {
			System.out.println(doc.getTitle());
		}
	}
}
