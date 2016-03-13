# Prior to the execution of this makefile
# define
#
# KEYSTORE_FILE
# KEYSTORE_PASSWORD_ENV
# KEYSTORE_ALIAS

VERSION = 0.0.2-SNAPSHOT

.PHONY: all
all: copy-static
	hugo

.PHONY: copy-static
copy-static:
	cd ../ontologizer.parent && mvn package
	mkdir -p static/webstart
	cp -p ../ontologizer.gui/ontologizer.jnlp static/webstart
	cp -p ../ontologizer.gui/target/ontologizer.gui-$(VERSION).jar static/webstart/ontologizer-gui.jar
	cp -p ../ontologizer/target/ontologizer-$(VERSION).jar static/webstart/ontologizer.jar
	cp -pR ../ontologizer/local-maven-repo static/webstart/
	find static/webstart | grep .jar$ | xargs -n 1 -I'{}' jarsigner -keystore $(KEYSTORE_FILE) -storepass:env $(KEYSTORE_PASSWORD_ENV) '{}' $(KEYSTORE_ALIAS)
