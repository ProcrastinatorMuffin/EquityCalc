package com.equitycalc.simulation;

import com.equitycalc.model.Card;
import com.equitycalc.model.PokerHand;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;



public class BitHandEvaluator {
    // Lookup tables
    private static final long[] STRAIGHT_MASKS;
    private static final long[] FLUSH_MASKS;
    private static final int[] RANK_COUNT_LOOKUP;

    private static final Map<Long, HandRanking> HAND_CACHE = new ConcurrentHashMap<>();
    private static final AtomicLong cacheHits = new AtomicLong();
    private static final AtomicLong cacheMisses = new AtomicLong();
    
    // Initialize lookup tables
    static {
        STRAIGHT_MASKS = initStraightMasks();
        FLUSH_MASKS = initFlushMasks();
        RANK_COUNT_LOOKUP = initRankCountLookup();
    }

    private static class RankAnalysis {
        final int[] rankCounts;
        long rankBits;
        final int maxCount;
        final int pairCount;
        
        RankAnalysis(long cardBits) {
            rankCounts = new int[Card.Rank.values().length];
            rankBits = 0L;
            
            // Use lookup table for rank counting
            for (int i = 0; i < 52; i += 4) { // Process 4 bits at a time
                int rankIndex = (int)((cardBits >> i) & 0xF);
                if (rankIndex > 0) {
                    rankCounts[i >> 2] = RANK_COUNT_LOOKUP[rankIndex];
                    if (rankCounts[i >> 2] > 0) {
                        rankBits |= (1L << (i >> 2));
                    }
                }
            }
            
            // Calculate derived values
            int maxCount = 0;
            int pairs = 0;
            for (int count : rankCounts) {
                if (count > maxCount) maxCount = count;
                if (count == 2) pairs++;
            }
            this.maxCount = maxCount;
            this.pairCount = pairs;
        }
    }

    private static RankAnalysis analyzeRanks(long cardBits) {
        return new RankAnalysis(cardBits);
    }
    
    // Main evaluation methods
    public static HandRanking evaluateHand(PokerHand hand) throws InterruptedException, ExecutionException {
        if (hand == null) {
            throw new IllegalArgumentException("Hand cannot be null");
        }
        
        int numCards = hand.getCardCount();
        if (numCards < 5) {
            throw new IllegalArgumentException("Hand must contain at least 5 cards, found: " + numCards);
        }
        
        // Handle 5-card hands directly
        if (numCards == 5) {
            long bits = hand.toBitMask();
            HandRanking cached = HAND_CACHE.get(bits);
            if (cached != null) {
                cacheHits.incrementAndGet();
                return cached;
            }
            return HAND_CACHE.computeIfAbsent(bits, key -> {
                cacheMisses.incrementAndGet();
                return evaluateFiveCardBits(key);
            });
        }
        
        long handBits = hand.toBitMask();
        List<Long> combinations = generateCombinations(handBits, numCards);
        
        // Single parallel stream with work stealing
        return combinations.parallelStream()
            .map(bits -> {
                HandRanking cached = HAND_CACHE.get(bits);
                if (cached != null) {
                    cacheHits.incrementAndGet();
                    return cached;
                }
                return HAND_CACHE.computeIfAbsent(bits, key -> {
                    cacheMisses.incrementAndGet();
                    return evaluateFiveCardBits(key);
                });
            })
            .max(HandRanking::compareTo)
            .orElseThrow(() -> new IllegalStateException("No valid combinations"));
    }

    private static List<Long> generateCombinations(long handBits, int numCards) {
        List<Long> combinations = new ArrayList<>();
        // Pre-calculate all combinations
        int[] cardIndices = new int[numCards];
        int idx = 0;
        for (int i = 0; i < 52; i++) {
            if ((handBits & (1L << i)) != 0) {
                cardIndices[idx++] = i;
            }
        }
        
        // Generate 5-card combinations
        combinationsHelper(cardIndices, 5, 0, new int[5], 0, combinations, handBits);
        return combinations;
    }
    
    private static void combinationsHelper(int[] cards, int r, int start, int[] temp, int tempIndex,
                                         List<Long> result, long handBits) {
        if (tempIndex == r) {
            long combination = 0L;
            for (int i = 0; i < r; i++) {
                combination |= 1L << temp[i];
            }
            result.add(combination);
            return;
        }
        
        for (int i = start; i <= cards.length - r + tempIndex; i++) {
            temp[tempIndex] = cards[i];
            combinationsHelper(cards, r, i + 1, temp, tempIndex + 1, result, handBits);
        }
    }
    
