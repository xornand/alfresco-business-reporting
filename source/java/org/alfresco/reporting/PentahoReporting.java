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

import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.pentaho.reporting.engine.classic.core.ClassicEngineBoot;
import org.pentaho.reporting.engine.classic.core.DataFactory;
import org.pentaho.reporting.engine.classic.core.ImageContainer;
import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.layout.output.AbstractReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.misc.datafactory.sql.DriverConnectionProvider;
import org.pentaho.reporting.engine.classic.core.modules.misc.datafactory.sql.SQLReportDataFactory;
import org.pentaho.reporting.engine.classic.core.modules.misc.datafactory.sql.StaticConnectionProvider;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.PageableReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.pdf.PdfOutputProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.base.FlowReportProcessor;
/*
import org.pentaho.reporting.engine.classic.core.modules.output.table.base.StreamReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.base.TableContentProducer;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.AllItemsHtmlPrinter;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.FileSystemURLRewriter;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlOutputProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlPrinter;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.StreamHtmlOutputProcessor;
*/
import org.pentaho.reporting.engine.classic.core.modules.output.table.xls.FlowExcelOutputProcessor;

import org.pentaho.reporting.libraries.resourceloader.ResourceManager;
import org.pentaho.reporting.libraries.resourceloader.Resource;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.TempFileProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
/* 
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.layout.model.LogicalPageBox;
import org.pentaho.reporting.engine.classic.core.layout.output.ContentProcessingException;
import org.pentaho.reporting.engine.classic.core.layout.output.LogicalPageKey;
import org.pentaho.reporting.engine.classic.core.layout.output.OutputProcessorMetaData;
import org.pentaho.reporting.engine.classic.core.modules.output.table.base.TableContentProducer;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlPrinter;
*/
public class PentahoReporting implements Reportable {

	private NodeRef input;
	private NodeRef output;
	private String format="";
	private Connection conn;
	private ServiceRegistry serviceRegistry;
	private DataFactory dataFactory;
	private String user = "";
	private String pass = "";
	private String url = "";
	private String driver = "";
	private File tempFile;
	public static String EXTENSION = ".prpt";
	
 	 /**
     * Performs the basic initialization required to generate a report
     */
	public PentahoReporting(){
		 // Initialize the reporting engine
		 ClassicEngineBoot.getInstance().start();
	}
	
	private static Log logger = LogFactory.getLog(PentahoReporting.class);
	
	@Override
	public void setUsername(String user){
		this.user= user; 
	}
		
	@Override
	public void setPassword(String pass){
		this.pass= pass; 
	}
		
	@Override
	public void setUrl(String url){
		this.url= url; 
	}
		
	@Override
	public void setDriver(String driver){
		this.driver= driver; 
	}
		
		@Override
		public void setReportDefinition(NodeRef input){
			this.input = input;
		}
		
		@Override
		public void setResultObject(NodeRef output){
			this.output = output;
		}
		
		@Override
		public void setOutputFormat(String format){
			this.format = format;
		}
		
