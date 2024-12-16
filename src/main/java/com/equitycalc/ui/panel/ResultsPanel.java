package com.equitycalc.ui.panel;

import com.equitycalc.simulation.SimulationResult;
import com.equitycalc.ui.util.BetSizingRecommender;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.util.List;

public class ResultsPanel extends JPanel {
    private static final Color PANEL_BG = new Color(44, 44, 46);
    private static final Color WIN_COLOR = new Color(34, 197, 94);
    private static final Color SPLIT_COLOR = new Color(245, 158, 11);
    private static final Color LOSE_COLOR = new Color(239, 68, 68);
    private static final Color TEXT_COLOR = new Color(235, 235, 235);
    private static final int CORNER_RADIUS = 8;
    private static final int PADDING = 12;

    private final JPanel resultsContainer;
    private boolean isVisible = false;

    public ResultsPanel() {
        setLayout(new GridBagLayout());
        setBackground(PANEL_BG);
        setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        setVisible(false);

        // Main container with padding and background
        resultsContainer = new RoundedPanel(12);
        resultsContainer.setLayout(new BoxLayout(resultsContainer, BoxLayout.Y_AXIS));
        resultsContainer.setBackground(PANEL_BG);
        
        // Set border with shadow
        resultsContainer.setBorder(BorderFactory.createCompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));

        // Set preferred width for results container
        resultsContainer.setPreferredSize(new Dimension(280, 0));
        resultsContainer.setMaximumSize(new Dimension(280, Integer.MAX_VALUE));

        // Add to wrapper panel
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.VERTICAL;
        add(resultsContainer, gbc);
    }

    public void updateResults(SimulationResult result, int playerIndex) {
        resultsContainer.removeAll();
        
        addEquitySection(result, playerIndex);
        addSeparator();
        addActionSection(result.getWinProbability(playerIndex));
        
        setVisible(true);
        revalidate();
        repaint();
    }

    private void addEquitySection(SimulationResult result, int playerIndex) {
        // Large equity display
        JPanel equityPanel = new JPanel(new BorderLayout(0, 8));
        equityPanel.setOpaque(false);
        
        double winEquity = result.getWinProbability(playerIndex);
        JLabel equityLabel = new JLabel(String.format("%.1f%%", winEquity * 100));
        equityLabel.setFont(new Font(".SF NS", Font.BOLD, 32));
        equityLabel.setForeground(TEXT_COLOR);
        equityPanel.add(equityLabel, BorderLayout.NORTH);

        // Probability bar
        EquityBar equityBar = new EquityBar(
            result.getWinProbability(playerIndex),
            result.getSplitProbability(playerIndex),
            result.getLossProbability(playerIndex)
        );
        equityPanel.add(equityBar, BorderLayout.CENTER);
        
        resultsContainer.add(equityPanel);
        resultsContainer.add(Box.createVerticalStrut(16));

        // Stats grid
        JPanel statsGrid = new JPanel(new GridLayout(3, 1, 8, 8));
        statsGrid.setOpaque(false);
        
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
            
        resultsContainer.add(statsGrid);
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

    private void addActionSection(double equity) {
        resultsContainer.add(Box.createVerticalStrut(20));
        
        JLabel header = new JLabel("Recommended Actions");
        header.setFont(new Font(".SF NS", Font.PLAIN, 13));
        header.setForeground(new Color(235, 235, 235, 180));
        resultsContainer.add(header);
        
        resultsContainer.add(Box.createVerticalStrut(12));
        
        // Action chips/pills
        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actionsPanel.setOpaque(false);
        
        List<Double> bets = BetSizingRecommender.getProfitableBetSizes(equity);
        for (Double bet : bets) {
            actionsPanel.add(new ActionChip(String.format("Bet %.0f%%", bet * 100)));
        }
        
        resultsContainer.add(actionsPanel);
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

    private void addSeparator() {
        resultsContainer.add(Box.createVerticalStrut(1));
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setForeground(new Color(60, 60, 60));
        separator.setBackground(new Color(60, 60, 60));
        resultsContainer.add(separator);
        resultsContainer.add(Box.createVerticalStrut(1));
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

    private static class RoundedPanel extends JPanel {
        private final int radius;
        
        public RoundedPanel(int radius) {
            super();
            this.radius = radius;
            setOpaque(false);
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            
            g2.dispose();
        }
    }
}