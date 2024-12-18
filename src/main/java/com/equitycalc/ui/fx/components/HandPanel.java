package com.equitycalc.ui.fx.components;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Line;

import com.equitycalc.model.Card;
import com.equitycalc.ui.fx.util.CardRenderer;

public class HandPanel extends Pane {
    // Update dimensions
    private static final double CARD_WIDTH = 80;
    private static final double CARD_HEIGHT = 110;
    private static final double PANEL_WIDTH = 135;
    private static final double PANEL_HEIGHT = 110;
    private static final double CARD_SPACING = PANEL_WIDTH - CARD_WIDTH * 2; // Calculated overlap
    
    // Update colors
    private static final Color EMPTY_CARD_BG = Color.web("#3F3F46");
    private static final Color EMPTY_CARD_BORDER = Color.web("#FFFFFF", 0.4);
    private static final double CORNER_RADIUS = 12;
    
    private Card card1;
    private Card card2;
    
    public HandPanel() {
        setPrefSize(PANEL_WIDTH, PANEL_HEIGHT);
        setMinSize(PANEL_WIDTH, PANEL_HEIGHT);
        
        drawEmptyState();
    }
    
    private void drawEmptyState() {
        getChildren().clear();
        
        // Draw second card (back)
        Rectangle card2Rect = createEmptyCard(CARD_WIDTH + CARD_SPACING, 0);
        Pane plus2 = createPlusSign(CARD_WIDTH + CARD_SPACING, 0);
        
        // Draw first card (front)
        Rectangle card1Rect = createEmptyCard(0, 0);
        Pane plus1 = createPlusSign(0, 0);
        
        // Add in correct order
        getChildren().addAll(card2Rect, plus2, card1Rect, plus1);
    }
    
    private Rectangle createEmptyCard(double x, double y) {
        Rectangle card = new Rectangle(x, y, CARD_WIDTH, CARD_HEIGHT);
        card.setFill(EMPTY_CARD_BG);
        card.setArcWidth(CORNER_RADIUS);
        card.setArcHeight(CORNER_RADIUS);
        card.setStroke(EMPTY_CARD_BORDER);
        card.getStrokeDashArray().addAll(4.0, 4.0);
        card.setStrokeWidth(1);
        return card;
    }
    
    private Pane createPlusSign(double cardX, double cardY) {
        Pane plusGroup = new Pane();
        
        // Create horizontal line
        Line horizontal = new Line();
        horizontal.setStartX(0);
        horizontal.setEndX(20);
        horizontal.setStartY(10);
        horizontal.setEndY(10);
        
        // Create vertical line
        Line vertical = new Line();
        vertical.setStartX(10);
        vertical.setEndX(10);
        vertical.setStartY(0);
        vertical.setEndY(20);
        
        // Set stroke properties for both lines
        horizontal.setStroke(EMPTY_CARD_BORDER);
        vertical.setStroke(EMPTY_CARD_BORDER);
        horizontal.setStrokeWidth(1);
        vertical.setStrokeWidth(1);
        
        // Add lines to group
        plusGroup.getChildren().addAll(horizontal, vertical);
        
        // Position the plus in the center of the card
        plusGroup.setLayoutX(cardX + (CARD_WIDTH - 20) / 2);
        plusGroup.setLayoutY(cardY + (CARD_HEIGHT - 20) / 2);
        
        return plusGroup;
    }
    
    public void setCards(Card c1, Card c2) {
        this.card1 = c1;
        this.card2 = c2;
        
        if (c1 == null && c2 == null) {
            drawEmptyState();
        } else {
            drawCards();
        }
    }
    
    private void drawCards() {
        getChildren().clear();
        if (card2 != null) {
            Pane card2Pane = CardRenderer.createCard(card2, CARD_WIDTH + CARD_SPACING, 0);
            getChildren().add(card2Pane);
        }
        if (card1 != null) {
            Pane card1Pane = CardRenderer.createCard(card1, 0, 0);
            getChildren().add(card1Pane);
        }
    }
}