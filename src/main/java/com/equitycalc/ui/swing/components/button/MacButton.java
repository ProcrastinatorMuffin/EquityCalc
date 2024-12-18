package com.equitycalc.ui.swing.components.button;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JComponent;

import com.equitycalc.ui.swing.panel.PlayerPanel;

public class MacButton extends JButton {
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
