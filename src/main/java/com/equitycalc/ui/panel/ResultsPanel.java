package com.equitycalc.ui.panel;

import com.equitycalc.simulation.SimulationResult;
import com.equitycalc.ui.components.button.ToggleButton;
import com.equitycalc.ui.util.BetSizingRecommender;
import com.equitycalc.model.Card;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;


public class ResultsPanel extends JPanel {
    private static final Color BG_COLOR = new Color(44, 44, 46);
    private static final Color PANEL_BG = new Color(50, 50, 52);
    private final JPanel contentPanel;
    private final ToggleButton toggleButton;
    
    public ResultsPanel() {
        setLayout(new BorderLayout());
        setBackground(BG_COLOR);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        contentPanel = new JPanel();
        contentPanel.setBackground(BG_COLOR);
        contentPanel.setLayout(new GridLayout(0, 3, 15, 15));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        contentPanel.setVisible(false);
        
        // Pass contentPanel reference to ToggleButton
        toggleButton = new ToggleButton(contentPanel);
        toggleButton.setVisible(false);
        
        JPanel togglePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        togglePanel.setBackground(new Color(28, 28, 30));
        togglePanel.add(toggleButton);
        
        add(togglePanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
    }
    
    public void displayResults(SimulationResult result, int randomPlayers, List<PlayerPanel> playerPanels) {
        contentPanel.removeAll();
        int playerIndex = 0;
        
        for (int i = 0; i < playerPanels.size(); i++) {
            PlayerPanel panel = playerPanels.get(i);
            if (panel.isActive()) {
                List<Card> cards = panel.getSelectedCards().stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
                    
                contentPanel.add(createPlayerResultPanel(
                    result, playerIndex, i, cards.size() == 2));
                playerIndex++;
            }
        }
        
        setVisible(true);
        toggleButton.setVisible(true);
        toggleButton.setExpanded(true);
        revalidate();
        repaint();
    }
    
    private JPanel createPlayerResultPanel(SimulationResult result, int playerIndex, 
                                         int displayIndex, boolean isKnownPlayer) {
        JPanel playerResult = new RoundedPanel(8);
        playerResult.setLayout(new BoxLayout(playerResult, BoxLayout.Y_AXIS));
        playerResult.setBackground(PANEL_BG);
        playerResult.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Add title
        addPlayerTitle(playerResult, displayIndex, playerIndex, isKnownPlayer);
        
        // Add probabilities
        addProbabilities(playerResult, result, playerIndex);
        
        // Add recommendations
        addRecommendations(playerResult, result.getWinProbability(playerIndex));
        
        return playerResult;
    }
    
    private void addPlayerTitle(JPanel panel, int displayIndex, int playerIndex, boolean isKnownPlayer) {
        JLabel titleLabel = new JLabel(isKnownPlayer ? 
            "Player " + (displayIndex + 1) : 
            "Random Player " + (playerIndex + 1));
        titleLabel.setFont(new Font("SF Pro Display", Font.BOLD, 14));
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(10));
    }
    
    private void addProbabilities(JPanel panel, SimulationResult result, int playerIndex) {
        double winProb = result.getWinProbability(playerIndex);
        double[] winCI = result.getWinProbabilityWithConfidence(playerIndex);
        double[] splitCI = result.getSplitProbabilityWithConfidence(playerIndex);
        double[] lossCI = result.getLossProbabilityWithConfidence(playerIndex);
        
        addStyledLabel(panel, String.format("Win: %.1f%% (%.1f-%.1f%%)",
            winProb * 100, winCI[0] * 100, winCI[1] * 100), new Color(52, 199, 89));
        addStyledLabel(panel, String.format("Split: %.1f%% (%.1f-%.1f%%)",
            result.getSplitProbability(playerIndex) * 100, splitCI[0] * 100, splitCI[1] * 100), 
            new Color(255, 214, 10));
        addStyledLabel(panel, String.format("Lose: %.1f%% (%.1f-%.1f%%)",
            result.getLossProbability(playerIndex) * 100, lossCI[0] * 100, lossCI[1] * 100), 
            new Color(255, 69, 58));
            
        panel.add(Box.createVerticalStrut(15));
    }
    
    private void addRecommendations(JPanel panel, double equity) {
        JPanel actionsPanel = new JPanel();
        actionsPanel.setLayout(new BoxLayout(actionsPanel, BoxLayout.Y_AXIS));
        actionsPanel.setBackground(PANEL_BG);
        
        List<Double> profitableBets = BetSizingRecommender.getProfitableBetSizes(equity);
        List<Double> profitableCalls = BetSizingRecommender.getProfitableCallSizes(equity);
        
        addRecommendationLabel(actionsPanel, "Profitable bets", profitableBets);
        addRecommendationLabel(actionsPanel, "Profitable calls", profitableCalls);
        
        panel.add(actionsPanel);
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
        label.setForeground(new Color(235, 235, 235));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(label);
        panel.add(Box.createVerticalStrut(5));
    }
}
