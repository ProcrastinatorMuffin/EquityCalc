package com.equitycalc.ui.swing.panel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

public class RoundedPanel extends JPanel {
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
