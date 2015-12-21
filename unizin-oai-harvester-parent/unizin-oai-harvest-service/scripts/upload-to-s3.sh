#!/bin/bash

set -e

BUCKET="unizin-deploy"
KEY="oai-harvester"

# The name of the maven project (and thus the resulting tarball) is the name of
# the current directory.
PWD=$(pwd)
PROJ=$(basename "${PWD}")
JAR=$(find target -name "${PROJ}-*.jar")
JAR="${PWD}/${JAR}"
DEST="${BUCKET}/${KEY}"
echo "Uploading ${JAR} to ${DEST}."
aws s3 cp "${JAR}" "s3://${DEST}/"
