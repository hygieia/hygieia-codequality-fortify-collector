package com.capitalone.dashboard.collector;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.springframework.web.client.RestClientException;

import com.capitalone.dashboard.model.FortifyProject;
import com.capitalone.dashboard.model.FortifyScanReport;
import com.capitalone.dashboard.util.EncryptionException;

public interface FortifyClient {

	void setServerDetails(Set<Map<String,String>> fortifyServers) throws EncryptionException;

	List<FortifyProject> getApplications(String instanceUrl, Collection<JSONObject> collection) throws ParseException;

	FortifyScanReport getFortifyReport(FortifyProject application, JSONObject latestVersionObject) throws ParseException, java.text.ParseException;
	
	Map<String, JSONObject> getApplicationArray(String instanceUrl) throws ParseException, RestClientException;
}
