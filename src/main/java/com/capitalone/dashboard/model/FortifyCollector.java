package com.capitalone.dashboard.model;

import java.util.ArrayList;
import java.util.List;

public class FortifyCollector extends Collector {
	
	public static final String NICE_NAME = "fortify";
    
	private List<String> fortifyServers = new ArrayList<>();

    public List<String> getFortifyServers() {
        return fortifyServers;
    }

    public static FortifyCollector prototype(List<String> servers) {
    	FortifyCollector protoType = new FortifyCollector();
        protoType.setName(NICE_NAME);
        protoType.setCollectorType(CollectorType.StaticSecurityScan);
        protoType.setOnline(true);
        protoType.setEnabled(true);
        protoType.getFortifyServers().addAll(servers);
        return protoType;
    }
}
