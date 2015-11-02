#!/bin/bash

# Usage: jenkins-build.sh MAVEN_TARGETS

export JAVA_HOME="/var/lib/jenkins/tools/hudson.model.JDK/Java_1.8"
export M2_HOME="/var/lib/jenkins/tools/hudson.tasks.Maven_MavenInstallation/nuxeo"

set -e

"${M2_HOME}/bin/mvn" -f unizin-oai-harvester-parent/ "$@"
