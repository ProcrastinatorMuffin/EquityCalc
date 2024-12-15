package com.equitycalc.ui.panel;

import com.equitycalc.model.Card;
import com.equitycalc.ui.util.CardSelector;
import com.equitycalc.ui.util.SimpleCardRenderer;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.stream.*;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.geom.AffineTransform;
// import atomic integer
import java.util.concurrent.atomic.AtomicInteger;

public class BoardPanel extends JPanel {
    private static final int CARD_WIDTH = 70;
    private static final int CARD_HEIGHT = 95;
    private static final int CARD_SPACING = 10;
    private static final int FLOP_GROUP_SPACING = 25;
    private static final Color BG_COLOR = new Color(44, 44, 46);
    private static final Color EMPTY_CARD_BG = new Color(50, 50, 52);
    private static final Color EMPTY_CARD_BORDER = new Color(80, 80, 82);
    private static final float[] DASH_PATTERN = {5.0f, 5.0f};
    
    private final List<Card> boardCards = new ArrayList<>(Arrays.asList(null, null, null, null, null));
    private static final int ANIMATION_FPS = 144; // Increase from 60 to 144
    private static final int ANIMATION_FRAME_TIME = 1000 / ANIMATION_FPS;
    private static final float HOVER_SCALE = 1.05f;
    private static final float SCALE_STEP = 0.004f;
    private final float[] cardScales = new float[5];
    private final javax.swing.Timer hoverTimer;
    private int hoveredIndex = -1;
    private boolean isHoveringFlop = false;
    private long lastUpdateTime = 0;

