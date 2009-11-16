library(ROCR)

dir<-"."
filename<-"result-fp.txt"


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
# Draw some performance plots
#
plot.roc<-function(d,alpha=NA,beta=NA,calc.auc=F,y.axis="tpr",x.axis="fpr",xlim=c(0,1),ylim=c(0,1),legend.place="bottomright",rocn=NA)
{
	nruns<-length(unique(d$run))
	
	if (nrow(d)==0)
	{
		return()
	}
	
	l<-list();

	v<-matrix(ncol=2,byrow=T,
	              c("p.tft","Term for Term",
				    "p.pcu", "Parent Child",
				    "p.tweight","Topology Weighted",
					"p.pb", "GenGO",
					"p.b2g.ideal.pop", "B2G: Known Parameter",
					"p.b2g.mcmc.pop", "B2G: Unknown Parameter",
					"p.b2g.ideal.pop.nop", "B2G: No Prior, Known Parameter"
	               ))

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

			d.new<-do.call(rbind,d.new) # merge the lists
			print(sprintf("%s: %f (alpha=%f, beta=%f)",v[i,2],avg.rocn,alpha,beta))
		}
	
		pred<-prediction(1-values,labels)
		perf<-performance(pred, measure = y.axis, x.measure = x.axis) 
		name<-v[i,2]
		
		if (calc.auc)
		{
			auc.perf<-performance(pred, measure = "auc")
			auc<-auc.perf@y.values[[1]]
			l<-append(l,sprintf("%s (%g)",name,auc))
		} else
		{
			l<-append(l,sprintf("%s",name))
		}

		if (i==1)
		{
			plot(perf, col=colors[i],pch=pchs[i],downsampling=300, type="l", ylim=ylim,xlim=xlim,main=sprintf("Comparison (alpha=%g,beta=%g)",alpha,beta))
			plot(perf, col=colors[i],pch=pchs[i],downsampling=25, type="p", add=TRUE)
		} else
		{
			plot(perf, col=colors[i],pch=pchs[i],downsampling=300, type="l", add=TRUE)
			plot(perf, col=colors[i],pch=pchs[i],downsampling=25, type="p", add=TRUE)
		}		
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
	pdf(file=filename,height=9,width=9)
	par(cex=1.3,cex.main=1.2,lwd=2)
	plot.roc(subset(d,d$senseful==0),alpha,beta,calc.auc=T,rocn=10)
	dev.off()

	filename<-sprintf("result-roc-a%d-b%d-senseful.pdf",alpha*100,beta*100)
	pdf(file=filename,height=9,width=9)
	par(cex=1.3,cex.main=1.2,lwd=2)
	plot.roc(subset(d,d$senseful==1),alpha,beta,calc.auc=T,rocn=10)
	dev.off()
});

#
# Pre/Recall
#

lapply(s,function(d) {
	alpha<-unique(d$alpha)
	beta<-unique(d$beta)

	filename<-sprintf("result-precall-a%d-b%d.pdf",alpha*100,beta*100)
	pdf(file=filename,height=9,width=9)
	par(cex=1.3,cex.main=1.2,lwd=2)
	plot.roc(subset(d,d$senseful==0),alpha,beta,y.axis="prec",x.axis="rec",legend.place="topright")
	dev.off()

	filename<-sprintf("result-precall-a%d-b%d-senseful.pdf",alpha*100,beta*100)
	pdf(file=filename,height=9,width=9)
	par(cex=1.3,cex.main=1.2,lwd=2)
	plot.roc(subset(d,d$senseful==1),alpha,beta,y.axis="prec",x.axis="rec",legend.place="topright")
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


	pdf(file="result-roc.pdf",height=5.5,width=5.5)
	par(cex=1.5)
	par(mfrow=c(1,1))
	plot.roc(d)
	dev.off()
	system("evince result.pdf")
	
	pdf(file="result-roc-senseful.pdf",height=5.5,width=5.5)
	par(cex=1.5)
	par(mfrow=c(1,1))
	plot.roc(subset(d,d$senseful==1))
	dev.off()
	
	pdf(file="result-roc-notsenseful.pdf",height=5.5,width=5.5)
	par(cex=1.5)
	par(mfrow=c(1,1))
	plot.roc(subset(d,d$senseful==0))
	dev.off()






s<-split(d,d$run)
filter<-function(el)
{
	root.idx<-which(el$term==0)
	total.genes<-el[root.idx,"pop.genes"]
	proportion<-el$pop.genes/total.genes
#	su<-sum(proportion[which(el$label==T)])
#	return(su > 0.2)
	return(sum(proportion[which(el$label==T)]>0.1) == length(which(el$label==T)))
}

filter.index<-as.vector(sapply(s,filter))
filter.list<-s[which(filter.index==T)]
filter.dat<-do.call("rbind",filter.list)

pdf(file="result-2.pdf",height=6,width=18)
par(mfrow=c(1,3))
plot.roc(filter.dat)
dev.off()



options(width=120)
ord<-order(d$run,d$term)
d.ord<-d[ord,]
print(d.ord[which(d.ord$label==T),])




#s<-split(d,d$run)
#void<-sapply(s,function(el){
#			print(s[which(el$label==T),])
#		})
