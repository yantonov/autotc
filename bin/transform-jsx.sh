#!/bin/bash

SCRIPT_DIR=$(cd `dirname $0` && pwd)

jsx --extension jsx $SCRIPT_DIR/../resources/public/js $SCRIPT_DIR/../resources/public/js/combined
