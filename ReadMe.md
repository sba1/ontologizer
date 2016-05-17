[![Build Status](https://travis-ci.org/sba1/ontologizer.svg?branch=master)](https://travis-ci.org/sba1/ontologizer)

Ontologizer
===========

Ontologizer is a tool for identifying enriched Gene Ontology terms in lists of names
of genes or gene products. Look at http://ontologizer.de for details and binary
distribution.

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

For instance, still being in the ```ontologizer.parent``` folder, you can start the GUI
version by

```
$ cd ../ontologizer.gui/target
$ java -jar Ontologizer-jar-with-dependencies.jar
```

The ```Ontologizer-jar-with-dependencies.jar``` includes all dependencies, incl. SWT
for your platform.

API
---

The core of Ontologizer can be in principle used by other applications, although the API
is rather dumb. Snapshots are regulary deployed in a dedicated GitHub repo and can be accessed
via Maven in your ```pom.xml``` in the following way:

```
	<repositories>
		...
		<repository>
			<id>ontologizer-mvn</id>
			<url>https://raw.githubusercontent.com/sba1/ontologizer-mvn/master/</url>
			<snapshots>
				<enabled>true</enabled>
				<updatePolicy>always</updatePolicy>
			</snapshots>
		</repository>
		...
	</repositories>
```

Further more, add

```
	<dependencies>
		...
		<dependency>
			<groupId>de.ontologizer</groupId>
			<artifactId>ontologizer</artifactId>
			<version>0.0.2-SNAPSHOT</version>
		</dependency>
		...
	</dependencies>
```

to your ```pom.xml``` file to let your project depend on the Ontologizer core.
Notice that the ```groupId``` is slightly different than it is used in this
project (```de.ontologizer``` vs ```ontologizer```). Also note that the URL for
the repository is subject to change.
