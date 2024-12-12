package com.equitycalc;

import com.equitycalc.model.Card;
import com.equitycalc.model.Player;
import com.equitycalc.simulation.*;
import java.util.*;

public class EquityCalc {
    private static final int DEFAULT_ITERATIONS = 50000;
    private final MonteCarloSim simulator;
    private final List<SimulationMetrics> allResults;

    public EquityCalc() {
        this.simulator = new MonteCarloSim();
        this.allResults = new ArrayList<>();
    }

    public static void main(String[] args) {
        EquityCalc calc = new EquityCalc();
        calc.runBenchmarks();
    }

    private void runBenchmarks() {
        System.out.println("Starting Equity Calculator Benchmarks\n");
        
        runPreflopScenarios();
        runFlopScenarios();
        runTurnScenarios();
        runRiverScenarios();
        
        generateReport();
    }

    private void runPreflopScenarios() {
        System.out.println("=== Preflop Scenarios ===");
        
        // Premium pairs vs premium holdings
        runScenario("AA vs KK", 
            new Card("As"), new Card("Ac"), 
            new Card("Kh"), new Card("Kd"));
            
        runScenario("KK vs QQ vs AK", 
            Arrays.asList(
                new Player(Arrays.asList(new Card("Ks"), new Card("Kc"))),
                new Player(Arrays.asList(new Card("Qs"), new Card("Qc"))),
                new Player(Arrays.asList(new Card("Ah"), new Card("Kh")))
            ));
    
        // Coinflip situations
        runScenario("JJ vs AK suited",
            new Card("Js"), new Card("Jc"),
            new Card("Ah"), new Card("Kh"));
            
        runScenario("TT vs AQ vs AJ",
            Arrays.asList(
                new Player(Arrays.asList(new Card("Ts"), new Card("Tc"))),
                new Player(Arrays.asList(new Card("As"), new Card("Qs"))),
                new Player(Arrays.asList(new Card("Ah"), new Card("Jh")))
            ));
    
        // Multi-way scenarios
        runScenario("AA vs KK vs QQ vs JJ",
            Arrays.asList(
                new Player(Arrays.asList(new Card("As"), new Card("Ac"))),
                new Player(Arrays.asList(new Card("Ks"), new Card("Kc"))),
                new Player(Arrays.asList(new Card("Qs"), new Card("Qc"))),
                new Player(Arrays.asList(new Card("Js"), new Card("Jc")))
            ));
    
        // Suited connectors vs high cards
        runScenario("89s vs AK vs KQ",
            Arrays.asList(
                new Player(Arrays.asList(new Card("8h"), new Card("9h"))),
                new Player(Arrays.asList(new Card("As"), new Card("Ks"))),
                new Player(Arrays.asList(new Card("Kc"), new Card("Qc")))
            ));
    
        // Small pairs vs overcards
        runScenario("55 vs AJ vs KQ",
            Arrays.asList(
                new Player(Arrays.asList(new Card("5s"), new Card("5c"))),
                new Player(Arrays.asList(new Card("As"), new Card("Js"))),
                new Player(Arrays.asList(new Card("Kh"), new Card("Qh")))
            ));
    }

    private void runScenario(String description, Card hero1, Card hero2, Card villain1, Card villain2) {
        Player hero = new Player(Arrays.asList(hero1, hero2));
        Player villain = new Player(Arrays.asList(villain1, villain2));
        
        runSimulation(description, 
            SimulationConfig.builder()
                .withKnownPlayers(Arrays.asList(hero, villain))
                .preflop()
                .withNumSimulations(DEFAULT_ITERATIONS)
                .build()
        );
    }
    
    private void runScenario(String description, List<Player> players) {
        runSimulation(description,
            SimulationConfig.builder()
                .withKnownPlayers(players)
                .preflop()
                .withNumSimulations(DEFAULT_ITERATIONS)
                .build()
        );
    }

    private void runFlopScenarios() {
        System.out.println("\n=== Flop Scenarios ===");
        
        // Scenario: Set vs Flush Draw
        Player hero = new Player(Arrays.asList(new Card("Ah"), new Card("As")));
        Player villain = new Player(Arrays.asList(new Card("Kh"), new Card("Qh")));
        
        runSimulation("Set vs Flush Draw", 
            SimulationConfig.builder()
                .withKnownPlayers(Arrays.asList(hero, villain))
                .flop(new Card("Ad"), new Card("7h"), new Card("2h"))
                .withNumSimulations(DEFAULT_ITERATIONS)
                .build()
        );
    }

    private void runTurnScenarios() {
        System.out.println("\n=== Turn Scenarios ===");
        
        // Scenario: Overpair vs Two Pair
        Player hero = new Player(Arrays.asList(new Card("As"), new Card("Ac")));
        Player villain = new Player(Arrays.asList(new Card("Kd"), new Card("Qd")));
        
        SimulationConfig config = SimulationConfig.builder()
            .withKnownPlayers(Arrays.asList(hero, villain))
            .withBoardCards(Arrays.asList(
                new Card("Kh"), new Card("Qs"), new Card("7c"), new Card("2d")))
            .withNumSimulations(DEFAULT_ITERATIONS)
            .build();
            
        runSimulation("Overpair vs Two Pair", config);
    }

