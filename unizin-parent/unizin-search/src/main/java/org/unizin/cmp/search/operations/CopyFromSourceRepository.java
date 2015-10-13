package org.unizin.cmp.search.operations;

import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.core.api.DocumentModel;

@Operation(id = CopyFromSourceRepository.ID,
           label = "Copy from source repository",
           description = "Attempt to copy this document's file blob from " +
           "hrv:sourceRepository to the local repository.")
public class CopyFromSourceRepository  {
    public static final String ID = "UnizinCMP.CopyFromSourceRepository";
    @OperationMethod
    public DocumentModel run(DocumentModel doc) {
        return doc;
    }
}
