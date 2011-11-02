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

import java.sql.Connection;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;

public interface Reportable {
	public String EXTENSION="";
	
	public void setUsername(String user);
	
	public void setPassword(String pass);
	
	public void setUrl(String url);
	
	public void setDriver(String driver);

	public void setReportDefinition(NodeRef input);
	
	public void setResultObject(NodeRef output);
	
	public void setOutputFormat(String format);
	
	public void setConnection(Connection conn);
	
	public void setServiceRegistry(ServiceRegistry serviceRegistry);
	
	public void processReport();
	
}
