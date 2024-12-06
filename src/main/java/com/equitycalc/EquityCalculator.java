package com.equitycalc;

import com.equitycalc.model.Card;
import com.equitycalc.model.Deck;
import com.equitycalc.model.Hand;
import com.equitycalc.model.Player;

import java.util.*;

// TODO: Implement hand evaluation logic
// TODO: Add Monte Carlo simulation for complex scenarios
// TODO: Add range-based calculations

public class EquityCalculator {
    private static final int BOARD_SIZE = 5;
    
    public void calculateEquity(List<Player> players, List<Card> communityCards) {
        validateInput(players, communityCards);
        
        Map<Player, Integer> wins = new HashMap<>();
        Map<Player, Integer> ties = new HashMap<>();
        players.forEach(p -> {
            wins.put(p, 0);
            ties.put(p, 0);
        });

        int totalHands = calculateAllPossibleBoards(players, communityCards, wins, ties);
        
        // Calculate final probabilities
        for (Player player : players) {
            player.setWinProbability((double) wins.get(player) / totalHands);
            player.setSplitProbability((double) ties.get(player) / totalHands);
            player.setLossProbability(1.0 - player.getWinProbability() - player.getSplitProbability());
        }
    }

    private void validateInput(List<Player> players, List<Card> communityCards) {
        if (players == null || players.size() < 2 || players.size() > 9) {
            throw new IllegalArgumentException("Must have between 2 and 9 players");
        }
        if (communityCards != null && communityCards.size() > BOARD_SIZE) {
            throw new IllegalArgumentException("Cannot have more than 5 community cards");
        }
        
        // Check for duplicate cards
        Set<Card> allCards = new HashSet<>();
        for (Player player : players) {
            for (Card card : player.getHoleCards()) {
                if (!allCards.add(card)) {
                    throw new IllegalArgumentException("Duplicate card detected: " + card);
                }
            }
        }
        if (communityCards != null) {
            for (Card card : communityCards) {
                if (!allCards.add(card)) {
                    throw new IllegalArgumentException("Duplicate card detected: " + card);
                }
            }
        }
    }

    private int calculateAllPossibleBoards(List<Player> players, List<Card> communityCards,
                                         Map<Player, Integer> wins, Map<Player, Integer> ties) {
        Set<Card> usedCards = new HashSet<>();
        players.forEach(p -> usedCards.addAll(p.getHoleCards()));
        if (communityCards != null) {
            usedCards.addAll(communityCards);
        }

        List<Card> remainingCards = new ArrayList<>();
        Deck deck = new Deck();
        deck.shuffle();
        for (Card card : deck.dealCards(52)) {
            if (!usedCards.contains(card)) {
                remainingCards.add(card);
            }
        }

        int cardsNeeded = BOARD_SIZE - (communityCards != null ? communityCards.size() : 0);
        return generateCombinations(remainingCards, cardsNeeded, communityCards, players, wins, ties);
    }

    private int generateCombinations(List<Card> remainingCards, int cardsNeeded, 
                                   List<Card> communityCards, List<Player> players,
                                   Map<Player, Integer> wins, Map<Player, Integer> ties) {
        if (cardsNeeded == 0) {
            evaluateHands(communityCards, players, wins, ties);
            return 1;
        }

        int combinations = 0;
        for (int i = 0; i <= remainingCards.size() - cardsNeeded; i++) {
            List<Card> newCommunityCards = new ArrayList<>(
                communityCards != null ? communityCards : Collections.emptyList());
            newCommunityCards.add(remainingCards.get(i));
            
            List<Card> newRemainingCards = new ArrayList<>(remainingCards.subList(i + 1, remainingCards.size()));
            combinations += generateCombinations(newRemainingCards, cardsNeeded - 1, 
                                              newCommunityCards, players, wins, ties);
        }
        return combinations;
    }

    private void evaluateHands(List<Card> communityCards, List<Player> players,
                             Map<Player, Integer> wins, Map<Player, Integer> ties) {
        // TODO: Implement hand evaluation logic
        // For now, using placeholder random winner selection
        Random rand = new Random();
        int winner = rand.nextInt(players.size());
        
        if (rand.nextDouble() < 0.1) { // 10% chance of split pot
            players.forEach(p -> ties.merge(p, 1, Integer::sum));
        } else {
            wins.merge(players.get(winner), 1, Integer::sum);
        }
    }

    public void updateEquityInRealTime(List<Player> players, List<Card> communityCards) {
        // For real-time updates, we'll use a smaller sample size
        calculateEquity(players, communityCards);
    }
}