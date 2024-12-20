package com.equitycalc;

import com.equitycalc.model.Card;
import com.equitycalc.model.Player;
import com.equitycalc.simulation.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;


public class EquityCalc {
    private static final boolean DEBUG_MODE = true;
    private static final int NUM_OPPONENTS = 1;
    private static final int MAX_HANDS_PER_RUN = 100;
    private static Set<String> simulatedHands = new HashSet<>();
    private static int handsSimulated = 0;
    private static final List<String> ALL_POSSIBLE_HANDS = generateAllPossibleHands();
    
    // Test hand categories
    private static final List<String> PREMIUM_HANDS = Arrays.asList(
        "AhAs", "KhKs", "QhQs", "AhKh", // Premium pairs and suited connectors
        "AcAd", "KcKd", "QcQd", "AcKc"
    );
    
    private static final List<String> PLAYABLE_HANDS = Arrays.asList(
        "JhJs", "ThTs", "AhQh", "KhQh", // Medium pairs and suited connectors
        "AhTs", "KhJs", "QhJs", "JhTs",
        "TsJs", "9s8s", "8s7s", "7s6s"
    );
    
    private static final List<String> UNPLAYABLE_HANDS = Arrays.asList(
        "2h7d", "3h8d", "4h9d", "5hTd", // Disconnected, unsuited hands
        "2c7s", "3c8s", "4c9s", "5cTs",
        "2d7h", "3d8h", "4d9h", "5dTh"
    );

