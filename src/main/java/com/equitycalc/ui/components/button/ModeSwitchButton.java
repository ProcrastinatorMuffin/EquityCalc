package com.equitycalc.ui.components.button;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;

public class ModeSwitchButton extends JButton {
    private static final Color BG_COLOR = new Color(44, 44, 46);
    private static final Color HOVER_COLOR = new Color(54, 54, 56);
    private static final Color PRESSED_COLOR = new Color(64, 64, 66);
    private static final Color ICON_COLOR = new Color(235, 235, 235);
    
    private static final int BUTTON_SIZE = 32;
    private static final int CORNER_RADIUS = 6;
    private Color currentBgColor = BG_COLOR;

    public ModeSwitchButton() {
        setPreferredSize(new Dimension(BUTTON_SIZE, BUTTON_SIZE));
        setOpaque(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                currentBgColor = HOVER_COLOR;
                repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                currentBgColor = BG_COLOR;
                repaint();
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                currentBgColor = PRESSED_COLOR;
                repaint();
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                currentBgColor = isMouseOver(e) ? HOVER_COLOR : BG_COLOR;
                repaint();
            }
        });
    }

    private boolean isMouseOver(MouseEvent e) {
        return getBounds().contains(e.getPoint());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw background
        g2.setColor(currentBgColor);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), CORNER_RADIUS, CORNER_RADIUS);
        
        // Draw switch icon
        int padding = 8;
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        
        g2.setColor(ICON_COLOR);
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        // Create curved arrows path
        Path2D path = new Path2D.Float();
        
        // Top arrow
        path.moveTo(centerX - 6, centerY - 4);
        path.curveTo(
            centerX - 2, centerY - 4,  // control point 1
            centerX - 2, centerY - 4,  // control point 2
            centerX + 2, centerY - 4   // end point
        );
        // Arrow head
        path.lineTo(centerX, centerY - 6);
        path.moveTo(centerX + 2, centerY - 4);
        path.lineTo(centerX, centerY - 2);
        
        // Bottom arrow
        path.moveTo(centerX + 6, centerY + 4);
        path.curveTo(
            centerX + 2, centerY + 4,  // control point 1
            centerX + 2, centerY + 4,  // control point 2
            centerX - 2, centerY + 4   // end point
        );
        // Arrow head
        path.lineTo(centerX, centerY + 6);
        path.moveTo(centerX - 2, centerY + 4);
        path.lineTo(centerX, centerY + 2);
        
        g2.draw(path);
        g2.dispose();
    }
}