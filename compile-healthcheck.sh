#!/bin/sh

# create build directory for class files if it's not there already
if [ ! -d build ]; then
  mkdir build
fi

$JAVA_HOME/bin/javac -d build src/org/ensembl/healthcheck/util/*.java src/org/ensembl/healthcheck/testcase/*.java src/org/ensembl/healthcheck/*.java

$JAVA_HOME/bin/jar cf lib/ensj-healthcheck.jar -C build org images/*.gif