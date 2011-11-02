var typeArray = new Array("TYPE:\"{http://www.alfresco.org/model/content/1.0}folder\" AND NOT TYPE:\"{http://www.alfresco.org/model/site/1.0}site\" AND NOT TYPE:\"{http://www.alfresco.org/model/datalist/1.0}dataList\" AND NOT TYPE:\"{http://www.alfresco.org/model/bpm/1.0}package\" AND NOT TYPE:\"{http://www.alfresco.org/model/content/1.0}systemfolder\" AND NOT TYPE:\"{http:\/\/www.alfresco.org\/model\/forum\/1.0}forum\"", 
  "TYPE:\"{http://www.alfresco.org/model/content/1.0}content\" AND NOT TYPE:\"{http:\/\/www.alfresco.org\/model\/bpm\/1.0}task\" AND NOT TYPE:\"{http://www.alfresco.org/model/datalist/1.0}dataListItem\" AND NOT TYPE:\"{http://www.alfresco.org/model/calendar}calendarEvent\" AND NOT TYPE:\"{http://www.alfresco.org/model/linksmodel/1.0}link\" AND NOT TYPE:\"{http://www.alfresco.org/model/content/1.0}dictionaryModel\"",
  "TYPE:\"{http://www.alfresco.org/model/site/1.0}site\"",
  "TYPE:\"{http://www.alfresco.org/model/datalist/1.0}dataList\"",
  "TYPE:\"{http://www.alfresco.org/model/datalist/1.0}dataListItem\"",
//Wiki
  "TYPE:\"{http://www.alfresco.org/model/bpm/1.0}package\"",
  "TYPE:\"{http:\/\/www.alfresco.org\/model\/bpm\/1.0}task\"",
  "TYPE:\"{http:\/\/www.alfresco.org\/model\/forum\/1.0}forum\"",
  "TYPE:\"{http:\/\/www.alfresco.org\/model\/forum\/1.0}topic\"",
  "TYPE:\"{http:\/\/www.alfresco.org\/model\/forum\/1.0}post\"",
  "TYPE:\"{http:\/\/www.alfresco.org\/model\/linksmodel\/1.0}link\"",
  "TYPE:\"{http:\/\/www.alfresco.org/model/content/1.0}person\"");

var fileString = "folder,document,site,datalist,datalistitem,workflowdef,workflowtask,forum,topic,post,link,person";
//fileString="document";
//typeArray = new Array( "TYPE:\"{http://www.alfresco.org/model/content/1.0}content\" AND NOT TYPE:\"{http:\/\/www.alfresco.org\/model\/bpm\/1.0}task\" AND NOT TYPE:\"{http://www.alfresco.org/model/datalist/1.0}dataListItem\" AND NOT TYPE:\"{http://www.alfresco.org/model/calendar}calendarEvent\" AND NOT TYPE:\"{http://www.alfresco.org/model/linksmodel/1.0}link\" AND NOT TYPE:\"{http://www.alfresco.org/model/content/1.0}dictionaryModel\"");

//fileString="folder";
//typeArray = new Array("TYPE:\"{http://www.alfresco.org/model/content/1.0}folder\" AND NOT TYPE:\"{http://www.alfresco.org/model/site/1.0}site\" AND NOT TYPE:\"{http://www.alfresco.org/model/datalist/1.0}dataList\" AND NOT TYPE:\"{http://www.alfresco.org/model/bpm/1.0}package\" AND NOT TYPE:\"{http://www.alfresco.org/model/content/1.0}systemfolder\" AND NOT TYPE:\"{http:\/\/www.alfresco.org\/model\/forum\/1.0}forum\"");  // exclude fm:forums, fm:forum, fm:topic, fm:post

var fileArray = fileString.split(",");

