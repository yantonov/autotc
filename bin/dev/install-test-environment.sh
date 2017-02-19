#!/bin/sh

DEVELOPMENT_BIN=~/Development/bin
TEAMCITY_HOME=$DEVELOPMENT_BIN/teamcity

if [ ! -d "$TEAMCITY_HOME" ];
then
    mkdir -p $TEAMCITY_HOME
fi
cd $TEAMCITY_HOME

if [ ! -d "$TEAMCITY_HOME/data" ];
then
    mkdir data
fi

if [ ! -d "$TEAMCITY_HOME/logs" ];
then
    mkdir logs
fi

docker run -d -it --name teamcity-server-instance  \
    -v $TEAMCITY_HOME/data:/data/teamcity_server/datadir \
    -v $TEAMCITY_HOME/teamcity/logs:/opt/teamcity/logs \
    -p 8111:8111 \
    jetbrains/teamcity-server

