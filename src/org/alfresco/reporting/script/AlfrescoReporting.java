/**
 * Copyright (C) 2011 - 2013 Alfresco Business Reporting project
 *
 * This file is part of the Alfresco Business Reporting project.
 *
 * Licensed under the GNU LGPL, Version 3.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.gnu.org/licenses/lgpl.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.alfresco.reporting.script;


import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.jscript.BaseScopableProcessorExtension;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.site.SiteModel;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.Path.ChildAssocElement;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.audit.AuditQueryParameters;
import org.alfresco.service.cmr.audit.AuditService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.CategoryService.Mode;
import org.alfresco.service.cmr.search.CategoryService.Depth;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.site.SiteInfo;
import org.alfresco.service.cmr.site.SiteService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.alfresco.reporting.*;
import org.alfresco.reporting.db.DatabaseHelperBean;
//import org.alfresco.reporting.mybatis.ReportingDAO;
import org.json.simple.parser.ParseException;

public class AlfrescoReporting extends BaseScopableProcessorExtension {
	private static Log logger = LogFactory.getLog(AlfrescoReporting.class);

	private ServiceRegistry serviceRegistry;
	private SearchService searchService;
	private NodeService nodeService=null;
	private AuthorityService authorityService=null;
	private AuditService auditService=null;
	private SiteService siteService = null;
	
	private Properties globalProperties;
	private Properties datadictionary;
	private Properties namespaces = null;
	private Properties replacementTypes = null;
	private DatabaseHelperBean dbhb = null;
	
	private NodeRef reportingRootRef = null;
	private String blacklist=",";
	private String method = Constants.UPDATE_VERSIONED;
//	private String table; // this is a dirty basterd! It is a global because it is called from JavaScript. Need to get rid of that!
	private List<NodeRef> queue = new ArrayList<NodeRef>();
	private Properties versionNodes = new Properties();
	
	private String reporting_custom_properties = Constants.REPORTING_CUSTOM_PROPERTIES;
	private String multivalue_seperator = Constants.MULTIVALUE_SEPERATOR;
	
	// -----------------------------------------------------------------------
	/**
	 * the obvious getters and setters from bean definition
	 */
	
	
	public ServiceRegistry getServiceRegistry() {
		return serviceRegistry;
	}

	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}
	
	public void setAuditService(AuditService auditService) {
		this.auditService = auditService;
	}
	
	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}
	
	public void setAuthorityService(AuthorityService authorityService) {
		this.authorityService = authorityService;
	}
	
	public void setSiteService(SiteService siteService) {
		this.siteService = siteService;
	}
	
	public void setDatabaseHelperBean(DatabaseHelperBean databaseHelperBean) {
		this.dbhb = databaseHelperBean;
	}
	
	public void setProperties(Properties properties){
		this.globalProperties = properties;
	}
	   
	// -----------------------------------------------------------------------

	private Properties getGlobalProperties(){
		if (this.globalProperties==null){
			logger.fatal("Whoot! globalProperties object is null!!");
		}
		return this.globalProperties;
	}

	private void setBlacklist(String list){
		this.blacklist = list ;
	}
	
	private String getBlacklist(){
		String keys = ",";
		if (",".equals(this.blacklist)){
			//String blockkeys = "trx_orphan,homeFolder,homeFolderProvider,cm_content,cm_source,cm_organization,cm_organizationId,cm_references,cm_attachments,cm_avatar";
			keys =  getGlobalProperties().getProperty(Constants.property_blockkeys, "-")+",";
			keys += getGlobalProperties().getProperty(Constants.property_blacklist, "")+",";
			keys = keys.replaceAll("-","_");
			keys = keys.replaceAll(":","_");
			setBlacklist(keys);
		} else {
			keys = this.blacklist;
		}
		return keys;
	}
	
	// -----------------------------------------------------------------------
		
	/**
	 * Can be called from script.
	 * Redefines SQL column types for the Alfresco properties existing in the 
	 * properties file. (Sum of all columns <65535 bytes, UTF-8 takes up to 4 bytes 
	 * for a char, so each d:text into VARCHAR(500) is too eager...
	 * This properties file will overide individual PROPERTIES, not Alfresco TYPES
	 * 
	 * @param newFileName a properties file that contains custom SQL column 
	 * defintions for a given Alfresco property
	 */
	public void setCustomModelProperties(String newFileName){
		reporting_custom_properties = newFileName;
	}
	
	
	public String getStoreList(){
		return getGlobalProperties().getProperty(Constants.property_storelist, "");
	}
	
	/**
	 * Adds this string to all multi value properties. Default = comma (',').
	 * @param inString seperator, can me multi-character. At least 1 character.
	 */
	public void setMultiValueSeperator(String inString){
		if ((inString!=null) && (inString.length()>0)){
			multivalue_seperator = inString+" ";
		}
	}
		

    public boolean isExecutionEnabled(){
    	boolean executionEnabled = true;
    	
    	try{
    		//executionEnabled = globalProperties.getProperty("reporting.execution.enabled", "true").equalsIgnoreCase("true");
    		logger.debug("isExecutionEnabled: "+ nodeService.getProperty(getReportingRoot(), ReportingModel.PROP_REPORTING_GLOBAL_EXECUTION_ENABLED));
    		executionEnabled = (Boolean)nodeService.getProperty(getReportingRoot(), ReportingModel.PROP_REPORTING_GLOBAL_EXECUTION_ENABLED);
    	} catch (Exception e) {
    		logger.debug("isExecutionEnabled() returning exception. Thus returning true;");
    		//logger.debug(e);
    		executionEnabled = true;
    	} 
    	return executionEnabled;
    }
    
    public boolean isHarvestEnabled(){
    	boolean harvestEnabled = true;
    	try{
    		//harvestEnabled = globalProperties.getProperty("reporting.harvest.enabled", "true").equalsIgnoreCase("true");
    		logger.debug("isHarvestEnabled: " + nodeService.getProperty(getReportingRoot(), ReportingModel.PROP_REPORTING_HARVEST_ENABLED));
    		harvestEnabled = (Boolean)nodeService.getProperty(getReportingRoot(), ReportingModel.PROP_REPORTING_HARVEST_ENABLED);
    	} catch (Exception e) {
    		logger.debug("isHarvestEnabled() returning exception. Thus returning true;");
    		//logger.debug(e);
    		harvestEnabled = true;
    	} 
    	return harvestEnabled;
    }
    
    /**
     * Get the Folder that contains the reporting:reporting aspect. This
     * contains the booleans harvestEnabled and globalExecutionEnabled
     * @return NodeRef of the folder
     */
    private NodeRef getReportingRoot(){
    	if (this.reportingRootRef!=null){
    		return this.reportingRootRef;
    	} else {
	    	NodeRef thisRootRef = null;
	    	ResultSet placeHolderResults= null;
	    	try{
	    		/*
	    		 * SearchParameters sp = new SearchParameters();
					String fullQuery = query + " +@sys\\:node-dbid:["+ highestDbId+" TO MAX]";  
					logger.debug("processPerson: query="+fullQuery);
					sp.setLanguage(SearchService.LANGUAGE_LUCENE);
					sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
					//sp.addSort("@" + ReportingModel.PROP_SYSTEM_NODE_DBID.toString(), true);
					sp.addSort("@{http://www.alfresco.org/model/system/1.0}node-dbid", true);
					sp.setQuery(fullQuery);
					logger.debug("processPerson: Before searchService" );
					rs = serviceRegistry.getSearchService().query(sp);
	    		 * 
	    		 */
	    		String fullQuery = "TYPE:\"reporting:reportingRoot\"";
	    		SearchParameters sp = new SearchParameters();
	    		sp.setLanguage(SearchService.LANGUAGE_LUCENE);
				sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
				sp.setQuery(fullQuery);
				placeHolderResults = searchService.query(sp);
				/*
	    		placeHolderResults = searchService.query(
						StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, 
						ReportingModel.TYPE_REPORTING_ROOT, 
						null);
			    */	
				//		Constants.QUERYLANGUAGE, 
				//		"ASPECT:\"reporting:reportingRoot\"");
			
				// cycle the resultset of containers
				for (ResultSetRow placeHolderRow : placeHolderResults){
					thisRootRef = placeHolderRow.getChildAssocRef().getChildRef();
					
					logger.debug("Found reporting root: " + 
								nodeService.getProperty(thisRootRef, ContentModel.PROP_NAME));
					
					
				} // end for ResultSetRow
	    	} catch (Exception e){
	    		e.printStackTrace();
	    	} finally {
	    		if (placeHolderResults!=null){
	    			placeHolderResults.close();
	    		}
	    	}
	    	this.reportingRootRef = thisRootRef;
	    	return thisRootRef;
    	}
    }
    
    public void resetLastTimestampTable(String tablename){
    	dbhb.resetLastTimestampTable(tablename);
    }
    
    /**
     * get the current status of the reporting tool
     * @return
     */
    public String getLastTimestampStatus(String tablename){
    	return dbhb.getLastTimestampStatus(tablename);
    }
    

    /**
     * get the timestamp of the last succesful run
     * @return
     */
    public String getLastTimestamp(String tablename){
    	return dbhb.getLastTimestamp(tablename);
    }
    
    public void setLastTimestampAndStatusDone(String tablename, String timestamp){
    	dbhb.setLastTimestampAndStatusDone(tablename, timestamp);
    }
    
    /**
     * Set the string value of timestamp in the table 'lastrun'
     * If the table does not yet exist, the table will be created.
     * @param timestamp
     * @throws SQLException
     */
    public void setLastTimestampStatusRunning(String tablename) {
    	dbhb.setLastTimestampStatusRunning(tablename);
    }
    
    public void createLastTimestampTable(String tablename){
    	dbhb.createLastTimestampTableRow(tablename);
    }
    
    public void dropLastTimestampTable(){
    	dbhb.dropLastTimestampTable();
    }
        
    /**
     * Mapping of Alfresco property TYPES onto SQL column definitions.
     * the value of "-" means the Alfresco property will NOT be automatically 
     * mapped into the SQL database. The proeprties file will be read from classpath
     * There are custom calls for Site, Category, Tags
     * 
     * @return Properties object
     * @throws Exception 
     */
    private Properties getClassToColumnType() throws Exception{
		if (datadictionary==null){		
			
			try {
				ClassLoader cl = this.getClass().getClassLoader();
				InputStream is =cl.getResourceAsStream(Constants.REPORTING_PROPERTIES);
				Properties p = new Properties();
				p.load(is);
				datadictionary = p;
			} catch (IOException e) {
				e.printStackTrace();
				throw new Exception(e);
			}
		}

    	return datadictionary;
    }
    
    /**
     * Reads external properties file. The properties in this file will override 
     * the default mapping of individual Alfresco PROPERTY types into SQL column  
     * definitions ** on a per-property basis**
     * (One often knows a zip code is d:text, but never more than 8 charaters, so 
     * VARCHAR(8) will do). The total length of the row (sum of the column lengths) 
     * can never be more than 65535 bytes. And i guess UTF-8 makes a reservation of 
     * 4 bytes per character
     * 
     * The properties file REPORTING_CUSTOM_PROPERTIES can be named differently using
     * the method setCustomModelProperties(String newFileName)
     * 
     * @return Properties object with as content the key/value pairs from the properties file.
     */
	private Properties getReplacementDataType(){ 
		
		if (replacementTypes==null){		
			
			try {
				ClassLoader cl = this.getClass().getClassLoader();
				InputStream is =cl.getResourceAsStream(reporting_custom_properties);
				Properties p = new Properties();
				p.load(is);
				replacementTypes = p;
			} catch (Exception e) {
				//e.printStackTrace();
				replacementTypes = new Properties();
			}
		}
      return replacementTypes;
	}
    
	
	public Map getShowTables() {
		return dbhb.getShowTables();
	}
	
	
	public AlfrescoReporting(){
		logger.info("Starting AlfrescoReporting module (Constructor)");
	}
	
	/**
	 * Can be called from script.
	 * @table the current active table/query name
	 */
	/*
	public void setTable(String table){
//		if (logger.isDebugEnabled()) 
//			logger.debug("Setting Table=" + table);
		this.table = table;
	}
	*/
	
	/**
	 * Will be called from script.
	 * @param scriptNode the node to be added to the execution queue
	 */
	public void addToQueue(ScriptNode scriptNode){
		if (logger.isDebugEnabled()) 
			logger.debug("addToQueue: Prepare adding scriptNode=" + scriptNode.getName() + " | " + scriptNode.getNodeRef());

		this.queue.add(scriptNode.getNodeRef());
	}
	
	/**
	 * Will be called from script.
	 * @param scriptNode the 'parent' node of the versioned instance
	 * @param versionNode teh versioned node to be added to the exeution queue
	 */
	public void addToQueue(ScriptNode scriptNode, ScriptNode versionNode){
		if (logger.isDebugEnabled()) 
			logger.debug("addToQueue: Prepare adding scriptNode=" + scriptNode.getNodeRef() + ", versionNode=" + versionNode.getNodeRef());
		versionNodes.setProperty(versionNode.getNodeRef().toString(), scriptNode.getNodeRef().toString());
		this.queue.add(versionNode.getNodeRef());
	}
	
	/**
	 * Start the execution of the queue -> put all properties of each and every entry in 
	 * the queue into the reporting database
	 * @throws Exception 
	 */
	public void executeQueue(String table) throws Exception{
		if (logger.isDebugEnabled()) 
			logger.debug("Executing queue");
		Properties nameSpaces = getNameSpaces();
		Properties p = getTableDefinitionFromQueue(nameSpaces);
		setTableDefinition(p, table);
		processUpdate(table);
		
	}

	
	/**
	 * Will be called from script.
	 * This method needs to be called between tables/queries. It will reset 
	 * all kind of variables 
	 */
	public void resetAll(){
		this.method=Constants.UPDATE_VERSIONED;
		this.datadictionary=null;
		this.queue = new ArrayList<NodeRef>();
		this.versionNodes = new Properties();
		this.replacementTypes = null;
	}


