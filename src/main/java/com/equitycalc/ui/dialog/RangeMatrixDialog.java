package com.equitycalc.ui.dialog;

import com.equitycalc.model.Card;
import com.equitycalc.ui.components.button.CornerButton;
import com.equitycalc.ui.components.button.SquareButton;
import com.equitycalc.ui.panel.PlayerPanel;
import com.equitycalc.model.Range;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class RangeMatrixDialog extends JDialog {
    private final PlayerPanel parentPanel;
    public enum Mode {
        COMBINATIONS,  // Standard mode showing combinations
        RANGE         // Range selection mode
    }

    private final Mode mode;
    private final Set<JButton> selectedButtons = new HashSet<>();
    private final JButton saveButton;

    private static final Color BG_COLOR = new Color(28, 28, 30);
    // More muted colors
    private static final Color BTN_PAIRS = new Color(180, 45, 40);     // Darker red
    private static final Color BTN_SUITED = new Color(35, 120, 55);    // Darker green  
    private static final Color BTN_OFFSUIT = new Color(15, 90, 180);   // Darker blue
    private static final Color HIGHLIGHT_BORDER = new Color(255, 255, 255, 60);
    private static final int TILE_SIZE = 60; // Square tiles
    private static final int SPACING = 2; // Minimal spacing
    private static final Font MATRIX_FONT = new Font("Geist-Semibold", Font.PLAIN, 18); // Larger mono font
    
    @FunctionalInterface
    public interface RangeSelectionCallback {
        void onRangeSelected(Card.Rank rank1, Card.Rank rank2, boolean suited);
    }

    private JPanel createMatrixPanel() {
        return createMatrixPanel(null);
    }

    public RangeMatrixDialog(JFrame parent, PlayerPanel playerPanel) {
        super(parent, "Select Hand Range", true);
        this.parentPanel = playerPanel;
        this.mode = Mode.COMBINATIONS; 
        this.saveButton = null;
        
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

    public RangeMatrixDialog(JFrame parent, Consumer<Range> rangeCallback) {
        super(parent, "Select Hand Range", true);
        this.mode = Mode.RANGE;
        this.parentPanel = null;
        
        setBackground(BG_COLOR);
        getRootPane().putClientProperty("apple.awt.windowAppearance", "dark");
        
        JPanel mainContainer = new JPanel(new BorderLayout(SPACING, SPACING));
        mainContainer.setBackground(BG_COLOR);
        mainContainer.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        
        JPanel matrixPanel = createMatrixPanel(null);
        mainContainer.add(matrixPanel, BorderLayout.CENTER);
        
        // Add save button
        saveButton = new JButton("Save Range");
        saveButton.setFont(MATRIX_FONT);
        saveButton.addActionListener(e -> {
            Range range = buildRangeFromSelection();
            rangeCallback.accept(range);
            dispose();
        });
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(BG_COLOR);
        buttonPanel.add(saveButton);
        mainContainer.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainContainer);
        pack();
        setLocationRelativeTo(parent);
        setResizable(true);
    }
    
    private JPanel createMatrixPanel(RangeSelectionCallback callback) {
        JPanel panel = new JPanel(new GridLayout(13, 13, SPACING, SPACING));
        panel.setBackground(BG_COLOR); 
        String[] ranks = {"A", "K", "Q", "J", "T", "9", "8", "7", "6", "5", "4", "3", "2"};
        
        for (int i = 0; i < ranks.length; i++) {
            for (int j = 0; j < ranks.length; j++) {
                final String rank1 = ranks[i];
                final String rank2 = ranks[j];
                String text = rank1 + rank2;
                boolean isPair = i == j;
                boolean isSuited = i < j;
                
                if (isSuited) text += "s";
                else if (!isPair) text += "o";
                
                JButton button;
                
                // Create button with appropriate corner style
                if (i == 0 && j == 0) {
                    button = new CornerButton(text, CornerButton.Corner.TOP_LEFT);
                } else if (i == 0 && j == ranks.length-1) {
                    button = new CornerButton(text, CornerButton.Corner.TOP_RIGHT);
                } else if (i == ranks.length-1 && j == 0) {
                    button = new CornerButton(text, CornerButton.Corner.BOTTOM_LEFT);
                } else if (i == ranks.length-1 && j == ranks.length-1) {
                    button = new CornerButton(text, CornerButton.Corner.BOTTOM_RIGHT);
                } else {
                    button = new SquareButton(text);
                }
    
                final boolean finalIsSuited = isSuited;
                
                // Handle clicks based on mode
                if (mode == Mode.RANGE) {
                    button.addActionListener(e -> {
                        Card.Rank r1 = Card.Rank.fromSymbol(rank1.charAt(0));
                        Card.Rank r2 = Card.Rank.fromSymbol(rank2.charAt(0));
                        
                        if (selectedButtons.contains(button)) {
                            selectedButtons.remove(button);
                            button.setBackground(isPair ? BTN_PAIRS : 
                                              finalIsSuited ? BTN_SUITED : BTN_OFFSUIT);
                        } else {
                            selectedButtons.add(button);
                            button.setBackground(brighten(isPair ? BTN_PAIRS : 
                                              finalIsSuited ? BTN_SUITED : BTN_OFFSUIT));
                        }
                    });
                } else { // COMBINATIONS mode
                    button.addActionListener(e -> {
                        if (callback != null) {
                            Card.Rank r1 = Card.Rank.fromSymbol(rank1.charAt(0));
                            Card.Rank r2 = Card.Rank.fromSymbol(rank2.charAt(0));
                            callback.onRangeSelected(r1, r2, finalIsSuited);
                        } else {
                            showCombinations(rank1, rank2, finalIsSuited);
                        }
                    });
                }
    
                styleButton(button, text, rank1, rank2, isPair, finalIsSuited);
                panel.add(button);
            }
        }
        return panel;
    }
    
    // Update styleButton to remove duplicate action listener
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
                if (!selectedButtons.contains(button)) {
                    button.setBackground(brighten(baseColor));
                    button.setBorder(BorderFactory.createLineBorder(HIGHLIGHT_BORDER, 1));
                    button.setBorderPainted(true);
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (!selectedButtons.contains(button)) {
                    button.setBackground(baseColor);
                    button.setBorderPainted(false);
                }
            }
        });
    }

    private Range buildRangeFromSelection() {
        Range range = new Range();
        
        for (JButton button : selectedButtons) {
            String text = button.getText();
            char rank1 = text.charAt(0);
            char rank2 = text.charAt(1);
            boolean suited = text.endsWith("s");
            
            // Convert ranks to enum for comparison
            Card.Rank r1 = Card.Rank.fromSymbol(rank1);
            Card.Rank r2 = Card.Rank.fromSymbol(rank2);
            
            // For pairs, order doesn't matter but keep consistent
            if (r1 == r2) {
                range.addHand(r1, r2, false); // Pairs are never suited
            } else {
                // For non-pairs, maintain matrix order which is already consistent
                range.addHand(r1, r2, suited);
            }
        }
        
        return range;
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
