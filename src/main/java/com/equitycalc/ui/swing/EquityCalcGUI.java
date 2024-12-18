package com.equitycalc.ui.swing;

import com.equitycalc.model.*;
import com.equitycalc.simulation.*;
import com.equitycalc.ui.swing.panel.BoardPanel;
import com.equitycalc.ui.swing.panel.PlayerPanel;
import com.equitycalc.ui.swing.panel.ControlPanel;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutionException;


public class EquityCalcGUI extends JFrame {
    private final MonteCarloSim simulator;
    private final List<PlayerPanel> playerPanels;
    private final BoardPanel boardPanel;
    private final ControlPanel controlPanel;
    private int simulationBatchSize;

    public EquityCalcGUI() {
        super("Poker Equity Calculator");
        
        // Initialize all final fields first
        this.simulator = new MonteCarloSim();
        this.playerPanels = new ArrayList<>();
        this.boardPanel = new BoardPanel();
        this.controlPanel = new ControlPanel();
        this.simulationBatchSize = 1000;

        // Setup frame properties
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBackground(new Color(28, 28, 30));
        getRootPane().putClientProperty("apple.awt.windowAppearance", "dark");
        
        // Setup content pane
        JPanel contentPane = new JPanel(new BorderLayout(15, 15));
        contentPane.setBackground(new Color(28, 28, 30));
        contentPane.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setContentPane(contentPane);
        
        // Setup top container
        JPanel topContainer = new JPanel(new BorderLayout(10, 15));
        topContainer.setBackground(new Color(28, 28, 30));
        topContainer.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        // Configure control panel
        setupControlPanel();
        
        // Configure board panel
        boardPanel.setPreferredSize(new Dimension(getWidth(), 150));
        boardPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        
        topContainer.add(controlPanel, BorderLayout.NORTH);
        topContainer.add(boardPanel, BorderLayout.CENTER);
        
        // Setup players panel
        setupPlayerPanels(topContainer);
        
        setPreferredSize(new Dimension(1000, 800));
        pack();
        setLocationRelativeTo(null);
    }

