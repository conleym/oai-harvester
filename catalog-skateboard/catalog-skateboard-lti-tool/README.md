
Developing JavaScript Client
============================

1. in `/etc/nuxeo/nuxeo.conf` add:

    org.unizin.catalogSearch.jsPath=http://localhost:8080

2. Restart Nuxeo
3. `npm install`
4. `npm start`

As long as `npm start` is running, `/nuxeo/site/catalog/` will load JS from your
webpack server. It will auto-reload any time you change some JS.

Important paths to know:
========================

* `/etc/nuxeo/nuxeo.conf`
* `/var/lib/nuxeo/server/nxserver/bundles/catalog-skateboard-lti-tool-1.0-SNAPSHOT.jar`
