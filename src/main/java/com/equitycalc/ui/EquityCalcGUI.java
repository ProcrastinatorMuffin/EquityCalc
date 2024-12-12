package com.equitycalc.ui;

import com.equitycalc.model.*;
import com.equitycalc.simulation.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
// import Path2D
import java.awt.geom.Path2D;
// import AbstractBorder
import javax.swing.border.AbstractBorder;
import javax.swing.border.TitledBorder;


public class EquityCalcGUI extends JFrame {
    private static final int MAX_PLAYERS = 6;
    private final MonteCarloSim simulator = new MonteCarloSim();
    private final List<PlayerPanel> playerPanels = new ArrayList<>();
    private final List<Player> knownPlayers = new ArrayList<>();
    private final JButton runButton = new MacButton("Run Simulation");
    private final JSpinner iterationsSpinner;
    private final JProgressBar progressBar;
    private final BoardPanel boardPanel;
    private final ResultsPanel resultsPanel = new ResultsPanel();
    private boolean resultsExpanded = false;
    
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

class ResultsPanel extends JPanel {
    private static final Color BG_COLOR = new Color(44, 44, 46);
    private static final Color PANEL_BG = new Color(50, 50, 52);
    private final JPanel contentPanel;
    private final ToggleButton toggleButton;
    private boolean expanded = false;
    
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
        expanded = true;
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

class ToggleButton extends JPanel {
    private static final int WIDTH = 40;
    private static final int HEIGHT = 20;
    private static final Color BG_COLOR = new Color(44, 44, 46);
    private static final Color ARROW_COLOR = new Color(200, 200, 200);
    private static final Color HOVER_BG = new Color(54, 54, 56);
    private boolean expanded = false;
    private final JPanel contentPanel; // Store reference to content

    public ToggleButton(JPanel contentPanel) { // Add constructor parameter
        this.contentPanel = contentPanel;
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(BG_COLOR);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                expanded = !expanded;
                contentPanel.setVisible(expanded); // Toggle content directly
                Container parent = getParent();
                if (parent != null) {
                    parent.revalidate();
                    parent.repaint();
                }
                repaint();
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(HOVER_BG);
                repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(BG_COLOR);
                repaint();
            }
        });
    }
    
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw arrow
        g2.setColor(ARROW_COLOR);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int size = 8;
        
        if (expanded) {
            // Down arrow
            g2.drawLine(centerX - size, centerY - size/2, centerX, centerY + size/2);
            g2.drawLine(centerX, centerY + size/2, centerX + size, centerY - size/2);
        } else {
            // Up arrow
            g2.drawLine(centerX - size, centerY + size/2, centerX, centerY - size/2);
            g2.drawLine(centerX, centerY - size/2, centerX + size, centerY + size/2);
        }
        
        g2.dispose();
    }
}

class PlayerPanel extends JPanel {
    private static final Color BG_COLOR = new Color(28, 28, 30);
    private static final Color PANEL_BG = new Color(44, 44, 46);
    private static final Color BORDER_COLOR = new Color(60, 60, 60);
    private static final Color ACCENT_COLOR = new Color(0, 122, 255); // macOS accent blue
    private static final Color BUTTON_BG = new Color(50, 50, 52);
    private static final int CORNER_RADIUS = 8;
    private static final int PADDING = 12;
    private static final Font BUTTON_FONT = new Font("SF Pro Text", Font.PLAIN, 13);
    
    private Card card1;
    private Card card2;
    private final MacCheckBox activeBox;
    private final JButton rangeButton;
    private final HandPanel handPanel;
    
    
    public static final float INACTIVE_ALPHA = 0.6f;
    public static final float HOVER_ALPHA = 0.8f;
    public static final float ACTIVE_ALPHA = 1.0f;
    
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
        
        rangeButton = new MacButton("Select Hand");
        rangeButton.addActionListener(e -> {
            RangeMatrixDialog dialog = new RangeMatrixDialog(
                (JFrame) SwingUtilities.getWindowAncestor(this), this);
            dialog.setVisible(true);
        });
        
        handPanel = new HandPanel(null, null);
        handPanel.setPreferredSize(new Dimension(145, 100));
        
        add(activeBox);
        add(rangeButton);
        add(handPanel);
        
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
        
        // Set initial opacity
        updateOpacityRecursively(this);
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
        Object alpha = getClientProperty("alpha");
        float alphaValue = alpha != null ? ((Number)alpha).floatValue() : ACTIVE_ALPHA;
        
        g2.setComposite(AlphaComposite.getInstance(
            AlphaComposite.SRC_OVER, 
            alphaValue
        ));
        super.paintComponent(g2);
        g2.dispose();
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
    
    public void setCards(Card c1, Card c2) {
        this.card1 = c1;
        this.card2 = c2;
        handPanel.updateCards(c1, c2);
        revalidate();
        repaint();
    }
    
    public boolean isActive() {
        return activeBox.isSelected();
    }
    
