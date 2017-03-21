+++
title = "Webstart"
weight = 5
+++

The Ontologizer can be started directly from this website using Java webstart by
clicking on the following icon:

<center>
[![Webstart](/images/webstart.jpg)](/webstart/ontologizer.jnlp)
</center>

Webstart applications are started directly from a web browser, which downloads the
program code to the local computer. If the code has already been downloaded, the
browser checks if the server has a newer version. If not, the local copy of the
program is started. Further information is available at the [webstart site of
Oracle](https://docs.oracle.com/javase/tutorial/deployment/webstart/running.html).

For users, webstart means that the webbrowser will automatically download and
use the latest available version of the webstart program, so users will automatically
benefit from updates and bugfixes. Once the current version of the program has been
downloaded, the program will start essentially as quickly as a traditional desktop
application.

Result Exploration
------------------

Following the analysis that lists a list of gene names or any other biological
entities that may optionally include a multiple-testing correction procedure, the
Ontologizer rows of terms together with their p-values or marginal probabilities,
annotation counts and other information. Relevance of a term is indicated by color
coding according to the sub ontology to which the term belongs, whereby the
intensity of the color correlates with the maginitude of relevance.

Users can click on any term in the table to display properties and results related
to the term such as its parents and children, its description, and a list of all
genes annotated to the term in the study set. This information is presented as a
hypertext in the lower panel with links to parent and child terms. The Ontologizer
also provides a tightly integrated graphical display of the results using GraphViz.



Troubles
--------

### Certificate

The Java runtime may refuse to start Ontologizer using Webstart facility as the binaries
are only self-signed. To circumvent this problem, the most easy solution is to add the
site `http://ontologizer.de` to the *Exception Site List* under the *Security*
tab within the *Java Control Panel* application. See
https://www.java.com/de/download/help/jcp_security.xml for details.

### Problems with the Term Browser and Help Window of the Ontologizer

With some combinations of debian linux and firefox, we have noticed a problem with the Ontologizer finding the libraries needed to allow browsing of GO terms and the show the HTML format of the help system. If you notice this problem, it may help to set the variable MOZILLA_FIVE_HOME in the file /etc/environment as follows:

    MOZILLA_FIVE_HOME=/usr/lib/xulrunner and to install the latest version of the packages "xulrunner" and "libxul-dev" with apt-get or aptitude.


The Ontologizer should start properly with all common Windows browsers (we have tested it with IE6, IE7 and Firefox).

### MacIntosh

The Ontologizer is built using GUI elements from the Standard Widget Toolkit (SWT) known from Eclipse. SWT has been built and test for Windows and Linux systems and the Ontologizer should work without problems on machines running Linux or modern versions of Windows. Unfortunately, the SWT for Mac OSX is not yet advanced enough, and therefore we decided to declare Ontologizer for Macintosh as unsupported at this time. Embarassed
