#!/bin/bash -x
#
# This script prepares the "local" maven repository for ontologizer.
# We use an local maven repository in order to not to rely on
# the maven plugin for eclipse. This also keeps the compiles consitent.
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
	mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -Dfile=tmp/swt.jar -Dsources=tmp/src.zip -DgroupId=ontologizer -DartifactId=$ARTIFACTID -Dversion=$VERSION -Dpackaging=jar -DlocalRepositoryPath=../local-maven-repo
}

# Install all swt archives
for file in *.zip; do
	install_swt $file
done

# Install apache commons cli
wget -N http://archive.apache.org/dist/commons/cli/binaries/commons-cli-1.1.zip
rm -Rf commons-cli-1.1
unzip commons-cli-1.1.zip
mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -Dfile=commons-cli-1.1/commons-cli-1.1.jar -DgroupId=ontologizer -DartifactId=commons-cli -Dversion=1.1 -Dpackaging=jar -DlocalRepositoryPath=../local-maven-repo

# Install Nat Table
wget -N http://sourceforge.net/projects/nattable/files/NatTable/1.6.5/nattable-core-1.6.5.jar
mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -Dfile=nattable-core-1.6.5.jar -DgroupId=ontologizer -DartifactId=nattable-core -Dversion=1.6.5 -Dpackaging=jar -DlocalRepositoryPath=../local-maven-repo

# Install Colt
wget -N http://acs.lbl.gov/software/colt/colt-download/releases/colt-1.2.0.zip
unzip colt-1.2.0.zip
mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -Dfile=./colt/lib/colt.jar -DgroupId=ontologizer -DartifactId=colt -Dversion=1.2.0 -Dpackaging=jar -DlocalRepositoryPath=../local-maven-repo

# Install SWTChart
wget -N http://sourceforge.net/projects/swt-chart/files/SWTChart/0.7.0/org.swtchart_0.7.0.zip/download
unzip org.swtchart_0.7.0.zip
mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -Dfile=./plugins/org.swtchart_0.7.0.v20110128.jar -DgroupId=ontologizer -DartifactId=swtchart -Dversion=0.7.0 -Dpackaging=jar -DlocalRepositoryPath=../local-maven-repo

popd
