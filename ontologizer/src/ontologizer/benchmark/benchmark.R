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
# the results of Benchmark.java        #
########################################

# Here, you can specify where the input file
# can be found and which which columns should
# be extracted
dir<-"."
filename<-"result-fp.txt"
v<-matrix(ncol=2,byrow=T,
          c("p.tft","TfT",
		    "p.pcu", "PCU",
		    "p.tweight","TopW",
			"p.gg", "GenGO'",
			"p.b2g.ideal.pop", "MGSA'",
			"p.b2g.mcmc.pop", "MGSA"
           ))

# ROCR was used for the plotting, but their (down)sampling scheme is not
# optimal (seems to only sample from the data, ignoring the fact that in
# some areas the data is denser than in others). It has been replaced by
# custom functions. You can comment the following line.
library(ROCR)

# Read the input file. Ignore any 0 term.
full.filename<-file.path(dir,filename)
d<-read.table(full.filename,h=T)
d<-subset(d,d$term!=0)


#
# Converts the given name to a more useful one.
#
decode.name<-function(name)
{
	pat<-"p.b2g.a(0.\\d+)\\.b(0.\\d+)\\.d(\\d+)"
	if (length(grep(pat,name,perl=T))>0)
	{
		a<-as.numeric(gsub(pat,"\\1",name,perl=T))
		b<-as.numeric(gsub(pat,"\\2",name,perl=T))
		dt<-as.numeric(gsub(pat,"\\3",name,perl=T))
		sprintf("b2g (a=%g b=%g dt=%d)",a,b,dt)
		return(sprintf("b2g (a=%g b=%g dt=%d)",a,b,dt))
	} else
	{
		return(name)
	}
}

#
# Returns a list with elements describing the given
# name encoded setting
#
decode.parameter.setting<-function(name)
{
	pat<-"p.b2g.a(0.\\d+)\\.b(0.\\d+)\\.d(\\d+)"
	if (length(grep(pat,name,perl=T))>0)
	{
		a<-as.numeric(gsub(pat,"\\1",name,perl=T))
		b<-as.numeric(gsub(pat,"\\2",name,perl=T))
		dt<-as.numeric(gsub(pat,"\\3",name,perl=T))
		return(list(m="b2g",a=a,b=b,dt=dt))
	} else
	{
		m<-gsub("^p\\.(.+)","\\1",name,perl=T);
		return(list(m=m,a=NA,b=NA,dt=NA));
	}
}

#
# Draw some performance plots using ROCR. Also k-truncated score
# is calculated in this routine (should have been moved into a
# plot.roc.new function, does not depend on ROCR...)
#
plot.roc<-function(d,main="Comparision",alpha=NA,beta=NA,calc.auc=F,y.axis="tpr",x.axis="fpr",xlim=c(0,1),ylim=c(0,1),legend.place="bottomright",rocn=NA,downsampling=300)
{
	nruns<-length(unique(d$run))
	
	if (nrow(d)==0)
	{
		return()
	}
	
	l<-list();

	colnames(v)<-c("short","full")
	colors<-rainbow(nrow(v))
	pchs<-1:nrow(v)
	
	for (i in (1:nrow(v)))
	{
		values<-d[,v[i,1]]
		labels<-d$label

		# rebuild the values if this should be a rocn
		if (!is.na(rocn))
		{
			d.by.run<-split(d,d$run)

			avg.rocn<-mean(sapply(d.by.run,function(d2)
			{
				values2<-d2[,v[i,1]]
			
				o<-order(values2,decreasing=F)
				negatives.idx<-which(d2[o,]$label==0)
				total.t<-length(which(d2[o,]$label==1))
				
				r.n<-sum(sapply(1:rocn,function(j)
				{
					return(negatives.idx[j]-j)
				}))
				
				roc.value<- 1.0 / rocn / total.t * r.n
				
				return (roc.value)
			}))

			print(sprintf("%s: %f (alpha=%f, beta=%f)",v[i,2],avg.rocn,alpha,beta))
		}
	
		pred<-prediction(1-values,labels)
		perf<-performance(pred, measure = y.axis, x.measure = x.axis) 
		name<-v[i,2]
		
		if (calc.auc)
		{
			auc.perf<-performance(pred, measure = "auc")
			auc<-auc.perf@y.values[[1]]
			l<-append(l,sprintf("%s (%.3g)",name,auc))
		} else
		{
			l<-append(l,sprintf("%s",name))
		}

		if (i==1)
		{
			title<-bquote(paste(.(main),": ", alpha,"=",.(alpha),",",beta,"=",.(beta)))
			plot(perf, col=colors[i],pch=pchs[i],downsampling=downsampling, type="l", ylim=ylim,xlim=xlim,main=title)
			plot(perf, col=colors[i],pch=pchs[i],downsampling=25, type="p", add=TRUE)
		} else
		{
			plot(perf, col=colors[i],pch=pchs[i],downsampling=downsampling, type="l", add=TRUE)
			plot(perf, col=colors[i],pch=pchs[i],downsampling=25, type="p", add=TRUE)
		}		
	}

	legend(legend.place, col=colors, pch=pchs, legend = unlist(l))
}