// this represents the mapping between Alfresco types, and how the ReportinDatabase will store these types.
// execture reporting.logPropertyTypes() (and enable logging) to get a list of all currently registered property types in the models.
var types = new Object; 
types["id"]="BIGINT";
types["text"]="VARCHAR(500)";
types["mltext"]="VARCHAR(500)";
types["datetime"]="DATETIME";
types["date"]="DATETIME";
types["period"]="IGNORE"; // we cannot process this (yet?)
types["boolean"]="BOOLEAN";
types["long"]="BIGINT";
types["double"]="DOUBLE PRECISION";
types["float"]="FLOAT";
types["int"]="INTEGER";
types["locale"]="VARCHAR(100)";
types["path"]="VARCHAR(500)";
types["any"]="VARCHAR(500)";
types["category"]="VARCHAR(300)";
types["taggable"]="VARCHAR(300)";
types["html"]="VARCHAR(500)";
types["content"]="VARCHAR(500)";
types["version"]="INTEGER";
types["qname"]="VARCHAR(100)";
types["noderef"]="VARCHAR(100)";
types["childassocref"]="IGNORE"; // we cannot process this
types["assocref"]="IGNORE"; // we cannot process this
types["uri"]="VARCHAR(500)";

// and 'coded' attributes we can calculate one way or the other:
types["type"]="VARCHAR(100)";
types["mimetype"]="VARCHAR(100)";
types["site"]="VARCHAR(100)";
types["displaypath"]="VARCHAR(500)";
types["categories"]="VARCHAR(200)";
types["taggable"]="VARCHAR(200)";

//@TODO: version,period


//-------------------------------------------------------------
var reportsOutputFolderName = "TestAlfrescoJasperReports";
var reportTemplateName="Data Dictionary/Presentation Templates/reporting/findAllAttributesForType2.ftl";

var storeRefs = "workspace://SpacesStore,archive://SpacesStore".split(","); 
	
var blockKeys=",homeFolder,homeFolderProvider,cm:content,cm:source,cm:description,cm:name,cm:organization,cm:organizationId,"; // properties to ignore (comma separated, no spaces, comma in the end!
var setOfKeys = ",";
var defSeperator = "~|~";
var lineSeperator = "~||~";
var definitionSeperator = "~|enddefinition|~";
var objectSeperator = "~|endobject|~";


//--------------------------------------------------------------------------


function getSiteName(result){
	var returnString = "";
	var path = result.displayPath;
	// /Company Home/Sites/testsite/documentLibrary
//logger.log("## PATH  ="+path);
//logger.log("## LENGTH="+path.length());
//logger.log("## END   ="+path.indexOf("/",21));
	var tmp = path.indexOf("/",21);
	if ((path.indexOf("Company Home/Sites/")>0) && (path.length()>22)){
		if (tmp<0) {tmp=9999;}
		returnString=path.substring(20, Math.min(tmp, path.length()));
	}
//logger.log("## SITE="+ returnString);
	return returnString;
}

//--------------------------------------------------------------------------

function getTagValues(result){
	var returnString = "";
	for (var j=0;j<result.properties["cm:taggable"].length;j++){
		if (returnString.length>0){returnString += ", ";}
		returnString += result.properties["cm:taggable"][j].name;
	}
	return returnString;
}

//--------------------------------------------------------------------------

function getCategoriesValues(result){
	var returnString = "";
	for (var j=0;j<result.properties["cm:categories"].length;j++){
		if (returnString.length>0){returnString += ", ";}
		returnString += result.properties["cm:categories"][j].name;
	}
	return returnString;
}

//--------------------------------------------------------------------------

function getSize(result){
  var returnValue=0;
  if (result.properties.content.size){
    returnValue=result.properties.content.size;
  }
  return returnValue;
}

//--------------------------------------------------------------------------

function getMimeType(result){
  var returnValue="";
  if (result.properties.content.mimetype!=null) {  
    returnValue=result.properties.content.mimetype;
  }
  return returnValue;
}

//--------------------------------------------------------------------------

