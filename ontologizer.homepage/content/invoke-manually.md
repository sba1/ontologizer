+++
title = "Invoke Manually"
weight = 10
+++

If the WebStart facility doesn't work for your platform, you can download [OntologizerGui.jar](/gui/OntologizerGui.jar). This comes with
all dependencies included with the exception of SWT.

Therefore, in order to run the application, you need to have the proper SWT-jar as well. This can be obtained by following [Eclipse's SWT homepage](http://www.eclipse.org/swt/). Current versions of Ontologizer are build against version 3.7 of SWT, thus it is recommended to use the [SWT 3.7](http://download.eclipse.org/eclipse/downloads/drops/R-3.7.2-201202080800/#SWT) as well. Note that the platform of the SWT archive has to match the platform of your computer system. For instance, if you are running on Windows, download the Windows SWT archive, or if you are running on a 64 bit x86 Linux, download the SWT archive indicated by x86_64.

Once you have downloaded the ontologizer.jar file and located the swt.jar file within the downloaded SWT archive you can start Ontologizer by typing (on Linux)

```
$ java -Xmx1G -cp swt.jar:OntologizerGui.jar ontologizer.gui.swt.Ontologizer
```

or by typing (on Windows)

```
> java -XmX1G -cp swt.jar;OntologizerGui.jar ontologizer.gui.swt.Ontologizer
```

in the command line, assuming that both swt.jar and OntologizerGui.jar files are present in the current directory. On MacOSX you may have to add -XstartOnFirstThread, -d32 or both before the -cp argument, depending on the SWT version you have just downloaded.
