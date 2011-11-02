var typeArray = new Array("TYPE:\"{http://www.alfresco.org/model/content/1.0}folder\" AND NOT TYPE:\"{http://www.alfresco.org/model/site/1.0}site\" AND NOT TYPE:\"{http://www.alfresco.org/model/datalist/1.0}dataList\" AND NOT TYPE:\"{http://www.alfresco.org/model/bpm/1.0}package\" AND NOT TYPE:\"{http://www.alfresco.org/model/content/1.0}systemfolder\" AND NOT TYPE:\"{http:\/\/www.alfresco.org\/model\/forum\/1.0}forum\"",  // exclude fm:forums, fm:forum, fm:topic, fm:post
  "TYPE:\"{http://www.alfresco.org/model/content/1.0}content\" AND NOT TYPE:\"{http:\/\/www.alfresco.org\/model\/bpm\/1.0}task\" AND NOT TYPE:\"{http://www.alfresco.org/model/datalist/1.0}dataListItem\" AND NOT TYPE:\"{http://www.alfresco.org/model/calendar}calendarEvent\" AND NOT TYPE:\"{http://www.alfresco.org/model/linksmodel/1.0}link\"",
  "TYPE:\"{http://www.alfresco.org/model/site/1.0}site\"",
  "TYPE:\"{http://www.alfresco.org/model/datalist/1.0}dataList\"",
  "TYPE:\"{http://www.alfresco.org/model/datalist/1.0}dataListItem\"",
//Wiki
  "TYPE:\"{http://www.alfresco.org/model/bpm/1.0}package\"",
  "TYPE:\"{http:\/\/www.alfresco.org\/model\/bpm\/1.0}task\"",
  "TYPE:\"{http:\/\/www.alfresco.org\/model\/forum\/1.0}forum\"",
  "TYPE:\"{http:\/\/www.alfresco.org\/model\/forum\/1.0}topic\"",
  "TYPE:\"{http:\/\/www.alfresco.org\/model\/forum\/1.0}post\"",
  "TYPE:\"{http:\/\/www.alfresco.org\/model\/linksmodel\/1.0\"",
  "TYPE:\"{http:\/\/www.alfresco.org/model/content/1.0}person\"");

typeArray = new Array( "TYPE:\"{http://www.alfresco.org/model/content/1.0}content\" AND NOT TYPE:\"{http:\/\/www.alfresco.org\/model\/bpm\/1.0}task\" AND NOT TYPE:\"{http://www.alfresco.org/model/datalist/1.0}dataListItem\" AND NOT TYPE:\"{http://www.alfresco.org/model/calendar}calendarEvent\" AND NOT TYPE:\"{http://www.alfresco.org/model/linksmodel/1.0}link\"");

var fileString = "folder,document,site,datalist,datalistitem,workflowdef,workflowtask,forum,topic,post,link,person";
fileString="document";
var fileArray = fileString.split(",");
//logger.log("Found " +typeArray.length + " entries");

//-------------------------------------------------------------
reporting.dropTables(fileString);
reporting.createEmptyTables(fileString);

var reportsOutputFolderName = "TestAlfrescoJasperReports";
var reportTemplateName="Data Dictionary/Presentation Templates/reporting/findAllAttributesForType.ftl";
	
// properties to ignore (comma separated, no spaces, comma in the end!
var blockKeys=",homeFolder,homeFolderProvider,";

var reportOutput = companyhome.childByNamePath(reportsOutputFolderName);
if (!reportOutput) reportOutput = companyhome.createFolder(reportsOutputFolderName);

var ftl = companyhome.childByNamePath(reportTemplateName);

  for (var i=0; i<fileArray.length; i++){
        var filename = fileArray[i] + ".txt";
        logger.log("Stuffing " + typeArray[i] + " into " + filename);
        var file = reportOutput.childByNamePath(filename);
	if (file)
	{
		//personTotal=file.content;
	} else {
		file = reportOutput.createFile(filename);
	}

       var letsGo = true;
       var contentTotal = "";
       var dbid="0";
       var query="";
       var results;
       var counter = 0;

       while (letsGo){
          counter++;
          query = typeArray[i] + query;
logger.log(query);
          results = search.luceneSearch(query, "@sys:node-dbid", true); 
          logger.log("Results length="+results.length);
          for (j=0;j<results.length;j++){
             var result = results[j];
             //contentTotal += processResultObject(result) + "\n";
             var dbid = result.properties["sys:node-dbid"];
//             logger.log(dbid  + " - " + result.name + " - " + result.name.type  + " - " + result.type);
             contentTotal += result.properties["sys:node-dbid"]  + " - " + result.name + "\n";
           } // end for
           var arguments = new Array();
 
           arguments["query"]=typeArray[i] + query;
           arguments["type"]=fileArray[i];
           var content = companyhome.processTemplate(ftl, arguments);

           reporting.processRepositoryUpdate(fileArray[i], content);
 
         if (results.length>0) {
              var dbid =results[(results.length)-1].properties["sys:node-dbid"]+1;
              query =  " AND @sys\\:node-dbid:["+ dbid + " TO MAX]";
              logger.log(counter + " New created = "+query);
          } else { 
              letsGo=false;
          } // end if results.length
       } // end while
       file.content = contentTotal;
       file.save();
  } // end for

function getMyDate(result){
/****
              var dd = result.getDate().toString();
              if (dd.length==1) { dd = "0" + dd; }
              var MM = result.getMonth().toString();
              if (MM.length==1) { MM = "0" + MM; }
              var hh = result.getHours();
              if (hh.length==1) { hh = "0" + hh; }
              var mm = result.getMinutes().toString();
              if (mm.length==1) { mm = "0" + mm; }
              var ss = (result.getSeconds()+1).toString();
              if (ss.length==1) { ss = "0" + ss; }
              var milli = result.getMilliseconds().toString();
              if (milli.length==2) { milli = "0" + milli; }
              if (milli.length==1) { milli = "00" + milli; }
              var searchDate = result.getFullYear() + "\-"+ MM + "\-" + dd +"T"+ hh+"\:"+mm + "\:"+ ss +"\." +  milli +"Z";  
***/
}