    public List<Card> getSelectedCards() {
        if (card1 == null || card2 == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(card1, card2);
    }
}

class MacCheckBox extends JCheckBox {
    private static final int BOX_SIZE = 18;  // Slightly larger
    private static final int BOX_PADDING = 4;
    private static final Color CHECK_COLOR = new Color(0, 122, 255);
    private static final Color BORDER_COLOR = new Color(100, 100, 100);
    private static final Color HOVER_BORDER = new Color(140, 140, 140);
    
    public MacCheckBox() {
        setFont(new Font("Geist-Semibold", Font.PLAIN, 13));
        setForeground(new Color(220, 220, 220)); // Brighter text
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // Propagate hover to parent PlayerPanel
                Container parent = getParent();
                while (parent != null) {
                    if (parent instanceof PlayerPanel) {
                        PlayerPanel playerPanel = (PlayerPanel) parent;
                        if (!playerPanel.isActive()) {
                            playerPanel.updateOpacityRecursively(playerPanel, PlayerPanel.HOVER_ALPHA);
                        }
                        break;
                    }
                    parent = parent.getParent();
                }
                repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                // Reset parent PlayerPanel opacity
                Container parent = getParent();
                while (parent != null) {
                    if (parent instanceof PlayerPanel) {
                        PlayerPanel playerPanel = (PlayerPanel) parent;
                        if (!playerPanel.isActive()) {
                            playerPanel.updateOpacityRecursively(playerPanel, PlayerPanel.INACTIVE_ALPHA);
                        }
                        break;
                    }
                    parent = parent.getParent();
                }
                repaint();
            }
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        
        // Get opacity from parent chain
        Container parent = getParent();
        float alpha = 1.0f;
        while (parent != null) {
            if (parent instanceof JComponent) {
                Object parentAlpha = ((JComponent)parent).getClientProperty("alpha");
                if (parentAlpha instanceof Number) {
                    alpha = ((Number)parentAlpha).floatValue();
                    break;
                }
            }
            parent = parent.getParent();
        }
        
        // Set composite for transparency
        Composite originalComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int x = BOX_PADDING;
        int y = (getHeight() - BOX_SIZE) / 2;
        
        if (isSelected()) {
            // Draw filled background with alpha
            g2.setColor(new Color(
                CHECK_COLOR.getRed(),
                CHECK_COLOR.getGreen(),
                CHECK_COLOR.getBlue(),
                (int)(255 * alpha)
            ));
            g2.fillRoundRect(x, y, BOX_SIZE, BOX_SIZE, 6, 6);
            
            // Draw checkmark with alpha
            g2.setColor(new Color(255, 255, 255, (int)(255 * alpha)));
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 5, y + 9, x + 7, y + 11);
            g2.drawLine(x + 7, y + 11, x + 13, y + 5);
        } else {
            // Draw border with hover effect and alpha
            Color borderColor = getModel().isRollover() ? HOVER_BORDER : BORDER_COLOR;
            g2.setColor(new Color(
                borderColor.getRed(),
                borderColor.getGreen(),
                borderColor.getBlue(),
                (int)(255 * alpha)
            ));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(x, y, BOX_SIZE, BOX_SIZE, 6, 6);
        }
        
        // Draw text with alpha
        g2.setColor(new Color(
            getForeground().getRed(),
            getForeground().getGreen(),
            getForeground().getBlue(),
            (int)(255 * alpha)
        ));
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
                           RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(getText(), 
            x + BOX_SIZE + BOX_PADDING * 2, 
            (getHeight() + fm.getAscent()) / 2 - 1);
            
        g2.setComposite(originalComposite);
        g2.dispose();
    }
}

class MacButton extends JButton {
    private static final Color BUTTON_BG = new Color(58, 58, 60);
    private static final Color HOVER_BG = new Color(68, 68, 70);
    private static final Color PRESSED_BG = new Color(48, 48, 50);
    private static final Color BORDER = new Color(70, 70, 72);
    private static final int CORNER_RADIUS = 10;
    
    public MacButton(String text) {
        super(text);
        setFont(new Font("Geist-Semibold", Font.PLAIN, 13));
        setForeground(new Color(255, 255, 255));
        setBackground(BUTTON_BG);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                // Propagate hover to parent PlayerPanel
                Container parent = getParent();
                while (parent != null) {
                    if (parent instanceof PlayerPanel) {
                        PlayerPanel playerPanel = (PlayerPanel) parent;
                        if (!playerPanel.isActive()) {
                            playerPanel.updateOpacityRecursively(playerPanel, PlayerPanel.HOVER_ALPHA);
                        }
                        break;
                    }
                    parent = parent.getParent();
                }
                setBackground(HOVER_BG);
                repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                // Reset parent PlayerPanel opacity
                Container parent = getParent();
                while (parent != null) {
                    if (parent instanceof PlayerPanel) {
                        PlayerPanel playerPanel = (PlayerPanel) parent;
                        if (!playerPanel.isActive()) {
                            playerPanel.updateOpacityRecursively(playerPanel, PlayerPanel.INACTIVE_ALPHA);
                        }
                        break;
                    }
                    parent = parent.getParent();
                }
                setBackground(BUTTON_BG);
                repaint();
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                setBackground(PRESSED_BG);
                repaint();
            }
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        
        // Get opacity from parent chain
        Container parent = getParent();
        float alpha = 1.0f;
        while (parent != null) {
            if (parent instanceof JComponent) {
                Object parentAlpha = ((JComponent)parent).getClientProperty("alpha");
                if (parentAlpha instanceof Number) {
                    alpha = ((Number)parentAlpha).floatValue();
                    break;
                }
            }
            parent = parent.getParent();
        }
        
