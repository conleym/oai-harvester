package org.unizin.cmp.search.operations;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationChain;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.test.AutomationFeature;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveReader;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@Deploy({"org.unizin.cmp.schemas", "org.unizin.cmp.search"})
public class CopyFromSourceRepositoryTest {

    @Inject
    AutomationService automationService;

    @Inject
    CoreSession session;

    private DocumentModel createTestDoc() throws IOException {
        InputStream archiveStream = getClass().getResourceAsStream("/testdoc.zip");
        DocumentReader reader = new NuxeoArchiveReader(archiveStream);
        DocumentWriter writer = new DocumentModelWriter(session, "/");
        DocumentPipe pipe = new DocumentPipeImpl();
        pipe.setReader(reader);
        pipe.setWriter(writer);
        pipe.run();
        session.save();
        return session.getDocument(new PathRef("/Untitled.1444280484660"));
    }

    @Test
    public void testCopyFromSourceRepository() throws
            OperationException,
            IOException {
        DocumentModel inputDoc = createTestDoc();
        OperationContext context = new OperationContext(session);
        context.setInput(inputDoc);
        OperationChain chain = new OperationChain("testCopyFromSourceRepository");
        chain.add(CopyFromSourceRepository.ID);
        DocumentModel outputDoc =
                (DocumentModel) automationService.run(context, chain);
    }
}
