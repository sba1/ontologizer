library(ROCR)

#dir<-"/home/sba/remote/solexa.sshfs/sba/b2go"
#dir<-"/home/sba/workspace/ontologizer-sf/"
dir<-"."
filename<-"result-fp.txt"


full.filename<-file.path(dir,filename)
d<-read.table(full.filename,h=T)
d<-subset(d,d$term!=0)

#result.files<-c("result-Term-For-Term-fp.txt",
#		        "result-Parent-Child-Union-fp.txt",
#				"result-Bayes2GO-fp.txt")

#for (filename in result.files)
#{
#	is.bayes<-length(grep("Bayes",filename))
#filename<-"result-Term-For-Term.txt"
#filename<-"result-Bayes2GO.txt"

#	if (is.bayes)
#	{
#		
#	}

#el<-s[[1]]
#pos<-which(el$pred==T)
#tp<-intersect(which(el$label==T),pos)
#fp<-setdiff(pos,tp)

#a<-t(sapply(s,function(el){
#			pos<-which(el$pred==T)
#			neg<-which(el$pred==F)
#			tp<-intersect(which(el$label==T),pos)
#			tn<-intersect(which(el$label==F),neg)
#			fp<-setdiff(pos,tp)
#			fn<-setdiff(neg,tn)
#			return(data.frame(tp=length(tp),fp=length(fp),tn=length(tn),fn=length(fn),pos=length(tp)+length(fn),neg=length(tn)+length(fp)))
#		}))
#
#for (b in  split(as.data.frame(a),as.numeric(a[,"pos"])))
#{
#	tpr<-sum(as.numeric(b[,"tp"]))/sum(as.numeric(b[,"pos"]))
#	tnr<-sum(as.numeric(b[,"tn"]))/sum(as.numeric(b[,"neg"]))
#	
#	print(paste("terms=",unique(b$pos)," tpr=",tpr," tnr=",tnr,sep=""))	
#}
#}


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
plot.roc<-function(d,alpha=NA,beta=NA,calc.auc=F,y.axis="tpr",x.axis="fpr",legend.place="bottomright")
{
	nruns<-length(unique(d$run))
	
	if (nrow(d)==0)
	{
		return()
	}

#	column.indices<-grep("^p\\.",colnames(d),perl=T)
#	sapply(colnames(d)[column.indices],decode.parameter.setting)
#	result.list<-list();
#	for (ci in column.indices)
#	{
#		cn<-colnames(d)[ci]
#
#		pred<-prediction(1-d[,ci],d$label)
#		perf<-performance(pred, measure = "tpr", x.measure = "fpr")
#		auc.perf<-performance(pred, measure = "auc")
#		auc<-auc.perf@y.values[[1]]
#
#		entry<-decode.parameter.setting(cn)
#		entry<-append(entry,list(auc=auc))
#		result.list<-append(result.list,list(entry))
#
#		print(paste(decode.name(cn)," auc=", auc, sep=""))		
#	}
#	# Convert the list of lists to a data frame
#	result.frame<-do.call(rbind,lapply(result.list,data.frame))

	l<-list();

#	v<-matrix(ncol=2,byrow=T,
#	              c("p.tft","Term for Term",
#                    "p.tft.bf","Term for Term: BF",
#				    "p.pcu", "Parent Child",
#				    "p.tweight","Topology Weighted",
#					"p.pb", "Probabilistic (Lu et al.)",
#					"p.b2g.ideal", "B2G: Ideal",
#					"p.b2g.ideal.pop", "B2G: Ideal, PaR",
#					"p.b2g.em", "B2G: EM",
#					"p.b2g.mcmc", "B2G: Full MCMC",
#					"p.b2g.mcmc.cexpt", "B2G: Full MCMC (p known)",
#					"p.b2g.ideal.nop", "B2G: No Prior"
#	               ))
	v<-matrix(ncol=2,byrow=T,
	              c("p.tft","Term for Term",
				    "p.pcu", "Parent Child",
				    "p.tweight","Topology Weighted",
					"p.pb", "Probabilistic (Lu et al.)",
					"p.b2g.ideal", "B2G: Ideal",
					"p.b2g.ideal.pop", "B2G: Ideal, PaR",
					"p.b2g.mcmc", "B2G: MCMC",
					"p.b2g.mcmc.pop", "B2G: MCMC, PaR",
					"p.b2g.ideal.pop.nop", "B2G: No Prior"
	               ))

	colnames(v)<-c("short","full")
	colors<-rainbow(nrow(v))
	pchs<-1:nrow(v)

	for (i in (1:nrow(v)))
	{
		pred<-prediction(1-d[,v[i,1]],d$label)
		perf<-performance(pred, measure = y.axis, x.measure = x.axis) 
		name<-v[i,2]
		
		if (calc.auc)
		{
			auc.perf<-performance(pred, measure = "auc")
			auc<-auc.perf@y.values[[1]]
			l<-append(l,sprintf("%s (%g)",name,auc))
		} else
		{
#			f.perf<-performance(pred, measure = "f")
#			f<-f.perf@y.values[[1]]
			l<-append(l,sprintf("%s",name))
		}

		if (i==1)
		{
			plot(perf, col=colors[i],pch=pchs[i],downsampling=200, type="l", ylim=c(0,1),xlim=c(0,1),main=sprintf("Comparison (alpha=%g,beta=%g)",alpha,beta))
			plot(perf, col=colors[i],pch=pchs[i],downsampling=25, type="p", add=TRUE)
		} else
		{
			plot(perf, col=colors[i],pch=pchs[i],downsampling=200, type="l", add=TRUE)
			plot(perf, col=colors[i],pch=pchs[i],downsampling=25, type="p", add=TRUE)
		}		
	}

	legend(legend.place, col=colors, pch=pchs, legend = unlist(l))
}

