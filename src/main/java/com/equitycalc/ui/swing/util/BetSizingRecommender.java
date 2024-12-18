package com.equitycalc.ui.swing.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BetSizingRecommender {
    private static final double[] POT_FRACTIONS = {0.25, 0.33, 0.5, 0.66, 0.75, 1.0, 1.5, 2.0, 3.0};
    
    public static List<Double> getProfitableBetSizes(double equity) {
        return Arrays.stream(POT_FRACTIONS)
            .filter(potFraction -> isProfitableToBet(equity, potFraction))
            .boxed()
            .collect(Collectors.toList());
    }
    
    public static List<Double> getProfitableCallSizes(double equity) {
        return Arrays.stream(POT_FRACTIONS)
            .filter(potFraction -> isProfitableToCall(equity, potFraction))
            .boxed()
            .collect(Collectors.toList());
    }
    
    private static boolean isProfitableToBet(double equity, double potFraction) {
        // For betting: Required equity = size/(size + 2*pot)
        double requiredEquity = potFraction / (potFraction + 2.0);
        return equity >= requiredEquity;
    }
    
    private static boolean isProfitableToCall(double equity, double potFraction) {
        // For calling: Required equity = size/(size + pot)
        double requiredEquity = potFraction / (potFraction + 1.0);
        return equity >= requiredEquity;
    }
}