    private static HandRanking evaluateFiveCardBits(long cardBits) {
        // Validate input - must have exactly 5 cards
        if (Card.countCards(cardBits) != 5) {
            throw new IllegalArgumentException("Must evaluate exactly 5 cards, found: " + 
                Card.countCards(cardBits));
        }
        
        // Check hands from highest to lowest ranking
        try {
            // Straight Flush
            if (isStraightFlush(cardBits)) {
                List<Card.Rank> ranks = getStraightRanks(cardBits);
                return new HandRanking(HandRanking.Type.STRAIGHT_FLUSH, ranks);
            }
            
            // Four of a Kind
            if (isFourOfAKind(cardBits)) {
                List<Card.Rank> ranks = getFourOfAKindRanks(cardBits);
                return new HandRanking(HandRanking.Type.FOUR_OF_A_KIND, ranks);
            }
            
            // Full House
            if (isFullHouse(cardBits)) {
                List<Card.Rank> ranks = getFullHouseRanks(cardBits);
                return new HandRanking(HandRanking.Type.FULL_HOUSE, ranks);
            }
            
            // Flush
            if (isFlush(cardBits)) {
                List<Card.Rank> ranks = getFlushRanks(cardBits);
                return new HandRanking(HandRanking.Type.FLUSH, ranks);
            }
            
            // Straight
            if (isStraight(cardBits)) {
                List<Card.Rank> ranks = getStraightRanks(cardBits);
                return new HandRanking(HandRanking.Type.STRAIGHT, ranks);
            }
            
            // Three of a Kind
            if (isThreeOfAKind(cardBits)) {
                List<Card.Rank> ranks = getThreeOfAKindRanks(cardBits);
                return new HandRanking(HandRanking.Type.THREE_OF_A_KIND, ranks);
            }
            
            // Two Pair
            if (isTwoPair(cardBits)) {
                List<Card.Rank> ranks = getTwoPairRanks(cardBits);
                return new HandRanking(HandRanking.Type.TWO_PAIR, ranks);
            }
            
            // One Pair
            if (isOnePair(cardBits)) {
                List<Card.Rank> ranks = getOnePairRanks(cardBits);
                return new HandRanking(HandRanking.Type.ONE_PAIR, ranks);
            }
            
            // High Card
            List<Card.Rank> ranks = getHighCardRanks(cardBits);
            return new HandRanking(HandRanking.Type.HIGH_CARD, ranks);
            
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Error evaluating hand: " + e.getMessage(), e);
        }
    }
    
    // Helper methods
    private static long[] initStraightMasks() {
        // We need 10 possible straights including Ace-low
        long[] straightMasks = new long[10];
        
        // Generate regular straights (T-A through 2-6)
        for (int i = 0; i < 9; i++) {
            long mask = 0L;
            // For each straight, set bits for 5 consecutive ranks
            for (int rank = i; rank < i + 5; rank++) {
                // For each rank, set bits for all suits
                for (int suit = 0; suit < 4; suit++) {
                    int bitPosition = (rank << Card.SUIT_BITS) | suit;
                    mask |= 1L << bitPosition;
                }
            }
            straightMasks[i] = mask;
        }
        
        // Handle Ace-low straight (A,2,3,4,5)
        long aceLowMask = 0L;
        // Add Ace bits (all suits)
        for (int suit = 0; suit < 4; suit++) {
            int aceBitPosition = (Card.Rank.ACE.ordinal() << Card.SUIT_BITS) | suit;
            aceLowMask |= 1L << aceBitPosition;
        }
        // Add 2,3,4,5 bits (all suits)
        for (int rank = 0; rank < 4; rank++) {
            for (int suit = 0; suit < 4; suit++) {
                int bitPosition = (rank << Card.SUIT_BITS) | suit;
                aceLowMask |= 1L << bitPosition;
            }
        }
        straightMasks[9] = aceLowMask;
        
        return straightMasks;
    }
    
    private static long[] initFlushMasks() {
        // One mask for each suit
        long[] flushMasks = new long[4];
        
        // Generate mask for each suit
        for (int suit = 0; suit < 4; suit++) {
            long mask = 0L;
            // Set bits for all 13 ranks of this suit
            for (int rank = 0; rank < 13; rank++) {
                int bitPosition = (rank << Card.SUIT_BITS) | suit;
                mask |= 1L << bitPosition;
            }
            flushMasks[suit] = mask;
        }
        
        return flushMasks;
    }
    
