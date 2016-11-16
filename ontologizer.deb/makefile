.PHONY: all
all:
	cd ../ontologizer.parent && mvn -Dmaven.javadoc.skip=true -pl ontologizer:ontologizer.cmdline -am package
	-aptly snapshot drop ontologizer
	-aptly publish drop unstable ontologizer 
	-aptly repo drop ontologizer
	aptly repo create -distribution=unstable -architectures=all ontologizer
	aptly repo add ontologizer ../ontologizer.cmdline/target/*.deb
	aptly snapshot create ontologizer from repo ontologizer
	aptly publish repo -skip-signing=true -architectures=all ontologizer ontologizer
	# Debian repo is now in $(HOME)/.aptly/public/ontologizer/
