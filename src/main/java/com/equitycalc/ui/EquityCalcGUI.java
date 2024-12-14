package com.equitycalc.ui;

import com.equitycalc.model.*;
import com.equitycalc.simulation.*;
import com.equitycalc.ui.components.button.MacButton;
import com.equitycalc.ui.panel.BoardPanel;
import com.equitycalc.ui.panel.PlayerPanel;
import com.equitycalc.ui.panel.ResultsPanel;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;


public class EquityCalcGUI extends JFrame {
    private final MonteCarloSim simulator = new MonteCarloSim();
    private final List<PlayerPanel> playerPanels = new ArrayList<>();
    private final JButton runButton = new MacButton("Run Simulation");
    private final JSpinner iterationsSpinner;
    private final JProgressBar progressBar;
    private final BoardPanel boardPanel;
    private final ResultsPanel resultsPanel = new ResultsPanel();
    
    public EquityCalcGUI() {
        super("Poker Equity Calculator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBackground(new Color(28, 28, 30));
        getRootPane().putClientProperty("apple.awt.windowAppearance", "dark"); // Enable dark mode
        
        // Main layout with spacing
        JPanel contentPane = new JPanel(new BorderLayout(15, 15)); // Increased spacing
        contentPane.setBackground(new Color(28, 28, 30));
        contentPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setContentPane(contentPane);
        
        // Top container
        JPanel topContainer = new JPanel(new BorderLayout(10, 15)); // More vertical spacing
        topContainer.setBackground(new Color(28, 28, 30));
        topContainer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        // Control panel with run controls
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        controlPanel.setBackground(new Color(44, 44, 46));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));
        
        // Style iterations spinner
        iterationsSpinner = new JSpinner(new SpinnerNumberModel(50000, 1000, 1000000, 1000));
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(iterationsSpinner, "#,###");
        iterationsSpinner.setEditor(editor);
        JFormattedTextField textField = ((JSpinner.DefaultEditor)editor).getTextField();
        textField.setBackground(new Color(60, 60, 64));
        textField.setForeground(Color.WHITE);
        textField.setCaretColor(Color.WHITE);
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(80, 80, 84)),
            BorderFactory.createEmptyBorder(2, 5, 2, 5)
        ));
        
        JLabel iterationsLabel = new JLabel("Iterations:");
        iterationsLabel.setForeground(Color.WHITE);
        iterationsLabel.setFont(new Font("Geist-Semibold", Font.PLAIN, 13));
        
        // Add run controls
        controlPanel.add(iterationsLabel);
        controlPanel.add(iterationsSpinner);
        controlPanel.add(Box.createHorizontalStrut(15));
        controlPanel.add(runButton);
        
        // Style progress bar
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        progressBar.setBackground(new Color(60, 60, 64));
        progressBar.setForeground(new Color(0, 122, 255));
        controlPanel.add(Box.createHorizontalStrut(15));
        controlPanel.add(progressBar);
        
        // Add run button handler
        runButton.addActionListener(e -> runSimulation());
        
        // Board panel with increased height
        boardPanel = new BoardPanel();
        boardPanel.setPreferredSize(new Dimension(getWidth(), 150)); // Increased height
        boardPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0)); // Remove title
        
        topContainer.add(controlPanel, BorderLayout.NORTH);
        topContainer.add(boardPanel, BorderLayout.CENTER);
        
        // Players panel
        JPanel playersMainPanel = new JPanel(new GridLayout(2, 4, 15, 15)); // Increased gaps
        playersMainPanel.setBackground(new Color(28, 28, 30));
        playersMainPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        for (int i = 0; i < 4; i++) {
            PlayerPanel playerPanel = new PlayerPanel("Player " + (i + 1));
            playerPanels.add(playerPanel);
            playersMainPanel.add(playerPanel);
        }
        
        add(topContainer, BorderLayout.NORTH);
        add(playersMainPanel, BorderLayout.CENTER);
        add(resultsPanel, BorderLayout.SOUTH);
        
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
        resultsPanel.displayResults(result, randomPlayers, playerPanels);
    }
    
    class RoundedPanel extends JPanel {
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
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new EquityCalcGUI().setVisible(true);
        });
    }
}