function processObject(result){
	var returnString="";

	if (result.isDocument && (setOfKeys.indexOf("size,mimetype")<0)){
		returnSetOfKeys+="\nsize"+defSeperator+types["long"];
		returnSetOfKeys+="\nmimetype"+defSeperator+types["mimetype"];
		setOfKeys+="size,mimetype,";
	}
	if ((objType!="cm:person") && (objType!="st:site") && (setOfKeys.indexOf("site,displaypath")<0)){
		returnSetOfKeys+="\nsite"+defSeperator+types["site"];
		returnSetOfKeys+="\ndisplaypath"+defSeperator+types["displaypath"];
		setOfKeys+="site,displaypath,";
	}
	if (setOfKeys.indexOf("closure")<0){
		returnSetOfKeys+="\nclosure"+defSeperator+types["int"];
		setOfKeys+="closure,";
	}
	for (var key in result.properties){
                var shortkey = utils.shortQName(key);
		if ((blockKeys.indexOf(","+shortkey+",")<0) && (result.properties[key]!= null)){
			// @TODO add property cache, only use Java route when needed. don't ask the same twice
			var type = reporting.typeForProperty(key);

			// Make sure the column name and type are captured -if- and only -if- it is a new column
			if (setOfKeys.indexOf(shortkey)<0){
				setOfKeys+=shortkey+",";
				if (returnSetOfKeys.length>0){returnSetOfKeys+="\n";}
				returnSetOfKeys+=shortkey.replace(":","_").replace("-","_")+defSeperator+types[type];
			}
			// build the string to return, containing shortkey, type and value
			if (returnString.length>0){returnString+=lineSeperator;}
			
			var done = false;
			if (type=="category"){
				if (result.properties["cm:categories"]!=null){
					returnString +=lineSeperator+shortkey.replace(":","_").replace("-","_")+defSeperator+types["categories"]+defSeperator + getCategoriesValues(result);
				}
				done=true;
			}
			if (type=="taggable"){
				if (result.properties["cm:taggable"]!=null){
					returnString +=lineSeperator+shortkey.replace(":","_").replace("-","_")+defSeperator+types["taggable"]+defSeperator + getTagValues(result);
				}
				done=true;
			}
			if ((type=="datetime") || (type=="date")){
				var d = new Date(result.properties[key]);
				if (isNaN(d)){
					logger.log("###### NaN for "+ result.properties[key] );

				} else {
					returnString += shortkey.replace(":","_").replace("-","_")+defSeperator+types[type]+defSeperator+d.getTime();
					done=true; 
				}
			}

			if (((type=="text") || (type=="mltext")) && (result.properties[key]!=null)){
				returnString += shortkey.replace(":","_").replace("-","_")+defSeperator+types[type]+defSeperator+result.properties[key];
				done=true; 
			}
			// if none of the above apply, treat it like a String
			if (!done){
				returnString += shortkey.replace(":","_").replace("-","_")+defSeperator+types[type]+defSeperator+result.properties[key];
			}
		}
	}
	// get the short name of the TYPE, we need it to determine if we need to add more prop's 
	var objType = utils.shortQName(result.type);
	returnString +=lineSeperator+"type"+defSeperator+types["type"]+defSeperator + objType; //always
	if ((objType!="cm:person") && (objType!="st:site")){
		returnString +=lineSeperator+"site"+defSeperator+types["site"]+defSeperator+getSiteName(result) ;
		returnString +=lineSeperator+"displaypath"+defSeperator+types["displaypath"]+defSeperator+result.displayPath;
	}
	if (result.isDocument){
		returnString +=lineSeperator+"size"+defSeperator+types["long"]+defSeperator + getSize(result);
		returnString +=lineSeperator+"mimetype"+defSeperator+types["mimetype"]+defSeperator+getMimeType(result);
	}
	returnString +=lineSeperator+"closure"+defSeperator+types["int"]+defSeperator,"0";
	//logger.log("## " + returnString);
//add stuff
	return returnString;
} // end function

//--------------------------------------------------------------------------


reporting.dropTables(fileString);
reporting.createEmptyTables(fileString);

// Search for deletes: archive://SpacesStore &&  {http://www.alfresco.org/model/system/1.0}archivedDate 
// @TODO Automatically manage multi value attributes like Categories and Tags. (Dictionary Service knows about them!)


var reportOutput = companyhome.childByNamePath(reportsOutputFolderName);
if (!reportOutput) reportOutput = companyhome.createFolder(reportsOutputFolderName);

var ftl = companyhome.childByNamePath(reportTemplateName);

