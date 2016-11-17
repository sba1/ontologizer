.PHONY: all
all:
	cd ../ontologizer.parent && mvn -Dmaven.javadoc.skip=true -pl ontologizer:ontologizer.cmdline -am package
	if [ -e ontologizer-deb/.git ]; then \
		cd ontologizer-deb && \
		git fetch origin && \
		git checkout gh-pages && \
		git rebase origin/gh-pages; \
	else \
		git clone git@github.com:sba1/ontologizer-deb.git; \
	fi
	-aptly snapshot drop ontologizer
	-aptly publish drop unstable ontologizer 
	-aptly repo drop ontologizer
	aptly repo create -distribution=unstable -architectures=all ontologizer
	-aptly repo add ontologizer $(shell find ontologizer-deb -name *.deb)
	aptly repo add ontologizer ../ontologizer.cmdline/target/*.deb
	aptly snapshot create ontologizer from repo ontologizer
	aptly publish repo -skip-signing=true -architectures=all ontologizer ontologizer
	# Debian repo is now in $(HOME)/.aptly/public/ontologizer/
	cp -R $(HOME)/.aptly/public/ontologizer/* ontologizer-deb
