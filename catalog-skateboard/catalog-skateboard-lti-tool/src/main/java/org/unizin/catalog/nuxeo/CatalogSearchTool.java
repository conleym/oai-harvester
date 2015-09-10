package org.unizin.catalog.nuxeo;

import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;


@Path("/catalog")
@Produces("text/html;charset=UTF-8")
@WebObject(type="CatalogSearchTool")
public class CatalogSearchTool extends ModuleRoot {

    @GET
    public Object doGet() {
        return getView("index").arg("jsPath",
                                    Framework.getProperty(
                                            "org.unizin.catalogSearch.jsPath"));
    }

    @POST
    @Path("showLaunch")
    @Produces("text/html;charset=UTF-8")
    public Object showLaunch(@Context HttpServletRequest request) {
        Map<String, Object> params = new HashMap<>();
        for (Map.Entry<String, String[]> item :
                request.getParameterMap().entrySet()) {
            params.put(item.getKey(), item.getValue()[0]);
        }
        return getView("showLaunch").arg("postParams", params);
    }

    @GET
    @Path("config.xml")
    @Produces("application/xml;charset=UTF-8")
    public Object showConfig() {
        try {
            URI nuxeoURI = new URI(Framework.getProperty("nuxeo.url"));
            URI nuxeoNoPath = new URI(
                    nuxeoURI.getScheme(), nuxeoURI.getHost(), null, null);
            return getView("showConfig").arg("nuxeoHost", nuxeoURI.getHost())
                    .arg("nuxeoURL", nuxeoNoPath.toString());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