for (s=0;s<storeRefs.length;s++){
	for (var i=0; i<fileArray.length; i++){
		var letsGo = true; // allow the initial run
		var dbid="0";    // the initial value of the highest dbid of the current run
		var query="";    // the final query, the cofigured base query expanded with modified-date and node-dbid
		var results;     // array containing the resultset of the query
		var counter = 0; // counts the total number of objects for the given query/table
		var contentTotal = "";
		//returnSetOfKeys="site~|~VARCHAR(100)\ntype~|~VARCHAR(100)"; // reset otherwise all subsequent tables will get the superset of attributes.
		returnSetOfKeys="\ntype"+defSeperator+types["mimetype"];
		//if (fileString[i]=="document") {
		//	returnSetOfKeys += "size~|~BIGINT\ndisplaypath~|~VARCHAR(500)\nmimetype~|~VARCHAR(100)";
		//}

		//var storeRef = "archive://SpacesStore";
		//var storeRef = "workspace://SpacesStore"; 

		var filename = fileArray[i] + ".txt";
		logger.log(storeRefs[s] + " Stuffing " + typeArray[i] + " into " + filename);
		var file = reportOutput.childByNamePath(filename);
		if (file)
		{
			//listTotal=file.content;
		} else {
			file = reportOutput.createFile(filename);
		}

		// process multiple queries, because the max result set is set to 1000 (or bond by time)
		// either way, we do not know how many	queries we need to fire before we capture all objects
	 	// that should be stored in the Reporting Database
		while (letsGo){
			setOfKeys=",";     // reset the unique collection of keys in this particular run
			var content = "";  // reset the string representing all content values for this particular run 
		

			query = typeArray[i] + query;
		 	var content = "";

			logger.log("Query: " + query);
			// execute the actual query
			results = search.luceneSearch(storeRefs[s], query, "@sys:node-dbid", true); 
			for(var j=0;j<results.length;j++){
				var result = results[j];
				var processed = processObject(result)+"\n";
				content += processed;
				counter++;
//logger.log(fileArray[i] + ": " + counter + " (" + content.length+ ") "+ result.name);
			}

			// get the superset of results over multiple runs to potentially store in a Alfresco Document
			contentTotal += content; 

			if (results.length>0) {
				reporting.processRepositoryUpdate(fileArray[i], returnSetOfKeys + "\n" + definitionSeperator + "\n"+ content);

				//reporting.processRepositoryInsert(fileArray[i], content);      
				//reporting.processRepositoryDelete(fileArray[i], content);

				// get the higest dbid found in the current search (next search should start from here)
				dbid = results[results.length-1].properties["sys:node-dbid"]+1;
				// modify the query for the next run
				query =  " AND @sys\\:node-dbid:["+ dbid + " TO MAX]";
				logger.log("Number of objects found: "+counter + " New query: "+query);
			} else { 
				// there is no next search... 
				letsGo=false;
			} // end if results.length
	       } // end while
	       file.content = returnSetOfKeys + "\n" + definitionSeperator + "\n"+ contentTotal;
	       file.save();
	       logger.log(returnSetOfKeys);

	       logger.log("Processed " + counter + " items for table " + fileArray[i]);	
	} // end for tables
} // end for storeRefs


// lend from http://www.mattkruse.com/javascript/date/source.html
// ===================================================================
// Author: Matt Kruse <matt@mattkruse.com>
// WWW: http://www.mattkruse.com/
//
// NOTICE: You may use this code for any purpose, commercial or
// private, without any further permission from the author. You may
// remove this notice from your final code if you wish, however it is
// appreciated by the author if at least my web site address is kept.
//
// You may *NOT* re-distribute this code in any way except through its
// use. That means, you can include it in your product, or your web
// site, or any other form where the code is actually being used. You
// may not put the plain javascript up on your site for download or
// include it in your javascript libraries for download. 
// If you wish to share this code with others, please just point them
// to the URL instead.
// Please DO NOT link directly to my .js files from your site. Copy
// the files to your server and use them there. Thank you.
// ===================================================================

// HISTORY
// ------------------------------------------------------------------
// May 17, 2003: Fixed bug in parseDate() for dates <1970
// March 11, 2003: Added parseDate() function
// March 11, 2003: Added "NNN" formatting option. Doesn't match up
//                 perfectly with SimpleDateFormat formats, but 
//                 backwards-compatability was required.

