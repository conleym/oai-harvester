package org.unizin.cmp.contribute;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import org.nuxeo.ecm.platform.api.login.UserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.CachableUserIdentificationInfo;
import org.nuxeo.ecm.platform.ui.web.auth.NXAuthConstants;
import org.nuxeo.ecm.platform.ui.web.auth.service.PluggableAuthenticationService;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.api.login.LoginService;


@Path("/contribute")
@Produces("text/html;charset=UTF-8")
@WebObject(type="ContributeTool")
public class ContributeTool extends ModuleRoot {

    // Most of the development work can be done without involving Canvas and the
    // LTI process. This function is for working on the UI
    @GET
    public Object doGet() {
        String ext_content_return_url = "about:blank";

        return getView("index").arg("jsPath",
                                    Framework.getProperty(
                                            "org.unizin.catalogSearch.jsPath"))
                               .arg("ext_content_return_url",
                                    ext_content_return_url);
    }

    @GET
    @Path("frame")
    public Object showFrame() {
        return getView("frame");
    }

    // This is the real way that Canvas will LTI over
    @POST
    public Object doPost(@Context HttpServletRequest request) throws
            LoginException {
        Principal user = request.getUserPrincipal();
        LoginService loginService = Framework.getService(LoginService.class);
        // constructor takes a username/password, but we don't care about them
        UserIdentificationInfo dummy = new UserIdentificationInfo("invalid", "invalid");
        CachableUserIdentificationInfo userInfo =
                new CachableUserIdentificationInfo(dummy);
        userInfo.setLoginContext(loginService.loginAs(user.getName()));
        userInfo.setAlreadyAuthenticated(true);
        userInfo.setPrincipal(user);
        PluggableAuthenticationService authService =
                (PluggableAuthenticationService) Framework.getRuntime().getComponent(
                        PluggableAuthenticationService.NAME);
        HttpSession session = request.getSession();
        session.setAttribute(NXAuthConstants.USERIDENT_KEY, userInfo);
        authService.onAuthenticatedSessionCreated(request, session, userInfo);
    	Map<String, String[]> params = request.getParameterMap();
        String[] urls = params.get("ext_content_return_url");
        String ext_content_return_url = "http://invalid.example.com";
        if (urls != null) {
             ext_content_return_url = urls[0];
        }
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
