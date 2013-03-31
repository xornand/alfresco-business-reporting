/**
 * Copyright (C) 2011 - 2013 Alfresco Business Reporting project
 * 
 * This file is part of the Alfresco Business Reporting project.
 * 
 * Licensed under the GNU LGPL, Version 3.0 (the "License"); you may not use
 * this file except in compliance with the License. You may obtain a copy of the
 * License at
 * 
 * http://www.gnu.org/licenses/lgpl.html
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

var timestampsAreDateOnly = false; // the Alfresco default. Change settings if
									// Lucene stores full time too.

//------------------------------------------------------------------------

function main(harvestRef, frequency) {
	logger.log("Welcome in main!");
	try{
		if (reporting.isHarvestEnabled()) {
			
			var harvestDefinitions;
			
			logger.log("Checking moreFrequent");
			if ((frequency!=null) && (frequency.toLowerCase()=="morefrequent")) {
				harvestDefinitions = search
					.luceneSearch("+TYPE:\"reporting:harvestDefinition\" +@reporting\\:harvestFrequency:\"moreFrequent\"");
				logger.log("it is a moreFrequent!");
			}
			
			logger.log("Checking lessFrequent");
			if ((frequency!=null) && (frequency.toLowerCase()=="lessfrequent")) {
				harvestDefinitions = search
					.luceneSearch("+TYPE:\"reporting:harvestDefinition\" +@reporting\\:harvestFrequency:\"lessFrequent\"");
				logger.log("It is a lessFrequent!");
			}
			
			logger.log("Checking harvestAll");
			if ((frequency!=null) && (frequency=="all")) {
				harvestDefinitions = search
					.luceneSearch("TYPE:\"reporting:harvestDefinition\"");
				logger.log("It is an All!");
			}
			
			logger.log("Checking harvestRef!=null");
			if (harvestRef!=null){ 
				var harvestDefinitions = new Array();
				var noderef = search.findNode(harvestRef);
				if (noderef.type=="{http://www.alfresco.org/model/reporting/1.0}harvestDefinition"){
					harvestDefinitions.push(search.findNode(harvestRef));
					logger.log("There is valid a nodeRef!! " + harvestRef);
				} else {
					logger.log("There is a lousy nodeRef!! " + harvestRef);
					harvestDefinitions = search.luceneSearch("TYPE:\"reporting:harvestDefinition\"");
					logger.log(">> It is an All after all!");
				}
			}	
			
			logger.log("Number of results: " + harvestDefinitions.length);
			
			for (var h in harvestDefinitions){
				var harvestDefinition = harvestDefinitions[h];
				logger.log("main: (" + h + ") Processing: " + harvestDefinition.name);
	
				if (harvestDefinition.properties["reporting:queryTablesEnabled"]) {
					processQueryTables(harvestDefinition);
				}
		
				if (harvestDefinition.properties["reporting:usersGroupsEnabled"]) {
					processUsersAndGroups(harvestDefinition);
				}
		
				if (harvestDefinition.properties["reporting:auditingExportsEnabled"]) {
					processAuditingExport(harvestDefinition);
				}
		
				if (harvestDefinition.properties["reporting:categoriesEnabled"]) {
					processCategories(harvestDefinition);
				}
			} // end for
		} // end if isHarvestEnabled
		else {
			logger.log("Harvesting not enabled...")
			}
	} catch (exception){
		// can we catch the unhandled exception from the Scheduled Job?
		// nothing
		logger.log("Bad news!: " + exception);
	}
} // end main

// ------------------------------------------------------------------------

function trim(instring) {
	try {
		while (instring.indexOf(" ") == 0) {
			instring = instring.substring(1, instring.length);
		}
		// TODO and the end too
	} catch (e) {
		// nothing
	}
	return instring;
}

// ------------------------------------------------------------------------

/**
 * get the array of queries
 * 
 * @param harvestDefinition
 *            ScriptNode containing *all* definitions
 * @returns Properties object
 */