// ------------------------------------------------------------------
// These functions use the same 'format' strings as the 
// java.text.SimpleDateFormat class, with minor exceptions.
// The format string consists of the following abbreviations:
// 
// Field        | Full Form          | Short Form
// -------------+--------------------+-----------------------
// Year         | yyyy (4 digits)    | yy (2 digits), y (2 or 4 digits)
// Month        | MMM (name or abbr.)| MM (2 digits), M (1 or 2 digits)
//              | NNN (abbr.)        |
// Day of Month | dd (2 digits)      | d (1 or 2 digits)
// Day of Week  | EE (name)          | E (abbr)
// Hour (1-12)  | hh (2 digits)      | h (1 or 2 digits)
// Hour (0-23)  | HH (2 digits)      | H (1 or 2 digits)
// Hour (0-11)  | KK (2 digits)      | K (1 or 2 digits)
// Hour (1-24)  | kk (2 digits)      | k (1 or 2 digits)
// Minute       | mm (2 digits)      | m (1 or 2 digits)
// Second       | ss (2 digits)      | s (1 or 2 digits)
// AM/PM        | a                  |
//
// NOTE THE DIFFERENCE BETWEEN MM and mm! Month=MM, not mm!
// Examples:
//  "MMM d, y" matches: January 01, 2000
//                      Dec 1, 1900
//                      Nov 20, 00
//  "M/d/yy"   matches: 01/20/00
//                      9/2/00
//  "MMM dd, yyyy hh:mm:ssa" matches: "January 01, 2000 12:30:45AM"
// ------------------------------------------------------------------

var MONTH_NAMES=new Array('January','February','March','April','May','June','July','August','September','October','November','December','Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec');
var DAY_NAMES=new Array('Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sun','Mon','Tue','Wed','Thu','Fri','Sat');
function LZ(x) {return(x<0||x>9?"":"0")+x}

// ------------------------------------------------------------------
// isDate ( date_string, format_string )
// Returns true if date string matches format of format string and
// is a valid date. Else returns false.
// It is recommended that you trim whitespace around the value before
// passing it to this function, as whitespace is NOT ignored!
// ------------------------------------------------------------------
function isDate(val,format) {
	var date=getDateFromFormat(val,format);
	if (date==0) { return false; }
	return true;
	}

// -------------------------------------------------------------------
// compareDates(date1,date1format,date2,date2format)
//   Compare two date strings to see which is greater.
//   Returns:
//   1 if date1 is greater than date2
//   0 if date2 is greater than date1 of if they are the same
//  -1 if either of the dates is in an invalid format
// -------------------------------------------------------------------
function compareDates(date1,dateformat1,date2,dateformat2) {
	var d1=getDateFromFormat(date1,dateformat1);
	var d2=getDateFromFormat(date2,dateformat2);
	if (d1==0 || d2==0) {
		return -1;
		}
	else if (d1 > d2) {
		return 1;
		}
	return 0;
	}

// ------------------------------------------------------------------
// formatDate (date_object, format)
// Returns a date in the output format specified.
// The format string uses the same abbreviations as in getDateFromFormat()
// ------------------------------------------------------------------
function formatDate(date,format) {
	format=format+"";
	var result="";
	var i_format=0;
	var c="";
	var token="";
	var y=date.getYear()+"";
	var M=date.getMonth()+1;
	var d=date.getDate();
	var E=date.getDay();
	var H=date.getHours();
	var m=date.getMinutes();
	var s=date.getSeconds();
	var yyyy,yy,MMM,MM,dd,hh,h,mm,ss,ampm,HH,H,KK,K,kk,k;
	// Convert real date parts into formatted versions
	var value=new Object();
	if (y.length < 4) {y=""+(y-0+1900);}
	value["y"]=""+y;
	value["yyyy"]=y;
	value["yy"]=y.substring(2,4);
	value["M"]=M;
	value["MM"]=LZ(M);
	value["MMM"]=MONTH_NAMES[M-1];
	value["NNN"]=MONTH_NAMES[M+11];
	value["d"]=d;
	value["dd"]=LZ(d);
	value["E"]=DAY_NAMES[E+7];
	value["EE"]=DAY_NAMES[E];
	value["H"]=H;
	value["HH"]=LZ(H);
	if (H==0){value["h"]=12;}
	else if (H>12){value["h"]=H-12;}
	else {value["h"]=H;}
	value["hh"]=LZ(value["h"]);
	if (H>11){value["K"]=H-12;} else {value["K"]=H;}
	value["k"]=H+1;
	value["KK"]=LZ(value["K"]);
	value["kk"]=LZ(value["k"]);
	if (H > 11) { value["a"]="PM"; }
	else { value["a"]="AM"; }
	value["m"]=m;
	value["mm"]=LZ(m);
	value["s"]=s;
	value["ss"]=LZ(s);
	while (i_format < format.length) {
		c=format.charAt(i_format);
		token="";
		while ((format.charAt(i_format)==c) && (i_format < format.length)) {
			token += format.charAt(i_format++);
			}
		if (value[token] != null) { result=result + value[token]; }
		else { result=result + token; }
		}
	return result;
	}
	
