package com.equitycalc.ui.panel;

import com.equitycalc.model.Card;
import com.equitycalc.simulation.SimulationResult;
import com.equitycalc.ui.dialog.RangeMatrixDialog;
import com.equitycalc.model.Range;
import com.equitycalc.model.RangePresetManager;
import com.equitycalc.ui.components.MacToggle;
import com.equitycalc.ui.components.button.ModeSwitchButton;
import com.equitycalc.ui.components.button.RangeSelectionButton;

import java.awt.*;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
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
    
    private static final int CORNER_RADIUS = 8;
    private static final int PADDING = 12;
    
    public static final float INACTIVE_ALPHA = 0.6f;
    public static final float HOVER_ALPHA = 0.8f;
    public static final float ACTIVE_ALPHA = 1.0f;
    
    private HandDisplayPanel handDisplay;
    private ResultsPanel resultsPanel;
    private final RangePresetManager presetManager = new RangePresetManager();

    private ModeSwitchButton modeSwitchButton;
    private RangeSelectionButton rangeSelectionButton;
    private CardLayout cardLayout;
    private JPanel contentPanel;
    private JButton presetButton;

    private JPanel leftPanel;  // Contains existing hand content
    private JPanel rightPanel; // Will contain results
    private JPanel handContainer; // New field for combined hand+checkbox
    private boolean isExpanded = false;
    private static final int ANIMATION_DURATION = 300; // ms

    private static final Color HERO_BORDER_COLOR = new Color(94, 132, 241);  // Blue tint for hero
    private final boolean isHero;

    // Add new fields
    private Mode currentMode = Mode.HAND;
    private Range playerRange;  // Store the range when in range mode
    private MacToggle modeToggle;

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
        
        // Initialize all panels
        this.leftPanel = new JPanel(new GridBagLayout());
        this.rightPanel = new JPanel();
        this.handContainer = new JPanel(new GridBagLayout());
        
        setLayout(new GridBagLayout());
        setBackground(PANEL_BG);
        
        setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(CORNER_RADIUS, isHero ? HERO_BORDER_COLOR : BORDER_COLOR),
            BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING)
        ));
    
        setupPanels();
        setupHandPanel();
        setupContentPanels();
    }

    private void initializeModeComponents() {
        modeSwitchButton = new ModeSwitchButton();
        modeSwitchButton.addActionListener(e -> toggleMode());
        
        rangeSelectionButton = new RangeSelectionButton(range -> {
            this.playerRange = range;
            // Trigger any additional updates needed
        });
    
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(PANEL_BG);
    }

    private void toggleMode() {
        currentMode = (currentMode == Mode.HAND) ? Mode.RANGE : Mode.HAND;
        cardLayout.show(contentPanel, currentMode.toString());
        updateModeVisibility();
    }
    
    private void updateModeVisibility() {
        rangeSelectionButton.setVisible(currentMode == Mode.RANGE);
        handDisplay.setVisible(currentMode == Mode.HAND);
        presetButton.setVisible(currentMode == Mode.RANGE);
    }

    private void setupPanels() {
        // Setup left panel
        leftPanel.setBackground(PANEL_BG);
        
        // Setup right panel 
        rightPanel.setBackground(PANEL_BG);
        rightPanel.setVisible(false); // Initially hidden
        
        // Setup hand container
        handContainer.setBackground(PANEL_BG);
    }

    private void setupHandPanel() {
        // Replace existing handPanel initialization with:
        handDisplay = new HandDisplayPanel(true);
        handDisplay.addHandClickListener(() -> showRangeDialog());
    }

    private void showRangeDialog() {
        // Use the combinations mode constructor that takes PlayerPanel
        RangeMatrixDialog dialog = new RangeMatrixDialog(
            (JFrame) SwingUtilities.getWindowAncestor(this),
            this  // Pass the PlayerPanel instance
        );
        dialog.setVisible(true);
    }

    private void setupContentPanels() {
        initializeModeComponents();
        
        leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBackground(PANEL_BG);
        leftPanel.setMinimumSize(new Dimension(280, 200)); // Set minimum size
        
        resultsPanel = new ResultsPanel();
        
        // Initialize and add modeToggle with fixed size
        this.modeToggle = new MacToggle();
        modeToggle.setPreferredSize(new Dimension(40, 24));
        GridBagConstraints toggleGbc = new GridBagConstraints();
        toggleGbc.gridx = 0;
        toggleGbc.gridy = 0;
        toggleGbc.weightx = 0;
        toggleGbc.weighty = 0;
        toggleGbc.anchor = GridBagConstraints.NORTHWEST;
        toggleGbc.insets = new Insets(PADDING, PADDING, 0, 0);
        leftPanel.add(modeToggle, toggleGbc);
    
        // Set fixed sizes for components
        handDisplay.setPreferredSize(new Dimension(240, 120));
        handDisplay.setMinimumSize(new Dimension(240, 120));
        
        rangeSelectionButton.setPreferredSize(new Dimension(240, 120));
        rangeSelectionButton.setMinimumSize(new Dimension(240, 120));
        
        // Create content panel with card layout
        contentPanel.add(handDisplay, Mode.HAND.toString());
        contentPanel.add(rangeSelectionButton, Mode.RANGE.toString());
        // Create and store preset button
        presetButton = new JButton("Load Preset");
        presetButton.addActionListener(e -> showPresetMenu(presetButton));
        presetButton.setVisible(currentMode == Mode.RANGE); // Set initial visibility
        
        
        // Layout components with fixed constraints
        GridBagConstraints gbc = new GridBagConstraints();

        // Add to layout near range button
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        leftPanel.add(presetButton, gbc);
        
        // Add content panel
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(PADDING, PADDING, PADDING, PADDING);
        leftPanel.add(contentPanel, gbc);
        
        // Add mode switch button at bottom right with fixed size
        modeSwitchButton.setPreferredSize(new Dimension(80, 32));
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.SOUTHEAST;
        gbc.insets = new Insets(0, PADDING, PADDING, PADDING);
        leftPanel.add(modeSwitchButton, gbc);
        
        // Add panels to main layout with fixed proportions
        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.45;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        add(leftPanel, gbc);
    
        gbc.gridx = 1;
        gbc.weightx = 0.55;
        add(resultsPanel, gbc);
        
        // Set initial mode
        cardLayout.show(contentPanel, Mode.HAND.toString());
    }
    
    private void showPresetMenu(Component source) {
        JPopupMenu menu = new JPopupMenu();
        
        // Add presets
        for (String preset : presetManager.getAvailablePresets()) {
            JMenuItem item = new JMenuItem(preset.replace(".txt", ""));
            item.addActionListener(e -> {
                Range range = presetManager.loadPreset(preset);
                playerRange = range;
            });
            menu.add(item);
        }
        
        menu.addSeparator();
        
        // Add option to open presets folder
        JMenuItem openFolder = new JMenuItem("Open Presets Folder");
        openFolder.addActionListener(e -> presetManager.openPresetsFolder());
        menu.add(openFolder);
        
        menu.show(source, 0, source.getHeight());
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
        resultsPanel.updateResults(result, playerIndex);
        if (!isExpanded) {
            animateExpansion();
        }
    }

    private void animateExpansion() {
        int startWidth = getWidth();
        int targetWidth = (int)(startWidth * 1.5);
        
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
                
                // Maintain fixed proportions during animation
                GridBagConstraints leftGbc = ((GridBagLayout)getLayout()).getConstraints(leftPanel);
                GridBagConstraints rightGbc = ((GridBagLayout)getLayout()).getConstraints(rightPanel);
                
                leftGbc.weightx = 0.45;
                rightGbc.weightx = 0.55;
                
                ((GridBagLayout)getLayout()).setConstraints(leftPanel, leftGbc);
                ((GridBagLayout)getLayout()).setConstraints(rightPanel, rightGbc);
                
                // Force minimum sizes
                leftPanel.setMinimumSize(new Dimension(280, leftPanel.getHeight()));
                rightPanel.setMinimumSize(new Dimension(280, rightPanel.getHeight()));
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

    // Fix isActive logic
    public boolean isActive() {
        // Handle case where modeToggle isn't initialized yet
        if (modeToggle == null) return true;
        return modeToggle.isActive();
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
        handDisplay.updateCards(c1, c2);
    }
    
    /**
     * @return Unmodifiable list of selected cards, empty if no cards selected
     */
    public List<Card> getSelectedCards() {
        Card[] cards = handDisplay.getCards();
        if (cards[0] == null || cards[1] == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(Arrays.asList(cards));
    }

}
