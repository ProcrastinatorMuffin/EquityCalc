package com.equitycalc.ui.fx.components;

import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.geometry.Insets;
import java.util.HashMap;
import java.util.Map;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;

public class RangeMatrix extends Region {
    private static final int MATRIX_SIZE = 13;
    private static final double SPACING = 1;
    private static final String[] RANKS = {"A", "K", "Q", "J", "T", "9", "8", "7", "6", "5", "4", "3", "2"};
    private static final double EDGE_THRESHOLD = 0.3;
    private static final double EXIT_THRESHOLD = 5; // pixels from edge
    private static final double MIN_OPACITY = 0.6; // Minimum opacity for displaced tiles
    
    private final GridPane gridPane;
    private final Map<String, MatrixTile> tiles;
    private MatrixTile.Mode currentMode;
    private MatrixTile currentMagnifiedTile = null;
    // Add flag to track if we're inside matrix bounds
    private boolean isInsideMatrix = false;
    
    public RangeMatrix() {
        this.tiles = new HashMap<>();
        this.currentMode = MatrixTile.Mode.EDIT;
        
        gridPane = new GridPane();
        gridPane.setHgap(0); // Remove spacing
        gridPane.setVgap(0);
        gridPane.setPadding(new Insets(0));
        
        initializeMatrix();
        getChildren().add(gridPane);
        
        setupMatrixInteractions();
    }
    
    private void initializeMatrix() {
        for (int i = 0; i < MATRIX_SIZE; i++) {
            for (int j = 0; j < MATRIX_SIZE; j++) {
                String handType = generateHandType(i, j);
                MatrixTile tile = new MatrixTile(handType, i, j, MATRIX_SIZE);
                tiles.put(handType, tile);
                gridPane.add(tile, j, i);
            }
        }
    }
    
    private String generateHandType(int row, int col) {
        String firstRank = RANKS[row];
        String secondRank = RANKS[col];
        
        if (row == col) {
            // Pocket pairs
            return firstRank + firstRank;
        } else if (row < col) {
            // Offsuit hands
            return firstRank + secondRank + "o";
        } else {
            // Suited hands
            return secondRank + firstRank + "s";
        }
    }
    
    public void setMode(MatrixTile.Mode mode) {
        this.currentMode = mode;
        tiles.values().forEach(tile -> tile.setMode(mode));
        // Update cursor when mode changes
        if (mode == MatrixTile.Mode.HEATMAP) {
            setCursor(Cursor.NONE);
        } else {
            setCursor(Cursor.DEFAULT);
        }
    }
    
    public void updateHeatmap(Map<String, Double> equities, Map<String, Double> weights) {
        tiles.forEach((handType, tile) -> {
            double equity = equities.getOrDefault(handType, 0.0);
            double weight = weights.getOrDefault(handType, 0.0);
            tile.setHeatmapValues(equity, weight);
        });
    }
    
    public Map<String, Boolean> getSelectedHands() {
        Map<String, Boolean> selected = new HashMap<>();
        tiles.forEach((handType, tile) -> 
            selected.put(handType, tile.isSelected())
        );
        return selected;
    }
    
    public void setSelectedHands(Map<String, Boolean> selections) {
        selections.forEach((handType, isSelected) -> {
            MatrixTile tile = tiles.get(handType);
            if (tile != null) {
                tile.setSelected(isSelected);
            }
        });
    }
    
    @Override
    protected void layoutChildren() {
        super.layoutChildren();
        double size = Math.min(getWidth(), getHeight());
        gridPane.setMaxSize(size, size);
        gridPane.setMinSize(size, size);
    }
    
    private void setupMatrixInteractions() {
        setOnMouseMoved(e -> {
            if (currentMode != MatrixTile.Mode.HEATMAP) return;

            Point2D mousePoint = new Point2D(e.getX(), e.getY());
            
            if (!getBoundsInLocal().contains(mousePoint)) {
                clearMagnification();
                setCursor(Cursor.DEFAULT);
                return;
            }

            MatrixTile hoveredTile = (MatrixTile) gridPane.getChildren().stream()
                .filter(node -> node.getBoundsInParent().contains(mousePoint))
                .findFirst()
                .orElse(null);

            if (hoveredTile != null) {
                if (hoveredTile != currentMagnifiedTile) {
                    clearMagnification();
                    currentMagnifiedTile = hoveredTile;
                    hoveredTile.setMagnified(true, mousePoint);
                }
                setCursor(Cursor.NONE);
            }
        });

        setOnMouseExited(e -> {
            clearMagnification();
            setCursor(Cursor.DEFAULT);
        });
    }

    private void clearMagnification() {
        if (currentMagnifiedTile != null) {
            currentMagnifiedTile.setMagnified(false, null);
            currentMagnifiedTile = null;
        }
    }
}