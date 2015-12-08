#!/bin/sh

git pull
./gradlew --daemon clean install
./gradlew --stop
