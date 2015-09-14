package org.unizin.catalog.harvester.oai;

import javax.xml.namespace.QName;

public class OAIConstants {
    private OAIConstants() {}
    public static final String OAI_NS_URL = "http://www.openarchives.org/OAI/2.0/";
    public static final QName RESUMPTION_TOKEN = new QName(OAI_NS_URL, "resumptionToken");
    public static final QName CURSOR = new QName("cursor");
    public static final QName COMPLETE_LIST_SIZE = new QName("completeListSize");
    public static final QName EXPIRATION_DATE = new QName("expirationDate");

}
