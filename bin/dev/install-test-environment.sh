#!/bin/sh

DEVELOPMENT_BIN=~/Development/bin
TEAMCITY_HOME=$DEVELOPMENT_BIN/teamcity

mkdir -p $TEAMCITY_HOME
cd $TEAMCITY_HOME

mkdir data
mkdir logs

docker run -d -it --name teamcity-server-instance  \
    -v $TEAMCITY_HOME/data:/data/teamcity_server/datadir \
    -v $TEAMCITY_HOME/teamcity/logs:/opt/teamcity/logs \
    -p 8111:8111 \
    jetbrains/teamcity-server

