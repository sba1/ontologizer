# Prior to the execution of this makefile
# define
#
# KEYSTORE_FILE the full path to the keystore file
# KEYSTORE_PASSWORD_ENV the name of the env variable that contains the keystore's password
# KEYSTORE_ALIAS the alias

VERSION = 0.0.2-SNAPSHOT

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

.PHONY: copy-static
copy-static:
	cd ../ontologizer.parent && mvn package
	rm -Rf static/webstart static/cmdline
	mkdir -p static/webstart static/cmdline
	cp -p ../ontologizer.gui/ontologizer.jnlp static/webstart
	cp -p ../ontologizer.gui/target/ontologizer.gui-$(VERSION).jar static/webstart/ontologizer-gui.jar
	cp -p ../ontologizer/target/ontologizer-$(VERSION).jar static/webstart/ontologizer.jar
	cp -pR ../ontologizer/local-maven-repo static/webstart/
	cp -p ../ontologizer.cmdline/ontologizer.cmdline-$(VERSION).jar static/cmdline/Ontologizer.jar
	find static/webstart | grep .jar$ | xargs -n 1 -I'{}' jarsigner -keystore $(KEYSTORE_FILE) -storepass:env $(KEYSTORE_PASSWORD_ENV) '{}' $(KEYSTORE_ALIAS)
