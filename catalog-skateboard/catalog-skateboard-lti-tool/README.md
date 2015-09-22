
Developing JavaScript Client
============================

1. (inside VM) in `/etc/nuxeo/nuxeo.conf` add:

    org.unizin.catalogSearch.jsPath=https://localhost:9595

2. (inside VM) Restart Nuxeo
3. (outside VM) `npm install`
4. (outside VM) `npm start`

As long as `npm start` is running, `/nuxeo/site/catalog/` will load JS from your
webpack server. It will auto-reload any time you change some JS.

Important paths to know:
========================

* `/etc/nuxeo/nuxeo.conf`
* `/var/lib/nuxeo/server/nxserver/bundles/catalog-skateboard-lti-tool-1.0-SNAPSHOT.jar`
