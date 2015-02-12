#!/bin/bash

output=`java -version 2>&1 >/dev/null | grep "Java(TM) SE Runtime Environment"`
if [ -z "$output" ]; then
    echo "[ERROR] - java not found";
    echo "[INFO] - you can get it here: www.oracle.com/technetwork/java/javase/downloads/index.html"
    exit 0
fi

output=`mvn --version | grep "Apache Maven"`
if [ -z "$output" ]; then
    echo "[ERROR] - mvn not found";
    echo "[INFO] - you can it here: http://maven.apache.org"
    exit 0
fi

output=`lein -version | grep "Leiningen"`
if [ -z "$output" ]; then
    echo "[ERROR] - lein not found";
    echo "[INFO] - you can get it here: http://leiningen.org"
    exit 0
fi

SCRIPT_DIR=$(cd `dirname $0` && pwd)

cd $SCRIPT_DIR

mkdir tmp
cd tmp

git clone https://github.com/yantonov/jcxsp.git
cd jcxsp
mvn install
cd ../

git clone https://github.com/yantonov/tc-api.git
cd tc-api
mvn install
cd ../


cd ../

rm -rf tmp

cd ../

lein deps