    private void runRiverScenarios() {
        System.out.println("\n=== River Scenarios ===");
        
        // Scenario: Full House vs Flush
        Player hero = new Player(Arrays.asList(new Card("Ah"), new Card("Ad")));
        Player villain = new Player(Arrays.asList(new Card("Kh"), new Card("Jh")));
        
        List<Card> board = Arrays.asList(
            new Card("Ac"), new Card("Kd"), new Card("Kc"),
            new Card("2h"), new Card("5h")
        );
        
        SimulationConfig config = SimulationConfig.builder()
            .withKnownPlayers(Arrays.asList(hero, villain))
            .withBoardCards(board)
            .withNumSimulations(DEFAULT_ITERATIONS)
            .build();
            
        runSimulation("Full House vs Flush", config);
    }

    private void runSimulation(String description, SimulationConfig config) {
        System.out.println("\nRunning: " + description);
        long startTime = System.currentTimeMillis();
        
        try {
            simulator.runSimulation(config);
            
            // Print results with confidence intervals
            List<Player> players = config.getKnownPlayers();
            SimulationResult result = simulator.getStoredResult(players);
            
            for (int i = 0; i < players.size(); i++) {
                Player p = players.get(i);
                double[] winCI = result.getWinProbabilityWithConfidence(i);
                double[] splitCI = result.getSplitProbabilityWithConfidence(i);
                double[] lossCI = result.getLossProbabilityWithConfidence(i);
                
                System.out.printf("Player %d:\n", i + 1);
                System.out.printf("  Win: %.2f%% (95%% CI: %.2f%% - %.2f%%)\n",
                    p.getWinProbability() * 100,
                    winCI[0] * 100,
                    winCI[1] * 100);
                System.out.printf("  Split: %.2f%% (95%% CI: %.2f%% - %.2f%%)\n",
                    p.getSplitProbability() * 100,
                    splitCI[0] * 100,
                    splitCI[1] * 100);
                System.out.printf("  Lose: %.2f%% (95%% CI: %.2f%% - %.2f%%)\n",
                    p.getLossProbability() * 100,
                    lossCI[0] * 100,
                    lossCI[1] * 100);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            System.out.printf("Time: %.2f seconds\n", duration / 1000.0);
            System.out.println("----------------------------------------");
            
            // Store metrics with confidence intervals
            allResults.add(new SimulationMetrics(description, duration, players, result));
            
        } catch (Exception e) {
            System.err.println("Error running simulation: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void generateReport() {
        System.out.println("\n=== Simulation Summary Report ===");
        System.out.println("\nTotal scenarios run: " + allResults.size());
        
        // Performance metrics
        long totalDuration = allResults.stream().mapToLong(m -> m.duration).sum();
        double avgDuration = totalDuration / (double)allResults.size();
        System.out.printf("\nPerformance Metrics:");
        System.out.printf("\n- Total time: %.2f seconds", totalDuration / 1000.0);
        System.out.printf("\n- Average time per scenario: %.2f seconds", avgDuration / 1000.0);
        
        // Results summary
        System.out.println("\nScenario Results:");
        for (SimulationMetrics metrics : allResults) {
            System.out.printf("\n%s:", metrics.description);
            metrics.results.forEach((player, result) -> {
                System.out.printf("\n  Player %d:", player + 1);
                System.out.printf("\n    Win: %s", result.formatRate(result.winCI()));
                System.out.printf("\n    Split: %s", result.formatRate(result.splitCI()));
                System.out.printf("\n    Lose: %s", result.formatRate(result.lossCI()));
            });
        }
        
        System.out.println("\n----------------------------------------");
    }

    private class SimulationMetrics {
        private final String description;
        private final long duration;
        private final Map<Integer, PlayerResultWithCI> results;
        
        public SimulationMetrics(String description, long duration, List<Player> players, SimulationResult simResult) {
            this.description = description;
            this.duration = duration;
            this.results = new HashMap<>();
            
            for (int i = 0; i < players.size(); i++) {
                results.put(i, new PlayerResultWithCI(
                    simResult.getWinProbabilityWithConfidence(i),
                    simResult.getSplitProbabilityWithConfidence(i),
                    simResult.getLossProbabilityWithConfidence(i)
                ));
            }
        }
        
        public record PlayerResultWithCI(
            double[] winCI,
            double[] splitCI,
            double[] lossCI
        ) {
            public String formatRate(double[] ci) {
                return String.format("%.2f%% (%.2f%% - %.2f%%)",
                    (ci[0] + ci[1]) / 2 * 100,
                    ci[0] * 100,
                    ci[1] * 100);
            }
        }
    }
}