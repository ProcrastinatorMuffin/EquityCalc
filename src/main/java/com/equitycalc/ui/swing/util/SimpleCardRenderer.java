package com.equitycalc.ui.swing.util;

import com.equitycalc.model.Card;
import com.equitycalc.ui.swing.panel.HandPanel;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RenderingHints;



public class SimpleCardRenderer {
    private static final int CARD_WIDTH = 70;
    private static final int CARD_HEIGHT = 95;
    private static final int CORNER_RADIUS = 20;
    private static final Font RANK_FONT = new Font("Geist-Semibold", Font.PLAIN, 42);
    private static final Font SUIT_FONT = new Font("Geist-Semibold", Font.PLAIN, 28);
    
    // Solid muted macOS colors
    private static final Color SPADES_BG = new Color(40, 40, 40);    // Dark gray
    private static final Color HEARTS_BG = new Color(200, 55, 45);   // Muted red
    private static final Color DIAMONDS_BG = new Color(20, 100, 200);// Muted blue
    private static final Color CLUBS_BG = new Color(40, 140, 65);    // Muted green
    
    private static final Color TEXT_COLOR = new Color(255, 255, 255);
    private static final float BORDER_WIDTH = 1.5f;
    
        public static void paintCard(Graphics2D g2, Card card, int x, int y) {
        // Store original composite
        Composite originalComposite = g2.getComposite();
        float alpha = 1.0f;
        if (originalComposite instanceof AlphaComposite) {
            alpha = ((AlphaComposite) originalComposite).getAlpha();
        }
    
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        
        if (card == null) {
            Color emptyColor = new Color(60, 60, 60);
            g2.setColor(new Color(emptyColor.getRed(), emptyColor.getGreen(), 
                                 emptyColor.getBlue(), (int)(255 * alpha)));
            g2.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, CORNER_RADIUS, CORNER_RADIUS);
            g2.setComposite(originalComposite);
            return;
        }
        
        Color bgColor = switch(card.getSuit().toString().toLowerCase()) {
            case "spades" -> SPADES_BG;
            case "hearts" -> HEARTS_BG;
            case "diamonds" -> DIAMONDS_BG;
            case "clubs" -> CLUBS_BG;
            default -> Color.GRAY;
        };
        
        // Create gradient with alpha
        Color startColor = new Color(bgColor.getRed(), bgColor.getGreen(), 
                                   bgColor.getBlue(), (int)(255 * alpha));
        Color endColor = new Color(
            (int)(bgColor.getRed() * 0.85),
            (int)(bgColor.getGreen() * 0.85),
            (int)(bgColor.getBlue() * 0.85),
            (int)(255 * alpha)
        );
        
        GradientPaint gradient = new GradientPaint(x, y, startColor, x, y + CARD_HEIGHT, endColor);
        g2.setPaint(gradient);
        g2.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, CORNER_RADIUS, CORNER_RADIUS);
        
        // Draw border for suited cards
        Card otherCard = HandPanel.getCurrentOtherCard(card);
        if (otherCard != null && card.getSuit() == otherCard.getSuit()) {
            g2.setStroke(new BasicStroke(BORDER_WIDTH));
            Color borderColor = new Color(
                Math.min(255, bgColor.getRed() + 40),
                Math.min(255, bgColor.getGreen() + 40),
                Math.min(255, bgColor.getBlue() + 40),
                (int)(255 * alpha)
            );
            g2.setColor(borderColor);
            g2.drawRoundRect(x, y, CARD_WIDTH-1, CARD_HEIGHT-1, CORNER_RADIUS, CORNER_RADIUS);
        }
    
        // Draw text with alpha
        Color textColorWithAlpha = new Color(TEXT_COLOR.getRed(), TEXT_COLOR.getGreen(), 
                                           TEXT_COLOR.getBlue(), (int)(255 * alpha));
        g2.setColor(textColorWithAlpha);
    
        // Draw rank
        g2.setFont(RANK_FONT);
        String rank = switch(card.getRank().toString().toLowerCase()) {
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
        FontMetrics fm = g2.getFontMetrics();
        int rankWidth = fm.stringWidth(rank);
        g2.drawString(rank, x + (CARD_WIDTH - rankWidth) / 2, 
                     y + (CARD_HEIGHT/3) + (fm.getAscent()/2));
        
        // Draw suit symbol
        g2.setFont(SUIT_FONT);
        String suitSymbol = switch(card.getSuit().toString().toLowerCase()) {
            case "spades" -> "♠";
            case "hearts" -> "♥";
            case "diamonds" -> "♦"; 
            case "clubs" -> "♣";
            default -> "";
        };
        FontMetrics sfm = g2.getFontMetrics();
        int suitWidth = sfm.stringWidth(suitSymbol);
        g2.drawString(suitSymbol, x + (CARD_WIDTH - suitWidth) / 2,
                     y + (CARD_HEIGHT*18/24) + (sfm.getAscent()/2));
                     
        // Restore original composite
        g2.setComposite(originalComposite);
    }
}
