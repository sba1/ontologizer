#! /bin/bash -x

(cd $(dirname $0)/ontologizer.parent && mvn -DskipSource -DskipTests package -Dmaven.javadoc.skip=true -pl ontologizer:ontologizer.cmdline -am)
