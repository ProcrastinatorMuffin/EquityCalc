package com.equitycalc.ui.components;

import com.equitycalc.ui.theme.AppColors;

import javax.swing.*;
import javax.swing.border.AbstractBorder;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;

public class NumberField extends JTextField {
    private final int min;
    private final int max;
    private boolean isHovered = false;
    private static final int STEP = 1000;

    public static final int NORMAL_STEP = 1000;
    public static final int FINE_STEP = 100;
    public static final int LARGE_STEP = 10000;
    
    private int startValue;
    private int targetValue;

    private Timer colorTimer;
    private Color currentColor;
    private Color targetColor;
    private float colorProgress = 0f;

    private static final int ANIMATION_DURATION = 150; // ms
    private static final float FRAME_TIME = 16f; // 60fps
    private static final float STEP_PROGRESS = FRAME_TIME / ANIMATION_DURATION;
    private static final int ROUNDING_STEP = 1000; // For rounding on key release

    
    private final Timer animationTimer;
    private Timer bounceTimer; // Remove final
    private float animationProgress = 0f;

    private static final int KEY_REPEAT_DELAY = 500; // Initial delay before repeat
    private static final int KEY_REPEAT_INTERVAL = 50; // Interval between repeats
    
    private Timer keyRepeatTimer;
    private int currentStep;
    private boolean isIncrementing;

    private boolean isValidating = false; // Add this field
    
