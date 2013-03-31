package org.alfresco.reporting.mybatis;

import java.util.List;

public interface ReportingDAO {
	
	public  List<String> getShowTables();
	
	public int getNumberOfRowsForTable(String tablename);
	
}
