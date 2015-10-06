#!/bin/bash


if [ -z "$1" ]; then
    cat << EOF
USAGE: $0 VMName

For this script to work you MUST be able to \`ssh VMName\`. Since everyone has a
custom setup there\'s no way to know what this name is.

$ cd /path/where/you/have/Vagrantfile
$ vagrant ssh-config

look at the first line here. Mine says \`Host default\`, but that\'s not a good
name. Before I copy this into ~/.ssh/config I changed my host to \`nuxeo\`. Now I
can \`./update.sh nuxeo\`

EOF
    exit 1
fi

set -ex

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
sudo nuxeoctl mp-remove unizin-search || true
sudo nuxeoctl mp-install /tmp/$FILE
sudo service nuxeo start
EOF