#
# Draw ROC plots (doesn't use ROCR)
#
plot.roc.new<-function(d,main="Comparision",alpha=NA,beta=NA,xlim=c(0,1),ylim=c(0,1),legend.place="bottomright",downsampling=300)
{
	nruns<-length(unique(d$run))
	
	if (nrow(d)==0)
	{
		return()
	}
	
	l<-list();

	colnames(v)<-c("short","full")
	colors<-rainbow(nrow(v))
	pchs<-1:nrow(v)
	
	for (i in (1:nrow(v)))
	{
		ord<-order(d[,v[i,1]])
		
		# data is orderd. Threshold is such that the values
		# above an element are flagged as positive (inclusive)
		# and values below an element as negative.
		values<-d[ord,v[i,1]]
		labels<-d[ord,]$label

		tps<-cumsum(labels)
		fps<-(1:length(labels)) - tps
		tpr<-tps / tps[length(tps)] # true postive rate
		fpr<-fps / fps[length(fps)] # false positive rate

		idx.dots<-cumsum(hist(fpr,plot=F,breaks=25)$counts)
		idx.lines<-c(1,cumsum(hist(fpr,plot=F,breaks=300)$counts))
		
		# For AUROC scores we request a higher resolution 
		idx.auroc<-c(1,cumsum(hist(fpr,plot=F,breaks=1000)$counts))

		# calculate the AUROC. Note that diff() returns the difference of
		# consecutive elements. We calculate the lower bound of the area.
		auroc<-sum(c(diff(fpr[idx.lines]),0) * tpr[idx.lines])
		name<-v[i,2]
		l<-append(l,sprintf("%s (%.3g)",name,auroc))

		if (i==1)
		{
			title<-bquote(paste(.(main),": ", alpha,"=",.(alpha),",",beta,"=",.(beta)))
			plot(fpr[idx.lines], xlab="False positive rate",tpr[idx.lines], ylab="True positive rate", col=colors[i],pch=pchs[i],type="l", ylim=ylim,xlim=xlim,main=title)
		} else
		{
			lines(fpr[idx.lines], tpr[idx.lines], col=colors[i],pch=pchs[i],type="l", ylim=ylim,xlim=xlim,main=sprintf("%s (alpha=%g,beta=%g)",main,alpha,beta))
		}
		points(fpr[idx.dots], tpr[idx.dots], col=colors[i],pch=pchs[i],type="p")
	}

	legend(legend.place, col=colors, pch=pchs, legend = unlist(l))
}


