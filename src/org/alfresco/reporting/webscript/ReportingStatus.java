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

import org.alfresco.reporting.db.DatabaseHelperBean;
import org.alfresco.service.ServiceRegistry;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;

public class ReportingStatus extends AbstractWebScript {

	private DatabaseHelperBean dbhb = null;
	private ServiceRegistry serviceRegistry = null;
	
	
	@Override
	public void execute(WebScriptRequest arg0, WebScriptResponse pResponse)
			throws IOException {
		
		
		//pResponse.getWriter().write("Alfresco Business Reporting Tables:<p>");
		
		try {
			//pResponse.getWriter().write("Harvesting Enabled: " + dbhb.isEnabled() + "\n\n");
			//pResponse.getWriter().write("Execution Enabled: " + dbhb.isEnabled() nabled() + "\n\n");
			Map<String, String> tables = dbhb.getShowTablesDetails();
			SortedSet<String> ss = new TreeSet<String> ( tables.keySet() );
			Iterator<String> keys = ss.iterator();
			pResponse.getWriter().write("Alfresco Business Reporting Tables:\n");
			pResponse.getWriter().write("===================================\n");
			String lastRun="";
			String content="";
			while (keys.hasNext()){
				String key = (String)keys.next();
				
				try{
					lastRun = dbhb.getLastTimestamp(key);
					lastRun ="Last successful run started: " + lastRun + " ";
						
					content = dbhb.getLastTimestampStatus(key);
					content = "\t status: " + content + " ";
							
				} catch (Exception e){
					pResponse.getWriter().write("No last successful run... (actually, an exception)\n\n");
					pResponse.getWriter().write("Exception: " + e.toString()+"\n");
				}	
				
				String[] t = tables.get(key).split(",");
				String number = t[0];
				String latest = "\t latest/older: " + t[1] + "/" + t[2];
				String work   = "\t work/archive: " + t[3] + "/" + t[4];
				pResponse.getWriter().write("  " + lastRun + content + "\t " + key + "\t (" + number + latest + work + ")\n");
			}
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
