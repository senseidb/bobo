

function createRequestObject(){
	var req;
	if (window.XMLHttpRequest) {
        req = new XMLHttpRequest();
       
    // branch for IE/Windows ActiveX version
    } else if (window.ActiveXObject) {
        req = new ActiveXObject("Microsoft.XMLHTTP");
    }    
	return req;
}

function JSONRPC(url){
	this.url=url;
	this.sndReq=function(action,request,callback,isXML) {	
		var reqser=request.toHttpGetString();
		if (!isXML){
			reqser=reqser+"&output=json";
		}
		//var path=url+action+"/";
			
		var path=url+reqser;	
		var http=createRequestObject();
		http.onreadystatechange = function(){
			if(http.readyState == 4){
	        	var response;
	        	if (isXML){
	        		response=http.responseXML;
	        	}
	        	else{
	        		response = http.responseText;
	        	}
				callback(response);
			}
		}
		
		try{
			http.open("GET", path,true);
			//http.send(reqser);
			http.send("");
		}
		catch(e){
			alert(e);
		}
		/*
		try{
		    http.open("POST", path,true);
		    http.setRequestHeader('Content-Type','application/x-www-form-urlencoded;charset=UTF-8');
		    
			var params="proto=browsejson&"+"reqstring="+reqser;			
			http.send(params);
		}
		catch(e){
			alert(e);
		}*/
	}
}

function BoboAPI(url){
	this.transport=new JSONRPC(url);
	this.browse=function(browsereq,callback){
		this.transport.sndReq("browse",browsereq,callback,false);
	}
}
