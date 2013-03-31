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

 package org.alfresco.reporting;

import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.reporting.execution.ReportTemplate;
import org.alfresco.reporting.execution.ReportingContainer;
import org.alfresco.reporting.execution.ReportingRoot;
import org.alfresco.service.cmr.repository.AssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class ReportingHelper {
	private NodeService nodeService;
	private NamespaceService namespaceService;
	
	private Properties namespacesShortToLong = null;
	private static Log logger = LogFactory.getLog(ReportingHelper.class);
	
	
	/**
	 * Navigate up in the folder structure a ReportingRoot is found.
	 * 
	 * @param currentNode
	 * @return NodeRef of the first ReportingRoot the currentNode is a child of
	 */
	public NodeRef getReportingRoot(final NodeRef currentNode){
		return getParentByType(currentNode, ReportingModel.TYPE_REPORTING_ROOT);
	}
	
	
	/**
	 * Navigate up in the folder structure a ReportingContainer is found.
	 * 
	 * @param currentNode
	 * @return NodeRef of the first ReportingContainer the currentNode is a child of
	 */
	public NodeRef getReportingContainer(final NodeRef currentNode){
		return getParentByType(currentNode, ReportingModel.TYPE_REPORTING_CONTAINER);
	}
	
	/**
	 * Navigate up in the folder structure until either the object type with the
	 * given QName-type (targetType) is found, or we touch the repositoryRoot.
	 * 
	 * @param currentNode
	 * @param targetType
	 * @return NodeRef of the first parent typed with the given QName
	 */
	private NodeRef getParentByType(final NodeRef currentNode, final QName targetType){
		// consider managing a cache of noderef-to-noderef relations per QName
		
		logger.debug("Enter getParentByType");
		NodeRef returnNode = null;
		  
		if (currentNode!=null){
		
			returnNode = AuthenticationUtil.runAs(new RunAsWork<NodeRef>() {
				public NodeRef doWork() throws Exception {
					
					NodeRef rootNode = nodeService.getRootNode(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
					
					logger.debug("getParentByType: rootNode="+rootNode);
					logger.debug("getParentByType: nodeRef="+currentNode);
					
					NodeRef returnNode = null;
					NodeRef loopRef = currentNode;
					boolean siteTypeFound = false;
					while ( !loopRef.equals(rootNode) && !siteTypeFound){
						//logger.debug("getTypeForNode: voor loopRef="+loopRef);
						loopRef = nodeService.getPrimaryParent(loopRef).getParentRef();
						//logger.debug("getTypeForNode: na   loopRef="+loopRef);
						siteTypeFound = nodeService.getType(loopRef).equals(targetType);
						if (siteTypeFound){
							returnNode = loopRef;
							logger.debug("getParentByType: Found QName node!");
						}
					}
					return returnNode;
				} // end do work
			}, AuthenticationUtil.getSystemUserName());
		 
		} // end if nodeRef!=null
		logger.debug("Exit getParentByType: " + returnNode);
		return returnNode;
    }
	
	/**
	 * Given the selected value in the picklist in the UI, return the JAVA
	 * API string for the particular language, as defined in he SearchService
	 * 
	 * @param objectLanguage UI search language
	 * @return JAVA API name for the language
	 */
	public String getSearchLanguage(String objectLanguage){
		String returnString = SearchService.LANGUAGE_LUCENE;
		if (objectLanguage!= null){
			if ("Full Text Search".equalsIgnoreCase(objectLanguage.trim()))
				returnString = SearchService.LANGUAGE_FTS_ALFRESCO;
			if ("Lucene".equalsIgnoreCase(objectLanguage.trim()))
				returnString = SearchService.LANGUAGE_LUCENE;
			if ("XPath".equalsIgnoreCase(objectLanguage.trim()))
				returnString = SearchService.LANGUAGE_XPATH;
		} // end if objectLanguage != null
		return returnString;
	}
	
	/** 
	 * I cannot get these objects get created with the magic of the node service
	 * on board. Therefore this method will finalize the construction of the object 
	 * by pulling the content out of the Alfresco object, and setting the object props
	 * 
	 * @param reportingRoot
	 */
	public void initializeReportingRoot(ReportingRoot reportingRoot){
		reportingRoot.setGlobalExecutionEnabled( 
				(Boolean)nodeService.getProperty(
						reportingRoot.getNodeRef(), 
						ReportingModel.PROP_REPORTING_GLOBAL_EXECUTION_ENABLED));
		
		reportingRoot.setHarvestEnabled(
				(Boolean)nodeService.getProperty(
						reportingRoot.getNodeRef(), 
						ReportingModel.PROP_REPORTING_HARVEST_ENABLED));
		
		reportingRoot.setRootQueryLanguage( 
				(String)nodeService.getProperty(
						reportingRoot.getNodeRef(), 
						ReportingModel.PROP_REPORTING_ROOT_QUERY_LANGUAGE));
		
		reportingRoot.setOutputExtensionExcel(
				(String)nodeService.getProperty(
						reportingRoot.getNodeRef(), 
						ReportingModel.PROP_REPORTING_OUTPUTEXTENSION_EXCEL));
		
		reportingRoot.setOutputExtensionPdf(
				(String)nodeService.getProperty(
						reportingRoot.getNodeRef(), 
						ReportingModel.PROP_REPORTING_OUTPUTEXTENSION_PDF));
		
		reportingRoot.setTargetQueries(
				(String)nodeService.getProperty(
						reportingRoot.getNodeRef(), 
						ReportingModel.PROP_REPORTING_TARGET_QUERIES));
		
		reportingRoot.setName(
				(String)nodeService.getProperty(
						reportingRoot.getNodeRef(), 
						ContentModel.PROP_NAME));
		
	}
	
	public void initializeReportingContainer(ReportingContainer reportingContainer){
		reportingContainer.setExecutionEnabled(
				(Boolean)nodeService.getProperty(
						reportingContainer.getNodeRef(), 
						ReportingModel.PROP_REPORTING_EXECUTION_ENABLED));
		
		reportingContainer.setExecutionFrequency( 
				(String)nodeService.getProperty(
						reportingContainer.getNodeRef(), 
						ReportingModel.PROP_REPORTING_EXECUTION_FREQUENCY));
		

		
		reportingContainer.setName(
				(String)nodeService.getProperty(
						reportingContainer.getNodeRef(), 
						ContentModel.PROP_NAME));

	}
	
	public void initializeReport(ReportTemplate report){
		report.setName(
				(String)nodeService.getProperty(
						report.getNodeRef(), 
						ContentModel.PROP_NAME));
		
		report.setOutputFormat(
				(String)nodeService.getProperty(
						report.getNodeRef(), 
						ReportingModel.PROP_REPORTING_REPORTING_FORMAT));
	
		report.setOutputVersioned(
				(Boolean)nodeService.getProperty(
						report.getNodeRef(), 
						ReportingModel.PROP_REPORTING_REPORTING_VERSIONED));
		report.setReportingDocument(nodeService.hasAspect(
						report.getNodeRef(), 
						ReportingModel.ASPECT_REPORTING_REPORTABLE));
		
		report.setTargetPath(
				(String)nodeService.getProperty(
						report.getNodeRef(), 
						ReportingModel.PROP_REPORTING_TARGET_PATH));
				
		report.setSubstitution(
				(String)nodeService.getProperty(
						report.getNodeRef(), 
						ReportingModel.PROP_REPORTING_SUBSTITUTION));
		
		 
		List<AssociationRef> ar = nodeService.getTargetAssocs(
				report.getNodeRef(), 
				 ReportingModel.ASSOC_REPORTING_TARGET_NODE);
		
		if (!ar.isEmpty()){
			report.setTargetNode(ar.get(0).getTargetRef());
		}
	}
	
	/**
	 * returns a Properties object with shortValue = longValue QName
	 * This enables the system to get a short key for a long QName
	 * @return
	 */
	public Properties getNameSpacesShortToLong(){
		if (this.namespacesShortToLong == null){
			this.namespacesShortToLong = new Properties();
			Collection<String> keys = namespaceService.getPrefixes(); 
			for (String shortValue : keys){
				String longValue = namespaceService.getNamespaceURI(shortValue);
				this.namespacesShortToLong.setProperty(shortValue, longValue);
				logger.debug("Replacing short value: " + 
							shortValue + 
							" into long value: " + 
							longValue);
			}
		} 
		return this.namespacesShortToLong;
	}
	
	/**
	 * Method to convert a short notation into a long notation to 
	 * be able to construct a QName object from that
	 * 
	 * @param inString in the form cm:name or st:sitePreset
	 * @return a valid QName representing the property (or type, 
	 * or aspect) in the Java World
	 */
	public QName replaceShortQNameIntoLong(String inString){
		Properties namespaces = getNameSpacesShortToLong();
		String namespace = inString.split(":")[0];
		String property  = inString.split(":")[1];
		String longName = namespaces.getProperty(namespace);
		logger.debug("Creating long QName: "+longName);
		QName longQName = QName.createQName(longName,property);
		return longQName;
	}
	
	/**
	 * Given the input string, replace all namespaces where possible. 
	 * @param namespace
	 * @return string whith replaced full namespaces into short namespace definitions
	 */
	/*
	public String replaceNameSpaces(String namespace) {
		// use regular expressions to do a global replace of the full namespace into the short version.
		Properties p = getNameSpacesShortToLong();
		Enumeration keys = p.keys(); 
		while (keys.hasMoreElements()){
			String into = (String)keys.nextElement();	
			String from = p.getProperty(into);
			namespace=namespace.replace(from, into);
		}
		//namespace=namespace.replace("-","_");
		  
		return namespace;
	}
	*/
	
	/**
	 * General Setter for the Alfresco NodeService
	 * @param nodeService
	 */
	public void setNodeService(NodeService nodeService)	{
	    this.nodeService = nodeService;
	}
	
	public void setNamespaceService(NamespaceService namespaceService) {
		this.namespaceService = namespaceService;
	}
}
