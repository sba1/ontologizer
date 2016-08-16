# Prior to the execution of this makefile
# define
#
# KEYSTORE_FILE the full path to the keystore file
# KEYSTORE_PASSWORD_ENV the name of the env variable that contains the keystore's password
# KEYSTORE_ALIAS the alias

VERSION = 0.0.3-SNAPSHOT

-include makefile.settings

ifndef KEYSTORE_FILE
$(error Please define KEYSTORE_FILE)
endif

ifndef KEYSTORE_PASSWORD_ENV
$(error Please define KEYSTORE_PASSWORD_ENV)
endif

ifndef KEYSTORE_ALIAS
$(error Please define KEYSTORE_ALIAS)
endif

.PHONY: all
all: copy-static
	hugo

#
# Check if the current checkout is clean and fail if this
# is not the case
#

.PHONY: check-clean-checkout
check-clean-checkout:
	if [ $$(git status --porcelain | wc -c) -ne 0 ]; then \
		echo "Working directory is dirty or contains untracked files. Aborting."; \
		exit 1; \
	fi

#
# Copy static files into the static folder
#
.PHONY: copy-static
copy-static: # check-clean-checkout
	cd ../ontologizer.parent && mvn clean && mvn -P Web clean
	cd ../ontologizer.parent && mvn package
	cd ../ontologizer.parent && mvn -P Web -DskipSource -DskipTests package -Dmaven.javadoc.skip=true -pl ontologizer:ontologizer.web -Dteavm.debug=false -am
	rm -Rf static/webstart static/cmdline static/gui static/web
	mkdir -p static/webstart static/cmdline static/gui static/web
	cp -p ../ontologizer.gui/ontologizer.jnlp static/webstart
	cp -p ../ontologizer.gui/target/ontologizer.gui-$(VERSION).jar static/webstart/ontologizer-gui.jar
	cp -p ../ontologizer.gui/target/ontologizer.gui-$(VERSION)-jar-with-dependencies.jar static/gui/OntologizerGui.jar
	cp -p ../ontologizer/target/ontologizer-$(VERSION).jar static/webstart/ontologizer.jar
	cp -Rp ../ontologizer.web/target/ontologizer.web-$(VERSION)/* static/web/
	rm static/web/go-basic.obo
	rsync -avm --exclude='*-sources.jar' --include='*.jar' -f 'hide,! */' ../ontologizer/local-maven-repo static/webstart/
	cp -p ../ontologizer.cmdline/target/ontologizer.cmdline-$(VERSION)-jar-with-dependencies.jar static/cmdline/Ontologizer.jar
	find static/webstart | grep .jar$ | xargs -n 1 -I'{}' jarsigner -keystore $(KEYSTORE_FILE) -storepass:env $(KEYSTORE_PASSWORD_ENV) '{}' $(KEYSTORE_ALIAS)

#
# Deploy the homepage
#
.PHONY: deploy
deploy: copy-static
	if [ -e ontologizer-pages/.git ]; then \
		cd ontologizer-pages && \
		git fetch origin && \
		git checkout gh-pages && \
		git rebase origin/gh-pages; \
	else \
		git clone git@github.com:sba1/ontologizer-pages.git; \
	fi
	hugo
	cp -Rp public/* ontologizer-pages

#
# Deploy the jars in a local ontologizer.mvn repo
#
.PHONY: deploy-jars
deploy-jars: check-clean-checkout
	cd ../ontologizer && mvn clean && mvn deploy
	if [ -e ontologizer-mvn/.git ]; then \
		cd ontologizer-mvn && \
		git fetch origin && \
		git checkout master && \
		git rebase origin/master; \
	else \
		git clone git@github.com:sba1/ontologizer-mvn.git; \
	fi
	cp -Rv ../ontologizer/target/staging-mvn-repo/* ontologizer-mvn
	cd ontologizer-mvn && ../update-maven-metadata/update-maven-metadata.py --do-it
