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
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import javax.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(AutomationFeature.class)
@Deploy({"org.unizin.cmp.schemas", "org.unizin.cmp.search"})
public class CopyFromSourceRepositoryTest {

    @Inject
    AutomationService automationService;

    @Inject
    CoreSession session;

    private DocumentModel createTestDoc() {
        DocumentModel inputDoc = session.createDocumentModel("File");
        inputDoc.addFacet("Harvested");
        return inputDoc;
    }

    @Test
    public void testCopyFromSourceRepository() throws OperationException {
        DocumentModel inputDoc = createTestDoc();
        OperationContext context = new OperationContext(session);
        context.setInput(inputDoc);
        OperationChain chain = new OperationChain("testCopyFromSourceRepository");
        chain.add(CopyFromSourceRepository.ID);
        DocumentModel outputDoc =
                (DocumentModel) automationService.run(context, chain);
    }
}
