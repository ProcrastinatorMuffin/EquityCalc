package com.equitycalc.simulation;

import com.equitycalc.model.*;
import com.equitycalc.util.ProgressTracker;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class MonteCarloSim {
    private static final int MAX_PLAYERS = 6;
    private static final int SIMULATION_BATCH_SIZE = 1000;
    
    private final Deck deck;
    private final int numSimulations;
    private PokerHandLookup lookupTable;
    private static final String DEFAULT_LOOKUP_PATH = "resources/poker_lookup.dat";
    
    public MonteCarloSim() {
        this.numSimulations = 1000000;
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
    
    public void runSimulation(List<Player> players) throws InterruptedException, ExecutionException {
        long startTime = System.nanoTime();
        
        if (players.size() > MAX_PLAYERS) {
            throw new IllegalArgumentException("Maximum " + MAX_PLAYERS + " players allowed");
        }
        
        SimulationResult result = new SimulationResult(players.size());
        ProgressTracker progress = new ProgressTracker(numSimulations);

        String heroHandKey = generateLookupKey(players.subList(0, 1));
        progress.setCurrentHand(heroHandKey);
        
        for (int i = 0; i < numSimulations; i++) {
            long batchStartTime = System.nanoTime();
            if (i % SIMULATION_BATCH_SIZE == 0) {
                deck.cards = new ArrayList<>(new Deck().cards);
                PerformanceLogger.logOperation("DeckReset", batchStartTime);
                
                // Update progress every batch
                progress.update(i, 
                    result.getWinProbability(0), 
                    result.getSplitProbability(0));
            }
            
            long handStartTime = System.nanoTime();
            simulateOneHand(players, result);
            PerformanceLogger.logOperation("SimulateHand", handStartTime);
        }
        
        progress.complete();
        
        // Update final results
        long resultUpdateTime = System.nanoTime();
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            player.setWinProbability(result.getWinProbability(i));
            player.setLossProbability(result.getLossProbability(i));
            player.setSplitProbability(result.getSplitProbability(i));
        }
        PerformanceLogger.logOperation("ResultUpdate", resultUpdateTime);
        
        // Store in lookup table
        long lookupTime = System.nanoTime();
        String key = generateLookupKey(players);
        lookupTable.addResult(key, result);
        PerformanceLogger.logOperation("LookupTableAdd", lookupTime);
        
        PerformanceLogger.logOperation("FullSimulation", startTime);
    }
    
    // Modify simulateOneHand method:
    private void simulateOneHand(List<Player> players, SimulationResult result) throws InterruptedException, ExecutionException {
        long deckPrepTime = System.nanoTime();
        deck.cards = new ArrayList<>(new Deck().cards);
        
        // Remove hole cards from deck
        Set<Card> usedCards = new HashSet<>();
        for (Player player : players) {
            usedCards.addAll(player.getHoleCards());
        }
        deck.cards.removeAll(usedCards);
        PerformanceLogger.logOperation("DeckPreparation", deckPrepTime);
        
        long shuffleTime = System.nanoTime();
        deck.shuffle();
        PerformanceLogger.logOperation("DeckShuffle", shuffleTime);
        
        long dealTime = System.nanoTime();
        List<Card> communityCards = deck.dealCards(5);
        PerformanceLogger.logOperation("DealCommunityCards", dealTime);
        
        long handCreateTime = System.nanoTime();
        List<PokerHand> hands = new ArrayList<>();
        for (Player player : players) {
            PokerHand hand = new PokerHand();
            for (Card card : player.getHoleCards()) {
                hand.addCard(card);
            }
            for (Card card : communityCards) {
                hand.addCard(card);
            }
            
            if (hand.getCardCount() != 7) {
                throw new IllegalStateException(
                    String.format("Invalid hand size: %d cards", hand.getCardCount())
                );
            }
            hands.add(hand);
        }
        PerformanceLogger.logOperation("HandCreation", handCreateTime);
        
        long evalTime = System.nanoTime();
        evaluateHandsAndUpdateResults(hands, result);
        PerformanceLogger.logOperation("HandEvaluation", evalTime);
    }
    
    // Modify evaluateHandsAndUpdateResults method:
    private void evaluateHandsAndUpdateResults(List<PokerHand> hands, SimulationResult result) throws InterruptedException, ExecutionException {
        long rankingTime = System.nanoTime();
        List<HandRanking> rankings = new ArrayList<>();
        
        for (PokerHand hand : hands) {
            rankings.add(BitHandEvaluator.evaluateHand(hand));
        }
        PerformanceLogger.logOperation("HandRanking", rankingTime);
        
        long compareTime = System.nanoTime();
        HandRanking bestRanking = Collections.max(rankings);
        List<Integer> winners = new ArrayList<>();
        
        for (int i = 0; i < rankings.size(); i++) {
            if (rankings.get(i).compareTo(bestRanking) == 0) {
                winners.add(i);
            }
        }
        PerformanceLogger.logOperation("WinnerDetermination", compareTime);
        
        long statsTime = System.nanoTime();
        result.incrementTotalHands();
        
        if (winners.size() == 1) {
            int winner = winners.get(0);
            result.incrementWin(winner);
            for (int i = 0; i < rankings.size(); i++) {
                if (i != winner) {
                    result.incrementLoss(i);
                }
            }
        } else {
            for (int winner : winners) {
                result.incrementSplit(winner);
            }
            for (int i = 0; i < rankings.size(); i++) {
                if (!winners.contains(i)) {
                    result.incrementLoss(i);
                }
            }
        }
        PerformanceLogger.logOperation("StatsUpdate", statsTime);
    }
    
}
