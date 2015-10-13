#!/bin/bash

set -e

BUCKET='s3://courseload-admin/puppet/files/courseload/'
NUXEO_API_BASE="https://connect.nuxeo.com/nuxeo/site/marketplace"
REDIS_SERVER="puppet.us-east-1.ops.courseload.com"
MARKETPLACE_PACKAGES=$(find "${WORKSPACE}" -wholename '**/target/*package*.zip')

echo "Uploading marketplace packages to S3."
s3cmd put ${MARKETPLACE_PACKAGES} "${BUCKET}"
echo "Packages uploaded."


function redis() {
    redis-cli -h "${REDIS_SERVER}" --raw -d " " "$@"
}


OWNER_ID=$(redis get unizin-cmp-owner-id)
USER=$(redis get unizin-cmp-marketplace-user)
PASS=$(redis get unizin-cmp-marketplace-password)

for PACKAGE in ${MARKETPLACE_PACKAGES}; do
    echo "Uploading ${PACKAGE} to nuxeo marketplace."
    curl -i -u "${USER}:${PASS}" -Fpackage=@"${PACKAGE}" \
         "${NUXEO_API_BASE}/upload?batch=true&client=${OWNER_ID}&owner=${OWNER_ID}"
    echo "Package uploaded."
done