        // Set composite for transparency
        Composite originalComposite = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw shadow with alpha
        g2.setColor(new Color(0, 0, 0, (int)(30 * alpha)));
        g2.fillRoundRect(1, 1, getWidth()-1, getHeight()-1, CORNER_RADIUS, CORNER_RADIUS);
        
        // Draw background with subtle gradient and alpha
        Color bgColor = getBackground();
        Color darker = new Color(
            Math.max(0, bgColor.getRed() - 5),
            Math.max(0, bgColor.getGreen() - 5),
            Math.max(0, bgColor.getBlue() - 5),
            (int)(255 * alpha)
        );
        Color startColor = new Color(
            bgColor.getRed(),
            bgColor.getGreen(),
            bgColor.getBlue(),
            (int)(255 * alpha)
        );
        
        GradientPaint gradient = new GradientPaint(
            0, 0, startColor,
            0, getHeight(), darker
        );
        g2.setPaint(gradient);
        g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, CORNER_RADIUS, CORNER_RADIUS);
        
        // Draw subtle border with alpha
        g2.setColor(new Color(
            BORDER.getRed(),
            BORDER.getGreen(),
            BORDER.getBlue(),
            (int)(255 * alpha)
        ));
        g2.setStroke(new BasicStroke(0.8f));
        g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, CORNER_RADIUS, CORNER_RADIUS);
        
        // Draw text with alpha
        g2.setColor(new Color(
            getForeground().getRed(),
            getForeground().getGreen(),
            getForeground().getBlue(),
            (int)(255 * alpha)
        ));
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
                           RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(getText(),
            (getWidth() - fm.stringWidth(getText())) / 2,
            (getHeight() + fm.getAscent()) / 2 - 1);
            
        g2.setComposite(originalComposite);
        g2.dispose();
    }
}

class BoardPanel extends JPanel {
    private static final int CARD_WIDTH = 70;
    private static final int CARD_HEIGHT = 95;
    private static final int CARD_SPACING = 10;
    private static final int FLOP_GROUP_SPACING = 25;
    private static final Color BG_COLOR = new Color(44, 44, 46);
    private static final Color EMPTY_CARD_BG = new Color(50, 50, 52);
    private static final Color EMPTY_CARD_BORDER = new Color(80, 80, 82);
    private static final float[] DASH_PATTERN = {5.0f, 5.0f};
    
    private final List<Card> boardCards = new ArrayList<>(Arrays.asList(null, null, null, null, null));
    
