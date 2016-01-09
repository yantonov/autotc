#!/bin/sh

SCRIPT_DIR=$(cd `dirname $0` && pwd)

cd $SCRIPT_DIR/../../

# clean all old cljs files
rm -rf resources/public/cljs/dev
# regenerate new files
lein cljsbuild once development

echo "generation cljs -> js has completed!"
