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

portDebug=5001

mkdir $home
mkdir $home/logs

source ~/.bash_profile
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-14.0.1.jdk/Contents/Home
echo $JAVE_HOME


$esPath/bin/elasticsearch
