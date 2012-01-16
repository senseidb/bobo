//all widgets must have an update method


function Widget(){
	this.update=function(selections,choice){};
}

SelectionWidget.prototype=new Widget();
SelectionWidget.prototype.constructor=SelectionWidget;

function makeDisplay(val,count){
	return val+" ("+count+")";
}



function SelectionWidget(){
	this.addItem=function(display,value,selected){};
	this.clearItems=function(){};
	this.displayFn=makeDisplay;
	
	this.update=function(selections,choices){
	
		var allselected=false;	
    	if (selections[this.field].values.length==0){
    		allselected=true;
    	}
    	
    	var tmp=new Object();
    	for (var i=0;i<selections[this.field].values.length;++i){
    		tmp[selections[this.field].values[i]]=selections[this.field].values[i];
    	}
		var choiceObj=choices[this.field];				
		var choiceArray=choiceObj.choicelist;
		var choiceTotalCount=choiceObj.totalcount;		
		
		this.clearItems();
		var selected=false;
		
		if (choiceArray.length==0){
			selected=true;
		}
		
		if (allselected){
			this.addItem("All","",true);
		}
		else{
			this.addItem("All","",selected);
		}
		var val,hits;
		var display;		
				
		for (var i=0;i<choiceArray.length;++i){
			val=choiceArray[i].val;
			hits=choiceArray[i].hits;
			display=this.displayFn(val,hits);	
			if (tmp[val]!=null) selected=true;
			else selected=false;
			this.addItem(display,val,selected);
		}
	}
}
/*
SelectBox.prototype=new SelectionWidget();
SelectBox.prototype.constructor=SelectBox;

function SelectBox(section,listener){
	this.field=section.id;	
	this.elem=document.createElement("select");
	this.section=section;
	this.elem.setAttribute("name",this.field);
	this.section.appendChild(this.elem);
	this.choices=this.elem.options;
	this.elem.onchange=function(){
		if (this.selectedIndex>=0){		
			var field=this.name;	
			var val=this.options[this.selectedIndex].value;
			listener(field,val);
		}
	}
	this.addItem=function(display,value,selected){
		this.choices[this.choices.length]=new Option(display,value,false,selected);
	}
	this.clearItems=function(){
		this.choices.length=0;
	}
}
*/
function Link(field,text,value,callback){
	this.field=field;
	this.node=document.createElement("a");
	this.node.innerHTML=text;
	this.node.name=value;
		
	this.node.onmouseout=function(){
		this.style.color='black';
		this.style.fontWeight='normal';
	}
	
	this.node.onmouseover=function(){
		this.style.cursor='pointer';
		this.style.color='blue';
		this.style.fontWeight='bold';
	}
	
	this.node.onclick=function(){
		callback(field,this.name);
	}		
}

function TagLink(field,text,weight,callback){		
	this.field=field;
	this.node=document.createElement("a");
	this.node.setAttribute('class','tag'+weight);
		this.node.setAttribute('className','tag'+weight);
	this.node.innerHTML=text;
	
	
	this.node.name=text;
	
	this.node.onmouseout=function(){
		this.style.color='black';
		this.style.fontWeight='normal';
	}
	
	this.node.onmouseover=function(){
		this.style.cursor='pointer';
		this.style.color='blue';
		this.style.fontWeight='bold';
	}
	
	this.node.onclick=function(){
		callback(field,this.name);
	}		
}

SelectList.prototype=new SelectionWidget();
SelectList.prototype.constructor=SelectList;

function SelectList(section,callback){
	this.field=section.id;
	
	this.table=document.createElement("table");
	this.table.width="100%";
	
	this.headerRow=this.table.insertRow(0);
	
	this.headerCell=this.headerRow.insertCell(0);
	
	this.headerCell.setAttribute("class","ROWHEAD");
	this.headerCell.setAttribute("className","ROWHEAD");
	this.headerCell.setAttribute("align","left");
	this.headerCell.appendChild(document.createTextNode(this.field+": "));

	
	this.dataRow=this.table.insertRow(1);
	
	this.dataCell=this.dataRow.insertCell(0);
	this.dataCell.className="SELECTELEM";
	this.dataRow.setAttribute("align","left");
	
	this.elem=document.createElement("ul");
	this.dataCell.appendChild(this.elem);
	this.callback=callback;
	
	this.section=section;
	this.section.appendChild(this.table);
	
	this.addItem=function(text,value,selected){
		var e;
		if (selected){
			e=document.createElement("span");
			e.className="LINKSELECTED";
			e.appendChild(document.createTextNode(text));
		}
		else{
			var link=new Link(this.field,text,value,this.callback);
			e=link.node;
		}
		var li=document.createElement("li");
		li.appendChild(e);
		this.elem.appendChild(li);
	}
	this.clearItems=function(){
		var items=this.elem.childNodes;
		var len=items.length;
		while(items[0]!=null){
			this.elem.removeChild(items[0]);
		}
	}
}