    public static void main(String[] args) {
        try {
            MonteCarloSim simulator = new MonteCarloSim();
            PerformanceLogger.startNewSimulation();
            
            // Initialize as empty HashSet
            simulatedHands = new HashSet<>();
            
            // Load existing lookup table
            try {
                simulator.loadLookupTable();
                // Create mutable copy of the Set
                simulatedHands = new HashSet<>(simulator.getSimulatedHandKeys());
                System.out.printf("Loaded existing lookup table with %d hands%n", 
                    simulatedHands.size());
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Creating new lookup table");
            }
            
            if (DEBUG_MODE) {
                runDebugSimulations(simulator);
            } else {
                runProductionSimulations(simulator);
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void runDebugSimulations(MonteCarloSim simulator) throws InterruptedException, ExecutionException {
        System.out.println("\n=== Testing Premium Hands ===");
        simulateHandCategory(simulator, PREMIUM_HANDS);
        
        System.out.println("\n=== Testing Playable Hands ===");
        simulateHandCategory(simulator, PLAYABLE_HANDS);
        
        System.out.println("\n=== Testing Unplayable Hands ===");
        simulateHandCategory(simulator, UNPLAYABLE_HANDS);
    }
    
    private static void simulateHandCategory(MonteCarloSim simulator, List<String> hands) throws InterruptedException, ExecutionException {
        for (String handKey : hands) {
            if (!simulatedHands.contains(handKey)) {
                List<Card> heroCards = Arrays.asList(
                    new Card(handKey.substring(0, 2)),
                    new Card(handKey.substring(2, 4))
                );
                
                System.out.printf("\nSimulating hand: %s %s%n", 
                    heroCards.get(0), heroCards.get(1));
                
                runSimulation(simulator, heroCards);
                simulatedHands.add(handKey);
                try {
                    simulator.saveLookupTable();
                } catch (IOException e) {
                    System.err.println("Error saving lookup table: " + e.getMessage());
                    e.printStackTrace();
                }

            }
        }
    }
    
    private static void runProductionSimulations(MonteCarloSim simulator) throws InterruptedException, ExecutionException {
        while (handsSimulated < MAX_HANDS_PER_RUN) {
            List<String> remainingHands = ALL_POSSIBLE_HANDS.stream()
                .filter(hand -> !simulatedHands.contains(hand))
                .collect(Collectors.toList());
            
            if (remainingHands.isEmpty()) {
                System.out.println("All possible hands have been simulated!");
                break;
            }
            
            String handKey = remainingHands.get(new Random().nextInt(remainingHands.size()));
            List<Card> heroCards = Arrays.asList(
                new Card(handKey.substring(0, 2)),
                new Card(handKey.substring(2, 4))
            );
            
            System.out.printf("Simulating hand: %s %s%n", 
                heroCards.get(0), heroCards.get(1));
            
            runSimulation(simulator, heroCards);
            handsSimulated++;
            simulatedHands.add(handKey);
            try {
                simulator.saveLookupTable();
            } catch (IOException e) {
                System.err.println("Error saving lookup table: " + e.getMessage());
                e.printStackTrace();
            }
            
            System.out.printf("Progress: %d/%d hands simulated (%d hands remaining)%n", 
                handsSimulated, MAX_HANDS_PER_RUN, remainingHands.size() - 1);
        }
    }

    private static List<String> generateAllPossibleHands() {
        List<String> hands = new ArrayList<>();
        Card.Rank[] ranks = Card.Rank.values();
        Card.Suit[] suits = Card.Suit.values();
        
        for (int i = 0; i < ranks.length; i++) {
            for (int j = 0; j < suits.length; j++) {
                Card card1 = new Card(ranks[i], suits[j]);
                for (int k = i; k < ranks.length; k++) {
                    for (int l = (k == i ? j + 1 : 0); l < suits.length; l++) {
                        Card card2 = new Card(ranks[k], suits[l]);
                        String handKey = generateHandKey(Arrays.asList(card1, card2));
                        hands.add(handKey);
                    }
                }
            }
        }
        return hands;
    }
    
    private static void runSimulation(MonteCarloSim simulator, List<Card> heroCards) throws InterruptedException, ExecutionException {
        long startTime = System.nanoTime();
        
        Player hero = new Player(heroCards);
        List<Player> players = new ArrayList<>();
        players.add(hero);
        
        // Generate opponent hands
        Set<Card> usedCards = new HashSet<>(heroCards);
        for (int i = 0; i < NUM_OPPONENTS; i++) {
            long opponentStartTime = System.nanoTime();
            List<Card> opponentCards = generateRandomHoleCards(usedCards);
            usedCards.addAll(opponentCards);
            players.add(new Player(opponentCards));
            PerformanceLogger.logOperation("GenerateOpponentHand", opponentStartTime);
        }
        
        simulator.runSimulation(players);
        printResults(players);
        
    }
    
    private static String generateHandKey(List<Card> cards) {
        // Normalize hand representation (e.g., AhKs and KsAh are the same hand)
        return cards.get(0).compareTo(cards.get(1)) <= 0 ? 
            cards.get(0).toString() + cards.get(1).toString() :
            cards.get(1).toString() + cards.get(0).toString();
    }
    
    private static List<Card> generateRandomHoleCards(Set<Card> usedCards) {
        List<Card> availableCards = new ArrayList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                Card card = new Card(rank, suit);
                if (!usedCards.contains(card)) {
                    availableCards.add(card);
                }
            }
        }
        
        if (availableCards.size() < 2) {
            throw new IllegalStateException("Not enough cards available");
        }
        
        Collections.shuffle(availableCards);
        return Arrays.asList(availableCards.get(0), availableCards.get(1));
    }
    
    private static void printResults(List<Player> players) {
        Player hero = players.get(0);
        System.out.printf("Hero (%s):%n", 
            hero.getHoleCards().stream()
                .map(Card::toString)
                .collect(Collectors.joining(" ")));
        System.out.printf("Win: %.2f%% ", hero.getWinProbability() * 100);
        System.out.printf("Split: %.2f%% ", hero.getSplitProbability() * 100);
        System.out.printf("Lose: %.2f%%%n", hero.getLossProbability() * 100);
        
        PerformanceLogger.printStats();
    }
}