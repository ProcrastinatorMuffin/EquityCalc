package com.equitycalc.ui.panel;

import com.equitycalc.model.Card;
import com.equitycalc.ui.util.SimpleCardRenderer;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Container;
import javax.swing.JPanel;
import javax.swing.JComponent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class HandPanel extends JPanel {
    private static final int CARD_WIDTH = 70;
    private static final int CARD_HEIGHT = 95;
    private static final int CARD_SPACING = -20;
    private static final Color EMPTY_CARD_BG = new Color(50, 50, 52);
    private static final Color EMPTY_CARD_BORDER = new Color(80, 80, 82);
    private static final float[] DASH_PATTERN = {5.0f, 5.0f};
    private static Card currentOtherCard;
    private static final Color HOVER_BORDER_COLOR = new Color(100, 100, 255);
    private boolean isHovered = false;
    private static final int PADDING = 2; // Reduced padding
    
    private Card card1;
    private Card card2;

    // Calculate actual visual dimensions based on card layout
    private static final int CONTENT_WIDTH = CARD_WIDTH * 2 + CARD_SPACING;
    private static final int CONTENT_HEIGHT = CARD_HEIGHT;
    private static final int TOTAL_VISUAL_WIDTH = CONTENT_WIDTH + (PADDING * 2);
    private static final int TOTAL_VISUAL_HEIGHT = CONTENT_HEIGHT + (PADDING * 2);
    
    public HandPanel(Card c1, Card c2) {
        this.card1 = c1;
        this.card2 = c2;
        setOpaque(false);
        setPreferredSize(new Dimension(TOTAL_VISUAL_WIDTH, TOTAL_VISUAL_HEIGHT));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        
        // Get opacity from parent PlayerPanel
        Container parent = getParent();
        while (parent != null && !(parent instanceof PlayerPanel)) {
            parent = parent.getParent();
        }
        
        if (parent instanceof PlayerPanel) {
            Object alpha = ((JComponent)parent).getClientProperty("alpha");
            float alphaValue = alpha != null ? ((Number)alpha).floatValue() : 1.0f;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaValue));
        }

        // Opacity handling remains the same...
        super.paintComponent(g2);
        
        int cardX2 = PADDING + CARD_WIDTH + CARD_SPACING;
        int cardX1 = PADDING;
        int cardY = PADDING;
        
        // Paint second card
        if (card2 != null) {
            currentOtherCard = card1;
            SimpleCardRenderer.paintCard(g2, card2, cardX2, cardY);
        } else {
            // Paint empty card background and plus
            paintEmptyCardBase(g2, cardX2, cardY);
            // Draw hover border if hovered
            if (isHovered) {
                paintHoverBorder(g2, cardX2, cardY);
            } else {
                paintDashedBorder(g2, cardX2, cardY);
            }
        }
        
        // Paint first card
        if (card1 != null) {
            currentOtherCard = card2;
            SimpleCardRenderer.paintCard(g2, card1, cardX1, cardY);
        } else {
            // Paint empty card background and plus
            paintEmptyCardBase(g2, cardX1, cardY);
            // Draw hover border if hovered
            if (isHovered) {
                paintHoverBorder(g2, cardX1, cardY);
            } else {
                paintDashedBorder(g2, cardX1, cardY);
            }
        }
        
        currentOtherCard = null;
        g2.dispose();
    }
    
    private void paintEmptyCardBase(Graphics2D g2, int x, int y) {
        // Draw card background
        g2.setColor(EMPTY_CARD_BG);
        g2.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 16, 16);
        
        // Draw plus symbol
        g2.setColor(EMPTY_CARD_BORDER);
        g2.setStroke(new BasicStroke(1.5f));
        int centerX = x + CARD_WIDTH/2;
        int centerY = y + CARD_HEIGHT/2;
        int size = 14;
        
        g2.drawLine(centerX - size/2, centerY, centerX + size/2, centerY);
        g2.drawLine(centerX, centerY - size/2, centerX, centerY + size/2);
    }
    
    private void paintDashedBorder(Graphics2D g2, int x, int y) {
        g2.setColor(EMPTY_CARD_BORDER);
        BasicStroke dashed = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, 
            BasicStroke.JOIN_ROUND, 0, DASH_PATTERN, 0);
        g2.setStroke(dashed);
        g2.drawRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 16, 16);
    }
    
    private void paintHoverBorder(Graphics2D g2, int x, int y) {
        g2.setColor(HOVER_BORDER_COLOR);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 16, 16);
    }

    public static Card getCurrentOtherCard(Card card) {
        return currentOtherCard;
    }
    
    public void updateCards(Card c1, Card c2) {
        this.card1 = c1;
        this.card2 = c2;
        repaint();
    }
}
