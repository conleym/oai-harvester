#!/bin/bash

set -e

cd unizin-oai-harvester-parent
(cd unizin-oai-harvest-service
./scripts/upload-to-s3.sh
)

(cd dynamodb-trigger-handler
 ./scripts/upload-to-s3.sh
)
