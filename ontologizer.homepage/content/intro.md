+++
title = "Introduction"
weight = 0
math = 1
+++

A number of methods have been developed to analyze microarray and other high-throughput data derived from measurements of hundreds or thousands of genes or proteins. Depending on the method, the results of analysis consist of a list of many differentially regulated genes or a partitioning of many or all of the genes in the data set into clusters (groups) that putatively share some functionally relevant characteristic. Among the many uses to which the Gene Ontology (GO) has been put is to provide a summary of such lists of genes/proteins according to functional role, biochemical reaction, or location of the gene product in the cell.
The first version of the Ontologizer has been completely redesigned to provide a versatile WebStart or desktop application for the GO term enrichment analysis whose user interface utilizes [Eclipse's](http://www.eclipse.org/) Standard Widget Toolkit. It supports not only the standard approach to GO term enrichment analysis but also our parent-child method as described by Grossmann, topology based methods as described by Alexa and our new model-based approach

Following analysis with one of the above methods and optionally a multiple-testing correction procedure, the Ontologizer rows of terms together with their p-values or marginal probabilities, annotation counts and other information. Relevance of a term is indicated by color coding according to the sub ontology to which the term belongs, whereby the intensity of the color correlates with the maginitude of relevance.

Users can click on any term in the table to display properties and results related to the term such as its parents and children, its description, and a list of all genes annotated to the term in the study set. This information is presented as a hypertext in the lower panel with links to parent and child terms. The Ontologizer also provides a tightly integrated graphical display of the results using GraphViz.

The following pages present a brief introduction to the Gene Ontology, the standard "term-for-term" approach to statistical analysis, and the the parent child approach.

## Background

This section intends to give readers a very quick overview of the GO. The [GO homepage](http://www.geneontology.org/) is a great source of documentation. Readers are also refered to the original paper on GO by Ashburner and coworkers as well as a [recent overview](http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?cmd=Retrieve&db=PubMed&dopt=Abstract&list_uids=16381878) for more information.

The Gene Ontology (GO) has provided a dynamic, controlled vocabulary for describing gene products in any organism. GO contains three extensive subontologies describing molecular function (the biochemical activity of a gene product), biological process (the objective or biological goal to which a gene product contributes) and cellular component (the place in the cell in which the biological activity of a gene product is exerted).

GO contains well over over 20,000 terms, each of which has an accession number, a name, a more detailed definition, and other information relating a term to its parent terms. Individual terms are organized as a directed acyclic graph, whereby the terms form the nodes in the ontology and the arcs the relationships. More specific terms are lower in the graph and terms are related to their parent terms by *is-a* relationships (e.g. condensed chromosome *is-a* chromosome) or *part-of* relationships (e.g. nucleolus is *part-of* nucleus). In contrast to simpler hierarchical structures, one node in a directed acyclic graph may have multiple parents. For instance, the term *DNA replication* (GO:0006260) is a child of the term *DNA replication and cell cycle* (GO:0000067) and also of the term *S phase of mitotic cell cycle* (GO:0000084). This allows for a more flexible, expressive and detailed description of biological functions.

The GO terms do not themselves describe specific genes or gene products. Rather, collaborating databases generate gene association files consisting of links between genes or gene products and GO terms. Genes and gene products are annotated at the most specific level possible, but are considered to share the attributes of all the parent nodes. Association files have been made available for m organisms, including human, mouse, yeast and Caenorhabditis elegans.

## Overrepresentation

### Introduction

We use the designation "Term-for-Term (TFT) analysis" to refer to the standard method of performing statistical analysis for overrepresentation in GO. Please see the section on Parent-Child analysis for an explanation of this name.



TFT analysis is based on the following assumptions. We are generally interesting in knowing if a subset of genes from a larger population has any special characteristics. We will define the subset as our study group. This could be a set of overexpressed genes from a microarray experiment. Note that there may be several study groups, for instance, overexpressed genes and underexpressed genes. The population group would then be defined as all genes represented on the microarray. We can then ask if the frequency of a annotation to a Gene Ontology term is different for the study group compared to the overall population.

### Modified Fisher's Exact Test

Thus, we can imagine a binary random variable for each gene in the population with two mutual exclusive values: 1) The gene is annotated to the GO term in question or 2) The gene is not annotated to the GO term.


