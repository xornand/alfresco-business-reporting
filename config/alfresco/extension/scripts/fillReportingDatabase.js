<import resource="/Company Home/Data Dictionary/Scripts/reporting/alfresco-business-reporting.lib.js">
// make sure we always include the reporting library 

var defSeperator = "~|~";
var lineSeperator = "~||~";
var definitionSeperator = "~|enddefinition|~";
var objectSeperator = "~|endobject|~";

//-------------------------------------------------------------
// the name of the file, carries of the date the last report has run
var dateDocumentName = "lastReportingDate";

var storeRefs = "workspace://SpacesStore,archive://SpacesStore".split(","); 
    
// properties to ignore (comma separated, no spaces, comma in the end!)
var blockKeys=",homeFolder,homeFolderProvider,cm:content,cm:source,cm:organization,cm:organizationId,"; 
var setOfKeys = ",";
var defSeperator = "~|~";
var lineSeperator = "~||~";
var definitionSeperator = "~|enddefinition|~";
var objectSeperator = "~|endobject|~";

var dateQuery = "";  //  this contains the query term to find objects with a modified date after dd/MM/yyyy hh:mm:ss

// We need companyhome, especially if we run from classpath instead of DataDictionary/Repository
var companyhome = search.luceneSearch("PATH:\"/app:company_home\"")[0];

//--------------------------------------------------------------------------


