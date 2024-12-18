package com.equitycalc.ui.swing.panel;

import com.equitycalc.model.Card;
import javax.swing.*;
import java.awt.*;
import javax.swing.event.EventListenerList;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class HandDisplayPanel extends JPanel {
    private static final Color PANEL_BG = new Color(44, 44, 46);
    private final HandPanel handPanel;
    private Card card1;
    private Card card2;
    private final boolean isInteractive;

    public HandDisplayPanel(boolean isInteractive) {
        this.isInteractive = isInteractive;
        
        setLayout(new GridBagLayout());
        setBackground(PANEL_BG);

        // Initialize hand panel
        handPanel = new HandPanel(null, null, HandPanel.Mode.BUTTON);
        handPanel.setPreferredSize(new Dimension(145, 100));
        handPanel.setMinimumSize(new Dimension(145, 100));
        handPanel.setMaximumSize(new Dimension(145, 100));

        if (isInteractive) {
            handPanel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    fireHandClicked();
                }
            });
        }

        // Add hand panel
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        add(handPanel, gbc);
    }

    public void updateCards(Card c1, Card c2) {
        this.card1 = c1;
        this.card2 = c2;
        handPanel.updateCards(c1, c2);
    }

    public Card[] getCards() {
        return new Card[]{card1, card2};
    }

    // Event handling
    private final EventListenerList listenerList = new EventListenerList();

    public interface HandClickListener extends java.util.EventListener {
        void onHandClicked();
    }

    public void addHandClickListener(HandClickListener listener) {
        listenerList.add(HandClickListener.class, listener);
    }

    protected void fireHandClicked() {
        for (HandClickListener listener : listenerList.getListeners(HandClickListener.class)) {
            listener.onHandClicked();
        }
    }
}