Assume the population of all genes contains M genes annotated to some term t and N genes not annotated to the term. The population thuis contains a total of M+N genes. The study set contains k genes, r of which are annotated to the term t. The hypergeometric distribution then gives the probability of sampling exactly r genes annotated term t:

`$$h_{k,N,M}(r)=\frac{\binom{M}{r}\binom{N}{k-r}}{\binom{N+M}{k}}$$`

Since we are interested in knowing the probability that a certain number of annotations to a certain GO term occured by chance (with low probabilities corresponding as usual to a high statistical significance), we actually need to calculate the chance of seeing r or more annotations in our study set:

`$$\sum_r^k h_{k,N,M}(r)$$`

This is essentially the same as the Fisher Exact Test, except that we are only testing for overrepresentation. The Fisher Exact test, in contrast, tests for either extreme (over- or underrepresentation). It has become the convention in GO analysis not to test for underrepresentation of GO terms, because the biological significance of underrepresentation is much less clear than the significance of overrepresentation.

### Parent-Child Analysis

The Parent-Child method represents a new algorithm for identifying overrepresented Gene Ontology (GO) annotations in gene sets. While the current methods treats each term independently and hence ignores the structure of the GO hierarchy, our approach takes parent-child relationships into account. Over-representation of a term is measured with respect to the presence of its parental terms in the set. This resolves the problem that the standard approach tends to falsely detect an over-representation of more specific terms below terms known to be over-represented. Our approach comes at no additional computational complexity when compared to the standard approach.

