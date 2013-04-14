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

package org.alfresco.reporting.execution;

import java.util.Properties;

import org.alfresco.model.ContentModel;
import org.alfresco.reporting.ReportingModel;
import org.alfresco.reporting.action.executer.ReportRootExecutor;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ReportingRoot {
	private NodeRef nodeRef;
	private boolean globalExecutionEnabled;
	private boolean harvestEnabled;
	private Properties targetQueries;
	private String rootQueryLanguage;
	private String outputExtensionExcel;
	private String outputExtensionPdf;
	private String name;
	
	private static Log logger = LogFactory.getLog(ReportingRoot.class);
			
	public ReportingRoot(NodeRef reportingRootRef){
		this.nodeRef = reportingRootRef;
	}
	
	public void setName(String name){
		this.name=name;
	}
	
	public String getName(){
		return name;
	}
	
	/**
	 * @return the nodeRef
	 */
	public NodeRef getNodeRef() {
		return nodeRef;
	}
	
	public void setGlobalExecutionEnabled(boolean executionEnabled) {
		this.globalExecutionEnabled=executionEnabled;
	}
	
	/**
	 * @return the globalExecutionEnabled
	 */
	public boolean isGlobalExecutionEnabled() {
		return globalExecutionEnabled;
	}
	
	public void setHarvestEnabled(boolean harvestEnabled) {
		this.harvestEnabled=harvestEnabled;
	}
	/**
	 * @return the harvestEnabled
	 */
	public boolean isHarvestEnabled() {
		return harvestEnabled;
	}
	
	/**
	 * @return the targetQueries
	 */
	public Properties getTargetQueries() {
		return targetQueries;
	}
	
	public void setTargetQueries(String queries) {
		if (logger.isDebugEnabled())
			logger.debug("enter getAllTargetQueries");
		Properties returnProps = new Properties();
		String[] lines = queries.split("\\n"); 
		for (String line : lines){
			line = line.trim();
			if (!line.trim().startsWith("#") && 
					line.indexOf("=")>1){
				int i = line.indexOf("=");
				String key = line.substring(0,i);
				String value = line.substring(i+1);
				returnProps.put(key, value);
				if (logger.isDebugEnabled()) 
						logger.debug("getAllTargetQueries: Storing " + key + "=" + value);
			} // end if
		} // end for
		
		if (logger.isDebugEnabled())
				logger.debug("exit getTargetQueries, size=" + returnProps.size());
		this.targetQueries=returnProps;
	}
	
	
	public void setRootQueryLanguage(String language) {
		this.rootQueryLanguage = language;
	}
	
	/**
	 * @return the rootQueryLanguage
	 */
	public String getRootQueryLanguage() {
		return rootQueryLanguage;
	}
	
	public void setOutputExtensionExcel(String extension) {
		this.outputExtensionExcel=extension;
	}
	/**
	 * @return the outputExtensionExcel
	 */
	public String getOutputExtensionExcel() {
		return outputExtensionExcel;
	}
	
	
	
	public void setOutputExtensionPdf(String extension){
		this.outputExtensionPdf=extension;
	}
	/**
	 * @return the outputExtensionPdf
	 */
	public String getOutputExtensionPdf() {
		return outputExtensionPdf;
	}
	
	
}
