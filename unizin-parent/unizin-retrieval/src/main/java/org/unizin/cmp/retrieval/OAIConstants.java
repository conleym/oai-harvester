package org.unizin.cmp.retrieval;

import yarfraw.generated.rss10.elements.RDF;

import javax.xml.namespace.NamespaceContext;
import java.util.Iterator;


public class OAIConstants {
    private OAIConstants() {}
    public static final String OAI_NS_URI = "http://www.openarchives.org/OAI/2.0/";
    public static final String DC_NS_URI = "http://purl.org/dc/elements/1.1/";
    public static final String OAI_DC_NS_URI = "http://www.openarchives.org/OAI/2.0/oai_dc/";
    public static final String XSI_NS_URI = "http://www.w3.org/2001/XMLSchema-instance";
    public static final String ATOM_NS_URI = "http://www.w3.org/2005/Atom";
    public static final String ORE_NS_URL = "http://www.openarchives.org/ore/terms/";
    public static final String OREATOM_NS_URL = "http://www.openarchives.org/ore/atom/";
    public static final String RDF_NS_URL = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    public static class OAIRecordContext implements NamespaceContext {

        @Override
        public String getNamespaceURI(String prefix) {
            String result;
            switch (prefix) {
            case "oai":
                result = OAI_NS_URI;
                break;
            case "dc":
                result = DC_NS_URI;
                break;
            case "oai_dc":
                result = OAI_DC_NS_URI;
                break;
            case "xsi":
                result = XSI_NS_URI;
                break;
            case "atom":
                result = ATOM_NS_URI;
                break;
            case "ore":
                result = ORE_NS_URL;
                break;
            case "oreatom":
                result = OREATOM_NS_URL;
                break;
            case "rdf":
                result = RDF_NS_URL;
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
}