The parent-child method is described in detail in [Grossmann et al. (2007), Improved Detection of Overrepresentation of Gene-Ontology Annotations with Parent-Child Analysis](http://www.ncbi.nlm.nih.gov/pubmed/17848398?dopt=Abstract) (Available as an Open-Access Bioinformatics Article). A conference report from the RECOMB06 [An Improved Statistic for Detecting Over-Represented Gene Ontology Annotations in Gene Sets](http://dx.doi.org/10.1007/11732990_9), has appeared in Springer's Lecture Notes in Computer Science.

### Multiple Testing

Since in general we will test up to thousands of GO terms for overrepresentation, we need to apply some correction for multiple testing. At present, the Ontologizer uses a classic Bonferroni correction, meaning it multiplies the nominal p-values calculated as described above by the number of tests performed. This is a very conservative form of correction for multiple testing. We can limit the number of tests performed by deciding not to test GO terms that do not annotate any genes in the population (since the study group is drawn from the population, if no genes are annotated than obviously overrepresentation of the term is impossible). Additionally, if a term annotates only one gene in the population than it is apparent that testing for overrepresentation in the study group has little meaning. Note that the number of genes in the study group annotated to the term does (and should) not need to be taken into account here.

It is possible to perform analysis on any number of groups (clusters) of genes simultaneously. The Ontologizer does not perform multiple testing correction based on the number of clusters analysed. Depending on the question posed by the user, it may or may not to be appropriate to do so.


Finally, note that Gene Ontology annotations are made to the most specific term possible. All ancestors of the term are considered to be implicitly annotated. Therefore, if we are calculating the total annotations of a term, we need to count annotations to all (more specific) descendents of the term also. Note that we need to avoid introducing "extra" (duplicate) counts if there are multiple paths from a descendent term to an ancestor term, or if two distinct descendents of a term are annotated for a certain gene. A discussion of these issues is available in Robinson et al. (2004) [Gene-Ontology analysis reveals association of tissue-specific 5' CpG-island genes with development and embryogenesis](http://www.ncbi.nlm.nih.gov/pubmed/15254011?dopt=Abstract)


### Downloading the Ontologizer Source Code

The [source code](http://svn.code.sf.net/p/ontologizer/svn/) contains all the code as well as an Eclipse project file. The source code can be imported as an Eclipse project and compiled with Eclipse.

## Webstart
 
The Ontologizer can be started directly from this website using Java webstart by clicking on the following icon:

[![Foo](/images/webstart.jpg)](http://compbio.charite.de/tl_files/ontologizer/webstart/ontologizer.jnlp)

Webstart applications are started directly from a web browser, which downloads the program code to the local computer. If the code has already been downloaded, the browser checks if the server has a newer version. If not, the local copy of the program is started. Further information is available at Sun's webstart site.

For users, webstart means that the webbrowser will automatically download and use the latest available version of the webstart program, so users will automatically benefit from updates and bugfixes. Once the current version of the program has been downloaded, the program will start essentially as quickly as a traditional desktop application.

### Potential Problems with the Term Browser and Help Window of the Ontologizer

With some combinations of debian linux and firefox, we have noticed a problem with the Ontologizer finding the libraries needed to allow browsing of GO terms and the show the HTML format of the help system. If you notice this problem, it may help to set the variable MOZILLA_FIVE_HOME in the file /etc/environment as follows:

    MOZILLA_FIVE_HOME=/usr/lib/xulrunner and to install the latest version of the packages "xulrunner" and "libxul-dev" with apt-get or aptitude.


The Ontologizer should start properly with all common Windows browsers (we have tested it with IE6, IE7 and Firefox).

### MacIntosh
The Ontologizer is built using GUI elements from the Standard Widget Toolkit (SWT) known from Eclipse. SWT has been built and test for Windows and Linux systems and the Ontologizer should work without problems on machines running Linux or modern versions of Windows. Unfortunately, the SWT for Mac OSX is not yet advanced enough, and therefore we decided to declare Ontologizer for Macintosh as unsupported at this time. Embarassed


## Manual Invocation

Here you can find the binary of the [Ontologizer application](). It can be used if, for instance, the webstart facility doesn't work for your platform.


In order to run the application, you need to have the proper SWT-jar as well. This can be obtained by following [Eclipse's SWT homepage](http://www.eclipse.org/swt/). Current versions of Ontologizer are build against version 3.7 of SWT, thus it is recommended to use the [SWT 3.7](http://download.eclipse.org/eclipse/downloads/drops/R-3.7.2-201202080800/#SWT) as well. Note that the platform of the SWT archive has to match the platform of your computer system. For instance, if you are running on Windows, download the Windows SWT archive, or if you are running on a 64 bit x86 Linux, download the SWT archive indicated by x86_64.

Once you have downloaded the ontologizer.jar file and located the swt.jar file within the downloaded SWT archive you can start Ontologizer by typing (on Linux)

    java -Xmx1G -cp swt.jar:ontologizer-gui-with-dependencies.jar ontologizer.gui.swt.Ontologizer
    
or by typing (on Windows)

    java -XmX1G -cp swt.jar;ontologizer-gui-with-dependencies.jar ontologizer.gui.swt.Ontologizer
    
in the command line, assuming that both swt.jar and ontologizer.jar files are present in the current directory.On MacOSX you may have to add -XstartOnFirstThread, -d32 or both before the -cp argument, depending on the SWT version you have just downloaded.


## Command Line
This how-to describes the command-line version of the Ontologizer. This version can be used for batch processing or pipelines. Most general users will prefer the Java WebStart version, though. At first, download the .jar file.

A possible more recent version of the command line utility is available [from here](https://github.com/charite/charite.github.io/tree/master/media/ontologizer)

Ontologizer is a Java-Application and needs to be started via the 'java' command and be invoked with a plenty of arguments. All possible command arguments can be viewed via the --help argument. E.g. java -jar Ontologizer.jar --help. Here is a full list of options:

    Short Option    Long Option    Explanation
    -m    --mtc    Specifies the MTC method to use. Possible values are: "Bonferroni" (default), "None", "Westfall-Young-Single-Step"
    -c    --calculation    Specifies the calculation method to use. Possible values are: "Parent-Child-Union", "Parent-Child-Intersection", "Term-For-Term" (default). For a full list, consult the output of the -h option.
    -a    --association    File containing associations from genes to GO terms. Required
    -d    --dot    For every studyset analysis write out an additional .dot file (GraphViz) containing the GOTerm graph with significant nodes. The optional argument in range between 0 and 0.5 specifies the maximum level on which a term is considered as significantly enriched. By appending a GO Term identifier (separated by a comma) the output is restriced to the subgraph originating at this GO Term.
    -f    --filter    Filter the gene names by appling rules in a given file (currently only mapping supported).
    -g    --go    Path to gene_ontology_edit.obo file (Required)
    -h    --help    Shows this help
    -i    --ignore    Ignore genes to which no association exist within the calculation.
    -n    --annotation    Create an additional file per study set which contains the annotations.
    -o    --outdir    Specfies the directory in which the results will be placed.
    -p    --population    File containing genes within the population. Required
    -r    --resamplingsteps    Specifies the number of steps used in resampling based MTCs
    -s    --studyset    File of the study set or a directory containing study set files. Required
    -v    --version    Shows version information and exits

Instructions for Running the Ontologizer

In order to do something useful, Ontologizer must be started with several arguments (as indicated with "Required" within the output above).
 
First, you are required to specify the -g (or --go) option. This defines the path to a file which contains the GO terminology and structure. Ontologizer is able to parse files in the OBO format. Such are available directly at the GO Website.
 
Second, you are required to specify the -a option which defines the mapping of gene names to GO terms. The GO Website provides association files for a variety of organisms, as well.
 
Third, you must specify a population file with the -p option. This file contains all gene names (one per line) of the population set, e.g. the names of the genes of your microarray.
 
Last, you need to specify the path to your study set(s) with the -s option.. This can eighter be a single file for a single study set or a directory, in which case all files (ending with *.txt) are considered as separate study sets. As for the population file, one line represents only a single gene name.
 
When started with these four parameters only, the output of Ontologizer's calculation is written to a basic ascii table file into the same directory where the study files are located. The table's filename is derived from the name of the study set in question but prepended with "table-" string.
 
Using the -d option, you can instruct Ontologizer to create a graphical output of the results. For every study set, a file (name is prepended with "view-") is written which can be read by the graphviz 'dot' tool to produce a viewable graphics file. In this file, terms are depicted by nodes and their hierachical relations are depicted by edges. Because the GO DAG contains a huge amount of terms, the graph is constructed only for signifant terms and their predecessors (up to the source) and those significant terms are highligthed. Which terms are considered as significant is influenced by their p-values and the significance threshold. This threshold is specified as a parameter to the -d argument. It must be a valid floating point value ranging from 0 to 0.5. For example use "-d 0.05" to define those terms as significant whose p-value falls below 0.05.
 
In addition, you can specify a GO Term ID, after the floating-point value (separated by ","). In this case only the subgraph starting at this term is written. For example use "-d 0.05,8152" to get only a graph with the term id GO:0008152 (metabolism) and its successors within the subgraph emanating from GO:0008152 such that all significantly overrepresented terms are included in the graph.
 
Some sample datasets and population sets can be downloaded from this page.
To perform parent-child analysis using Westphal-Young MTC on the Yeast data set from the tutorial page and display the results using dot, enter the following command:

    java -jar Ontologizer.jar -a gene_association.sgd -g gene_ontology.obo -s study/4hourSMinduced.txt -p population.txt -c Parent-Child-Union -m Westfall-Young-Single-Step -d 0.05 -r 1000
The corresponding files must be in the current directory (or their full path must be indicated). To create a PNG image with the result, enter

    dot -Tpng view-4hourSMinduced-Parent-Child-Westfall-Young-Single-Step.dot -oExample.png
    
The corresponding graphic should look something like this:
![Dot Example](/images/Example.png)

## Obtaining Graphviz

Graphviz is a Open Source project which provides tools for layouting and depicting graphs. Hereby, graph references to mathematical entities which consists of nodes and edges.

A installed Graphviz system is a requirement for Ontologizer's facility to visualize the results of an enrichment analysis. More precisely, the tool named dot is invoked which performs the layouting step of the graph.

Windows users can obtain the installation package of Graphviz following the Download link of www.graphviz.org. On this site, you also can find the latest source packages which can be used, for instance by Linux, users to compile the package. Linux Distributions such as Debian provide binary packages for Graphviz as well. This allows a straithforward installation of Graphviz.

Note that in general, the standard installtion (of the Windows and Linux installation) is perfectly suited for Ontologizer. If however, for any reason, the dot exectutable is not in the command path, you have to tell Ontologizer explicitly where this command can be found by entering the full path within the Preferences window, which can be obtained via the Window > Preferences.... menu entry.

## Accessing Source
 
The source of Ontologizer can be found in an SVN repository which is hosted on SourceForge. The project name is ontologizer.