    private void setupControlPanel() {
        controlPanel.setSimulationCallback(params -> {
            try {
                List<Player> knownPlayers = new ArrayList<>();
                List<Range> playerRanges = new ArrayList<>();
                List<Card> boardCards = boardPanel.getSelectedCards();
                int randomPlayers = 0;
                
                // Debug logging
                System.out.println("Starting simulation setup:");
                
                for (PlayerPanel panel : playerPanels) {
                    if (panel.isActive()) {
                        System.out.println("Checking active player panel");
                        
                        List<Card> cards = panel.getSelectedCards().stream()
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                            
                        if (cards.size() == 2) {
                            System.out.println("Adding known player with cards: " + cards);
                            knownPlayers.add(new Player(cards));
                        } else {
                            Range range = panel.getRange();
                            if (range != null && !range.getPossibleHands().isEmpty()) {
                                System.out.println("Adding player with range: " + 
                                    range.getPossibleHands().size() + " possible hands");
                                playerRanges.add(range);
                            } else {
                                System.out.println("Counting as random player");
                                randomPlayers++;
                            }
                        }
                    }
                }
    
                System.out.println("Configuration summary:");
                System.out.println("Known players: " + knownPlayers.size());
                System.out.println("Range players: " + playerRanges.size());
                System.out.println("Random players: " + randomPlayers);
    
                SimulationConfig config = SimulationConfig.builder()
                    .withKnownPlayers(knownPlayers)
                    .withPlayerRanges(playerRanges)
                    .withRandomPlayers(randomPlayers)
                    .withBoardCards(boardCards)
                    .withNumSimulations(params.iterations)
                    .build();
                
                runSimulation(config);
                
            } catch (IllegalArgumentException ex) {
                System.err.println("Validation error: " + ex.getMessage());
                JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Invalid Input",
                    JOptionPane.WARNING_MESSAGE);
                controlPanel.setRunButtonEnabled(true);
            }
        });
    }

    private void setupPlayerPanels(JPanel topContainer) {
        JPanel playersMainPanel = new JPanel(new GridLayout(2, 4, 15, 15));
        playersMainPanel.setBackground(new Color(28, 28, 30));
        playersMainPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        
        // Create Hero panel first
        PlayerPanel heroPanel = new PlayerPanel("Hero", true);
        playerPanels.add(heroPanel);
        playersMainPanel.add(heroPanel);
        
        // Create remaining villain panels
        for (int i = 2; i < 5; i++) {
            PlayerPanel playerPanel = new PlayerPanel("Player " + (i + 1), false);
            playerPanels.add(playerPanel);
            playersMainPanel.add(playerPanel);
        }
        
        add(topContainer, BorderLayout.NORTH);
        add(playersMainPanel, BorderLayout.CENTER);
    }

    private void runSimulation(SimulationConfig config) {
        controlPanel.setRunButtonEnabled(false);
        
        try {
            validateSimulationConfig(config);
            
            SwingWorker<SimulationResult, Integer> worker = new SwingWorker<>() {
                @Override
                protected SimulationResult doInBackground() throws Exception {
                    controlPanel.startSimulation();
                    simulator.setProgressCallback(this::setProgress);
                    simulator.runSimulation(config);
                    return simulator.getStoredResult(config.getKnownPlayers());
                }
                
                @Override
                protected void done() {
                    try {
                        SimulationResult result = get();
                        displayResults(result, config.getNumRandomPlayers());
                        controlPanel.completeSimulation();
                    } catch (Exception ex) {
                        handleSimulationError(ex);
                        controlPanel.failSimulation();
                    }
                }
            };
            
            worker.execute();
            
        } catch (IllegalArgumentException ex) {
            handleValidationError(ex);
        }
    }
    
    private void validateSimulationConfig(SimulationConfig config) {
        // Validate board state
        List<Card> boardCards = config.getBoardCards();
        if (!boardCards.isEmpty() && boardCards.size() < 3) {
            throw new IllegalArgumentException(
                "Board must have 0 cards (preflop) or at least 3 cards (flop)"
            );
        }
    
        // Validate total player count including range-based players
        int totalPlayers = config.getKnownPlayers().size() + 
                          config.getPlayerRanges().size() + 
                          config.getNumRandomPlayers();
        if (totalPlayers == 0) {
            throw new IllegalArgumentException("At least one active player required");
        }
    
        // Validate known player hands
        for (Player player : config.getKnownPlayers()) {
            if (player.getHoleCards().size() != 2) {
                throw new IllegalArgumentException(
                    "Each known player must have exactly 2 cards selected"
                );
            }
        }
    
        // Validate ranges are not empty
        for (Range range : config.getPlayerRanges()) {
            if (range == null || range.getPossibleHands().isEmpty()) {
                throw new IllegalArgumentException(
                    "Player ranges must not be empty"
                );
            }
        }
    }
    
    private void handleSimulationError(Exception ex) {
        String message = "Error running simulation: " + ex.getMessage();
        if (ex instanceof InterruptedException) {
            message = "Simulation was interrupted";
        } else if (ex instanceof ExecutionException) {
            message = "Simulation failed: " + ex.getCause().getMessage();
        }
        
        JOptionPane.showMessageDialog(this,
            message,
            "Error",
            JOptionPane.ERROR_MESSAGE);
    }
    
    private void handleValidationError(IllegalArgumentException ex) {
        JOptionPane.showMessageDialog(this,
            ex.getMessage(),
            "Invalid Input",
            JOptionPane.WARNING_MESSAGE);
        controlPanel.setRunButtonEnabled(true);
    }
    
    private void displayResults(SimulationResult result, int randomPlayers) {
        int playerIndex = 0;
        
        for (int i = 0; i < playerPanels.size(); i++) {
            PlayerPanel panel = playerPanels.get(i);
            if (panel.isActive()) {
                List<Card> cards = panel.getSelectedCards().stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
                    
                panel.displayResults(result, playerIndex, cards.size() == 2);
                playerIndex++;
            }
        }
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

