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
			"p.b2g.em.pop", "MGSA-EM",
			"p.b2g.mcmc.pop", "MGSA"
           ))

# Read the input file. Ignore any 0 term.
full.filename<-file.path(dir,filename)
d<-read.table(full.filename,h=T)
d<-subset(d,d$term!=0)


# Brings the results to a managable form
evaluate<-function(d)
{
	nruns<-length(unique(d$run))
	
	if (nrow(d)==0)
	{
		return()
	}
	
	res<-list();
	colnames(v)<-c("short","full")

	# ROCn preparations
	d.by.run<-split(d,d$run)
	rocn<-10

	for (i in (1:nrow(v)))
	{
		ord<-order(d[,v[i,1]])

		# data is orderd. Threshold is such that the values
		# above an element are flagged as positive (inclusive)
		# and values below an element as negative.
		values<-d[ord,v[i,1]]
		labels<-d[ord,]$label

		tps<-cumsum(labels)					# true positves
		fps<-(1:length(labels)) - tps		# false postives

		tpr<-tps / tps[length(tps)]			# true positves rate
		fpr<-fps / fps[length(fps)]			# false postives rate
		prec<-tps/(1:length(values))		# number of true positives / (number of all positives = (true positives + false negatives))
		recall<-tps/sum(labels)      		# number of true positives / (true positives + false negatives = all positive samples)

		l<-list(name=v[i,2],short=v[i,1])
		
		# precision/recall values
		idx.dots<-cumsum(hist(recall,plot=F,breaks=15)$counts)
		idx.lines<-cumsum(hist(recall,plot=F,breaks=300)$counts)
		l<-c(l,prec.lines=list(prec[idx.lines]),recall.lines=list(recall[idx.lines]))
		l<-c(l,prec.dots =list(prec[idx.dots]), recall.dots =list(recall[idx.dots]))

		#
		# true positive / false postive
		#
		idx.dots<-cumsum(hist(fpr,plot=F,breaks=25)$counts)
		idx.lines<-c(1,cumsum(hist(fpr,plot=F,breaks=300)$counts))
		
		# For AUROC scores we request a higher resolution 
		idx.auroc<-c(1,cumsum(hist(fpr,plot=F,breaks=1000)$counts))
		
		# calculate the AUROC. Note that diff() returns the difference of
		# consecutive elements. We calculate the lower bound of the area.
		auroc<-sum(c(diff(fpr[idx.lines]),0) * tpr[idx.lines])

		# Average of ROCn
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

		l<-c(l,fpr.lines=list(fpr[idx.lines]), tpr.lines=list(tpr[idx.lines]))
		l<-c(l,fpr.dots= list(fpr[idx.dots]),  tpr.dots= list(tpr[idx.dots]))
		l<-c(l,auroc=auroc)
		l<-c(l,avg.rocn=avg.rocn)
		l<-c(l,rocn=rocn)

		res<-c(res,list(l));
	}
	
	return(res)
}


s<-split(d,list(d$alpha,d$beta))

#
# Evaluate
#
r.all<-lapply(s, function(d) {
	alpha<-unique(d$alpha)
	beta<-unique(d$beta)

	r<-evaluate(subset(d,d$senseful==0))

	# keep results and alpha/beta entries in a new list	
	r<-list(r=r,alpha=alpha,beta=beta)
	
	return(r)
})

