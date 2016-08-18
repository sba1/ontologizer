#! /bin/bash -x
#
# This is a shortcut script for quickly building the web version of Ontologizer
#

(cd ontologizer.parent && mvn -DskipSource -DskipTests -Dmaven.javadoc.skip=true package -pl ontologizer:ontologizer.web -am)
