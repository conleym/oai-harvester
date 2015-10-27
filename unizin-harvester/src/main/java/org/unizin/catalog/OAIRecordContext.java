package org.unizin.catalog;

import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;

import org.unizin.cmp.oai.OAI2Constants;

public class OAIRecordContext implements NamespaceContext {

    @Override
    public String getNamespaceURI(String prefix) {
        String result;
        switch (prefix) {
        case "oai":
            result = OAI2Constants.OAI_2_NS_URI;
            break;
        case "dc":
            result = OAI2Constants.DC_NS_URI;
            break;
        case "oai_dc":
            result = OAI2Constants.OAI_DC_NS_URI;
            break;
        case "xsi":
            result = OAI2Constants.XSI_NS_URI;
            break;
        default:
            result = null;
        }
        return result;
    }

    @Override
    public String getPrefix(String namespaceURI) {
        return null;
    }

    @Override
    public Iterator<?> getPrefixes(String namespaceURI) {
        return null;
    }
}
