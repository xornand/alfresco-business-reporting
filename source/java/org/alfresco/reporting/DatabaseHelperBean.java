package org.alfresco.reporting;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DatabaseHelperBean {

	private static Log logger = LogFactory.getLog(DatabaseHelperBean.class);
	private String database;
	private String jdbcdriver;
	private String username;
	private String password;
	private Connection conn;
	//private Properties properties;
	
	//private String initialTableList = "document,site,folder,person,datalist,datalistitem,package,task,forum,topic,post";
	
	 public void setMyProperties(Properties properties)
	 {
		//this.properties = properties;
	 	this.username  =properties.getProperty("reporting.username");
	 	this.password  =properties.getProperty("reporting.password");
	 	this.database  =properties.getProperty("reporting.url");
	 	this.jdbcdriver=properties.getProperty("reporting.driver");
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

	public void init(){
		logger.debug("Starting getTableDescription");
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
			    	logger.info("  " + table + " ("+ rs2.getLong(1)+ ")");	
			    }
		    }
		} catch (SQLException e) {
			// TODO Auto-generated catch block
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
	
	public Connection getConnection(){
		try {
			if ((this.conn == null) || conn.isClosed()){
				logger.debug("DatabaseURL: " + database);
				logger.debug("JDBC Driver: " + jdbcdriver);
				logger.debug("Username   : " + username);
				Class.forName(jdbcdriver);
				logger.info("Connecting to database " + database);
				conn = DriverManager.getConnection(database,username,password);
			}
	   } catch(SQLException se){
		      //Handle errors for JDBC
		      se.printStackTrace();
	   } catch (ClassNotFoundException e) {
		// TODO Auto-generated catch block
		   e.printStackTrace();
	   }	
		 
		return conn;
	}
	
	
	private void dropTable(Statement stmt, String table) throws SQLException{
		String query = "DROP TABLE IF EXISTS "+table+";";	
		logger.debug("Dropping table. Query: "+query);
		int numberOfLines = stmt.executeUpdate(query);
	}
	
	
	private void createEmptyTable(Statement stmt, String table) throws SQLException{
		String query = "CREATE TABLE IF NOT EXISTS " + table + " (id INT AUTO_INCREMENT PRIMARY KEY, sys_node_uuid VARCHAR(100));";
		logger.debug("Creating table. Query: "+query);
		int numberOfLines = stmt.executeUpdate(query);
	}
	
	
	public void extendTable(Statement stmt, String table, String column, String type) throws SQLException{
		
		//ALTER TABLE $table ADD $column $type
		String query = "ALTER TABLE " + table + " ADD " + column + " " + type;
		logger.debug("Executing Query: "+query);
		int numberOfLines = stmt.executeUpdate(query);
	}

	
	public boolean rowExists(Statement stmt, ReportLine rl) throws SQLException{
	
		String query = "SELECT count(*) FROM " + rl.getTable();
		query += " WHERE sys_node_uuid='" + rl.getValue("sys_node_uuid")+"'";
		logger.debug(query);
	    ResultSet rs = stmt.executeQuery(query);
	    rs.next();
	    logger.debug("Before getLong");
	    long counter = rs.getLong(1);
    	logger.debug("Found: "+counter);
	    return counter>0;
	}
	
	
	
	public int updateIntoTable(Statement stmt, ReportLine rl) throws SQLException{
		String query = "UPDATE "+ rl.getTable() + " SET " +rl.getUpdateSet() + "";
		query += " WHERE sys_node_uuid='" + rl.getValue("sys_node_uuid")+"'";
		logger.info("### Update Query: " + query);
		return stmt.executeUpdate(query);
	}
	
	public int insertIntoTable(Statement stmt, ReportLine rl) throws SQLException{
		
		String query = "INSERT INTO "+ rl.getTable() + " (" +rl.getInsertListOfKeys()
				+ ") VALUES (" +rl.getInsertListOfValues() + ") ";
		logger.info("### insert Query: " + query);
		return stmt.executeUpdate(query);
	}

	
	
    /**
     * dropTables drops a list of tables if they exist
     * 
     * @param tables a comma separated list of table names
     */
    public void dropTables(String tablesToDrop){
    	logger.debug("Starting dropTables: "+tablesToDrop);
    	Statement stmt=null;
		try {
			stmt = getConnection().createStatement();
			for (String table : tablesToDrop.split(",")){
				dropTable(stmt, table);
			}
			stmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
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
			// TODO Auto-generated catch block
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
		logger.debug("Starting getTableDescription");
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
			// TODO Auto-generated catch block
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
		logger.debug("Starting getCurrentTables");
		Statement stmt=null;
		String returnString = ",";
		try {
			stmt = getConnection().createStatement();
			String sql = "show tables;";
		    ResultSet rs = stmt.executeQuery(sql);
		    while(rs.next()){
		         //Retrieve by column number
		         String value = rs.getString(1);
		         logger.debug("Found table: "+value);
		         returnString += value+",";
		    }
		} catch (SQLException e) {
			// TODO Auto-generated catch block
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
}
