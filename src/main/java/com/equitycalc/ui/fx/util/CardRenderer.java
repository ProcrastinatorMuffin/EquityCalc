package com.equitycalc.ui.fx.util;

import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.paint.CycleMethod;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.effect.DropShadow;
import com.equitycalc.model.Card;

public class CardRenderer {
    private static final double CARD_WIDTH = 80;
    private static final double CARD_HEIGHT = 110;
    private static final double CORNER_RADIUS = 12;
    
    // Modern, muted color palette inspired by shadcn/ui
    private static final Color SPADES_BG = Color.web("#18181B");    // Zinc-900
    private static final Color HEARTS_BG = Color.web("#EF4444");    // Red-500
    private static final Color DIAMONDS_BG = Color.web("#3B82F6");  // Blue-500
    private static final Color CLUBS_BG = Color.web("#22C55E");     // Green-500

    private static final Color SPADES_BORDER = Color.web("#27272A");    
    private static final Color HEARTS_BORDER = Color.web("#FCA5A5");   
    private static final Color DIAMONDS_BORDER = Color.web("#93C5FD"); 
    private static final Color CLUBS_BORDER = Color.web("#86EFAC");    
    
    private static final Color CARD_OVERLAY = Color.web("#FFFFFF", 0.08); // Subtle highlight
    private static final Color TEXT_COLOR = Color.web("#FFFFFF", 0.87);   // Slightly soft white
    
    public static Pane createCard(Card card, double x, double y) {
        Pane cardPane = new Pane();
        cardPane.setLayoutX(x);
        cardPane.setLayoutY(y);
        
        if (card == null) {
            Rectangle emptyCard = new Rectangle(CARD_WIDTH, CARD_HEIGHT);
            emptyCard.setFill(Color.web("#27272A")); // Zinc-800
            emptyCard.setArcWidth(CORNER_RADIUS * 2);
            emptyCard.setArcHeight(CORNER_RADIUS * 2);
            // Add subtle border
            emptyCard.setStroke(Color.web("#FFFFFF", 0.1));
            emptyCard.setStrokeWidth(1);
            cardPane.getChildren().add(emptyCard);
            return cardPane;
        }
        
        // Base card shape
        Rectangle cardRect = new Rectangle(CARD_WIDTH, CARD_HEIGHT);
        cardRect.setArcWidth(CORNER_RADIUS * 2);
        cardRect.setArcHeight(CORNER_RADIUS * 2);
        
        Color baseColor = switch(card.getSuit().toString().toLowerCase()) {
            case "spades" -> SPADES_BG;
            case "hearts" -> HEARTS_BG;
            case "diamonds" -> DIAMONDS_BG;
            case "clubs" -> CLUBS_BG;
            default -> Color.web("#27272A");
        };
        
        Color borderColor = switch(card.getSuit().toString().toLowerCase()) {
            case "spades" -> SPADES_BORDER;
            case "hearts" -> HEARTS_BORDER;
            case "diamonds" -> DIAMONDS_BORDER;
            case "clubs" -> CLUBS_BORDER;
            default -> Color.web("#FFFFFF", 0.1);
        };
        
        cardRect.setFill(baseColor);
        
        // Inner border rectangle
        Rectangle innerBorder = new Rectangle(
            CARD_WIDTH - 2, 
            CARD_HEIGHT - 2
        );
        innerBorder.setX(1);
        innerBorder.setY(1);
        innerBorder.setArcWidth(CORNER_RADIUS * 2);
        innerBorder.setArcHeight(CORNER_RADIUS * 2);
        innerBorder.setFill(Color.TRANSPARENT);
        innerBorder.setStroke(borderColor);
        innerBorder.setStrokeWidth(1);
        innerBorder.setOpacity(0.3);
        
        // Add rank and suit texts
        Text rankText = new Text(getRankDisplay(card));
        rankText.setFont(Font.font("Inter", FontWeight.MEDIUM, 42));
        rankText.setFill(TEXT_COLOR);
        rankText.setX((CARD_WIDTH - rankText.getBoundsInLocal().getWidth()) / 2);
        rankText.setY(CARD_HEIGHT * 0.4);
        
        Text suitText = new Text(getSuitSymbol(card));
        suitText.setFont(Font.font("Inter", FontWeight.MEDIUM, 28));
        suitText.setFill(TEXT_COLOR);
        suitText.setX((CARD_WIDTH - suitText.getBoundsInLocal().getWidth()) / 2);
        suitText.setY(CARD_HEIGHT * 0.75);
        
        // Add subtle shadow
        cardPane.setEffect(new DropShadow(
            10, 0, 2, Color.web("#000000", 0.15)
        ));
        
        cardPane.getChildren().addAll(cardRect, innerBorder, rankText, suitText);
        
        return cardPane;
    }
    
    private static String getRankDisplay(Card card) {
        return switch(card.getRank().toString().toLowerCase()) {
            case "ace" -> "A";
            case "king" -> "K";
            case "queen" -> "Q"; 
            case "jack" -> "J";
            case "ten" -> "T";
            case "nine" -> "9";
            case "eight" -> "8";
            case "seven" -> "7";
            case "six" -> "6";
            case "five" -> "5";
            case "four" -> "4";
            case "three" -> "3";
            case "two" -> "2";
            default -> card.getRank().toString();
        };
    }
    
    private static String getSuitSymbol(Card card) {
        return switch(card.getSuit().toString().toLowerCase()) {
            case "spades" -> "♠";
            case "hearts" -> "♥";
            case "diamonds" -> "♦";
            case "clubs" -> "♣";
            default -> "";
        };
    }
}