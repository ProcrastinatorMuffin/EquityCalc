package com.equitycalc.ui.panel;

import com.equitycalc.model.Card;
import com.equitycalc.ui.util.CardSelector;
import com.equitycalc.ui.util.SimpleCardRenderer;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.stream.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.geom.AffineTransform;
// import atomic integer
import java.util.concurrent.atomic.AtomicInteger;

public class BoardPanel extends JPanel {
    private static final int CARD_WIDTH = 70;
    private static final int CARD_HEIGHT = 95;
    private static final int CARD_SPACING = 10;
    private static final int FLOP_GROUP_SPACING = 25;
    private static final Color BG_COLOR = new Color(44, 44, 46);
    private static final Color EMPTY_CARD_BG = new Color(50, 50, 52);
    private static final Color EMPTY_CARD_BORDER = new Color(80, 80, 82);
    private static final float[] DASH_PATTERN = {5.0f, 5.0f};
    
    private final List<Card> boardCards = new ArrayList<>(Arrays.asList(null, null, null, null, null));
    private static final float HOVER_SCALE = 1.05f;
    private static final int ANIMATION_FPS = 60;
    private final float[] cardScales = new float[5];
    private final javax.swing.Timer hoverTimer;
    private int hoveredIndex = -1;
    private boolean isHoveringFlop = false;

