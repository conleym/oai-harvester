package org.unizin.cmp.oai.harvester.simple;

import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unizin.cmp.oai.OAIVerb;
import org.unizin.cmp.oai.harvester.HarvestParams;
import org.unizin.cmp.oai.harvester.Harvester;
import org.unizin.cmp.oai.harvester.exception.OAIProtocolException;
import org.unizin.cmp.oai.harvester.response.FilesOAIResponseHandler;
import org.unizin.cmp.oai.harvester.response.OAIResponseHandler;

public class SimpleHarvester {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleHarvester.class);
    public static void main(String[] args) throws
    JAXBException {
        ConfigReader reader = new ConfigReader();
        ExecutorService threadPool = Executors.newCachedThreadPool();
        for (Repository repository : reader.parse(new File("repositories.xml"))) {
            threadPool.execute(() -> {
                LOGGER.info("Starting harvest of " + repository.name);
                try {
                    File repoOutputDir = new File(repository.name);
                    repoOutputDir.mkdir();
                    for (String set: repository.sets) {
                        File setOutputDir = new File(repoOutputDir.toURI()
                                .resolve(set.replaceAll(":", "/")));
                        setOutputDir.mkdirs();
                        Harvester harvester = new Harvester.Builder()
                                .build();
                        HarvestParams params = new HarvestParams.Builder(repository.url,
                                OAIVerb.LIST_RECORDS)
                                .withSet(set)
                                .build();
                        OAIResponseHandler responseHandler =
                                new FilesOAIResponseHandler(setOutputDir);
                        harvester.start(params, responseHandler);
                    }
                } catch (OAIProtocolException e) {
                    LOGGER.error("OAI errors harvesting from {}: {}",
                            repository.name, e.getOAIErrors());
                } catch (Exception e) {
                    LOGGER.error("Error harvesting from " + repository.name, e);
                }
                LOGGER.info("Finished harvest of " + repository.name);
            });
        }
    }
}
