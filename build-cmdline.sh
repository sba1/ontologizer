#! /bin/bash -x

(cd ontologizer.parent && mvn -DskipSource -DskipTests package -Dmaven.javadoc.skip=true -pl ontologizer:ontologizer.cmdline -am)
