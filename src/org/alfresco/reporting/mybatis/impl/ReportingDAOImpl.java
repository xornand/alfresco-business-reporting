package org.alfresco.reporting.mybatis.impl;

import java.sql.ResultSet;
import java.util.List;
import java.util.Properties;

import org.alfresco.reporting.mybatis.ReportingDAO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;


public class ReportingDAOImpl implements ReportingDAO {

	private static Log logger = LogFactory.getLog(ReportingDAOImpl.class);
	private SqlSession template = null;
	private Properties globalProperties = null;
	
	public List<String> getShowTables(){
		@SuppressWarnings("unchecked")
		List<String> results = (List<String>)template.selectList("show-tables");
		return results;
	}
	
	public int getNumberOfRowsForTable(String tablename){
		return (Integer)template.selectOne("show-table-count", tablename);
	}
	
	
	// ------------------------------------------------
	
	
	public void setReportingTemplate(SqlSessionTemplate template){
		this.template = template;
	}
	
	public void setProperties(Properties properties){
		this.globalProperties = properties;
	}
	
}
