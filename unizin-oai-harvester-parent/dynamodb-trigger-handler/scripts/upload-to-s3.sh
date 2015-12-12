#!/bin/bash

set -e

BUCKET="unizin-lambda-functions"

# The name of the maven project (and thus the resulting zip file) is the name of
# the current directory.
PWD=$(pwd)
PROJ=$(basename "${PWD}")
ZIP=$(find target -name "${PROJ}-*.zip")
ZIP="${PWD}/${ZIP}"
echo "Uploading ${ZIP} to ${BUCKET}."
aws s3 cp "${ZIP}" "s3://${BUCKET}/"
