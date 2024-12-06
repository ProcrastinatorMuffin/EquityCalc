package com.equitycalc.simulation;

import com.equitycalc.model.Card;
import com.equitycalc.model.PokerHand;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class HandEvaluator {
    public static HandRanking evaluateHand(PokerHand hand) {
        long startTime = System.nanoTime();
        List<Card> cards = hand.getCards();
        List<List<Card>> combinations = getCombinations(cards, 5);
        
        HandRanking result = combinations.stream()
            .map(HandEvaluator::evaluateFiveCardHand)
            .max(HandRanking::compareTo)
            .orElseThrow();
            
        PerformanceLogger.logOperation("EvaluateHand", startTime);
        return result;
    }

    private static HandRanking evaluateFiveCardHand(List<Card> cards) {
        long startTime = System.nanoTime();
        Map<Card.Rank, Integer> rankCount = getRankCount(cards);
        
        HandRanking result;
        if (isStraightFlush(cards)) {
            result = new HandRanking(HandType.STRAIGHT_FLUSH, getStraightHighCard(cards));
        } else if (rankCount.containsValue(4)) {
            result = new HandRanking(HandType.FOUR_OF_A_KIND, getFourOfAKindRank(cards));
        } else if (rankCount.containsValue(3) && rankCount.containsValue(2)) {
            result = new HandRanking(HandType.FULL_HOUSE, getFullHouseRank(cards));
        } else if (isFlush(cards)) {
            result = new HandRanking(HandType.FLUSH, getHighCard(cards));
        } else if (isStraight(cards)) {
            result = new HandRanking(HandType.STRAIGHT, getStraightHighCard(cards));
        } else if (rankCount.containsValue(3)) {
            result = new HandRanking(HandType.THREE_OF_A_KIND, getThreeOfAKindRank(cards));
        } else if (getRankCount(cards).values().stream().filter(count -> count == 2).count() == 2) {
            result = new HandRanking(HandType.TWO_PAIR, getTwoPairRanks(cards));
        } else if (rankCount.containsValue(2)) {
            result = new HandRanking(HandType.ONE_PAIR, getOnePairRank(cards));
        } else {
            result = new HandRanking(HandType.HIGH_CARD, getHighCard(cards));
        }
        
        PerformanceLogger.logOperation("EvaluateFiveCardHand", startTime);
        return result;
    }

    private static List<List<Card>> getCombinations(List<Card> cards, int r) {
        long startTime = System.nanoTime();
        List<List<Card>> combinations = new ArrayList<>();
        combinationsHelper(cards, r, 0, new ArrayList<>(), combinations);
        PerformanceLogger.logOperation("GetCombinations", startTime);
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