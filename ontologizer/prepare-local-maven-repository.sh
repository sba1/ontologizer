#!/bin/bash -x
#
# This script prepares the "local" maven repository for ontologizer.
# We use an local maven repository in order to not to rely on
# the maven plugin for eclipse. This also keeps the compiles consitent.
#

rm -Rf work
mkdir -p work

pushd work
MIRROR_BASE=ftp://ftp.halifax.rwth-aachen.de/eclipse/eclipse/downloads/drops4/R-4.5.1-201509040015

wget -N $MIRROR_BASE/swt-4.5.1-cocoa-macosx-x86_64.zip
wget -N $MIRROR_BASE/swt-4.5.1-gtk-linux-x86.zip
wget -N $MIRROR_BASE/swt-4.5.1-gtk-linux-x86_64.zip
wget -N $MIRROR_BASE/swt-4.5.1-win32-win32-x86.zip
wget -N $MIRROR_BASE/swt-4.5.1-win32-win32-x86_64.zip

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

# Install apache commons cli 1.1
wget -N http://archive.apache.org/dist/commons/cli/binaries/commons-cli-1.1.zip
rm -Rf commons-cli-1.1
unzip commons-cli-1.1.zip
mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -Dfile=commons-cli-1.1/commons-cli-1.1.jar -DgroupId=ontologizer -DartifactId=commons-cli -Dversion=1.1 -Dpackaging=jar -DlocalRepositoryPath=../local-maven-repo

# Install apache common cli 1.2
wget -N http://apache.mirror.clusters.cc/commons/cli/binaries/commons-cli-1.2-bin.zip
rm -Rf commons-cli-1.2
unzip commons-cli-1.2-bin.zip
mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -Dfile=commons-cli-1.2/commons-cli-1.2.jar -Dsource=commons-cli-1.2/commons-cli-1.2-sources.jar -DgroupId=ontologizer -DartifactId=commons-cli -Dversion=1.2 -Dpackaging=jar -DlocalRepositoryPath=../local-maven-repo

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

# Install LWJGL
wget -N http://sourceforge.net/projects/java-game-lib/files/Official%20Releases/LWJGL%202.8.4/lwjgl-2.8.4.zip/download -o lwjgl.zip
rm -Rf lwjgl
mv lwjgl-2.8.4 lwjgl

#
# Prepare LWJGL archive
#
mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -Dfile=lwjgl/jar/lwjgl.jar -DgroupId=ontologizer -DartifactId=lwjgl -Dversion=2.8.4 -Dpackaging=jar -DlocalRepositoryPath=../local-maven-repo
mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -Dfile=lwjgl/jar/lwjgl_util.jar -DgroupId=ontologizer -DartifactId=lwjgl-util -Dversion=2.8.4 -Dpackaging=jar -DlocalRepositoryPath=../local-maven-repo

mkdir ../local-maven-repo/lwjgl-native
cp -R lwjgl/native/linux/* ../local-maven-repo/lwjgl-native
cp -R lwjgl/native/macosx/* ../local-maven-repo/lwjgl-native
cp -R lwjgl/native/windows/* ../local-maven-repo/lwjgl-native

# Install JCommander
wget -N http://search.maven.org/remotecontent?filepath=com/beust/jcommander/1.30/jcommander-1.30.jar -O jcommander-1.30.jar 
wget -N http://search.maven.org/remotecontent?filepath=com/beust/jcommander/1.30/jcommander-1.30-sources.jar -O jcommander-1.30-sources.jar 
mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -Dfile=jcommander-1.30.jar -Dsources=jcommander-1.30-sources.jar -DgroupId=com.beust -DartifactId=jcommander -Dversion=1.30 -Dpackaging=jar -DlocalRepositoryPath=../local-maven-repo

popd
