package com.equitycalc.simulation;

import com.equitycalc.model.*;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class MonteCarloSim {
    private static final int MAX_PLAYERS = 6;
    private static final int SIMULATION_BATCH_SIZE = 1000;
    
    private final Deck deck;
    private final int numSimulations;
    private PokerHandLookup lookupTable;
    private static final String DEFAULT_LOOKUP_PATH = "poker_lookup.dat";
    
    public MonteCarloSim() {
        this.numSimulations = 10000;
        this.deck = new Deck();
        this.lookupTable = new PokerHandLookup(numSimulations);
    }

    public Set<String> getSimulatedHandKeys() {
        return lookupTable.getAllKeys();
    }

     private String generateLookupKey(List<Player> players) {
        // Sort hole cards for consistent keys
        return players.stream()
            .map(p -> {
                List<Card> cards = p.getHoleCards();
                return cards.get(0).compareTo(cards.get(1)) <= 0 ? 
                    cards.get(0).toString() + cards.get(1).toString() :
                    cards.get(1).toString() + cards.get(0).toString();
            })
            .sorted()
            .collect(Collectors.joining("|"));
    }

    public void saveLookupTable() throws IOException {
        saveLookupTable(DEFAULT_LOOKUP_PATH);
    }

    public void saveLookupTable(String filename) throws IOException {
        File file = new File(filename);
        File dir = file.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }

        try (ObjectOutputStream out = new ObjectOutputStream(
                new BufferedOutputStream(new FileOutputStream(file)))) {
            out.writeObject(lookupTable);
        }
    }

    public void loadLookupTable() throws IOException, ClassNotFoundException {
        loadLookupTable(DEFAULT_LOOKUP_PATH);
    }

    public void loadLookupTable(String filename) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = new ObjectInputStream(
                new BufferedInputStream(new FileInputStream(filename)))) {
            lookupTable = (PokerHandLookup) in.readObject();
        }
    }

    public SimulationResult getStoredResult(List<Player> players) {
        String key = generateLookupKey(players);
        return lookupTable.getResult(key);
    }
    
    public void runSimulation(List<Player> players) {
        long startTime = System.nanoTime();
        
        if (players.size() > MAX_PLAYERS) {
            throw new IllegalArgumentException("Maximum " + MAX_PLAYERS + " players allowed");
        }
        
        SimulationResult result = new SimulationResult(players.size());
        
        for (int i = 0; i < numSimulations; i++) {
            if (i % SIMULATION_BATCH_SIZE == 0) {
                deck.cards = new ArrayList<>(new Deck().cards);
            }
            
            long handStartTime = System.nanoTime();
            simulateOneHand(players, result);
            PerformanceLogger.logOperation("SimulateHand", handStartTime);
        }
        
        // Update player probabilities
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            player.setWinProbability(result.getWinProbability(i));
            player.setLossProbability(result.getLossProbability(i));
            player.setSplitProbability(result.getSplitProbability(i));
        }
        
        // Store in lookup table
        String key = generateLookupKey(players);
        lookupTable.addResult(key, result);
        
        PerformanceLogger.logOperation("FullSimulation", startTime);
    }
    
    private void simulateOneHand(List<Player> players, SimulationResult result) {
        // Create new deck for each hand
        deck.cards = new ArrayList<>(new Deck().cards);
        
        // Remove hole cards from deck
        Set<Card> usedCards = new HashSet<>();
        for (Player player : players) {
            usedCards.addAll(player.getHoleCards());
        }
        deck.cards.removeAll(usedCards);
        
        deck.shuffle();
        
        // Deal community cards from remaining deck
        List<Card> communityCards = deck.dealCards(5);
        
        // Create and evaluate hands
        List<PokerHand> hands = new ArrayList<>();
        for (Player player : players) {
            PokerHand hand = new PokerHand();
            // Add hole cards
            for (Card card : player.getHoleCards()) {
                hand.addCard(card);
            }
            // Add community cards
            for (Card card : communityCards) {
                hand.addCard(card);
            }
            hands.add(hand);
        }
        
        evaluateHandsAndUpdateResults(hands, result);
    }
    
    private void evaluateHandsAndUpdateResults(List<PokerHand> hands, SimulationResult result) {
        List<HandRanking> rankings = new ArrayList<>();
        
        // Evaluate each hand
        for (PokerHand hand : hands) {
            rankings.add(HandEvaluator.evaluateHand(hand));
        }
        
        // Find best hand(s)
        HandRanking bestRanking = Collections.max(rankings);
        List<Integer> winners = new ArrayList<>();
        
        // Find all players with the best hand (for split pots)
        for (int i = 0; i < rankings.size(); i++) {
            int comparison = rankings.get(i).compareTo(bestRanking);
            if (comparison == 0) {
                winners.add(i);
            }
        }
        result.incrementTotalHands();
        // Update statistics
        if (winners.size() == 1) {
            // Single winner
            int winner = winners.get(0);
            result.incrementWin(winner);
            for (int i = 0; i < rankings.size(); i++) {
                if (i != winner) {
                    result.incrementLoss(i);
                }
            }
        } else {
            // Split pot
            for (int winner : winners) {
                result.incrementSplit(winner);
            }
            for (int i = 0; i < rankings.size(); i++) {
                if (!winners.contains(i)) {
                    result.incrementLoss(i);
                }
            }
        }
    }
    
}

enum HandType {
    HIGH_CARD,
    ONE_PAIR,
    TWO_PAIR, 
    THREE_OF_A_KIND,
    STRAIGHT,
    FLUSH,
    FULL_HOUSE,
    FOUR_OF_A_KIND,
    STRAIGHT_FLUSH
}