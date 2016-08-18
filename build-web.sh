#! /bin/bash -x

(cd ontologizer.parent && mvn -P Web -DskipSource -DskipTests package -Dmaven.javadoc.skip=true -pl ontologizer:ontologizer.web -am)
