package com.equitycalc.ui.swing.components.button;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import javax.swing.JButton;
// import Rectangle;
import java.awt.Rectangle;

public class CornerButton extends JButton {
    public enum Corner { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }
    public final Corner corner;
    public static final int CORNER_RADIUS = 10;
    
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
