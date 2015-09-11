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

    // Most of the development work can be done without involving Canvas and the
    // LTI process. This function is for working on the UI
    @GET
    public Object doGet() {
        String ext_content_return_url = "https://unizin.instructure.com/courses/31/external_content/success/external_tool_dialog";

        return getView("index").arg("jsPath",
                                    Framework.getProperty(
                                            "org.unizin.catalogSearch.jsPath"))
                               .arg("ext_content_return_url",
                                    ext_content_return_url);
    }

    // This is the real way that Canvas will LTI over
    @POST
    public Object doPost(@Context HttpServletRequest request) {
    	Map<String, String[]> params = request.getParameterMap();
    	String ext_content_return_url = params.get("ext_content_return_url")[0];

    	return getView("index").arg("jsPath",
    			Framework.getProperty(
    					"org.unizin.catalogSearch.jsPath"))
    			.arg("ext_content_return_url",
    					ext_content_return_url);
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
                    nuxeoURI.getScheme(), null, nuxeoURI.getHost(), nuxeoURI.getPort(), null, null, null);
            return getView("showConfig").arg("nuxeoHost", nuxeoURI.getHost())
                    .arg("nuxeoURL", nuxeoNoPath.toString());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
