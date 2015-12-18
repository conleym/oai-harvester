#!/bin/bash

set -e

BUCKET="unizin-deploy"
KEY="oai-harvester"

# The name of the maven project (and thus the resulting tarball) is the name of
# the current directory.
PWD=$(pwd)
PROJ=$(basename "${PWD}")
TGZ=$(find target -name "${PROJ}-*.tar.gz")
TGZ="${PWD}/${TGZ}"
DEST="${BUCKET}/${KEY}"
echo "Uploading ${TGZ} to ${DEST}."
aws s3 cp "${TGZ}" "s3://${DEST}/"
