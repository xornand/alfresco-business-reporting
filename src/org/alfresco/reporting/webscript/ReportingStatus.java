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

package org.alfresco.reporting.webscript;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.alfresco.reporting.Constants;
import org.alfresco.reporting.db.DatabaseHelperBean;
import org.alfresco.service.ServiceRegistry;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

public class ReportingStatus extends AbstractWebScript {

	private DatabaseHelperBean dbhb = null;
	private ServiceRegistry serviceRegistry = null;
	
	
	@Override
	public void execute(WebScriptRequest arg0, WebScriptResponse pResponse)
			throws IOException {
		
		
		try {
			final Map<String, String> tables = dbhb.getShowTablesDetails();
			
			final Iterator<String> keys = new TreeSet<String> ( tables.keySet() ).iterator();
			final JSONObject mainObject = new JSONObject();
			final JSONArray mainArray = new JSONArray();

			while (keys.hasNext()){
				String key = (String)keys.next();
				if (!Constants.TABLE_LASTRUN.equalsIgnoreCase(key.trim())){
					JSONObject rowObject = new JSONObject();
					rowObject.put("table", key.trim());
					try{
						rowObject.put("last_run", dbhb.getLastTimestamp(key));
						
						rowObject.put("status", dbhb.getLastTimestampStatus(key));
						
					} catch (Exception e){
						pResponse.getWriter().write("No last successful run... (actually, an exception)\n\n");
						pResponse.getWriter().write("Exception: " + e.toString()+"\n");
					}	
					
					String[] t = tables.get(key).split(",");
					rowObject.put("number_of_rows", t[0].toString());
					rowObject.put("number_of_latest", t[1].toString());
					rowObject.put("number_of_non_latest", t[2].toString());
					rowObject.put("number_in_workspace", t[3].toString());
					rowObject.put("number_in_archivespace", t[4].toString());
					mainArray.add(rowObject);
				}
			} // end while
			mainObject.put("result", mainArray);
			pResponse.getWriter().write(mainObject.toJSONString());
		} catch (IOException e) {
			e.printStackTrace();
		} 


	}
	
	public void setServiceRegistry(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}
	
	public void setDatabaseHelperBean(DatabaseHelperBean databaseHelperBean) {
		this.dbhb = databaseHelperBean;
	}
	

}