s<-split(d,list(d$alpha,d$beta))
#
#	filename<-sprintf("result-roc-a%d-b%d.pdf",alpha*100,beta*100)
#	pdf(file="huhuh.pdf",height=9,width=9)
#	par(cex=1.3,cex.main=1.2,lwd=2)
#	plot.roc(d,alpha,beta)
#	dev.off()


lapply(s,function(d) {
	alpha<-unique(d$alpha)
	beta<-unique(d$beta)

	filename<-sprintf("result-roc-a%d-b%d.pdf",alpha*100,beta*100)
	pdf(file=filename,height=9,width=9)
	par(cex=1.3,cex.main=1.2,lwd=2)
	plot.roc(d,alpha,beta,calc.auc=T)
	dev.off()

	filename<-sprintf("result-roc-a%d-b%d-senseful.pdf",alpha*100,beta*100)
	pdf(file=filename,height=9,width=9)
	par(cex=1.3,cex.main=1.2,lwd=2)
	plot.roc(subset(d,d$senseful==1),alpha,beta,calc.auc=T)
	dev.off()

	filename<-sprintf("result-roc-a%d-b%d-no-restriction.pdf",alpha*100,beta*100)
	pdf(file=filename,height=9,width=9)
	par(cex=1.3,cex.main=1.2,lwd=2)
	plot.roc(subset(d,d$senseful==0),alpha,beta,calc.auc=T)
	dev.off()
});


lapply(s,function(d) {
	alpha<-unique(d$alpha)
	beta<-unique(d$beta)

	filename<-sprintf("result-precall-a%d-b%d.pdf",alpha*100,beta*100)
	pdf(file=filename,height=9,width=9)
	par(cex=1.3,cex.main=1.2,lwd=2)
	plot.roc(d,alpha,beta,y.axis="prec",x.axis="rec",legend.place="topright")
	dev.off()

	filename<-sprintf("result-precall-a%d-b%d-senseful.pdf",alpha*100,beta*100)
	pdf(file=filename,height=9,width=9)
	par(cex=1.3,cex.main=1.2,lwd=2)
	plot.roc(subset(d,d$senseful==1),alpha,beta,y.axis="prec",x.axis="rec",legend.place="topright")
	dev.off()

	filename<-sprintf("result-precall-a%d-b%d-no-restriction.pdf",alpha*100,beta*100)
	pdf(file=filename,height=9,width=9)
	par(cex=1.3,cex.main=1.2,lwd=2)
	plot.roc(subset(d,d$senseful==0),alpha,beta,y.axis="prec",x.axis="rec",legend.place="topright")
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