#
# Draw precision/recall plots (doesn't use ROCR)
# The outer frame looks as in plot.roc.new()
#
plot.pr<-function(d,main="Comparision",alpha=NA,beta=NA,xlim=c(0,1),ylim=c(0,1),legend.place="bottomright",downsampling=300)
{
	nruns<-length(unique(d$run))
	
	if (nrow(d)==0)
	{
		return()
	}
	
	l<-list();

	colnames(v)<-c("short","full")
	colors<-rainbow(nrow(v))
	pchs<-1:nrow(v)
	
	for (i in (1:nrow(v)))
	{
		ord<-order(d[,v[i,1]])

		# data is orderd. Threshold is such that the values
		# above an element are flagged as positive (inclusive)
		# and values below an element as negative.
		values<-d[ord,v[i,1]]
		labels<-d[ord,]$label

		tps<-cumsum(labels)
		fps<-(1:length(labels)) - tps
		tpr<-tps / tps[length(tps)]
		fpr<-fps / fps[length(fps)]

		prec<-tps/(1:length(values)) # number of true positives / (number of all positives = (true positives + false negatives))
		recall<-tps/sum(labels)      # number of true positives / (true positives + false negatives = all positive samples)
		
		name<-v[i,2]
		l<-append(l,sprintf("%s",name))

		idx.dots<-cumsum(hist(recall,plot=F,breaks=25)$counts)
		idx.lines<-cumsum(hist(recall,plot=F,breaks=300)$counts)

		if (i==1)
		{
			title<-bquote(paste(.(main),": ", alpha,"=",.(alpha),",",beta,"=",.(beta)))
			plot(recall[idx.lines], xlab="Recall", prec[idx.lines], ylab="Precision", col=colors[i],pch=pchs[i],type="l", ylim=ylim,xlim=xlim,main=title)
		} else
		{
			lines(recall[idx.lines], prec[idx.lines], col=colors[i],pch=pchs[i],type="l", ylim=ylim,xlim=xlim,main=sprintf("%s (alpha=%g,beta=%g)",main,alpha,beta))
		}
		points(recall[idx.dots],  prec[idx.dots], col=colors[i],pch=pchs[i],type="p")
	}

	legend(legend.place, col=colors, pch=pchs, legend = unlist(l))
}


s<-split(d,list(d$alpha,d$beta))

#
# ROC
#
lapply(s,function(d) {

	alpha<-unique(d$alpha)
	beta<-unique(d$beta)

	filename<-sprintf("result-roc-a%d-b%d.pdf",alpha*100,beta*100)
	pdf(file=filename,height=8,width=8)
	par(cex=1.3,cex.main=1.2,lwd=2)
	plot.roc.new(subset(d,d$senseful==0),alpha,beta,main="ROC")
	dev.off()
});

#
# Pre/Recall
#

lapply(s,function(d) {
	alpha<-unique(d$alpha)
	beta<-unique(d$beta)

	filename<-sprintf("result-precall-a%d-b%d.pdf",alpha*100,beta*100)
	pdf(file=filename,height=8,width=8)
	par(cex=1.3,cex.main=1.2,lwd=2)
	plot.pr(subset(d,d$senseful==0),alpha,beta,legend.place="topright",main="Precision/Recall")
	dev.off()
});

# F/Cutoff
#

lapply(s,function(d) {
	alpha<-unique(d$alpha)
	beta<-unique(d$beta)

	filename<-sprintf("result-fcutoff-a%d-b%d.pdf",alpha*100,beta*100)
	pdf(file=filename,height=8,width=8)
	par(cex=1.3,cex.main=1.2,lwd=2)
	plot.roc(subset(d,d$senseful==0),alpha,beta,y.axis="f",x.axis="cutoff",legend.place="bottomright",main="F/Cutoff")
	dev.off()

});

#
# ROCn
#
lapply(s,function(d) {

	alpha<-unique(d$alpha)
	beta<-unique(d$beta)

	filename<-sprintf("result-roc50-a%d-b%d.pdf",alpha*100,beta*100)
	pdf(file=filename,height=9,width=9)
	par(cex=1.3,cex.main=1.2,lwd=2)
	plot.roc(d,alpha,beta,calc.auc=T,rocn=50)
	dev.off()
});