    public BoardPanel() {
        // Initialize scales
        Arrays.fill(cardScales, 1.0f);
        
        // Create animation timer
        hoverTimer = new javax.swing.Timer(1000 / ANIMATION_FPS, e -> {
            boolean needsRepaint = false;
            
            for (int i = 0; i < cardScales.length; i++) {
                float targetScale;
                if (i < 3) {
                    targetScale = isHoveringFlop ? HOVER_SCALE : 1.0f;
                } else {
                    targetScale = (i == hoveredIndex) ? HOVER_SCALE : 1.0f;
                }
                
                if (Math.abs(cardScales[i] - targetScale) > 0.001f) {
                    cardScales[i] += (targetScale - cardScales[i]) * 0.2f;
                    needsRepaint = true;
                }
            }
            
            if (!needsRepaint) {
                ((javax.swing.Timer)e.getSource()).stop();
            }
            repaint();
        });
        setBackground(BG_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(null, "Board",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                new Font("SF Pro Display", Font.PLAIN, 13),
                Color.WHITE),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        setPreferredSize(new Dimension(
            5 * CARD_WIDTH + 4 * CARD_SPACING + FLOP_GROUP_SPACING,
            CARD_HEIGHT + 20)); // Add some padding for the border
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    int x = e.getX();
                    int y = e.getY();
                    
                    // Calculate centering positions
                    int totalCardsWidth = 5 * CARD_WIDTH + 4 * CARD_SPACING + FLOP_GROUP_SPACING;
                    int startX = (getWidth() - totalCardsWidth) / 2;
                    int startY = (getHeight() - CARD_HEIGHT) / 2;
                    
                    // Adjust coordinates relative to cards start position
                    x -= startX;
                    y -= startY;
                    
                    // Check if click is within card area
                    if (y < 0 || y > CARD_HEIGHT) return;
                    
                    int cardIndex;
                    if (x < 3 * (CARD_WIDTH + CARD_SPACING)) {
                        // Clicked in flop area
                        cardIndex = x / (CARD_WIDTH + CARD_SPACING);
                        if (cardIndex < 3) {
                            showFlopSelector();
                        }
                    } else {
                        // Clicked in turn/river area
                        x -= 3 * (CARD_WIDTH + CARD_SPACING) + FLOP_GROUP_SPACING - CARD_SPACING;
                        cardIndex = 3 + (x / (CARD_WIDTH + CARD_SPACING));
                        if (cardIndex < 5) {
                            showCardSelector(cardIndex);
                        }
                    }
                }

            @Override
            public void mouseExited(MouseEvent e) {
                isHoveringFlop = false;
                hoveredIndex = -1;
                hoverTimer.restart();
            }
        });
        
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Add mouse motion listener
        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateHoverState(e.getX(), e.getY());
            }
        });
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                isHoveringFlop = false;
                hoveredIndex = -1;
                hoverTimer.restart();
            }
            
            @Override
            public void mouseClicked(MouseEvent e) {
                // Keep existing click handler code
                int x = e.getX();
                int y = e.getY();
                
                int totalCardsWidth = 5 * CARD_WIDTH + 4 * CARD_SPACING + FLOP_GROUP_SPACING;
                int startX = (getWidth() - totalCardsWidth) / 2;
                int startY = (getHeight() - CARD_HEIGHT) / 2;
                
                x -= startX;
                y -= startY;
                
                if (y < 0 || y > CARD_HEIGHT) return;
                
                if (x < 3 * (CARD_WIDTH + CARD_SPACING)) {
                    int cardIndex = x / (CARD_WIDTH + CARD_SPACING);
                    if (cardIndex < 3) {
                        showFlopSelector();
                    }
                } else {
                    x -= 3 * (CARD_WIDTH + CARD_SPACING) + FLOP_GROUP_SPACING - CARD_SPACING;
                    int cardIndex = 3 + (x / (CARD_WIDTH + CARD_SPACING));
                    if (cardIndex < 5) {
                        showCardSelector(cardIndex);
                    }
                }
            }
        });
    }

    private void updateHoverState(int mouseX, int mouseY) {
        int totalCardsWidth = 5 * CARD_WIDTH + 4 * CARD_SPACING + FLOP_GROUP_SPACING;
        int startX = (getWidth() - totalCardsWidth) / 2;
        int startY = (getHeight() - CARD_HEIGHT) / 2;
        
        mouseX -= startX;
        mouseY -= startY;
        
        if (mouseY >= 0 && mouseY <= CARD_HEIGHT) {
            if (mouseX < 3 * (CARD_WIDTH + CARD_SPACING)) {
                isHoveringFlop = true;
                hoveredIndex = -1;
            } else {
                mouseX -= 3 * (CARD_WIDTH + CARD_SPACING) + FLOP_GROUP_SPACING - CARD_SPACING;
                int index = 3 + (mouseX / (CARD_WIDTH + CARD_SPACING));
                isHoveringFlop = false;
                hoveredIndex = (index < 5 && mouseX >= 0) ? index : -1;
            }
        } else {
            isHoveringFlop = false;
            hoveredIndex = -1;
        }
        hoverTimer.restart();
    }
    
    private void paintEmptyCard(Graphics2D g2, int x, int y) {
        // Draw card background
        g2.setColor(EMPTY_CARD_BG);
        g2.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 16, 16);
        
        // Draw dashed border
        g2.setColor(EMPTY_CARD_BORDER);
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, 
            BasicStroke.JOIN_ROUND, 0, DASH_PATTERN, 0));
        g2.drawRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 16, 16);
        
        // Draw plus symbol
        g2.setStroke(new BasicStroke(1.5f));
        int centerX = x + CARD_WIDTH/2;
        int centerY = y + CARD_HEIGHT/2;
        int size = 14;
        
        g2.drawLine(centerX - size/2, centerY, centerX + size/2, centerY);
        g2.drawLine(centerX, centerY - size/2, centerX, centerY + size/2);
    }
    
    private void showFlopSelector() {
        AtomicInteger flopIndex = new AtomicInteger(0);
        while (flopIndex.get() < 3) {
            JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), 
                "Select Flop Card " + (flopIndex.get() + 1), true);
            dialog.setBackground(new Color(28, 28, 30));
            dialog.getRootPane().putClientProperty("apple.awt.windowAppearance", "dark");
            
            Card selectedCard = CardSelector.showDialog(this, "Select Flop Card " + (flopIndex.get() + 1));
            if (selectedCard != null) {
                // Validate against existing cards
                List<Card> existingCards = boardCards.stream()
                    .filter(c -> c != null && !c.equals(boardCards.get(flopIndex.get())))
                    .collect(Collectors.toList());
                    
                if (!existingCards.contains(selectedCard)) {
                    boardCards.set(flopIndex.get(), selectedCard);
                    repaint();
                    flopIndex.incrementAndGet();
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Card already exists on board",
                        "Duplicate Card",
                        JOptionPane.WARNING_MESSAGE);
                    // Don't increment, retry same position
                }
            } else {
                break; // User cancelled
            }
        }
    }
    
    private void showCardSelector(int cardIndex) {
        JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), 
            "Select " + (cardIndex == 3 ? "Turn" : "River"), true);
        dialog.setBackground(new Color(28, 28, 30));
        dialog.getRootPane().putClientProperty("apple.awt.windowAppearance", "dark");
        
        // Use single card selector
        Card selectedCard = CardSelector.showDialog(this, 
            "Select " + (cardIndex == 3 ? "Turn" : "River"));
        if (selectedCard != null) {
            // Validate against existing cards
            List<Card> existingCards = boardCards.stream()
                .filter(c -> c != null && !c.equals(boardCards.get(cardIndex)))
                .collect(Collectors.toList());
                
            if (!existingCards.contains(selectedCard)) {
                boardCards.set(cardIndex, selectedCard);
                repaint();
            } else {
                JOptionPane.showMessageDialog(this,
                    "Card already exists on board",
                    "Duplicate Card",
                    JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int totalCardsWidth = 5 * CARD_WIDTH + 4 * CARD_SPACING + FLOP_GROUP_SPACING;
        int startX = (getWidth() - totalCardsWidth) / 2;
        int startY = (getHeight() - CARD_HEIGHT) / 2;
        
        for (int i = 0; i < boardCards.size(); i++) {
            AffineTransform originalTransform = g2.getTransform();
            
            int x = startX + i * (CARD_WIDTH + CARD_SPACING);
            if (i >= 3) {
                x += FLOP_GROUP_SPACING - CARD_SPACING;
            }
            
            // Apply scale transform
            if (cardScales[i] != 1.0f) {
                int centerX = x + CARD_WIDTH / 2;
                int centerY = startY + CARD_HEIGHT / 2;
                g2.translate(centerX, centerY);
                g2.scale(cardScales[i], cardScales[i]);
                g2.translate(-centerX, -centerY);
            }
            
            Card card = boardCards.get(i);
            if (card != null) {
                SimpleCardRenderer.paintCard(g2, card, x, startY);
            } else {
                paintEmptyCard(g2, x, startY);
            }
            
            g2.setTransform(originalTransform);
        }
    }
    
    public List<Card> getSelectedCards() {
        return boardCards.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