    public BoardPanel() {
        setBackground(BG_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(null, "Board",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                new Font("SF Pro Display", Font.PLAIN, 13),
                Color.WHITE),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        setLayout(new FlowLayout(FlowLayout.CENTER, CARD_SPACING, 0));
        
        // Create flop group (first 3 cards)
        JPanel flopGroup = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintEmptyCards(g, 3);
            }
        };
        flopGroup.setOpaque(false);
        flopGroup.setPreferredSize(new Dimension(CARD_WIDTH * 3 + CARD_SPACING * 2, CARD_HEIGHT));
        flopGroup.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        flopGroup.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showFlopSelector();
            }
        });
        
        // Create turn card
        JPanel turnCard = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintEmptyCard((Graphics2D)g, 0, 0);
            }
        };
        turnCard.setOpaque(false);
        turnCard.setPreferredSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));
        turnCard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        turnCard.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showCardSelector(3); // Turn is index 3
            }
        });
        
        // Create river card
        JPanel riverCard = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                paintEmptyCard((Graphics2D)g, 0, 0);
            }
        };
        riverCard.setOpaque(false);
        riverCard.setPreferredSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));
        riverCard.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        riverCard.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showCardSelector(4); // River is index 4
            }
        });
        
        add(flopGroup);
        add(Box.createHorizontalStrut(FLOP_GROUP_SPACING - CARD_SPACING)); // Extra space after flop
        add(turnCard);
        add(riverCard);
    }
    
    private void paintEmptyCards(Graphics g, int count) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        for (int i = 0; i < count; i++) {
            int x = i * (CARD_WIDTH + CARD_SPACING);
            paintEmptyCard(g2, x, 0);
        }
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
        JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), "Select Flop", true);
        dialog.setBackground(new Color(28, 28, 30));
        dialog.getRootPane().putClientProperty("apple.awt.windowAppearance", "dark");
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(28, 28, 30));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Create card selectors panel
        JPanel selectorsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        selectorsPanel.setBackground(new Color(28, 28, 30));
        
        CardSelector[] selectors = new CardSelector[3];
        for (int i = 0; i < 3; i++) {
            selectors[i] = new CardSelector();
            selectorsPanel.add(selectors[i]);
        }
        
        // Create buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonsPanel.setBackground(new Color(28, 28, 30));
        
        JButton cancelButton = new MacButton("Cancel");
        JButton selectButton = new MacButton("Select");
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        selectButton.addActionListener(e -> {
            // Collect selected cards
            List<Card> selectedCards = Arrays.stream(selectors)
                .map(CardSelector::getSelectedCard)
                .collect(Collectors.toList());
            
            // Validate selections
            if (selectedCards.stream().anyMatch(Objects::isNull)) {
                JOptionPane.showMessageDialog(dialog,
                    "Please select all three cards",
                    "Incomplete Selection",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Check for duplicates
            if (selectedCards.size() != selectedCards.stream().distinct().count()) {
                JOptionPane.showMessageDialog(dialog,
                    "Please select different cards",
                    "Duplicate Cards",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Update board cards
            for (int i = 0; i < 3; i++) {
                boardCards.set(i, selectedCards.get(i));
            }
            
            dialog.dispose();
            repaint();
        });
        
        buttonsPanel.add(cancelButton);
        buttonsPanel.add(selectButton);
        
        mainPanel.add(selectorsPanel, BorderLayout.CENTER);
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        dialog.setVisible(true);
    }
    
    private void showCardSelector(int cardIndex) {
        JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), 
            "Select " + (cardIndex == 3 ? "Turn" : "River"), true);
        dialog.setBackground(new Color(28, 28, 30));
        dialog.getRootPane().putClientProperty("apple.awt.windowAppearance", "dark");
        
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(28, 28, 30));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Create card selector panel
        JPanel selectorPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        selectorPanel.setBackground(new Color(28, 28, 30));
        
        CardSelector selector = new CardSelector();
        selectorPanel.add(selector);
        
        // Create buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonsPanel.setBackground(new Color(28, 28, 30));
        
        JButton cancelButton = new MacButton("Cancel");
        JButton selectButton = new MacButton("Select");
        
        cancelButton.addActionListener(e -> dialog.dispose());
        
        selectButton.addActionListener(e -> {
            Card selectedCard = selector.getSelectedCard();
            
            // Validate selection
            if (selectedCard == null) {
                JOptionPane.showMessageDialog(dialog,
                    "Please select a card",
                    "Incomplete Selection",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Check for duplicates with existing board cards
            List<Card> existingCards = boardCards.stream()
                .filter(c -> c != null && !c.equals(boardCards.get(cardIndex)))
                .collect(Collectors.toList());
                
            if (existingCards.contains(selectedCard)) {
                JOptionPane.showMessageDialog(dialog,
                    "Card already exists on board",
                    "Duplicate Card",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Update board card
            boardCards.set(cardIndex, selectedCard);
            
            dialog.dispose();
            repaint();
        });
        
        buttonsPanel.add(cancelButton);
        buttonsPanel.add(selectButton);
        
        mainPanel.add(selectorPanel, BorderLayout.CENTER);
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
        
        dialog.add(mainPanel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);
        dialog.setVisible(true);
    }
    
    public List<Card> getSelectedCards() {
        return boardCards.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
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

    public void setCard(Card card) {
        if (card == null) {
            rankCombo.setSelectedIndex(0);
            suitCombo.setSelectedIndex(0);
            return;
        }
        
        // Set rank
        rankCombo.setSelectedItem(card.getRank().toString());
        
        // Convert suit to symbol and set
        String suitSymbol = switch(card.getSuit().toString().toLowerCase()) {
            case "clubs" -> "♣";
            case "diamonds" -> "♦";
            case "hearts" -> "♥";
            case "spades" -> "♠";
            default -> "";
        };
        suitCombo.setSelectedItem(suitSymbol);
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

class RangeMatrixDialog extends JDialog {
    private final PlayerPanel parentPanel;

    private static final Color BG_COLOR = new Color(28, 28, 30);
    // More muted colors
    private static final Color BTN_PAIRS = new Color(180, 45, 40);     // Darker red
    private static final Color BTN_SUITED = new Color(35, 120, 55);    // Darker green  
    private static final Color BTN_OFFSUIT = new Color(15, 90, 180);   // Darker blue
    private static final Color HIGHLIGHT_BORDER = new Color(255, 255, 255, 60);
    private static final int TILE_SIZE = 60; // Square tiles
    private static final int SPACING = 2; // Minimal spacing
    private static final int CORNER_RADIUS = 16;
    private static final Font MATRIX_FONT = new Font("Geist-Semibold", Font.PLAIN, 18); // Larger mono font
    
    public RangeMatrixDialog(JFrame parent, PlayerPanel playerPanel) {
        super(parent, "Select Hand Range", true);
        this.parentPanel = playerPanel;
        
        setBackground(BG_COLOR);
        getRootPane().putClientProperty("apple.awt.windowAppearance", "dark");
        
        JPanel mainContainer = new JPanel(new BorderLayout(SPACING, SPACING));
        mainContainer.setBackground(BG_COLOR);
        mainContainer.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16)); // Increased padding
        
        JPanel matrixPanel = createMatrixPanel();
        mainContainer.add(matrixPanel, BorderLayout.CENTER);
        
        add(mainContainer);
        pack();
        setLocationRelativeTo(parent);
        setResizable(true);
    }
    
    private JPanel createMatrixPanel() {
        JPanel panel = new JPanel(new GridLayout(13, 13, SPACING, SPACING));
        panel.setBackground(BG_COLOR);
        String[] ranks = {"A", "K", "Q", "J", "T", "9", "8", "7", "6", "5", "4", "3", "2"};
        
        for (int i = 0; i < ranks.length; i++) {
            for (int j = 0; j < ranks.length; j++) {
                String text = ranks[i] + ranks[j];
                boolean isPair = i == j;
                boolean isSuited = i < j;
                
                if (isSuited) text += "s";
                else if (!isPair) text += "o";
                
                JButton button;
                
                // Determine if this is a corner tile
                if (i == 0 && j == 0) { // Top left
                    button = new CornerButton(text, CornerButton.Corner.TOP_LEFT);
                } else if (i == 0 && j == ranks.length-1) { // Top right
                    button = new CornerButton(text, CornerButton.Corner.TOP_RIGHT);
                } else if (i == ranks.length-1 && j == 0) { // Bottom left
                    button = new CornerButton(text, CornerButton.Corner.BOTTOM_LEFT);
                } else if (i == ranks.length-1 && j == ranks.length-1) { // Bottom right
                    button = new CornerButton(text, CornerButton.Corner.BOTTOM_RIGHT);
                } else {
                    button = new SquareButton(text);
                }
                
                styleButton(button, text, ranks[i], ranks[j], isPair, isSuited);
                panel.add(button);
            }
        }
        return panel;
    }

    private void styleButton(JButton button, String text, String rank1, String rank2, 
                           boolean isPair, boolean isSuited) {
        button.setFont(MATRIX_FONT);
        button.setForeground(Color.WHITE);
        button.setPreferredSize(new Dimension(TILE_SIZE, TILE_SIZE));
        
        Color baseColor = isPair ? BTN_PAIRS : 
                         isSuited ? BTN_SUITED : BTN_OFFSUIT;
        button.setBackground(baseColor);
        
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(brighten(baseColor));
                button.setBorder(BorderFactory.createLineBorder(HIGHLIGHT_BORDER, 1));
                button.setBorderPainted(true);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(baseColor);
                button.setBorderPainted(false);
            }
        });
        
        button.addActionListener(e -> showCombinations(rank1, rank2, isSuited));
    }

    private Color brighten(Color color) {
        return new Color(
            Math.min(255, (int)(color.getRed() * 1.2)),
            Math.min(255, (int)(color.getGreen() * 1.2)),
            Math.min(255, (int)(color.getBlue() * 1.2))
        );
    }

    private List<Card[]> generateCombinations(String rank1, String rank2, boolean suited) {
        List<Card[]> combinations = new ArrayList<>();
        String[] suits = {"s", "h", "d", "c"};
        
        if (suited) {
            // Generate suited combinations
            for (String suit : suits) {
                combinations.add(new Card[]{
                    new Card(rank1 + suit),
                    new Card(rank2 + suit)
                });
            }
        } else {
            // Generate offsuit combinations
            for (String suit1 : suits) {
                for (String suit2 : suits) {
                    if (!suit1.equals(suit2)) {
                        combinations.add(new Card[]{
                            new Card(rank1 + suit1),
                            new Card(rank2 + suit2)
                        });
                    }
                }
            }
        }
        return combinations;
    }
    
    private void showCombinations(String rank1, String rank2, boolean suited) {
        List<Card[]> possibleCombinations = generateCombinations(rank1, rank2, suited);
        CombinationsDialog dialog = new CombinationsDialog(
            (JFrame) SwingUtilities.getWindowAncestor(this),
            parentPanel,
            possibleCombinations
        );
        dispose(); // Close matrix dialog first
        dialog.setVisible(true); // Then show combinations
    }
}

class CornerButton extends JButton {
    enum Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }
    private final Corner corner;
    private static final int CORNER_RADIUS = 10;
    
    public CornerButton(String text, Corner corner) {
        super(text);
        this.corner = corner;
        setContentAreaFilled(false);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        
        // Create path with one rounded corner
        Path2D.Float path = new Path2D.Float();
        int w = getWidth();
        int h = getHeight();
        
        switch (corner) {
            case TOP_LEFT:
                path.moveTo(CORNER_RADIUS, 0);
                path.lineTo(w, 0);
                path.lineTo(w, h);
                path.lineTo(0, h);
                path.lineTo(0, CORNER_RADIUS);
                path.quadTo(0, 0, CORNER_RADIUS, 0);
                break;
            case TOP_RIGHT:
                path.moveTo(0, 0);
                path.lineTo(w-CORNER_RADIUS, 0);
                path.quadTo(w, 0, w, CORNER_RADIUS);
                path.lineTo(w, h);
                path.lineTo(0, h);
                break;
            case BOTTOM_LEFT:
                path.moveTo(0, 0);
                path.lineTo(w, 0);
                path.lineTo(w, h);
                path.lineTo(CORNER_RADIUS, h);
                path.quadTo(0, h, 0, h-CORNER_RADIUS);
                break;
            case BOTTOM_RIGHT:
                path.moveTo(0, 0);
                path.lineTo(w, 0);
                path.lineTo(w, h-CORNER_RADIUS);
                path.quadTo(w, h, w-CORNER_RADIUS, h);
                path.lineTo(0, h);
                break;
        }
        path.closePath();
        
        // Draw background
        g2.setColor(getBackground());
        g2.fill(path);
        
        // Draw border if needed
        if (isBorderPainted() && getBorder() != null) {
            g2.setColor(getForeground());
            g2.draw(path);
        }
        
        // Draw text
        g2.setColor(getForeground());
        FontMetrics fm = g2.getFontMetrics();
        Rectangle textRect = fm.getStringBounds(getText(), g2).getBounds();
        int x = (w - textRect.width) / 2;
        int y = (h - textRect.height) / 2 + fm.getAscent();
        g2.drawString(getText(), x, y);
        
        g2.dispose();
    }
}

