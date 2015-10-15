package org.unizin.cmp.retrieval;

import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationContext;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.runtime.api.Framework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetrieveCopyWork extends AbstractWork {

    public static final Logger LOG = LoggerFactory.getLogger(RetrieveCopyWork.class);
    private static final long serialVersionUID = 283389351343964688L;

    public RetrieveCopyWork(String repositoryName, String id) {
        setDocument(repositoryName, id);
    }

    @Override
    public void work() {
        initSession();
        OperationContext context = new OperationContext(session);
        context.setInput(getDocument().getDocRef());
        try {
            AutomationService automationService = Framework.getService(AutomationService.class);
            automationService.run(context, CopyFromSourceRepository.ID);
        } catch (OperationException e) {
            LOG.error("error copying from source repository", e);
        }
    }

    @Override
    public String getTitle() {
        return "UnizinCMP: request copy";
    }
}
