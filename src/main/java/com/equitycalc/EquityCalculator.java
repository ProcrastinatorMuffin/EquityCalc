package com.equitycalc;

import com.equitycalc.model.Card;
import com.equitycalc.model.Deck;
import com.equitycalc.model.Hand;
import com.equitycalc.model.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// TODO: Implement hand evaluation logic
// TODO: Add Monte Carlo simulation for complex scenarios
// TODO: Add range-based calculations

public class EquityCalculator {
    private static final int SIMULATION_COUNT = 10000000;
    private static final int BOARD_SIZE = 5;
    private static final Random RANDOM = new Random();

    private static final String LOOKUP_FILE = "poker_lookups.bin";
    private static final int[] HAND_RANKS = new int[7462];
    private static final int[] FLUSH_LOOKUP = new int[8192];
    private static final int[] RANK_LOOKUP = new int[8192];

    // Initialize lookup tables in static block
    static {
        loadOrInitializeLookupTables();
    }

    private static void loadOrInitializeLookupTables() {
        if (!loadLookupTables()) {
            System.out.println("Generating new lookup tables...");
            initializeLookupTables();
            saveLookupTables();
        }
    }

    private static boolean loadLookupTables() {
        File lookupFile = new File(LOOKUP_FILE);
        if (!lookupFile.exists()) {
            return false;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(lookupFile))) {
            System.out.println("Loading lookup tables from file...");
            int[] handRanks = (int[]) ois.readObject();
            int[] flushLookup = (int[]) ois.readObject();
            int[] rankLookup = (int[]) ois.readObject();
            
            System.arraycopy(handRanks, 0, HAND_RANKS, 0, handRanks.length);
            System.arraycopy(flushLookup, 0, FLUSH_LOOKUP, 0, flushLookup.length);
            System.arraycopy(rankLookup, 0, RANK_LOOKUP, 0, rankLookup.length);
            
            System.out.println("Lookup tables loaded successfully");
            return true;
        } catch (Exception e) {
            System.err.println("Error loading lookup tables: " + e.getMessage());
            return false;
        }
    }

    private static void saveLookupTables() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(LOOKUP_FILE))) {
            System.out.println("Saving lookup tables to file...");
            oos.writeObject(HAND_RANKS);
            oos.writeObject(FLUSH_LOOKUP);
            oos.writeObject(RANK_LOOKUP);
            System.out.println("Lookup tables saved successfully");
        } catch (Exception e) {
            System.err.println("Error saving lookup tables: " + e.getMessage());
        }
    }

    private static void initializeLookupTables() {
        System.out.println("Initializing lookup tables...");
        // Prime numbers for hash function
        final int[] PRIMES = {2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41};
        
        // Initialize straight and flush masks
        for (int straight = 0; straight < 0x1FFF; straight++) {
            if (isStraight(straight)) {
                int value = straightValue(straight);
                RANK_LOOKUP[straight] = value;
            }
        }
        System.out.println("Straight lookup initialized");
        
        // Initialize flush lookup
        for (int flush = 0; flush < 0x1FFF; flush++) {
            int count = Integer.bitCount(flush);
            if (count >= 5) {
                FLUSH_LOOKUP[flush] = calculateFlushValue(flush);
            }
        }
        System.out.println("Flush lookup initialized");
        
        // Initialize rank combinations
        for (int ranks = 0; ranks < 0x1FFF; ranks++) {
            int count = Integer.bitCount(ranks);
            if (count >= 5) {
                RANK_LOOKUP[ranks] = calculateRankValue(ranks);
            }
        }
        System.out.println("Rank lookup initialized");
        
        // Initialize hand ranks table using perfect hash
        for (int i = 0; i < 7462; i++) {
            HAND_RANKS[i] = calculateHandRank(i);
            System.out.println("Hand rank " + i + ": " + HAND_RANKS[i]);
        }
        System.out.println("Hand ranks initialized");
    }
    
    private static boolean isStraight(int rankMask) {
        // Check for A-5 straight
        if ((rankMask & 0x100F) == 0x100F) return true;
        
        // Check for normal straights
        for (int i = 0; i <= 8; i++) {
            if ((rankMask & (0x1F << i)) == (0x1F << i)) return true;
        }
        return false;
    }
    
    private static int straightValue(int rankMask) {
        // A-5 straight
        if ((rankMask & 0x100F) == 0x100F) return 5;
        
        // Find highest straight
        for (int i = 8; i >= 0; i--) {
            if ((rankMask & (0x1F << i)) == (0x1F << i)) {
                return i + 9;
            }
        }
        return 0;
    }
    
    private static int calculateFlushValue(int flushMask) {
        int value = 0;
        int shift = 0;
        
        // Calculate value based on highest cards in flush
        while (flushMask != 0) {
            if ((flushMask & 1) != 0) {
                value += (1 << shift);
            }
            flushMask >>= 1;
            shift++;
        }
        return value + 6000; // Flush base value
    }
    
    private static int calculateRankValue(int rankMask) {
        int[] counts = new int[13];
        int value = 0;
        
        // Count occurrences of each rank
        for (int i = 0; i < 13; i++) {
            if ((rankMask & (1 << i)) != 0) {
                counts[i]++;
            }
        }
        
        // Calculate hand value based on rank patterns
        boolean hasThreeOfKind = false;
        int pairs = 0;
        
        for (int i = 12; i >= 0; i--) {
            if (counts[i] == 4) return 5000 + i; // Four of a kind
            if (counts[i] == 3) {
                hasThreeOfKind = true;
                value = 3000 + i;
            }
            if (counts[i] == 2) {
                pairs++;
                value = 2000 + i;
            }
        }
        
        if (hasThreeOfKind && pairs > 0) return 4000 + value; // Full house
        if (hasThreeOfKind) return value; // Three of a kind
        if (pairs == 2) return value + 2500; // Two pair
        if (pairs == 1) return value; // One pair
        
        // High card
        return highCardValue(rankMask);
    }
    
    private static int highCardValue(int rankMask) {
        int value = 0;
        int multiplier = 1;
        
        for (int i = 12; i >= 0 && multiplier <= 100000; i--) {
            if ((rankMask & (1 << i)) != 0) {
                value += i * multiplier;
                multiplier *= 13;
            }
        }
        return value;
    }
    
    // Base hand rankings
    private static final int STRAIGHT_FLUSH_BASE = 8000;
    private static final int FOUR_KIND_BASE = 7000;
    private static final int FULL_HOUSE_BASE = 6000;
    private static final int FLUSH_BASE = 5000;
    private static final int STRAIGHT_BASE = 4000;
    private static final int THREE_KIND_BASE = 3000;
    private static final int TWO_PAIR_BASE = 2000;
    private static final int PAIR_BASE = 1000;

    private static int calculateHandRank(int index) {
        
        // Convert index to 7-card combination
        int[] cards = indexToCards(index);
        
        // Extract suits and ranks
        int[] suitCounts = new int[4];
        int[] rankCounts = new int[13];
        int rankMask = 0;
        
        for (int card : cards) {
            int suit = card / 13;
            int rank = card % 13;
            suitCounts[suit]++;
            rankCounts[rank]++;
            rankMask |= (1 << rank);
        }
        
        // Check for straight flush
        for (int suit = 0; suit < 4; suit++) {
            if (suitCounts[suit] >= 5) {
                int suitMask = 0;
                for (int card : cards) {
                    if (card / 13 == suit) {
                        suitMask |= (1 << (card % 13));
                    }
                }
                if (isStraight(suitMask)) {
                    return STRAIGHT_FLUSH_BASE + straightValue(suitMask);
                }
            }
        }
        
        // Check for four of a kind
        for (int rank = 12; rank >= 0; rank--) {
            if (rankCounts[rank] == 4) {
                return FOUR_KIND_BASE + rank;
            }
        }
        
        // Check for full house
        int tripRank = -1;
        int pairRank = -1;
        for (int rank = 12; rank >= 0; rank--) {
            if (rankCounts[rank] == 3) {
                if (tripRank == -1) tripRank = rank;
            } else if (rankCounts[rank] >= 2) {
                if (pairRank == -1) pairRank = rank;
            }
        }
        if (tripRank != -1 && pairRank != -1) {
            return FULL_HOUSE_BASE + (tripRank * 13) + pairRank;
        }
        
        // Check for flush
        for (int suit = 0; suit < 4; suit++) {
            if (suitCounts[suit] >= 5) {
                return FLUSH_BASE + calculateFlushValue(rankMask);
            }
        }
        
        // Check for straight
        if (isStraight(rankMask)) {
            return STRAIGHT_BASE + straightValue(rankMask);
        }
        
        // Check for three of a kind
        if (tripRank != -1) {
            return THREE_KIND_BASE + tripRank;
        }
        
        // Check for two pair
        int firstPair = -1;
        int secondPair = -1;
        for (int rank = 12; rank >= 0; rank--) {
            if (rankCounts[rank] >= 2) {
                if (firstPair == -1) firstPair = rank;
                else if (secondPair == -1) {
                    secondPair = rank;
                    break;
                }
            }
        }
        if (firstPair != -1 && secondPair != -1) {
            return TWO_PAIR_BASE + (firstPair * 13) + secondPair;
        }
        
        // Check for one pair
        if (firstPair != -1) {
            return PAIR_BASE + firstPair;
        }
        
        // High card
        return highCardValue(rankMask);
    }
    
    private static long combinations(int n, int r) {
        if (r > n) return 0;
        if (r == 0 || r == n) return 1;
        if (r > n - r) r = n - r;
        
        long result = 1;
        for (int i = 0; i < r; i++) {
            result *= (n - i);
            result /= (i + 1);
        }
        return result;
    }

    // Update indexToCards to use long for calculations
    private static int[] indexToCards(int index) {
        int[] cards = new int[7];
        long remaining = index;
        int card = 0;
        int pos = 0;
        
        while (pos < 7) {
            long count = combinations(51 - card, 6 - pos);
            if (remaining >= count) {
                remaining -= count;
                card++;
            } else {
                cards[pos++] = card;
                card++;
            }
        }
        return cards;
    }

    // Add thread-safe counters for wins/ties
    private static class HandResult {
        private final AtomicInteger wins = new AtomicInteger(0);
        private final AtomicInteger ties = new AtomicInteger(0);
        
        public void addWin() {
            wins.incrementAndGet();
        }
        
        public void addTie() {
            ties.incrementAndGet();
        }
        
        public int getWins() {
            return wins.get();
        }
        
        public int getTies() {
            return ties.get();
        }
    }
    
    public void calculateEquity(List<Player> players, List<Card> communityCards) {
        validateInput(players, communityCards);
        
        Map<Player, HandResult> results = new ConcurrentHashMap<>();
        players.forEach(p -> results.put(p, new HandResult()));

        long[] playerHands = players.stream()
            .mapToLong(p -> convertToBitCards(p.getHoleCards()))
            .toArray();
        long boardMask = communityCards != null ? convertToBitCards(communityCards) : 0L;

        IntStream.range(0, SIMULATION_COUNT)
            .parallel()
            .forEach(i -> simulateHand(playerHands, boardMask, communityCards, players, results));

        // Calculate final probabilities
        for (Player player : players) {
            HandResult result = results.get(player);
            double totalHands = SIMULATION_COUNT;
            double winCount = result.getWins();
            double tieCount = result.getTies();
            
            player.setWinProbability(winCount / totalHands);
            player.setSplitProbability(tieCount / totalHands);
            player.setLossProbability(1.0 - ((winCount + tieCount) / totalHands));
        }
    }

    private void validateInput(List<Player> players, List<Card> communityCards) {
        // Validate players
        if (players == null || players.isEmpty()) {
            throw new IllegalArgumentException("Players list cannot be null or empty");
        }
        if (players.size() < 2 || players.size() > 10) {
            throw new IllegalArgumentException("Number of players must be between 2 and 10");
        }
    
        // Validate hole cards
        Set<Card> usedCards = new HashSet<>();
        for (Player player : players) {
            List<Card> holeCards = player.getHoleCards();
            if (holeCards == null || holeCards.size() != 2) {
                throw new IllegalArgumentException("Each player must have exactly 2 hole cards");
            }
            
            // Check for duplicate cards
            for (Card card : holeCards) {
                if (!usedCards.add(card)) {
                    throw new IllegalArgumentException("Duplicate card detected: " + card);
                }
            }
        }
    
        // Validate community cards
        if (communityCards != null) {
            if (communityCards.size() != 0 && communityCards.size() != 3 
                && communityCards.size() != 4 && communityCards.size() != 5) {
                throw new IllegalArgumentException(
                    "Community cards must be null or contain 0, 3, 4, or 5 cards");
            }
    
            // Check for duplicate cards
            for (Card card : communityCards) {
                if (!usedCards.add(card)) {
                    throw new IllegalArgumentException("Duplicate card detected: " + card);
                }
            }
        }
    }

    
    private void simulateHand(long[] playerHands, long boardMask, List<Card> communityCards,
                     List<Player> players, Map<Player, HandResult> results) {
        // Generate random board
        long finalBoard = generateRandomBoard(boardMask, communityCards, playerHands);
        
        // Evaluate hands
        int[] handValues = new int[players.size()];
        for (int i = 0; i < players.size(); i++) {
            handValues[i] = evaluateHandFast(playerHands[i] | finalBoard);
        }

        // Find best hand value
        int bestHand = Integer.MIN_VALUE;
        for (int value : handValues) {
            bestHand = Math.max(bestHand, value);
        }
        
        // Count winners
        int winnerCount = 0;
        List<Integer> winners = new ArrayList<>();
        for (int i = 0; i < handValues.length; i++) {
            if (handValues[i] == bestHand) {
                winners.add(i);
                winnerCount++;
            }
        }

        // Update results
        if (winnerCount == 1) {
            results.get(players.get(winners.get(0))).addWin();
        } else {
            for (int winner : winners) {
                results.get(players.get(winner)).addTie();
            }
        }
    }

    private long generateRandomBoard(long boardMask, List<Card> communityCards, long[] playerHands) {
        // Initialize deck and remove used cards
        Deck deck = new Deck();
        List<Card> usedCards = new ArrayList<>();
        
        // Add existing community cards to used cards
        if (communityCards != null) {
            usedCards.addAll(communityCards);
        }
        
        // Add player hole cards to used cards
        for (long handMask : playerHands) {
            usedCards.addAll(convertFromBitMask(handMask));
        }
        
        // Remove used cards from deck
        for (Card card : usedCards) {
            deck.cards.remove(card);
        }
        
        // Calculate how many more cards needed
        int cardsNeeded = BOARD_SIZE - (communityCards != null ? communityCards.size() : 0);
        
        // Draw random cards
        List<Card> newBoardCards = deck.dealCards(cardsNeeded);
        
        // Combine existing board with new cards
        if (communityCards != null) {
            newBoardCards.addAll(communityCards);
        }
        
        // Convert to bit mask
        return convertToBitCards(newBoardCards);
    }
    
    private List<Card> convertFromBitMask(long bitMask) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 52; i++) {
            if ((bitMask & (1L << i)) != 0) {
                int rank = i % 13;
                int suit = i / 13;
                cards.add(new Card(Card.Rank.values()[rank], Card.Suit.values()[suit]));
            }
        }
        return cards;
    }

    // Fix card conversion
    private long convertToBitCards(List<Card> cards) {
        if (cards == null) return 0L;
        
        long bitMask = 0L;
        for (Card card : cards) {
            // Reverse order: rank first, then suit
            int cardIndex = card.getRank().ordinal() + (card.getSuit().ordinal() * 13);
            bitMask |= 1L << cardIndex;
        }
        return bitMask;
    }

    private int generateHandKey(long cardMask) {
        // Initialize counters
        int[] suitCounts = new int[4];
        int[] rankCounts = new int[13];
        
        // Count cards by suit and rank
        for (int i = 0; i < 52; i++) {
            if ((cardMask & (1L << i)) != 0) {
                int rank = i % 13;
                int suit = i / 13;
                suitCounts[suit]++;
                rankCounts[rank]++;
            }
        }
        
        // Use a better hashing algorithm that avoids overflow
        long key = 0;
        final int PRIME = 31;
        
        // Hash rank counts (0-4 possible values)
        for (int i = 0; i < 13; i++) {
            key = (key * PRIME + rankCounts[i]) & 0x7FFFFFFFL;
        }
        
        // Hash suit counts (0-7 possible values)
        for (int i = 0; i < 4; i++) {
            key = (key * PRIME + suitCounts[i]) & 0x7FFFFFFFL;
        }
        
        // Map to valid index range [0, HAND_RANKS.length)
        return (int)(key % HAND_RANKS.length);
    }
    
    private int evaluateHandFast(long cardMask) {
        // Validate input
        if (cardMask == 0) {
            return 0;
        }
        
        int key = generateHandKey(cardMask);
        
        // Ensure positive index
        if (key < 0) {
            key = Math.abs(key % HAND_RANKS.length);
        }
        
        return HAND_RANKS[key];
    }
}