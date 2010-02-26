function toHash(array){
	hash=new Object();
	if (array!==null){
		for (var i=0;i<array.length;i++){
			hash[array[i]]=array[i];
		}
	}
	return hash;
}

function toArray(hash){
	array=new Array();
	var count=0;
	for (var i in hash){
		array[count]=hash[i];
		count=count+1;
	}
	return array;
}

var Selection_Type_Simple="simple";
var Selection_Type_Path="path";
var Selection_Type_Range="range";

var Value_Operation_OR=0;
var Value_Operation_AND=1;

function Selection(type){
	this.type=type;
	this.depth=0;
	this.strict=false;
	this.operation=Value_Operation_OR;
	this.values=new Array();
	this.addValue=function(value){
		this.values[this.values.length]=value;
	}
	this.clear=function(){
		this.values.length=0;
	}
	
	this.toHttpGetString=function(name){
		var getString="";
		getString+="&bobo.sel."+name+".depth="+this.depth;
		if (this.strict){
			getString+="&bobo.sel."+name+".strict=true";
		}
		if (this.operation==Value_Operation_AND){
			getString+="&bobo.sel."+name+".operation=and";
		}
		for (var i=0;i<this.values.length;i++){
			val=this.values[i];
			getString+="&bobo.sel."+name+".val="+val;
		}
		return getString;
	}
}

var OutputSpec_Order_Hits=1;
var OutputSpec_Order_Value=0;

function OutputSpec(){
	this.order=OutputSpec_Order_Value;
	this.max=0;
	this.expandSelection=false;
	
	this.toHttpGetString=function(name){
		var getString="";
		getString+="&bobo.groupby."+name+".max="+this.max;
		if (this.expandSelection){
			getString+="&bobo.groupby."+name+".expand=true";
		}
		if (this.order==OutputSpec_Order_Hits){
			getString+="&bobo.groupby."+name+".orderby=hits";
		}
		return getString;
	}
}

function BrowseRequest(numPerPage){
	this.numPerPage=numPerPage;
	this.offset=0;
	this.queryString="";
	this.selections=new Object();
	this.sortSpec=new Object();
	this.ospecs=new Object();
	
	this.maxSortHistory=10;
	
	this.getSortSpec=function(field){
		var sort=sortSpecs[field];
		if (sort!=null){
			return sort.reverse;
		}
		else{
			return null;
		}
	}
	
	this.toggleSort=function(field){
		
		if (this.sortSpec.name==field){
			this.sortSpec.reverse=!this.sortSpec.reverse;
		}
		else{
			this.sortSpec.name=field;
			this.sortSpec.reverse=false;
		}
	}
	
	this.clearSelection=function(field){
		this.selections[field].values=new Array();
	}
	
	this.setQueryString=function(queryString){
		if (queryString!=null){
			this.queryString=queryString;
		}
		else{
			this.queryString="";
		}
	}
	
	this.reset=function(){
		this.offset=0;
		this.queryString="";
		for (var i in this.selections){
			this.clearSelection(i);
		}
	}
	
	this.toHttpGetString=function(){
		var getString="";
		getString+="q="+this.queryString;
		getString+="&start="+this.offset;
		getString+="&rows="+this.numPerPage;
		
		for (var i in this.selections){
			sel=this.selections[i];
			getString+=sel.toHttpGetString(i);
		}
		
		for (var i in this.ospecs){
			ospec=this.ospecs[i];
			getString+=ospec.toHttpGetString(i);
		}
		
		if (this.sortSpec.name){
		    getString+="&sort=";
			var dir;
			if (this.sortSpec.reverse){
				dir=this.sortSpec.name+" desc";
			}
			else{
				dir=this.sortSpec.name+" asc";
			}
			getString+=dir;
		}
		
		
		return getString;
	}
}
