package com.equitycalc.ui.panel;

import com.equitycalc.model.Card;
import com.equitycalc.simulation.SimulationResult;
import com.equitycalc.ui.components.checkbox.MacCheckBox;
import com.equitycalc.ui.dialog.RangeMatrixDialog;
import com.equitycalc.ui.util.BetSizingRecommender;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.Timer;

import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Panel representing a player in the equity calculator.
 * Displays player's cards and controls for hand selection.
 */
public class PlayerPanel extends JPanel {
    private static final Color PANEL_BG = new Color(44, 44, 46);
    private static final Color BORDER_COLOR = new Color(60, 60, 60);
    private static final Color WIN_COLOR = new Color(52, 199, 89);
    private static final Color SPLIT_COLOR = new Color(255, 214, 10);
    private static final Color LOSE_COLOR = new Color(255, 69, 58);
    private static final Color TEXT_COLOR = new Color(235, 235, 235);
    
    private static final int CORNER_RADIUS = 8;
    private static final int PADDING = 12;
    
    public static final float INACTIVE_ALPHA = 0.6f;
    public static final float HOVER_ALPHA = 0.8f;
    public static final float ACTIVE_ALPHA = 1.0f;
    
    private Card card1;
    private Card card2;
    private final MacCheckBox activeBox;
    private final HandPanel handPanel;

    private final JPanel leftPanel;  // Contains existing hand content
    private final JPanel rightPanel; // Will contain results
    private final JPanel handContainer; // New field for combined hand+checkbox
    private boolean isExpanded = false;
    private static final int ANIMATION_DURATION = 300; // ms
    private static final int CHECKBOX_MARGIN = 8;
    
