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

package org.alfresco.reporting.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.alfresco.reporting.Constants;
import org.alfresco.reporting.ReportLine;
import org.alfresco.reporting.mybatis.ReportingDAO;
import org.alfresco.reporting.mybatis.impl.ReportingDAOImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DatabaseHelperBean {

	private static Log logger = LogFactory.getLog(DatabaseHelperBean.class);
	private String database;
	private String jdbcdriver;
	private String username;
	private String password;
	private Connection conn;
	private Properties globalProperties;
	private ReportingDAO reportingDAO;
	
	
	public void setReportingDAOImpl (ReportingDAO reportingDAO){
		this.reportingDAO = reportingDAO;
	}
	
    public void setProperties(Properties properties){
    	this.globalProperties = properties;
    }
    
    public void logAllProperties(){
 
    	String returnString = "Size: " + globalProperties.size()+" - "; 
    	Enumeration keys = globalProperties.keys();
   		while (keys.hasMoreElements()){
   			String key = (String)keys.nextElement();
   			if (key.indexOf("reporting.")>-1){
   				if (logger.isDebugEnabled()) logger.debug(key+"="+globalProperties.getProperty(key));
	   			returnString += key+"="+globalProperties.getProperty(key)+"\n";
   			}
   		}

    }
	// jdbc:mysql://localhost/AlfrescoReporting340
	public void setDatabase(String database)
    {
        this.database = database;
    }
	
	// com.mysql.jdbc.Driver
	public void setJdbcdriver(String jdbcdriver)
    {
        this.jdbcdriver= jdbcdriver;
    }
	
	public void setUsername(String username)
    {
        this.username = username;
    }
	
	public void setPassword(String password)
    {
        this.password = password;
    }

	public Map<String, String> getShowTables(){
		//Map<String, String> sm = new HashMap<String, String>();
		/*
		List<String> tables = reportingDAO.getShowTables();
		logger.info("Found show tables results: " + tables.size());
		for (int t=0;t<tables.size(); t++){
			String tablename = tables.get(t);
			logger.info("Processing table " + tablename);
			int amount = reportingDAO.getNumberOfRowsForTable(tablename);
			logger.info("Returned " + amount);
			sm.put(tablename, String.valueOf(amount));
		}
		*/
		
		Map<String, String> sm = new HashMap<String, String>();
		Statement stmt=null;
		try {
			stmt = getConnection().createStatement();
			String sql = "SHOW TABLES;";
		    ResultSet rs = stmt.executeQuery(sql);
		    
		    while(rs.next()){
		         //Retrieve by column number
		    	String table = rs.getString(1);
		    	sql = "SELECT COUNT(*) FROM " + table;
		    	Statement stmt2 = getConnection().createStatement();
		    	ResultSet rs2 = stmt2.executeQuery(sql);
			    while(rs2.next()){
			    	//p.setProperty(table, Long.toString(rs2.getLong(1)));
			    	sm.put(table, Long.toString(rs2.getLong(1)));
			    }
		    }
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
		      //finally block used to close resources
			  try{
			     if(stmt!=null)
			        stmt.close();
			  }catch(SQLException se2){
		      }// nothing we can do
		}	
		
		return sm;
	}
	
	private String postFix(String base, final int size, final String filler){
		while (base.length()<size){
			base += filler;
		}
		return base;
	}
	
	public Map<String, String> getShowTablesDetails(){
		Map<String, String> sm = new HashMap<String, String>();
		//Properties p = new Properties();
		if (logger.isDebugEnabled()) logger.debug("Starting getShowTablesDetails");
		Statement stmt=null;
		try {
			stmt = getConnection().createStatement();
			String sql = "SHOW TABLES;";
		    ResultSet rs = stmt.executeQuery(sql);
		    
		    while(rs.next()){
		         //Retrieve by column number
		    	String table = rs.getString(1);
		    	int tableLength = table.length()+5;
		    	String totaal = "";
		    	String isLatest = "0";
		    	String isNonLatest = "0";
		    	String isWorkSpace = "0";
		    	String isAchive = "0";
		    	
		    	sql = "SELECT COUNT(*) FROM " + table;
		    	Statement stmt2 = getConnection().createStatement();
		    	ResultSet rs2 = stmt2.executeQuery(sql);
			    while(rs2.next()){
			    	//p.setProperty(table, Long.toString(rs2.getLong(1)));
			    	totaal = Long.toString(rs2.getLong(1));
			    }
			    
			    String isLatestQuery = "select isLatest, count(*) from "+table+" group by isLatest;";
			    try{
			    	rs2 = stmt2.executeQuery(isLatestQuery);
			    	while(rs2.next()){
			    	
				    	//p.setProperty(table, Long.toString(rs2.getLong(1)));
				    	if (Long.toString(rs2.getLong(1)).equals("0")){
				    		isNonLatest = Long.toString(rs2.getLong(2));
				    	} else {
				    		isLatest = Long.toString(rs2.getLong(2));
				    	}
			    	}
			    } catch (Exception e){}
			    
			    String workSpaceArchiveQuery = "select sys_store_protocol,count(*) from " + table + " group by sys_store_protocol order by sys_store_protocol";
			    try{
			    	rs2 = stmt2.executeQuery(workSpaceArchiveQuery);
			    	while(rs2.next()){
			    	
				    	//p.setProperty(table, Long.toString(rs2.getLong(1)));
				    	if (rs2.getString(1).equals("archive")){
				    		isAchive = Long.toString(rs2.getLong(2));
				    	} else {
				    		isWorkSpace = Long.toString(rs2.getLong(2));
				    	}
			    	} 
			    } catch (Exception e){}
			    
			    sm.put(postFix(table, tableLength," "), totaal + ","+isLatest+","+isNonLatest+","+isWorkSpace+","+isAchive);
		    }
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
		      //finally block used to close resources
			  try{
			     if(stmt!=null)
			        stmt.close();
			  }catch(SQLException se2){
		      }// nothing we can do
		}	
		
		return sm;
	}
	
	public void init(){
		if (logger.isDebugEnabled()) logger.debug("Starting getTableDescription");
	
		//logger.info("TJAKKAAAAA: " + reportingDAO.getShowTables().toString());
	
		try{
			Map<String, String> p = getShowTables();
			SortedSet<String> ss = new TreeSet<String> ( p.keySet() );
			Iterator<String> keys = ss.iterator();
			if (ss.size()==0){
				logger.info("  No reporting tables to display...");
			} else {
				while (keys.hasNext()){
					String key = (String)keys.next();
					logger.info("  " + key + " (" + p.get(key) + ")");
				}
			} // end if ss.size()			
		} catch (Exception e){
			logger.warn("Reporting table information could not be retrieved!!");
		}

	}
	
	
    public boolean isEnabled(){
    	boolean returnBoolean = !globalProperties.getProperty("reporting.enabled", "true").equalsIgnoreCase("false");
    	return returnBoolean;
    }
    
    
    private String replaceAllKeysInValue(String inString){
    	String first="";
    	String last="";
    	String key="";
    	while (inString.indexOf("$")>-1){
    		first= inString.substring(0,inString.indexOf("${"));
    		last = inString.substring(inString.indexOf("}")+1, inString.length());
    		key  = inString.substring(inString.indexOf("${")+2,inString.indexOf("}"));
//    		logger.debug("Key="+key);
    		inString = first + globalProperties.getProperty(key,"") + last;
    	}
    	return inString;
    }
    
	public Connection getConnection()  {
		try {
			if ((this.conn == null) || conn.isClosed()){
				String jndiName = globalProperties.getProperty(Constants.property_jndiName , "");
				
				if ("".equals(jndiName)){
					database   = globalProperties.getProperty("reporting.db.url");
					jdbcdriver = globalProperties.getProperty("reporting.db.driver");
					username   = globalProperties.getProperty("reporting.db.username");
					password   = globalProperties.getProperty("reporting.db.password");
	
					database   = replaceAllKeysInValue(database);
					jdbcdriver = replaceAllKeysInValue(jdbcdriver);
					username   = replaceAllKeysInValue(username);
					password   = replaceAllKeysInValue(password);
					
					if (logger.isDebugEnabled()){
						logger.debug("Enabled    : " + isEnabled());
						logger.debug("DatabaseURL: " + database);
						logger.debug("JDBC Driver: " + jdbcdriver);
						logger.debug("Username   : " + username);
					}
					if (isEnabled()){
						Class.forName(jdbcdriver);
						if (logger.isDebugEnabled()) logger.info("Connecting to database " + database);
						conn = DriverManager.getConnection(database,username,password);
					}
				} else {
					Context context = new InitialContext();
					DataSource ds = (DataSource)context.lookup("java:/comp/env/jdbc/"+jndiName);
					conn = ds.getConnection();
					if (logger.isDebugEnabled()){
						logger.info("Enabled    : " + isEnabled());
						logger.info("Connecting using JNDI name " + jndiName);
					}
				}
			}
	   } catch(SQLException se){
		      //Handle errors for JDBC
		      se.printStackTrace();
	   } catch (ClassNotFoundException e) {
		   e.printStackTrace();
	   } catch (NamingException e){
		   e.printStackTrace();
	   }
		 
		return conn;
	}
	
	
	private void dropTable(Statement stmt, String table) throws SQLException{
		String query = "DROP TABLE IF EXISTS `"+table+"`;";	
		if (logger.isDebugEnabled()) logger.debug("Dropping table. Query: "+query);
		int numberOfLines = stmt.executeUpdate(query);
	}
	
	
	private void createEmptyTable(Statement stmt, String table) throws SQLException{
		table = table.replaceAll("-", "_").trim();
		String query = "CREATE TABLE IF NOT EXISTS `" + table + "` (id INT AUTO_INCREMENT PRIMARY KEY, sys_node_uuid VARCHAR(100), isLatest BOOLEAN DEFAULT TRUE, validFrom DATETIME, validUntil DATETIME);";
		if (logger.isDebugEnabled()) logger.debug("Creating table. Query: "+query);
		int numberOfLines = stmt.executeUpdate(query);
	}
	
	
	public void extendTable(Statement stmt, String table, String column, String type) throws SQLException{
		table = table.replaceAll("-", "_").trim();
		//ALTER TABLE $table ADD $column $type
		String query = "ALTER TABLE `" + table + "` ADD " + column + " " + type;
		if (logger.isDebugEnabled()) logger.debug("Executing Query: "+query);
		int numberOfLines = stmt.executeUpdate(query);
	}

	
	public boolean rowExists(Statement stmt, ReportLine rl) throws SQLException{
		boolean returnValue = false;
		String query = "SELECT count(*) FROM `" + rl.getTable();
		query += "` WHERE sys_node_uuid='" + rl.getValue("sys_node_uuid")+"'";
		if (logger.isDebugEnabled()) logger.debug(query);
	    ResultSet rs = stmt.executeQuery(query);
	    rs.next();
	    long counter = rs.getLong(1);
    	returnValue = counter>0;
	    if (logger.isDebugEnabled()){
	    	logger.debug("rowExists1: returning " + returnValue);
	    }
	    return returnValue;
	}
	
	private String prefix(int inInt, int len, String character){
		String returnString = Integer.toString(inInt);
		while (returnString.length()<len){
			returnString = character+returnString;
		}
		
		return returnString;
	}
	
	private boolean rowEqualsModifiedDate(Statement stmt, ReportLine rl, String lastModified) throws SQLException{
		// if this query exists, there already is a valid entry. Workaround for issue that Lucene search does not return time, just date
		String query = "SELECT count(*) FROM `" + rl.getTable() +"`";  // AND isLatest=1 
		query += " WHERE sys_node_uuid='" + rl.getValue("sys_node_uuid")+"' AND cm_modified='"+lastModified+"' ";
		//AND cm_versionLabel='"+rl.getValue("cm_versionLabel")+"'";
		if (rl.hasValue("cm_versionLabel")){
			query += "AND cm_versionLabel='"+rl.getValue("cm_versionLabel")+"'";
		}
		if (logger.isDebugEnabled()) logger.debug("rowEqualsModifiedDate: " + query);
	    ResultSet rs = stmt.executeQuery(query);
	    rs.next();
//	    logger.debug("rowEqualsModifiedDate: Before getLong");
	    long counter = rs.getLong(1);
    	if (logger.isDebugEnabled()) logger.debug("rowEqualsModifiedDate: Found: "+counter);
	    return counter>0;
	}
	
	@SuppressWarnings("deprecation")
	public int updateVersionedIntoTable(Statement stmt, ReportLine rl) throws SQLException{
		
		if (!rowEqualsModifiedDate(stmt, rl, rl.getValue("cm_modified"))){
			
			String query = "UPDATE `"+ rl.getTable() + "` SET validUntil='" + rl.getValue("cm_modified") + "', ";
			query += " isLatest=0 ";
			query += " WHERE sys_node_uuid='" + rl.getValue("sys_node_uuid")+"' AND (isLatest=1)";
			logger.debug("updateVersionedIntoTable: update isLatest: " + query);
			stmt.executeUpdate(query);
			return insertIntoTable(stmt, rl);
		} else {
			return  0;
		}
	
	}
	
	public int updateIntoTable(Statement stmt, ReportLine rl) throws SQLException{
		String query = "UPDATE `"+ rl.getTable() + "` SET " +rl.getUpdateSet() + "";
		query += " WHERE sys_node_uuid='" + rl.getValue("sys_node_uuid")+"'";
		logger.debug("### Update Query: " + query);
		return stmt.executeUpdate(query);
	}
	
	public int insertIntoTable(Statement stmt, ReportLine rl) throws SQLException{
		//logger.debug("### sys_store_protocol="+rl.getValue("sys_store_protocol"));
		if ("archive".equals(rl.getValue("sys_store_protocol"))){
			// validFrom = cm_created, validUntil=sys_archivedDate, isLatest=false
			String created  = rl.getValue("cm_created");
			String archived = rl.getValue("sys_archivedDate");
			
			rl.setLine("isLatest", "BOOLEAN", "false", new Properties());
			rl.setLine("validFrom", "DATETIME", created, new Properties());
			rl.setLine("validUntil", "DATETIME", archived, new Properties());
		} else {
			String modified = rl.getValue("cm_modified");
			rl.setLine("validFrom", "DATETIME", modified, new Properties());
		}
		
		String query = "INSERT INTO `"+ rl.getTable() + "` (" +rl.getInsertListOfKeys()
				+ ") VALUES (" + rl.getInsertListOfValues() + ") ";
//		String query = "INSERT INTO "+ rl.getTable() + " (validFrom, " +rl.getInsertListOfKeys()
//		+ ") VALUES ('" + rl.getValue("cm_modified") + "', " + rl.getInsertListOfValues() + ") ";

		logger.debug("### insert Query: " + query);
		return stmt.executeUpdate(query);
	}

	
	
    /**
     * dropTables drops a list of tables if they exist
     * 
     * @param tables a comma separated list of table names
     */
    public void dropTables(String tablesToDrop){
//    	logger.debug("Starting dropTables: "+tablesToDrop);
    	Statement stmt=null;
		try {
			stmt = getConnection().createStatement();
			for (String table : tablesToDrop.split(",")){
				dropTable(stmt, table);
			}
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
		      //finally block used to close resources
			  try{
			     if(stmt!=null)
			        stmt.close();
			  }catch(SQLException se2){
		      }// nothing we can do
		}
    }
    
    /** 
     * createEmptyTables: creates emtpy tables for the known Alfresco types
     *                    only if the table does not exist yet
     * The one and only column created is sys_node_uuid
     * 
     * @param tables a comma separated list of table names
     */
    public void createEmptyTables(String tablesToCreate){
    	logger.debug("Starting createEmptyTables: " + tablesToCreate);
    	Statement stmt=null;
		try {
			stmt = getConnection().createStatement();
			//String existingTables = getCurrentTables(conn);
			for (String table : tablesToCreate.split(",")){
				//if (existingTables.indexOf(","+table+",")<0){
					createEmptyTable(stmt, table);
				//}
			}
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
		      //finally block used to close resources
			  try{
			     if(stmt!=null)
			        stmt.close();
			  }catch(SQLException se2){
		      }// nothing we can do
		}
    }


	
	public Properties getTableDescription(Statement stmt, String table){
//		logger.debug("Starting getTableDescription");
		Properties props = new Properties();
		
		try {
			stmt = getConnection().createStatement();
			String sql = "DESC " + table +";";
		    ResultSet rs = stmt.executeQuery(sql);
		    while(rs.next()){
		         //Retrieve by column number
		         props.setProperty(rs.getString(1),rs.getObject(2).toString());
		         logger.debug("Found column: "+rs.getString(1) + " - Type: " + rs.getObject(2).toString());
		    }
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
		      //finally block used to close resources
			  try{
			     if(stmt!=null)
			        stmt.close();
			  }catch(SQLException se2){
		      }// nothing we can do
		}
		return props;
	}
	
	private String getCurrentTables(){
//		logger.debug("Starting getCurrentTables");
		Statement stmt=null;
		String returnString = ",";
		try {
			stmt = getConnection().createStatement();
			String sql = "show tables;";
		    ResultSet rs = stmt.executeQuery(sql);
		    while(rs.next()){
		         //Retrieve by column number
		         String value = rs.getString(1);
//		         logger.debug("Found table: "+value);
		         returnString += value+",";
		    }
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
		      //finally block used to close resources
			  try{
			     if(stmt!=null)
			        stmt.close();
			  }catch(SQLException se2){
		      }// nothing we can do
		}
		return returnString;
	}
	
	public String getUsername(){
		return username;
	}
	
	public String getPassword(){
		return password;
	}
	
	public String getJdbcDriver(){
		return jdbcdriver;
	}
	
	public String getDatabase(){
		return database;
	}
	
	public void resetLastTimestampTable(String tableName){
    	Connection conn = getConnection();
    	Statement stmt;
    	final String sql = "UPDATE `" + Constants.TABLE_LASTRUN + 
    			"` SET "+ Constants.COLUMN_STATUS + " = '" + Constants.STATUS_DONE + "'";
    	logger.debug("resetLastTimestampTable: Query=" + sql);
    	
		try {
			conn.setAutoCommit(true);
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			
		} catch (SQLException e) {
			//e.printStackTrace(); // will be thrown initially because table does not exist...
		}	
		if (getNumberOfRowsLastTimestamp(tableName)==0){
    		// create the tables
    		createLastTimestampTableRow(tableName);
    	} 
    }
	
 
	public void dropLastTimestampTable(){
		Connection conn = getConnection();
    	Statement stmt;
    	final String sql = "DELETE FROM `" + Constants.TABLE_LASTRUN + "` WHERE ID>0";
    	logger.debug("resetLastTimestampTable: Query=" + sql);
    	
		try {
			conn.setAutoCommit(true);
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	
    public String getLastTimestampStatus(String tablename){
    	tablename = tablename.replaceAll("-", "_");
    	logger.debug("enter getLastTimestampStatus for table="+tablename);
    	final String sql = "SELECT " + Constants.COLUMN_STATUS +
    			" FROM `" + Constants.TABLE_LASTRUN +
    			"` WHERE " + Constants.COLUMN_TABLENAME + "='" + tablename+ "' ";
    	logger.debug("getLastTimestampStatus: Query=" + sql);
    	String returnString = "";
    	Connection conn = getConnection();
    	Statement stmt;
		try {
			conn.setAutoCommit(true);
			stmt = conn.createStatement();
			java.sql.ResultSet rs = stmt.executeQuery(sql);
			logger.debug("getLastTimestampStatus: ...");
			if ((rs!=null) && (!rs.wasNull())){
				rs.next();
				returnString = rs.getString(1);
			} else {
				// if there is no date available, make it somewhere early 1970, so we 
				// include as much as possible in the reporting DB 
		    	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    	Date myDate = new Date(1);
		    	returnString = format.format(myDate);
		    	returnString = returnString.replaceAll(" ", "T").trim();
			}
		} catch (SQLException e) {
			//e.printStackTrace();
			// don't bother. Will throw exeption when this table is empty
			// or non-existent. Then will be created later on
		}	catch (Exception e){
			// nothing... usually triggered the very first time when there is no table yet
		}
		logger.debug("exit getLastTimestampStatus returning: " + returnString);
    	return returnString;
    }

    private void setLastTimestampStatus(String tablename, String newStatus){
    	//lastrun-updateLastSuccessfulRunForTable
    	tablename = tablename.replaceAll("-", "_");
    	logger.debug("enter setLastTimestampStatus table="+tablename);
    	if (getNumberOfRowsLastTimestamp(tablename)==0){
    		// create the tables
    		createLastTimestampTableRow(tablename);
    	} 
    	
    	String status;
    	if (newStatus.toLowerCase().equals(Constants.STATUS_DONE)){
    		status = Constants.STATUS_DONE;
    	} else {
    		status = Constants.STATUS_RUNNING;
    	}
    	final String sql = "UPDATE `" + Constants.TABLE_LASTRUN + 
    			"` SET "+ Constants.COLUMN_STATUS + " = '" + status + "'" +
    			" WHERE (" + Constants.COLUMN_TABLENAME +"='" + tablename+ "')";
    	logger.debug("getLastTimestampStatus: Query=" + sql);
    	Connection conn = getConnection();
    	Statement stmt;
		try {
			conn.setAutoCommit(true);
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			//e.printStackTrace();//
		}
    }

    private int getNumberOfRowsLastTimestamp(String tablename){
    	//lastrun-selectCountForTablename
    	tablename = tablename.replaceAll("-", "_");
    	logger.debug("enter getNumberOfRowsLastTimestamp");
    	final String sql = "SELECT count(*) FROM `" + Constants.TABLE_LASTRUN +
    			"` WHERE " + Constants.COLUMN_TABLENAME + "='" + tablename +"'";
    	logger.debug("getNumberOfRowsLastTimestamp: Query=" + sql);
    	int returnInt = 0;
    	Connection conn = getConnection();
    	Statement stmt;
		try {
			conn.setAutoCommit(true);
			stmt = conn.createStatement();
			java.sql.ResultSet rs = stmt.executeQuery(sql);
			rs.next();
			returnInt = rs.getInt(1);
		} catch (SQLException e) {
			//e.printStackTrace();//
		}	
		logger.debug("exit getNumberOfRowsLastTimestamp returning=" + returnInt);
    	return returnInt;
    }
    

    public String getLastTimestamp(String tableName){
    	//lastrun-selectLastRunForTable
    	tableName = tableName.replaceAll("-", "_");
    	logger.debug("enter getLastTimestamp table="+tableName);
    	final String sql = "SELECT " + Constants.COLUMN_LASTRUN + 
    			" FROM `" + Constants.TABLE_LASTRUN +
    			"` WHERE " + Constants.COLUMN_TABLENAME +"='" + tableName + "'";
    	logger.debug("getLastTimestampStatus: Query=" + sql);
    	String returnString = "";
    	Connection conn = getConnection();
    	Statement stmt;
		try {
			conn.setAutoCommit(true);
			stmt = conn.createStatement();
			java.sql.ResultSet rs = stmt.executeQuery(sql);
			if ((rs!=null) && (!rs.wasNull())){
				rs.next();
				returnString = rs.getString(1);
			} else {
				// if there is no date available, make it somewhere early 1970, so we 
				// include as much as possible in the reporting DB 
		    	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		    	Date myDate = new Date(1);
		    	returnString = format.format(myDate);
		    	returnString = returnString.replaceAll(" ", "T").trim();
			}
		} catch (SQLException e) {
			//e.printStackTrace();//
			//e.getMessage();
		} catch (Exception e) {
			// nothing
		}	
		if ((returnString==null) ||(returnString.trim().equals(""))){
			// if there is no date available, make it somewhere early 1970, so we 
			// include as much as possible in the reporting DB 
	    	SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    	Date myDate = new Date(1);
	    	returnString = format.format(myDate);
	    	returnString = returnString.replaceAll(" ", "T").trim();
		}
		logger.debug("exit getLastTimestamp returning " + returnString);
    	return returnString;
    }

    public boolean tableIsRunning(String tableName){
    	//
    	tableName = tableName.replaceAll("-", "_");
    	logger.debug("enter tableIsRunning table="+tableName);
    	final String sql = "SELECT count(*) " + 
    			" FROM `" + Constants.TABLE_LASTRUN + "`" +
    			" WHERE " +Constants.COLUMN_STATUS +"='"+Constants.STATUS_RUNNING+"'";
    	logger.debug("tableIsRunning: Query=" + sql);
    	boolean returnBoolean = false;
    	Connection conn = getConnection();
    	Statement stmt;
		try {
			conn.setAutoCommit(true);
			stmt = conn.createStatement();
			java.sql.ResultSet rs = stmt.executeQuery(sql);
			if ((rs!=null) && (!rs.wasNull())){
				rs.next();
				returnBoolean = rs.getBoolean(1);
			} 
		} catch (SQLException e) {
			//e.printStackTrace();//
			//e.getMessage();
		} catch (Exception e) {
			// nothing
		}	
		logger.debug("exit tableIsRunning returning " + returnBoolean);
		// IT IS A BAD THING TO RUN A HARVESTING JOB IF ANY OTHER HARVESTING JOB IS RUNNING
    	return returnBoolean;
    }

    public void setLastTimestampAndStatusDone(String tableName, String timestamp){
    	//lastrun-updateLastSuccessfulRunForTable
    	tableName = tableName.replaceAll("-", "_");
    	logger.debug("enter setLastTimestamp");
    	
    	if (getNumberOfRowsLastTimestamp(tableName)==0){
    		// create the tables
    		createLastTimestampTableRow(tableName);
    	}
    	final String sql = "UPDATE `" + Constants.TABLE_LASTRUN + "` SET " + 
    			Constants.COLUMN_LASTRUN + " = '" + timestamp + "', " +
    			Constants.COLUMN_STATUS + " = '" + Constants.STATUS_DONE + "'" +
    			" WHERE " + Constants.COLUMN_TABLENAME + "='" + tableName +"'";
    	logger.debug("getLastTimestampStatus: Query=" + sql);
    	Connection conn = getConnection();
    	Statement stmt;
		try { 
			conn.setAutoCommit(true);
			stmt = conn.createStatement();
			stmt.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();//
		} catch (Exception e) {
			// nothing
		}
    }
        
    public void setLastTimestampStatusRunning(String tableName) {
    	tableName = tableName.replaceAll("-", "_");
    	logger.debug("enter setLastTimestamp: for table="+tableName);
    	if (getNumberOfRowsLastTimestamp(tableName)==0){
    		// create the tables
    		createLastTimestampTableRow(tableName);
    	} 
    	setLastTimestampStatus(tableName, Constants.STATUS_RUNNING);
    	logger.debug("exit setLastTimestamp");
    }
    
    
    public void createLastTimestampTableRow(String tableName){
    	//lastrun-insertTablename
    	tableName = tableName.replaceAll("-", "_");
    	logger.debug("enter createLastTimestampTableRow table=" + tableName);
    	try {
    		//createEmptyTables(Constants.TABLE_LASTRUN);
        	createEmptyTables(Constants.TABLE_LASTRUN);
        	Connection conn = getConnection();
        	conn.setAutoCommit(true);
        	Statement stmt = conn.createStatement();
        	extendTable(stmt, Constants.TABLE_LASTRUN, Constants.COLUMN_TABLENAME, "VARCHAR(50)");
        	extendTable(stmt, Constants.TABLE_LASTRUN, Constants.COLUMN_LASTRUN, "VARCHAR(50)");
        	extendTable(stmt, Constants.TABLE_LASTRUN, Constants.COLUMN_STATUS, "VARCHAR(10)");
		} catch (SQLException e) {
			// do nothing
			//e.printStackTrace();//
		}
	   	try {
	   		Connection conn = getConnection();
        	conn.setAutoCommit(true);
        	Statement stmt = conn.createStatement();
        	String sql = "INSERT INTO `" + Constants.TABLE_LASTRUN + 
        				"` (isLatest, "+Constants.COLUMN_LASTRUN + ", " + Constants.COLUMN_STATUS+ ", " + Constants.COLUMN_TABLENAME+")" +
        			" VALUES (1, '', '', '" + tableName + "')";
        	logger.debug("createLastTimestampTableRow: Query=" + sql);
        	stmt.executeUpdate(sql);
		} catch (SQLException e) {
			//e.printStackTrace();//
		}
		logger.debug("exit createLastTimestampTableRow");
    }
}
