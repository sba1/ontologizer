#
# A simple makefile to conduct the benchmark
#

VERSION=0.0.3-SNAPSHOT

.PHONY: all
all: clperf
	cd ../ontologizer.parent && mvn package -DskipTests -pl ontologizer:ontologizer.benchmark -am
	ONTOLOGIZER_ENABLE_EXPERIMENTAL=yes java -jar target/ontologizer.benchmark-$(VERSION)-jar-with-dependencies.jar \
		--obo ../ontologizer.tests/data/gene_ontology.1_2.obo.gz \
		--association ../ontologizer.tests/data/gene_association.sgd.gz \
		--max-terms-per-combination 3 \
		--term-combinations-per-run 20 \
		--output-dir /tmp/ontobench

.PHONY: help
help:
	ONTOLOGIZER_ENABLE_EXPERIMENTAL=yes java -jar target/ontologizer.benchmark-$(VERSION)-jar-with-dependencies.jar --help

clperf:
	# Checkout most recent version of clperf
	(cd clperf.git && git pull --ff-only origin master) || git clone https://github.com/sba1/clperf.git clperf.git

	# Compile it
	make -C clperf.git

	# Copy it
	cp clperf.git/clperf .
