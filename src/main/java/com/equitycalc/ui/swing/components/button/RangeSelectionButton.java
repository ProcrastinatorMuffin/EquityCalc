package com.equitycalc.ui.swing.components.button;

import com.equitycalc.model.Range;
import com.equitycalc.ui.swing.dialog.RangeMatrixDialog;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

public class RangeSelectionButton extends JButton {
    private static final Color BG_COLOR = new Color(44, 44, 46);
    private static final Color HOVER_COLOR = new Color(54, 54, 56);
    private static final Color PRESSED_COLOR = new Color(64, 64, 66);
    private static final Color TEXT_COLOR = new Color(235, 235, 235);
    private static final int CORNER_RADIUS = 8;
    private static final int SHADOW_SIZE = 4;

    private Color currentBgColor = BG_COLOR;
    private final Consumer<Range> rangeCallback;

    public RangeSelectionButton(Consumer<Range> callback) {
        this.rangeCallback = callback;
        
        setText("Select Range");
        setFont(new Font(".SF NS", Font.PLAIN, 13));
        setForeground(TEXT_COLOR);
        setOpaque(false);
        setBorder(new CompoundBorder(
            new ShadowBorder(),
            BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));
        setFocusPainted(false);
        
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

        addActionListener(e -> {
            JFrame parent = (JFrame) SwingUtilities.getWindowAncestor(this);
            RangeMatrixDialog dialog = new RangeMatrixDialog(parent, rangeCallback);
            dialog.setVisible(true);
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
        
        // Draw text
        FontMetrics fm = g2.getFontMetrics();
        int textX = (getWidth() - fm.stringWidth(getText())) / 2;
        int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
        
        g2.setColor(getForeground());
        g2.drawString(getText(), textX, textY);
        
        g2.dispose();
    }

    private static class ShadowBorder extends AbstractBorder {
        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            for (int i = 0; i < SHADOW_SIZE; i++) {
                float alpha = (SHADOW_SIZE - i) / (float) SHADOW_SIZE * 0.3f;
                g2.setColor(new Color(0, 0, 0, (int)(255 * alpha)));
                g2.setStroke(new BasicStroke(i * 2));
                g2.drawRoundRect(x + SHADOW_SIZE - i, y + SHADOW_SIZE - i, 
                               width - (SHADOW_SIZE - i) * 2, height - (SHADOW_SIZE - i) * 2,
                               CORNER_RADIUS, CORNER_RADIUS);
            }
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(SHADOW_SIZE, SHADOW_SIZE, SHADOW_SIZE, SHADOW_SIZE);
        }
    }

    private static class CompoundBorder extends AbstractBorder {
        private final AbstractBorder shadowBorder;
        private final javax.swing.border.Border paddingBorder;

        public CompoundBorder(AbstractBorder shadowBorder, javax.swing.border.Border paddingBorder) {
            this.shadowBorder = shadowBorder;
            this.paddingBorder = paddingBorder;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            shadowBorder.paintBorder(c, g, x, y, width, height);
            paddingBorder.paintBorder(c, g, x, y, width, height);
        }

        @Override
        public Insets getBorderInsets(Component c) {
            Insets shadow = shadowBorder.getBorderInsets(c);
            Insets padding = paddingBorder.getBorderInsets(c);
            return new Insets(
                shadow.top + padding.top,
                shadow.left + padding.left,
                shadow.bottom + padding.bottom,
                shadow.right + padding.right
            );
        }
    }
}