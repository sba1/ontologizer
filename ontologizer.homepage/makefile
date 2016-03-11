.PHONY: all
all: copy-static
	hugo

.PHONY: copy-static
copy-static:
	cd ../ontologizer.parent && mvn package
	mkdir -p static/webstart
	cp -p ../ontologizer.gui/ontologizer.jnlp static/webstart
