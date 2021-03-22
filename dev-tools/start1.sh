#
# Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
# or more contributor license agreements. Licensed under the Elastic License
# 2.0 and the Server Side Public License, v 1; you may not use this file except
# in compliance with, at your election, the Elastic License 2.0 or the Server
# Side Public License, v 1.
#

path=$(pwd)

home=/Volumes/Software/Elasticsearch/data/es1

esPath=$path/distribution/archives/darwin-tar/build/install/elasticsearch-8.0.0-SNAPSHOT

ikPath=/Volumes/Software/Elasticsearch/ik
payloadPath=$path/plugins/query-payload/build

serverPath=$path/server/build/distributions

mkdir $home/data
mkdir $home/logs

cp -R $serverPath/elasticsearch-8.0.0-SNAPSHOT.jar $esPath/lib

# copy ik
rm -rf $esPath/plugins/*
mkdir $esPath/plugins/ik
cp -R $ikPath/* $esPath/plugins/ik

# copy payload
mkdir $esPath/plugins/payload
cp -R $payloadPath/generated-resources/* $esPath/plugins/payload
cp -R $payloadPath/distributions/*.jar $esPath/plugins/payload

portDebug=5001

echo "cluster.name: my-application" >  $esPath/config/elasticsearch.yml
echo "node.name: node-1"            >> $esPath/config/elasticsearch.yml
echo "http.port: 9200"              >> $esPath/config/elasticsearch.yml
echo "path.data: "$home/data        >> $esPath/config/elasticsearch.yml
echo "path.logs: "$home/logs        >> $esPath/config/elasticsearch.yml

export ES_JAVA_OPTS="-Xms2g -Xmx2g -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=9201 "

source ~/.bash_profile
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-14.0.1.jdk/Contents/Home

$esPath/bin/elasticsearch
