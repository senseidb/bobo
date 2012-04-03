var url="/browse?";
var sortnames=new Array("color","category","price","year","mileage");
var api=new BoboAPI(url);


function app_setup(){	
	var priceSection=document.getElementById("price");
	var priceWidget=new SelectList(priceSection,handleSelection);
	priceWidget.displayFn=format;
	widgets.unshift(priceWidget);
		
	var yearSection=document.getElementById("year");	
	var yearWidget=new SelectList(yearSection,handleSelection);
	yearWidget.displayFn=format;
	widgets.unshift(yearWidget);
	
	var mileageSection=document.getElementById("mileage");	
	var mileageWidget=new SelectList(mileageSection,handleSelection);
	mileageWidget.displayFn=format;
	widgets.unshift(mileageWidget);
	
	var colorSection=document.getElementById("color");
//	widgets.unshift(new SelectBox(colorSection,handleSelection));
widgets.unshift(new SelectList(colorSection,handleSelection));
			
	var categorySection=document.getElementById("category");
	widgets.unshift(new SelectList(categorySection,handleSelection));		
		
	var makeModelSection=document.getElementById("makemodel");	
	
	var citySection=document.getElementById("city");	

	widgets.unshift(new SelectPath(citySection,handleSelection,"/",4,5));
	widgets.unshift(new SelectPath(makeModelSection,handleSelection,"/",4,5));
		
	
	var tagSection=document.getElementById("tags");
	widgets.unshift(new TagCloud(tagSection,tagselected));
			
	var colorSelection=new Selection(Selection_Type_Simple);
	request.selections["color"]=colorSelection;
	var categorySelection=new Selection(Selection_Type_Simple);
	request.selections["category"]=categorySelection;
	var priceSelection=new Selection(Selection_Type_Range);
	request.selections["price"]=priceSelection;
	var yearSelection=new Selection(Selection_Type_Range);
	request.selections["year"]=yearSelection;
	var mileageSelection=new Selection(Selection_Type_Range);
	request.selections["mileage"]=mileageSelection;
	var modelSelection=new Selection(Selection_Type_Path);
	request.selections["makemodel"]=modelSelection;
	var citySelection=new Selection(Selection_Type_Path);
	request.selections["city"]=citySelection;
	
	var tagSelection=new Selection(Selection_Type_Simple);
	tagSelection.operation=Value_Operation_AND;
	request.selections["tags"]=tagSelection;
	
	var colorOutputSpec=new OutputSpec();	
	colorOutputSpec.expandSelection=true;
	
	request.ospecs["color"]=colorOutputSpec;
	
	var categoryOutputSpec=new OutputSpec();
	categoryOutputSpec.expandSelection=true;
	
	var modelOutputSpec=new OutputSpec();	
	modelOutputSpec.depth=1;
	
	var cityOutputSpec=new OutputSpec();	
	cityOutputSpec.depth=1;
	
	var rangeOutputSpec=new OutputSpec();	
	rangeOutputSpec.max=5;
	
	request.ospecs["category"]=categoryOutputSpec;
	request.ospecs["makemodel"]=modelOutputSpec;	
	request.ospecs["city"]=cityOutputSpec;	
	request.ospecs["price"]=rangeOutputSpec;	
	request.ospecs["year"]=rangeOutputSpec;	
	request.ospecs["mileage"]=rangeOutputSpec;	
	
	
	var tagOutputSpec=new OutputSpec();
	tagOutputSpec.order=OutputSpec_Order_Hits;
	tagOutputSpec.max=10;

	request.ospecs["tags"]=tagOutputSpec;
	resultslist=new ResultsTable(document.getElementById("resultset"),sortnames);
}

