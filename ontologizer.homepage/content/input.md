+++
title = "Input"
weight = 18
+++

For conducting an Ontologizer gene enrichment analysis, three types of files are
necessary inputs.

### Gene Ontology Files

The Ontologizer requires the Gene Ontology terms file (e.g., ```gene_ontology.obo```), which is the file that describes individual GO terms and their relationships to one another. This file can be downloaded from the [Gene Ontology homepage](http://geneontology.org/). Alternatively, the Ontologizer can automatically download the latest version if you specify the URL to this file.

### Gene Association Files

#### GAF
The OBO file describes only the functional hierarchy of terms and does not provide functional annotations for actual genes. For this purpose, users need to download the appropriate annotation file. These files are generally entitled gene_association.XXX, where XXX stands for the database or species (e.g., mgi for [Mouse Genome Informatics](http://www.informatics.jax.org/), and sgd for [Saccharomyces Genome Database](http://www.yeastgenome.org/). Association files can also be downloaded from the Gene Ontology homepage. The Ontologizer can also automatically download the latest version of the association file for a number of commonly analyzed organisms (See the tutorial within the Ontologizer application for more details).

#### Simple Format

For mappings not available via official annotation files, a simple two column
format is supported as well. The name of such mapping files must have a ```.ids```
suffix. The actual contents of must conform to the following structure:
```
GoStat IDs Format Version 1.0
<gene><tab><termid>,<termid>,...,<termid>
...
<gene><tab><termid>,<termid>,...,<termid>
```

### Data Files / Study Sets

The Ontologizer produces listings of GO annotations for user supplied lists of genes or gene products. One situation in which this can be useful is for "clustering" analysis of microarray data, but there are many other potential uses. The Ontologizer assumes that each group of genes resides in its own file, and presently accepts FASTA files as well as files in which each gene is on its own line. For FASTA and plain files, the first word on a line (for FASTA files, the first word following the ">" sign) is taken to be the name of the gene or gene product, and anything else on the line is taken to be a description. It is easy to extend the source code to allow for other types of file formats.

The user should copy files representing the results of clustering into a separate directory. Note that the names of the genes or gene products need to correspond to the nomenclature used in the association file for the Ontologizer to function properly. This can be especially problematic if (for instance) EST names are present in the results of clustering. Several solutions to such problems are presented below on this web page in the form of Perl scripts that can transform files and gene names such that the Ontologizer will recognize the names.
