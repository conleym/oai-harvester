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

SRC=$(ls catalog-skateboard-marketplace-package/target/*.zip)
FILE=$(basename $SRC)

scp $SRC $VM_NAME:/tmp/$FILE

ssh -q $VM_NAME << EOF
sudo service nuxeo stop
sudo nuxeoctl mp-remove catalog-skateboard
sudo nuxeoctl mp-install /tmp/$FILE
sudo service nuxeo start
EOF
