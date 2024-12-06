package com.equitycalc.simulation;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class PerformanceLogger {
    private static final Map<String, Long> currentSimTotalTime = new HashMap<>();
    private static final Map<String, Long> currentSimCounts = new HashMap<>();
    private static final List<Map<String, Double>> historicalStats = new ArrayList<>();
    private static int simulationCount = 0;
    
    private static final String LOG_DIR = "logs";
    private static final DateTimeFormatter DATE_FORMAT = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final String LOG_FILE = LOG_DIR + "/simulation_" + 
        LocalDateTime.now().format(DATE_FORMAT) + ".log";
    
    static {
        try {
            Files.createDirectories(Paths.get(LOG_DIR));
        } catch (IOException e) {
            System.err.println("Failed to create log directory: " + e.getMessage());
        }
    }
    
    public static void startNewSimulation() {
        if (!currentSimTotalTime.isEmpty()) {
            Map<String, Double> simStats = new HashMap<>();
            currentSimTotalTime.forEach((op, total) -> {
                long count = currentSimCounts.get(op);
                simStats.put(op + "_total_ns", total / 1.0); // Store in nanoseconds
                simStats.put(op + "_avg_ns", (total / (double) count) / 1.0); // Average in nanoseconds
                simStats.put(op + "_count", (double) count);
            });
            historicalStats.add(simStats);
            
            // Log simulation completion
            writeToLog(String.format("\nSimulation %d completed at %s\n", 
                simulationCount, LocalDateTime.now().format(DATE_FORMAT)));
        }
        
        currentSimTotalTime.clear();
        currentSimCounts.clear();
        simulationCount++;
    }
    
    private static void writeToLog(String message) {
        try {
            Files.write(Paths.get(LOG_FILE), 
                       (message + "\n").getBytes(), 
                       StandardOpenOption.CREATE, 
                       StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }
    
    public static void logOperation(String operation, long startTime) {
        long duration = System.nanoTime() - startTime;
        currentSimTotalTime.merge(operation, duration, Long::sum);
        currentSimCounts.merge(operation, 1L, Long::sum);
    }
    
    public static void printStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("\nCurrent Simulation Statistics:\n");
        stats.append("----------------------------\n");
        stats.append(getCurrentStatsString());
        
        if (!historicalStats.isEmpty()) {
            stats.append("\nHistorical Statistics Summary:\n");
            stats.append("---------------------------\n");
            stats.append(getHistoricalSummaryString());
        }
        
        // Print to console and write to file
        // System.out.print(stats.toString());
        writeToLog(stats.toString());
    }
    
    private static String getCurrentStatsString() {
        StringBuilder sb = new StringBuilder();
        currentSimTotalTime.forEach((operation, total) -> {
            long count = currentSimCounts.get(operation);
            double avgMs = (total / (double) count) / 1_000_000.0;  // Convert to ms
            double totalMs = total / 1_000_000.0;  // Convert to ms
            sb.append(String.format("%s:\n  Total: %.6f ms\n  Count: %d\n  Avg: %.6f ms\n\n",
                                  operation, totalMs, count, avgMs));
        });
        return sb.toString();
    }
    
    private static String getHistoricalSummaryString() {
        StringBuilder sb = new StringBuilder();
        Map<String, List<Double>> metrics = new HashMap<>();
        
        for (Map<String, Double> stats : historicalStats) {
            stats.forEach((metric, value) -> 
                metrics.computeIfAbsent(metric, k -> new ArrayList<>()).add(value));
        }
        
        metrics.forEach((metric, values) -> {
            DoubleSummaryStatistics stats = values.stream()
                .mapToDouble(Double::doubleValue)
                .summaryStatistics();
            sb.append(String.format("%s:\n  Min: %.3f\n  Max: %.3f\n  Avg: %.3f\n\n",
                                  metric, stats.getMin(), stats.getMax(), stats.getAverage()));
        });
        return sb.toString();
    }
}