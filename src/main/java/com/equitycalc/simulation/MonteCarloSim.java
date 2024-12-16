package com.equitycalc.simulation;

import com.equitycalc.model.*;
import com.equitycalc.util.ProgressTracker;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class MonteCarloSim {
    private static final int MAX_PLAYERS = 6;
    private static final int SIMULATION_BATCH_SIZE = 1000;
    private Consumer<Integer> progressCallback;
    
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

    public void setProgressCallback(Consumer<Integer> callback) {
        this.progressCallback = callback;
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
    
    public void runSimulation(SimulationConfig config) throws InterruptedException, ExecutionException {
        long startTime = System.nanoTime();
        
        // Calculate total players correctly
        int totalPlayers = config.getKnownPlayers().size() + 
                        config.getPlayerRanges().size() + 
                        config.getNumRandomPlayers();
                        
        if (totalPlayers > MAX_PLAYERS) {
            throw new IllegalArgumentException("Maximum " + MAX_PLAYERS + " players allowed");
        }
        
        // Initialize result with correct number of players
        SimulationResult result = new SimulationResult(totalPlayers);
        
        ProgressTracker progress = new ProgressTracker(config.getNumSimulations(), config);
        
        for (int i = 0; i < config.getNumSimulations(); i++) {
            if (i % SIMULATION_BATCH_SIZE == 0) {
                long batchStartTime = System.nanoTime(); // Added this line
                deck.cards = new ArrayList<>(new Deck().cards);
                PerformanceLogger.logOperation("DeckReset", batchStartTime);
                
                // Update progress with all players' stats
                List<Double> winRates = new ArrayList<>();
                List<Double> splitRates = new ArrayList<>();
                
                for (int p = 0; p < totalPlayers; p++) {
                    winRates.add(result.getWinProbability(p));
                    splitRates.add(result.getSplitProbability(p));
                }
                
                progress.update(i, winRates, splitRates);
    
                // Renamed progress to progressPercent
                if (progressCallback != null) {
                    int progressPercent = (int)((i * 100.0) / config.getNumSimulations());
                    progressCallback.accept(progressPercent);
                }
            }
            
            long handStartTime = System.nanoTime();
            simulateOneHand(config, result);
            PerformanceLogger.logOperation("SimulateHand", handStartTime);
        }
        
        progress.complete();
        
        // Update final results for known players
        long resultUpdateTime = System.nanoTime();
        List<Player> knownPlayers = config.getKnownPlayers();
        for (int i = 0; i < knownPlayers.size(); i++) {
            Player player = knownPlayers.get(i);
            player.setWinProbability(result.getWinProbability(i));
            player.setLossProbability(result.getLossProbability(i));
            player.setSplitProbability(result.getSplitProbability(i));
        }
        PerformanceLogger.logOperation("ResultUpdate", resultUpdateTime);
        
        // Store in lookup table
        long lookupTime = System.nanoTime();
        String key = generateLookupKey(config.getKnownPlayers());
        lookupTable.addResult(key, result);
        PerformanceLogger.logOperation("LookupTableAdd", lookupTime);
        
        PerformanceLogger.logOperation("FullSimulation", startTime);
    }
    
    private void simulateOneHand(SimulationConfig config, SimulationResult result) throws InterruptedException, ExecutionException {
        long deckPrepTime = System.nanoTime();
        deck.cards = new ArrayList<>(new Deck().cards);
        
        // Remove all known cards from deck
        Set<Card> usedCards = new HashSet<>();
        usedCards.addAll(config.getBoardCards());    
        usedCards.addAll(config.getDeadCards());     
        
        // Remove known player hole cards
        for (Player player : config.getKnownPlayers()) {
            usedCards.addAll(player.getHoleCards());
        }
        
        // Create available cards set for range hands
        Set<Card> availableCards = new HashSet<>(deck.cards);
        availableCards.removeAll(usedCards);
        
        deck.cards.removeAll(usedCards);
        PerformanceLogger.logOperation("DeckPreparation", deckPrepTime);
        
        long shuffleTime = System.nanoTime();
        deck.shuffle();
        PerformanceLogger.logOperation("DeckShuffle", shuffleTime);
        
        // Deal hands for range-based players
        List<Player> allPlayers = new ArrayList<>(config.getKnownPlayers());
        
        // Get all range-based hands first
        List<List<Card[]>> allRangeHands = new ArrayList<>();
        for (Range range : config.getPlayerRanges()) {
            // Filter hands against currently available cards
            List<Card[]> possibleHands = range.getPossibleHands().stream()
                .filter(hand -> availableCards.contains(hand[0]) && availableCards.contains(hand[1]))
                .collect(Collectors.toList());
                
            allRangeHands.add(possibleHands);
        }
        
        // Deal hands for range players one at a time
        for (int i = 0; i < config.getPlayerRanges().size(); i++) {
            List<Card[]> possibleHands = allRangeHands.get(i);
            
            // Remove hands containing used cards
            possibleHands.removeIf(hand -> 
                !availableCards.contains(hand[0]) || !availableCards.contains(hand[1]));
            
            if (possibleHands.isEmpty()) {
                throw new IllegalStateException(
                    String.format("No valid hands available in range for player %d (Available cards: %d)", 
                        i + 1, availableCards.size())
                );
            }
            
            // Pick random hand from remaining possibilities
            Card[] randomHand = possibleHands.get(new Random().nextInt(possibleHands.size()));
            Player rangePlayer = new Player(Arrays.asList(randomHand));
            allPlayers.add(rangePlayer);
            
            // Update available cards and deck
            availableCards.remove(randomHand[0]);
            availableCards.remove(randomHand[1]);
            deck.cards.remove(randomHand[0]);
            deck.cards.remove(randomHand[1]);
        }
        
        // Deal random hands for remaining players
        for (int i = 0; i < config.getNumRandomPlayers(); i++) {
            List<Card> randomHoleCards = deck.dealCards(2);
            allPlayers.add(new Player(randomHoleCards));
        }
        
        // Complete the board if needed
        long dealTime = System.nanoTime();
        List<Card> finalBoard = new ArrayList<>(config.getBoardCards());
        int remainingCards = 5 - finalBoard.size();
        if (remainingCards > 0) {
            finalBoard.addAll(deck.dealCards(remainingCards));
        }
        PerformanceLogger.logOperation("DealCommunityCards", dealTime);
        
        // Create hands for all players
        long handCreateTime = System.nanoTime();
        List<PokerHand> hands = new ArrayList<>();
        for (Player player : allPlayers) {
            PokerHand hand = new PokerHand();
            hand.addCards(player.getHoleCards());
            hand.addCards(finalBoard);
            
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
