#!/bin/sh

SCRIPT_DIR=$(cd `dirname $0` && pwd)

cd $SCRIPT_DIR/../../

lein cljsbuild once development
# cp target/autotc-web.js resources/public/cljs/dev/

echo "generation cljs -> js has completed!"
