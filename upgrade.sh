#!/bin/sh -ex

./gradlew --daemon clean install -xjavadoc -xanimalSniffer
./gradlew --stop
jps -l | grep GradleDaemon | cut -f1 -d' ' | xargs -rt kill
