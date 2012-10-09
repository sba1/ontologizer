######################################################################################
# Copyright (c) 2009, Ontologizer Open Source Team
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without modification,
# are permitted provided that the following conditions are met:
#
#    * Redistributions of source code must retain the above copyright notice, 
#      this list of conditions and the following disclaimer.
#    * Redistributions in binary form must reproduce the above copyright notice,
#      this list of conditions and the following disclaimer in the documentation
#      and/or other materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
# ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
# WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
# IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
# INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
# NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
# PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
# WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
# ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
# POSSIBILITY OF SUCH DAMAGE.
######################################################################################


########################################
# This is the R script used to analyze #
# the results of Randomize.java        #
########################################

filename<-"randomized.txt"

library(gplots)

# read the table and split by term
d<-read.table(filename,h=T)
dt<-split(d,d$term)

marg<-sapply(dt,function(x){return(mean(x$marg))})
mard.sd<-sapply(dt,function(x){return(sd(x$marg))})
mard.qt<-sapply(dt,function(x){a<-x$marg;return(qnorm(0.975)*sd(a)/sqrt(length(a)))})
marg.u<-sapply(dt,function(x){a<-x$marg;l<-length(a);return() })
marg.quantile.u<-sapply(dt,function(x){a<-x$marg;return(quantile(a,probs=0.975)) })
marg.quantile.l<-sapply(dt,function(x){a<-x$marg;return(quantile(a,probs=0.025)) })
marg.called<-sapply(dt,function(x){a<-x$marg;return(sum(a>=0.5)) })
marg.median<-sapply(dt,function(x){a<-x$marg;return(median(a)) })
margs<-sapply(dt,function(x){a<-x$marg;return(a) })

term.name<-sapply(dt,function(x){return(as.character(x$term.name[1]))})

ord<-order(marg.median,decreasing=T)
marg<-marg[ord]
mard.sd<-mard.sd[ord]
mard.qt<-mard.qt[ord]
marg.called<-marg.called[ord]
term.name<-term.name[ord]
marg.quantile.u<-marg.quantile.u[ord]
marg.quantile.l<-marg.quantile.l[ord]
marg.median<-marg.median[ord]
num.runs<-length(unique(d$run))
margs<-margs[ord]

relevant.terms.idx<-which(marg>0.4)
colors<-hcl((1:length(relevant.terms.idx))/length(relevant.terms.idx) * 360,l=55)
title<-paste("Mean marginals of",num.runs,"subsampled (90%) study sets from yeast data set")
pdf(file="randomized-mean-marg-90.pdf",height=5,width=8)
par(mar=c(4,14,4,2))
ci.u<-marg.quantile.u[relevant.terms.idx]
ci.l<-marg.quantile.l[relevant.terms.idx]
bp<-barplot2(marg[relevant.terms.idx],xlim=c(0,1),horiz=T,col=colors,names.arg="",plot.ci=T,ci.l=ci.l,ci.u=ci.u)
text(-0.03, bp,, adj = 1, labels = term.name[relevant.terms.idx], xpd = TRUE, offset = 1, col = "black")
title(title,outer=T,line=-1.5)
dev.off()

# box plot
relevant.terms.idx<-10:1
#relevant.terms.idx<-order(marg.median,decreasing=T)[1:10]
colors<-hcl((1:length(relevant.terms.idx))/length(relevant.terms.idx) * 360,l=55)
title<-paste("Boxplot of",num.runs,"subsampled (90%) study sets from yeast data set")
pdf(file="randomized-boxplot-marg-90.pdf",height=5,width=8)
par(mar=c(4,15.7,4,2))
#bp<-barplot2(marg[relevant.terms.idx],xlim=c(0,1),horiz=T,col=colors,names.arg="",plot.ci=T,ci.l=ci.l,ci.u=ci.u)
#text(-0.03, bp, adj = 1, labels = term.name[relevant.terms.idx], xpd = TRUE, offset = 1, col = "black")
bp<-boxplot(margs[relevant.terms.idx],horizontal=T,names=rep("",length(relevant.terms.idx)),col=colors)
text(-0.07, 1:length(relevant.terms.idx), adj = 1, labels = term.name[relevant.terms.idx], xpd = TRUE, offset = 1, col = "black")
title(title,outer=T,line=-1.5)
dev.off()


relevant.terms.idx<-order(marg.called,decreasing=T)[1:6]
title<-paste("Calling proportion of",num.runs,"subsampled (90%) study sets from yeast data set")
pdf(file="randomized-called-90.pdf",height=5,width=8)
par(mar=c(4,14,4,2))
bp<-barplot2(marg.called[relevant.terms.idx]/num.runs,xlim=c(0,1),horiz=T,col=colors,names.arg="")
text(-0.03, bp,, adj = 1, labels = term.name[relevant.terms.idx], xpd = TRUE, offset = 1, col = "black")
title(title,outer=T,line=-1.5)
dev.off()

relevant.terms.idx<-order(marg.called,decreasing=T)[1:10]
title<-paste("Calling proportion of",num.runs,"subsampled (90%) study sets from yeast data set")
colors<-hcl((1:length(relevant.terms.idx))/length(relevant.terms.idx) * 360,l=55)
pdf(file="randomized-called-90-more.pdf",height=5,width=8)
par(mar=c(4,14,4,2))
bp<-barplot2(marg.called[relevant.terms.idx]/num.runs,xlim=c(0,1),horiz=T,col=colors,names.arg="")
text(-0.03, bp,, adj = 1, labels = term.name[relevant.terms.idx], xpd = TRUE, offset = 1, col = "black")
title(title,outer=T,line=-1.5)
dev.off()
