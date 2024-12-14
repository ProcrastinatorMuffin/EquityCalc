package com.equitycalc.ui.panel;

import com.equitycalc.model.Card;
import com.equitycalc.ui.components.checkbox.MacCheckBox;
import com.equitycalc.ui.dialog.RangeMatrixDialog;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.AbstractBorder;

import java.util.*;
import java.util.List;

/**
 * Panel representing a player in the equity calculator.
 * Displays player's cards and controls for hand selection.
 */
public class PlayerPanel extends JPanel {
    private static final Color PANEL_BG = new Color(44, 44, 46);
    private static final Color BORDER_COLOR = new Color(60, 60, 60);
    private static final int CORNER_RADIUS = 8;
    private static final int PADDING = 12;
    
    public static final float INACTIVE_ALPHA = 0.6f;
    public static final float HOVER_ALPHA = 0.8f;
    public static final float ACTIVE_ALPHA = 1.0f;
    
    private Card card1;
    private Card card2;
    private final MacCheckBox activeBox;
    private final HandPanel handPanel;
    
    /**
     * Creates a new player panel with the specified name.
     * @param name The player's name
     */
    public PlayerPanel(String name) {
        setLayout(new FlowLayout(FlowLayout.CENTER, PADDING, PADDING));
        setBackground(PANEL_BG);
        setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(CORNER_RADIUS, BORDER_COLOR),
            BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING)
        ));
        
        activeBox = new MacCheckBox();
        activeBox.setSelected(true);
        activeBox.setBackground(PANEL_BG);
        activeBox.addItemListener(e -> updateOpacityRecursively(this));
        
        handPanel = new HandPanel(null, null);
        handPanel.setPreferredSize(new Dimension(145, 100));
        handPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showRangeDialog();
            }
        });
        
        setupComponents();
        setupMouseListeners();
        updateOpacityRecursively(this);
    }

    private void showRangeDialog() {
        RangeMatrixDialog dialog = new RangeMatrixDialog(
            (JFrame) SwingUtilities.getWindowAncestor(this), this);
        dialog.setVisible(true);
    }

    private void setupComponents() {
        add(activeBox);
        add(handPanel);
    }

    private void setupMouseListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!isActive()) {
                    updateOpacityRecursively(PlayerPanel.this, HOVER_ALPHA);
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (!isActive()) {
                    updateOpacityRecursively(PlayerPanel.this, INACTIVE_ALPHA);
                }
            }
        });
    }
    
    public void updateOpacityRecursively(Container container) {
        updateOpacityRecursively(container, isActive() ? ACTIVE_ALPHA : INACTIVE_ALPHA);
    }
    
    public void updateOpacityRecursively(Container container, float alpha) {
        if (container instanceof JComponent) {
            ((JComponent)container).putClientProperty("alpha", alpha);
        }
        
        for (Component child : container.getComponents()) {
            if (child instanceof Container) {
                updateOpacityRecursively((Container)child, alpha);
            }
        }
        container.repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            Object alpha = getClientProperty("alpha");
            float alphaValue = alpha != null ? ((Number)alpha).floatValue() : ACTIVE_ALPHA;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaValue));
            super.paintComponent(g2);
        } finally {
            g2.dispose();
        }
    }

    // Custom rounded border
    private static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;
        
        RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }
        
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }
        
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }
    }
    
    /**
     * Sets the player's cards and updates the display.
     * @param c1 First card, may be null
     * @param c2 Second card, may be null
     */
    public void setCards(Card c1, Card c2) {
        this.card1 = c1;
        this.card2 = c2;
        handPanel.updateCards(c1, c2);
        revalidate();
        repaint();
    }
    
    /**
     * @return true if the player is active, false otherwise
     */
    public boolean isActive() {
        return activeBox.isSelected();
    }
    
    /**
     * @return Unmodifiable list of selected cards, empty if no cards selected
     */
    public List<Card> getSelectedCards() {
        if (card1 == null || card2 == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(Arrays.asList(card1, card2));
    }
}
