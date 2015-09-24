# unizin-cmp
Unizin Content Management Platform and Tools

This repository is intended to implement the functionality described in the
[scope statement](https://github.com/unizin/unizin-cmp/wiki/Scope-Statement).
There are two components that use a common Nuxeo repository: an LTI tool and an
OAI-PMH harvester.

## LTI Tool
For this project, a [Nuxeo WebEngine](https://doc.nuxeo.com/pages/viewpage.action?pageId=950281)
application will implement the LTI integration and basic search interfaces.
It should save time to use the existing Nuxeo implementations of search views,
results views, and item preview, as well as the built-in OAuth authentication, 
validation, and encryption facilities.  The server-side Nuxeo components are
contained in `unizin-cmp/unizin-parent/`.

## OAI-PMH Harvester
This will use an "off-the-shelf" open source OAI harvesting library and the
Nuxeo client library to harvest metadata records from OAI-PMH servers and
create corresponding metadata records in a Nuxeo repository.  It will run in
a separate process and interact with the Nuxeo repository via the REST API.
The harvester code is contained in `unizin-cmp/unizin-harvester/`.
