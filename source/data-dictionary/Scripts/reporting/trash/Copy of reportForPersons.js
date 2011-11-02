var typeArray = new Array("TYPE:\"{http://www.alfresco.org/model/content/1.0}folder\" AND NOT TYPE:\"{http://www.alfresco.org/model/site/1.0}site\" AND NOT TYPE:\"{http://www.alfresco.org/model/datalist/1.0}dataList\" AND NOT TYPE:\"{http://www.alfresco.org/model/bpm/1.0}package\"",  // exclude fm:forums, fm:forum, fm:topic, fm:post
  "TYPE:\"{http://www.alfresco.org/model/content/1.0}content\" AND NOT TYPE:\"{http:\/\/www.alfresco.org\/model\/bpm\/1.0}task\" AND NOT TYPE:\"{http://www.alfresco.org/model/datalist/1.0}dataListItem\"",
  "TYPE:\"{http://www.alfresco.org/model/site/1.0}site\"",
  "TYPE:\"{http://www.alfresco.org/model/datalist/1.0}dataList\"",
  "TYPE:\"{http://www.alfresco.org/model/datalist/1.0}dataListItem\"",
//Wiki
  "TYPE:\"{http://www.alfresco.org/model/bpm/1.0}package\"",
  "TYPE:\"{http:\/\/www.alfresco.org\/model\/bpm\/1.0}task\"",
  "TYPE:\"{http:\/\/www.alfresco.org\/model\/forum\/1.0}forum\"",
  "TYPE:\"{http:\/\/www.alfresco.org\/model\/forum\/1.0}topic\"",
  "TYPE:\"{http:\/\/www.alfresco.org\/model\/forum\/1.0}post\"",
  "TYPE:\"{http://www.alfresco.org/model/content/1.0}person\"");

var fileString = "folder,document,site,datalist,datalistitem,workflowdef,workflowtask,forum,topic,post,person";
var fileArray = fileString.split(",");
logger.log("Found " +typeArray.length + " entries");

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

if (ftl)
{
  for (var i=0; i<typeArray.length; i++){
        var filename = fileArray[i] + ".txt";
        logger.log("Stuffing " + typeArray[i] + " into " + filename);
        var file = reportOutput.childByNamePath(filename);
	if (file)
	{
		//personTotal=file.content;
	} else {
		file = reportOutput.createFile(filename);
	}

       var arguments = new Array();
       var modelOut = new Array(); // contains table description, column name = type
       var valuesOut = new Array(); // contains colum name = value

       arguments["query"]=typeArray[i];
       arguments["type"]=fileArray[i];
       var content = companyhome.processTemplate(ftl, arguments);

        reporting.processRepositoryUpdate(fileArray[i], content);

       file.content = content;
       file.save();
  } // end for
} // end if ftl