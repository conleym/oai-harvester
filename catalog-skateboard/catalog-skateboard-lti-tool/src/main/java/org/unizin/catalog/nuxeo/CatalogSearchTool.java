package org.unizin.catalog.nuxeo;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

@Path("/catalog")
@Produces("text/html;charset=UTF-8")
@WebObject(type="CatalogSearchTool")
public class CatalogSearchTool extends ModuleRoot {

    @GET
    public Object doGet() {
        return getView("index");
    }
}