// ------------------------------------------------------------------
// Utility functions for parsing in getDateFromFormat()
// ------------------------------------------------------------------
function _isInteger(val) {
	var digits="1234567890";
	for (var i=0; i < val.length; i++) {
		if (digits.indexOf(val.charAt(i))==-1) { return false; }
		}
	return true;
	}
function _getInt(str,i,minlength,maxlength) {
	for (var x=maxlength; x>=minlength; x--) {
		var token=str.substring(i,i+x);
		if (token.length < minlength) { return null; }
		if (_isInteger(token)) { return token; }
		}
	return null;
	}
	
// ------------------------------------------------------------------
// getDateFromFormat( date_string , format_string )
//
// This function takes a date string and a format string. It matches
// If the date string matches the format string, it returns the 
// getTime() of the date. If it does not match, it returns 0.
// ------------------------------------------------------------------
function getDateFromFormat(val,format) {
	val=val+"";
	format=format+"";
	var i_val=0;
	var i_format=0;
	var c="";
	var token="";
	var token2="";
	var x,y;
	var now=new Date();
	var year=now.getYear();
	var month=now.getMonth()+1;
	var date=1;
	var hh=now.getHours();
	var mm=now.getMinutes();
	var ss=now.getSeconds();
	var ampm="";
	
	while (i_format < format.length) {
		// Get next token from format string
		c=format.charAt(i_format);
		token="";
		while ((format.charAt(i_format)==c) && (i_format < format.length)) {
			token += format.charAt(i_format++);
			}
		// Extract contents of value based on format token
		if (token=="yyyy" || token=="yy" || token=="y") {
			if (token=="yyyy") { x=4;y=4; }
			if (token=="yy")   { x=2;y=2; }
			if (token=="y")    { x=2;y=4; }
			year=_getInt(val,i_val,x,y);
			if (year==null) { return 0; }
			i_val += year.length;
			if (year.length==2) {
				if (year > 70) { year=1900+(year-0); }
				else { year=2000+(year-0); }
				}
			}
		else if (token=="MMM"||token=="NNN"){
			month=0;
			for (var i=0; i<MONTH_NAMES.length; i++) {
				var month_name=MONTH_NAMES[i];
				if (val.substring(i_val,i_val+month_name.length).toLowerCase()==month_name.toLowerCase()) {
					if (token=="MMM"||(token=="NNN"&&i>11)) {
						month=i+1;
						if (month>12) { month -= 12; }
						i_val += month_name.length;
						break;
						}
					}
				}
			if ((month < 1)||(month>12)){return 0;}
			}
		else if (token=="EE"||token=="E"){
			for (var i=0; i<DAY_NAMES.length; i++) {
				var day_name=DAY_NAMES[i];
				if (val.substring(i_val,i_val+day_name.length).toLowerCase()==day_name.toLowerCase()) {
					i_val += day_name.length;
					break;
					}
				}
			}
		else if (token=="MM"||token=="M") {
			month=_getInt(val,i_val,token.length,2);
			if(month==null||(month<1)||(month>12)){return 0;}
			i_val+=month.length;}
		else if (token=="dd"||token=="d") {
			date=_getInt(val,i_val,token.length,2);
			if(date==null||(date<1)||(date>31)){return 0;}
			i_val+=date.length;}
		else if (token=="hh"||token=="h") {
			hh=_getInt(val,i_val,token.length,2);
			if(hh==null||(hh<1)||(hh>12)){return 0;}
			i_val+=hh.length;}
		else if (token=="HH"||token=="H") {
			hh=_getInt(val,i_val,token.length,2);
			if(hh==null||(hh<0)||(hh>23)){return 0;}
			i_val+=hh.length;}
		else if (token=="KK"||token=="K") {
			hh=_getInt(val,i_val,token.length,2);
			if(hh==null||(hh<0)||(hh>11)){return 0;}
			i_val+=hh.length;}
		else if (token=="kk"||token=="k") {
			hh=_getInt(val,i_val,token.length,2);
			if(hh==null||(hh<1)||(hh>24)){return 0;}
			i_val+=hh.length;hh--;}
		else if (token=="mm"||token=="m") {
			mm=_getInt(val,i_val,token.length,2);
			if(mm==null||(mm<0)||(mm>59)){return 0;}
			i_val+=mm.length;}
		else if (token=="ss"||token=="s") {
			ss=_getInt(val,i_val,token.length,2);
			if(ss==null||(ss<0)||(ss>59)){return 0;}
			i_val+=ss.length;}
		else if (token=="a") {
			if (val.substring(i_val,i_val+2).toLowerCase()=="am") {ampm="AM";}
			else if (val.substring(i_val,i_val+2).toLowerCase()=="pm") {ampm="PM";}
			else {return 0;}
			i_val+=2;}
		else {
			if (val.substring(i_val,i_val+token.length)!=token) {return 0;}
			else {i_val+=token.length;}
			}
		}
	// If there are any trailing characters left in the value, it doesn't match
	if (i_val != val.length) { return 0; }
	// Is date valid for month?
	if (month==2) {
		// Check for leap year
		if ( ( (year%4==0)&&(year%100 != 0) ) || (year%400==0) ) { // leap year
			if (date > 29){ return 0; }
			}
		else { if (date > 28) { return 0; } }
		}
	if ((month==4)||(month==6)||(month==9)||(month==11)) {
		if (date > 30) { return 0; }
		}
	// Correct hours value
	if (hh<12 && ampm=="PM") { hh=hh-0+12; }
	else if (hh>11 && ampm=="AM") { hh-=12; }
	var newdate=new Date(year,month-1,date,hh,mm,ss);
	return newdate.getTime();
	}

