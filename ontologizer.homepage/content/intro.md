+++
title = "Introduction"
weight = 1
math = 1
+++

A number of methods have been developed to analyze microarray and other high-throughput data derived from measurements of hundreds or thousands of genes or proteins. Depending on the method, the results of analysis consist of a list of many differentially regulated genes or a partitioning of many or all of the genes in the data set into clusters (groups) that putatively share some functionally relevant characteristic. Among the many uses to which the Gene Ontology (GO) has been put is to provide a summary of such lists of genes/proteins according to functional role, biochemical reaction, or location of the gene product in the cell. This kind of analysis is also known as GO term enrichment analysis.

Ontologizer a is versatile application for the GO term enrichment analysis that comes in different flavours depending on the user's need. It is available with a user interface based on [Eclipse's](http://www.eclipse.org/) Standard Widget Toolkit, a command line interface, and a Web interface sorely based on Javascript. Thus, it is directly executed on the users computer similar to the traditional desktop application. 

Ontologizer supports not only the standard approach to GO term enrichment analysis but also our parent-child method as described by Grossmann, topology based methods as described by Alexa and our more recent model-based approach.

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
