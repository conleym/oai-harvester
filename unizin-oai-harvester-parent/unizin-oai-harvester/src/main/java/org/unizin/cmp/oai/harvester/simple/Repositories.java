package org.unizin.cmp.oai.harvester.simple;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name="repositories")
@XmlAccessorType(XmlAccessType.FIELD)
public class Repositories {
    @XmlElement(name="repository")
    public List<Repository> repositories = new ArrayList<>();
}
