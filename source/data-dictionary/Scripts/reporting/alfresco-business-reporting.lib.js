var queries = new Object;
// key = tablename, value=Lucene query
queries["folder"]  	="TYPE:\"{http://www.alfresco.org/model/content/1.0}folder\" AND NOT TYPE:\"{http://www.alfresco.org/model/site/1.0}site\" AND NOT TYPE:\"{http://www.alfresco.org/model/datalist/1.0}dataList\" AND NOT TYPE:\"{http://www.alfresco.org/model/bpm/1.0}package\" AND NOT TYPE:\"{http://www.alfresco.org/model/content/1.0}systemfolder\" AND NOT TYPE:\"{http:\/\/www.alfresco.org\/model\/forum\/1.0}forum\"";
queries["document"]	="TYPE:\"{http://www.alfresco.org/model/content/1.0}content\" AND NOT TYPE:\"{http:\/\/www.alfresco.org\/model\/bpm\/1.0}task\" AND NOT TYPE:\"{http://www.alfresco.org/model/datalist/1.0}dataListItem\" AND NOT TYPE:\"{http://www.alfresco.org/model/calendar}calendarEvent\" AND NOT TYPE:\"{http://www.alfresco.org/model/linksmodel/1.0}link\" AND NOT TYPE:\"{http://www.alfresco.org/model/content/1.0}dictionaryModel\"";
queries["site"]    	="TYPE:\"{http://www.alfresco.org/model/site/1.0}site\"";
queries["datalist"]	="TYPE:\"{http://www.alfresco.org/model/datalist/1.0}dataList\"";
queries["datalistitem" ]="TYPE:\"{http://www.alfresco.org/model/datalist/1.0}dataListItem\"";
queries["workflowdef"]  ="TYPE:\"{http://www.alfresco.org/model/bpm/1.0}package\"";
queries["workflowtask"] ="TYPE:\"{http:\/\/www.alfresco.org\/model\/bpm\/1.0}task\"";
queries["forum"] 	="TYPE:\"{http:\/\/www.alfresco.org\/model\/forum\/1.0}forum\"";
queries["topic"] 	="TYPE:\"{http:\/\/www.alfresco.org\/model\/forum\/1.0}topic\"";
queries["post"]  	="TYPE:\"{http:\/\/www.alfresco.org\/model\/forum\/1.0}post\"";
queries["link"]  	="TYPE:\"{http:\/\/www.alfresco.org\/model\/linksmodel\/1.0}link\"";
queries["person"]	="TYPE:\"cm:person\"";
//@TODO: Wiki


var reportsInputFolderName  = "AlfrescoBusinessReporting/input";   // folder where the report definitions are located
var reportsOutputFolderName = "AlfrescoBusinessReporting/output";  // folder where the resulting PDF reports should be placed
var reportsLogFolderName = "AlfrescoBusinessReporting/log";  // folder where the resulting PDF reports should be placed

var ext = "pdf"; // pick from pdf, xls, doc
var doCheckout = false; // this is not tested yet!
var checkinMessage = "Checked in by automatic report generation";
var timestampsAreDateOnly=true; // the Alfresco default. Change settings if Lucene stores full time too.
var debug = false; // toggle to write intermediate file formats into the repository or not. (Will be placed in folder in var reportsLogFolderName)


// FileString is used to create or drop all tables at once
var fileString = "";
for (var table in queries){
  if (fileString!="") { fileString +=",";}
  fileString +=table;
} 

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
types["username"]="VARCHAR(100)";
types["role"]="VARCHAR(100)";

//@TODO: period, multi-value properties



