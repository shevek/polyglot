#!/bin/sh

./gradlew --daemon clean install -xjavadoc
./gradlew --stop
