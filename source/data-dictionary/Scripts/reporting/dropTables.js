<import resource="/Company Home/Data Dictionary/Scripts/reporting/alfresco-business-reporting.lib.js">
// make sure we always include the reporting library 

// This script is part of the Alfresco Business Reporting project.

//var fileString = "person,folder,document,site,datalist,datalistitem,workflowdef,workflowtask,forum,topic,post";
// otherwise, define a subset of the tables to drop
reporting.dropTables(fileString);