function stringCompare(s1,s2){
	var len=Math.min(s1.length,s2.length);
	for (var i=0;i<len;++i){
		if (s1.charAt(i) < s2.charAt(i)) return -1;
		if (s1.charAt(i) > s2.charAt(i)) return 1;
	}
	
	if (len<s1.length){
		return 1;
	}
	else if (len<s2.length){
		return -1;
	}
	else{
		return 0;
	}
}

function tagSort(tag1,tag2){
	s1=tag1.val.toLowerCase();
	s2=tag2.val.toLowerCase();
	val = stringCompare(s1,s2);
	return val;
}


TagCloud.prototype=new SelectionWidget();
TagCloud.prototype.constructor=TagCloud;

function TagCloud(section,callback){
    this.section=section;
    this.field=section.id;
    
    this.table=document.createElement("table");
    this.table.width="100%";
    
    this.section.appendChild(this.table);
    
    this.topRow=this.table.insertRow(0);
    this.topCell=this.topRow.insertCell(0);
    
    this.topCell.setAttribute("class","ROWHEAD");
    this.topCell.setAttribute("className","ROWHEAD");
    this.topCell.appendChild(document.createTextNode(this.field+": "));
    this.tagList=document.createElement("span");
    this.topCell.appendChild(this.tagList);
    
    this.bottomRow=this.table.insertRow(1);
    
    this.bottomCell=this.bottomRow.insertCell(0);
    this.bottomCell.align="center";
    
	this.field=section.id;
	
	this.clearItems=function(){		
		while (this.bottomCell.childNodes[0]) {
    		this.bottomCell.removeChild(this.bottomCell.childNodes[0]);
		}
	}
	
	this.update=function(selections,choices){
		
		var tagSel=selections[this.field];
		var choiceObj=choices[this.field];
		this.clearItems();	
		
		
		var selectedTags;

		if (tagSel!=null && tagSel.values!=null){
			selectedTags=toHash(tagSel.values);
		}
		else{
			selectedTags=new Object();
		}
		
	    // update selected tags
		while (this.tagList.childNodes[0]) {
	   		this.tagList.removeChild(this.tagList.childNodes[0]);
		}


		if (tagSel.values!=null){		
			for(var i=0;i<tagSel.values.length;++i){
				var link=new Link(this.field,tagSel.values[i],tagSel.values[i],handleRemoveTag);
				if (i!=0){
					this.tagList.appendChild(document.createTextNode(", "));
				}
				this.tagList.appendChild(link.node);
			}
		}
				
				
		var largest=10;
		var smallest=10;
			
		var choiceTotalCount=0;
		var choiceArray=null;
		if (choiceObj!=null){
			choiceTotalCount=choiceObj.totalcount;
			choiceArray=choiceObj.choicelist;	
		}
		
		if (choiceArray!=null && choiceArray.length>0){
			largest=choiceArray[0].hits;
			smallest=choiceArray[choiceArray.length-1].hits;
		
			var n=largest-smallest;
			
			choiceArray.sort(tagSort);
			
			for (var i=0;i<choiceArray.length;++i){
				
				var text=choiceArray[i].val;
				var weight=choiceArray[i].hits;
				
				if (selectedTags[text]!=null) continue;
				
				var rank;
				if (n>0){
					rank=parseInt(9*weight/n+1);
					if (rank>10) rank=10;
					if (rank<1) rank=1;
				}
				else{
					rank=10;
				}
				
				
				var link=new TagLink(this.field,text,rank,callback);
				
				this.bottomCell.appendChild(link.node);
				this.bottomCell.appendChild(document.createTextNode(" "));
			}
		}
	}
}

