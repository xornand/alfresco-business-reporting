/**
 * Copyright (C) 2011 Alfresco Business Reporting project
 *
 * This file is part of the Alfresco Business Reporting project.
 *
 * Licensed under the GNU GPL, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.gnu.org/licenses/gpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.alfresco.reporting;


import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.jscript.BaseScopableProcessorExtension;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.alfresco.reporting.*;

public class AlfrescoReporting extends BaseScopableProcessorExtension {
	private static Log logger = LogFactory.getLog(AlfrescoReporting.class);

	private ServiceRegistry serviceRegistry;

	private String blacklist=",";
	private Properties globalProperties;
	private Properties namespaces = null;
	private DatabaseHelperBean dbhb = null;

	 
	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}
	
	public void setDatabaseHelperBean(DatabaseHelperBean databaseHelperBean) {
		this.dbhb = databaseHelperBean;
	}
	
	   public void setProperties(Properties properties){
	    	this.globalProperties = properties;
	    }
	    
	    public void logAllProperties(){
	 
	    	String returnString = "Size: " + globalProperties.size()+" - "; 
	    	Enumeration keys = globalProperties.keys();
	   		while (keys.hasMoreElements()){
	   			String key = (String)keys.nextElement();
	   			if (key.indexOf("reporting.")>-1){
		   			logger.debug(key+"="+globalProperties.getProperty(key));
		   			returnString += key+"="+globalProperties.getProperty(key)+"\n";
	   			}
	   		}

	    }
	    
	    public boolean isEnabled(){
	    	boolean returnBoolean = !globalProperties.getProperty("reporting.enabled").equalsIgnoreCase("false");
	    	return returnBoolean;
	    }
	    
	public AlfrescoReporting(){
		logger.info("Starting AlfrescoReporting module (Constructor)");
	}
	

// ----------------------------------------------------------------------------	
// util methods
// ----------------------------------------------------------------------------	
	
	private Properties getNameSpaces(){
		if (this.namespaces == null){
			this.namespaces = new Properties();
			Collection<String> keys = serviceRegistry.getNamespaceService().getPrefixes(); 
			for (String key : keys){
				String value = serviceRegistry.getNamespaceService().getNamespaceURI(key);
				String into = key + "_";
				String from = "{" + value + "}";
				this.namespaces.setProperty(into, from);
				//logger.debug("Replacing: " + from + " into: " + into);
			}
		} 
		
		return namespaces;
	}
	
	
	private String replaceNameSpaces(String namespace) {
		// use regular expressions to do a global replace of the full namespace into the short version.
		Properties p = getNameSpaces();
		Enumeration keys = p.keys(); 
		while (keys.hasMoreElements()){
			String into = (String)keys.nextElement();	
			String from = p.getProperty(into);
			namespace=namespace.replace(from, into);
		}
		namespace=namespace.replace("-","_");
		  
		return namespace;
	}
	
	private String replaceSQLValues(String value){ //u0022 u0025 u0026
		//final Pattern p = Pattern.compile("/'");
		//RegExp r = null;
		value = value.replace("//u0022/g", "");
		value = value.replace("//u0025/g", "");
		value = value.replace("//u0026/g", "");
		value = value.replace("//u0027/g", "");
		value = value.replace("//u0028/g", "");
		value = value.replace("//u0029/g", ""); 
		value = value.replaceAll("'", "");
		//value = value.replaceAll("_", "\_");
		
		return value;
	}
	

	private Properties transformDefinitionKeyValue(String input, String separator){
		String key="";
		String value="";
		Properties p = new Properties();
		boolean changeInBacklog=false;

		value="";
		try{
			String tmp[] = input.split(separator);
			key = tmp[0].trim();
			key = replaceNameSpaces(key);
			value = tmp[1];
		} catch (Exception e) {System.out.println(e);}
		//logger.debug("key:   "+ key);
		//logger.debug("value: "+ value);
		if (this.blacklist.indexOf(","+key+",")<0){
			p.setProperty(key, replaceSQLValues(value));		
		} else {
			// add property to blacklist, and do not include in INSERT statement
			logger.error("Column " + key + " has no valid type!");
			if (this.blacklist.indexOf(","+key+",")<0){
				this.blacklist += key + ",";
				changeInBacklog=true;
			}
		}
		
		
		if (!blacklist.equals(",") && changeInBacklog){
			logger.error("Blacklisted properties: " + blacklist.substring(1,blacklist.length()));
			changeInBacklog=false;
		}
		
		return p;
	}

// ----------------------------------------------------------------------------
// actual script methods
// ----------------------------------------------------------------------------
	
	public void processRepositoryUpdate(String table, String result){
		processRepositoryUpdate(table, result, true);
	}
	    
	
	
	
	/**
     * The method expects a formatted string, one object per line.
     * In the start there are definition lines, key value pairs.
     * 		key   = column name
     * 		type  = column type
     * then a seperator appears ~|enddefinition|~
     * In the main section each line is the description of an Alfresco business object
     * It is a concatenation of properties, seperated by ~||~
     * Each property is a tripple, seperated by ~|~
     * 		key   = column name
     * 		type  = column type
     * 		value = actual value of the property
     * 
     * @param table Name of the table to be inserted/updated
     * @param result The set of definition and content for the given run
     */
    public void processRepositoryUpdate(String table, String result, boolean insertOnly){
    	logger.debug("processRepositoryUpdate: "+table + " (insertOnly = " + insertOnly+")");
    	Statement stmt=null;
    	Properties tableDesc = new Properties();
    	String[] lines = result.split("\n");
    	logger.debug("Table Description found " + lines.length + " lines");
    	String seperator = "~|~";
    	String properties = "";
    	
    	if (isEnabled()){
	    	try {
				stmt = dbhb.getConnection().createStatement();
				dbhb.getConnection().setAutoCommit(true);
				tableDesc = dbhb.getTableDescription(stmt, table);
				logger.debug("Got the description ");
				String key  ="";
				String type = "";
				String value = "";
				int numberOfRows=0;
				ReportLine rl = new ReportLine(table);// container for description of 1 Object containing n properties
				String state = "DEFINITION"; //choice of DEFINITION or DATA
		    	for (String line : lines){
		    		logger.debug(line);
		    		line = line.trim();
		    		if (line.equals("~|enddefinition|~")) {
		    			state="DATA";
		    		} else {
			    		
			    		if (state.equals("DEFINITION") && (line != null) && (line.indexOf("~|~")>-1)){
			    			key = line.substring(0, line.indexOf("~|~"));
			    			type = line.substring(line.indexOf("~|~")+3, line.length());
			    			key = replaceNameSpaces(key.trim());
							//logger.debug("DEFINITION Found column: " + key +" and type: " + type);
			    			// ignore datatypes that cannot be matched to SQL types (like associations)
							if ((!"IGNORE".equals(type)) && (!tableDesc.containsKey(key))){ 
								dbhb.extendTable(stmt, table, key, type);
							} else {
								logger.debug("DEFINITION Column " + key + " already exists.");
							} 
			    		} // end if DEFINITON
			    		
			    		if (state.equals("DATA")){
			    			numberOfRows = 0;
			    			while ((line != null) && (line.indexOf("~||~") > 0)){
			    				properties = line.substring(0, line.indexOf("~||~"));
			    				line  = line.substring(line.indexOf("~||~")+4, line.length());
			    				if (null!=properties){
				    				int c = properties.indexOf("~|~");
				    				if (c>0){
					    				key   = properties.substring(0,c);
					    				type  = properties.substring(c+3, properties.lastIndexOf("~|~") );
					    				value = properties.substring(properties.lastIndexOf("~|~")+3, properties.length());
					    				// ignore property types of IGNORE, these cannot be mapped to SQL (like assocs)
					    				if (!"IGNORE".equals(type)){
					    					rl.setLine(key, type, value);
					    				} //end if type != IGNORE
				    				} // end if (c>0)
			    				} // end if null!=properties... WTF, how can this happen?
			    			} // end while
			    			try{
			    				if ( (rl.size()>0) && (rl.getValue("sys_node_uuid").length()>5)){
				    				logger.debug("insertOnly="+insertOnly+" row exists?");
				    				if (!insertOnly && dbhb.rowExists(stmt, rl)){
				    					logger.debug("Going UPDATE");
				    					numberOfRows = dbhb.updateIntoTable(stmt, rl);
				    					logger.debug(numberOfRows+ " rows updated");
				    				} else {
				    					logger.debug("Going INSERT");
				    					numberOfRows = dbhb.insertIntoTable(stmt, rl);
				    					logger.debug(numberOfRows+ " rows inserted");
				    				}
			    				}
			    			} catch (SQLException e){
		    					logger.error("Error in insert/update against table " + table);
		    					logger.error(e);
		    					e.printStackTrace();
		    				} finally {
		    					rl.reset();
		    				}
			    			
			    		} //end if DATA
		    		} // end else
				} // end for line:lines
				stmt.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				logger.error(e);
				e.printStackTrace();
			} finally {
			      //finally block used to close resources
				  try{
				     if(stmt!=null)
				        stmt.close();
				  }catch(SQLException se2){
			      }// nothing we can do
			}    	
    	} else {
    		logger.warn("Alfresco Business Reporting is NOT enabled.");
    	}
    }

    
    /**
     * dropTables drops a list of tables if they exist
     * 
     * @param tables a comma separated list of table names
     */
    public void dropTables(String tablesToDrop){
    	logger.debug("Starting dropTables: "+tablesToDrop);
    	dbhb.dropTables(tablesToDrop);
    }

    
    /**
     * createEmptyTables creates a list of empty tables only containing the node_uuid as a single column
     * 
     * @param tables a comma separated list of table names
     */
    public void createEmptyTables(String tablesToCreate){
    	logger.debug("Starting createEmptyTables: " + tablesToCreate);
    	dbhb.createEmptyTables(tablesToCreate);
    }

    
    /**
     * processReport executes a JasperReports/iReport Report
     * @param jrxmlNodeRef the noderef containing the report definition
     * @param outputNodeRef the noderef containing the resulting report (pdf, doc...)
     * @param outputType the type of report to generate [pdf, html, doc, xls]
     */
    public void processReport(NodeRef inputNodeRef, NodeRef outputNodeRef, String outputType){
    	logger.debug("starting ProcessReport generating a " + outputType);
    	if (isEnabled()){
	    	String name = serviceRegistry.getNodeService().getProperty(inputNodeRef, ContentModel.PROP_NAME).toString();
	    	Reportable reportable = null;
	    	if (name.toLowerCase().endsWith(".jrxml") || name.toLowerCase().endsWith(".jasper")){
	    		logger.debug("It is a Jasper thingy!");
	    		reportable = new JasperReporting();
	    	}
	    	if (name.endsWith(PentahoReporting.EXTENSION)){
	    		logger.debug("It is a Pentaho thingy!");
	    		reportable = new PentahoReporting();
	    	}
	    	if (reportable!= null){
	    		reportable.setUsername(dbhb.getUsername());
	    		reportable.setPassword(dbhb.getPassword());
	    		reportable.setDriver(dbhb.getJdbcDriver());
	    		reportable.setUrl(dbhb.getDatabase());
	    		//reportable.setConnection(dbhb.getConnection());
				reportable.setServiceRegistry(serviceRegistry);
				reportable.setReportDefinition(inputNodeRef);
				reportable.setOutputFormat(outputType);
				reportable.setResultObject(outputNodeRef);
				logger.debug("Lets go processReport!");
				reportable.processReport();
	    	} else {
	    		logger.error(name + " is not a valid report definition");
	    	}
    	} else {
    		logger.warn("Alfresco Business Reporting is NOT enabled...");
    	}
    }
    
    
    /**
     * typeForProperty returns the short type, given a property full-name
     * @param property
     * @return type definition of the property
     */
    public String typeForProperty(String property){
    	String returnType="undefined";
    	try{
    		// How typical, can't find this property type... Lets make a manual exception
    		if (property.equals("{http://www.alfresco.org/model/content/1.0}contentType")){
    			returnType="type";
    		} else {
		    	// @TODO use a property cache to get the props from cache instead of asking db over again
		    	String model = property.substring(property.indexOf("{")+1, property.indexOf("}"));
		    	String name =  property.substring(property.indexOf("}")+1, property.length());
		    	PropertyDefinition pd = serviceRegistry.getDictionaryService().getProperty(QName.createQName(model, name));
		    	returnType=pd.getDataType().getName().toString();
		    	returnType=returnType.substring(returnType.indexOf("}")+1,returnType.length());
        	}
    	} catch (NullPointerException npe){
    		logger.error("ERROR: input="+property);
    	} finally {
    		return returnType;
    	}
    }
    
	    
	/**
	 * logAllPropertyTypes shows all property types that are currently registered in the Alfresco repository
	 * required: enable debug logging of this class!
	 */
    public void logAllPropertyTypes(){
    	Collection<QName> dts = serviceRegistry.getDictionaryService().getAllDataTypes();
    	Iterator myIterator = dts.iterator();
    	while (myIterator.hasNext()){
    		QName q = (QName)myIterator.next(); 
    		String returnType = q.toString();
    		returnType=returnType.substring(returnType.indexOf("}")+1,returnType.length());
    		logger.debug(returnType);
    	} // end while
    }

}
