package org.unizin.cmp.oai.harvester.simple;

import org.junit.Test;
import org.unizin.cmp.oai.harvester.simple.ConfigReader;
import org.unizin.cmp.oai.harvester.simple.Repository;

import javax.xml.bind.JAXBException;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ConfigReaderTest {
    @Test
    public void testParse() throws JAXBException {
        ConfigReader reader = new ConfigReader();
        InputStream stream = getClass().getResourceAsStream("/testrepositories.xml");
        List<Repository> repo = reader.parse(stream);
        assertEquals(16, repo.get(0).sets.size());
    }
}
