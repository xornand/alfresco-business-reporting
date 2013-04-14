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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ReportLine {

	private Properties types = new Properties();
	private Properties values = new Properties();
	private String table = "";
	private final SimpleDateFormat sdf = new SimpleDateFormat(Constants.DATE_FORMAT_DATABASE);// "yyyy-MM-dd hh:mm:ss");
	private static Log logger = LogFactory.getLog(ReportLine.class);
	
	public ReportLine(String table){
		this.table = table;
	}
	
	public void reset(){
		types  = new Properties();
		values = new Properties();
	}
	
	public void setLine(String key, String type, String value, Properties replacementTypes){
		if ((key!=null) && (type!=null)){
			if (value==null) value="";
			type = type.trim();
			value= value.trim();
			// validate if the current definition is overridden...
			if (replacementTypes.containsKey(key)){
				type = replacementTypes.getProperty(key, "-").trim();
			}
			if (type.toUpperCase().contains("VARCHAR")){
				int length = Integer.parseInt(type.substring(8,type.length()-1));
				if (value.length()>length){
					value = value.substring(0,length-4);
					logger.debug("chopped " + key + " length to " + length + " for " + value);
				}
			}
			types.setProperty(key, type);
			values.setProperty(key, value);
		}
	}
	
	public void setTable(String table){
		this.table = table;
	}
	
	public String getType(String key){
		return types.getProperty(key);
	}
	
	public boolean hasValue(String key){
		String returnString = values.getProperty(key);
		return (returnString != null);
	}
	
	public String getValue(String key){
		String returnString = values.getProperty(key);
		if (returnString==null){returnString="";}
		returnString = returnString.replaceAll("'", "_").replaceAll("\"","_");
		return returnString;
	}
	
	public int size(){
		return types.size();
	}
	
	public Enumeration getKeys(){
		return types.keys();
	}
	
	public String getTable(){
		return table;
	}
	
	/**
	 * generate "a=1, b=2, c=3"
	 * @return String of set
	 */
	public String getUpdateSet(){
		String returnString = "";
		String ignoreKey="sys_node_uuid"; // comma seperated list of values. sys:node-uuid is key that is used to check if the row already exists
		Enumeration<String> keys = getKeys();
		while (keys.hasMoreElements()){
			String key = (String)keys.nextElement();
			if (ignoreKey.indexOf(key)<0){
				if (returnString!=""){returnString+=", ";}
				String value = getValue(key);
				String type  = getType(key);
				returnString += key + "="+ formatValue(type, value);
			}
		}
		return returnString;
	}
	
	private String formatValue(String type, String value){
		String returnString = value;
		if ("BIGINT,BOOLEAN".indexOf(type)>-1){
			returnString =  value;
			if ("".equals(returnString)){
				returnString="NULL";
			}
		} else {
			if ("DATETIME".equals(type) || "DATE".equals(type)){
				if ("".equals(value) || "-".equals(value) || (value==null)){
					returnString="NULL";
				} else {
					try{
						Date myDate  = new Date(Long.parseLong(value));
						returnString =  "'" + sdf.format(myDate).replace(" ","T")+"'";
					} catch (Exception e) {
						returnString =  "'"+value+"'";	
					}
				}
			} else {
				returnString =  "'"+value+"'";
			}
		}
		return returnString;
	}
	

	public String getInsertListOfKeys(){
		String returnString = "";
		String key = "";
		@SuppressWarnings("unchecked")
		Enumeration<String> keys = getKeys();
		while (keys.hasMoreElements()){
			if (returnString!=""){returnString+=", ";}
			key = keys.nextElement(); 
			returnString += key;
		}
		return returnString;
	}
	
	public String getInsertListOfValues(){
		String returnString = "";
		@SuppressWarnings("rawtypes")
		Enumeration keys = getKeys();
		while (keys.hasMoreElements()){
			String key = (String)keys.nextElement();
			if (returnString!=""){returnString+=", ";}
			String value = getValue(key);
			String type  = getType(key);
			returnString += formatValue(type, value);
		}
		return returnString;
	}
}
