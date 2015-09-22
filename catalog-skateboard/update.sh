#!/bin/bash

set -ex

mvn clean package

SRC=$(ls catalog-skateboard-marketplace-package/target/*.zip)
FILE=$(basename $SRC)

scp $SRC nuxeo:/tmp/$FILE

ssh -q nuxeo << EOF
sudo service nuxeo stop
sudo nuxeoctl mp-remove catalog-skateboard
sudo nuxeoctl mp-install /tmp/$FILE
sudo service nuxeo start
EOF
