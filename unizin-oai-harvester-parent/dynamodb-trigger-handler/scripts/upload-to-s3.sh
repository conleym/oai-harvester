#!/bin/bash

set -e

BUCKET="unizin-lambda-functions"

# The name of the maven project (and thus the resulting jar) is the name of the
# current directory. We need to grab this because the build produces more than
# one jar, and we want to upload only one.
PWD=$(pwd)
PROJ=$(basename "${PWD}")
JAR=$(find target -name "${PROJ}-*.zip")
JAR="${PWD}/${JAR}"
echo "Uploading ${JAR} to ${BUCKET}."
aws s3 cp "${JAR}" "s3://${BUCKET}/"
