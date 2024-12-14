package com.equitycalc.ui.util;

import com.equitycalc.model.Card;
import com.equitycalc.ui.panel.BoardPanel;
import com.equitycalc.ui.panel.PlayerPanel;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import javax.swing.*;

import java.util.ArrayList;
import java.util.List;


public class CardSelector extends JPanel {
    private static final String[] RANKS = {"2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K", "A"};
    private static final String[] SUITS = {"c", "d", "h", "s"};
    private Card selectedCard = null;
    private static final float HOVER_SCALE = 1.05f;
    private static final int ANIMATION_DURATION = 150; // ms

    private static class AnimatedCardPanel extends JPanel {
        private final Timer hoverTimer;
        public float scale = 1.0f;
        private boolean isHovered = false;
        private final String text;
        
        public AnimatedCardPanel(String text) {
            this.text = text;
            setOpaque(false);
            
            // Create hover animation timer
            hoverTimer = new Timer(16, e -> { // ~60fps
                if (isHovered && scale < HOVER_SCALE) {
                    scale += 0.01f;
                } else if (!isHovered && scale > 1.0f) {
                    scale -= 0.01f;
                } else {
                    ((Timer)e.getSource()).stop();
                }
                repaint();
            });
            
            // Add mouse listeners
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    isHovered = true;
                    hoverTimer.restart();
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    hoverTimer.restart();
                }
            });
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Center the card
            int w = getWidth();
            int h = getHeight();
            int cardW = (int)(70 * scale);
            int cardH = (int)(95 * scale);
            int x = (w - cardW) / 2;
            int y = (h - cardH) / 2;
            
            // Draw scaled card
            g2.translate(x + cardW/2, y + cardH/2);
            g2.scale(scale, scale);
            g2.translate(-cardW/2, -cardH/2);
            
            // Draw card background with shadow if hovered
            if (isHovered) {
                g2.setColor(new Color(0, 0, 0, 30));
                g2.fillRoundRect(2, 2, cardW, cardH, 16, 16);
            }
            
            g2.setColor(new Color(50, 50, 52));
            g2.fillRoundRect(0, 0, cardW, cardH, 16, 16);
            
            // Draw text
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SF Pro Display", Font.BOLD, 42));
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(text);
            int textHeight = fm.getHeight();
            g2.drawString(text, 
                (cardW - textWidth) / 2,
                (cardH + textHeight) / 2 - fm.getDescent());
                
            // Reset transform
            g2.setTransform(new AffineTransform());
        }
        
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(80, 105);
        }
    }
    
    public static Card showDialog(Component parent, String title) {
        CardSelector selector = new CardSelector();
        
        JDialog rankDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), title, true);
        rankDialog.setBackground(new Color(28, 28, 30));
        rankDialog.getRootPane().putClientProperty("apple.awt.windowAppearance", "dark");
        
        // Use FlowLayout for horizontal arrangement
        JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        mainPanel.setBackground(new Color(28, 28, 30));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Create animated rank cards
        for (String rank : RANKS) {
            AnimatedCardPanel cardPanel = new AnimatedCardPanel(rank);
            cardPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            cardPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent evt) {
                    rankDialog.dispose();
                    showSuitDialog(parent, title, rank, selector);
                }
            });
            mainPanel.add(cardPanel);
        }
        
        rankDialog.add(mainPanel);
        rankDialog.pack();
        rankDialog.setLocationRelativeTo(parent);
        rankDialog.setResizable(false);
        rankDialog.setVisible(true);
        
        return selector.getSelectedCard();
    }
    
    private static void showSuitDialog(Component parent, String title, String rank, CardSelector selector) {
        JDialog suitDialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(parent), title, true);
        suitDialog.setBackground(new Color(28, 28, 30));
        suitDialog.getRootPane().putClientProperty("apple.awt.windowAppearance", "dark");
        
        JPanel mainPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
        mainPanel.setBackground(new Color(28, 28, 30));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        for (String suit : SUITS) {
            Card card = new Card(rank + suit);
            if (isCardUsed(card, parent)) continue;
            
            JPanel cardPanel = new JPanel() {
                private final Timer hoverTimer;
                private float scale = 1.0f;
                private boolean isHovered = false;
                
                {
                    setOpaque(false);
                    setPreferredSize(new Dimension(80, 105));
                    
                    hoverTimer = new Timer(16, e -> {
                        if (isHovered && scale < HOVER_SCALE) {
                            scale += 0.01f;
                        } else if (!isHovered && scale > 1.0f) {
                            scale -= 0.01f;
                        } else {
                            ((Timer)e.getSource()).stop();
                        }
                        repaint();
                    });
                    
                    addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseEntered(MouseEvent e) {
                            isHovered = true;
                            hoverTimer.restart();
                        }
                        
                        @Override
                        public void mouseExited(MouseEvent e) {
                            isHovered = false;
                            hoverTimer.restart();
                        }
                        
                        @Override
                        public void mouseClicked(MouseEvent evt) {
                            selector.setSelectedCard(card);
                            suitDialog.dispose();
                        }
                    });
                }
                
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    // Save original transform
                    AffineTransform origTransform = g2.getTransform();
                    
                    // Calculate center position
                    int w = getWidth();
                    int h = getHeight();
                    int cardW = (int)(70 * scale);
                    int cardH = (int)(95 * scale);
                    int x = (w - cardW) / 2;
                    int y = (h - cardH) / 2;
                    
                    // Apply scaling from center
                    g2.translate(x + cardW/2, y + cardH/2);
                    g2.scale(scale, scale);
                    g2.translate(-cardW/2, -cardH/2);
                    
                    // Draw shadow if hovered
                    if (isHovered) {
                        g2.setColor(new Color(0, 0, 0, 30));
                        g2.fillRoundRect(2, 2, cardW, cardH, 16, 16);
                    }
                    
                    // Draw card
                    SimpleCardRenderer.paintCard(g2, card, 0, 0);
                    
                    // Restore original transform
                    g2.setTransform(origTransform);
                }
            };
            
            cardPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            mainPanel.add(cardPanel);
        }
        
        suitDialog.add(mainPanel);
        suitDialog.pack();
        suitDialog.setLocationRelativeTo(parent);
        suitDialog.setResizable(false);
        suitDialog.setVisible(true);
    }
    
    // Helper method to check if card is already used
    private static boolean isCardUsed(Card card, Component parent) {
        // Get parent window to find other components
        Window parentWindow = SwingUtilities.getWindowAncestor(parent);
        if (parentWindow == null) return false;
    
        // Find board panel
        BoardPanel boardPanel = findBoardPanel(parentWindow);
        if (boardPanel != null) {
            List<Card> boardCards = boardPanel.getSelectedCards();
            if (boardCards.contains(card)) return true;
        }
    
        // Find all player panels
        List<PlayerPanel> playerPanels = findPlayerPanels(parentWindow);
        for (PlayerPanel playerPanel : playerPanels) {
            if (playerPanel.isActive()) {
                List<Card> playerCards = playerPanel.getSelectedCards();
                if (playerCards.contains(card)) return true;
            }
        }
    
        return false;
    }
    
    // Helper methods to find components
    private static BoardPanel findBoardPanel(Container container) {
        if (container instanceof BoardPanel) return (BoardPanel) container;
        
        for (Component c : container.getComponents()) {
            if (c instanceof BoardPanel) return (BoardPanel) c;
            if (c instanceof Container) {
                BoardPanel found = findBoardPanel((Container) c);
                if (found != null) return found;
            }
        }
        return null;
    }
    
    private static List<PlayerPanel> findPlayerPanels(Container container) {
        List<PlayerPanel> panels = new ArrayList<>();
        
        if (container instanceof PlayerPanel) {
            panels.add((PlayerPanel) container);
        }
        
        for (Component c : container.getComponents()) {
            if (c instanceof PlayerPanel) {
                panels.add((PlayerPanel) c);
            } else if (c instanceof Container) {
                panels.addAll(findPlayerPanels((Container) c));
            }
        }
        
        return panels;
    }

    public Card getSelectedCard() {
        return selectedCard;
    }

    private void setSelectedCard(Card card) {
        this.selectedCard = card;
    }
    
    public interface CardSelectionCallback {
        void onCardSelected(Card card);
    }
}
    