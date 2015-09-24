package org.unizin.catalog.harvester.config;

import javax.xml.bind.annotation.*;
import java.net.URI;

@XmlRootElement(name="repository")
@XmlAccessorType(XmlAccessType.FIELD)
public class Repository {
    @XmlElement(name="name")
    public String name;

    @XmlElement(name="institution")
    public String institution;

    @XmlElement(name="url")
    public URI url;
}
