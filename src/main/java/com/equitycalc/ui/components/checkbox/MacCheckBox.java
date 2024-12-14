package com.equitycalc.ui.components.checkbox;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JCheckBox;
import javax.swing.JComponent;

import com.equitycalc.ui.panel.PlayerPanel;


public class MacCheckBox extends JCheckBox {
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
