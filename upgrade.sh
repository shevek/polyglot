#!/bin/sh

./gradlew --daemon clean install
./gradlew --stop
