package com.equitycalc.ui;

import com.equitycalc.model.*;
import com.equitycalc.simulation.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class EquityCalcGUI extends JFrame {
    private static final int MAX_PLAYERS = 6;
    private final MonteCarloSim simulator;
    private final List<PlayerPanel> playerPanels;
    private final BoardPanel boardPanel;
    private final JPanel resultsPanel;
    private final JButton runButton;
    private final JSpinner iterationsSpinner;
    private final JProgressBar progressBar;
    private final List<Player> knownPlayers = new ArrayList<>();
    
    public EquityCalcGUI() {
        super("Poker Equity Calculator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        simulator = new MonteCarloSim();
        playerPanels = new ArrayList<>();
        
        // Main layout
        setLayout(new BorderLayout(10, 10));
        
        // Main split pane
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        mainSplitPane.setResizeWeight(0.6); // Top section gets 60% initially
        
        // Top section with controls, players and board
        JPanel topSection = new JPanel(new BorderLayout(10, 10));
        
        // Control panel (top)
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        iterationsSpinner = new JSpinner(new SpinnerNumberModel(50000, 1000, 1000000, 1000));
        runButton = new JButton("Run Simulation");
        controlPanel.add(new JLabel("Iterations:"));
        controlPanel.add(iterationsSpinner);
        controlPanel.add(runButton);
        
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        controlPanel.add(progressBar);
        
        topSection.add(controlPanel, BorderLayout.NORTH);
        
        // Players and board split pane
        JSplitPane playersBoardSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        
        // Players panel (center)
        JPanel playersMainPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        for (int i = 0; i < 4; i++) {
            PlayerPanel playerPanel = new PlayerPanel("Player " + (i + 1));
            playerPanels.add(playerPanel);
            playersMainPanel.add(playerPanel);
        }
        JScrollPane playersScroll = new JScrollPane(playersMainPanel);
        playersScroll.setMinimumSize(new Dimension(400, 300));
        
        // Board panel (right)
        boardPanel = new BoardPanel();
        boardPanel.setMinimumSize(new Dimension(150, 300));
        
        playersBoardSplit.setLeftComponent(playersScroll);
        playersBoardSplit.setRightComponent(boardPanel);
        playersBoardSplit.setResizeWeight(0.8); // Players get 80% of horizontal space
        
        topSection.add(playersBoardSplit, BorderLayout.CENTER);
        
        // Results panel (bottom)
        resultsPanel = new JPanel();
        resultsPanel.setBorder(BorderFactory.createTitledBorder("Results"));
        resultsPanel.setMinimumSize(new Dimension(800, 250));
        JScrollPane resultsScroll = new JScrollPane(resultsPanel);
        
        mainSplitPane.setTopComponent(topSection);
        mainSplitPane.setBottomComponent(resultsScroll);
        
        add(mainSplitPane);
        
        // Add run button handler
        runButton.addActionListener(e -> runSimulation());
        
        setPreferredSize(new Dimension(1000, 800));
        pack();
        setLocationRelativeTo(null);
    }
    
    private void runSimulation() {
        runButton.setEnabled(false);
        
        try {
            // Collect and validate player hands
            List<Player> knownPlayers = new ArrayList<>();
            int randomPlayers = 0;
            
            for (PlayerPanel panel : playerPanels) {
                if (panel.isActive()) {
                    List<Card> cards = panel.getSelectedCards();
                    // Filter null cards
                    cards = cards.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());
                        
                    if (cards.size() == 2) {
                        knownPlayers.add(new Player(cards));
                    } else if (cards.isEmpty()) {
                        randomPlayers++;
                    } else {
                        throw new IllegalArgumentException(
                            "Players must have either 0 cards (random) or exactly 2 cards selected"
                        );
                    }
                }
            }
    
            final int finalRandomPlayers = randomPlayers;
            
            if (knownPlayers.isEmpty() && randomPlayers == 0) {
                throw new IllegalArgumentException("At least one active player required");
            }
            
            // Get and validate board cards
            List<Card> boardCards = boardPanel.getSelectedCards();
            
            if (!boardCards.isEmpty() && boardCards.size() < 3) {
                throw new IllegalArgumentException(
                    "Board must have 0 cards (preflop) or at least 3 cards (flop)"
                );
            }
            
            // Create simulation config
            SimulationConfig config = SimulationConfig.builder()
                .withKnownPlayers(knownPlayers)
                .withRandomPlayers(finalRandomPlayers)
                .withBoardCards(boardCards)
                .withNumSimulations((Integer)iterationsSpinner.getValue())
                .build();
                
            SwingWorker<SimulationResult, Integer> worker = new SwingWorker<>() {
                    @Override
                    protected SimulationResult doInBackground() throws Exception {
                        progressBar.setVisible(true);
                        simulator.setProgressCallback((Integer progress) -> {
                            setProgress(progress);
                        });
                        simulator.runSimulation(config);
                        return simulator.getStoredResult(knownPlayers);
                    }
                    
                    @Override
                    protected void process(List<Integer> chunks) {
                        if (!chunks.isEmpty()) {
                            progressBar.setValue(chunks.get(chunks.size() - 1));
                        }
                    }
                    
                    @Override
                    protected void done() {
                        try {
                            SimulationResult result = get();
                            displayResults(result, finalRandomPlayers);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(EquityCalcGUI.this,
                                "Error running simulation: " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                        }
                        progressBar.setVisible(false);
                        runButton.setEnabled(true);
                    }
                };
                
                worker.addPropertyChangeListener(evt -> {
                    if ("progress".equals(evt.getPropertyName())) {
                        progressBar.setValue((Integer)evt.getNewValue());
                    }
                });
                
                worker.execute();
            
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                ex.getMessage(),
                "Invalid Input",
                JOptionPane.WARNING_MESSAGE);
            runButton.setEnabled(true);
        }
    }
    
    private void displayResults(SimulationResult result, int randomPlayers) {
        resultsPanel.removeAll();
        resultsPanel.setLayout(new GridLayout(0, 3, 5, 5));
        
        int playerIndex = 0;
        
        for (int i = 0; i < playerPanels.size(); i++) {
            PlayerPanel panel = playerPanels.get(i);
            if (panel.isActive()) {
                List<Card> cards = panel.getSelectedCards().stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
                    
                JPanel playerResult = new JPanel(new GridLayout(4, 1)); // Changed to 4 rows
                String title = cards.size() == 2 ? 
                    "Player " + (i + 1) : 
                    "Random Player " + (playerIndex - this.knownPlayers.size() + 1);
                playerResult.setBorder(BorderFactory.createTitledBorder(title));
                
                double winProb = result.getWinProbability(playerIndex);
                double[] winCI = result.getWinProbabilityWithConfidence(playerIndex);
                double[] splitCI = result.getSplitProbabilityWithConfidence(playerIndex);
                double[] lossCI = result.getLossProbabilityWithConfidence(playerIndex);
                
                playerResult.add(new JLabel(String.format("Win: %.2f%% (%.2f-%.2f%%)",
                    winProb * 100, winCI[0] * 100, winCI[1] * 100)));
                playerResult.add(new JLabel(String.format("Split: %.2f%% (%.2f-%.2f%%)",
                    result.getSplitProbability(playerIndex) * 100, splitCI[0] * 100, splitCI[1] * 100)));
                playerResult.add(new JLabel(String.format("Lose: %.2f%% (%.2f-%.2f%%)",
                    result.getLossProbability(playerIndex) * 100, lossCI[0] * 100, lossCI[1] * 100)));
                
                // In displayResults method, replace the bet sizing section:
                JPanel actionsPanel = new JPanel(new GridLayout(2, 1));

                // Add bet sizing recommendations
                List<Double> profitableBets = BetSizingRecommender.getProfitableBetSizes(winProb);
                if (!profitableBets.isEmpty()) {
                    StringBuilder sb = new StringBuilder("<html>Profitable bets (pot %): ");
                    sb.append(profitableBets.stream()
                        .map(size -> String.format("%.0f%%", size * 100))
                        .collect(Collectors.joining(", ")));
                    sb.append("</html>");
                    actionsPanel.add(new JLabel(sb.toString()));
                } else {
                    actionsPanel.add(new JLabel("No profitable bets"));
                }

                // Add call sizing recommendations
                List<Double> profitableCalls = BetSizingRecommender.getProfitableCallSizes(winProb);
                if (!profitableCalls.isEmpty()) {
                    StringBuilder sb = new StringBuilder("<html>Profitable calls (pot %): ");
                    sb.append(profitableCalls.stream()
                        .map(size -> String.format("%.0f%%", size * 100))
                        .collect(Collectors.joining(", ")));
                    sb.append("</html>");
                    actionsPanel.add(new JLabel(sb.toString()));
                } else {
                    actionsPanel.add(new JLabel("No profitable calls"));
                }

                playerResult.add(actionsPanel);
                
                resultsPanel.add(playerResult);
                playerIndex++;
            }
        }
        
        resultsPanel.revalidate();
        resultsPanel.repaint();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new EquityCalcGUI().setVisible(true);
        });
    }
}