// ------------------------------------------------------------------
// parseDate( date_string [, prefer_euro_format] )
//
// This function takes a date string and tries to match it to a
// number of possible date formats to get the value. It will try to
// match against the following international formats, in this order:
// y-M-d   MMM d, y   MMM d,y   y-MMM-d   d-MMM-y  MMM d
// M/d/y   M-d-y      M.d.y     MMM-d     M/d      M-d
// d/M/y   d-M-y      d.M.y     d-MMM     d/M      d-M
// A second argument may be passed to instruct the method to search
// for formats like d/M/y (european format) before M/d/y (American).
// Returns a Date object or null if no patterns match.
// ------------------------------------------------------------------
function parseDate(val) {
	var preferEuro=(arguments.length==2)?arguments[1]:false;
	generalFormats=new Array('y-M-d','MMM d, y','MMM d,y','y-MMM-d','d-MMM-y','MMM d');
	monthFirst=new Array('M/d/y','M-d-y','M.d.y','MMM-d','M/d','M-d');
	dateFirst =new Array('d/M/y','d-M-y','d.M.y','d-MMM','d/M','d-M');
	var checkList=new Array('generalFormats',preferEuro?'dateFirst':'monthFirst',preferEuro?'monthFirst':'dateFirst');
	var d=null;
	for (var i=0; i<checkList.length; i++) {
		var l=window[checkList[i]];
		for (var j=0; j<l.length; j++) {
			d=getDateFromFormat(val,l[j]);
			if (d!=0) { return new Date(d); }
			}
		}
	return null;
	}

