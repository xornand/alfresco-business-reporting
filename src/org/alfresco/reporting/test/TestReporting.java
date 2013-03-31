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


package org.alfresco.reporting.test; 

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NamingException;
import javax.sql.ConnectionPoolDataSource;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.DataFactory;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.layout.output.AbstractReportProcessor;
//import org.pentaho.reporting.engine.classic.core.modules.misc.datafactory.sql
//import org.pentaho.reporting.engine.classic.core.modules.misc.datafactory.sql.DriverConnectionProvider;
import org.pentaho.reporting.engine.classic.core.modules.misc.datafactory.sql.DriverConnectionProvider;
import org.pentaho.reporting.engine.classic.core.modules.misc.datafactory.sql.JndiConnectionProvider;
import org.pentaho.reporting.engine.classic.core.modules.misc.datafactory.sql.SQLReportDataFactory;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.PageableReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.pdf.PdfOutputProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.base.FlowReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.xls.FlowExcelOutputProcessor;
import org.pentaho.reporting.libraries.resourceloader.Resource;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;
import com.mysql.jdbc.jdbc2.optional.*;
public class TestReporting {

	private static String user = "ibfd";
	private static String pass = "ibfd";
	private static String url = "jdbc:mysql://localhost:3306/alfrescoreporting";
	private static String driver = "org.gjt.mm.mysql.Driver";
	
	  
    public static enum OutputType {
        PDF, EXCEL, HTML
    }
    	
	  public static SQLReportDataFactory getDataFactory() 
	  {
		  System.out.println("Enter getDataFactory");
		  final DriverConnectionProvider sampleDriverConnectionProvider = new DriverConnectionProvider();
	      sampleDriverConnectionProvider.setDriver(driver);
	      sampleDriverConnectionProvider.setUrl(url);
	      sampleDriverConnectionProvider.setProperty("user", user);
	      sampleDriverConnectionProvider.setProperty("password", pass);
	      final SQLReportDataFactory dataFactory = new SQLReportDataFactory(sampleDriverConnectionProvider);
	      //final SQLReportDataFactory dataFactory = new SQLReportDataFactory(getInitializedConnectionProvider());
	      System.out.println("Exit getDataFactory");
	      return dataFactory;
	  }		  
  
	private static MasterReport getReportDefinition(String fromFile) throws IOException {
		System.out.println("Enter getReportDefinition");
		MasterReport mr=null;
		Resource report = null;
		try {
			File tempFile = new File(fromFile);		    	
		    final ResourceManager resourceManager = new ResourceManager();
		    resourceManager.registerDefaults();
		    String contentUrl = tempFile.getAbsolutePath();
		    report = resourceManager.createDirectly(contentUrl, MasterReport.class);
			mr = (MasterReport) report.getResource();
		} catch (ResourceException e) {
		   	e.printStackTrace();
		}
		System.out.println("Exit getReportDefinition");
		return mr;
	}
	  
