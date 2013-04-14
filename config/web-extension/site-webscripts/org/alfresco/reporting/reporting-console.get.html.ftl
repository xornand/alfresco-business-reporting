
<#assign el=args.htmlid?html>


<div id="${el}-body" class="reporting-console">
 	<div class="header-bar">
        <div class="title">Alfresco Business Reporting</div>
    </div>
	<div id="${el}-main">
	    
		<div id="${el}-inputTabs" class="yui-navset">
		    <ul class="yui-nav">
		        <li class="selected"><a href="#itab1"><em>Reporting Database</em></a></li>
		        <li><a href="#itab2"><em>Harvesting data (to be)</em></a></li>
		        <li><a href="#itab3"><em>Report execution (to be)</em></a></li>
		    </ul>            
		    <div id="${el}-displayReportingDatabase" class="yui-content">
		    <table cellpadding="2">
		    <tr>
		    	<td colspan="3"></td>
		    	<th colspan="4" align="center">Number of rows...</th>
		    </tr>
		    <tr>
				<th>Table</th>
		    	<th>Last run</th>
		    	<th>Status</th>
		    	<th>#rows</th>
		    	<th>#isLatest</th>
		    	<th>#non-latest</th>
		    	<th>#workspace</th>
		    	<th>#archive</th>
		    </tr>
		    <#list reportingtables as reportingtable>
		    
		    	<tr>
		    		<td> ${reportingtable.table!""} </td>
		    		<td align="center"> ${reportingtable.last_run!""} &nbsp;</td>
		    		<td> ${reportingtable.status!""} &nbsp;</td>
		    		<td align="right"> ${reportingtable.number_of_rows!""} &nbsp;</td>
		    		<td align="right"> ${reportingtable.number_of_latest!""} &nbsp;</td>
		    		<td align="right"> ${reportingtable.number_of_non_latest!""} &nbsp;</td>
		    		<td align="right"> ${reportingtable.number_in_workspace!""} &nbsp;</td>
		    		<td align="right"> ${reportingtable.number_in_archivespace!""} &nbsp;</td>
		    	</tr>
		    
		    </#list>
		    </table>
		    <p>&nbsp;</p>
		    <p><i>The status Done means the table is not active. Previous run was completed as expected. Status Running means the tool is actively working against this table (of failed to complete normally)</i></p>
		    </div>
		</div>
	</div>
</div>
