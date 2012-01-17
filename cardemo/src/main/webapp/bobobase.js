var widgets=new Array();

var resultslist=null;
var numPerPage=10;
var numHits=0;

var request=new BrowseRequest(numPerPage);

function isNumeric(sText){
   var ValidChars = "0123456789.";
   var IsNumber=true;
   var Char;

 
   for (i = 0; i < sText.length && IsNumber == true; i++) 
      { 
      Char = sText.charAt(i); 
      if (ValidChars.indexOf(Char) == -1) 
         {
         IsNumber = false;
         }
      }
   return IsNumber;   
}

function format(val,count){
	var parts=val.split("|");
	
	//for (var i=0;i<parts.length;++i){
//		parts[i]=Number(parts[i]);
	//}
	
	if (parts.length==2){
		if (parts[0]==parts[1]){
			return parts[0]+" ("+count+")";
		}
		else{
			return parts[0]+" - "+parts[1]+" ("+count+")";
		}
	}
	else{
		if (parts[0]==null || parts[0].length==0){
			return parts[1]+" ("+count+")";
		}
		else{
			return parts[0]+" ("+count+")";
		}
	}
}

function formatPrice(val,count){
	var parts=val.split("|");
	
	for (var i=0;i<parts.length;++i){
		parts[i]=Number(parts[i]);
	}
	if (parts.length==2){
		if (parts[0]==parts[1]){
			return "$"+parts[0]+" ("+count+")";
		}
		else{
			return "$"+parts[0]+" - $"+parts[1]+" ("+count+")";
		}
	}
	else{
		if (parts[0]==null || parts[0].length==0){
			return "< $"+parts[1]+" ("+count+")";
		}
		else{
			return "> $"+parts[0]+" ("+count+")";
		}
	}
}


function updateSearchStat(browseResponse){
	numHits=browseResponse.hitCount;
	var hitStatElem=document.getElementById("hitcount");
	var hitstat=numHits + " out of " + browseResponse.totalDocs + " ("+browseResponse.time+" seconds)";
	hitStatElem.innerHTML=hitstat;
}


function loadBody(){
	api.browse(request,handleResponse);
}

function reset(){
	request.reset();
	api.browse(request,handleResponse);
	var search=document.getElementById("search");
	search.value="";
}

function handleSelection(field,value){

	var selection=request.selections[field];
	selection.clear();
	if (value!=null && value.length>0){
		selection.addValue(value);
	}
	request.offset=0;
	api.browse(request,handleResponse);
}

function handleResultListChange(response){
	var response=JSON.parse(response);
	resultslist.update(response.hits,request.offset);
	updateSearchStat(response);
}

function handleSortChange(sortBy){
	request.toggleSort(sortBy);
	api.browse(request,handleResultListChange);
}


function handleSearch(){
	var elem=document.getElementById("search");
	request.queryString=elem.value;
	api.browse(request,handleResponse);	
}

function handlePaging(action){
	var numPages=parseInt(numHits/numPerPage);
	var remainder=parseInt(numHits%numPerPage);
	if (remainder==0 && numPages>0) numPages--;

	var whichPage=parseInt(request.offset/numPerPage);
	
	if (action=="top") {
		if (request.offset==0) return;
		request.offset=0;
	}
	else if (action=="up") {
		if (request.offset==0) return;
			request.offset-=numPerPage;
	}
	else if (action=="down"){
		if (whichPage<numPages)
			request.offset+=numPerPage;
	}
	else if (action=="bottom"){
		if (whichPage<numPages)
			request.offset=numPages*numPerPage;
	}
	else return;
	
	api.browse(request,handleResultListChange);
}

function handleRemoveTag(field,tag){

        var tagSel=request.selections[field];
		var localHash=toHash(tagSel.values);
		delete localHash[tag];
		tagSel.values=toArray(localHash);		
		api.browse(request,handleResponse);
}

function tagselected(field,tag){
//alert(tag);
    var tagSel=request.selections[field];
	var localHash=toHash(tagSel.values);
	localHash[tag]=tag;
	tagSel.values=toArray(localHash);
	api.browse(request,handleResponse);
}

function handleResponse(response) {
	// populate stats

	    var browseResponse=JSON.parse(response);    
	
	    updateSearchStat(browseResponse);        
	    
	    //populate widgets
	   	var choices=browseResponse.choices;
	
	    for (var i=0;i<widgets.length;++i){
			widgets[i].update(request.selections,choices);		
	    }
	    resultslist.update(browseResponse.hits,request.offset);
}

