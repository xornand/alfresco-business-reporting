<import resource="/Company Home/Data Dictionary/Scripts/reporting/alfresco-business-reporting.lib.js">
// make sure we always include the reporting library 

//var fileString = "person,folder,document,site,datalist,datalistitem,workflowdef,workflowtask,forum,topic,post";
// otherwise, define a subset of the tables to create
reporting.createEmptyTables(fileString);
