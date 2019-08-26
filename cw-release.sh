#!/bin/sh -ex
./gradlew -i clean publishReleasePublicationToMavenLocal

SRC="$HOME/.m2/repository/org/anarres/polyglot"
DST="$HOME/cw-resources/private-customer-compilerworks/maven/org/anarres"
mkdir -p "$DST"
rsync -avzP --delete "$SRC" "$DST" --exclude '*sources*' --exclude '*javadoc*'