    private static int[] initRankCountLookup() {
        // Create lookup table for 13 possible ranks (2^13 combinations)
        int[] rankCountLookup = new int[1 << 13];
        
        // For each possible combination of ranks
        for (int i = 0; i < rankCountLookup.length; i++) {
            int count = 0;
            int pattern = i;
            
            // Count bits set to 1 (representing present ranks)
            while (pattern != 0) {
                count += pattern & 1;
                pattern >>>= 1;
            }
            
            rankCountLookup[i] = count;
        }
        
        return rankCountLookup;
    }
    
    // Bit manipulation helpers
    private static boolean isStraightFlush(long cardBits) {
        if (Card.countCards(cardBits) != 5) {
            return false;
        }
        
        // Check each suit
        for (long flushMask : FLUSH_MASKS) {
            long suitCards = cardBits & flushMask;
            if (Card.countCards(suitCards) == 5) {
                // Found flush, now check if these cards form a straight
                long rankBits = 0L;
                for (int i = 0; i < 52; i++) {
                    if ((suitCards & (1L << i)) != 0) {
                        int rank = i >> Card.SUIT_BITS;
                        rankBits |= (1L << rank);
                    }
                }
                
                // Check each straight pattern
                for (long straightMask : STRAIGHT_MASKS) {
                    boolean isMatch = true;
                    for (int i = 0; i < 13; i++) {
                        if ((straightMask & (1L << (i * 4))) != 0) {
                            if ((rankBits & (1L << i)) == 0) {
                                isMatch = false;
                                break;
                            }
                        }
                    }
                    if (isMatch) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    private static boolean isFourOfAKind(long cardBits) {
        if (Card.countCards(cardBits) != 5) return false;
        return analyzeRanks(cardBits).maxCount == 4;
    }
    
    private static boolean isFullHouse(long cardBits) {
        if (Card.countCards(cardBits) != 5) return false;
        RankAnalysis analysis = analyzeRanks(cardBits);
        return analysis.maxCount == 3 && analysis.pairCount == 1;
    }
    
    private static boolean isFlush(long cardBits) {
        if (Card.countCards(cardBits) != 5) {
            return false;
        }
        
        // Check each suit using pre-computed flush masks
        for (long flushMask : FLUSH_MASKS) {
            // If all bits in cardBits are in one suit's mask and count is 5,
            // then it's a flush
            if (Card.countCards(cardBits & flushMask) == 5) {
                return true;
            }
        }
        
        return false;
    }
    
    private static boolean isStraight(long cardBits) {
        if (Card.countCards(cardBits) != 5) {
            return false;
        }
        
        // Get unique ranks (remove suit information)
        long rankBits = 0L;
        for (int i = 0; i < 52; i++) {
            if ((cardBits & (1L << i)) != 0) {
                int rank = i >> Card.SUIT_BITS;
                rankBits |= (1L << rank);
            }
        }
        
        // Check each straight pattern using pre-computed masks
        for (long straightMask : STRAIGHT_MASKS) {
            // For each straight pattern, check if we have one card of each required rank
            boolean isMatch = true;
            for (int i = 0; i < 13; i++) {
                // If this rank is required by the straight pattern
                if ((straightMask & (1L << (i * 4))) != 0) {
                    // Check if we have a card of this rank
                    if ((rankBits & (1L << i)) == 0) {
                        isMatch = false;
                        break;
                    }
                }
            }
            if (isMatch) {
                return true;
            }
        }
        
        return false;
    }

    private static boolean isThreeOfAKind(long cardBits) {
        if (Card.countCards(cardBits) != 5) return false;
        RankAnalysis analysis = analyzeRanks(cardBits);
        return analysis.maxCount == 3 && analysis.pairCount == 0;
    }

    private static boolean isTwoPair(long cardBits) {
        if (Card.countCards(cardBits) != 5) return false;
        return analyzeRanks(cardBits).pairCount == 2;
    }

    private static boolean isOnePair(long cardBits) {
        if (Card.countCards(cardBits) != 5) return false;
        RankAnalysis analysis = analyzeRanks(cardBits);
        return analysis.pairCount == 1 && analysis.maxCount == 2;
    }
    
    // Rank extraction helpers
    private static List<Card.Rank> getHighestRanks(long cardBits) {
        if (cardBits == 0) {
            throw new IllegalArgumentException("No cards in bit mask");
        }
    
        List<Card.Rank> ranks = new ArrayList<>();
        
        // Process each bit position
        for (int i = 0; i < 52; i++) {
            if ((cardBits & (1L << i)) != 0) {
                // Extract rank from bit position (rank is in upper 4 bits)
                Card card = Card.fromBits(i);
                ranks.add(card.getRank());
            }
        }
        
        // Sort ranks in descending order
        Collections.sort(ranks, Collections.reverseOrder());
        
        return ranks;
    }
    
    private static List<Card.Rank> getFourOfAKindRanks(long cardBits) {
        if (cardBits == 0) {
            throw new IllegalArgumentException("No cards in bit mask");
        }
    
        List<Card.Rank> result = new ArrayList<>();
        int[] rankCounts = new int[Card.Rank.values().length];
        
        // Count ranks using bit manipulation
        for (int i = 0; i < 52; i++) {
            if ((cardBits & (1L << i)) != 0) {
                Card card = Card.fromBits(i);
                rankCounts[card.getRank().ordinal()]++;
            }
        }
        
        // Find four of a kind rank
        Card.Rank quadRank = null;
        for (int i = 0; i < rankCounts.length; i++) {
            if (rankCounts[i] == 4) {
                quadRank = Card.Rank.values()[i];
                break;
            }
        }
        
        if (quadRank == null) {
            throw new IllegalArgumentException("No four of a kind found in bit mask");
        }
        
        // Add quad rank first
        result.add(quadRank);
        
        // Add highest kicker
        for (int i = rankCounts.length - 1; i >= 0; i--) {
            if (rankCounts[i] == 1) {
                result.add(Card.Rank.values()[i]);
                break;
            }
        }
        
        return result;
    }
    
    private static List<Card.Rank> getFullHouseRanks(long cardBits) {
        if (cardBits == 0) {
            throw new IllegalArgumentException("No cards in bit mask");
        }
    
        List<Card.Rank> result = new ArrayList<>();
        int[] rankCounts = new int[Card.Rank.values().length];
        
        // Count ranks
        for (int i = 0; i < 52; i++) {
            if ((cardBits & (1L << i)) != 0) {
                Card card = Card.fromBits(i);
                rankCounts[card.getRank().ordinal()]++;
            }
        }
        
        // Find trips rank (highest if multiple)
        Card.Rank tripsRank = null;
        for (int i = rankCounts.length - 1; i >= 0; i--) {
            if (rankCounts[i] == 3) {
                tripsRank = Card.Rank.values()[i];
                break;
            }
        }
        
        // Find pair rank (highest if multiple)
        Card.Rank pairRank = null;
        for (int i = rankCounts.length - 1; i >= 0; i--) {
            if (rankCounts[i] == 2) {
                pairRank = Card.Rank.values()[i];
                break;
            }
        }
        
        if (tripsRank == null || pairRank == null) {
            throw new IllegalArgumentException("No full house found in bit mask");
        }
        
        result.add(tripsRank);
        result.add(pairRank);
        
        return result;
    }

    private static List<Card.Rank> getFlushRanks(long cardBits) {
        if (cardBits == 0) {
            throw new IllegalArgumentException("No cards in bit mask");
        }
    
        List<Card.Rank> result = new ArrayList<>();
        
        // Find which suit has the flush
        for (long flushMask : FLUSH_MASKS) {
            long suitCards = cardBits & flushMask;
            if (Card.countCards(suitCards) == 5) {
                // Extract ranks from flush suit
                for (int i = 0; i < 52; i++) {
                    if ((suitCards & (1L << i)) != 0) {
                        Card card = Card.fromBits(i);
                        result.add(card.getRank());
                    }
                }
                
                // Sort ranks in descending order
                Collections.sort(result, Collections.reverseOrder());
                return result;
            }
        }
        
        throw new IllegalArgumentException("No flush found in bit mask");
    }

    private static List<Card.Rank> getStraightRanks(long cardBits) {
        if (cardBits == 0) {
            throw new IllegalArgumentException("No cards in bit mask");
        }
    
        List<Card.Rank> result = new ArrayList<>();
        
        // Get unique ranks
        long rankBits = 0L;
        for (int i = 0; i < 52; i++) {
            if ((cardBits & (1L << i)) != 0) {
                int rank = i >> Card.SUIT_BITS;
                rankBits |= (1L << rank);
            }
        }
        
        // Check each straight pattern
        for (int startRank = 0; startRank < 9; startRank++) {
            boolean isMatch = true;
            // Check 5 consecutive ranks
            for (int offset = 0; offset < 5; offset++) {
                if ((rankBits & (1L << (startRank + offset))) == 0) {
                    isMatch = false;
                    break;
                }
            }
            if (isMatch) {
                // Add highest rank first
                result.add(Card.Rank.values()[startRank + 4]);
                return result;
            }
        }
        
        // Check Ace-low straight (A,2,3,4,5)
        if ((rankBits & (1L << Card.Rank.ACE.ordinal())) != 0) {
            boolean isAceLow = true;
            for (int rank = 0; rank < 4; rank++) {
                if ((rankBits & (1L << rank)) == 0) {
                    isAceLow = false;
                    break;
                }
            }
            if (isAceLow) {
                result.add(Card.Rank.FIVE);
                return result;
            }
        }
        
        throw new IllegalArgumentException("No straight found in bit mask");
    }

    private static List<Card.Rank> getThreeOfAKindRanks(long cardBits) {
        if (cardBits == 0) {
            throw new IllegalArgumentException("No cards in bit mask");
        }
    
        List<Card.Rank> result = new ArrayList<>();
        int[] rankCounts = new int[Card.Rank.values().length];
        
        // Count ranks
        for (int i = 0; i < 52; i++) {
            if ((cardBits & (1L << i)) != 0) {
                Card card = Card.fromBits(i);
                rankCounts[card.getRank().ordinal()]++;
            }
        }
        
        // Find trips rank
        Card.Rank tripsRank = null;
        for (int i = 0; i < rankCounts.length; i++) {
            if (rankCounts[i] == 3) {
                tripsRank = Card.Rank.values()[i];
                break;
            }
        }
        
        if (tripsRank == null) {
            throw new IllegalArgumentException("No three of a kind found in bit mask");
        }
        
        // Add trips rank first
        result.add(tripsRank);
        
        // Add remaining kickers in descending order
        for (int i = rankCounts.length - 1; i >= 0; i--) {
            if (rankCounts[i] == 1) {
                result.add(Card.Rank.values()[i]);
            }
        }
        
        return result;
    }

    private static List<Card.Rank> getTwoPairRanks(long cardBits) {
        if (cardBits == 0) {
            throw new IllegalArgumentException("No cards in bit mask");
        }
    
        List<Card.Rank> result = new ArrayList<>();
        int[] rankCounts = new int[Card.Rank.values().length];
        
        // Count ranks
        for (int i = 0; i < 52; i++) {
            if ((cardBits & (1L << i)) != 0) {
                Card card = Card.fromBits(i);
                rankCounts[card.getRank().ordinal()]++;
            }
        }
        
        // Find pairs in descending order
        List<Card.Rank> pairRanks = new ArrayList<>();
        for (int i = rankCounts.length - 1; i >= 0; i--) {
            if (rankCounts[i] == 2) {
                pairRanks.add(Card.Rank.values()[i]);
            }
        }
        
        if (pairRanks.size() != 2) {
            throw new IllegalArgumentException("No two pair found in bit mask");
        }
        
        // Add pairs in descending order
        result.addAll(pairRanks);
        
        // Add kicker
        for (int i = rankCounts.length - 1; i >= 0; i--) {
            if (rankCounts[i] == 1) {
                result.add(Card.Rank.values()[i]);
                break;
            }
        }
        
        return result;
    }

    private static List<Card.Rank> getOnePairRanks(long cardBits) {
        if (cardBits == 0) {
            throw new IllegalArgumentException("No cards in bit mask");
        }
    
        List<Card.Rank> result = new ArrayList<>();
        int[] rankCounts = new int[Card.Rank.values().length];
        
        // Count ranks
        for (int i = 0; i < 52; i++) {
            if ((cardBits & (1L << i)) != 0) {
                Card card = Card.fromBits(i);
                rankCounts[card.getRank().ordinal()]++;
            }
        }
        
        // Find pair rank (search from high to low)
        Card.Rank pairRank = null;
        for (int i = rankCounts.length - 1; i >= 0; i--) {
            if (rankCounts[i] == 2) {
                pairRank = Card.Rank.values()[i];
                break;
            }
        }
        
        if (pairRank == null) {
            throw new IllegalArgumentException("No pair found in bit mask");
        }
        
        // Add pair rank first
        result.add(pairRank);
        
        // Add remaining kickers in descending order
        for (int i = rankCounts.length - 1; i >= 0; i--) {
            if (rankCounts[i] == 1) {
                result.add(Card.Rank.values()[i]);
            }
        }
        
        return result;
    }

    private static List<Card.Rank> getHighCardRanks(long cardBits) {
        if (cardBits == 0) {
            throw new IllegalArgumentException("No cards in bit mask");
        }
        
        if (Card.countCards(cardBits) != 5) {
            throw new IllegalArgumentException("High card hand must have exactly 5 cards");
        }
        
        // getHighestRanks already sorts in descending order
        List<Card.Rank> ranks = getHighestRanks(cardBits);
        
        // Verify no pairs or better
        Set<Card.Rank> uniqueRanks = new HashSet<>(ranks);
        if (uniqueRanks.size() != 5) {
            throw new IllegalArgumentException("Cards contain pairs or better");
        }
        
        return ranks;
    }
}