    public NumberField(int value, int min, int max) {
        super(formatNumber(value));
        this.min = min;
        this.max = max;

        setEditable(false);
        setFocusable(false);

        // Setup animation timers
        animationTimer = new Timer((int)FRAME_TIME, e -> {
            animationProgress = Math.min(1f, animationProgress + STEP_PROGRESS);
            int currentValue = (int) lerp(startValue, targetValue, easeOutCubic(animationProgress));
            setText(formatNumber(currentValue));
            
            if (animationProgress >= 1f) {
                ((Timer)e.getSource()).stop();
                animationProgress = 0f;
                validateInput();
            }
        });

        // Initialize color animation
        colorTimer = new Timer((int)FRAME_TIME, e -> {
            colorProgress = Math.min(1f, colorProgress + STEP_PROGRESS);
            if (colorProgress >= 1f) {
                ((Timer)e.getSource()).stop();
                colorProgress = 0f;
            }
            repaint();
        });

        keyRepeatTimer = new Timer(KEY_REPEAT_INTERVAL, e -> {
            if (isIncrementing) {
                animateToValue(Math.min(max, getValue() + currentStep));
            } else {
                animateToValue(Math.max(min, getValue() - currentStep));
            }
        });
        keyRepeatTimer.setInitialDelay(KEY_REPEAT_DELAY);
        
        currentColor = AppColors.TEXT;
        targetColor = AppColors.TEXT;
        
        setHorizontalAlignment(RIGHT);
        setFont(new Font("SF Pro Text", Font.PLAIN, 13));
        updateBackground();
        setForeground(AppColors.TEXT);
        setCaretColor(AppColors.TEXT);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppColors.BORDER),
            BorderFactory.createEmptyBorder(0, 8, 0, 8)
        ));
        
        // Add input validation
        getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update() {
                validateInput();
            }
        });
        
        // Add hover effect
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                updateBackground();
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                updateBackground();
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!isEnabled()) return;
                
                currentStep = NORMAL_STEP;
                if (e.isShiftDown()) currentStep = LARGE_STEP;
                if (e.isAltDown()) currentStep = FINE_STEP;
                
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_UP:
                        isIncrementing = true;
                        animateToValue(Math.min(max, getValue() + currentStep));
                        keyRepeatTimer.start();
                        e.consume();
                        break;
                    case KeyEvent.VK_DOWN:
                        isIncrementing = false;
                        animateToValue(Math.max(min, getValue() - currentStep));
                        keyRepeatTimer.start();
                        e.consume();
                        break;
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN) {
                    keyRepeatTimer.stop();
                    int currentValue = getValue();
                    int rounded = Math.round((float)currentValue / ROUNDING_STEP) * ROUNDING_STEP;
                    rounded = Math.max(min, Math.min(max, rounded));
                    animateToValue(rounded);
                }
            }
        });

        setupBounceEffect(); // Initialize bounce timer
        setupVisuals();
    }

    private void animateColorTransition(Color from, Color to) {
        if (colorTimer.isRunning()) {
            colorTimer.stop();
        }
        
        currentColor = from;
        targetColor = to;
        colorProgress = 0f;
        colorTimer.restart();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (colorTimer.isRunning()) {
            Color blendedColor = new Color(
                (int)lerp(currentColor.getRed(), targetColor.getRed(), easeOutCubic(colorProgress)),
                (int)lerp(currentColor.getGreen(), targetColor.getGreen(), easeOutCubic(colorProgress)),
                (int)lerp(currentColor.getBlue(), targetColor.getBlue(), easeOutCubic(colorProgress))
            );
            super.setForeground(blendedColor); // Use super to avoid triggering property change
        }
    }
    
    private void animateToValue(int newValue) {
        startValue = getValue();
        targetValue = newValue;
        animationProgress = 0f;
        animationTimer.restart();
    }

    private void setupBounceEffect() {
        bounceTimer = new Timer((int)FRAME_TIME, e -> {
            float progress = animationProgress;
            float bounceScale = progress < 0.5f
                ? 1.0f + (0.02f * easeOutCubic(1 - (progress * 2)))  // Bounce out
                : 1.0f + (0.02f * easeOutCubic((progress - 0.5f) * 2)); // Bounce in
            
            setFont(getFont().deriveFont(AffineTransform.getScaleInstance(bounceScale, bounceScale)));
            
            if (progress >= 1f) {
                ((Timer)e.getSource()).stop();
                setFont(getFont().deriveFont(AffineTransform.getScaleInstance(1.0, 1.0)));
            }
            animationProgress += STEP_PROGRESS;
        });
    }
    
    private float lerp(float start, float end, float alpha) {
        return start + (end - start) * alpha;
    }
    
    private float easeOutCubic(float x) {
        return 1 - (float)Math.pow(1 - x, 3);
    }
    
    private void setupVisuals() {
        setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(6, AppColors.BORDER),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
    }
    
    // Custom rounded border
    private static class RoundedBorder extends AbstractBorder {
        private final int radius;
        private final Color color;
        
        RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }
        
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, width-1, height-1, radius, radius);
            g2.dispose();
        }
    }
    
    private void validateInput() {
        if (isValidating) return; // Prevent recursive validation
        
        isValidating = true;
        try {
            int value = parseNumber(getText());
            Color newColor = (value == -1 || value < min || value > max) ? 
                AppColors.ERROR : AppColors.TEXT;
                
            if (!getForeground().equals(newColor)) {
                animateColorTransition(getForeground(), newColor);
            }
        } finally {
            isValidating = false;
        }
    }
    
    private static String formatNumber(int value) {
        return String.format("%,d", value).replace(",", ",");
    }
    
    private static int parseNumber(String text) {
        try {
            // Remove both commas and dots
            String cleaned = text.replaceAll("[,.]", "");
            return Integer.parseInt(cleaned);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    public void setValue(int value) {
        if (value == min || value == max) {
            animationProgress = 0f;
            bounceTimer.restart();
        }
        setText(formatNumber(value));
        validateInput();
    }
    
    private void formatNumber() {
        int value = parseNumber(getText());
        if (value == -1) {
            setValue(min);
        } else {
            value = Math.max(min, Math.min(max, value));
            setValue(value);
        }
    }
    
    public int getValue() {
        int value = parseNumber(getText());
        return value == -1 ? min : value;
    }
    
    private void updateBackground() {
        if (!isEnabled()) {
            setBackground(AppColors.CONTROL_BACKGROUND.darker());
        } else if (isHovered) {
            setBackground(AppColors.CONTROL_BACKGROUND.brighter());
        } else {
            setBackground(AppColors.CONTROL_BACKGROUND);
        }
    }

    private interface SimpleDocumentListener extends javax.swing.event.DocumentListener {
        void update();
        
        @Override
        default void insertUpdate(javax.swing.event.DocumentEvent e) {
            update();
        }
        
        @Override
        default void removeUpdate(javax.swing.event.DocumentEvent e) {
            update();
        }
        
        @Override
        default void changedUpdate(javax.swing.event.DocumentEvent e) {
            update();
        }
    }
}


