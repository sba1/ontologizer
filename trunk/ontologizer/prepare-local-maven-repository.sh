#!/bin/bash -x
#
# This script prepares the "local" maven repository for ontologizer.
# We use an local maven repository in order to not to rely on
# the maven plugin for eclipse.
#

rm -Rf work
mkdir -p work

pushd work
wget -N ftp://sunsite.informatik.rwth-aachen.de/pub/mirror/eclipse/R-3.7.1-201109091335/swt-3.7.1-cocoa-macosx-x86_64.zip
wget -N ftp://sunsite.informatik.rwth-aachen.de/pub/mirror/eclipse/R-3.7.1-201109091335/swt-3.7.1-cocoa-macosx.zip
wget -N ftp://sunsite.informatik.rwth-aachen.de/pub/mirror/eclipse/R-3.7.1-201109091335/swt-3.7.1-gtk-linux-x86.zip
wget -N ftp://sunsite.informatik.rwth-aachen.de/pub/mirror/eclipse/R-3.7.1-201109091335/swt-3.7.1-gtk-linux-x86_64.zip
wget -N ftp://sunsite.informatik.rwth-aachen.de/pub/mirror/eclipse/R-3.7.1-201109091335/swt-3.7.1-win32-win32-x86.zip
wget -N ftp://sunsite.informatik.rwth-aachen.de/pub/mirror/eclipse/R-3.7.1-201109091335/swt-3.7.1-win32-win32-x86_64.zip

#
# Install the given SWT archive into the maven repository
#
install_swt () {
	echo "Installing $1"
	rm -Rf tmp
	mkdir tmp
	unzip $1 -d tmp
	BASENAME=`basename $1 .zip`
	VERSION=`echo $BASENAME | cut -d- -f 2`
	ARTIFACTID=`echo $BASENAME | cut -d- -f 2 --complement`
	mv tmp/swt.jar tmp/$ARTIFACTID.jar
	mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -Dfile=tmp/$ARTIFACTID.jar -DgroupId=ontologizer -DartifactId=$ARTIFACTID -Dversion=$VERSION -Dpackaging=jar -DlocalRepositoryPath=../local-maven-repo
	rm -Rf tmp
}

for file in *.zip; do
	install_swt $file
done

popd
