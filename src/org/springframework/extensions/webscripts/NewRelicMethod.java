package org.springframework.extensions.webscripts;

import org.springframework.extensions.webscripts.processor.BaseProcessorExtension;

public class NewRelicMethod extends BaseProcessorExtension {
	 public String getBrowserTimingHeader(){
	    	return com.newrelic.api.agent.NewRelic.getBrowserTimingHeader();
		}
		
	 public String getBrowserTimingFooter(){
			return com.newrelic.api.agent.NewRelic.getBrowserTimingFooter();
			
		}
}