class SquareButton extends JButton {
    public SquareButton(String text) {
        super(text);
        setContentAreaFilled(false);
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
                           RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, 
                           RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        
        // Flat background
        if (getModel().isPressed()) {
            g2.setColor(getBackground().darker());
        } else {
            g2.setColor(getBackground());
        }
        g2.fillRect(0, 0, getWidth(), getHeight());
        
        // Border if painted
        if (isBorderPainted() && getBorder() != null) {
            getBorder().paintBorder(this, g2, 0, 0, getWidth(), getHeight());
        }
        
        // Centered text
        g2.setColor(getForeground());
        FontMetrics fm = g2.getFontMetrics();
        Rectangle textRect = fm.getStringBounds(getText(), g2).getBounds();
        int x = (getWidth() - textRect.width) / 2;
        int y = (getHeight() - textRect.height) / 2 + fm.getAscent();
        g2.drawString(getText(), x, y);
        
        g2.dispose();
    }
}

class SimpleCardRenderer {
    private static final int CARD_WIDTH = 70;
    private static final int CARD_HEIGHT = 95;
    private static final int CORNER_RADIUS = 20;
    private static final Font RANK_FONT = new Font("Geist-Semibold", Font.PLAIN, 42);
    private static final Font SUIT_FONT = new Font("Geist-Semibold", Font.PLAIN, 28);
    
