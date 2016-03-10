[![Build Status](https://travis-ci.org/sba1/ontologizer.svg?branch=master)](https://travis-ci.org/sba1/ontologizer)

Ontologizer
===========

Ontologizer is a tool for identifying enriched Gene Ontology terms in lists of names
of genes or gene products.

Building
--------

Ontologizer's build process is driven by Maven. After cloning and checking out the
source repository via
```
$ git clone https://github.com/sba1/ontologizer
```

you can start the build procedure via

```
$ cd ontologizer.parent
$ mvn package
```

If successful, the command line version can be found in ```ontologizer.cmdline``` and the
UI version in the ```ontologizer.gui``` modules in the respective ```target``` folders.
