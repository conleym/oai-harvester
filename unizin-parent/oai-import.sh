#!/bin/bash

request() {
    curl --basic -u 'Administrator:Administrator' "$@"
}


WORKSPACE="Harvested"
TARGET_PATH="/default-domain/workspaces/${WORKSPACE}"
INPUT_PATH="/unizin-cmp/harvests.zip"
BATCH_SIZE=10
MAX_THREADS=10

# You can set NX_URL to something else to run on a different server if you like.
${NX_URL:="http://localhost:8080/nuxeo/site"}
${TARGET_PATH:="/unizin-cmp/harvests.zip"}

WORKSPACES_URL="${NX_URL}/api/v1/path/default-domain/workspaces"

# File importer URLs
BASE_URL="${NX_URL}/fileImporter"
RUN_URL="${BASE_URL}/run?targetPath=${TARGET_PATH}&inputPath=${INPUT_PATH}&batchSize=${BATCH_SIZE}&interactive=false&nbThreads=${MAX_THREADS}&skipRootContainerCreation=false"
LOG_ACTIVATE_URL="${BASE_URL}/logActivate"
LOG_URL="${BASE_URL}/log"

# Create the workspace.
echo "Creating workspace if necessary."

request "${WORKSPACES_URL}"

read -r -d '' POST_DATA <<EOF
{
  "entity-type": "document",
  "name": "${WORKSPACE}",
  "type": "Workspace",
  "properties": {"dc:title": "${WORKSPACE}"}
}
EOF

request -v -X POST "${WORKSPACES_URL}" -H 'content-type:application/json' --data-binary "${POST_DATA}"

printf "\nStarting import.\n"
request "${LOG_ACTIVATE_URL}"
request "${RUN_URL}"
printf "\nVisit %s for logs\n" "${LOG_URL}"