function getReportingQueries(harvestDefinition) {
	var result = new Object();
	if ((harvestDefinition.properties.content!=null) && 
			(harvestDefinition.properties.content!=null) && 
			(harvestDefinition.properties.content.size>0)){
		
		var content = new String();
		content = harvestDefinition.properties.content.content;

		if ((content!=null) && (content !=undefined)){

			var lines = content.split("\n");
		//logger.log("%%5");
			var index; // numeric index of the string array
			var line = new String(); // to be string value of each entry in string array
			for ( var index in lines) {
				line = lines[index];
				if ((line!=null) && 
					(line!=undefined) &&
					!(trim(line).indexOf("#") == 0)) {
					var keyvalue = line.split("=");
					logger.log("## storing key=" + trim(keyvalue[0]) + " value="
							+ trim(keyvalue[1]));
					result[trim(keyvalue[0])] = trim(keyvalue[1]);
				} // end if line.startwWith
			} // end for (line in lines)
		} // end if content != null
	} // end content.size>0
	else {
		logger.log("NO GO!");
	}
	return result;
}

// ------------------------------------------------------------------------

function processQueryTables(harvestDefinition) {
	// get the Properties object with tablename=query values
	var queries = getReportingQueries(harvestDefinition);
    //if (queries.length>0){
		// auto create tables that are needed
		var tableNames = "";
		var tableName;
		for (tableName in queries) {
			if ((tableName!=null) && (tableName!=undefined) && (tableName.length>3)){
				if (tableNames.length > 0)
					tableNames += ",";
				tableNames += tableName;
			}
		}
		logger.log("Querytables, Creating tables: " + tableNames);

		// only needed for JasperSoft
		// reporting.setDataType("JNDI");

		startQueries(queries);

    //} // end queries.length>0
} // end function
    

// ------------------------------------------------------------------------

function processUsersAndGroups(harvestDefinition) {
	// ------------- process Person objects -----------------
	// we need to set this date if the run to fill the reporting DB was
	// successful
	if (!reporting.tableIsRunning("person")){
		var thisTimestamp = new Date();
		reporting.dropTables("person");
		reporting.createEmptyTables("person");
	
		// update the status of the status table
		reporting.setLastTimestampStatusRunning("person");
	
		reporting.processPerson("person");
	
		// persist the date of this successful run
		setQueryDate("person", thisTimestamp);
	}
	
	// ------------- process Group objects -----------------
	// we need to set this date if the run to fill the reporting DB was
	// successful
	if (!reporting.tableIsRunning("groups")){
		thisTimestamp = new Date();
		reporting.dropTables("groups");
		reporting.createEmptyTables("groups");
	
		// update the status of the status table
		reporting.setLastTimestampStatusRunning("groups");
	
		reporting.processGroups("groups");
	
		// persist the date of this successful run
		setQueryDate("groups", thisTimestamp);
	}
	// ------------- process SitePerson details -----------------
	// we need to set this date if the run to fill the reporting DB was
	// successful
	if (!reporting.tableIsRunning("siteperson")){
		thisTimestamp = new Date();
		reporting.dropTables("siteperson");
		reporting.createEmptyTables("siteperson");
		// update the status of the status table
		reporting.setLastTimestampStatusRunning("siteperson");
	
		reporting.processSitePerson("siteperson");
	
		// persist the date of this successful run
		setQueryDate("siteperson", thisTimestamp);
	}
}

// ------------------------------------------------------------------------

function processAuditingExport(harvestDefinition) {
	var auditFeeds = harvestDefinition.properties["reporting:auditingQueries"];
	if ((auditFeeds != null) && (auditFeeds!=undefined)){
		var feeds = auditFeeds.split(",");

		for ( var f in feeds) {
			logger.log("processAuditingExport with id: " + f + ", " + feeds[f]);
			var feed = trim(feeds[f]);
			if ((feed!=null) && 
					(feed!=undefined) && 
					(feed.length>3) && 
					!reporting.tableIsRunning(feed.toLowerCase())){

				logger.log("processAuditingExport with feed: " + feed);
				var table = feed.toLowerCase();
				logger.log("processAuditingExport with table: " + table);
				reporting.createEmptyTables(table);

				// we need to set this date if the run to fill the reporting DB was
				// successful
				var thisTimestamp = new Date();

				// update the status of the status table
				reporting.setLastTimestampStatusRunning(table);
				logger.log("START RUNNING processAuditingExport with " + feed);
				reporting.processAuditingExport(feed);
				// persist the date of this successful run
				setQueryDate(table, thisTimestamp);
			} // end if feed !=null
		} // end for table in tables
	}

}

