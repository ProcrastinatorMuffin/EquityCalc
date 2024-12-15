package com.equitycalc.model;

import java.util.*;

public class Range {
    private final Set<HandRange> hands;
    
    public Range() {
        this.hands = new HashSet<>();
    }
    
    // Range types
    public static class HandRange {
        private final Card.Rank first;
        private final Card.Rank second;
        private final boolean suited;
        
        public HandRange(Card.Rank first, Card.Rank second, boolean suited) {
            // Always store higher rank first for consistency
            if (first.ordinal() < second.ordinal()) {
                this.first = second;
                this.second = first;
            } else {
                this.first = first;
                this.second = second;
            }
            // Suited only matters if ranks are different
            this.suited = suited && (first != second);
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof HandRange)) return false;
            HandRange other = (HandRange) o;
            return first == other.first && 
                   second == other.second && 
                   suited == other.suited;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(first, second, suited);
        }
    }
    
    // Add specific hand to range
    public void addHand(Card.Rank first, Card.Rank second, boolean suited) {
        hands.add(new HandRange(first, second, suited));
    }
    
    // Add all pairs above and including the specified rank
    public void addPairRange(Card.Rank minRank) {
        for (Card.Rank rank : Card.Rank.values()) {
            if (rank.ordinal() >= minRank.ordinal()) {
                addHand(rank, rank, false);
            }
        }
    }
    
    // Add all suited hands above and including the specified ranks
    public void addSuitedRange(Card.Rank first, Card.Rank second) {
        for (Card.Rank r1 : Card.Rank.values()) {
            for (Card.Rank r2 : Card.Rank.values()) {
                if (r1.ordinal() >= first.ordinal() && 
                    r2.ordinal() >= second.ordinal() && 
                    r1 != r2) {
                    addHand(r1, r2, true);
                }
            }
        }
    }
    
    // Add all offsuit hands above and including the specified ranks
    public void addOffsuitRange(Card.Rank first, Card.Rank second) {
        for (Card.Rank r1 : Card.Rank.values()) {
            for (Card.Rank r2 : Card.Rank.values()) {
                if (r1.ordinal() >= first.ordinal() && 
                    r2.ordinal() >= second.ordinal() && 
                    r1 != r2) {
                    addHand(r1, r2, false);
                }
            }
        }
    }
    
    // Check if a specific hand is in range
    public boolean containsHand(Card.Rank first, Card.Rank second, boolean suited) {
        return hands.contains(new HandRange(first, second, suited));
    }
    
    // Get all possible specific hands in this range
    public List<Card[]> getPossibleHands() {
        List<Card[]> result = new ArrayList<>();
        for (HandRange range : hands) {
            if (range.suited) {
                // Generate all suited combinations
                for (Card.Suit suit : Card.Suit.values()) {
                    result.add(new Card[]{
                        new Card(range.first, suit),
                        new Card(range.second, suit)
                    });
                }
            } else if (range.first == range.second) {
                // Generate all pair combinations
                for (Card.Suit s1 : Card.Suit.values()) {
                    for (Card.Suit s2 : Card.Suit.values()) {
                        if (s1.ordinal() < s2.ordinal()) {
                            result.add(new Card[]{
                                new Card(range.first, s1),
                                new Card(range.first, s2)
                            });
                        }
                    }
                }
            } else {
                // Generate all offsuit combinations
                for (Card.Suit s1 : Card.Suit.values()) {
                    for (Card.Suit s2 : Card.Suit.values()) {
                        if (s1 != s2) {
                            result.add(new Card[]{
                                new Card(range.first, s1),
                                new Card(range.second, s2)
                            });
                        }
                    }
                }
            }
        }
        return result;
    }
    
    // Parse range from string notation (e.g., "AKs+,QQ+,AJo+")
    public static Range parseRange(String notation) {
        Range range = new Range();
        String[] parts = notation.split(",");
        
        for (String part : parts) {
            part = part.trim().toUpperCase();
            boolean hasPlus = part.endsWith("+");
            if (hasPlus) {
                part = part.substring(0, part.length() - 1);
            }
            
            if (part.length() < 2) continue;
            
            Card.Rank first = Card.Rank.fromSymbol(part.charAt(0));
            Card.Rank second = Card.Rank.fromSymbol(part.charAt(1));
            boolean suited = part.endsWith("s");
            
            if (hasPlus) {
                if (first == second) {
                    range.addPairRange(first);
                } else if (suited) {
                    range.addSuitedRange(first, second);
                } else {
                    range.addOffsuitRange(first, second);
                }
            } else {
                range.addHand(first, second, suited);
            }
        }
        
        return range;
    }
}