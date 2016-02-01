#!/bin/sh

./gradlew --daemon clean install -xjavadoc -xanimalSniffer
./gradlew --stop