class PlayerPanel extends JPanel {
    private final CardSelector card1;
    private final CardSelector card2;
    private final JCheckBox activeBox;
    
    public PlayerPanel(String name) {
        setBorder(BorderFactory.createTitledBorder(name));
        setLayout(new FlowLayout(FlowLayout.LEFT));
        
        activeBox = new JCheckBox("Active", true);
        card1 = new CardSelector();
        card2 = new CardSelector();
        
        add(activeBox);
        add(card1);
        add(card2);
    }
    
    public boolean isActive() {
        return activeBox.isSelected();
    }
    
    public List<Card> getSelectedCards() {
        return Arrays.asList(card1.getSelectedCard(), card2.getSelectedCard());
    }
}

class BoardPanel extends JPanel {
    private final List<CardSelector> cardSelectors;
    
    public BoardPanel() {
        setBorder(BorderFactory.createTitledBorder("Board"));
        setLayout(new GridLayout(5, 1));
        
        cardSelectors = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            CardSelector selector = new CardSelector();
            cardSelectors.add(selector);
            add(selector);
        }
    }
    
    public List<Card> getSelectedCards() {
        return cardSelectors.stream()
            .map(CardSelector::getSelectedCard)
            .filter(Objects::nonNull)
            .collect(java.util.stream.Collectors.toList());
    }
}