      public static void generateReport(String jndiName, final OutputType outputType, OutputStream outputStream, String fromFile) 
      	throws IllegalArgumentException, ReportProcessingException, IOException, NamingException, SQLException {
    	   System.out.println("Enter generateReport");
    	   Registry registry = null;

    	   if (outputStream == null) {
	                 throw new IllegalArgumentException("The output stream was not specified");}
	       final MasterReport report = getReportDefinition(fromFile);
	       
	       boolean JDBC=false;
	       boolean JNDI=true;
	       
	       DataFactory dataFactory =null;
	       if (JDBC){
	    	   System.out.println("generateReport: Getting JDBC DataFactory ");
	    	   dataFactory = getDataFactory();
	    	   report.setDataFactory(dataFactory);
		       
	       }
	       if (JNDI){
	    	   System.out.println("generateReport: Getting JNDI DataSource ");
	    	   registry = startRegistry();
	    	   //org.pentaho.reporting.engine.classic.core.filter.DataSource ds = getJndiDataSource(jndiName);
	    	   //report.setDataSource(ds);
	       }
	       
	       report.getParameterValues().put("external_collId", "kf");
	       // Prepare to generate the report
	       AbstractReportProcessor reportProcessor = null;
	       try {
	       	// Create the report processor for the specified output type
	         switch (outputType) {
	             case PDF: {
	                  final PdfOutputProcessor outputProcessor = new PdfOutputProcessor(
	                  	report.getConfiguration(), outputStream, report.getResourceManager());
	                  reportProcessor = new PageableReportProcessor(report, outputProcessor);
	                  break;
	             }
	
	             case EXCEL: {
	                  final FlowExcelOutputProcessor target = new FlowExcelOutputProcessor(
	                  	report.getConfiguration(), outputStream, report.getResourceManager());
	                  reportProcessor = new FlowReportProcessor(report, target);
	                  break;
	             }
	         }
	         // Generate the report
	         System.out.println("generateReport: just before processReport()");
	         reportProcessor.processReport();
	         System.out.println("generateReport: just after processReport()");
	
			} finally {
	             if (reportProcessor != null) {
	            	 reportProcessor.close();
	             }
	             if (JNDI) {
	          		 stopRegistry(registry);
	          	 }
	       }
	  }
      // @TODO: open report, if has JDBC credentials, replace, if has JNDI, replace with JDBC
		public static void processReport(String jndiName, String fromFile, String toFile) throws FileNotFoundException{
			System.out.println("Enter processReport");
			ClassicEngineBoot.getInstance().start(); 

			File outputFile = new File(toFile);
			FileOutputStream outputStream = new FileOutputStream(outputFile); 
			System.out.println("processReport: Got the outputstream: " + outputStream);
			try {

				generateReport(jndiName, OutputType.PDF, outputStream, fromFile);

			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (ReportProcessingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (NamingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Exit processReport");
		}
		
		private static Registry startRegistry() throws RemoteException {
			System.out.println("enter startRegistry");
			Registry registry = null;
			try{
				registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
			    System.out.println("exit startRegistry: RMI registry created.");
			} catch (ExportException e){
				System.out.println("exit startRegistry: RMI registry already existed.");
			}
			return registry;
		}
		
		private static boolean stopRegistry(Registry registry) {
			System.out.println("enter stopRegistry");
			boolean result = false;
		   	try {
				result = UnicastRemoteObject.unexportObject(registry, true);
			} catch (NoSuchObjectException e) {
				System.out.println("stopRegistry: RMI registry already stopped.");
				//e.printStackTrace();
			}
		    System.out.println("exit stopRegistry: RMI registry stopped: " + result);
		    return result;
		}
	
		private static InitialContext createContext() throws NamingException {
		    Properties env = new Properties();
		    env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.rmi.registry.RegistryContextFactory");
		    env.put(Context.PROVIDER_URL, "rmi://localhost:"+Registry.REGISTRY_PORT);
		    InitialContext context = new InitialContext(env);
		    return context;
		}
		
		private static org.pentaho.reporting.engine.classic.core.filter.DataSource getJndiDataSource(String source) throws NamingException, RemoteException{
			System.out.println("enter getJndiDataSource source="+source);
			
			ConnectionPoolDataSource dataSource = new MysqlConnectionPoolDataSource();
		      ((MysqlDataSource) dataSource).setUser("alfrescoo");
		      ((MysqlDataSource) dataSource).setPassword("d=dMdva1fresco");
		      ((MysqlDataSource) dataSource).setServerName("localhost");
		      ((MysqlDataSource) dataSource).setPort(3306);
		      ((MysqlDataSource) dataSource).setDatabaseName("alfrescoreporting");
		    //DataSource ds = (javax.sql.DataSource)dataSource;
		    //Context ctx = new InitialContext();
			//System.out.println("Done initialContext");
			org.pentaho.reporting.engine.classic.core.filter.DataSource ds = (org.pentaho.reporting.engine.classic.core.filter.DataSource)dataSource;
			//ctx.lookup("ibfdReporting");
			//System.out.println("Done lookup");
			System.out.println(ds.toString());    
		    //InitialContext context = createContext();
		    //context.rebind("java:"+source, ds);
		    System.out.println("exit getJndiDataSource");
		    return ds;
		}
		
		private static void setJndiDataSource(String source) throws NamingException, RemoteException{
			System.out.println("enter setJndiDataSource source="+source);
			
			ConnectionPoolDataSource dataSource = new MysqlConnectionPoolDataSource();
		      ((MysqlDataSource) dataSource).setUser("alfrescoo");
		      ((MysqlDataSource) dataSource).setPassword("d=dMdva1fresco");
		      ((MysqlDataSource) dataSource).setServerName("localhost");
		      ((MysqlDataSource) dataSource).setPort(3306);
		      ((MysqlDataSource) dataSource).setDatabaseName("alfrescoreporting");
		    DataSource ds = (javax.sql.DataSource)dataSource;
			InitialContext context = createContext();
		    context.rebind("java:/comp/env/jdbc/"+source, ds);
		    System.out.println("exit setJndiDataSource");
		}
			  
		public static void main(String[] args) throws FileNotFoundException, NamingException, SQLException{
			if (args.length!=3){
				System.out.println("org.alfresco.reporting.TestReporting [jndi-resource] [fromFile] [toFile]");
				System.out.println(" fromfile file is the full path to the prpt file");
				System.out.println(" toFile   the file that is generated. (a PDF for now)");
			}
			processReport(args[0], args[1], args[2]);
		}
}
