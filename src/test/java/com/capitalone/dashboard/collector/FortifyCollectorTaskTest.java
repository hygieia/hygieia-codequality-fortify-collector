package com.capitalone.dashboard.collector;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.bson.types.ObjectId;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.client.RestClientException;

import com.capitalone.dashboard.model.CodeQuality;
import com.capitalone.dashboard.model.Component;
import com.capitalone.dashboard.model.Configuration;
import com.capitalone.dashboard.model.FortifyCollector;
import com.capitalone.dashboard.model.FortifyProject;
import com.capitalone.dashboard.model.FortifyScanReport;
import com.capitalone.dashboard.repository.CodeQualityRepository;
import com.capitalone.dashboard.repository.ComponentRepository;
import com.capitalone.dashboard.repository.ConfigurationRepository;
import com.capitalone.dashboard.repository.FortifyCollectorRepository;
import com.capitalone.dashboard.repository.FortifyProjectRepository;
import com.capitalone.dashboard.repository.FortifyScanRepository;

@RunWith(MockitoJUnitRunner.class)
public class FortifyCollectorTaskTest {
    @Mock private TaskScheduler taskScheduler;
    @Mock private FortifyCollectorRepository fortifyCollectorRepository;
    @Mock private FortifyProjectRepository fortifyProjectRepository;
    @Mock private FortifyScanRepository fortifyScanRepository;
    @Mock private FortifySettings fortifySettings;
    @Mock private ComponentRepository dbComponentRepository;
    @Mock private ConfigurationRepository configurationRepository;
    @Mock private FortifyClient fortifyClient;
    @Mock private CodeQualityRepository codeQualityRepository;

    @InjectMocks private FortifyCollectorTask task;

    @Test
    public void collect_noBuildServers_nothingAdded() {
        when(configurationRepository.findByCollectorName(any())).thenReturn(new Configuration());
        task.collect(new FortifyCollector());
        verifyZeroInteractions(fortifyScanRepository);
        verifyZeroInteractions(codeQualityRepository);
    }
    
    @Test
    public void collector_register() {
        when(configurationRepository.findByCollectorName(any())).thenReturn(configuration());
        FortifyCollector collector = task.getCollector();
        
        assertThat(collector.getFortifyServers().size(), is(1));
        assertThat(collector.getFortifyServers().get(0), is("http://mockServer/ssc"));
        verifyZeroInteractions(codeQualityRepository);
    }
    
    
    @Test
    public void noDuplicateFortifyScanReports() throws Exception {
    	Map<String, JSONObject> applications = getApplications();
    	
        when(dbComponentRepository.findAll()).thenReturn(components());
        when(configurationRepository.findByCollectorName(any())).thenReturn(configuration());
        when(fortifyClient.getApplicationArray(anyString())).thenReturn(applications);
        when(fortifyProjectRepository.findEnabledProjects(any(ObjectId.class), anyString())).thenReturn(fortifyProject());
        when(codeQualityRepository.findByCollectorItemIdAndTimestamp(any(ObjectId.class), anyLong())).thenReturn(new CodeQuality());
        
        task.collect(fortifyCollector(configuration()));
        
        verify(codeQualityRepository, never()).save(any(CodeQuality.class));
        verify(fortifyScanRepository, never()).save(any(FortifyScanReport.class));
    }
    
    @Test
    public void scanReportTest() throws Exception {
    	FortifyScanReport fortifyScanReport = new FortifyScanReport();
    	fortifyScanReport.setCollectorItemId(new ObjectId());
    	Map<String, JSONObject> applications = getApplications();
    	
        when(dbComponentRepository.findAll()).thenReturn(components());
        when(configurationRepository.findByCollectorName(any())).thenReturn(configuration());
        when(fortifyClient.getApplicationArray(anyString())).thenReturn(applications);
        when(fortifyClient.getFortifyReport(any(FortifyProject.class), any(JSONObject.class))).thenReturn(fortifyScanReport);
        when(fortifyProjectRepository.findEnabledProjects(any(ObjectId.class), anyString())).thenReturn(fortifyProject());
        when(codeQualityRepository.findByCollectorItemIdAndTimestamp(any(ObjectId.class), anyLong())).thenReturn(null);
        
        task.collect(fortifyCollector(configuration()));
        
        verify(codeQualityRepository, times(1)).save(any(CodeQuality.class));
        verify(fortifyScanRepository, times(1)).save(any(FortifyScanReport.class));
    }
    
    @Test
    public void noScanReportTest() throws Exception {
    	Map<String,JSONObject> applications = getApplications();
    	
        when(dbComponentRepository.findAll()).thenReturn(components());
        when(configurationRepository.findByCollectorName(any())).thenReturn(configuration());
        when(fortifyClient.getApplicationArray(anyString())).thenReturn(applications);
        when(fortifyClient.getFortifyReport(any(FortifyProject.class), any(JSONObject.class))).thenThrow(new RestClientException("Client error for fetching scan report"));
        when(fortifyProjectRepository.findEnabledProjects(any(ObjectId.class), anyString())).thenReturn(fortifyProject());
        when(codeQualityRepository.findByCollectorItemIdAndTimestamp(any(ObjectId.class), anyLong())).thenReturn(new CodeQuality());
        
        task.collect(fortifyCollector(configuration()));
        
        verify(codeQualityRepository, never()).save(any(CodeQuality.class));
        verify(fortifyScanRepository, never()).save(any(FortifyScanReport.class));
    }
    
