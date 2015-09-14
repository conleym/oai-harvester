package org.unizin.catalog.harvester;

import org.apache.commons.io.output.NullOutputStream;
import org.unizin.catalog.harvester.config.ConfigReader;
import org.unizin.catalog.harvester.config.Repository;
import org.unizin.catalog.harvester.oai.OAIClient;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class SimpleHarvester {
    public static void main(String[] args) throws
            JAXBException,
            IOException,
            XMLStreamException {
        ConfigReader reader = new ConfigReader();
        OAIClient client = new OAIClient();
        for (Repository repository : reader.parse(new File("repositories.xml"))) {
            try (FileOutputStream out = new FileOutputStream(repository.name + ".xml")) {
                client.listRecords(repository.url, out);
            }
        }
    }

}
