#!/bin/bash

SCRIPT_DIR=$(cd `dirname $0` && pwd)

output=`jsx --help | grep "Usage:"`
if [ -z "$output" ]; then
    echo "[ERROR] - jsx not found";
    echo "[INFO] - you can install it using node js package manager: "
    echo "\$> npm install -g react-tool"
    exit 0
fi

jsx --extension jsx $SCRIPT_DIR/../resources/public/js $SCRIPT_DIR/../resources/public/js/combined
