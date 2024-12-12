package com.equitycalc.simulation;

import com.equitycalc.model.Card;
import com.equitycalc.model.Player;
import java.util.*;

public class SimulationConfig {
    private static final int MAX_BOARD_CARDS = 5;
    private static final int DEFAULT_SIMULATIONS = 200000;
    
    private final List<Player> knownPlayers;
    private final int numRandomPlayers;
    private final List<Card> boardCards;
    private final List<Card> deadCards;
    private final int numSimulations;
    private Street cachedStreet;
    private static final boolean DEBUG_VALIDATION = Boolean.getBoolean("debug.validation");

    public enum Street {
        PREFLOP(0), FLOP(3), TURN(4), RIVER(5);
        
        private final int numCards;
        
        Street(int numCards) {
            this.numCards = numCards;
        }
        
        public int getNumCards() {
            return numCards;
        }
    }

    private SimulationConfig(Builder builder) {
        this.knownPlayers = Collections.unmodifiableList(new ArrayList<>(builder.knownPlayers));
        this.numRandomPlayers = builder.numRandomPlayers;
        this.boardCards = Collections.unmodifiableList(new ArrayList<>(builder.boardCards));
        this.deadCards = Collections.unmodifiableList(new ArrayList<>(builder.deadCards));
        this.numSimulations = builder.numSimulations;
        validate();
    }

    public static class Builder {
        private List<Player> knownPlayers = new ArrayList<>();
        private int numRandomPlayers = 0;
        private List<Card> boardCards = new ArrayList<>();
        private List<Card> deadCards = new ArrayList<>();
        private int numSimulations = DEFAULT_SIMULATIONS;

        public Builder withKnownPlayers(List<Player> players) {
            this.knownPlayers = new ArrayList<>(players);
            return this;
        }

        public Builder withRandomPlayers(int num) {
            this.numRandomPlayers = num;
            return this;
        }

        public Builder withBoardCards(List<Card> cards) {
            this.boardCards = new ArrayList<>(cards);
            return this;
        }

        public Builder withDeadCards(List<Card> cards) {
            this.deadCards = new ArrayList<>(cards);
            return this;
        }

        public Builder withNumSimulations(int num) {
            this.numSimulations = num;
            return this;
        }

        public SimulationConfig build() {
            return new SimulationConfig(this);
        }

        public Builder withStreet(Street street, List<Card> cards) {
            if (cards.size() != street.getNumCards()) {
                throw new IllegalArgumentException(
                    String.format("Street %s requires exactly %d cards", 
                        street, street.getNumCards())
                );
            }
            this.boardCards = new ArrayList<>(cards);
            return this;
        }
    
        // Convenience method for preflop
        public Builder preflop() {
            this.boardCards.clear();
            return this;
        }
    
        // Convenience method for flop
        public Builder flop(Card c1, Card c2, Card c3) {
            this.boardCards = Arrays.asList(c1, c2, c3);
            return this;
        }
    
        // Convenience method for turn
        public Builder turn(Card c1, Card c2, Card c3, Card c4) {
            this.boardCards = Arrays.asList(c1, c2, c3, c4);
            return this;
        }
    
        // Convenience method for river
        public Builder river(Card c1, Card c2, Card c3, Card c4, Card c5) {
            this.boardCards = Arrays.asList(c1, c2, c3, c4, c5);
            return this;
        }
    }

    private void validate() {
        if (knownPlayers.isEmpty()) {
            throw new IllegalArgumentException("At least one known player is required");
        }

        if (numRandomPlayers < 0) {
            throw new IllegalArgumentException("Number of random players cannot be negative");
        }

        if (boardCards.size() > MAX_BOARD_CARDS) {
            throw new IllegalArgumentException("Maximum " + MAX_BOARD_CARDS + " board cards allowed");
        }

        if (numSimulations <= 0) {
            throw new IllegalArgumentException("Number of simulations must be positive");
        }

        validateStreetConsistency();
        validateNoDuplicateCards();
        validateDeadCardsNotOnBoard();
    }

    private void validateStreetConsistency() {
        int boardSize = boardCards.size();
        if (boardSize != 0 && boardSize != 3 && boardSize != 4 && boardSize != 5) {
            throw new IllegalArgumentException(
                "Board must have 0 (preflop), 3 (flop), 4 (turn), or 5 (river) cards"
            );
        }
    }

    private void validateDeadCardsNotOnBoard() {
        if (boardCards.stream().anyMatch(deadCards::contains)) {
            throw new IllegalArgumentException("Dead card cannot be on the board");
        }
    }

    private void validateNoDuplicateCards() {
        Set<Card> allCards = new HashSet<>();
        Set<Card> duplicates = new HashSet<>();
        
        if (DEBUG_VALIDATION) {
            System.out.println("\nCard Validation Starting");
        }
        
        // Validate board cards
        for (Card card : boardCards) {
            assert card != null : "Null board card detected";
            if (!allCards.add(card)) {
                duplicates.add(card);
                if (DEBUG_VALIDATION) {
                    System.out.println("Duplicate board card: " + card);
                }
            }
        }
    
        // Validate dead cards
        for (Card card : deadCards) {
            assert card != null : "Null dead card detected";
            if (!allCards.add(card)) {
                duplicates.add(card);
                if (DEBUG_VALIDATION) {
                    System.out.println("Duplicate dead card: " + card);
                }
            }
        }
    
        // Validate player cards
        for (Player player : knownPlayers) {
            assert player != null : "Null player detected";
            for (Card card : player.getHoleCards()) {
                assert card != null : "Null hole card detected";
                if (!allCards.add(card)) {
                    duplicates.add(card);
                    if (DEBUG_VALIDATION) {
                        System.out.println("Duplicate hole card: " + card);
                    }
                }
            }
        }
    
        if (!duplicates.isEmpty()) {
            throw new IllegalArgumentException("Duplicate cards detected: " + duplicates);
        }
    }

    public Street getCurrentStreet() {
        if (cachedStreet == null) {
            cachedStreet = switch (boardCards.size()) {
                case 0 -> Street.PREFLOP;
                case 3 -> Street.FLOP;
                case 4 -> Street.TURN;
                case 5 -> Street.RIVER;
                default -> throw new IllegalStateException("Invalid number of board cards: " + boardCards.size());
            };
        }
        return cachedStreet;
    }

    // Getters (no setters to maintain immutability)
    public List<Player> getKnownPlayers() {
        return knownPlayers;
    }

    public int getNumRandomPlayers() {
        return numRandomPlayers;
    }

    public List<Card> getBoardCards() {
        return boardCards;
    }

    public List<Card> getDeadCards() {
        return deadCards;
    }

    public int getNumSimulations() {
        return numSimulations;
    }

    public static Builder builder() {
        return new Builder();
    }
}