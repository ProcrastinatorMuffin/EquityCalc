package com.equitycalc.ui.panel;

import com.equitycalc.model.Card;
import com.equitycalc.simulation.SimulationResult;
import com.equitycalc.ui.dialog.RangeMatrixDialog;
import com.equitycalc.ui.util.BetSizingRecommender;
import com.equitycalc.model.Range;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.Timer;


import java.util.*;
import java.util.List;

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
    private HandPanel handPanel;

    private JPanel leftPanel;  // Contains existing hand content
    private JPanel rightPanel; // Will contain results
    private JPanel handContainer; // New field for combined hand+checkbox
    private boolean isExpanded = false;
    private static final int ANIMATION_DURATION = 300; // ms

    private static final Color HERO_BORDER_COLOR = new Color(94, 132, 241);  // Blue tint for hero
    private final boolean isHero;

    private static final int TOGGLE_HEIGHT = 24;
    private static final int TOGGLE_WIDTH = 50;
    private static final Color TOGGLE_BG = new Color(50, 50, 52);
    private static final Color TOGGLE_SLIDER = new Color(235, 235, 235);
    private static final Color TOGGLE_ACTIVE = new Color(94, 132, 241);

    // Add new fields
    private Mode currentMode = Mode.HAND;
    private Range playerRange;  // Store the range when in range mode
    private MacToggle modeToggle;
    
    // Custom macOS style toggle
    private class MacToggle extends JPanel {
        private boolean isActive = true;
        private final int width = TOGGLE_WIDTH;
        private final int height = TOGGLE_HEIGHT;
        private float sliderPosition = 1.0f;
        private float opacity = 0.3f; // Default faded state
        private Timer animationTimer;
        private Timer fadeTimer;
        private static final int HOVER_RADIUS = 50; // Pixel radius for hover detection
        private AWTEventListener globalMouseListener;
        private static final int FPS = 144;
        private static final int FRAME_TIME = 1000 / FPS; // ≈7ms
        private static final int ANIMATION_DURATION = 180; // ms, slightly longer for smoother feel
        
        private float easeOutExpo(float x) {
            return x == 1 ? 1 : 1 - (float)Math.pow(2, -10 * x);
        }

        public MacToggle() {
            setPreferredSize(new Dimension(width, height));
            setMinimumSize(new Dimension(width, height));
            setMaximumSize(new Dimension(width, height));
            setOpaque(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            // Mouse click handler
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    isActive = !isActive;
                    animateToggle(isActive);
                    updateOpacityRecursively(PlayerPanel.this);
                    repaint();
                }
                
                @Override
                public void mouseEntered(MouseEvent e) {
                    animateOpacity(1.0f);
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    // Only fade if mouse is far enough
                    Point p = e.getPoint();
                    SwingUtilities.convertPointToScreen(p, MacToggle.this);
                    if (!isMouseNear(p)) {
                        animateOpacity(0.3f);
                    }
                }
            });
            
            // Mouse motion tracking
            globalMouseListener = event -> {
                if (event instanceof MouseEvent) {
                    MouseEvent me = (MouseEvent) event;
                    if (me.getID() == MouseEvent.MOUSE_MOVED) {
                        Point mousePoint = me.getLocationOnScreen();
                        boolean isNear = isMouseNear(mousePoint);
                        
                        if (isNear && opacity < 1.0f) {
                            animateOpacity(1.0f);
                        } else if (!isNear && opacity > 0.3f) {
                            animateOpacity(0.3f);
                        }
                    }
                }
            };

            Toolkit.getDefaultToolkit().addAWTEventListener(
                globalMouseListener, 
                AWTEvent.MOUSE_MOTION_EVENT_MASK
            );

            // Clean up listener when component is removed
            addAncestorListener(new AncestorListener() {
                public void ancestorAdded(AncestorEvent event) {}
                public void ancestorMoved(AncestorEvent event) {}
                public void ancestorRemoved(AncestorEvent event) {
                    Toolkit.getDefaultToolkit().removeAWTEventListener(globalMouseListener);
                }
            });
        }

        private boolean isMouseNear(Point mousePoint) {
            Point toggleCenter = getLocationOnScreen();
            toggleCenter.x += width / 2;
            toggleCenter.y += height / 2;
            
            double distance = mousePoint.distance(toggleCenter);
            return distance < HOVER_RADIUS;
        }
        
        private void animateOpacity(float targetOpacity) {
            if (fadeTimer != null && fadeTimer.isRunning()) {
                fadeTimer.stop();
            }
            
            // Immediate feedback
            opacity = opacity + (targetOpacity - opacity) * 0.3f;
            repaint();
            
            final float startOpacity = opacity;
            final long startTime = System.nanoTime();
            
            fadeTimer = new Timer(FRAME_TIME, e -> {
                long elapsed = (System.nanoTime() - startTime) / 1_000_000L;
                float progress = Math.min(1f, (float)elapsed / ANIMATION_DURATION);
                float easedProgress = easeOutExpo(progress);
                
                opacity = startOpacity + (targetOpacity - startOpacity) * easedProgress;
                
                if (progress >= 1) {
                    fadeTimer.stop();
                    opacity = targetOpacity;
                }
                repaint();
            });
            
            fadeTimer.setCoalesce(false);
            fadeTimer.setRepeats(true);
            fadeTimer.start();
        }
    
        private void animateToggle(boolean targetState) {
            if (animationTimer != null && animationTimer.isRunning()) {
                animationTimer.stop();
            }
            
            // Immediate feedback
            float targetPos = targetState ? 1.0f : 0.0f;
            sliderPosition = sliderPosition + (targetPos - sliderPosition) * 0.3f;
            repaint();
            
            final float startPos = sliderPosition;
            final long startTime = System.nanoTime();
            
            animationTimer = new Timer(FRAME_TIME, e -> {
                long elapsed = (System.nanoTime() - startTime) / 1_000_000L;
                float progress = Math.min(1f, (float)elapsed / ANIMATION_DURATION);
                float easedProgress = easeOutExpo(progress);
                
                sliderPosition = startPos + (targetPos - startPos) * easedProgress;
                
                if (progress >= 1) {
                    animationTimer.stop();
                    sliderPosition = targetPos;
                }
                repaint();
            });
            
            animationTimer.setCoalesce(false);
            animationTimer.setRepeats(true);
            animationTimer.start();
        }
    
        public boolean isActive() {
            return isActive;
        }
        
        @Override 
        public boolean isOpaque() {
            return false;
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Apply opacity to entire component
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            
            int cornerRadius = height;
            RoundRectangle2D toggleShape = new RoundRectangle2D.Float(
                0, 0, width - 1, height - 1, cornerRadius, cornerRadius
            );
            
            // Set clip to ensure everything stays within rounded bounds
            g2.setClip(toggleShape);
            
            // Draw background
            g2.setColor(sliderPosition > 0 ? TOGGLE_ACTIVE : TOGGLE_BG);
            g2.fill(toggleShape);
            
            // Draw border
            g2.setColor(new Color(0, 0, 0, 30));
            g2.draw(toggleShape);
            
            // Draw slider
            int sliderDiameter = height - 4;
            int sliderX = 2 + (int)((width - sliderDiameter - 4) * sliderPosition);
            
            // Draw slider shadow
            g2.setColor(new Color(0, 0, 0, 30));
            g2.fillOval(sliderX + 1, 3, sliderDiameter, sliderDiameter);
            
            // Draw slider
            g2.setColor(TOGGLE_SLIDER);
            g2.fillOval(sliderX, 2, sliderDiameter, sliderDiameter);
            
            g2.dispose();
        }
    }

    public enum Mode {
        HAND("Hand"),
        RANGE("Range");
        
        private final String display;
        Mode(String display) { this.display = display; }
        public String toString() { return display; }
    }
    
    /**
     * Creates a new player panel with the specified name.
     * @param name The player's name
     */
    public PlayerPanel(String name, boolean isHero) {
        this.isHero = isHero;
        this.playerRange = new Range();
        this.modeToggle = new MacToggle(); // Initialize toggle first
        
        setLayout(new GridBagLayout());
        setBackground(PANEL_BG);
        
        setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(CORNER_RADIUS, isHero ? HERO_BORDER_COLOR : BORDER_COLOR),
            BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING)
        ));
    
        setupHandPanel();
        setupContentPanels();
    }

    private void setupHandPanel() {
        handPanel = new HandPanel(null, null, HandPanel.Mode.BUTTON);
        handPanel.setPreferredSize(new Dimension(145, 100));
        handPanel.setMinimumSize(new Dimension(145, 100));
        handPanel.setMaximumSize(new Dimension(145, 100));
        handPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showRangeDialog();
            }
        });
    }

    private void setupContentPanels() {
        leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBackground(PANEL_BG);
        
        rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBackground(PANEL_BG);
        rightPanel.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        rightPanel.setVisible(false);
    
        // Create toggle container
        JPanel toggleContainer = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        toggleContainer.setBackground(PANEL_BG);
        toggleContainer.add(modeToggle);
    
        // Create hand container
        handContainer = new JPanel(new GridBagLayout());
        handContainer.setBackground(PANEL_BG);
        
        // Add hand panel to handContainer
        GridBagConstraints handPanelGbc = new GridBagConstraints();
        handPanelGbc.gridx = 0;
        handPanelGbc.gridy = 0;
        handPanelGbc.weightx = 1.0;
        handPanelGbc.weighty = 1.0;
        handPanelGbc.anchor = GridBagConstraints.CENTER;
        handContainer.add(handPanel, handPanelGbc);
    
        // Add components to left panel
        GridBagConstraints gbc = new GridBagConstraints();
        
        // Add toggle at top
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets = new Insets(0, 0, PADDING/2, 0);
        leftPanel.add(toggleContainer, gbc);
    
        // Add empty panel for top spacing
        JPanel topSpacer = new JPanel();
        topSpacer.setBackground(PANEL_BG);
        gbc.gridy = 1;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.BOTH;
        leftPanel.add(topSpacer, gbc);
    
        // Add hand container
        gbc.gridy = 2;
        gbc.weighty = 0.5;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 0, 0);
        leftPanel.add(handContainer, gbc);
    
        // Add empty panel for bottom spacing
        JPanel bottomSpacer = new JPanel();
        bottomSpacer.setBackground(PANEL_BG);
        gbc.gridy = 3;
        gbc.weighty = 0.5;
        gbc.fill = GridBagConstraints.BOTH;
        leftPanel.add(bottomSpacer, gbc);
    
        // Left panel takes 45% of space
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.45;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        add(leftPanel, gbc);

        // Right panel takes 55% of space
        gbc.gridx = 1;
        gbc.weightx = 0.55;
        add(rightPanel, gbc);
    }

    private void showRangeDialog() {
        if (currentMode == Mode.HAND) {
            // Existing hand selection dialog
            RangeMatrixDialog dialog = new RangeMatrixDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this), this);
            dialog.setVisible(true);
        } else {
            // Modified matrix dialog for range selection
            RangeMatrixDialog dialog = new RangeMatrixDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this),
                (rank1, rank2, suited) -> {
                    playerRange.addHand(rank1, rank2, suited);
                });
            dialog.setVisible(true);
        }
    }
    
    // Add getter for range
    public Range getRange() {
        return currentMode == Mode.RANGE ? playerRange : null;
    }
    
    public void updateOpacityRecursively(Container container) {
        updateOpacityRecursively(container, isActive() || modeToggle.isActive() ? ACTIVE_ALPHA : INACTIVE_ALPHA);
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
        
        // Main container with padding and background
        JPanel resultsContainer = new RoundedPanel(12);
        resultsContainer.setLayout(new BoxLayout(resultsContainer, BoxLayout.Y_AXIS));
        resultsContainer.setBackground(PANEL_BG);
        
        // Reduced padding
        resultsContainer.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));
    
        // Set preferred width for results container
        resultsContainer.setPreferredSize(new Dimension(280, 0));
        resultsContainer.setMaximumSize(new Dimension(280, Integer.MAX_VALUE));
    
        // Wrap results in a centered container
        JPanel wrapperPanel = new JPanel(new GridBagLayout());
        wrapperPanel.setBackground(PANEL_BG);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.VERTICAL;
        wrapperPanel.add(resultsContainer, gbc);
    
        // Add sections
        addEquitySection(resultsContainer, result, playerIndex);
        addSeparator(resultsContainer);
        addActionSection(resultsContainer, result.getWinProbability(playerIndex));
        
        // Add wrapper to right panel with proper constraints
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.VERTICAL;
        rightPanel.add(wrapperPanel, gbc);
    
        if (!isExpanded) animateExpansion();
    }
    
    private void addEquitySection(JPanel panel, SimulationResult result, int playerIndex) {
        // Large equity display
        JPanel equityPanel = new JPanel(new BorderLayout(0, 8));
        equityPanel.setOpaque(false);
        
        double winEquity = result.getWinProbability(playerIndex);
        JLabel equityLabel = new JLabel(String.format("%.1f%%", winEquity * 100));
        equityLabel.setFont(new Font(".SF NS", Font.BOLD, 32));
        equityLabel.setForeground(new Color(235, 235, 235));
        equityPanel.add(equityLabel, BorderLayout.NORTH);
    
        // Probability bar
        EquityBar equityBar = new EquityBar(
            result.getWinProbability(playerIndex),
            result.getSplitProbability(playerIndex),
            result.getLossProbability(playerIndex)
        );
        equityPanel.add(equityBar, BorderLayout.CENTER);
        
        panel.add(equityPanel);
        panel.add(Box.createVerticalStrut(16));
    
        // Detailed stats in grid
        JPanel statsGrid = new JPanel(new GridLayout(3, 1, 8, 8));
        statsGrid.setOpaque(false);
        
        // Convert values to strings before passing to addStatRow
        addStatRow(statsGrid, "Win", 
            formatProbability(result.getWinProbability(playerIndex)),
            formatConfidenceInterval(result.getWinProbabilityWithConfidence(playerIndex)),
            WIN_COLOR);
        addStatRow(statsGrid, "Split",
            formatProbability(result.getSplitProbability(playerIndex)),
            formatConfidenceInterval(result.getSplitProbabilityWithConfidence(playerIndex)),
            SPLIT_COLOR);
        addStatRow(statsGrid, "Lose",
            formatProbability(result.getLossProbability(playerIndex)),
            formatConfidenceInterval(result.getLossProbabilityWithConfidence(playerIndex)),
            LOSE_COLOR);
            
        panel.add(statsGrid);
    }

    class EquityBar extends JPanel {
        private final double win, split, lose;
        
        public EquityBar(double win, double split, double lose) {
            this.win = win;
            this.split = split;
            this.lose = lose;
            setPreferredSize(new Dimension(0, 6));
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int w = getWidth();
            int h = getHeight();
            
            // Draw segments
            int winWidth = (int)(w * win);
            int splitWidth = (int)(w * split);
            
            g2.setColor(WIN_COLOR);
            g2.fillRoundRect(0, 0, winWidth, h, h, h);
            
            g2.setColor(SPLIT_COLOR);
            g2.fillRect(winWidth, 0, splitWidth, h);
            
            g2.setColor(LOSE_COLOR);
            g2.fillRoundRect(winWidth + splitWidth, 0, w - (winWidth + splitWidth), h, h, h);
            
            g2.dispose();
        }
    }

    private void animateExpansion() {
        int startWidth = getWidth();
        int targetWidth = (int)(startWidth * 1.5); // Reduced from 2.0
        
        Timer timer = new Timer(16, null);
        final long startTime = System.currentTimeMillis();
        
        timer.addActionListener(e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            float progress = Math.min(1f, (float)elapsed / ANIMATION_DURATION);
            float easedProgress = easeInOutCubic(progress);
            
            int currentWidth = startWidth + (int)((targetWidth - startWidth) * easedProgress);
            setPreferredSize(new Dimension(currentWidth, getHeight()));
            
            if (progress >= 0.2f && !rightPanel.isVisible()) {
                rightPanel.setVisible(true);
                rightPanel.setOpaque(false);
                
                GridBagConstraints leftGbc = ((GridBagLayout)getLayout()).getConstraints(leftPanel);
                GridBagConstraints rightGbc = ((GridBagLayout)getLayout()).getConstraints(rightPanel);
                
                leftGbc.weightx = 0.45;
                rightGbc.weightx = 0.55;
                
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

    private void addStatRow(JPanel panel, String label, String value, String confidence, Color color) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        
        // Label with dot indicator
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        labelPanel.setOpaque(false);
        
        JLabel dot = new JLabel("•");
        dot.setFont(new Font(".SF NS", Font.BOLD, 16));
        dot.setForeground(color);
        labelPanel.add(dot);
        
        JLabel lblText = new JLabel(label);
        lblText.setFont(new Font(".SF NS", Font.PLAIN, 13));
        lblText.setForeground(TEXT_COLOR);
        labelPanel.add(lblText);
        
        row.add(labelPanel, BorderLayout.WEST);
        
        // Value and confidence
        JPanel valuePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        valuePanel.setOpaque(false);
        
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font(".SF NS", Font.PLAIN, 13));
        valueLabel.setForeground(color);
        valuePanel.add(valueLabel);
        
        if (confidence != null) {
            JLabel ciLabel = new JLabel(confidence);
            ciLabel.setFont(new Font(".SF NS", Font.PLAIN, 12));
            ciLabel.setForeground(new Color(color.getRed(), color.getGreen(), color.getBlue(), 180));
            valuePanel.add(ciLabel);
        }
        
        row.add(valuePanel, BorderLayout.EAST);
        panel.add(row);
    }

    private void addActionSection(JPanel panel, double equity) {
        panel.add(Box.createVerticalStrut(20));
        
        JLabel header = new JLabel("Recommended Actions");
        header.setFont(new Font(".SF NS", Font.PLAIN, 13));
        header.setForeground(new Color(235, 235, 235, 180));
        panel.add(header);
        
        panel.add(Box.createVerticalStrut(12));
        
        // Action chips/pills
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionsPanel.setOpaque(false);
        
        List<Double> bets = BetSizingRecommender.getProfitableBetSizes(equity);
        for (Double bet : bets) {
            actionsPanel.add(new ActionChip(String.format("Bet %.0f%%", bet * 100)));
        }
        
        panel.add(actionsPanel);
    }
    
    class ActionChip extends JPanel {
        public ActionChip(String text) {
            setLayout(new FlowLayout(FlowLayout.CENTER, 12, 6));
            setOpaque(false);
            
            JLabel label = new JLabel(text);
            label.setFont(new Font(".SF NS", Font.PLAIN, 12));
            label.setForeground(TEXT_COLOR);
            add(label);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2.setColor(new Color(70, 70, 72));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            
            g2.dispose();
        }
    }
    
    private String formatProbability(double prob) {
        return String.format("%.1f%%", prob * 100);
    }
    
    private String formatConfidenceInterval(double[] ci) {
        return String.format("(%.1f-%.1f%%)", ci[0] * 100, ci[1] * 100);
    }

    // Fix isActive logic
    public boolean isActive() {
        if (!modeToggle.isActive()) return false;
        return true;
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
     * @return Unmodifiable list of selected cards, empty if no cards selected
     */
    public List<Card> getSelectedCards() {
        if (card1 == null || card2 == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(Arrays.asList(card1, card2));
    }

    private static class ShadowBorder extends AbstractBorder {
        private static final int SHADOW_SIZE = 4;
    
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Draw soft shadow
            for (int i = 0; i < SHADOW_SIZE; i++) {
                float alpha = (SHADOW_SIZE - i) / (float) SHADOW_SIZE * 0.3f;
                g2.setColor(new Color(0, 0, 0, (int)(255 * alpha)));
                g2.setStroke(new BasicStroke(i * 2));
                g2.drawRoundRect(x + SHADOW_SIZE - i, y + SHADOW_SIZE - i, 
                               width - (SHADOW_SIZE - i) * 2, height - (SHADOW_SIZE - i) * 2,
                               CORNER_RADIUS, CORNER_RADIUS);
            }
            g2.dispose();
        }
    
        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(SHADOW_SIZE, SHADOW_SIZE, SHADOW_SIZE, SHADOW_SIZE);
        }
    }
    
    // Add separator method
    private void addSeparator(JPanel panel) {
        panel.add(Box.createVerticalStrut(1));
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setForeground(new Color(60, 60, 60));
        separator.setBackground(new Color(60, 60, 60));
        panel.add(separator);
        panel.add(Box.createVerticalStrut(1));
    }
}