// ------------------------------------------------------------------------

function processCategories(harvestDefinition) {
	var categories = harvestDefinition.properties["reporting:categories"];
	var tableNames = "";
	
	if ((categories != null) && (categories!=undefined) && (categories != "")) {
		logger.log("#### We zijn wel verder...");
		for ( var c = 0; c < categories.length; c++) {
			if ((categories[c]!=null) && (categories[c]!=undefined)){
				if (tableNames.length > 0)
					tableNames += ",";
				tableNames += categories[c].name;
			} // end if null OR undefined
		}
		
		logger.log("Processing categories: " + tableNames);

		for ( var c = 0; c < categories.length; c++) {
			var tablename = categories[c].name.toLowerCase();
			if (!reporting.tableIsRunning(tablename)){
				
				logger.log("There we go: " + tablename);
				reporting.dropTables(tablename);
				logger.log("Categories, Creating table: "+tablename);
				reporting.createEmptyTables(tablename);
	
				// we need to set this date if the run to fill the reporting DB was
				// successful
				var thisTimestamp = new Date();
	
				// update the status of the status table
				reporting.setLastTimestampStatusRunning(tablename);
	
				reporting.processCategoriesAsPath(tablename, // table
															// name,
				categories[c].name, // RootCategoryName
				categories[c].name.toLowerCase() + "Path"); // reporting_table_column_name
	
				// persist the date of this successful run
				setQueryDate(tablename, thisTimestamp);
			} // end if if (!reporting.tableIsRunning())
		}
	}
}


// --------------------------------------------------------------------------

function stringLength(input) {
	var myInput = new String(input);
	if (myInput.length < 2) {
		myInput = "0" + myInput;
	}
	return myInput;
}

// --------------------------------------------------------------------------

function setQueryDate(tablename, timestamp) {
	logger.log("Enter setQueryDate for timestamp=" + timestamp + " and table="
			+ tablename);
	// prepare the new time string

	var timeString = "";
	if (timestampsAreDateOnly) {
		// store date only. Replace time by 00:00:00 in order to find all
		// documents created within this day
		timeString = timestamp.getFullYear() + "-"
				+ stringLength(timestamp.getMonth() + 1) + "-"
				+ stringLength(timestamp.getDate()) + "T00:00:00";
	} else {
		timeString = timestamp.getFullYear() + "-"
				+ stringLength(timestamp.getMonth() + 1) + "-"
				+ stringLength(timestamp.getDate()) + "T"
				+ stringLength(timestamp.getHours()) + ":"
				+ stringLength(timestamp.getMinutes()) + ":"
				+ stringLength(timestamp.getSeconds());
	}
	reporting.setLastTimestampAndStatusDone(tablename, timeString);
	logger.log("Enter setQueryDate for timeString=" + timeString);

}

// --------------------------------------------------------------------------
// startQueries
// --------------------------------------------------------------------------

