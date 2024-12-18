package com.equitycalc.ui.swing.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import com.equitycalc.model.Card;
import com.equitycalc.ui.swing.panel.HandPanel;
import com.equitycalc.ui.swing.panel.PlayerPanel;
import com.equitycalc.ui.swing.panel.RoundedPanel;

public class CombinationsDialog extends JDialog {
    private static final Color BG_COLOR = new Color(28, 28, 30);
    private static final Color PANEL_COLOR = new Color(44, 44, 46);
    private static final Color HIGHLIGHT_COLOR = new Color(94, 132, 241, 200); // Softer blue with transparency
    private static final Color TEXT_COLOR = new Color(255, 255, 255);
    private static final int SPACING = 12;
    private static final int CORNER_RADIUS = 16; // Match HandPanel corner radius
    private final PlayerPanel parentPanel;
    private static final int CONTAINER_PADDING = 8;

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
            
            HandPanel handPanel = new HandPanel(combo[0], combo[1], HandPanel.Mode.DISPLAY);
            cardContainer.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            
            // Center the hand in the container
            JPanel centeringPanel = new JPanel(new GridBagLayout());
            centeringPanel.setOpaque(false);
            centeringPanel.add(handPanel);
            cardContainer.add(centeringPanel, BorderLayout.CENTER);
            
            MouseAdapter mouseAdapter = new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    parentPanel.setCards(combo[0], combo[1]);
                    dispose();
                }
                
                @Override
                public void mouseEntered(MouseEvent e) {
                    cardContainer.setBackground(PANEL_COLOR.brighter());
                    cardContainer.setBorderColor(HIGHLIGHT_COLOR);
                    handPanel.setHovered(true);
                }
                
                @Override
                public void mouseExited(MouseEvent e) {
                    cardContainer.setBackground(PANEL_COLOR);
                    cardContainer.setBorderColor(null);
                    handPanel.setHovered(false);
                }
            };
            
            cardContainer.addMouseListener(mouseAdapter);
        
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
