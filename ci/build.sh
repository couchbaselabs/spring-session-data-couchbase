#!/bin/bash

set -euo pipefail

[[ -d $PWD/maven && ! -d $HOME/.m2 ]] && ln -s $PWD/maven $HOME/.m2

spring_session_data_couchbase_artifactory=$(pwd)/spring-session-data-couchbase-artifactory

rm -rf $HOME/.m2/repository/org/springframework/ws 2> /dev/null || :

cd spring-session-data-couchbase-github

./mvnw -Pdistribute,docs -Dmaven.test.skip=true clean deploy \
    -DaltDeploymentRepository=distribution::default::file://${spring_session_data_couchbase_artifactory}
