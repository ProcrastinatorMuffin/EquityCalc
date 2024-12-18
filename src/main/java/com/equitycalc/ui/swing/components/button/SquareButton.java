package com.equitycalc.ui.swing.components.button;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import javax.swing.JButton;

public class SquareButton extends JButton {
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
