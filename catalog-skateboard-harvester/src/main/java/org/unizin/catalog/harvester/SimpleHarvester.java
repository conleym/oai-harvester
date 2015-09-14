package org.unizin.catalog.harvester;

import org.unizin.catalog.harvester.config.ConfigReader;
import org.unizin.catalog.harvester.config.Repository;

import javax.xml.bind.JAXBException;
import java.io.File;

public class SimpleHarvester {
    public static void main(String[] args) throws JAXBException {
        ConfigReader reader = new ConfigReader();
        for (Repository repository : reader.parse(new File("repositories.xml"))) {
            System.out.println(repository.url);
        }
    }

}