BreadCrumb.prototype.update=function(currentpath,sep,count){
	this.clearItems();	
	var link=null;
	
	link=new Link(this.field,"All","",this.callback);
	this.section.appendChild(link.node);	
	this.section.appendChild(document.createTextNode(sep));
	
	if (currentpath!=null && currentpath.length>0){
	
		var start=0;
		var index=0;
		var callback=this.listener;
		
		while (index!=-1){
			index=currentpath.indexOf(sep,start);
			var substr,path;
			if (index==-1){
				substr=currentpath.substring(start,currentpath.length);
				path=currentpath;		
			}
			else{
				substr=currentpath.substring(start,index);
				path=currentpath.substring(0,index);		
			}
	
			link=new Link(this.field,substr,path,this.callback);
			
			this.section.appendChild(link.node);
			
			
			if (index!=-1){
				this.section.appendChild(document.createTextNode(sep));
			}
			start=index+1;
		}
	}
		
	if (count>0){
		this.section.appendChild(document.createTextNode(" ("+count+") "));
	}
}

function BreadCrumb(field,section,callback){
	this.field=field;
	this.section=section;
	this.callback=callback;
	this.clearItems=function(){
		var items=this.section.childNodes;
		var len=items.length;
		while(items[0]!=null){
			this.section.removeChild(items[0]);
		}
	}
}

SelectPath.prototype=new SelectionWidget();
SelectPath.prototype.constructor=SelectPath;

function SelectPath(section,callback,sep,numCols,numRows){
	this.field=section.id;
	this.callback=callback;	
	this.sep=sep;
	this.section=section;
	this.table=document.createElement("table");
	this.table.width="100%";
	this.section.appendChild(this.table);
	
	this.brRow=this.table.insertRow(0);
	
	this.brCell=this.brRow.insertCell(0);
	this.brCell.setAttribute("class","ROWHEAD");
	this.brCell.setAttribute("className","ROWHEAD");
	this.brCell.setAttribute("align","left");
	
	
	this.brCell.appendChild(document.createTextNode(this.field+": "));
	this.brSection=document.createElement("span");
	this.brCell.appendChild(this.brSection);
	
	this.breadcrumb=new BreadCrumb(this.field,this.brSection,this.callback);
	
	this.dataRow=this.table.insertRow(1);
	this.dataCell=this.dataRow.insertCell(0);
	
	this.dataTable=document.createElement("table");
	this.dataTable.width="100%";
	this.dataCell.appendChild(this.dataTable);
	
	this.items=new Array();

	this.numCols=numCols;
	this.numRows=numRows;
	
	this.draw=function(){
		// kill the current table
		var tbl=this.dataTable;
		var useless=this.dataTable.rows;
		var len=useless.length;
		while(useless[0]!=null){
			tbl.deleteRow(0);
		}

		var rowCount=parseInt(this.items.length/this.numCols)+1;
		var where;
		var row;

		for (var i=0;i<rowCount;i++){
			row=this.dataTable.insertRow(i);
			for (var k=0;k<numCols;k++){
				where=numCols*i+k;

				if (where<this.items.length){
					var col=row.insertCell(k);
					var link=this.items[where];				
					col.appendChild(link.node);
				}
			}
			
		}			
	}
	
	this.addItem=function(text,value,selected){				
		var link=new Link(this.field,text,value,this.callback);
		this.items[this.items.length]=link;
	}
	
	this.clearItems=function(){
		this.items.length=0;
	}
	
	this.getLastVal=function(val){
		if (val==null || val.length==0) return val;
		var parts=val.split(this.sep);
		for (var i=parts.length-1;i>=0;i--){		
			if (parts[i]!=null && parts[i].length>0){
				return parts[i];
			}
		}
		return "";
	}
	
	this.update=function(selections,choices){
		var selection=selections[this.field];
		var choiceObj=choices[this.field];
		
		this.clearItems();	
		
		var modelPath="";
   		if (selection!=null && selection.values!=null && selection.values.length>0){
   			modelPath=selection.values[0];
   		}
    		
		var choiceTotalCount=0;
		if (choiceObj!=null){
			choiceTotalCount=choiceObj.totalcount;
			var choiceArray=choiceObj.choicelist;
					
			var val,hits;
			var display;		
					
			for (var i=0;i<choiceArray.length;++i){
				val=choiceArray[i].val;
				hits=choiceArray[i].hits;
	
				display=this.getLastVal(val)+" ("+hits+")";				
				selected=choiceArray[i].selected;		
				this.addItem(display,val,selected);
			}
		}
		
		this.breadcrumb.update(modelPath,this.sep,choiceTotalCount);
		this.draw();
	}
}