    @Test
    public void removeUnwantedJobsTest() throws Exception {
    	Map<String,JSONObject> applications = getApplications();
        when(fortifyClient.getApplicationArray(anyString())).thenReturn(applications);

    	FortifyProject project = fortifyProject().get(0);
    	project.setInstanceUrl("http://removedServer/ssc");
    	List<FortifyProject> projects  = new ArrayList<>();
    	projects.add(project);
    	
        when(dbComponentRepository.findAll()).thenReturn(components());
        when(configurationRepository.findByCollectorName(any())).thenReturn(configuration());
        when(fortifyProjectRepository.findByCollectorIdIn(Matchers.anyListOf(ObjectId.class))).thenReturn(projects);
        when(fortifyProjectRepository.findEnabledProjects(any(ObjectId.class), anyString())).thenReturn(new ArrayList<>());
        
        task.collect(fortifyCollector(configuration()));
        verify(fortifyProjectRepository, times(1)).delete(Matchers.anyListOf(FortifyProject.class));
    }

    @Test
    public void addNewProjectsTest() throws Exception {
    	FortifyCollector fortifyCollector = new FortifyCollector();
    	fortifyCollector.setId(new ObjectId());
    	List<FortifyProject> project = fortifyProject();
    	
        when(fortifyClient.getApplications(anyString(), Matchers.anyCollectionOf(JSONObject.class))).thenReturn(project);
        when(dbComponentRepository.findAll()).thenReturn(components());
        when(configurationRepository.findByCollectorName(any())).thenReturn(configuration());
        
        task.collect(fortifyCollector(configuration()));
        verify(fortifyProjectRepository, atLeastOnce()).save(Matchers.anyListOf(FortifyProject.class));
    }
    
    @Test
    public void addNewProjects_noDuplicate() throws Exception {
    	FortifyCollector fortifyCollector = new FortifyCollector();
    	fortifyCollector.setId(new ObjectId());
    	List<FortifyProject> project = fortifyProject();
    	
        when(fortifyClient.getApplications(anyString(), Matchers.anyCollectionOf(JSONObject.class))).thenReturn(project);
        when(fortifyProjectRepository.findByCollectorIdIn(Matchers.anySetOf(ObjectId.class))).thenReturn(fortifyProject());
        when(dbComponentRepository.findAll()).thenReturn(components());
        when(configurationRepository.findByCollectorName(any())).thenReturn(configuration());
        
        task.collect(fortifyCollector(configuration()));
        verify(fortifyProjectRepository, never()).save(Matchers.anyListOf(FortifyProject.class));
    }
    
	private List<FortifyProject> fortifyProject() {
		List<FortifyProject> enabledProjectList = new ArrayList<>();
		FortifyProject project = new FortifyProject();
		project.setCollectorId(new ObjectId());
		project.setProjectName("TestProject");
		project.setProjectId("12345678");
		project.setVersionId("47243171");
		project.setInstanceUrl("http://mockServer/ssc");
		enabledProjectList.add(project);
		return enabledProjectList;
	}

	private String getJson(String fileName) throws Exception {
    	Class<?> cls = Class.forName("com.capitalone.dashboard.collector.FortifyCollectorTaskTest");
    	ClassLoader cLoader = cls.getClassLoader();
    	InputStream inputStream = cLoader.getResourceAsStream(fileName);
        return IOUtils.toString(inputStream);
    }

	private Configuration configuration() {
		Configuration config = new Configuration();
		config.setCollectorName(FortifyCollector.NICE_NAME);
		Map<String,String> fortifyServers = new HashMap<>();
		
		fortifyServers.put("url", "http://mockServer/ssc");
		fortifyServers.put("userName", "test");
		fortifyServers.put("password", "testpwd");
		config.getInfo().add(fortifyServers);
		
		return config;
	}

	private ArrayList<com.capitalone.dashboard.model.Component> components() {
    	ArrayList<com.capitalone.dashboard.model.Component> cArray = new ArrayList<com.capitalone.dashboard.model.Component>();
    	com.capitalone.dashboard.model.Component c = new Component();
    	c.setId(new ObjectId());
    	c.setName("COMPONENT1");
    	c.setOwner("JOHN");
    	cArray.add(c);
    	return cArray;
    }
    
	private FortifyCollector fortifyCollector (Configuration config) {
		List<String> serverUrls = config.getInfo().stream()
                .map(server -> server.get("url")).collect(toList());

		FortifyCollector coll = FortifyCollector.prototype(serverUrls);
        coll.setId(new ObjectId(new java.util.Date()));

        return coll;
	}
	
	private Map<String,JSONObject> getApplications() throws Exception {
		Map<String, JSONObject> applications = new HashMap<>();
		applications.put("47243171", (JSONObject) new JSONParser().parse(getJson("applications.json")));
		return applications;
	}
}
