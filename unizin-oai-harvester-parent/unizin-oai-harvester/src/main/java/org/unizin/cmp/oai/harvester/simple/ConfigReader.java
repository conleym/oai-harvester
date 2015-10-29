package org.unizin.cmp.oai.harvester.simple;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.InputStream;
import java.util.List;

public class ConfigReader {

    private final Unmarshaller unmarshaller;

    public ConfigReader() throws JAXBException {
        JAXBContext jc = JAXBContext.newInstance(Repositories.class);
        unmarshaller = jc.createUnmarshaller();
    }

    public List<Repository> parse(File inputFile) throws JAXBException {
        Repositories repositories = (Repositories) unmarshaller.unmarshal(inputFile);
        return repositories.repositories;
    }

    public List<Repository> parse(InputStream stream) throws JAXBException {
        Repositories repositories = (Repositories) unmarshaller.unmarshal(stream);
        return repositories.repositories;
    }
}
