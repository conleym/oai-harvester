
Developing JavaScript Client
============================

1. (inside VM) in `/etc/nuxeo/nuxeo.conf` add:

    org.unizin.catalogSearch.jsPath=https://localhost:9595

2. (inside VM) Restart Nuxeo `sudo nuxeoctl restart`
3. (outside VM) `npm install`
4. (outside VM) `npm start`
5. visit https://localhost:9595 directly so you can tell your browser to accept webpack's self signed cert
  * Chrome is a real pain here. It doesn't allow you to make a permanent exception for this site. Getting Chrome to work here reliably requires a bunch of extra work.

As long as `npm start` is running, `/nuxeo/site/catalog/` will load JS from your
webpack server. It will auto-reload any time you change some JS.



Important paths to know:
========================

* `/etc/nuxeo/nuxeo.conf`
* `/var/lib/nuxeo/server/nxserver/bundles/unizin-search-1.0.0-SNAPSHOT.jar`