class CardSelector extends JPanel {
    private final JComboBox<String> rankCombo;
    private final JComboBox<String> suitCombo;
    
    public CardSelector() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
        
        String[] ranks = {"", "2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K", "A"};
        String[] suits = {"", "♣", "♦", "♥", "♠"};
        
        rankCombo = new JComboBox<>(ranks);
        suitCombo = new JComboBox<>(suits);
        
        add(rankCombo);
        add(suitCombo);
    }
    
    public Card getSelectedCard() {
        String rank = (String)rankCombo.getSelectedItem();
        String suit = (String)suitCombo.getSelectedItem();
        
        if (rank.isEmpty() || suit.isEmpty()) {
            return null;
        }
        
        // Convert suit symbol to letter
        char suitChar = switch(suit) {
            case "♣" -> 'c';
            case "♦" -> 'd';
            case "♥" -> 'h';
            case "♠" -> 's';
            default -> ' ';
        };
        
        return new Card(rank + suitChar);
    }
}

class BetSizingRecommender {
    private static final double[] POT_FRACTIONS = {0.25, 0.33, 0.5, 0.66, 0.75, 1.0, 1.5, 2.0, 3.0};
    
    public static List<Double> getProfitableBetSizes(double equity) {
        return Arrays.stream(POT_FRACTIONS)
            .filter(potFraction -> isProfitableToBet(equity, potFraction))
            .boxed()
            .collect(Collectors.toList());
    }
    
    public static List<Double> getProfitableCallSizes(double equity) {
        return Arrays.stream(POT_FRACTIONS)
            .filter(potFraction -> isProfitableToCall(equity, potFraction))
            .boxed()
            .collect(Collectors.toList());
    }
    
    private static boolean isProfitableToBet(double equity, double potFraction) {
        // For betting: Required equity = size/(size + 2*pot)
        double requiredEquity = potFraction / (potFraction + 2.0);
        return equity >= requiredEquity;
    }
    
    private static boolean isProfitableToCall(double equity, double potFraction) {
        // For calling: Required equity = size/(size + pot)
        double requiredEquity = potFraction / (potFraction + 1.0);
        return equity >= requiredEquity;
    }
}