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

plot.roc<-function(d)
{
	nruns<-length(unique(d$run))

	column.indices<-grep("^p\\.",colnames(d),perl=T)
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
	# Convert the list of lists to a data frame
#	result.frame<-do.call(rbind,lapply(result.list,data.frame))

	colors<-c("red","blue","green","cyan","magenta", "gray", "purple", "brown")
	l<-list();

	pred<-prediction(1-d$p.tft,d$label)
	perf<-performance(pred, measure = "tpr", x.measure = "fpr") 
	auc.perf<-performance(pred, measure = "auc")
	auc<-auc.perf@y.values[[1]]
#plot(perf, col="red",main=sprintf("comparison for %d runs (AUC=%g)",nruns,auc),downsampling=100)
	name<-"Term for Term"
	l<-append(l,sprintf("%s (%g)",name,auc))
	plot(perf, col=colors[1],main="Comparison",downsampling=100)

	pred<-prediction(1-d$p.pcu,d$label)
	perf<-performance(pred, measure = "tpr", x.measure = "fpr") 
	auc.perf<-performance(pred, measure = "auc")
	auc<-auc.perf@y.values[[1]]
#plot(perf, col=rainbow(10),main=sprintf("Parent Child Union for %d runs (AUC=%g)",nruns,auc),downsampling=100)
	name<-"Parent Child"
	l<-append(l,sprintf("%s (%g)",name,auc))
	plot(perf, col=colors[2],downsampling=100, add=TRUE)
	
	pred<-prediction(1-d$p.tweight,d$label)
	perf<-performance(pred, measure = "tpr", x.measure = "fpr") 
	auc.perf<-performance(pred, measure = "auc")
	auc<-auc.perf@y.values[[1]]
	name<-"Topology Weighted"
	l<-append(l,sprintf("%s (%g)",name,auc))
	plot(perf, col=colors[3],downsampling=100, add=TRUE)

	pred<-prediction(1-d$p.pb,d$label)
	perf<-performance(pred, measure = "tpr", x.measure = "fpr") 
	auc.perf<-performance(pred, measure = "auc")
	auc<-auc.perf@y.values[[1]]
	name<-"Probabilistic"
	l<-append(l,sprintf("%s (%g)",name,auc))
	plot(perf, col=colors[4],downsampling=100, add=TRUE)

	pred<-prediction(1-d$p.b2g.ideal,d$label)
	perf<-performance(pred, measure = "tpr", x.measure = "fpr") 
	auc.perf<-performance(pred, measure = "auc")
	auc<-auc.perf@y.values[[1]]
	name<-"B2G: Ideal"
	l<-append(l,sprintf("%s (%g)",name,auc))
	plot(perf, col=colors[5],downsampling=100, add=TRUE)

	pred<-prediction(1-d$p.b2g.em,d$label)
	perf<-performance(pred, measure = "tpr", x.measure = "fpr") 
	auc.perf<-performance(pred, measure = "auc")
	auc<-auc.perf@y.values[[1]]
	name<-"B2G: EM"
	l<-append(l,sprintf("%s (%g)",name,auc))
	plot(perf, col=colors[6],downsampling=100, add=TRUE)

	pred<-prediction(1-d$p.b2g.ideal.nop,d$label)
	perf<-performance(pred, measure = "tpr", x.measure = "fpr") 
	auc.perf<-performance(pred, measure = "auc")
	auc<-auc.perf@y.values[[1]]
	name<-"B2G: No Prior"
	l<-append(l,sprintf("%s (%g)",name,auc))
	plot(perf, col=colors[7],downsampling=100, add=TRUE)

	legend("right",	col=colors, legend = unlist(l), fill=colors)
}

pdf(file="result.pdf",height=6,width=6)
par(mfrow=c(1,1))
plot.roc(d)
dev.off()
system("evince result.pdf")








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
