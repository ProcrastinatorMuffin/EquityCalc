package com.equitycalc.ui.swing.components.button;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

public class RunButton extends JButton {
    // macOS-like colors
    private static final Color ACCENT = new Color(0, 122, 255);
    private static final Color ACCENT_HOVER = new Color(0, 111, 233);
    private static final Color ACCENT_PRESSED = new Color(0, 99, 208);
    private static final Color DISABLED = new Color(128, 128, 128, 80);
    
    private final int size;
    private boolean isHovered = false;
    private boolean isPressed = false;
    private boolean isRunning = false;
    private float animationProgress = 0f;
    private final Timer animationTimer;
    private final Timer spinTimer;
    private float spinAngle = 0f;
    
    private static final int ANIMATION_DURATION = 200;
    private static final float SPIN_SPEED = 2f;

    public RunButton(int size) {
        this.size = size;
        
        setPreferredSize(new Dimension(size, size));
        setBorder(new EmptyBorder(4, 4, 4, 4));
        setContentAreaFilled(false);
        setFocusPainted(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Main state animation timer
        animationTimer = new Timer(16, e -> {
            float step = 16f / ANIMATION_DURATION;
            if (isRunning) {
                animationProgress = Math.min(1f, animationProgress + step);
            } else {
                animationProgress = Math.max(0f, animationProgress - step);
            }
            if (animationProgress == 0f || animationProgress == 1f) {
                ((Timer)e.getSource()).stop();
            }
            repaint();
        });
        
        // Continuous spin animation timer
        spinTimer = new Timer(16, e -> {
            spinAngle += SPIN_SPEED;
            if (spinAngle >= 360f) {
                spinAngle = 0f;
            }
            repaint();
        });
        
        setupListeners();
    }

    private void setupListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    isHovered = true;
                    repaint();
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                isPressed = false;
                repaint();
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    isPressed = true;
                    repaint();
                }
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                isPressed = false;
                repaint();
            }
        });
    }

    public void setRunning(boolean running) {
        if (this.isRunning != running) {
            this.isRunning = running;
            animationTimer.restart();
            
            if (running) {
                spinTimer.start();
            } else {
                spinTimer.stop();
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw background
        Color bgColor;
        if (!isEnabled()) {
            bgColor = DISABLED;
        } else if (isPressed) {
            bgColor = ACCENT_PRESSED;
        } else if (isHovered) {
            bgColor = ACCENT_HOVER;
        } else {
            bgColor = ACCENT;
        }
        
        g2.setColor(bgColor);
        g2.fillOval(1, 1, size - 2, size - 2);

        // Draw icon with smooth transition
        g2.setColor(Color.WHITE);
        if (isRunning) {
            drawLoadingSpinner(g2);
        } else {
            drawPlayIcon(g2, animationProgress);
        }
        
        g2.dispose();
    }

    private void drawLoadingSpinner(Graphics2D g2) {
        int strokeWidth = size / 12;
        int diameter = size - (strokeWidth * 4);
        
        g2.setStroke(new BasicStroke(strokeWidth, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        // Save original transform
        AffineTransform originalTransform = g2.getTransform();
        
        // Center and rotate
        g2.translate(size/2, size/2);
        g2.rotate(Math.toRadians(spinAngle));
        
        // Draw spinner arc
        g2.setColor(new Color(255, 255, 255, 180));
        g2.drawArc(-diameter/2, -diameter/2, diameter, diameter, 0, 300);
        
        // Restore transform
        g2.setTransform(originalTransform);
    }

    private void drawPlayIcon(Graphics2D g2, float progress) {
        int padding = size / 4;
        int centerX = size / 2;
        int centerY = size / 2;
        
        // Create play triangle path
        Path2D playPath = new Path2D.Float();
        playPath.moveTo(padding + 2, padding);
        playPath.lineTo(padding + 2, size - padding);
        playPath.lineTo(size - padding, centerY);
        playPath.closePath();
        
        g2.fill(playPath);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        repaint();
    }

    public boolean isRunning() {
        return isRunning;
    }
}