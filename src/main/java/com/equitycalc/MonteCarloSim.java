package com.equitycalc;

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
        this.numSimulations = 100000;
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
        if (players.size() > MAX_PLAYERS) {
            throw new IllegalArgumentException("Maximum " + MAX_PLAYERS + " players allowed");
        }
        
        SimulationResult result = new SimulationResult(players.size());
        
        for (int i = 0; i < numSimulations; i++) {
            if (i % SIMULATION_BATCH_SIZE == 0) {
                deck.cards = new ArrayList<>(new Deck().cards); // Reset deck periodically
            }
            
            simulateOneHand(players, result);
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

class SimulationResult implements Serializable {
    private final int[] wins;
    private final int[] losses;
    private final int[] splits;
    private int totalHands;
    
    public SimulationResult(int numPlayers) {
        wins = new int[numPlayers];
        losses = new int[numPlayers];
        splits = new int[numPlayers];
    }
    
    public double getWinProbability(int playerIndex) {
        return totalHands > 0 ? (double) wins[playerIndex] / totalHands : 0;
    }
    
    public double getLossProbability(int playerIndex) {
        return totalHands > 0 ? (double) losses[playerIndex] / totalHands : 0;
    }
    
    public double getSplitProbability(int playerIndex) {
        return totalHands > 0 ? (double) splits[playerIndex] / totalHands : 0;
    }
    
    public void incrementWin(int playerIndex) {
        wins[playerIndex]++;
    }
    
    public void incrementLoss(int playerIndex) {
        losses[playerIndex]++;
    }
    
    public void incrementSplit(int playerIndex) {
        splits[playerIndex]++;
    }
    
    public void incrementTotalHands() {
        totalHands++;
    }
}

class HandEvaluator {
    public static HandRanking evaluateHand(PokerHand hand) {
        List<Card> cards = hand.getCards();
        // Find best 5-card combination from 7 cards
        List<List<Card>> combinations = getCombinations(cards, 5);
        
        return combinations.stream()
            .map(HandEvaluator::evaluateFiveCardHand)
            .max(HandRanking::compareTo)
            .orElseThrow();
    }

    private static HandRanking evaluateFiveCardHand(List<Card> cards) {
        Map<Card.Rank, Integer> rankCount = getRankCount(cards);
        
        if (isStraightFlush(cards)) {
            return new HandRanking(HandType.STRAIGHT_FLUSH, getStraightHighCard(cards));
        }
        
        if (rankCount.containsValue(4)) {
            return new HandRanking(HandType.FOUR_OF_A_KIND, getFourOfAKindRank(cards));
        }
        
        if (rankCount.containsValue(3) && rankCount.containsValue(2)) {
            return new HandRanking(HandType.FULL_HOUSE, getFullHouseRank(cards));
        }
        
        if (isFlush(cards)) {
            return new HandRanking(HandType.FLUSH, getHighCard(cards));
        }
        
        if (isStraight(cards)) {
            return new HandRanking(HandType.STRAIGHT, getStraightHighCard(cards));
        }
        
        if (rankCount.containsValue(3)) {
            return new HandRanking(HandType.THREE_OF_A_KIND, getThreeOfAKindRank(cards));
        }
        
        if (getRankCount(cards).values().stream().filter(count -> count == 2).count() == 2) {
            return new HandRanking(HandType.TWO_PAIR, getTwoPairRanks(cards));
        }
        
        if (rankCount.containsValue(2)) {
            return new HandRanking(HandType.ONE_PAIR, getOnePairRank(cards));
        }
        
        return new HandRanking(HandType.HIGH_CARD, getHighCard(cards));
    }

    private static List<List<Card>> getCombinations(List<Card> cards, int r) {
        List<List<Card>> combinations = new ArrayList<>();
        combinationsHelper(cards, r, 0, new ArrayList<>(), combinations);
        return combinations;
    }
    
    private static void combinationsHelper(List<Card> cards, int r, int start, 
                                         List<Card> current, List<List<Card>> combinations) {
        if (current.size() == r) {
            combinations.add(new ArrayList<>(current));
            return;
        }
        
        for (int i = start; i < cards.size(); i++) {
            current.add(cards.get(i));
            combinationsHelper(cards, r, i + 1, current, combinations);
            current.remove(current.size() - 1);
        }
    }

    // Implement helper methods for each hand type check
    private static boolean isStraightFlush(List<Card> cards) {
        return isFlush(cards) && isStraight(cards);
    }
    
    private static boolean isFourOfAKind(List<Card> cards) {
        Map<Card.Rank, Integer> rankCount = getRankCount(cards);
        return rankCount.containsValue(4);
    }
    
    private static boolean isFullHouse(List<Card> cards) {
        Map<Card.Rank, Integer> rankCount = getRankCount(cards);
        return rankCount.containsValue(3) && rankCount.containsValue(2);
    }

    private static boolean isFlush(List<Card> cards) {
        Card.Suit firstSuit = cards.get(0).getSuit();
        return cards.stream().allMatch(card -> card.getSuit() == firstSuit);
    }

    private static boolean isStraight(List<Card> cards) {
        List<Card.Rank> ranks = cards.stream()
            .map(Card::getRank)
            .sorted()
            .distinct()
            .toList();

        // Check regular straight
        if (isConsecutive(ranks)) return true;

        // Check Ace-low straight (A,2,3,4,5)
        if (ranks.contains(Card.Rank.ACE)) {
            List<Card.Rank> aceLowRanks = new ArrayList<>(ranks);
            aceLowRanks.remove(Card.Rank.ACE);
            aceLowRanks.add(0, Card.Rank.ACE); // Move Ace to front
            return isConsecutive(aceLowRanks);
        }
        return false;
    }

    private static boolean isConsecutive(List<Card.Rank> ranks) {
        for (int i = 0; i < ranks.size() - 1; i++) {
            if (ranks.get(i).ordinal() + 1 != ranks.get(i + 1).ordinal()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isThreeOfAKind(List<Card> cards) {
        Map<Card.Rank, Integer> rankCount = getRankCount(cards);
        return rankCount.containsValue(3);
    }

    private static boolean isTwoPair(List<Card> cards) {
        return getRankCount(cards).values().stream()
            .filter(count -> count == 2)
            .count() == 2;
    }

    private static boolean isOnePair(List<Card> cards) {
        return getRankCount(cards).values().stream()
            .filter(count -> count == 2)
            .count() == 1;
    }

    private static List<Card.Rank> getHighCard(List<Card> cards) {
        return Collections.singletonList(
            cards.stream()
                .map(Card::getRank)
                .max(Enum::compareTo)
                .orElseThrow()
        );
    }

    private static List<Card.Rank> getFourOfAKindRank(List<Card> cards) {
        Map<Card.Rank, Integer> rankCount = getRankCount(cards);
        List<Card.Rank> ranks = new ArrayList<>();
        
        // Get the rank of four of a kind
        Card.Rank fourOfAKindRank = rankCount.entrySet().stream()
            .filter(e -> e.getValue() == 4)
            .map(Map.Entry::getKey)
            .findFirst()
            .orElse(null);
            
        if (fourOfAKindRank != null) {
            ranks.add(fourOfAKindRank);
            
            // Add highest kicker
            rankCount.entrySet().stream()
                .filter(e -> e.getValue() == 1)
                .map(Map.Entry::getKey)
                .max(Enum::compareTo)
                .ifPresent(ranks::add);
        }
        
        return ranks;
    }

    private static List<Card.Rank> getFullHouseRank(List<Card> cards) {
        Map<Card.Rank, Integer> rankCount = getRankCount(cards);
        List<Card.Rank> ranks = new ArrayList<>();
        
        // Get three of a kind rank
        rankCount.entrySet().stream()
            .filter(e -> e.getValue() == 3)
            .map(Map.Entry::getKey)
            .findFirst()
            .ifPresent(ranks::add);
            
        // Get pair rank
        rankCount.entrySet().stream()
            .filter(e -> e.getValue() == 2)
            .map(Map.Entry::getKey)
            .findFirst()
            .ifPresent(ranks::add);
            
        return ranks;
    }

    private static List<Card.Rank> getStraightHighCard(List<Card> cards) {
        List<Card.Rank> ranks = cards.stream()
            .map(Card::getRank)
            .sorted()
            .distinct()
            .toList();
            
        // Handle Ace-low straight
        if (ranks.contains(Card.Rank.ACE) && 
            ranks.contains(Card.Rank.TWO) && 
            ranks.contains(Card.Rank.THREE) && 
            ranks.contains(Card.Rank.FOUR) && 
            ranks.contains(Card.Rank.FIVE)) {
            return Collections.singletonList(Card.Rank.FIVE);
        }
        
        return Collections.singletonList(ranks.get(ranks.size() - 1));
    }

    private static List<Card.Rank> getThreeOfAKindRank(List<Card> cards) {
        Map<Card.Rank, Integer> rankCount = getRankCount(cards);
        List<Card.Rank> ranks = new ArrayList<>();
        
        // Get three of a kind rank
        ranks.add(rankCount.entrySet().stream()
            .filter(e -> e.getValue() == 3)
            .map(Map.Entry::getKey)
            .findFirst()
            .orElseThrow());
            
        // Add kickers in descending order
        rankCount.entrySet().stream()
            .filter(e -> e.getValue() == 1)
            .map(Map.Entry::getKey)
            .sorted(Enum::compareTo)
            .forEach(ranks::add);
            
        return ranks;
    }

    private static List<Card.Rank> getTwoPairRanks(List<Card> cards) {
        Map<Card.Rank, Integer> rankCount = getRankCount(cards);
        List<Card.Rank> ranks = new ArrayList<>();
        
        // Get pair ranks in descending order
        rankCount.entrySet().stream()
            .filter(e -> e.getValue() == 2)
            .map(Map.Entry::getKey)
            .sorted(Comparator.reverseOrder())  // Sort descending
            .forEach(ranks::add);
            
        // Add kicker
        rankCount.entrySet().stream()
            .filter(e -> e.getValue() == 1)
            .map(Map.Entry::getKey)
            .max(Enum::compareTo)  // Get highest kicker
            .ifPresent(ranks::add);
            
        return ranks;
    }

    private static List<Card.Rank> getOnePairRank(List<Card> cards) {
        Map<Card.Rank, Integer> rankCount = getRankCount(cards);
        List<Card.Rank> ranks = new ArrayList<>();
        
        // Get pair rank
        ranks.add(rankCount.entrySet().stream()
            .filter(e -> e.getValue() == 2)
            .map(Map.Entry::getKey)
            .findFirst()
            .orElseThrow());
            
        // Add kickers in descending order
        rankCount.entrySet().stream()
            .filter(e -> e.getValue() == 1)
            .map(Map.Entry::getKey)
            .sorted(Enum::compareTo)
            .forEach(ranks::add);
            
        return ranks;
    }
    
    private static Map<Card.Rank, Integer> getRankCount(List<Card> cards) {
        Map<Card.Rank, Integer> rankCount = new HashMap<>();
        for (Card card : cards) {
            rankCount.merge(card.getRank(), 1, Integer::sum);
        }
        return rankCount;
    }
}

class HandRanking implements Comparable<HandRanking> {
    private final HandType type;
    private final List<Card.Rank> tiebreakers;
    
    public HandRanking(HandType type, List<Card.Rank> tiebreakers) {
        this.type = type;
        this.tiebreakers = tiebreakers;
    }
    
    @Override
    public int compareTo(HandRanking other) {
        int typeComparison = type.compareTo(other.type);
        if (typeComparison != 0) return typeComparison;
        
        // Compare tiebreakers in order
        for (int i = 0; i < tiebreakers.size() && i < other.tiebreakers.size(); i++) {
            int rankComparison = tiebreakers.get(i).compareTo(other.tiebreakers.get(i));
            if (rankComparison != 0) return rankComparison;
        }
        return 0;
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

class PokerHandLookup implements Serializable {
    private static final long serialVersionUID = 1L;
    private final Map<String, SimulationResult> results;
    private final int simulationCount;
    private final long timestamp;

    public PokerHandLookup(int simulationCount) {
        this.results = new HashMap<>();
        this.simulationCount = simulationCount;
        this.timestamp = System.currentTimeMillis();
    }

    public void addResult(String key, SimulationResult result) {
        results.put(key, result);
    }

    public Set<String> getAllKeys() {
        return Collections.unmodifiableSet(results.keySet());
    }

    public SimulationResult getResult(String key) {
        return results.get(key);
    }

    public int getSimulationCount() {
        return simulationCount;
    }
}