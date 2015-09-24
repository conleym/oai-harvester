#!/bin/bash

set -ex

if [ -z "$1" ]; then
    echo "USAGE: $0 VMName"
    echo "VMName should be whatever you use to ssh to your nuxeo VM"
    exit 1
fi

VM_NAME=$1

# verify the name works before rebuilding everything
ssh $VM_NAME "true" || exit 1

mvn clean package

SRC=$(ls ./search-package/target/search-package-*.zip)
FILE=$(basename $SRC)

scp $SRC $VM_NAME:/tmp/$FILE

ssh -q $VM_NAME << EOF
set -ex
sudo service nuxeo stop
sudo nuxeoctl mp-remove unizin-search
sudo nuxeoctl mp-install /tmp/$FILE
sudo service nuxeo start
EOF
