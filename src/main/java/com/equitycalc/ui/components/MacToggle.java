package com.equitycalc.ui.components;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import com.equitycalc.ui.panel.PlayerPanel;

public class MacToggle extends JPanel {
    private static final Color TOGGLE_BG = new Color(50, 50, 52);
    private static final Color TOGGLE_SLIDER = new Color(235, 235, 235);
    private static final Color TOGGLE_ACTIVE = new Color(94, 132, 241);
    private static final int TOGGLE_HEIGHT = 24;
    private static final int TOGGLE_WIDTH = 50;
    private static final int HOVER_RADIUS = 50;
    private static final int FPS = 144;
    private static final int FRAME_TIME = 1000 / FPS;
    private static final int ANIMATION_DURATION = 180;

    private boolean isActive = true;
    private final int width = TOGGLE_WIDTH;
    private final int height = TOGGLE_HEIGHT;
    private float sliderPosition = 1.0f;
    private float opacity = 0.3f;
    private Timer animationTimer;
    private Timer fadeTimer;
    private AWTEventListener globalMouseListener;

    public MacToggle() {
        setPreferredSize(new Dimension(width, height));
        setMinimumSize(new Dimension(width, height));
        setMaximumSize(new Dimension(width, height));
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        setupMouseListeners();
        setupGlobalMouseTracking();
    }

    private void setupMouseListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                isActive = !isActive;
                animateToggle(isActive);
                Container parent = getParent();
                while (parent != null && !(parent instanceof PlayerPanel)) {
                    parent = parent.getParent();
                }
                if (parent instanceof PlayerPanel) {
                    ((PlayerPanel) parent).updateOpacityRecursively(parent);
                }
                repaint();
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                animateOpacity(1.0f);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                Point p = e.getPoint();
                SwingUtilities.convertPointToScreen(p, MacToggle.this);
                if (!isMouseNear(p)) {
                    animateOpacity(0.3f);
                }
            }
        });
    }

    private void setupGlobalMouseTracking() {
        globalMouseListener = event -> {
            if (event instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) event;
                if (me.getID() == MouseEvent.MOUSE_MOVED) {
                    Point mousePoint = me.getLocationOnScreen();
                    boolean isNear = isMouseNear(mousePoint);
                    
                    if (isNear && opacity < 1.0f) {
                        animateOpacity(1.0f);
                    } else if (!isNear && opacity > 0.3f) {
                        animateOpacity(0.3f);
                    }
                }
            }
        };

        Toolkit.getDefaultToolkit().addAWTEventListener(
            globalMouseListener, 
            AWTEvent.MOUSE_MOTION_EVENT_MASK
        );

        addAncestorListener(new AncestorListener() {
            public void ancestorAdded(AncestorEvent event) {}
            public void ancestorMoved(AncestorEvent event) {}
            public void ancestorRemoved(AncestorEvent event) {
                Toolkit.getDefaultToolkit().removeAWTEventListener(globalMouseListener);
            }
        });
    }

    public boolean isMouseNear(Point mousePoint) {
        if (!isShowing()) return false;
        
        try {
            // Use relative coordinates instead
            Point point = MouseInfo.getPointerInfo().getLocation();
            SwingUtilities.convertPointFromScreen(point, this);
            
            Rectangle bounds = getBounds();
            int distance = (int) point.distance(
                bounds.getX() + bounds.getWidth()/2,
                bounds.getY() + bounds.getHeight()/2
            );
            
            return distance < HOVER_RADIUS;
        } catch (Exception e) {
            // Fallback if we can't get coordinates
            return false;
        }
    }

    private void animateOpacity(float targetOpacity) {
        if (fadeTimer != null && fadeTimer.isRunning()) {
            fadeTimer.stop();
        }
        
        opacity = opacity + (targetOpacity - opacity) * 0.3f;
        repaint();
        
        final float startOpacity = opacity;
        final long startTime = System.nanoTime();
        
        fadeTimer = new Timer(FRAME_TIME, e -> {
            long elapsed = (System.nanoTime() - startTime) / 1_000_000L;
            float progress = Math.min(1f, (float)elapsed / ANIMATION_DURATION);
            float easedProgress = easeOutExpo(progress);
            
            opacity = startOpacity + (targetOpacity - startOpacity) * easedProgress;
            
            if (progress >= 1) {
                fadeTimer.stop();
                opacity = targetOpacity;
            }
            repaint();
        });
        
        fadeTimer.setCoalesce(false);
        fadeTimer.setRepeats(true);
        fadeTimer.start();
    }

    private void animateToggle(boolean targetState) {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
        
        float targetPos = targetState ? 1.0f : 0.0f;
        sliderPosition = sliderPosition + (targetPos - sliderPosition) * 0.3f;
        repaint();
        
        final float startPos = sliderPosition;
        final long startTime = System.nanoTime();
        
        animationTimer = new Timer(FRAME_TIME, e -> {
            long elapsed = (System.nanoTime() - startTime) / 1_000_000L;
            float progress = Math.min(1f, (float)elapsed / ANIMATION_DURATION);
            float easedProgress = easeOutExpo(progress);
            
            sliderPosition = startPos + (targetPos - startPos) * easedProgress;
            
            if (progress >= 1) {
                animationTimer.stop();
                sliderPosition = targetPos;
            }
            repaint();
        });
        
        animationTimer.setCoalesce(false);
        animationTimer.setRepeats(true);
        animationTimer.start();
    }

    private float easeOutExpo(float x) {
        return x == 1 ? 1 : 1 - (float)Math.pow(2, -10 * x);
    }

    public boolean isActive() {
        return isActive;
    }
    
    @Override 
    public boolean isOpaque() {
        return false;
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        
        int cornerRadius = height;
        RoundRectangle2D toggleShape = new RoundRectangle2D.Float(
            0, 0, width - 1, height - 1, cornerRadius, cornerRadius
        );
        
        g2.setClip(toggleShape);
        
        g2.setColor(sliderPosition > 0 ? TOGGLE_ACTIVE : TOGGLE_BG);
        g2.fill(toggleShape);
        
        g2.setColor(new Color(0, 0, 0, 30));
        g2.draw(toggleShape);
        
        int sliderDiameter = height - 4;
        int sliderX = 2 + (int)((width - sliderDiameter - 4) * sliderPosition);
        
        g2.setColor(new Color(0, 0, 0, 30));
        g2.fillOval(sliderX + 1, 3, sliderDiameter, sliderDiameter);
        
        g2.setColor(TOGGLE_SLIDER);
        g2.fillOval(sliderX, 2, sliderDiameter, sliderDiameter);
        
        g2.dispose();
    }
}