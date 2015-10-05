package org.unizin.catalog.harvester.config;

import javax.xml.bind.annotation.*;
import java.net.URI;
import java.util.List;

@XmlRootElement(name="repository")
@XmlAccessorType(XmlAccessType.FIELD)
public class Repository {
    @XmlElement(name="name")
    public String name;

    @XmlElement(name="institution")
    public String institution;

    @XmlElement(name="url")
    public URI url;

    @XmlElementWrapper(name="sets")
    @XmlElement(name="set")
    public List<String> sets;

}