    // Solid muted macOS colors
    private static final Color SPADES_BG = new Color(40, 40, 40);    // Dark gray
    private static final Color HEARTS_BG = new Color(200, 55, 45);   // Muted red
    private static final Color DIAMONDS_BG = new Color(20, 100, 200);// Muted blue
    private static final Color CLUBS_BG = new Color(40, 140, 65);    // Muted green
    
    private static final Color TEXT_COLOR = new Color(255, 255, 255);
    private static final float BORDER_WIDTH = 1.5f;
    
        public static void paintCard(Graphics2D g2, Card card, int x, int y) {
        // Store original composite
        Composite originalComposite = g2.getComposite();
        float alpha = 1.0f;
        if (originalComposite instanceof AlphaComposite) {
            alpha = ((AlphaComposite) originalComposite).getAlpha();
        }
    
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        
        if (card == null) {
            Color emptyColor = new Color(60, 60, 60);
            g2.setColor(new Color(emptyColor.getRed(), emptyColor.getGreen(), 
                                 emptyColor.getBlue(), (int)(255 * alpha)));
            g2.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, CORNER_RADIUS, CORNER_RADIUS);
            g2.setComposite(originalComposite);
            return;
        }
        
        Color bgColor = switch(card.getSuit().toString().toLowerCase()) {
            case "spades" -> SPADES_BG;
            case "hearts" -> HEARTS_BG;
            case "diamonds" -> DIAMONDS_BG;
            case "clubs" -> CLUBS_BG;
            default -> Color.GRAY;
        };
        
        // Create gradient with alpha
        Color startColor = new Color(bgColor.getRed(), bgColor.getGreen(), 
                                   bgColor.getBlue(), (int)(255 * alpha));
        Color endColor = new Color(
            (int)(bgColor.getRed() * 0.85),
            (int)(bgColor.getGreen() * 0.85),
            (int)(bgColor.getBlue() * 0.85),
            (int)(255 * alpha)
        );
        
