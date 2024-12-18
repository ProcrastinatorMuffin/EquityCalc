package com.equitycalc.ui.swing.components.button;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class ToggleButton extends JPanel {
    private static final int WIDTH = 40;
    private static final int HEIGHT = 20;
    private static final Color BG_COLOR = new Color(44, 44, 46);
    private static final Color ARROW_COLOR = new Color(200, 200, 200);
    private static final Color HOVER_BG = new Color(54, 54, 56);
    private boolean expanded = false;

    public ToggleButton(JPanel contentPanel) { // Add constructor parameter
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(BG_COLOR);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                expanded = !expanded;
                contentPanel.setVisible(expanded); // Toggle content directly
                Container parent = getParent();
                if (parent != null) {
                    parent.revalidate();
                    parent.repaint();
                }
                repaint();
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(HOVER_BG);
                repaint();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(BG_COLOR);
                repaint();
            }
        });
    }
    
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // Draw arrow
        g2.setColor(ARROW_COLOR);
        g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int size = 8;
        
        if (expanded) {
            // Down arrow
            g2.drawLine(centerX - size, centerY - size/2, centerX, centerY + size/2);
            g2.drawLine(centerX, centerY + size/2, centerX + size, centerY - size/2);
        } else {
            // Up arrow
            g2.drawLine(centerX - size, centerY + size/2, centerX, centerY - size/2);
            g2.drawLine(centerX, centerY - size/2, centerX + size, centerY + size/2);
        }
        
        g2.dispose();
    }
}