    /**
     * Creates a new player panel with the specified name.
     * @param name The player's name
     */
    public PlayerPanel(String name) {
        setLayout(new GridBagLayout());
        setBackground(PANEL_BG);
        setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(CORNER_RADIUS, BORDER_COLOR),
            BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING)
        ));

        // Initialize hand panel
        handPanel = new HandPanel(null, null);
        handPanel.setPreferredSize(new Dimension(145, 100));
        handPanel.setMinimumSize(new Dimension(145, 100));
        handPanel.setMaximumSize(new Dimension(145, 100));
        handPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showRangeDialog();
            }
        });
        
        // Initialize checkbox
        activeBox = new MacCheckBox();
        activeBox.setSelected(true);
        activeBox.setBackground(PANEL_BG);
        activeBox.addItemListener(e -> updateOpacityRecursively(this));
        
        // Create checkbox container
        JPanel checkboxContainer = new JPanel(new GridBagLayout());
        checkboxContainer.setBackground(PANEL_BG);
        checkboxContainer.setPreferredSize(new Dimension(30, 100));
        checkboxContainer.add(activeBox);
        
        // Create hand container
        handContainer = new JPanel();
        handContainer.setLayout(new BoxLayout(handContainer, BoxLayout.X_AXIS));
        handContainer.setBackground(PANEL_BG);
        handContainer.add(checkboxContainer);
        handContainer.add(Box.createHorizontalStrut(CHECKBOX_MARGIN));
        handContainer.add(handPanel);
        
        // Setup left panel
        leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBackground(PANEL_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        leftPanel.add(handContainer, gbc);
        
        // Setup right panel
        rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBackground(PANEL_BG);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        rightPanel.setVisible(false);
        
        // Add panels to main container
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(leftPanel, gbc);
        
        gbc.gridx = 1;
        gbc.weightx = 0.0;
        add(rightPanel, gbc);
        
        setupMouseListeners();
        updateOpacityRecursively(this);
    }

    private void showRangeDialog() {
        RangeMatrixDialog dialog = new RangeMatrixDialog(
            (JFrame) SwingUtilities.getWindowAncestor(this), this);
        dialog.setVisible(true);
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

    public void displayResults(SimulationResult result, int playerIndex, boolean isKnownPlayer) {
        rightPanel.removeAll();
        
        // Create wrapper panel for results
        JPanel resultsWrapper = new JPanel();
        resultsWrapper.setLayout(new BoxLayout(resultsWrapper, BoxLayout.Y_AXIS));
        resultsWrapper.setBackground(PANEL_BG);
        resultsWrapper.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        addProbabilities(resultsWrapper, result, playerIndex);
        addRecommendations(resultsWrapper, result.getWinProbability(playerIndex));
        
        // Add wrapper to right panel with centering
        rightPanel.add(resultsWrapper, new GridBagConstraints());
        
        if (!isExpanded) {
            animateExpansion();
        } else {
            revalidate();
            repaint();
        }
    }

    private void animateExpansion() {
        int startWidth = getWidth();
        int targetWidth = startWidth * 2;
        
        Timer timer = new Timer(16, null);
        final long startTime = System.currentTimeMillis();
        
        timer.addActionListener(e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            float progress = Math.min(1f, (float)elapsed / ANIMATION_DURATION);
            float easedProgress = easeInOutCubic(progress);
            
            int currentWidth = startWidth + (int)((targetWidth - startWidth) * easedProgress);
            setPreferredSize(new Dimension(currentWidth, getHeight()));
            
            // Update weights during animation
            if (progress >= 0.3f && !rightPanel.isVisible()) {
                rightPanel.setVisible(true);
                rightPanel.setOpaque(false);
                
                // Update constraints while preserving component sizes
                GridBagConstraints leftGbc = ((GridBagLayout)getLayout()).getConstraints(leftPanel);
                GridBagConstraints rightGbc = ((GridBagLayout)getLayout()).getConstraints(rightPanel);
                
                leftGbc.weightx = 1.0;
                leftGbc.fill = GridBagConstraints.BOTH;
                leftGbc.anchor = GridBagConstraints.CENTER;
                
                rightGbc.weightx = 1.0;
                rightGbc.fill = GridBagConstraints.BOTH;
                rightGbc.anchor = GridBagConstraints.CENTER;
                
                ((GridBagLayout)getLayout()).setConstraints(leftPanel, leftGbc);
                ((GridBagLayout)getLayout()).setConstraints(rightPanel, rightGbc);
            }
            
            if (rightPanel.isVisible()) {
                float fadeProgress = (progress - 0.3f) / 0.7f;
                fadeProgress = Math.min(1f, Math.max(0f, fadeProgress));
                float fade = easeInOutCubic(fadeProgress);
                rightPanel.setBackground(new Color(
                    PANEL_BG.getRed(),
                    PANEL_BG.getGreen(), 
                    PANEL_BG.getBlue(),
                    (int)(255 * fade)
                ));
            }
            
            if (progress >= 1.0f) {
                timer.stop();
                isExpanded = true;
                rightPanel.setOpaque(true);
                rightPanel.setBackground(PANEL_BG);
            }
            
            revalidate();
            repaint();
        });
        
        timer.start();
    }
    
    private float easeInOutCubic(float x) {
        return x < 0.5f ? 
            4 * x * x * x : 
            1 - (float)Math.pow(-2 * x + 2, 3) / 2;
    }

    private void addProbabilities(JPanel panel, SimulationResult result, int playerIndex) {
        // Add wrapper panel to maintain alignment
        JPanel probPanel = new JPanel();
        probPanel.setLayout(new BoxLayout(probPanel, BoxLayout.Y_AXIS));
        probPanel.setBackground(PANEL_BG);
        probPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        double winProb = result.getWinProbability(playerIndex);
        double[] winCI = result.getWinProbabilityWithConfidence(playerIndex);
        double[] splitCI = result.getSplitProbabilityWithConfidence(playerIndex);
        double[] lossCI = result.getLossProbabilityWithConfidence(playerIndex);
        
        addStyledLabel(panel, String.format("Win: %.1f%% (%.1f-%.1f%%)",
            winProb * 100, winCI[0] * 100, winCI[1] * 100), WIN_COLOR);
        addStyledLabel(panel, String.format("Split: %.1f%% (%.1f-%.1f%%)",
            result.getSplitProbability(playerIndex) * 100, splitCI[0] * 100, splitCI[1] * 100), 
            SPLIT_COLOR);
        addStyledLabel(panel, String.format("Lose: %.1f%% (%.1f-%.1f%%)",
            result.getLossProbability(playerIndex) * 100, lossCI[0] * 100, lossCI[1] * 100), 
            LOSE_COLOR);
            
        
        panel.add(probPanel);
    }
    
    private void addRecommendations(JPanel panel, double equity) {
        // Add wrapper panel to maintain alignment
        JPanel recPanel = new JPanel();
        recPanel.setLayout(new BoxLayout(recPanel, BoxLayout.Y_AXIS));
        recPanel.setBackground(PANEL_BG);
        recPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        List<Double> profitableBets = BetSizingRecommender.getProfitableBetSizes(equity);
        List<Double> profitableCalls = BetSizingRecommender.getProfitableCallSizes(equity);
        
        addRecommendationLabel(recPanel, "Profitable bets", profitableBets);
        addRecommendationLabel(recPanel, "Profitable calls", profitableCalls);
        
        panel.add(recPanel);
    }
    
    private void addStyledLabel(JPanel panel, String text, Color valueColor) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SF Pro Display", Font.PLAIN, 13));
        label.setForeground(valueColor);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createVerticalStrut(5));
    }
    
    private void addRecommendationLabel(JPanel panel, String type, List<Double> values) {
        StringBuilder sb = new StringBuilder("<html>");
        sb.append(type).append(": ");
        if (!values.isEmpty()) {
            sb.append(values.stream()
                .map(size -> String.format("%.0f%%", size * 100))
                .collect(Collectors.joining(", ")));
        } else {
            sb.append("none");
        }
        sb.append("</html>");
        
        JLabel label = new JLabel(sb.toString());
        label.setFont(new Font("SF Pro Display", Font.PLAIN, 13));
        label.setForeground(TEXT_COLOR);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createVerticalStrut(5));
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
