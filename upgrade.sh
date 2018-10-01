#!/bin/sh -ex

./gradlew --daemon clean install -xjavadoc
./gradlew --stop
jps -l | grep GradleDaemon | cut -f1 -d' ' | xargs -rt kill
