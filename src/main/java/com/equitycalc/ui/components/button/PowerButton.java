package com.equitycalc.ui.components.button;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.nio.file.Path;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;

public class PowerButton extends JButton {
    private static final int DEFAULT_SIZE = 100;
    private boolean isOn;
    private final Path onImagePath;
    private final Path offImagePath;
    private Image onImage;
    private Image offImage;
    private float stateTransition = 0f;
    private Point clickPoint;
    private float rippleScale = 0f;
    private Timer hoverTimer;
    private Timer stateTimer;
    private Timer rippleTimer;
    private float fadeOpacity = 0f;


    public PowerButton(Path onImagePath, Path offImagePath) {
        this.onImagePath = onImagePath;
        this.offImagePath = offImagePath;
        this.isOn = false;
        
        // Set size first
        setPreferredSize(new Dimension(DEFAULT_SIZE, DEFAULT_SIZE));
        setSize(DEFAULT_SIZE, DEFAULT_SIZE);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        
        // Load images after size is set
        loadImages();
        addMouseListeners();
        initializeAnimations();
    }

    private void initializeAnimations() {
        hoverTimer = new Timer(16, null); // ~60fps
        stateTimer = new Timer(16, null);
        rippleTimer = new Timer(16, null);
        
        // Override mouse listeners
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setCursor(new Cursor(Cursor.HAND_CURSOR));
                animateHover(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                animateHover(false);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                clickPoint = e.getPoint();
                startRippleAnimation();
            }
        });
    }

    private void animateHover(boolean hovering) {
        hoverTimer.stop();
        hoverTimer = new Timer(16, e -> {
            float targetOpacity = hovering ? 0.1f : 0f;
            fadeOpacity += (targetOpacity - fadeOpacity) * 0.1f;
            
            if (Math.abs(targetOpacity - fadeOpacity) < 0.001f) {
                hoverTimer.stop();
            }
            repaint();
        });
        hoverTimer.start();
    }

    private void startRippleAnimation() {
        rippleTimer.stop();
        rippleScale = 0f;
        rippleTimer = new Timer(16, e -> {
            rippleScale += 0.1f;
            if (rippleScale >= 1f) {
                rippleTimer.stop();
                rippleScale = 0f;
            }
            repaint();
        });
        rippleTimer.start();
    }

    private void animateStateChange() {
        stateTimer.stop();
        stateTimer = new Timer(16, e -> {
            float target = isOn ? 1f : 0f;
            stateTransition += (target - stateTransition) * 0.2f;
            
            if (Math.abs(target - stateTransition) < 0.001f) {
                stateTimer.stop();
            }
            repaint();
        });
        stateTimer.start();
    }

    private void loadImages() {
        try {
            onImage = loadSVG(onImagePath);
            offImage = loadSVG(offImagePath);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load images for PowerButton: " + e.getMessage());
        }
    }

    private BufferedImage loadSVG(Path path) throws Exception {
        TranscoderInput input = new TranscoderInput(path.toUri().toString());
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        TranscoderOutput output = new TranscoderOutput(outputStream);
        
        PNGTranscoder transcoder = new PNGTranscoder();
        
        // Double the resolution for better quality
        float width = (getWidth() > 0 ? getWidth() : DEFAULT_SIZE) * 2;
        float height = (getHeight() > 0 ? getHeight() : DEFAULT_SIZE) * 2;
        
        transcoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, width);
        transcoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, height);
        
        transcoder.transcode(input, output);
        
        byte[] imgData = outputStream.toByteArray();
        return ImageIO.read(new ByteArrayInputStream(imgData));
    }

    private void addMouseListeners() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                setBorder(BorderFactory.createLoweredBevelBorder());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                setBorder(BorderFactory.createRaisedBevelBorder());
            }
        });
    }

    public void toggleState() {
        isOn = !isOn;
        animateStateChange();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        
        // Draw base image
        Image currentImage = isOn ? onImage : offImage;
        if (currentImage != null) {
            g2d.drawImage(currentImage, 0, 0, getWidth(), getHeight(), this);
        }
        
        // Draw hover fade
        if (fadeOpacity > 0) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fadeOpacity));
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
        
        // Keep ripple effect
        if (rippleScale > 0 && clickPoint != null) {
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 
                Math.max(0, 0.3f - rippleScale * 0.3f)));
            int rippleSize = (int)(Math.max(getWidth(), getHeight()) * rippleScale * 2);
            g2d.setColor(new Color(255, 255, 255, 100));
            g2d.fillOval(
                clickPoint.x - rippleSize/2, 
                clickPoint.y - rippleSize/2, 
                rippleSize, 
                rippleSize
            );
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return super.getPreferredSize();
    }

    @Override
    public void setSize(Dimension d) {
        super.setSize(d);
        if (d.width > 0 && d.height > 0) {
            loadImages();
        }
    }

    @Override
    public void setSize(int width, int height) {
        super.setSize(width, height);
        if (width > 0 && height > 0) {
            loadImages();
        }
    }

    public void setButtonSize(int width, int height) {
        setPreferredSize(new Dimension(width, height));
        setSize(width, height);
        revalidate();
        repaint();
    }

    // Test the PowerButton
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Power Button Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 400);
            frame.setLayout(new BorderLayout());
    
            // Create panel to center the button
            JPanel centerPanel = new JPanel(new GridBagLayout());
            frame.add(centerPanel, BorderLayout.CENTER);
    
            // Create status label
            JLabel statusLabel = new JLabel("Power: OFF", SwingConstants.CENTER);
            frame.add(statusLabel, BorderLayout.SOUTH);
    
            // Create and configure power button
            Path onPath = Path.of("/Users/procrastinatormuffin/dev/assets/PowerButton(ON).svg");
            Path offPath = Path.of("/Users/procrastinatormuffin/dev/assets/PowerButton(OFF).svg");
            PowerButton powerButton = new PowerButton(onPath, offPath);
            powerButton.setButtonSize(100, 100);
    
            // Add action listener to update status label
            powerButton.addActionListener(e -> {
                PowerButton btn = (PowerButton)e.getSource();
                btn.toggleState(); // This will handle both state change and repaint
                statusLabel.setText("Power: " + (btn.isOn ? "ON" : "OFF"));
            });
    
            // Add button to center panel
            centerPanel.add(powerButton);
    
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
