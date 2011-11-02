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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.DataFactory;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.layout.output.AbstractReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.misc.datafactory.sql.DriverConnectionProvider;
import org.pentaho.reporting.engine.classic.core.modules.misc.datafactory.sql.SQLReportDataFactory;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.PageableReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.pdf.PdfOutputProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.base.FlowReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.xls.FlowExcelOutputProcessor;
import org.pentaho.reporting.libraries.resourceloader.Resource;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;

public class TestReporting {

	private static String user = "alfresco";
	private static String pass = "alfresco";
	private static String url = "jdbc:mysql://localhost:3306/alfrescoreporting";
	private static String driver = "org.gjt.mm.mysql.Driver";
	
	private static Log logger = LogFactory.getLog(JasperReporting.class);
	  
    public static enum OutputType {
        PDF, EXCEL, HTML
    }
    
	  public static SQLReportDataFactory getDataFactory() 
	  {
	    final DriverConnectionProvider sampleDriverConnectionProvider = new DriverConnectionProvider();
	    //sampleDriverConnectionProvider.setDriver(driver);
	    //sampleDriverConnectionProvider.setUrl(url);
	    //sampleDriverConnectionProvider.setProperty("user", user);
	    //sampleDriverConnectionProvider.setProperty("password", pass);
	    final SQLReportDataFactory dataFactory = new SQLReportDataFactory(sampleDriverConnectionProvider);
	    return null;
	  }		  
    
	private static MasterReport getReportDefinition(String fromFile) throws IOException {
		try {
			File tempFile = new File(fromFile);		    	
		    final ResourceManager resourceManager = new ResourceManager();
		    resourceManager.registerDefaults();
		    String contentUrl = tempFile.getAbsolutePath();
		    final Resource report = resourceManager.createDirectly(contentUrl, MasterReport.class);
			return (MasterReport) report.getResource();
		} catch (ResourceException e) {
		   	e.printStackTrace();
		}
		return null;
	}
	  
      public static void generateReport(final OutputType outputType, OutputStream outputStream, String fromFile) 
      	throws IllegalArgumentException, ReportProcessingException, IOException {
    	  logger.debug("generateReport start");
	       if (outputStream == null) {
	                 throw new IllegalArgumentException("The output stream was not specified");}
	       ClassicEngineBoot.getInstance().start();
	       final MasterReport report = getReportDefinition(fromFile);
	       
	       DataFactory dataFactory =null;
	       if (report.getDataFactory()== null){
	    	   System.out.println("DataFactory was null in the report");
	    	   dataFactory = getDataFactory();
	       }
	       // Set the data factory for the report
	       
	       if (dataFactory != null) {
	      	 logger.debug("Setting dataFactory");
	      	 report.setDataFactory(dataFactory);
	       }
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
	         logger.debug("just before processReport()");
	         reportProcessor.processReport();
	         logger.debug("Just after processReport()");
	
			} finally {
	             if (reportProcessor != null) {
	          	   reportProcessor.close();
	             }
	       }
	  }
      // @TODO: open report, if has JDBC credentials, replace, if has JNDI, replace with JDBC
		public static void processReport(String fromFile, String toFile) throws FileNotFoundException{
			logger.debug("processreport start");
			File outputFile = new File(toFile);
			FileOutputStream outputStream = new FileOutputStream(outputFile); 
			logger.debug("Got the outputstream: " + outputStream);
			try {

				generateReport(OutputType.PDF, outputStream, fromFile);

			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ReportProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			logger.debug("processReport end");
		}
		
		public static void main(String[] args) throws FileNotFoundException{
			if (args.length!=2){
				System.out.println("org.alfresco.reporting.TestReporting [fromFile] [toFile]");
				System.out.println(" fromfile file is the full path to the prpt file");
				System.out.println(" toFile   the file that is generated. (a PDF for now)");
			}
			processReport(args[0], args[1]);
		}
}