function startQueries(queries) {
	var storeRefs = reporting.getStoreList().split(",");

	for (s = 0; s < storeRefs.length; s++) {
		try{
			logger.log("in startQueries");
			for ( var table in queries) {
	
				var letsGo = true; // allow the initial run
				var dbid = "0"; // the initial value of the highest dbid of the
								// current run
				var query = ""; // the final query, the configured base query
								// expanded with modified-date and node-dbid
				var results; // array containing the resultset of the query
				var counter = 0; // counts the total number of objects for the
								// given query/table
				reporting.createEmptyTables(table);
				
				if (!reporting.tableIsRunning(table)){
					dateQuery = reporting.getLastTimestamp(table);
					reporting.setLastTimestampStatusRunning(table);
					if ("" != dateQuery) {
						dateQuery = " AND @cm\\:modified:[" + dateQuery + " TO NOW]";
					}
		
					logger.log("@@ new dateQuery=" + dateQuery);
		
					// we need to set this date if the run to fill the reporting DB was
					// successful
					var thisTimestamp = new Date();
		
					// process multiple queries, because the max result set is set to
					// 1000 (=default; or bound by time)
					// either way, we do not know how many queries we need to fire
					// before we capture all objects
					// that should be stored in the Reporting Database
					while (letsGo) {
						// query = queries[table] + query + " AND NOT
						// @sys:node-uuid=NULL " + dateQuery;
						query = queries[table] + query + dateQuery;
		
						logger.log("Query: " + query);
						// execute the actual query
						results = search.luceneSearch(storeRefs[s], query,
								"@sys:node-dbid", true);
						// logger.log("@@@@ Results=" + (results!=null));
						// logger.log("@@@@ Length =" + results.length);
						letsGo = (letsGo && (results != null) && (results.length > 0));
						// logger.log("@@@@ letsGo=" + letsGo);
		
						if (letsGo) {
			
							for ( var j = 0; j < results.length; j++) {
								try { // prevent annoying breaking exception if
										// content not exists
									var result = results[j];
									logger.log("Processing " + result.name);
									if ((result != null) && (result != undefined)) {
										try {
		
											if (result.isVersioned /*
																	 * &&
																	 * (result.properties["cm:versionLabel"]!="0.1" )
																	 */) {
												logger
														.log("############### WE HAVE A VERSION ##################");
												var version;
												var versionNode;
												var versions = result.versionHistory;
												logger.log("versions.length="
														+ versions.length);
												if (versions.length > 1) {
		
													for ( var v = 0; v < versions.length; v++) {
														version = versions[versions.length
																- v - 1];
														versionNode = version.node;
														//reporting.setTable(table);
														reporting.addToQueue(result,
																versionNode);
														counter++;
													}
		
												} else {
													//reporting.setTable(table);
													reporting.addToQueue(result);
													counter++;
												}
		
											} else {
												//reporting.setTable(table);
												reporting.addToQueue(result);
												counter++;
											}
		
										} catch (exception) {
											// object appears null-ish
											logger.log("ERROR: " + exception);
										}
									} // end if(result)
								} catch (exception) {
									logger
											.log("There is a problem, but we're going to ignore it!");
								}
							} // end for
							reporting.executeQueue(table);
							reporting.resetAll();
							dbid = results[results.length - 1].properties["sys:node-dbid"] + 1;
							// modify the query for the next run
							query = " AND @sys\\:node-dbid:[" + dbid + " TO MAX]";
							logger.log("Number of objects found: " + counter
									+ " New query: " + query);
						} // end if results letsGo
						else {
							logger.log("No results for query " + query);
							letsGo = false;
						}
		
						if (results) {
							logger
									.log("#### Number of results (lets see if we continue: "
											+ results.length);
						} else {
							logger.log("#### results appears NULL...");
						}
		
						if ((results != null) && (results.length > 0)) {
							// get the highest dbid found in the current search (next
							// search should start from here)
							dbid = results[results.length - 1].properties["sys:node-dbid"] + 1;
							// modify the query for the next run
							query = " AND @sys\\:node-dbid:[" + dbid + " TO MAX]";
							logger.log("Number of objects found: " + counter
									+ " New query: " + query);
						} else {
							// there is no next search...
							letsGo = false;
							query = " AND @sys\\:node-dbid:0";
							logger.log("#### LETS-GO = false, afnokken...");
						} // end if results.length
					
					} // end while
					logger.log("Processed " + counter + " items for table " + table
							+ ". Done now");
		
					// persist the date of this successful run
					setQueryDate(table, thisTimestamp);
				} // end if (!reporting.tableIsRunning())
			} // end for tables
		} catch (exception){
			// do nothing. prevent org.springframework.transaction.UnexpectedRollbackException...
		}
			logger.log("Done all tables in Store " + storeRefs[s]);
	} // end for storeRefs
	logger.log("Done all stores");
} // end function startQueries