        GradientPaint gradient = new GradientPaint(x, y, startColor, x, y + CARD_HEIGHT, endColor);
        g2.setPaint(gradient);
        g2.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, CORNER_RADIUS, CORNER_RADIUS);
        
        // Draw border for suited cards
        Card otherCard = HandPanel.getCurrentOtherCard(card);
        if (otherCard != null && card.getSuit() == otherCard.getSuit()) {
            g2.setStroke(new BasicStroke(BORDER_WIDTH));
            Color borderColor = new Color(
                Math.min(255, bgColor.getRed() + 40),
                Math.min(255, bgColor.getGreen() + 40),
                Math.min(255, bgColor.getBlue() + 40),
                (int)(255 * alpha)
            );
            g2.setColor(borderColor);
            g2.drawRoundRect(x, y, CARD_WIDTH-1, CARD_HEIGHT-1, CORNER_RADIUS, CORNER_RADIUS);
        }
    
        // Draw text with alpha
        Color textColorWithAlpha = new Color(TEXT_COLOR.getRed(), TEXT_COLOR.getGreen(), 
                                           TEXT_COLOR.getBlue(), (int)(255 * alpha));
        g2.setColor(textColorWithAlpha);
    
        // Draw rank
        g2.setFont(RANK_FONT);
        String rank = switch(card.getRank().toString().toLowerCase()) {
            case "ace" -> "A";
            case "king" -> "K";
            case "queen" -> "Q"; 
            case "jack" -> "J";
            case "ten" -> "T";
            case "nine" -> "9";
            case "eight" -> "8";
            case "seven" -> "7";
            case "six" -> "6";
            case "five" -> "5";
            case "four" -> "4";
            case "three" -> "3";
            case "two" -> "2";
            default -> card.getRank().toString();
        };
        FontMetrics fm = g2.getFontMetrics();
        int rankWidth = fm.stringWidth(rank);
        g2.drawString(rank, x + (CARD_WIDTH - rankWidth) / 2, 
                     y + (CARD_HEIGHT/3) + (fm.getAscent()/2));
        
        // Draw suit symbol
        g2.setFont(SUIT_FONT);
        String suitSymbol = switch(card.getSuit().toString().toLowerCase()) {
            case "spades" -> "♠";
            case "hearts" -> "♥";
            case "diamonds" -> "♦"; 
            case "clubs" -> "♣";
            default -> "";
        };
        FontMetrics sfm = g2.getFontMetrics();
        int suitWidth = sfm.stringWidth(suitSymbol);
        g2.drawString(suitSymbol, x + (CARD_WIDTH - suitWidth) / 2,
                     y + (CARD_HEIGHT*18/24) + (sfm.getAscent()/2));
                     
        // Restore original composite
        g2.setComposite(originalComposite);
    }
}

class HandPanel extends JPanel {
    private static final int CARD_WIDTH = 70;
    private static final int CARD_HEIGHT = 95;
    private static final int CARD_SPACING = -20;
    private static final Color EMPTY_CARD_BG = new Color(50, 50, 52);
    private static final Color EMPTY_CARD_BORDER = new Color(80, 80, 82);
    private static final float[] DASH_PATTERN = {5.0f, 5.0f};
    private static Card currentOtherCard;
    
    private Card card1;
    private Card card2;
    
    public HandPanel(Card c1, Card c2) {
        this.card1 = c1;
        this.card2 = c2;
        setOpaque(false);
        setPreferredSize(new Dimension(CARD_WIDTH * 2 + CARD_SPACING, CARD_HEIGHT));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        
        // Get opacity from parent PlayerPanel
        Container parent = getParent();
        while (parent != null && !(parent instanceof PlayerPanel)) {
            parent = parent.getParent();
        }
        
        if (parent instanceof PlayerPanel) {
            Object alpha = ((JComponent)parent).getClientProperty("alpha");
            float alphaValue = alpha != null ? ((Number)alpha).floatValue() : 1.0f;
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaValue));
        }

        super.paintComponent(g2);
        
        // Paint cards with the same opacity
        if (card2 != null) {
            currentOtherCard = card1;
            SimpleCardRenderer.paintCard(g2, card2, CARD_WIDTH + CARD_SPACING, 0);
        } else {
            paintEmptyCard(g2, CARD_WIDTH + CARD_SPACING, 0);
        }
        
        if (card1 != null) {
            currentOtherCard = card2;
            SimpleCardRenderer.paintCard(g2, card1, 0, 0);
        } else {
            paintEmptyCard(g2, 0, 0);
        }
        
        currentOtherCard = null;
        g2.dispose();
    }
    
    private void paintEmptyCard(Graphics2D g2, int x, int y) {
        // Draw card background
        g2.setColor(EMPTY_CARD_BG);
        g2.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 16, 16);
        
        // Draw dashed border
        g2.setColor(EMPTY_CARD_BORDER);
        BasicStroke dashed = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, 
            BasicStroke.JOIN_ROUND, 0, DASH_PATTERN, 0);
        g2.setStroke(dashed);
        g2.drawRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 16, 16);
        
        // Draw plus symbol
        g2.setStroke(new BasicStroke(1.5f));
        int centerX = x + CARD_WIDTH/2;
        int centerY = y + CARD_HEIGHT/2;
        int size = 14;
        
        g2.drawLine(centerX - size/2, centerY, centerX + size/2, centerY);
        g2.drawLine(centerX, centerY - size/2, centerX, centerY + size/2);
    }

    public static Card getCurrentOtherCard(Card card) {
        return currentOtherCard;
    }
    
    public void updateCards(Card c1, Card c2) {
        this.card1 = c1;
        this.card2 = c2;
        repaint();
    }
}

