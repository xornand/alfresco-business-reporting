<import resource="/Company Home/Data Dictionary/Scripts/reporting/alfresco-business-reporting.lib.js">

// We need companyhome, especially if we run from classpath instead of DataDictionary/Repository
var companyhome = search.luceneSearch("PATH:\"/app:company_home\"")[0];

// ----------- no mutation below this line ---------------------------------------

var reportInputFolder  = companyhome.childByNamePath( reportsInputFolderName );  // get the actual InputFolder
var reportOutputFolder = companyhome.childByNamePath( reportsOutputFolderName ); // get the actual OutputFolder
var isCheckedOut = false;

logger.log("Got InputFolder:  " + reportInputFolder.displayPath);
logger.log("Got OutputFolder:  " + reportOutputFolder.displayPath);

// -------------------------------------------------------------------------------

function isReportingDefinition(report){
  // return true if the file has an extension known to belong to JasperReports or Pentaho. Otherwise, return false
  var returnValue=false;

  if (report.isDocument){
    var theName = report.properties["cm:name"];
    var ext = theName.substring(theName.lastIndexOf(".")+1, theName.length).toLowerCase();
    if (".jrxml .jasper .prpt".indexOf(ext)>-1){
      returnValue=true;
    }
  } // end if isDocument

  return returnValue;
} // end function

// -------------------------------------------------------------------------------

if (reporting.isEnabled()){
	// Loop all child-documents, and execute the report (it will not execute at all if it is no report)
	for (var i in reportInputFolder.children) {
	  var report = reportInputFolder.children[i];
	
	  if ( isReportingDefinition(report) ){
	    
	    var theName = report.properties["cm:name"];
	    theName = theName.substring(0, theName.lastIndexOf("."));
	    var output = reportOutputFolder.childByNamePath(theName+"."+ext);
	    if (output==null){
	       output=reportOutputFolder.createFile(theName+"."+ext);
	    } // end output==null
	    else { 
	      if (doCheckout){
	        if (!output.hasAspect("cm:versionable")){
	          output.addAspect("cm:versionable");
		}
	        output = output.checkout();
	        isCheckedOut = true;
	      } // end doCheckout
	    } // end if/else
	
	    logger.log("Preparing to exec: ");
	    logger.log(" -From: " + report.properties["cm:name"] + " ("+ report.nodeRef +")");
	    logger.log(" -To:   " + output.properties["cm:name"] + " (" + output.nodeRef+")");
	
	    reporting.processReport(report.nodeRef, output.nodeRef, ext);
	    if (isCheckedOut && doCheckout) {
	      output = output.checkin( checkinMessage );
	    }
	    logger.log("Done generating " + output.properties["cm:name"]);
	  } //end if report.isDocument 
	}
} // end if isEnabled


