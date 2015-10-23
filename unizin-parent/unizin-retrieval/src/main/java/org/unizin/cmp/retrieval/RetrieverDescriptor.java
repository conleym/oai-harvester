package org.unizin.cmp.retrieval;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

import java.io.Serializable;

@XObject("retriever")
public class RetrieverDescriptor implements Serializable {

    private static final long serialVersionUID = 1114480630832270258L;

    @XNode("sourceRepository")
    protected String sourceRepository;

    @XNode("@class")
    private Class<? extends Retriever> retrieverClass;

    public String getSourceRepository() {
        return sourceRepository;
    }

    private Class<? extends Retriever> getRetrieverClass() {
        return retrieverClass;
    }

    public Retriever instance() {
        try {
            return (Retriever) getRetrieverClass().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
