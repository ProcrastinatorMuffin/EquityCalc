package com.equitycalc.simulation;

import com.equitycalc.model.Card;
import java.util.List;


public class HandRanking implements Comparable<HandRanking> {
    public enum Type {
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

    public final Type type;
    public final List<Card.Rank> tiebreakers;
    
    public HandRanking(Type type, List<Card.Rank> tiebreakers) {
        this.type = type;
        this.tiebreakers = tiebreakers;
    }
    
    @Override
    public int compareTo(HandRanking other) {
        long startTime = System.nanoTime();
        int typeComparison = type.compareTo(other.type);
        if (typeComparison != 0) {
            PerformanceLogger.logOperation("HandCompare", startTime);
            return typeComparison;
        }
        
        // Compare tiebreakers in order
        for (int i = 0; i < tiebreakers.size() && i < other.tiebreakers.size(); i++) {
            int rankComparison = tiebreakers.get(i).compareTo(other.tiebreakers.get(i));
            if (rankComparison != 0) {
                PerformanceLogger.logOperation("HandCompare", startTime);
                return rankComparison;
            }
        }
        PerformanceLogger.logOperation("HandCompare", startTime);
        return 0;
    }
}