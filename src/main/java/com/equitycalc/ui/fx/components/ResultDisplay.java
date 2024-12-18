package com.equitycalc.ui.fx.components;

import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.Font;

public class ResultDisplay extends Region {
    private static final double DEFAULT_WIDTH = 187;
    private static final double DEFAULT_HEIGHT = 40;
    private static final double CORNER_RADIUS = 6;
    
    public enum State {
        NULL(Color.web("#52525B"), "?"),
        CALL(Color.web("#22C55E"), "CALL"),
        BET(Color.web("#F59E0B"), "BET"),
        RAISE(Color.web("#EF4444"), "RAISE"),
        FOLD(Color.web("#DC2626"), "FOLD");
        
        final Color color;
        final String defaultText;
        
        State(Color color, String defaultText) {
            this.color = color;
            this.defaultText = defaultText;
        }
    }
    
    private final Rectangle background;
    private final Text label;
    private State currentState;
    private String amount;
    
    public ResultDisplay() {
        setPrefSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setMinSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        setMaxSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        
        background = new Rectangle(DEFAULT_WIDTH, DEFAULT_HEIGHT);
        background.setArcWidth(CORNER_RADIUS * 2);
        background.setArcHeight(CORNER_RADIUS * 2);
        
        label = new Text();
        label.setFont(Font.font("Inter", 14));
        label.setFill(Color.WHITE);
        
        getChildren().addAll(background, label);
        
        setState(State.NULL);
    }
    
    public void setState(State state) {
        setState(state, null);
    }
    
    public void setState(State state, String amount) {
        this.currentState = state;
        this.amount = amount;
        
        background.setFill(state.color);
        
        String displayText = state.defaultText;
        if (amount != null) {
            switch (state) {
                case BET -> displayText = "BET: " + amount;
                case RAISE -> displayText = "RAISE: " + amount;
            }
        }
        
        label.setText(displayText);
        
        // Center the text
        double textWidth = label.getLayoutBounds().getWidth();
        double textHeight = label.getLayoutBounds().getHeight();
        
        label.setLayoutX((DEFAULT_WIDTH - textWidth) / 2);
        label.setLayoutY((DEFAULT_HEIGHT + textHeight) / 2 - textHeight * 0.2);
    }
}