		@Override
		public void setConnection(Connection conn){
			this.conn = conn;
			logger.debug("setConnection start");
			try {
				Properties props = conn.getClientInfo();
				logger.debug("Found Connection props:");
				while (props.keys().hasMoreElements()){
					String key = (String)props.keys().nextElement();
					logger.debug("* "+ key + " = " + props.getProperty(key));
				}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			logger.debug("setConnection end");
		}
		
		@Override
		public void setServiceRegistry(ServiceRegistry serviceRegistry){
			this.serviceRegistry = serviceRegistry;
		}

		/**
         * The supported output types for this sample
         */
        public static enum OutputType {
                 PDF, EXCEL, HTML
        }
	/**	
		 private void generateReport(final OutputType outputType, File outputFile) 
		 		throws IllegalArgumentException, IOException { //, ReportProcessingException {
		
			if (outputFile == null) {
			     throw new IllegalArgumentException("The output file was not specified");
			}
			OutputStream outputStream = null;
			
			try {
			     // Open the output stream
			     outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
			     // Generate the report to this output stream
			     generateReport(outputType, outputStream);
			} catch (Exception e) {
			     System.out.println("Exception: " + e.getMessage());
			} finally {
			     if (outputStream != null) {
			              outputStream.close();
			     }
			}
		} // end generateReport
		**/
		private String writeImage(ImageContainer image, String encoderType, float quality, boolean alpha) {
		
			return null;
		
		}

		  private MasterReport getReportDefinition() throws ContentIOException, IOException {
			logger.debug("getReportDefinition start");
		    try {
		    	ContentReader contentReader = serviceRegistry.getContentService().getReader(input, ContentModel.PROP_CONTENT);
		    	tempFile = TempFileProvider.createTempFile("reporting_", EXTENSION);
		    	logger.debug("Prepping tempFile: "+ tempFile.getAbsolutePath());
		    	contentReader.getContent(tempFile);
		        logger.debug("Wrote the tempFile: "+ tempFile.getAbsolutePath());
		    	
		    	final ResourceManager resourceManager = new ResourceManager();
		     	logger.debug("Got the ResourceManager");
		     	resourceManager.registerDefaults();
		     	logger.debug("ResourceManager registered defaults done");
		     	String contentUrl = "file://"+tempFile.getAbsolutePath();
		     	final Resource report = resourceManager.createDirectly(new URL(contentUrl), MasterReport.class);
		     		     	  
			    logger.debug("Returning the MasterReport: " + report.toString() ); 
			    return (MasterReport) report.getResource();
		    } catch (ResourceException e) {
		      e.printStackTrace();
		    }
		    logger.debug("getReportDefinition end");
		    return null;
		  }
		  
		  /**
		   * Returns the data factory which will be used to generate the data used during report generation. In this example,
		   * we will return null since the data factory has been defined in the report definition.
		   *
		   * @return the data factory used with the report generator
		   */

		  public DataFactory getSQLDataFactory(String user, String pass, String url, String driver)
		  {
			logger.debug("getSQLDataFactory start");
		    final DriverConnectionProvider sampleDriverConnectionProvider = new DriverConnectionProvider();
		    sampleDriverConnectionProvider.setDriver(driver);
		    sampleDriverConnectionProvider.setUrl(url);
		    sampleDriverConnectionProvider.setProperty("user", user);
		    sampleDriverConnectionProvider.setProperty("password", pass);
		    final SQLReportDataFactory dataFactory = new SQLReportDataFactory(sampleDriverConnectionProvider);
		    logger.debug("getSQLDataFactory end");
		    return dataFactory;
		  }	
		  
		  public DataFactory getConnectionFactory()
		  {
			logger.debug("getConnectionFactory start");
		    final StaticConnectionProvider sampleDriverConnectionProvider = new StaticConnectionProvider(conn);
		    final SQLReportDataFactory dataFactory = new SQLReportDataFactory(sampleDriverConnectionProvider);
		    logger.debug("getConnectionFactory end");
		    return dataFactory;
		  }	
		   /**

         * Generates the report in the specified <code>outputType</code> and writes
         * it into the specified <code>outputStream</code>.
         * <p/>
         * It is the responsibility of the caller to close the
         * <code>outputStream</code> after this method is executed.
         *
         * @param outputType
         *            the output type of the report (HTML, PDF, HTML)
         * @param outputStream
         *            the stream into which the report will be written
         * @throws IllegalArgumentException
         *             indicates the required parameters were not provided
         * @throws ReportProcessingException
         *             indicates an error generating the report
		 * @throws IOException 
		 * @throws ContentIOException 
         */

        public void generateReport(final OutputType outputType, OutputStream outputStream) 
        	throws IllegalArgumentException, ReportProcessingException, ContentIOException, IOException {
        	logger.debug("generateReport start");
             if (outputStream == null) {
                       throw new IllegalArgumentException("The output stream was not specified");}
             //ClassicEngineBoot.getInstance().start();
             MasterReport report = getReportDefinition();
             //DataFactory dataFactory = getSQLDataFactory(user, pass, url, driver);
             //final DataFactory dataFactory = getConnectionFactory();
             // Set the data factory for the report
             if (dataFactory != null) {
            	 logger.debug("Setting dataFactory");
            	 report.setDataFactory(dataFactory); 
             }
             Enumeration<String> conf = report.getConfiguration().getConfigProperties();
             while (conf.hasMoreElements()){
            	 String key = (String)conf.nextElement();
            	 logger.debug("Config: "+ key );
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
/*
                   case HTML: {
                        final StreamRepository targetRepository = new StreamRepository( outputStream);
                        final ContentLocation targetRoot = targetRepository.getRoot();
                        final HtmlOutputProcessor outputProcessor = new StreamHtmlOutputProcessor(
                        		report.getConfiguration());
                        final MyHtmlPrinter printer = new AllItemsHtmlPrinter(report.getResourceManager());
                        printer.setContentWriter(targetRoot, 
                        		new DefaultNameGenerator(targetRoot, "index", "HTML"));
                        printer.setDataWriter(null, null);
                        printer.setUrlRewriter(new FileSystemURLRewriter());
                        outputProcessor.setPrinter(printer);
                        reportProcessor = new StreamReportProcessor(report, outputProcessor);
                        final StreamRepository targetRepository = new StreamRepository(outputStream);
                        final ContentLocation targetRoot = targetRepository.getRoot();
                        final HtmlOutputProcessor outputProcessor = new StreamHtmlOutputProcessor(
                        		report.getConfiguration());
                        final HtmlPrinter printer = new MyHtmlPrinter(report.getResourceManager());
                        printer.setContentWriter(targetRoot, 
                        		new DefaultNameGenerator(targetRoot, "index", "html"));
                        printer.setDataWriter(null, null);
                        printer.setUrlRewriter(new FileSystemURLRewriter());
                        outputProcessor.setPrinter(printer);
                        final StreamReportProcessor sp = new StreamReportProcessor(
                                            report, outputProcessor);
                        sp.processReport();
                        sp.close();
                        break;
                   }
  */
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
        /**
        public class MyHtmlPrinter extends HtmlPrinter {

            public MyHtmlPrinter(final ResourceManager resourceManager) {
                      super(resourceManager);
            }
 
            @Override
            public void print(LogicalPageKey logicalPageKey,
                       LogicalPageBox logicalPage,
                       TableContentProducer contentProducer,
                       OutputProcessorMetaData metaData, boolean incremental)
                       { //throws ContentProcessingException {
              super.print(logicalPageKey, logicalPage, contentProducer, metaData, incremental);
            }

            @Override
            public String writeImage(ImageContainer image, String encoderType,
                       float quality, boolean alpha) throws IOException { //ContentIOException {
			  if (image instanceof DefaultImageReference) {
		           DefaultImageReference dir = (DefaultImageReference) image;
		           return dir.getSourceURLString();
			  }
			  return super.writeImage(image, encoderType, quality, alpha);
            }
   }

**/
		
		public void processReport(){
			logger.debug("processreport start");
			ContentWriter contentWriter = serviceRegistry.getContentService().getWriter(output, ContentModel.PROP_CONTENT, true);
			logger.debug("Found: " +  serviceRegistry.getNodeService().getProperty(output, QName.createQName("http://www.alfresco.org/model/content/1.0", "name")));
			OutputStream outputStream = contentWriter.getContentOutputStream();
			logger.debug("Got the outputstream: " + outputStream);
			try {

				generateReport(OutputType.PDF, outputStream);

			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ReportProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ContentIOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			logger.debug("processReport end");
		}
		
		
		
	}
