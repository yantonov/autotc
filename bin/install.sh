#!/bin/bash

mvn_output=`mvn --version | grep "Apache Maven"`
if [ -z "$mvn_output" ]; then
    echo "mvn not found";
    echo "you can get apache maven here: http://maven.apache.org"
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

