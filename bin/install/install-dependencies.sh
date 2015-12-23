#!/bin/bash

output=`java -version 2>&1 >/dev/null | grep "Java(TM) SE Runtime Environment"`
if [ -z "$output" ]; then
    echo "[ERROR] - java not found";
    echo "[INFO] - you can get it here: www.oracle.com/technetwork/java/javase/downloads/index.html"
    exit 0
fi

output=`javac 2>&1 >/dev/null | grep "Usage: javac"`
if [ -z "$output" ]; then
    echo "[ERROR] - javac not found, install JDK";
    echo "[INFO] - you can get it here: www.oracle.com/technetwork/java/javase/downloads/index.html"
    exit 0
fi

# check maven
if [ -z "$JAVA_HOME" ]; then
    echo "[ERROR] - JAVA_HOME is undefined"
    exit 0
fi

output=`"$JAVA_HOME"/bin/javac 2>&1 >/dev/null | grep "Usage: javac"`
if [ -z "$output" ]; then
    echo "[ERROR] - JAVA_HOME is not point to JDK"
    exit 0
fi

output=`mvn --version | grep "Apache Maven"`
if [ -z "$output" ]; then
    echo "[ERROR] - mvn not found";
    echo "[INFO] - you can it here: http://maven.apache.org"
    exit 0
fi
# end check maven

output=`lein -version | grep "Leiningen"`
if [ -z "$output" ]; then
    echo "[ERROR] - lein not found";
    echo "[INFO] - you can get it here: http://leiningen.org"
    exit 0
fi

SCRIPT_DIR=$(cd `dirname $0` && pwd)

cd $SCRIPT_DIR

rm -rf tmp
mkdir tmp
cd tmp

git clone https://github.com/yantonov/jcxsp.git
cd jcxsp
git checkout v0.0.1
mvn install
cd ../

git clone https://github.com/yantonov/tc-api.git
cd tc-api
git checkout v0.0.1
mvn install
cd ../

git clone https://github.com/yantonov/rex.git
cd  rex
lein install
cd ../

cd ../

rm -rf tmp

cd ../

# generate js from clojurescript
./bin/dev/generate-production.js

# install mvn dependencies
lein deps
