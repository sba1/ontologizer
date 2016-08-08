#! /bin/bash -x

(cd ontologizer.parent && mvn -P Web -DskipTests package -Dmaven.javadoc.skip=true -pl ontologizer:ontologizer.web -am)