function ResultsTable(section,fieldnames){
	this.table=document.createElement("table");
	this.table.width="100%";
	
	this.thead=document.createElement("thead");
	
	this.headRow=document.createElement("tr");
	this.thead.appendChild(this.headRow);
	
	var th=document.createElement("th");
	th.width="10%";
	th.appendChild(document.createTextNode("Sort by: "));
	this.headRow.appendChild(th);
	
	for (var i=0;i<fieldnames.length;++i){
		th=document.createElement("th");

		th.setAttribute("class","sortable");		
		th.setAttribute("className","sortable");
		th.width="18%";
		var thAnchor=document.createElement("a");
		th.appendChild(thAnchor);
		thAnchor.innerHTML=fieldnames[i];
		thAnchor.name=fieldnames[i];
		thAnchor.onclick=function(){handleSortChange(this.name);}
		thAnchor.onmouseover=function(){this.style.cursor="pointer";}
		this.headRow.appendChild(th);
	}
	
	
	this.tbody=document.createElement("tbody");
	
	this.tfoot=document.createElement("tfoot");
						
	this.footRow=document.createElement("tr");
	this.footRow.valign="bottom";
	this.footCell=document.createElement("td");
	
	this.footCell.setAttribute("colspan","6");	
	this.footCell.colSpan=6;
	this.footCell.setAttribute("class","scroll");
	this.footCell.setAttribute("className","scroll");
	this.footCell.setAttribute("align","right");
	this.footCell.align="right";
	
	this.topA=document.createElement("a");
	this.topA.innerHTML="Top ";
	this.topA.onclick=function(){handlePaging("top");}
	this.topA.onmouseover=function(){
		this.style.cursor="pointer";
	}
	this.footCell.appendChild(this.topA);
	
	
	this.upA=document.createElement("a");
	this.upA.innerHTML="Up ";	
	this.upA.onclick=function(){handlePaging("up");}
	this.upA.onmouseover=function(){
		this.style.cursor="pointer";
	}
	this.footCell.appendChild(this.upA);
	
	
	this.downA=document.createElement("a");
	this.downA.innerHTML="Down ";
	this.downA.onclick=function(){handlePaging("down");}
	this.downA.onmouseover=function(){
		this.style.cursor="pointer";
	}
	this.footCell.appendChild(this.downA);
	
	
	this.bottomA=document.createElement("a");
	this.bottomA.innerHTML="Bottom ";
	this.bottomA.onclick=function(){handlePaging("bottom");}
	this.bottomA.onmouseover=function(){
		this.style.cursor="pointer";
	}
	
	this.footCell.appendChild(this.bottomA);
	
	this.footRow.appendChild(this.footCell);
	this.tfoot.appendChild(this.footRow);


	this.table.appendChild(this.thead);
	this.table.appendChild(this.tbody);
	this.table.appendChild(this.tfoot);

	section.appendChild(this.table);
	
	this.fieldnames=fieldnames;
	this.clearRows=function(){
		var len=this.tbody.childNodes.length;

		while(this.tbody.childNodes[0]!=null){
			this.tbody.removeChild(this.tbody.childNodes[0]);
		}
	}
	
	this.update=function(hits,offset){
		this.clearRows();
		for (var i=0;i<hits.length;++i){
			var doc=hits[i].doc;
			// draw a row
			var row=document.createElement("tr");
			this.tbody.appendChild(row);
			
			var cell=row.insertCell(0);
			cell.align="center";
			cell.appendChild(document.createTextNode(offset+1+i));
			for (var k=0;k<this.fieldnames.length;++k){
				var fieldval=doc[this.fieldnames[k]];
				cell=row.insertCell(k+1);
				cell.align="center";
				
				cell.appendChild(document.createTextNode(fieldval));
			}
		}
	}
}
