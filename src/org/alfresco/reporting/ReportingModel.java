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

import org.alfresco.service.namespace.QName;

public class ReportingModel {

	public static final String REPORTING_URI = "http://www.alfresco.org/model/reporting/1.0";
	public static final String REPORTING_PREFIX = "reporting";
	public static final String SYSTEM_URI = "http://www.alfresco.org/model/system/1.0";
	
	public static final QName TYPE_REPORTING_HARVEST_DEFINITION= 
			QName.createQName(REPORTING_URI,"harvestDefinition");
	
	public static final QName TYPE_REPORTING_ROOT = 
			QName.createQName(REPORTING_URI,"reportingRoot");

	/**
	 * QName for the Folder containing reports that are executed by a schedule. 
	 * This type is automatcally specialized if a folder is created under a parent
	 * having aspect ASCPECT_REPORTING_REPORTING
	 */
	public static final QName TYPE_REPORTING_CONTAINER = 
				QName.createQName(REPORTING_URI,"reportingContainer");
	
	/**
	 * QName for the executable Pentaho or JarsperSoft report. If a Document is 
	 * created underneath a folder typed TYPE_REPORTING_CONTAINER, it will be
	 * automaticlaly specuialized into this type
	 */
	public static final QName TYPE_REPORTING_REPORTTEMPLATE = 
				QName.createQName(REPORTING_URI,"reportTemplate");
		
	/**
	 * This is the 'parent aspect' Any folder directly below 
	 * this aspect will be specialized into a TYPE_REPORTING_CONTAINER
	 */
	public static final QName ASPECT_REPORTING_REPORTING_ROOTABLE = 
				QName.createQName(REPORTING_URI, "reportingRootable");
	
	
	public static final QName ASPECT_REPORTING_EXECUTIONRESULT =
			QName.createQName(REPORTING_URI, "executionResult");
	/**
	 * 
	 */
	public static final QName PROP_REPORTING_GLOBAL_EXECUTION_ENABLED  = 
				QName.createQName(REPORTING_URI, "globalExecutionEnabled");
	
	public static final QName PROP_REPORTING_HARVEST_ENABLED  = 
			QName.createQName(REPORTING_URI, "harvestEnabled");

	public static final QName PROP_REPORTING_ROOT_QUERY_LANGUAGE =
			QName.createQName(REPORTING_URI, "globalExecutionLanguage");
	/**
	 * contains the properties-like key=value notation of placeholder 
	 * names with related (lucene) queries
	 */
	public static final QName PROP_REPORTING_TARGET_QUERIES = 
				QName.createQName(REPORTING_URI, "targetQueries");
	
	public static final QName PROP_REPORTING_OUTPUTEXTENSION_EXCEL = 
			QName.createQName(REPORTING_URI, "outputExtensionExcel");
	public static final QName PROP_REPORTING_OUTPUTEXTENSION_PDF = 
			QName.createQName(REPORTING_URI, "outputExtensionPdf");
	/** 
	 * Containerable is the mandatory aspect on a 
	 * reporting:reportingContainer folder
	 */
	public static final QName ASPECT_REPORTING_CONTAINERABLE     = 
				QName.createQName(REPORTING_URI, "reportingContainerable");
	public static final QName PROP_REPORTING_EXECUTION_ENABLED   = 
				QName.createQName(REPORTING_URI, "executionEnabled");
	public static final QName PROP_REPORTING_EXECUTION_FREQUENCY = 
				QName.createQName(REPORTING_URI, "executionFrequency");
	public static final QName PROP_REPORTING_TARGET_PATH  = 
				QName.createQName(REPORTING_URI, "targetPath");
	public static final QName PROP_REPORTING_SUBSTITUTION = 	
				QName.createQName(REPORTING_URI, "substitution");
	public static final QName ASSOC_REPORTING_TARGET_NODE  = 
			QName.createQName(REPORTING_URI, "targetNode");

	
	/**
	 * Reportable is the mandatory aspect on a reporting:report document
	 */
	public static final QName ASPECT_REPORTING_REPORTABLE = 
				QName.createQName(REPORTING_URI, "reportable");
	public static final QName PROP_REPORTING_REPORTING_FORMAT = 
				QName.createQName(REPORTING_URI, "outputFormat");
	public static final QName PROP_REPORTING_REPORTING_VERSIONED = 
			QName.createQName(REPORTING_URI, "outputVersioned");

	public static final QName PROP_SYSTEM_NODE_DBID = 
			QName.createQName(SYSTEM_URI, "node-dbid");
}
