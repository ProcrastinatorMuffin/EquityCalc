package com.equitycalc.ui.fx.controllers;

import javafx.fxml.FXML;
import com.equitycalc.ui.fx.components.RangeMatrix;
import com.equitycalc.ui.fx.components.MatrixTile;
import java.util.HashMap;
import java.util.Map;

public class MatrixTestController {
    @FXML
    private RangeMatrix rangeMatrix;

    @FXML
    private void initialize() {
        // Switch to HEATMAP mode
        rangeMatrix.setMode(MatrixTile.Mode.HEATMAP);

        // Create sample equity and weight data
        Map<String, Double> equities = new HashMap<>();
        Map<String, Double> weights = new HashMap<>();

        // Premium pairs
        equities.put("AA", 85.0);
        equities.put("KK", 82.0);
        equities.put("QQ", 80.0);
        weights.put("AA", 1.0);
        weights.put("KK", 1.0);
        weights.put("QQ", 1.0);

        // Strong broadways
        equities.put("AKs", 67.0);
        equities.put("AQs", 66.0);
        equities.put("AKo", 65.0);
        weights.put("AKs", 0.8);
        weights.put("AQs", 0.7);
        weights.put("AKo", 0.6);

        // Medium strength hands
        equities.put("TT", 60.0);
        equities.put("JTs", 55.0);
        weights.put("TT", 0.5);
        weights.put("JTs", 0.4);

        // Mark these hands as selected
        Map<String, Boolean> selections = new HashMap<>();
        equities.forEach((hand, equity) -> selections.put(hand, true));

        // Update matrix
        rangeMatrix.setSelectedHands(selections);
        rangeMatrix.updateHeatmap(equities, weights);
    }
}