class CombinationsDialog extends JDialog {
    private static final Color BG_COLOR = new Color(28, 28, 30);
    private static final Color PANEL_COLOR = new Color(44, 44, 46);
    private static final Color BORDER_COLOR = new Color(55, 55, 57);
    private static final Color HIGHLIGHT_COLOR = new Color(94, 132, 241, 200); // Softer blue with transparency
    private static final Color TEXT_COLOR = new Color(255, 255, 255);
    private static final int SPACING = 12;
    private static final int CORNER_RADIUS = 16; // Match HandPanel corner radius
    private final PlayerPanel parentPanel;
    private static final int CONTAINER_PADDING = 8;
    private static final int BORDER_WIDTH = 3;

    public CombinationsDialog(JFrame parent, PlayerPanel playerPanel, List<Card[]> combinations) {
        super(parent, "Select Hand Combination", true);
        this.parentPanel = playerPanel;
        
        // Get screen dimensions for max size
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int maxHeight = screenSize.height - 100; // Leave some margin
        
        // Calculate optimal grid dimensions
        int numCombos = combinations.size();
        // Start with 4 columns, adjust if needed
        int columns = 4;
        int rows = (int)Math.ceil((double)numCombos / columns);
        
        // Get exact HandPanel size
        HandPanel sampleHand = new HandPanel(null, null);
        Dimension handSize = sampleHand.getPreferredSize();
        
        setBackground(BG_COLOR);
        getRootPane().putClientProperty("apple.awt.windowAppearance", "dark");
        
        JPanel mainContainer = new JPanel();
        mainContainer.setLayout(new BorderLayout(SPACING, SPACING));
        mainContainer.setBackground(BG_COLOR);
        mainContainer.setBorder(BorderFactory.createEmptyBorder(SPACING, SPACING, SPACING, SPACING));
        
        // Grid layout with fixed spacing
        JPanel cardsPanel = new JPanel(new GridLayout(rows, columns, SPACING, SPACING));
        cardsPanel.setBackground(BG_COLOR);
        
        for (Card[] combo : combinations) {
            RoundedPanel cardContainer = new RoundedPanel(CORNER_RADIUS);
            cardContainer.setLayout(new BorderLayout());
            cardContainer.setBackground(PANEL_COLOR);
            // Add padding around the hand
            cardContainer.setBorder(BorderFactory.createEmptyBorder(
                CONTAINER_PADDING, CONTAINER_PADDING, 
                CONTAINER_PADDING, CONTAINER_PADDING));
            
            HandPanel handPanel = new HandPanel(combo[0], combo[1]);
            handPanel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            // Center the hand in the container
            JPanel centeringPanel = new JPanel(new GridBagLayout());
            centeringPanel.setOpaque(false);
            centeringPanel.add(handPanel);
            cardContainer.add(centeringPanel, BorderLayout.CENTER);
            
            cardContainer.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    parentPanel.setCards(combo[0], combo[1]);
                    dispose();
                }
                
                @Override
                public void mouseEntered(MouseEvent e) {
                    cardContainer.setBackground(PANEL_COLOR.brighter());
                    cardContainer.setBorderColor(HIGHLIGHT_COLOR);
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    cardContainer.setBackground(PANEL_COLOR);
                    cardContainer.setBorderColor(null);
                }
            });
            
            cardsPanel.add(cardContainer);
        }
        
        // Add empty panels to fill the grid if needed
        int emptyCells = (columns * rows) - numCombos;
        for (int i = 0; i < emptyCells; i++) {
            JPanel emptyPanel = new JPanel();
            emptyPanel.setBackground(BG_COLOR);
            cardsPanel.add(emptyPanel);
        }
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, SPACING, SPACING));
        buttonPanel.setBackground(BG_COLOR);
        
        JButton cancelButton = new JButton("Cancel");
        cancelButton.putClientProperty("JButton.buttonType", "roundRect");
        cancelButton.setForeground(TEXT_COLOR);
        cancelButton.setBackground(PANEL_COLOR);
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelButton);
        
        mainContainer.add(cardsPanel, BorderLayout.CENTER);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainContainer);
        pack();
        
        // If height exceeds screen, adjust columns
        while (getHeight() > maxHeight && columns < 6) {
            columns++;
            rows = (int)Math.ceil((double)numCombos / columns);
            cardsPanel.setLayout(new GridLayout(rows, columns, SPACING, SPACING));
            pack();
        }
        
        setLocationRelativeTo(parent);
        setResizable(false);
    }
}

class RoundedPanel extends JPanel {
    private final int radius;
    private Color borderColor;
    private static final int BORDER_WIDTH = 3;
    
    public RoundedPanel(int radius) {
        super();
        this.radius = radius;
        setOpaque(false);
    }
    
    public void setBorderColor(Color color) {
        this.borderColor = color;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Fill background
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
        
        // Draw border if color set
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(BORDER_WIDTH));
            // Adjust for border width
            g2.drawRoundRect(
                BORDER_WIDTH/2, 
                BORDER_WIDTH/2, 
                getWidth()-BORDER_WIDTH, 
                getHeight()-BORDER_WIDTH, 
                radius, 
                radius
            );
        }
        
        g2.dispose();
    }
}