#
# Plot all gfx. Take pre evaluated r.all.
#
lapply(r.all, function(r.one) {
	alpha<-r.one$alpha
	beta<-r.one$beta
	r<-r.one$r

	o<-c(4,5,7,3,2,1)
	r<-r[o]

	colors<-c(hcl(c(0,60),l=60,alpha=0.9),hcl(c(60,120,180,240),l=45))
	sp<-c(0.25,0.25,0.5,0.25,0.25,0.25)
	dens<-c(-1,-1,-1,-1,-1,-1,-1)
	
	pchs<-1:length(r)

	# Pre/Recall plots
	filename<-sprintf("result-precall-a%d-b%d.pdf",alpha*100,beta*100)
	pdf(file=filename,height=8,width=8)
	par(cex=1.4,cex.main=1.5,lwd=2)
	title<-bquote(paste("Precision/Recall: ", alpha,"=",.(alpha),",",beta,"=",.(beta)))
	plot(main=title,ylab="Precision",xlab="Recall",xlim=c(0,1),ylim=c(0,1),0.5,0.5,type="n") # fake invisible point such that the next loop can be simplified
	for (i in 1:length(r))
	{
		lines(y=r[[i]]$prec.lines, x=r[[i]]$recall.lines, col=colors[i],pch=pchs[i],type="l")
		points(y=r[[i]]$prec.dots, x=r[[i]]$recall.dots,  col=colors[i],pch=pchs[i],type="p")	
	}
	legend("topright", col=colors, pch=pchs, legend = sapply(r,function(x){return(x$name)}))
	dev.off()
	

	# ROC plots
	filename<-sprintf("result-roc-a%d-b%d.pdf",alpha*100,beta*100)
	pdf(file=filename,height=8,width=8)
	par(cex=1.4,cex.main=1.5,lwd=2)
	title<-bquote(paste("ROC: ", alpha,"=",.(alpha),",",beta,"=",.(beta)))
	plot(main=title,ylab="True postive rate",xlab="False positive rate",xlim=c(0,1),ylim=c(0,1),0.5,0.5,type="n") # fake invisible point such that the next loop can be simplified
	for (i in 1:length(r))
	{
		lines(y=r[[i]]$tpr.lines, x=r[[i]]$fpr.lines, col=colors[i],pch=pchs[i],type="l")
		points(y=r[[i]]$tpr.dots, x=r[[i]]$fpr.dots,  col=colors[i],pch=pchs[i],type="p")	
	}
	legend("bottomright", col=colors, pch=pchs, legend = sapply(r,function(x){return(sprintf("%s (%.3g)",x$name,x$auroc))}))
	dev.off()

	# Bar plots for prec/recall
	filename<-sprintf("result-bar-a%d-b%d.pdf",alpha*100,beta*100)
	pdf(file=filename,height=8,width=8)
	par(cex=1.36,cex.main=1.5,lwd=2)
	title<-bquote(paste("Precision at Recall of 0.2: ", alpha,"=",.(alpha),",",beta,"=",.(beta)))
	heights<-sapply(r,function(x){ idx<-which(x$recall.lines>0.199)[1]; return(x$prec.lines[idx]) })
	names(heights)<-sapply(r,function(x){return(x$name)})
	barplot2(main=title,heights,col=colors,ylim=c(0,0.1),plot.grid=T,space=sp,density=dens)
	dev.off()
});



	
	# Bar plots for AUROC and prec/recall
#	filename<-sprintf("result-bar-prauroc-a%d-b%d.pdf",alpha*100,beta*100)
#	pdf(file=filename,height=8,width=8)
#	par(cex=1.4,cex.main=1.5,lwd=2)
#	title<-bquote(paste("AUROC and Precision at Recall of 0.2: ", alpha,"=",.(alpha),",",beta,"=",.(beta)))
#	heights.prec<-sapply(r,function(x){ idx<-which(x$recall.lines>0.199)[1]; return(x$prec.lines[idx]) })
#	heights.auroc<-sapply(r,function(x){ return(x$auroc) })
#	heights<-rbind(heights.auroc,heights.prec)
#	colnames(heights)<-sapply(r,function(x){return(x$name)})
#	barplot2(main=title,heights,col=rbind(colors2,colors),beside=T)
#	dev.off()

	# Bar plots for modAUROC and prec/recall
#	filename<-sprintf("result-bar-prmodauroc-a%d-b%d.pdf",alpha*100,beta*100)
#	pdf(file=filename,height=8,width=8)
#	par(cex=1.4,cex.main=1.5,lwd=2)
#	title<-bquote(paste("ModAUROC and Precision at Recall of 0.2: ", alpha,"=",.(alpha),",",beta,"=",.(beta)))
#	heights.prec<-sapply(r,function(x){ idx<-which(x$recall.lines>0.199)[1]; return(x$prec.lines[idx]) })
#	heights.auroc<-sapply(r,function(x){ return(abs(x$auroc - 0.5)*2) })
#	heights<-rbind(heights.auroc,heights.prec)
#	colnames(heights)<-sapply(r,function(x){return(x$name)})
#	barplot2(main=title,heights,col=rbind(colors2,colors),beside=T)
#	dev.off()




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