function getSiteName(result){
    var returnString = "";
    var path = result.displayPath;
    // /Company Home/Sites/testsite/documentLibrary
    var tmp = path.indexOf("/",21);
    if ((path.indexOf("Company Home/Sites/")>0) && (path.length()>22)){
        if (tmp<0) {tmp=9999;}
        returnString=path.substring(20, Math.min(tmp, path.length()));
    }
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
    if (result.mimetype!=null) {  
        returnValue=result.mimetype;
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

    for (var key in result.properties){
        var shortkey = utils.shortQName(key);
        if ( (key!=null) && (blockKeys.indexOf(","+shortkey+",")<0) && (result.properties[key]!= null)){
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
        } // if (key!=null etc...
    } // key in result.properties

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

    return returnString+lineSeperator;

} // end function

//--------------------------------------------------------------------------

// reportOutput contains the Space where the file named dateDocumentName exists. Or not.
function getQueryDate(reportOutput, dateDocumentName){
    logger.log("Startng getQueryDate");
    var returnQuery = "";
    var dateDocument=reportOutput.childByNamePath(dateDocumentName);
    if ((dateDocument!=null) && (dateDocument.properties.title!=null) && (dateDocument.properties.title!="")){
        // @TODO @cm\:modified:["2006-07-20T00:00:00" to NOW]   
        returnQuery = " AND @cm\\:modified:[" +dateDocument.properties.title+ " TO NOW]";
    }
    logger.log("getQueryDate returning: " + returnQuery);
    return returnQuery;
}

//--------------------------------------------------------------------------

function stringLength(input){
    var myInput = new String(input);
    if (myInput.length<2){
        myInput = "0" + myInput;
    }
    return myInput;
}

//--------------------------------------------------------------------------


function setQueryDate(reportOutput, dateDocumentName, timestamp){
  var dateDocument=reportOutput.childByNamePath(dateDocumentName); 
  logger.log("2Found " + dateDocumentName);
  if (!dateDocument) {
    dateDocument = reportOutput.createFile(dateDocumentName);
    dateDocument.content="This page is intentionally left blank.";
    dateDocument.save();
    logger.log("Created " + dateDocument.name + " | " + dateDocument.properties["cm:title"]);
  }
  if (timestampsAreDateOnly){
      // store date only. Replace time by 00:00:00 in order to find all documents created within this day
	  dateDocument.properties["cm:title"]= timestamp.getFullYear() + "-" 
	            + stringLength(timestamp.getMonth()+1) + "-" 
	            + stringLength(timestamp.getDate()) + "T00:00:00";
  } else {
	  dateDocument.properties["cm:title"]= timestamp.getFullYear() + "-" 
	            + stringLength(timestamp.getMonth()+1) + "-" 
	            + stringLength(timestamp.getDate()) + "T" 
	            + stringLength(timestamp.getHours()) + ":" 
	            + stringLength(timestamp.getMinutes()) + ":"
	            + stringLength(timestamp.getSeconds());
  }
  dateDocument.save();
  
}

//--------------------------------------------------------------------------
//                                  M A I N
//--------------------------------------------------------------------------

function main(){
  for (s=0;s<storeRefs.length;s++){

    for (var table in queries) {    

        var letsGo = true; // allow the initial run
        var dbid="0";      // the initial value of the highest dbid of the current run
        var query="";      // the final query, the cofigured base query expanded with modified-date and node-dbid
        var results;       // array containing the resultset of the query
        var counter = 0;   // counts the total number of objects for the given query/table
        var contentTotal = "";
        returnSetOfKeys="\ntype"+defSeperator+types["mimetype"];

        var filename = table + "-" + storeRefs[s] + ".txt";

        logger.log(storeRefs[s] + " Stuffing " + queries[table] + " into " + filename);

        if (debug){
            var file = reportOutput.childByNamePath(filename);
            if (!file)
            {
                file = reportOutput.createFile(filename);
            }
        } // end if debug

        // process multiple queries, because the max result set is set to 1000 (=default; or bound by time)
        // either way, we do not know how many    queries we need to fire before we capture all objects
        // that should be stored in the Reporting Database
        while (letsGo){
            setOfKeys=",";     // reset the unique collection of keys in this particular run
            var content = "";  // reset the string representing all content values for this particular run 
        
            //query = queries[table] + query + " AND NOT @sys:node-uuid=NULL " +  dateQuery;
            query = queries[table] + query + dateQuery;
            var content = "";

            logger.log("Query: " + query);
            // execute the actual query
            results = search.luceneSearch(storeRefs[s], query, "@sys:node-dbid", true); 
            if (results) { 
                for (var j=0;j<results.length;j++) {
                    var result = results[j];
                    if (result) {
                        try{
                            //var tst=result.properties["sys:node-uuid"].length(); // if we can touch, it is not null-ish
                            var processed = processObject(result)+"\n";
                            content += processed;
                            counter++;
                        } catch (exception) {
                            //object appears null-ish
                        }
                    } // end if(result)
                } // end for
            } // end if results
            // get the superset of results over multiple runs to potentially store in a Alfresco Document
            contentTotal += content; 

            if ((results) && (results.length>0)) {
                reporting.processRepositoryUpdate(table, returnSetOfKeys + "\n" + definitionSeperator + "\n"+ content, false);

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
        if (debug){
          file.content = returnSetOfKeys + "\n" + definitionSeperator + "\n"+ contentTotal;
              file.save();
        }
            logger.log(returnSetOfKeys);
        logger.log("Processed " + counter + " items for table " + table);    
    
    } // end for tables
  } // end for storeRefs

} // end function main

//--------------------------------------------------------------------------
//--------------------------------------------------------------------------
//--------------------------------------------------------------------------
//--------------------------------------------------------------------------

// Dangerous; this will kill your reporting content!!!
//reporting.dropTables(fileString);
//reporting.createEmptyTables(fileString);


var reportOutput = companyhome.childByNamePath(reportsLogFolderName);
if (!reportOutput) {
  reportOutput = companyhome.createFolder(reportsLogFolderName);
  reportOutput.save();
}

dateQuery = getQueryDate(reportOutput, dateDocumentName); // this contains the date of the last succesfull run
logger.log("@@ new dateQuery=" + dateQuery);
var thisTimestamp = new Date(); // we need to set this date if the run to fill the reporting DB was succesfull

main();
logger.log("@@ DONE WITH MAIN, finishing up");
setQueryDate(reportOutput, dateDocumentName, thisTimestamp); // persist the date of this succesfull run