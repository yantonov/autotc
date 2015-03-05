#!/bin/sh

SCRIPT_DIR=$(cd `dirname $0` && pwd)

jsx --extension jsx --watch $SCRIPT_DIR/../resources/public/js $SCRIPT_DIR/../resources/public/js/combined

