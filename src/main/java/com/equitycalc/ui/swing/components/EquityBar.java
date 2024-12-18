package com.equitycalc.ui.swing.components;

import javax.swing.*;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Area;

public class EquityBar extends JPanel {
    private double winPercentage;
    private double splitPercentage;
    private double losePercentage;
    private boolean isNullState;
    private static final float RELATIVE_CORNER_RADIUS = 0.4f;

    public EquityBar() {
        this.winPercentage = 0;
        this.splitPercentage = 0;
        this.losePercentage = 0;
        this.isNullState = true;
        
        setPreferredSize(new Dimension(187, 15));
        setOpaque(false);
    }

    private int getCornerRadius() {
        return (int)(getHeight() * RELATIVE_CORNER_RADIUS);
    }

    public void setEquity(double winPercentage, double splitPercentage, double losePercentage) {
        this.winPercentage = winPercentage;
        this.splitPercentage = splitPercentage;
        this.losePercentage = losePercentage;
        this.isNullState = false;
        repaint();
    }

    public void resetToNullState() {
        this.isNullState = true;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        double screenScale = getGraphicsConfiguration().getDefaultTransform().getScaleX();
        
        // Create an even higher resolution buffer
        BufferedImage bufferedImage = new BufferedImage(
            (int)(getWidth() * screenScale * 2), 
            (int)(getHeight() * screenScale * 2), 
            BufferedImage.TYPE_INT_ARGB
        );
        
        Graphics2D g2d = bufferedImage.createGraphics();
        
        // Set maximum quality rendering hints
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Scale for higher resolution rendering
        g2d.scale(screenScale * 2, screenScale * 2);
        
        // Enable stroke normalization
        g2d.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        
        if (isNullState) {
            drawNullState(g2d);
        } else {
            drawEquityState(g2d);
        }
        
        g2d.dispose();
        
        // Multi-pass downscaling for better quality
        BufferedImage intermediate = new BufferedImage(
            (int)(getWidth() * screenScale), 
            (int)(getHeight() * screenScale), 
            BufferedImage.TYPE_INT_ARGB
        );
        
        Graphics2D g2dIntermediate = intermediate.createGraphics();
        g2dIntermediate.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2dIntermediate.drawImage(bufferedImage, 0, 0, intermediate.getWidth(), intermediate.getHeight(), null);
        g2dIntermediate.dispose();
        
        // Final draw to screen
        Graphics2D g2 = (Graphics2D)g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.drawImage(intermediate, 0, 0, getWidth(), getHeight(), null);
    }

    private void drawNullState(Graphics2D g2d) {
        g2d.setColor(Color.decode("#52525B"));
        RoundRectangle2D.Float roundRect = new RoundRectangle2D.Float(
            0f, 0f, getWidth(), getHeight(),
            getCornerRadius(), getCornerRadius()
        );
        g2d.setColor(Color.decode("#52525B"));
        g2d.fill(roundRect);
    
        g2d.setColor(Color.decode("#FAFAFA"));
        g2d.setFont(new Font("Inter", Font.PLAIN, 10));
        FontMetrics metrics = g2d.getFontMetrics();
        String text = "?";
        int x = (getWidth() - metrics.stringWidth(text)) / 2;
        int y = ((getHeight() - metrics.getHeight()) / 2) + metrics.getAscent();
        g2d.drawString(text, x, y);
    }

    private void drawEquityState(Graphics2D g2d) {
        float width = getWidth();
        float height = getHeight();
        float cornerRadius = getCornerRadius();
        
        // Create outer shape first
        RoundRectangle2D.Float baseRect = new RoundRectangle2D.Float(
            0f, 0f, width, height,
            cornerRadius, cornerRadius
        );
        
        // Create a composite area for better antialiasing
        Area totalArea = new Area(baseRect);
        
        // Calculate widths with extra precision
        float x = 0;
        float winWidth = width * (float)(winPercentage / 100.0);
        float splitWidth = width * (float)(splitPercentage / 100.0);
        float loseWidth = width - winWidth - splitWidth;
        
        // Draw sections using Areas instead of direct rectangles
        Area winArea = new Area(new Rectangle2D.Float(x, 0, winWidth, height));
        winArea.intersect(totalArea);
        g2d.setColor(Color.decode("#22C55E"));
        g2d.fill(winArea);
        
        if (winWidth > 30) {
            drawCenteredText(g2d, String.format("%.1f%%", winPercentage), (int)x, (int)winWidth, (int)height);
        }
        x += winWidth;
        
        Area splitArea = new Area(new Rectangle2D.Float(x, 0, splitWidth, height));
        splitArea.intersect(totalArea);
        g2d.setColor(Color.decode("#F59E0B"));
        g2d.fill(splitArea);
        x += splitWidth;
        
        Area loseArea = new Area(new Rectangle2D.Float(x, 0, loseWidth, height));
        loseArea.intersect(totalArea);
        g2d.setColor(Color.decode("#EF4444"));
        g2d.fill(loseArea);
        
        if (loseWidth > 30) {
            drawCenteredText(g2d, String.format("%.1f%%", losePercentage), (int)x, (int)loseWidth, (int)height);
        }
    }

    private void drawCenteredText(Graphics2D g2d, String text, int x, int width, int height) {
        g2d.setColor(Color.decode("#FAFAFA")); // White text color
        g2d.setFont(new Font("Inter", Font.PLAIN, 10)); // Use Inter font, 10pt
        FontMetrics metrics = g2d.getFontMetrics();
        int textX = x + (width - metrics.stringWidth(text)) / 2;
        int textY = ((height - metrics.getHeight()) / 2) + metrics.getAscent();
        g2d.drawString(text, textX, textY);
    }

    // Test the EquityBar
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Equity Bar Test");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JPanel marginPanel = new JPanel();
            marginPanel.setLayout(new BorderLayout()); // Use BorderLayout
            marginPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            marginPanel.setBackground(Color.decode("#18181B"));
            
            EquityBar equityBar = new EquityBar();
            marginPanel.add(equityBar, BorderLayout.CENTER); // Center the bar
            frame.add(marginPanel);

            frame.setSize(600, 150);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            equityBar.setEquity(31.4, 2.1, 66.5);
        });
    }
}

