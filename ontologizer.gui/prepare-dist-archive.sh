#!/bin/bash
#
# This simple script is designed to pack the results
# into a single zip file. The jars in the zip file
# will be signed.
#

KEYSTORE=$1
PASS=$2

rm -Rf work
mkdir -p work/ontologizer
cp -R ../ontologizer/local-maven-repo work/ontologizer
find work/ontologizer | grep .svn$ | xargs rm -Rf 
cp ../ontologizer/target/ontologizer-0.0.1-SNAPSHOT.jar  work/ontologizer/ontologizer.jar
cp target/ontologizer.gui-0.0.1-SNAPSHOT.jar  work/ontologizer/ontologizer-gui.jar
cp target/ontologizer.gui-0.0.1-SNAPSHOT-jar-with-dependencies.jar work/ontologizer/ontologizer-gui-with-dependencies.jar
cp ontologizer.jnlp work/ontologizer
find work/ontologizer | grep .jar$ | xargs -n 1 -I'{}' jarsigner -keystore $KEYSTORE -storepass $PASS '{}' myself

rm -Rf ontologizer.zip
pushd work 
zip -r ../ontologizer.zip ontologizer
popd
