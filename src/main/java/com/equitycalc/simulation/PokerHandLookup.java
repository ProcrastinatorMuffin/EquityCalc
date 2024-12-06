package com.equitycalc.simulation;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PokerHandLookup implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Map<String, SimulationResult> results;
    private final int simulationCount;

    public PokerHandLookup(int simulationCount) {
        this.results = new HashMap<>();
        this.simulationCount = simulationCount;
    }

    public void addResult(String key, SimulationResult result) {
        long startTime = System.nanoTime();
        results.put(key, result);
        PerformanceLogger.logOperation("LookupTableAdd", startTime);
    }

    public Set<String> getAllKeys() {
        return Collections.unmodifiableSet(results.keySet());
    }

    public SimulationResult getResult(String key) {
        long startTime = System.nanoTime();
        SimulationResult result = results.get(key);
        PerformanceLogger.logOperation("LookupTableGet", startTime);
        return result;
    }

    public int getSimulationCount() {
        return simulationCount;
    }
}