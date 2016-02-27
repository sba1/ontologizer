+++
title = "How To"
weight = 5
+++

The following sections show some solutions to some common problems revolving around the lack of uniformity of gene name nomenclature, and show examples of Ontologizer output for C. elegans, mouse, yeast and Drosophila. 

### Gene Ontology Files

The Ontologizer requires the Gene Ontology Terms file (gene_ontology.obo), which is the file that describes individual GO terms and their relationships to one another. This file can be downloaded from the [Gene Ontology homepage](http://geneontology.org/). Alternatively, the Ontologizer can automatically download the latest version if you specify the URL to this file.

### Gene Association Files

The OBO file describes only the functional hierarchy of terms and does not provide functional annotations for actual genes. For this purpose, users need to download the appropriate annotation file. These files are generally entitled gene_association.XXX, where XXX stands for the database or species (e.g., mgi for [Mouse Genome Informatics](http://www.informatics.jax.org/), and sgd for [Saccharomyces Genome Database](http://www.yeastgenome.org/). Association files can also be downloaded from the Gene Ontology homepage. The Ontologizer can also automatically download the latest version of the association file for a number of commonly analyzed organisms (See the tutorial within the Ontologizer application for more details).

### Data Files

The Ontologizer produces listings of GO annotations for user supplied lists of genes or gene products. One situation in which this can be useful is for "clustering" analysis of microarray data, but there are many other potential uses. The Ontologizer assumes that each group of genes resides in its own file, and presently accepts FASTA files as well as files in which each gene is on its own line. For FASTA and plain files, the first word on a line (for FASTA files, the first word following the ">" sign) is taken to be the name of the gene or gene product, and anything else on the line is taken to be a description. It is easy to extend the source code to allow for other types of file formats.

The user should copy files representing the results of clustering into a separate directory. Note that the names of the genes or gene products need to correspond to the nomenclature used in the association file for the Ontologizer to function properly. This can be especially problematic if (for instance) EST names are present in the results of clustering. Several solutions to such problems are presented below on this web page in the form of Perl scripts that can transform files and gene names such that the Ontologizer will recognize the names.

### Preparing Data for the Ontologizer

The Ontologizer uses the gene or gene-product nomeclature as found in the various gene_association files. The Ontologizer recognizes names entered as DB_Object, DB_Object_Name, or Synonym in these files. However, in many cases, gene names will be listed in other form, such as for instance EST accession numbers or Affymetrix ids. Further details on the gene_association files can be found on the Gene Ontology website. In these cases, it will be necessary to transform your data by mapping non-standard gene ids to a form that will be recognized by the Ontologizer.

Although there are many ways of doing this, we recommend using scripts to transform the files into new files where the first word is the standard gene or gene-product name, and everything after the first tab-stop is an optional description consisting of at least the original name or accession number (so that the user can refer back to the original data if desired). The Ontologizer will automatically output the description underneath the standard name in the "detail" page for each cluster.

The following sections present sample output from the Ontologizer for several different species. In many cases, we were able to use the output files directly without further processing (For C. elegans for instance). In other cases, it was necessary to create a mapping for the names of the original files such that they would conform to those used in the gene_association files. We present some "cookbook" Perl scripts that demonstrate how to perform such transformations.

### C. elegans

The [recommended nomeclature for C. elegans](http://www.wormbase.org/wiki/index.php/UserGuide:Nomenclature_nematode) forsees that genes are given names consisting of three italicized letters, a hyphen, and an italicized Arabic number, e.g., dpy-5. However, it is still not uncommon to see cosmid names: Sequences that are predicted to be genes from sequence data alone were initially named on the basis of the sequenced cosmid, plus a number. For example, the genes predicted for the cosmid K07F5 are called K07F5.1, K07F5.2, and so on. As more becomes known, it is expected that these names will be replaced by standard three-letter names.

In this example, we used the Ontologizer to analyze a set of genes identified by [Liu et al. (2004)](http://www.ncbi.nlm.nih.gov/pubmed/15380030?dopt=Abstract) as being regulated by TGFbeta during entry into dauer diapause in C. elegans.

When resources are scant, C. elegans larvae arrest as long-lived dauers under the control of insulin/IGF- and TGFbeta-related signaling pathways. The authors identified genes that show different levels of expression in a comparison of wild-type L2 or L3 larvae (non-dauer) to TGFbeta mutants at similar developmental stages undergoing dauer formation. The data shown here represent the strongly regulated (>2.1 fold) genes in this experiment and were taken from table 2. The population set was taken to be all C. elegans genes for which annotations were provided by Wormbase. Among other things, GO analysis revealed that a number of terms related to ribosomes were highly significantly overrepresented. The gene names in these clusters represent a mix of cosmid and standard names. The **gene_association.wb** file provided by Wormbase and the cluster files with gene names were used as is. [Example data](https://github.com/charite/charite.github.io/blob/master/media/ontologizer/examples/yeastSampleFiles.zip?raw=true).

### Mus musculus
Mouse Genome Informatics (MGI) (Jackson Laboratories) supplies a gene_association file for M. musculus. They also provide [guidelines for mouse gene nomeclature](http://www.informatics.jax.org/mgihome/nomen/gene.shtml). The gene_association.mgi file provided by MGI contain annotations based primarily on MGI accession numbers. Therefore, one way of using the Ontologizer to analyze mouse datasets that are based on NCBI accession numbers is to transform the accession numbers into the corresponding MGI accession numbers. We present a way of doing this with perl scripts using files from NCBI's Entrez Gene (gene_info and gene2accession).

The perl script NCBI2MG.pl maps NCBI accession numbers via Entrez Gene ids to MGI accession numbers. The output file, NCBI2MGI.txt, can be used as a mapping file for the Ontologizer. The original accession numbers are visible (together with the full gene name, locusID, and UniGene cluster) directly after the MGI accession number. For accession numbers for which no MGI id could be identified (mainly some ESTs), just the original accession number is displayed. The corresponding original files can be [downloaded here](https://github.com/charite/charite.github.io/blob/master/media/ontologizer/examples/mouseSampleFiles.tar.gz?raw=true).


### Results

The results window contains a table with one line for each gene in the population. Clicking on a line causes more information about a term to be displayed in the lower panel. A list of annotated genes/proteins is shown, and links to parent and child terms are available. By clicking on the symbol for "graph", a graph is generated and shown in the right hand panel. Buttons for zooming in and out are provided. The table of results can be saved and the graph can be saved as a PNG file or a dot file (this is the GraphViz definition file). Note that the graphics function requires that [GraphViz](http://www.graphviz.org/) is installed on your computer.

## Datasets

### Datasets for GO Analysis
A typical dataset for statistical analysis using the Ontologizer or many other GO tools will consist of a list of gene/protein names making up a study set (for instance, all differentially expressed genes in an experiment) and a longer list of gene/protein names making up the population (for instance, all genes represented on a microarray that was used to perform the experiment). It has been our experience that is is difficult to find such datasets on the web, which is a stumbling block for testing new algorithms or tools for GO analysis. 

On this page we present an R/Bioconductor script that makes it easy to create study sets/population sets using publically available microarray datasets from NCBI's Gene Expression Omnibus (GEO) database. It should be easy to adapt this script to analyze in house datasets derived from Affymetrix microarray hybridizations. Currently there are thousands of datasets in the GEO database, so extensive testing and comparisons of different algorithms should be possible. On this page, we explain the R script and present several datasets obtained using it.

### Software Prerequisites and Data Sources

*R and Bioconductor*

R is an extremely powerful open-source language for statistical computing and graphics that runs on all standard computer plattforms. Bioconductor is a set of packages for the R environment that provide a larege number of useful tools for the analysis of microarray and other forms of genomic data.


*Bioconductor packages*

Our script makes use of two Bioconductor packages to perform the analysis. The package GEOquery automatically retrieves microarray from the GEO website, and the package limma performs a number of statistical analyses including the identification of differentially expressed genes. Depending on your setup, you may need to install these packages. There are several ways of doing this, but the easiest is probably


{{< highlight R >}}
source("http://www.bioconductor.org/biocLite.R")
biocLite("GEOquery")
{{< / highlight >}}

*GEO*

NCBI's Gene Expression Omnibus (GEO) database is a repository of thousands of microarray datasets. GEO DataSets (GDS) are curated sets of GEO Sample data. A GDS record represents a collection of biologically and statistically comparable GEO Samples and forms the basis of GEO's suite of data display and analysis tools.

### R/Bioconductor Code for Creating Study and Population Datasets
 
The following code demonstrates how to use R/Bioconductor to download and analyze datasets from the NCBI GEO database and to create study and population sets from them.

{{< highlight R "linenos=inline,hl_lines=2 3" >}}
# A script for fetching data and creating study/population datasets
# for the Ontologizer

library(Biobase)
library(GEOquery)
library(limma)


dataset.name <- "GDS2821"
gds <- getGEO(dataset.name,destdir=".")
#gds <- getGEO(filename = system.file("GDS2860.soft.gz",package = "GEOquery"))
eset <- GDS2eSet(gds,do.log2=TRUE)
## extract affymetrix IDs
ids<-rownames(exprs(eset))


## Extract phenotypic information
## Use gsub to simplify the names (makes it easier to define factors)
state <- Columns(gds)$disease.state
state <- gsub("Parkinson's disease","parkinson",state)

## Define the factors for the statistical analysis
f <- factor(state)
design <- model.matrix(~0+f)
contrast.matrix<-makeContrasts(fparkinson-fcontrol,levels=design)

## Get the platform name and check that we got data
platform<-as.character(Meta(gds)$platform)
print(paste("Platform",platform,"contains ",length(ids),"spots"))

## Retrieve the platform information.
gpl.name <- paste(platform,".soft",sep="")
if (file.exists(gpl.name)) {
  gpl<-getGEO(filename=gpl.name,destdir=".")
} else {
  gpl<-getGEO(platform,destdir=".")
}

## Correspondence between the affymetrix IDs and the gene symbol
mapping <- Table(gpl)[,c("ID","Gene.Symbol")]

## t-test
fit<-lmFit(eset,design)
fit2<-contrasts.fit(fit,contrast.matrix)
fit2<-eBayes(fit2)

## Adjust for multiple testing
p.values<-fit2$p.value
p.BH <- p.adjust(p.values,method="BH")

## get the indices of all significant p values
ord<-order(p.val)
ord.sign<-subset(ord,p.val[ord]<0.1)

## check results
mapping[ord.sign,]

## Write study set
studyset.name = paste("study",dataset.name,".txt",sep="")
write.table(mapping[ord.sign,2],file=studyset.name,col.names=F,row.names=F,quote=F)

pop.name = paste("population",platform,"txt",sep="")
write.table(mapping[,2],col.names=F,row.names=F,quote=F,file=pop.name)
{{< / highlight >}}
