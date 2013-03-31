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

public class Constants {
	// three methods of using the reporting database
	public final static String INSERT_ONLY = "INSERT_ONLY";
	public final static String SINGLE_INSTANCE = "SINGLE_INSTANCE";
	public final static String UPDATE_VERSIONED = "UPDATE_VERSIONED";
	public final static String QUERYLANGUAGE = "Lucene";
	public final static String SEPARATOR = "~";
	
	public final static String property_jndiName = "reporting.db.jndiName";
	public final static String property_audit_maxResults = "reporting.harvest.audit.maxResults";
	public final static String property_blacklist = "reporting.harvest.blacklist"; // the keys that -can- be blocked
	public final static String property_blockkeys = "reporting.harvest.blockkeys"; // the keys that *must* be blocked by definition
	public final static String property_storelist = "reporting.harvest.stores";
	
	//location of configuration files
	/**
	 * Properties file containing mapping of Alfresco types like text, noderef, date
	 * into SQL types.  This file is shipped with the reporting tool (and mandatory). 
	 * 
	 * Sample:
	 * text=VARCHAR(500)
	 * noderef=VARCHAR(100)
	 * date=DATE
	 * datetime=TIMESTAMP
	 */
	public final static String REPORTING_PROPERTIES = "alfresco/module/org.alfresco.reporting/reporting-model.properties";
	
	/**
	 * Properties file containing custom mapping of named properties (short notation) 
	 * into SQL datatypes/column definitions. The rough mapping maps each Alfresc text 
	 * field into say VARCHAR(500). There are some properties that are known to always 
	 * fit in smaller or bigger sizes.
	 * 
	 * Applicable to text based properties only (or properties that map on VARCHAR() columns). 
	 * Purpose: work around the max length of a database row. Expectation MySQL: 64.000 bytes 
	 * devided by 4 (max number of bytes needed to represent any UTF8 character). Using large sets
	 * of custom properties in a single reporting table can cause problems if all default values are used. 
	 * 
	 *  Sample:
	 *  cm_versionLabel=VARCHAR(10)
	 *  child_noderef=VARCHAR(1000)
	 *  
	 */
	public final static String REPORTING_CUSTOM_PROPERTIES = "alfresco/extension/reporting-custom-model.properties";
	
	
	/**
	 * character to be used when concatinating multivalue property values 
	 * into one string-like representation in the reporting database
	 */
	public final static String MULTIVALUE_SEPERATOR = ",";	
	
	/**
	 * Date format to be used in the reporting database so it is actually stored as a Date
	 */
	public final static String DATE_FORMAT_DATABASE = "yyyy-MM-dd hh:mm:ss";
	
	// contants for managing the last-successful-run table
	
	/**
	 * status if fillReporting-script is running
	 */
	public final static String STATUS_RUNNING = "Running";
	
	/**
	 * status if fillReportingDatabase is idle
	 */
	public final static String STATUS_DONE = "Done";	
	
	/**
	 * table name
	 */
	public final static String TABLE_LASTRUN = "lastsuccessfulrun";
	
	/**
	 * column name, contains timestamp in DATE_FORMAT_DATABASE
	 */
	public final static String COLUMN_LASTRUN = "lastrun"; 
	
	/**
	 * column name, contains status: Running | Done;
	 */
	public final static String COLUMN_STATUS = "status";  
	
	public final static String COLUMN_TABLENAME = "tablename";
}