    public BoardPanel() {
        setDoubleBuffered(true); // Enable hardware acceleration
        Arrays.fill(cardScales, 1.0f);
        
        // Improved animation timer
        hoverTimer = new javax.swing.Timer(ANIMATION_FRAME_TIME, e -> {
            boolean needsRepaint = false;
            long currentTime = System.nanoTime();
            float delta = (currentTime - lastUpdateTime) / 1_000_000_000.0f;
            float step = SCALE_STEP * (1000.0f / ANIMATION_FRAME_TIME) * delta;
            
            for (int i = 0; i < cardScales.length; i++) {
                float targetScale = (i < 3 && isHoveringFlop) || (i == hoveredIndex) 
                    ? HOVER_SCALE : 1.0f;
                
                if (Math.abs(cardScales[i] - targetScale) < step) {
                    cardScales[i] = targetScale;
                } else if (cardScales[i] < targetScale) {
                    cardScales[i] = Math.min(cardScales[i] + step, targetScale);
                    needsRepaint = true;
                } else if (cardScales[i] > targetScale) {
                    cardScales[i] = Math.max(cardScales[i] - step, targetScale);
                    needsRepaint = true;
                }
            }
            
            lastUpdateTime = currentTime;
            if (!needsRepaint) {
                ((javax.swing.Timer)e.getSource()).stop();
            }
            repaint();
        });

        setBackground(BG_COLOR);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(null, "Board",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                new Font("SF Pro Display", Font.PLAIN, 13),
                Color.WHITE),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        
        setPreferredSize(new Dimension(
            5 * CARD_WIDTH + 4 * CARD_SPACING + FLOP_GROUP_SPACING,
            CARD_HEIGHT + 20)); // Add some padding for the border
            
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int x = e.getX();
                int y = e.getY();
                
                int totalCardsWidth = 5 * CARD_WIDTH + 4 * CARD_SPACING + FLOP_GROUP_SPACING;
                int startX = (getWidth() - totalCardsWidth) / 2;
                int startY = (getHeight() - CARD_HEIGHT) / 2;
                
                x -= startX;
                y -= startY;
                
                if (y < 0 || y > CARD_HEIGHT) return;
                
                if (x < 3 * (CARD_WIDTH + CARD_SPACING)) {
                    int cardIndex = x / (CARD_WIDTH + CARD_SPACING);
                    if (cardIndex < 3) {
                        showFlopSelector();
                    }
                } else {
                    x -= 3 * (CARD_WIDTH + CARD_SPACING) + FLOP_GROUP_SPACING - CARD_SPACING;
                    int cardIndex = 3 + (x / (CARD_WIDTH + CARD_SPACING));
                    if (cardIndex < 5) {
                        showCardSelector(cardIndex);
                    }
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                isHoveringFlop = false;
                hoveredIndex = -1;
                hoverTimer.restart();
            }
        });    

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateHoverState(e.getX(), e.getY());
            }
        });
        
    }

    private void updateHoverState(int mouseX, int mouseY) {
        int totalCardsWidth = 5 * CARD_WIDTH + 4 * CARD_SPACING + FLOP_GROUP_SPACING;
        int startX = (getWidth() - totalCardsWidth) / 2;
        int startY = (getHeight() - CARD_HEIGHT) / 2;
        
        mouseX -= startX;
        mouseY -= startY;

        boolean changed = false;
        boolean newHoveringFlop = false;
        int newHoveredIndex = -1;
        
        if (mouseY >= 0 && mouseY <= CARD_HEIGHT) {
            if (mouseX < 3 * (CARD_WIDTH + CARD_SPACING)) {
                newHoveringFlop = true;
            } else {
                mouseX -= 3 * (CARD_WIDTH + CARD_SPACING) + FLOP_GROUP_SPACING - CARD_SPACING;
                int index = 3 + (mouseX / (CARD_WIDTH + CARD_SPACING));
                if (index < 5 && mouseX >= 0) newHoveredIndex = index;
            }
        }
        
        if (newHoveringFlop != isHoveringFlop || newHoveredIndex != hoveredIndex) {
            isHoveringFlop = newHoveringFlop;
            hoveredIndex = newHoveredIndex;
            lastUpdateTime = System.nanoTime();
            if (!hoverTimer.isRunning()) {
                hoverTimer.restart();
            }
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
        AtomicInteger flopIndex = new AtomicInteger(0);
        while (flopIndex.get() < 3) {
            JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), 
                "Select Flop Card " + (flopIndex.get() + 1), true);
            dialog.setBackground(new Color(28, 28, 30));
            dialog.getRootPane().putClientProperty("apple.awt.windowAppearance", "dark");
            
            Card selectedCard = CardSelector.showDialog(this, "Select Flop Card " + (flopIndex.get() + 1));
            if (selectedCard != null) {
                // Validate against existing cards
                List<Card> existingCards = boardCards.stream()
                    .filter(c -> c != null && !c.equals(boardCards.get(flopIndex.get())))
                    .collect(Collectors.toList());
                    
                if (!existingCards.contains(selectedCard)) {
                    boardCards.set(flopIndex.get(), selectedCard);
                    repaint();
                    flopIndex.incrementAndGet();
                } else {
                    JOptionPane.showMessageDialog(this,
                        "Card already exists on board",
                        "Duplicate Card",
                        JOptionPane.WARNING_MESSAGE);
                    // Don't increment, retry same position
                }
            } else {
                break; // User cancelled
            }
        }
    }
    
    private void showCardSelector(int cardIndex) {
        JDialog dialog = new JDialog((JFrame) SwingUtilities.getWindowAncestor(this), 
            "Select " + (cardIndex == 3 ? "Turn" : "River"), true);
        dialog.setBackground(new Color(28, 28, 30));
        dialog.getRootPane().putClientProperty("apple.awt.windowAppearance", "dark");
        
        // Use single card selector
        Card selectedCard = CardSelector.showDialog(this, 
            "Select " + (cardIndex == 3 ? "Turn" : "River"));
        if (selectedCard != null) {
            // Validate against existing cards
            List<Card> existingCards = boardCards.stream()
                .filter(c -> c != null && !c.equals(boardCards.get(cardIndex)))
                .collect(Collectors.toList());
                
            if (!existingCards.contains(selectedCard)) {
                boardCards.set(cardIndex, selectedCard);
                repaint();
            } else {
                JOptionPane.showMessageDialog(this,
                    "Card already exists on board",
                    "Duplicate Card",
                    JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        
        // Enable better rendering
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        
        int totalCardsWidth = 5 * CARD_WIDTH + 4 * CARD_SPACING + FLOP_GROUP_SPACING;
        int startX = (getWidth() - totalCardsWidth) / 2;
        int startY = (getHeight() - CARD_HEIGHT) / 2;
        
        // Create a single transform instance to reuse
        AffineTransform originalTransform = g2.getTransform();
        
        for (int i = 0; i < boardCards.size(); i++) {
            int x = startX + i * (CARD_WIDTH + CARD_SPACING);
            if (i >= 3) x += FLOP_GROUP_SPACING - CARD_SPACING;
            
            // Optimized scale transform
            if (cardScales[i] != 1.0f) {
                g2.translate(x + CARD_WIDTH/2, startY + CARD_HEIGHT/2);
                g2.scale(cardScales[i], cardScales[i]);
                g2.translate(-(x + CARD_WIDTH/2), -(startY + CARD_HEIGHT/2));
            }
            
            Card card = boardCards.get(i);
            if (card != null) {
                SimpleCardRenderer.paintCard(g2, card, x, startY);
            } else {
                paintEmptyCard(g2, x, startY);
            }
            
            g2.setTransform(originalTransform);
        }
    }
    
    public List<Card> getSelectedCards() {
        return boardCards.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
