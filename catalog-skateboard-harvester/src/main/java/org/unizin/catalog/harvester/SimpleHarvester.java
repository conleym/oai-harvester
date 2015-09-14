package org.unizin.catalog.harvester;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unizin.catalog.harvester.config.ConfigReader;
import org.unizin.catalog.harvester.config.Repository;
import org.unizin.catalog.harvester.oai.OAIClient;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SimpleHarvester {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleHarvester.class);
    public static void main(String[] args) throws
            JAXBException,
            IOException,
            XMLStreamException {
        ConfigReader reader = new ConfigReader();
        OAIClient client = new OAIClient();
        ExecutorService threadPool = Executors.newCachedThreadPool();
        for (Repository repository : reader.parse(new File("repositories.xml"))) {
            threadPool.execute(() -> {
                LOGGER.info("Starting harvest of " + repository.name);
                try (FileOutputStream out = new FileOutputStream(
                        repository.name + ".xml")) {
                    client.listRecords(repository.url, out);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (XMLStreamException e) {
                    e.printStackTrace();
                }
                LOGGER.info("Finished harvest of " + repository.name);
            });

        }
    }

}