// ----------------------------------------------------------------------------	
// util methods
// ----------------------------------------------------------------------------	

	/**
	 * Given the ScriptNode, get all available associations (parent/child & target/source)
	 * Create a column to store the noderef if there are any of these associations
	 * @throws Exception 
	 * 
	 */ 
	private Properties processAssociationDefinitions(Properties definition, NodeRef nodeRef, String defBacklist) throws Exception{
		//Child References
		try{
			List<ChildAssociationRef> childCars= serviceRegistry.getNodeService().getChildAssocs(nodeRef);
			if (childCars.size()>0){
				String type = getClassToColumnType().getProperty("noderefs","-");
				if (getReplacementDataType().containsKey("child_noderef")){
					type = getReplacementDataType().getProperty("child_noderef", "-").trim();
				}
				definition.setProperty("child_noderef", type);
			}
		} catch (Exception e) {
			logger.debug("processAssociationDefinitions: child_noderef ERROR!");
			e.printStackTrace();
			
		}
		
		try{
			// Parent References
			ChildAssociationRef parentCar= serviceRegistry.getNodeService().getPrimaryParent(nodeRef);
			if (parentCar!=null){
				String type = getClassToColumnType().getProperty("noderef","-");
				if (getReplacementDataType().containsKey("parent_noderef")){
					type = getReplacementDataType().getProperty("parent_noderef", "-").trim();
				}
				definition.setProperty("parent_noderef", type);
			}
		} catch (Exception e) {
			logger.debug("processAssociationDefinitions: parent_noderef ERROR!");
			e.printStackTrace();
			
		}
		
		try{	
			Collection<QName> assocTypes = serviceRegistry.getDictionaryService().getAllAssociations();
			for (QName type : assocTypes){
				String key="";
				String shortName = replaceNameSpaces(type.toString());
				String store = (String)serviceRegistry.getNodeService().getProperty(nodeRef, ContentModel.PROP_STORE_PROTOCOL);
				//logger.debug("STORE="+store);
				boolean stop = nodeRef.toString().startsWith("versionStore") || nodeRef.toString().startsWith("archive");
				if (stop || shortName.startsWith("trx") || shortName.startsWith("act") ||  shortName.startsWith("blg_") || 
						shortName.startsWith("wca") || shortName.startsWith("wcm") ||
						shortName.startsWith("ver") || shortName.startsWith("fm_") ||
						shortName.startsWith("emailserver_") || shortName.startsWith("sys_") ||
						shortName.startsWith("cm_member") || shortName.startsWith("cm_subcategories") ||
						shortName.startsWith("cm_subscribedBy") || shortName.startsWith("cm_attachments") ||
						shortName.startsWith("cm_translations") || shortName.startsWith("cm_preference") ||
						shortName.startsWith("cm_replaces") || shortName.startsWith("cm_ml") ||
						shortName.startsWith("cm_failed") || shortName.startsWith("cm_references") ||
						shortName.startsWith("cm_avatar") || shortName.startsWith("rn_") ||
						shortName.startsWith("imap_") || shortName.startsWith("usr_") ){} 
				else {
					try{
						//logger.debug("ASSOCIATIONS: processing " + shortName + " for " + shortName);
						List<AssociationRef> targetRefs = 
								serviceRegistry.getNodeService().getTargetAssocs(nodeRef, type);
						if (targetRefs.size()>0){
							logger.debug("Found a Target association! " + type.toString());
							key = type.toString();
							logger.debug("Target: key1="+key);
							key = replaceNameSpaces(key);
							logger.debug("Target: key2="+key);
							if (!defBacklist.contains(","+key+",") && !type.equals("-")){
								logger.debug("Target: Still in the game! key="+key);
								String sType = getClassToColumnType().getProperty("noderefs","-");
								if (getReplacementDataType().containsKey(key)){
									sType = getReplacementDataType().getProperty(key, "-").trim();
								}
								logger.debug("Target: Setting " + key + "="+sType);
								definition.setProperty(key, sType);
								// extensionPoint: Include username, or name property of target
							}
						}
					} catch (Exception e){
						logger.debug("processAssociationDefinitions: Target_Association ERROR! key="+key);
						//e.printStackTrace();
					}
					
					try{
						List<AssociationRef> sourceRefs = 
							serviceRegistry.getNodeService().getSourceAssocs(nodeRef, type);
						if (sourceRefs.size()>0){
							logger.debug("Found a Source association! " + type.toString());
							
							key = type.toString();
							key = replaceNameSpaces(key);
							if (!defBacklist.contains(","+key+",") && !type.equals("-")){
								String sType = getClassToColumnType().getProperty("noderefs","-");
								if (getReplacementDataType().containsKey(key)){
									sType = getReplacementDataType().getProperty(key, "-").trim();
								}
								definition.setProperty(key, sType);
								//extensionPoint: Include username, or name property of source
							}
						}
					} catch (Exception e){
						logger.debug("processAssociationDefinitions: Source_Association ERROR! key="+key);
						//e.printStackTrace();
					}
				} // end exclude trx_orphan
			} // end for
		} catch (Exception e) {
			logger.debug("processAssociationDefinitions: source-target ERROR!");
			e.printStackTrace();
			
		}
		return definition;
	}	
	
	/**
	 * 
	 * @param definition
	 * @param nodeRef	Current nodeRef to put all related and relevant property 
	 * values into the reporting database
	 * @param defBacklist the Blacklist String
	 * @return
	 */
	private Properties processPropertyDefinitions(Properties definition, NodeRef nodeRef, String defBacklist){
		try{
			Map<QName, Serializable> map = serviceRegistry.getNodeService().getProperties(nodeRef);
			Iterator<QName> keys = map.keySet().iterator();
			while (keys.hasNext()){
				String key="";
				String type="";
				try{
					QName qname = keys.next();
					//Serializable s = map.get(qname);
					if (qname!=null){
						key = qname.toString();
						key = replaceNameSpaces(key);
						//logger.debug("processPropertyDefinitions: Processing key " + key);
						if (!key.startsWith("{urn:schemas_microsoft_com:}") && !definition.containsKey(key) ){
							type="";
							if (getReplacementDataType().containsKey(key)){
								type = getReplacementDataType().getProperty(key, "-").trim();
							} else {
								type="-";
								try{
									type=serviceRegistry.getDictionaryService().getProperty(qname)
										.getDataType().toString().trim();
									type = type.substring(type.indexOf("}")+1, type.length());
									type = getClassToColumnType().getProperty(type,"-");
								} catch (NullPointerException npe){
									// ignore. cm_source and a few others have issues in their datatype??
									//logger.fatal("Silent drop of NullPointerException against " + key);
								}
								// if the key is not in the BlackList, add it to the prop object that 
								// will update the table definition
							}
							if ((type!=null) && !type.equals("-") && !type.equals("") && (key!=null) && 
									(!key.equals("")) && (!defBacklist.contains(","+key+","))){
								definition.setProperty(key, type);
								//if (logger.isDebugEnabled())
								//	logger.debug("processPropertyDefinitions: Adding column "+ key + "=" + type);
							} else {
								//if (logger.isDebugEnabled())
								//	logger.debug("Ignoring column "+ key + "=" + type);
							}
						} // end if containsKey
					} //end if key!=null
				} catch (Exception e) {
					logger.info("processPropertyDefinitions: Property not found! Property below...");
					logger.info("processPropertyDefinitions: type=" + type + ", key="+ key);
					e.printStackTrace();
				}
			} // end while
		} catch (Exception e){
			e.printStackTrace();
		}
		//logger.debug("Exit processPropertyDefinitions");
		return definition;
	}
	
	private Properties getTableDefinitionFromQueue(Properties nameSpaces) throws Exception{
		logger.debug("Enter getTableDefinitionFromQueue");
		Properties definition = new Properties(); // set of propname-proptype
		String defBacklist = ",sys_node_uuid,"+getBlacklist();
		int queuesize = queue.size();
		for (int q=0;q<queue.size();q++){
		//for (NodeRef nodeRef : queue){
			NodeRef nodeRef = queue.get(q);
			try{
				String name = (String)serviceRegistry.getNodeService().getProperty(nodeRef, ContentModel.PROP_NAME);
				
				logger.debug("getTableDefinitionFromQueue: "+q+"/"+queuesize+ ": " + name);
				// Process Properties
				definition = processPropertyDefinitions(definition, nodeRef, getBlacklist());
		//logger.debug("getTableDefinitionFromQueue: Returned from processPropertyDefinitions");
			
				// if it is a versioned noderef, add the original noderef too
				if (versionNodes.containsKey(nodeRef.toString())){
					definition.setProperty("orig_noderef", getClassToColumnType().getProperty("noderef","-"));
				}
			} catch (Exception e) {
				logger.debug("getTableDefinitionFromQueue: ERROR: versionNodes.containsKey or before");
				e.printStackTrace();
			}
	//logger.debug("getTableDefinitionFromQueue: try/catch survived");
					
			// Process 'manual' properties
			definition.setProperty("site", getClassToColumnType().getProperty("site","-"));
			definition.setProperty("path", getClassToColumnType().getProperty("path","-"));
			definition.setProperty("noderef", getClassToColumnType().getProperty("noderef","-"));
	//logger.debug("getTableDefinitionFromQueue: custom props survived");
			QName myType = serviceRegistry.getNodeService().getType(nodeRef);
			if (serviceRegistry.getDictionaryService().isSubClass(myType, ContentModel.TYPE_CONTENT)){
				definition.setProperty("size", getClassToColumnType().getProperty("size","-"));
				definition.setProperty("mimetype", getClassToColumnType().getProperty("mimetype","-"));
				definition.setProperty("orig_noderef", getClassToColumnType().getProperty("noderef","-"));
				// and some stuff default reporting is dependent on
				definition.setProperty("cm_workingcopylink", getClassToColumnType().getProperty("noderef","-"));
				definition.setProperty("cm_lockOwner", getClassToColumnType().getProperty("noderef","-"));
				definition.setProperty("cm_lockType", getClassToColumnType().getProperty("noderef","-"));
				definition.setProperty("cm_expiryDate", getClassToColumnType().getProperty("datetime","-"));
				definition.setProperty("sys_archivedDate", getClassToColumnType().getProperty("datetime","-"));
				definition.setProperty("sys_archivedBy", getClassToColumnType().getProperty("noderef","-"));
				definition.setProperty("sys_archivedOriginalOwner", getClassToColumnType().getProperty("noderef","-"));
			}
			if (serviceRegistry.getDictionaryService().isSubClass(myType, ContentModel.TYPE_PERSON)){
				definition.setProperty("enabled", getClassToColumnType().getProperty("boolean","-"));
			}
			QName objectType = serviceRegistry.getNodeService().getType(nodeRef);
			if ((serviceRegistry.getNodeService().hasAspect(nodeRef, ContentModel.ASPECT_VERSIONABLE)) &&
					(serviceRegistry.getDictionaryService().isSubClass(objectType, ContentModel.TYPE_CONTENT))){
				String type = getClassToColumnType().getProperty("text","-");
				if (getReplacementDataType().containsKey("cm_versionLabel")){
					type = getReplacementDataType().getProperty("cm_versionLabel", "-").trim();
				} 
				definition.setProperty("cm_versionLabel", type);
					
				type = getClassToColumnType().getProperty("text","-");
				if (getReplacementDataType().containsKey("cm_versionType")){
					type = getReplacementDataType().getProperty("cm_versionType", "-").trim();
				}
				definition.setProperty("cm_versionType", type);
				
			}
	//logger.debug("getTableDefinitionFromQueue: content specific props survived");		
			// Process Associations
			definition = processAssociationDefinitions(definition, nodeRef, getBlacklist());
	//logger.debug("getTableDefinitionFromQueue: associations survived");		
			//list door property keys
		} // end for sn:queue
		logger.debug("Exit getTableDefinitionFromQueue");
		return definition;
	}
	
	/**
	 * Validate if the unique sum of properties exists in the table definition.
	 * Update the table definition if columns are not yet defined
	 * @param props unique set of columns and their type
	 * @throws SQLException
	 */
	private void setTableDefinition(Properties props, String tableName) throws SQLException{
		logger.debug("Enter setTableDefinition tableName="+tableName);
		// get the existing table definition
		Connection conn = dbhb.getConnection();
		conn.setAutoCommit(true);
		Statement stmt =conn.createStatement();
		
		Properties tableDesc = dbhb.getTableDescription(stmt, tableName);
		
		// check if our properties are defined or not
		Enumeration keys = props.keys();
		while (keys.hasMoreElements()){
			String key = (String)keys.nextElement();
			String type = props.getProperty(key,"-");
		
			if ((!"-".equals(type)) && ((!"".equals(type))) && (!tableDesc.containsKey(key))){ 
				if (logger.isDebugEnabled())
					logger.debug("Adding column: " + key +"=" + type);
				dbhb.extendTable(stmt, tableName, key, type);
			} else {
				if (logger.isDebugEnabled())
					logger.debug("DEFINITION Column " + key + " already exists.");
			} // end if else
		} // end while
		logger.debug("Exit setTableDefinition");
	} // end setTableDefinition
	
	/**
	 * 
	 * @param rl
	 * @param sn
	 * @param blacklist
	 * @return
	 */
	private ReportLine processPropertyValues(ReportLine rl, NodeRef nodeRef, String blacklist){
		Map<QName, Serializable> map = 
			serviceRegistry.getNodeService().getProperties(nodeRef);
		
		if (serviceRegistry.getDictionaryService().isSubClass(
						nodeService.getType(nodeRef), 
						ContentModel.TYPE_CONTENT)){
			try {
				rl.setLine("cm_workingcopylink", getClassToColumnType().getProperty("noderef",""), null, getReplacementDataType());
				rl.setLine("cm_lockOwner", getClassToColumnType().getProperty("noderef",""), null, getReplacementDataType());
				rl.setLine("cm_lockType", getClassToColumnType().getProperty("noderef",""), null, getReplacementDataType());
				rl.setLine("cm_expiryDate", getClassToColumnType().getProperty("datetime",""), null, getReplacementDataType());
			} catch (Exception e) {
				e.printStackTrace();
			}
		} // end pre-set valued for these props, since they need to be cleared for cases the checkout has been undone
		
		Iterator<QName> keys = map.keySet().iterator();
		while (keys.hasNext()){
			String key = "";
			String dtype = "";
			try{
				QName qname = keys.next();
				key = qname.toString();
//			logger.debug("processPropertyValues: voor: KEY="+key);
				if (!key.startsWith("{urn:schemas_microsoft_com:}")) {
					key = replaceNameSpaces(key);
					//logger.debug("processPropertyValues: na: KEY="+key);
					
					dtype = serviceRegistry.getDictionaryService().getProperty(qname)
							.getDataType().toString();
					
					//logger.debug("processPropertyValues: voor: DTYPE="+dtype);
					
					dtype = dtype.substring(dtype.indexOf("}")+1, dtype.length()).trim();
					
					//logger.debug("processPropertyValues: na: DTYPE="+dtype);
					
					Object theObject = getClassToColumnType().getProperty(dtype,"-"); 
					String type = theObject.toString();
					//logger.debug("processPropertyValues: na: TYPE="+type);
					
					boolean multiValued = false;
					multiValued = serviceRegistry.getDictionaryService().getProperty(qname).isMultiValued();
					
					//logger.debug("processPropertyValues EVAL: key="+key + ", type="+type+", dtype="+dtype+", value=" + getPropertyValue(nodeRef, qname, dtype, multiValued));
					//logger.debug("processPropertyValues: blacklist="+ blacklist);
				
					if (!blacklist.contains(","+key+",") && !type.equals("-")){
						String value = getPropertyValue(nodeRef, qname, dtype, multiValued);
						rl.setLine(key, type, value, getReplacementDataType());
					}
				} // end exclude Microsoft shizzle. It is created when doing WebDAV
			} catch (Exception e){
				//logger.info("processPropertyValues: " + e.toString());
				logger.info("processPropertyValues: Error in object, property "+key+" not found! (" + dtype +")");
			}
		} // end while loop through this object's properties
		return rl;
	}
	
	/**
	 * 
	 * @param rl
	 * @param sn
	 * @return
	 * @throws Exception 
	 */
	private ReportLine processAssociationValues(ReportLine rl, NodeRef nodeRef, String defBacklist) throws Exception{
		//Child References
		try{
			List<ChildAssociationRef> childCars= serviceRegistry.getNodeService().getChildAssocs(nodeRef);
			if (childCars.size()>0){
				String value="";
				for (ChildAssociationRef car:childCars){
					if (value.length()>0) value +=",";
					value += car.getChildRef(); 
				}
				rl.setLine("child_noderef", getClassToColumnType().getProperty("noderefs","-"), value, getReplacementDataType());
			}
		} catch (Exception e){
			logger.error("Error in processing processAssociationValues");
			e.printStackTrace();
		}
		
		// Parent References
		try{
			ChildAssociationRef parentCar= serviceRegistry.getNodeService().getPrimaryParent(nodeRef);
			if (parentCar!=null){
				String value = parentCar.getParentRef().toString();
				rl.setLine("parent_noderef", getClassToColumnType().getProperty("noderef","-"), value, getReplacementDataType());
			}
		} catch (Exception e){}
		
		
		// Other associations
		Collection<QName> assocTypes = serviceRegistry.getDictionaryService().getAllAssociations();
		for (QName type : assocTypes){
			String shortName = replaceNameSpaces(type.toString());
			//logger.debug("ASSOCIATIONS: processing " + shortName + " for " + sn.getTypeShort());
			if (shortName.startsWith("trx") || shortName.startsWith("act") || shortName.startsWith("wca")){
				// nothing. Dont like these namespaces, that's all.
			} 
			else {
				try{
					List<AssociationRef> targetRefs = 
							serviceRegistry.getNodeService().getTargetAssocs(nodeRef, type);
					if (targetRefs.size()>0){
						String key = type.toString();
						key = replaceNameSpaces(key);
						if (!defBacklist.contains(","+key+",") && !type.equals("-")){
							if ((targetRefs!=null) && targetRefs.size()>0){
								String valueRef="";
								for (AssociationRef ar:targetRefs){
									if (valueRef.length()>0) valueRef +=",";
									valueRef += ar.getTargetRef().toString(); 
								}
								rl.setLine(key, getClassToColumnType().getProperty("noderefs","-"), valueRef, getReplacementDataType());
							}
						} // end if blacklist
						// extensionPoint: Include username, or name property of target
					}
				} catch (Exception e){}
				try{
					List<AssociationRef> sourceRefs = 
						serviceRegistry.getNodeService().getSourceAssocs(nodeRef, type);
					if (sourceRefs.size()>0){
						String key = type.toString();
						key = replaceNameSpaces(key);
						if (!defBacklist.contains(","+key+",") && !type.equals("-")){
							if ((sourceRefs!=null) && sourceRefs.size()>0){
								String value="";
								for (AssociationRef ar:sourceRefs){
									if (value.length()>0) value +=",";
									value += ar.getSourceRef().toString(); 
								}
								rl.setLine(key, getClassToColumnType().getProperty("noderefs","-"), value, getReplacementDataType());
							}
							//extensionPoint: Include username, or name property of source
						} // end if blacklist
					}
					
				} catch (Exception e){}
			} // it is not a trx_
		} // end or
		
		return rl;
	}
	
	 /**
     * @param path
     * @return  display path
     */
    private String toDisplayPath(Path path)
    {
        StringBuffer displayPath = new StringBuffer();
        if (path.size() == 1)
        {
            displayPath.append("/");
        }
        else
        {
            for (int i = 1; i < path.size(); i++)
            {
                Path.Element element = path.get(i);
                if (element instanceof ChildAssocElement)
                {
                    ChildAssociationRef assocRef = ((ChildAssocElement)element).getRef();
                    NodeRef node = assocRef.getChildRef();
                    displayPath.append("/");
                    displayPath.append(serviceRegistry.getNodeService().getProperty(node, ContentModel.PROP_NAME));
                }
            }
        }
        return displayPath.toString();
    }
   
    private void storeRegionPath(Statement stmt, ReportLine rl, NodeRef nodeRef, String regionPath, String columnName, String labelValue){
    	//logger.debug("storeRegionPath: " + table + " | " + regionPath);
    	try{
    		String uuid = nodeRef.toString();
    		rl.setLine("sys_node_uuid", getClassToColumnType().getProperty("noderef"), uuid.split("SpacesStore/")[1], getReplacementDataType());
    		rl.setLine(columnName, getClassToColumnType().getProperty("path"), regionPath, getReplacementDataType());
    		rl.setLine("label", getClassToColumnType().getProperty("label"), labelValue, getReplacementDataType());
    		int numberOfRows = dbhb.insertIntoTable(stmt, rl);
			//logger.debug("storeRegionPath: " + numberOfRows+ " rows inserted");
	    } catch (Exception e){
			e.printStackTrace();
		} finally {
		}

    }
    
    /**
     * 
     * @param table
     */
    public void processPerson(String tableName){
    	logger.debug("Enter processPerson");
    	dbhb.createEmptyTables(tableName);
    	ReportLine rl = new ReportLine(tableName);
    	Statement stmt = null;
    	Properties definition = new Properties(); // set of propname-proptype
    	Properties replacementTypes = getReplacementDataType();
    	
    	try{
    		long highestDbId=0;
    		Connection conn = dbhb.getConnection();
			conn.setAutoCommit(true);
			stmt = conn.createStatement();
			boolean continueSearchCycle=true;
			String query = "+TYPE:\"cm:person\"";
			//setTable(tableName);
			ResultSet rs = null;
			
			while (continueSearchCycle){
				//continueSearchCycle=false;
				try { // make sure to have a finally to close the result set)
					SearchParameters sp = new SearchParameters();
					String fullQuery = query + " +@sys\\:node-dbid:["+ highestDbId+" TO MAX]";  
					logger.debug("processPerson: query="+fullQuery);
					sp.setLanguage(SearchService.LANGUAGE_LUCENE);
					sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
					//sp.addSort("@" + ReportingModel.PROP_SYSTEM_NODE_DBID.toString(), true);
					sp.addSort("@{http://www.alfresco.org/model/system/1.0}node-dbid", true);
					sp.setQuery(fullQuery);
					logger.debug("processPerson: Before searchService" );
					rs = searchService.query(sp);
					logger.debug("processPerson: Found results=" + rs.length());
					if (rs.length()==0){
						continueSearchCycle=false;
						logger.debug("processPerson: Break fired!");
						break; // we're done, no more search results
					}
					if (continueSearchCycle){
						Iterator<ResultSetRow> rsi = rs.iterator();
						while (rsi.hasNext()){
							ResultSetRow rsr = rsi.next();
							definition = processPropertyDefinitions(definition, rsr.getNodeRef(), ",cm_homeFolder,cm_homeFolderProvider"+getBlacklist());
				    		definition.setProperty("noderef", getClassToColumnType().getProperty("noderef","-"));
				    		definition.setProperty("account_enabled", getClassToColumnType().getProperty("boolean","-"));
				    		definition.setProperty("account_expires", getClassToColumnType().getProperty("boolean","-"));
				    		definition.setProperty("account_expirydate", getClassToColumnType().getProperty("datetime","-"));
				    		definition.setProperty("account_locked", getClassToColumnType().getProperty("boolean","-"));
				    		definition.setProperty("zone", getClassToColumnType().getProperty("zone","-"));
				    		
				    		logger.debug("Procesing person with dbid="+nodeService.getProperty(rsr.getNodeRef(),ReportingModel.PROP_SYSTEM_NODE_DBID));
				    		highestDbId=(Long) nodeService.getProperty(rsr.getNodeRef(),ReportingModel.PROP_SYSTEM_NODE_DBID)+1;
						}
						
						logger.debug("processPerson: Before setTableDefinition size=" + definition.size());
						setTableDefinition(definition, tableName);
						
						rsi = rs.iterator();
						while (rsi.hasNext()){
							ResultSetRow rsr = rsi.next();
							rl.reset();
							
				    		//logger.debug("processPerson: Before processProperties");
							try{
								rl = processPropertyValues(rl, rsr.getNodeRef(), ",cm_homeFolder,cm_homeFolderProvider"+getBlacklist());
							} catch (Exception e){
								//logger.error("processUpdate: That is weird, rl.setLine(noderef) crashed! " + rsr.getNodeRef());
								e.printStackTrace();
							}
							
							//logger.debug("processPerson: Before noderef" );
							try{
								rl.setLine("noderef", getClassToColumnType().getProperty("noderef"), rsr.getNodeRef().toString(), replacementTypes);
							} catch (Exception e){
								logger.error("processPerson: That is weird, rl.setLine(noderef) crashed! " + rsr.getNodeRef());
								e.printStackTrace();
							}
							
							//logger.debug("processPerson: Before enabled" );
							try{
								String username 		  = (String)nodeService.getProperty(rsr.getNodeRef(), ContentModel.PROP_USERNAME);
								String account_expires 	  = null;
								String account_expirydate = null;
								String account_locked 	  = null;
								String enabled = null;
	
								username 		  = (String)nodeService.getProperty(rsr.getNodeRef(), ContentModel.PROP_USERNAME);
								account_expires    = (String)nodeService.getProperty(rsr.getNodeRef(), ContentModel.PROP_ACCOUNT_EXPIRES);
								account_expirydate = (String)nodeService.getProperty(rsr.getNodeRef(), ContentModel.PROP_ACCOUNT_EXPIRY_DATE);
								account_locked 	  = (String)nodeService.getProperty(rsr.getNodeRef(), ContentModel.PROP_ACCOUNT_LOCKED);
								Set<String> zones = authorityService.getAuthorityZones(username); 
								if (serviceRegistry.getAuthenticationService().getAuthenticationEnabled(username)){
									enabled="true";
								} else {
									enabled="false";
								}
								
								//logger.debug("processPerson: Setting user " + username + " is enabled="+ enabled);
								rl.setLine("account_enabled", getClassToColumnType().getProperty("boolean"), enabled.toString(), replacementTypes);
								rl.setLine("account_expires", getClassToColumnType().getProperty("boolean"), account_expires, replacementTypes);
								rl.setLine("account_expirydate", getClassToColumnType().getProperty("datetime"), account_expirydate, replacementTypes);
								rl.setLine("account_locked", getClassToColumnType().getProperty("boolean"), account_locked, replacementTypes);
								rl.setLine("zone", getClassToColumnType().getProperty("zone"), Utils.setToString(zones), replacementTypes);
							} catch (Exception e){
								logger.error("processPerson: That is weird, rl.setLine(noderef) crashed! " + rsr.getNodeRef());
								e.printStackTrace();
							}	
						
							
							int numberOfRows=0;
							if (dbhb.rowExists(stmt, rl)){
								numberOfRows = dbhb.updateIntoTable(stmt, rl);
								//logger.debug(numberOfRows+ " rows updated");
							} else {
								numberOfRows = dbhb.insertIntoTable(stmt, rl);
								//logger.debug(numberOfRows+ " rows inserted");
								
							} // end if/else
						} // end while
					} // end if !continueSearchCycle
				} catch (Exception e){
					e.printStackTrace();
				} finally {
					if (rs!=null){
						rs.close();
					}
				}
				
			} // end while continueSearchCycle
			
		} catch (Exception e) {
			logger.fatal("1#############################################");
			e.printStackTrace();
		} finally {
			rl.reset();
			try{
			     if(stmt!=null)
			        stmt.close();
			}catch(SQLException se2){
				logger.fatal("2#############################################");
		    }// nothing we can do
		}
    	logger.debug("Exit processPerson");
    }
    
    
    public void processAuditingExport(final String auditFeed){
    	logger.debug("enter processAuditingExport");
    	// http://code.google.com/a/apache-extras.org/p/alfresco-opencmis-extension/source/browse/trunk/src/main/java/org/alfresco/cmis/client/authentication/OAuthCMISAuthenticationProvider.java?r=19
    	if ((auditFeed!=null) && (!"".equals(auditFeed.trim()))){
	    	String tableName = auditFeed.toLowerCase();
	    	tableName = tableName.replaceAll("-", "_");
	    	//setTable(tableName);
	    	
	    	dbhb.createEmptyTables(tableName);
	    	String timestamp = getLastTimestamp(tableName);
	    	timestamp = timestamp.replaceAll("T", " ").trim();
	    	
	    	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    	
	    	Date startDate=null;
			try {
				//logger.debug("Parsable date?=" + timestamp);
				startDate = format.parse(timestamp);
			} catch (java.text.ParseException e1) {
				e1.printStackTrace();
			}
	    	Long fromTime = startDate.getTime() +1; // +1 otherwise you always get the last one double
	    	
	    	ReportLine rl = new ReportLine(tableName);
	    	Properties replacementTypes = getReplacementDataType();
	    	Statement stmt = null;
	    	
			
	    	
	        try
	        {
	        	//logger.debug("processAuditingExport: Prepping table columns");
	    		Properties definition = new Properties();
	    		definition.setProperty("timestamp", getClassToColumnType().getProperty("datetime","-"));
	    		definition.setProperty("username", getClassToColumnType().getProperty("name","-"));
	    		//definition.setProperty("success", getClassToColumnType().getProperty("boolean","-"));
	    		setTableDefinition(definition, tableName);
	    		//logger.debug("processAuditingExport: Done prepping table columns");
	        	
	    		
	    		Connection conn = dbhb.getConnection();
				conn.setAutoCommit(true);
				stmt = conn.createStatement();
				int maxAmount = 50000;
				/*
				try{
					maxAmount = Integer.parseInt(globalProperties.getProperty(Constants.property_audit_maxResults, "50000"));
				} catch (NumberFormatException nfe){
					// nothing
				}
				*/
				
				
	        	// Replace this JSON stuff with the Real Thing:
	        	// http://svn.alfresco.com/repos/alfresco-open-mirror/alfresco/HEAD/root/projects/repository/source/java/org/alfresco/cmis/changelog/CMISChangeLogServiceImpl.java
				/*
				if (!auditService.isAuditEnabled(auditFeeds, ("/" + auditFeeds)))
		        {
		            logger.fatal("Auditing for " + auditFeeds + " is disabled!");
		        }
				*/
	        	EntryIdCallback changeLogCollectingCallback = new EntryIdCallback(true, stmt, rl, replacementTypes, tableName, auditFeed)
	            {
	        		private String validateColumnName(String tablename){
	        			logger.debug("enter validateColumnName: " + tablename);
	        			String origTablename = tablename;
	        			if (getCache().containsKey(tablename)){
	        				//logger.debug("Cache hit! returning: " + getCache().getProperty(tablename));
	        				return getCache().getProperty(tablename);
	        			}
	        			
	        			String replaceChars = "/-:;'.,;";
	        			int index=10;
	        			try{
		        			for (int i=0;i<replaceChars.length();i++){
		        				while (tablename.indexOf(replaceChars.charAt(i))>-1){
			        				index = tablename.indexOf(replaceChars.charAt(i));
			        				//logger.debug("Processing char=" + replaceChars.charAt(i) + " at index="+index + " | " + tablename);
			        				
			        				// the first
			        				if (index==0){
			        					tablename=tablename.substring(1, tablename.length());
			        				} else {
			        					// the last
				        				if (index==tablename.length()-1){
				        					tablename=tablename.substring(0, tablename.length()-2);
				        				} else {
				        					if ((index<(tablename.length()-1)) && (index>-1)){
				        						// otherwise in between
				        						tablename = tablename.substring(0,index) + "_" +
					            						tablename.substring(index+1, tablename.length());
				        					} else {
				        						//logger.fatal("That's weird: index=" + index + " and length="+ tablename.length()+ " " + tablename);
				        					}
				        				}
			        				} // end if/else index==0
		        				} // end while
		        				
		        			}
		        			// update the cache with our newly calculated replacement string
		        			if (!getCache().containsKey(tablename)){
		        				this.addToCache(origTablename, tablename);
		        			}
		        		} catch (Exception e){
		        			logger.fatal("That's weird: index=" + index + " and length="+ tablename.length()+ " " + tablename);
		        			e.getMessage();
		        			
		        		}
	        			
	        			logger.debug("exit validateColumnName: " + tablename.toLowerCase());
	        			return tablename.toLowerCase();
	        		}
	        		
	                @Override
	                public boolean handleAuditEntry(Long entryId, String user, long time, Map<String, Serializable> values)
	                {
	                	// get the datemask in order to convert Date to String and back. 
	                	// Remind, the 'T' is missing, and a blank instead. Replace this later 
	                	// on in execution (see replaceAll(" ","T");!!)
	                	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	                	Date theDate = new Date(time);
	
	                    //process the values into the reporting DB;
	                	//logger.debug("id="+ entryId+" user="+ user+" time="+ time+" values"+ values);
	                	Set<String> theValues = null;
	                	if (values!=null){
		                	theValues = values.keySet();
		                	Properties definition = new Properties();
		    				for (String value : theValues){
		    					try{
			    		    		definition.setProperty(	validateColumnName(value), 
			    		    								getClassToColumnType().getProperty(
			    		    											"noderef",
			    		    											"-")
			    		    								);
			    		    		setTableDefinition(definition, getTableName());
		    					} catch (Exception e){
		    						logger.fatal("handleAuditEntry: UNABLE to process property from Values Map object");
		    					}
		    						
		    				} // end for
	                	} // end if values !=null
	    				try{
	    					getRl().reset();
	    					getRl().setLine("sys_node_uuid", 
	    									getClassToColumnType().getProperty("noderef"), 
	    									entryId.toString(), 
	    									getReplacementTypes());
	    					getRl().setLine("timestamp", 
	    									getClassToColumnType().getProperty("datetime"), 
	    									format.format(theDate).replaceAll(" ", "T"), 
	    									getReplacementTypes());
	    					getRl().setLine("username", 
	    									getClassToColumnType().getProperty("name"), 
	    									user, 
	    									getReplacementTypes());
	    					if (values!=null){
		    					for (String value : theValues){
		       						getRl().setLine(validateColumnName(value), 
		       										getClassToColumnType().getProperty("noderef"), 
		       										(String)values.get(value), 
		       										getReplacementTypes());
		        				} // end for value:theValues from Map
	    					} // end if
	    				} catch (Exception e){
	    					logger.error("Setting values in ResultLine object failed...");
	    					e.printStackTrace();
	    				}
	    				
	    				int numberOfRows=0;
	    				try {
							if (dbhb.rowExists(getStatement(), getRl())){
								numberOfRows = dbhb.updateIntoTable(getStatement(), getRl());
								//logger.debug(numberOfRows+ " rows updated");
							} else {
								numberOfRows = dbhb.insertIntoTable(getStatement(), getRl());
								//logger.debug(numberOfRows+ " rows inserted");
								
							}
						} catch (SQLException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
	                    return super.handleAuditEntry(entryId, user, time, values);
	                    //return true;
	                }
	            };
	            
	            //logger.debug("Before auditQuery, fromtime=" + fromTime + " | Timestamp=" + timestamp);
	            AuditQueryParameters params = new AuditQueryParameters();
	            params.setApplicationName(auditFeed);
	            params.setForward(true);
	            params.setFromTime(fromTime);
	            
	            
	            auditService.auditQuery(changeLogCollectingCallback, params, maxAmount);
	           // logger.debug("After auditQuery");
					
	        }
	        catch (ParseException e)
	        {
	        	e.printStackTrace();
	            throw new RuntimeException(e);
	        } catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				rl.reset();
				try{
				     if(stmt!=null)
				        stmt.close();
				}catch(SQLException se2){
					logger.fatal("2#############################################");
			    }// nothing we can do
			}
    	} // end if auditFeed !="" --> Prevent action if auditLog=enabled but no value specified
    	logger.debug("exit processAuditingExport");
    }
    
    /**
     * 
     * @param tableName
     */
    public void processGroups(String tableName){
    	logger.debug("enter processGroups");
    	dbhb.createEmptyTables(tableName);
    	
    	//setTable(tableName);
    	ReportLine rl = new ReportLine(tableName);
    	Properties replacementTypes = getReplacementDataType();
    	Statement stmt = null;
    	
    	try{
    		// first make sure our table has the right set of columns
    		//logger.debug("processGroups: Prepping table columns");
    		Properties definition = new Properties();
    		definition.setProperty("groupName", getClassToColumnType().getProperty("name","-"));
    		definition.setProperty("groupDisplayName", getClassToColumnType().getProperty("name","-"));
    		definition.setProperty("userName", getClassToColumnType().getProperty("name","-"));
    		definition.setProperty("zone", getClassToColumnType().getProperty("zone","-"));
    		setTableDefinition(definition, tableName);
    		//logger.debug("processGroups: Done prepping table columns");
    		
    		Connection conn = dbhb.getConnection();
			conn.setAutoCommit(true);
			stmt = conn.createStatement();
	    	Set<String> groupNames = authorityService.getAllAuthorities(AuthorityType.GROUP);
	    	for (String groupName : groupNames){
	    		String groupDisplayName=authorityService.getAuthorityDisplayName(groupName);
	    		Set<String> zones = authorityService.getAuthorityZones(groupName); 
	    		Set<String> userNames = authorityService.getContainedAuthorities(AuthorityType.USER, groupName, false);
	    		for (String userName : userNames){
	    			String userDisplayName = authorityService.getAuthorityDisplayName(userName);
	    			
	    			//logger.debug("Processing: " + groupDisplayName + " and user " + userDisplayName);
	    			
	    			// store groupname, groupDisplayName, userName
	    			rl.reset();
					
		    		//logger.debug("processUpdate: Before processProperties");
					try{
						rl.setLine("groupName", getClassToColumnType().getProperty("name"), groupName, replacementTypes);
						rl.setLine("groupDisplayName", getClassToColumnType().getProperty("name"), groupDisplayName, replacementTypes);
						rl.setLine("userName", getClassToColumnType().getProperty("name"), userName, replacementTypes);
						rl.setLine("zone", getClassToColumnType().getProperty("zone"), Utils.setToString(zones), replacementTypes);
						
					} catch (Exception e){
						logger.error("processUpdate: That is weird");
						e.printStackTrace();
					}
					
					int numberOfRows=0;
					numberOfRows = dbhb.insertIntoTable(stmt, rl);
	    		}
	    	}
    	} catch (Exception e) {
			logger.fatal("processGroups - terrible error:");
			e.printStackTrace();
		} finally {
			rl.reset();
			try{
			     if(stmt!=null)
			        stmt.close();
			}catch(SQLException se2){
				logger.fatal("2#############################################");
		    }// nothing we can do
		}
    	logger.debug("Exit processGroups");	
    	
    }
    
    public void processSitePerson(String tableName){
    	logger.debug("enter processSitePerson");
    	ReportLine rl = new ReportLine(tableName);
    	Properties replacementTypes = getReplacementDataType();
    	Statement stmt = null;
    	
    	try{
    		// first make sure our table has the right set of columns
    		//logger.debug("processSitePerson: Prepping table columns");
    		Properties definition = new Properties();
    		definition.setProperty("siteName", getClassToColumnType().getProperty("name","-"));
    		definition.setProperty("siteRole", getClassToColumnType().getProperty("name","-"));
    		definition.setProperty("siteRoleGroup", getClassToColumnType().getProperty("name","-"));
    		definition.setProperty("userName", getClassToColumnType().getProperty("name","-"));
    		//setTable(tableName);
        	setTableDefinition(definition, tableName);
    		//logger.debug("processSitePerson: Done prepping table columns");
    		
    		Connection conn = dbhb.getConnection();
			conn.setAutoCommit(true);
			stmt = conn.createStatement();
			
			List<String> roleList = siteService.getSiteRoles();
	
	    	List<SiteInfo> siteInfoList = siteService.listSites(null, null);
	    	for (SiteInfo siteInfo : siteInfoList){
	    		for (String role : roleList){
	    			//logger.debug("processSitePerson: getting role " + role +" from site " + siteInfo.getShortName());
	    			String roleGroup = siteService.getSiteRoleGroup(siteInfo.getShortName(), role);
	    			Map<String, String> someMap = siteService.listMembers(siteInfo.getShortName(), null, role, 0, true);
	    			Set<String> keys = someMap.keySet();
	    			for (String userName : keys){
	    				logger.debug("processSitePerson: " +
	        					siteInfo.getShortName() + " | " +
	        					roleGroup + " | " + 
	        					userName);
	    				
	    				rl.reset();
	    				//logger.debug("processSitePerson: Before processProperties");
						try{
							rl.setLine("siteName", getClassToColumnType().getProperty("name"), siteInfo.getShortName(), replacementTypes);
							rl.setLine("siteRole", getClassToColumnType().getProperty("name"), role, replacementTypes);
							rl.setLine("siteRoleGroup", getClassToColumnType().getProperty("name"), roleGroup, replacementTypes);
							rl.setLine("userName", getClassToColumnType().getProperty("name"), userName, replacementTypes);
						} catch (Exception e){
							//logger.error("processSitePerson: siteName; That is weird");
							e.printStackTrace();
						}
							
						int numberOfRows=0;
						numberOfRows = dbhb.insertIntoTable(stmt, rl);
	    			} // end for key in keySet
	    			
	    		} // end for role in roleList
	    	} // end for siteInfo in siteInfoList
    	} catch (Exception e) {
			logger.fatal("processSitePerson - terrible error:");
			e.printStackTrace();
		} finally {
			rl.reset();
			try{
			     if(stmt!=null)
			        stmt.close();
			}catch(SQLException se2){
				logger.fatal("2#############################################");
		    }// nothing we can do
		}
    	logger.debug("Exit processSitePerson");	

    	
    }
    
    public void processCategoriesAsPath(String table, final String rootName, String columnName) throws Exception{
    	logger.debug("Enter processCategoriesAsPath, rootName="+rootName);
    	logger.debug("Currently supporting 3 levels deep structures only!!");
    	if ((rootName!=null) && (!"".equals(rootName))){
	    	ReportLine rl = new ReportLine(table.toLowerCase());
	    	Statement stmt = null;
	    	Properties definition = new Properties(); // set of propname-proptype
			definition.setProperty(columnName, getClassToColumnType().getProperty("path","-"));
			definition.setProperty("label", getClassToColumnType().getProperty("label","-"));
			
			try{
				//setTable(table.toLowerCase());
				setTableDefinition(definition, table);
				stmt = dbhb.getConnection().createStatement();
				dbhb.getConnection().setAutoCommit(true);
				
		    	Collection<ChildAssociationRef> car = serviceRegistry.getCategoryService().getRootCategories(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, ContentModel.ASPECT_GEN_CLASSIFIABLE);
		    	for (ChildAssociationRef rootRef:car){
		    		NodeRef catRef = rootRef.getChildRef();
		    		String actualRootName = (String)serviceRegistry.getNodeService().getProperty(catRef, ContentModel.PROP_NAME);
		    		if (actualRootName.equals(rootName)){
		    			
		    			Collection<ChildAssociationRef> rcrs = serviceRegistry.getCategoryService().getChildren(catRef, Mode.SUB_CATEGORIES, Depth.IMMEDIATE);
		    			String labelValue="";
		    			// process werelddeel
		    			for (ChildAssociationRef regionChildRef:rcrs){
		    	    		NodeRef regionRef = regionChildRef.getChildRef();
		    	    		String regionPath = (String)serviceRegistry.getNodeService().getProperty(regionRef, ContentModel.PROP_NAME);
		    	    		storeRegionPath(stmt, rl, regionRef, regionPath, columnName, regionPath);
		    	    		rl.reset();
		    	    		
		    	    		// get Country refs
		    	    		Collection<ChildAssociationRef> ccrs = serviceRegistry.getCategoryService().getChildren(regionRef, Mode.SUB_CATEGORIES, Depth.IMMEDIATE);
		    	    		if (ccrs.size()>0){
		    	    	    	for (ChildAssociationRef countryChildRef:ccrs){
		    	    	    		NodeRef countryRef = countryChildRef.getChildRef();
		    	    	    		labelValue = (String)serviceRegistry.getNodeService().getProperty(countryRef, ContentModel.PROP_NAME);
		    	    	    		String countryPath = regionPath + "/" + labelValue;
		    	    	    		storeRegionPath(stmt, rl, countryRef, countryPath, columnName, labelValue);
		    	    	    		rl.reset();
		
		    	    	    		// get countryDiv refs
		    	    	    		Collection<ChildAssociationRef> cdcrs = serviceRegistry.getCategoryService().getChildren(countryRef, Mode.SUB_CATEGORIES, Depth.IMMEDIATE);
		    	    	    		if (cdcrs.size()>0){
		    	    	    	    	for (ChildAssociationRef countryDivChildRef:cdcrs){
		    	    	    	    		NodeRef countryDivRef = countryDivChildRef.getChildRef();
		    	    	    	    		labelValue = (String)serviceRegistry.getNodeService().getProperty(countryDivRef, ContentModel.PROP_NAME);
		    	    	    	    		String countryDivPath = countryPath + "/" + labelValue;
		    	    	    	    		storeRegionPath(stmt, rl, countryDivRef, countryDivPath, columnName, labelValue);
		    	    	    	    		rl.reset();
		    	    	    			}
		    	    	    		} // end if cdcrs
		    	    			} // end for
		    	    		} // end if ccrs
		    			} // end for
		    		} // end if rcrs
		    	} // on the search for root ref
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				rl.reset();
				try{
				     if(stmt!=null)
				        stmt.close();
				}catch(SQLException se2){
			    }// nothing we can do
			}
    	} // end if rootName !=null && rootName!=""
    	logger.debug("Exit processCategoriesAsPath");
    }
    
    /**
     * Gien the current noderef, retrieve the value of the Site name (will be stored 
     * as a column by default) 
     * @param currentRef
     * @return cm:name of the Site, or "" if no site found
     */
	private String getSiteName(NodeRef currentRef){
		//logger.debug("In getSiteName");
		String siteName = "";
		NodeRef primaryParent = null;
		
		if (currentRef!=null){
			
			NodeService nodeService = serviceRegistry.getNodeService();
			NodeRef rootNode = nodeService.getRootNode(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
						
			NodeRef siteRef = null;
			
			boolean siteTypeFound = nodeService.getType(currentRef).equals(SiteModel.TYPE_SITE);
			if (siteTypeFound){ siteRef = currentRef; }
			
			while ( !currentRef.equals(rootNode) && (!siteTypeFound)){
				//logger.debug("getTypeForNode: voor loopRef="+currentRef);
				currentRef = nodeService.getPrimaryParent(currentRef).getParentRef();
				//logger.debug("getTypeForNode: na   loopRef="+currentRef);
				siteTypeFound = nodeService.getType(currentRef).equals(SiteModel.TYPE_SITE);
				if (siteTypeFound){
					siteRef = currentRef;
					//logger.debug("getTypeForNode: Found QName node!");
				}
			}
			if (siteRef!=null){
				siteName=(String)serviceRegistry.getNodeService().getProperty(siteRef, ContentModel.PROP_NAME);
			}
		} // end if nodeRef!=null
		return siteName;
	}
	
	private void processUpdate(String table) throws Exception{
		logger.debug("Enter processUpdate table=" + table);

		//Statement stmt = null;
		//Properties tableDesc = dbhb.getTableDescription(stmt, table);
		
		logger.debug("************ Found " + queue.size() + " entries in " + table + " **************** " + method);
		ReportLine rl = new ReportLine(table);
		Properties replacementTypes = getReplacementDataType();
		
		Connection conn = dbhb.getConnection();
		Statement stmt=null;
		try{
			conn.setAutoCommit(true);
			stmt = conn.createStatement();
			if (stmt == null){
				throw new Exception("Something wrong with DB connection!!");
			}

			int queuesize = queue.size();
			for (int q=0;q<queue.size();q++){
				//for (NodeRef nodeRef : queue){
				
				NodeRef nodeRef = queue.get(q);	
				
				if (logger.isDebugEnabled()){
					String name = (String)serviceRegistry.getNodeService().getProperty(nodeRef, ContentModel.PROP_NAME);
					logger.debug("getTableDefinitionFromQueue: "+q+"/"+queuesize+ ": " + name);
				}
				
				try{
					rl = processPropertyValues(rl, nodeRef, getBlacklist());
				} catch (Exception e){
					logger.error("processUpdate: That is weird, processPropertyValues crashed! " + nodeRef);
					e.printStackTrace();
				}
				
				try{
					rl = processAssociationValues(rl, nodeRef, getBlacklist());
				} catch (Exception e){
					logger.error("processUpdate: That is weird, processAssociationValues crashed! " + nodeRef);
					e.printStackTrace();
				}
				
				try{
					rl.setLine("noderef", getClassToColumnType().getProperty("noderef"), nodeRef.toString(), replacementTypes);
				} catch (Exception e){
					logger.error("processUpdate: That is weird, rl.setLine(noderef) crashed! " + nodeRef);
					e.printStackTrace();
				}
				
				Path path;
				String displayPath="";
				try{
					path = serviceRegistry.getNodeService().getPath(nodeRef);
					displayPath = toDisplayPath(path);
					rl.setLine("path", getClassToColumnType().getProperty("path"), displayPath, replacementTypes);
				} catch (Exception e){
					// it does not have a path. Bad luck. Don't crash (versionStore?!)
				}
				
				String site =""; 
				try{
					site = getSiteName(nodeRef);
					rl.setLine("site", getClassToColumnType().getProperty("site"), site, replacementTypes);
				} catch (Exception e) {
					// it is not in a site. Bad luck. Don't crash (versionStore?!)
				}
				QName myType = serviceRegistry.getNodeService().getType(nodeRef);
				if (serviceRegistry.getDictionaryService().isSubClass(myType, ContentModel.TYPE_CONTENT)){
					long size = 0;
					String sizeString="0";
					try{
						size = serviceRegistry.getFileFolderService()
										.getFileInfo(nodeRef).getContentData().getSize();
						
						if (size==0){
							sizeString = "0";
						} else {
							sizeString = Long.toString(size);
						}
						rl.setLine("size", getClassToColumnType().getProperty("size"), sizeString,replacementTypes);
					} catch (Exception e) {
						logger.debug("Huh, no size?");
						sizeString="0";
					}
					
					try{
						String mimetype = serviceRegistry.getFileFolderService()
								.getFileInfo(nodeRef).getContentData().getMimetype();
						if (mimetype==null) mimetype="NULL";
						rl.setLine("mimetype", getClassToColumnType().getProperty("mimetype"), mimetype, replacementTypes);
					} catch (Exception e) {
						logger.debug("Huh, no mimetype?");
					}
					
					try{
						if (versionNodes.containsKey(nodeRef.toString())){
							//logger.debug("Setting nodeRef to orig_noderef - VERSION!!!");
							rl.setLine("orig_noderef", getClassToColumnType().getProperty("noderef"), (String)versionNodes.getProperty(nodeRef.toString(),""), replacementTypes);
						} else {
							rl.setLine("orig_noderef", getClassToColumnType().getProperty("noderef"), nodeRef.toString(), replacementTypes);
							//logger.debug("Setting currentRef to orig_noderef!!!");
						}
							
						/*
						if (nodeRef.toString().contains("version2Store")){
							logger.debug("VERSION!!!");
							//NodeRef workspaceRef = getWorkspaceNodeRefForVersion2StoreNodeRef(nodeRef);
							//rl.setLine("orig_noderef", getClassToColumnType().getProperty("noderef"), workspaceRef.toString());
						}
						*/
							
						//	} // end if content
					} catch (Exception e){
						// don't crash... (versionStore?!)
					}
				} // end if
				else {
					logger.debug(myType.toString() + " is no content subclass!");
				}
				
				int numberOfRows;
				logger.debug("Current method=" + this.method);
				try{ //SINGLE_INSTANCE, 
					//logger.debug(method + " ##### " + rl.size());
					if ( (rl.size()>0) /* && (rl.getValue("sys_node_uuid").length()>5)*/){
	    				//logger.debug("method="+method+" && row exists?");
	    				
						if (this.method.equals(Constants.INSERT_ONLY) ){
	    					//if (logger.isDebugEnabled()) logger.debug("Going INSERT_ONLY");
	    					
	    					numberOfRows = dbhb.insertIntoTable(stmt, rl);
	    					//logger.debug(numberOfRows+ " rows inserted");
	    				}
	    				
	    				// -------------------------------------------------------------
	    				
	    				if (this.method.equals(Constants.SINGLE_INSTANCE) ) {
	    					//if (logger.isDebugEnabled()) logger.debug("Going SINGLE_INSTANCE");
	    					
	    					if (dbhb.rowExists(stmt, rl)){
	    						numberOfRows = dbhb.updateIntoTable(stmt, rl);
	    						//logger.debug(numberOfRows+ " rows updated");
	    					} else {
	    						numberOfRows = dbhb.insertIntoTable(stmt, rl);
	    						//logger.debug(numberOfRows+ " rows inserted");
	    						
	    					}
	    					
	    				}
	    				
	    				// -------------------------------------------------------------
	    				
	    				if (this.method.equals(Constants.UPDATE_VERSIONED)) {
	    					if (logger.isDebugEnabled()) logger.debug("Going UPDATE_VERSIONED");
	    					if (dbhb.rowExists(stmt, rl)){
		    					numberOfRows = dbhb.updateVersionedIntoTable(stmt, rl);
		    					//logger.debug(numberOfRows+ " rows updated");
	    					} else {
	    						numberOfRows = dbhb.insertIntoTable(stmt, rl);
	    						//logger.debug(numberOfRows+ " rows inserted");
	    						
	    					}
	    				}
					} // end if rl.size>0
	
				} catch (Exception e){
					logger.fatal(e);
					e.printStackTrace();
				} finally {
					rl.reset();
				}
			} // end for scriptnode in queue
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
		logger.debug("Exit processUpdate");
	}

	
	/**
	 * Get the full list of namespaces and their short form. Cache for future need.
	 * @return
	 */
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
		return this.namespaces;
	}
	
	
	/**
	 * Given the input string, replace all namespaces where possible. 
	 * @param namespace
	 * @return string whith replaced full namespaces into short namespace definitions
	 */
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
	
	/**
	 * prefixes a string with some char, until the length of the 
	 * string is equal to len 
	 * @param inString String to be prefixed
	 * @param len the size the new string has to become
	 * @character the character the inString to prefix with
	 * @return
	 */
	private String prefix(String inString, int len, String character){
		String returnString = inString;
		if (inString!=null){
			while (returnString.length()<len){
				returnString = character+returnString;
			}
		}
		return returnString;
	}
	
	/*
	 * Prepare to remove
	private String prefix(int intString, int len, String character){
		return prefix(Integer.toString(intString), len, character);
	}
	*/
	
	private String getPropertyValue(final NodeRef nodeRef, 
									final QName qname, 
									final String dtype, 
									final boolean multiValued){
		logger.debug("Enter getPropertyValue");
		String returnValue = "";
		Serializable s = serviceRegistry.getNodeService().getProperty(nodeRef, qname);
		// Tjarda: Check of s!=null wel valide is! Bij Tags en Categories
		if (multiValued && !"category".equals(dtype)){
			ArrayList<Object> values = new ArrayList();
			
			values = (ArrayList)serviceRegistry.getNodeService().getProperty(nodeRef, qname);
			
			if ((values!=null) && (!values.isEmpty()) && (values.size()>0)){
				
				if (dtype.equals("date") || dtype.equals("datetime")){
					SimpleDateFormat dateformat = new SimpleDateFormat(Constants.DATE_FORMAT_DATABASE);
					Calendar c = Calendar.getInstance();
					
					for (int v=0;v<values.size();v++){
						returnValue += dateformat.format((Date)values.get(v)) + multivalue_seperator;		
					}
				}
				
				if (dtype.equals("id") || dtype.equals("long")){
					for (int v=0;v<values.size();v++){
						returnValue += Long.toString((Long)values.get(v)) + multivalue_seperator;		
					}
				}
				
				if (dtype.equals("int")){
					for (int v=0;v<values.size();v++){
						returnValue += Integer.toString((Integer)values.get(v)) + multivalue_seperator;		
					}
				}
				
				if (dtype.equals("float") || dtype.equals("double") ){
					for (int v=0;v<values.size();v++){
						returnValue += Double.toString((Double)values.get(v)) + multivalue_seperator;		
					}
				}
				
				if (dtype.equals("boolean")){
					for (int v=0;v<values.size();v++){
						returnValue += Boolean.toString((Boolean)values.get(v)) + multivalue_seperator;		
					}
				}
			
				if (dtype.equals("text")){
					for (int v=0;v<values.size();v++){
						returnValue += (String)values.get(v) + multivalue_seperator;		
					}
				}
				
				if (dtype.equals("noderef")){
					for (int v=0;v<values.size();v++){
						returnValue += values.get(v).toString() + multivalue_seperator;		
					}
				}
				
				if (returnValue.equals("")){
					for (int v=0;v<values.size();v++){
						returnValue += (String)values.get(v) + multivalue_seperator;		
					}
				}
			} 
			// end multivalue
		} else {
			if ((s!=null) && !"category".equals(dtype)){

				if (dtype.equals("date") || dtype.equals("datetime")){
					SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
					Calendar c = Calendar.getInstance();
					c.setTimeInMillis( ((Date)s).getTime() );
					returnValue = dateformat.format((Date)s);
					//returnValue = c.YEAR + "/"+ prefix(c.MONTH+1, 2, "0") + "/"+ prefix(c.DAY_OF_MONTH, 2, "0") + "T" + prefix(c.HOUR_OF_DAY, 2, "0")+":"+prefix(c.MINUTE, 2, "0")+":"+prefix(c.SECOND, 2, "0"); 
				}
				
				if (dtype.equals("id") || dtype.equals("long")){
					returnValue = Long.toString((Long)s);
				}

				if (dtype.equals("int")){
					returnValue = Integer.toString((Integer)s);
				}
				
				if (dtype.equals("float") || dtype.equals("double") ){
					returnValue = Double.toString((Double)s);
				}
				
				if (dtype.equals("boolean")){
						returnValue = Boolean.toString((Boolean)s);
				}
			
				if (dtype.equals("text")){
					returnValue = s.toString();
				}
				
				if (dtype.equals("noderef")){
					returnValue = s.toString();
				}
				
				if (returnValue.equals("")){
					returnValue = s.toString();
				}
			}
		} // end single valued
		/*
		if (qname.toString().endsWith("taggable")) {
			logger.error("I am a taggable!");
			List<String> tags = serviceRegistry.getTaggingService().getTags(nodeRef);
			logger.error("Found " + tags.size() + " tags!");
			for (String tag : tags){
				logger.error("processing tag: " + tag);
				if (returnValue.length()>0) returnValue+=",";
				returnValue+=tag;
			}
		} // end taggable
		*/
		
		if (dtype.equals("category")){
			logger.debug("I am a category!");
			List<NodeRef> categories = (List<NodeRef>) nodeService.getProperty(nodeRef, qname);
			if (categories != null){
				
				for (NodeRef cat : categories){
					String catName = nodeService.getProperty(
							cat, ContentModel.PROP_NAME).toString(); 
					
					if (returnValue.length()>0) returnValue+=",";
					returnValue+= catName;
				} // end for
			} // end if categories != null
		} // end category
	
		logger.debug("Exit getPropertyValue, returning: " + returnValue);
		return returnValue; 
	}

// ----------------------------------------------------------------------------
// actual script methods
// ----------------------------------------------------------------------------
    
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

    public boolean tableIsRunning(String tableName){
    	logger.debug("Starting tableIsRunning: " + tableName);
    	return dbhb.tableIsRunning(tableName);
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
	 * Required: enable debug logging of this class!
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

	
    /** 
     * Log all reporting related key/values from alfresco-global.properties
     * Required: enable debug logging of this class!
     */
    public void logAllProperties(){
	 
    	String returnString = "Size: " + getGlobalProperties().size()+" - "; 
    	Enumeration keys = getGlobalProperties().keys();
   		while (keys.hasMoreElements()){
   			String key = (String)keys.nextElement();
   			if (key.contains("reporting.")){
	   			logger.debug(key+"="+getGlobalProperties().getProperty(key));
	   			returnString += key+"="+getGlobalProperties().getProperty(key)+"\n";
   			}
   		}
    }
	
}
