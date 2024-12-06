// Create new file PokerHand.java
package com.equitycalc.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PokerHand extends Hand {
    private static final int MAX_CARDS = 7;
    private final List<Card> cards;

    public PokerHand() {
        this.cards = new ArrayList<>();
    }

    @Override
    public void addCard(Card card) {
        if (cards.size() >= MAX_CARDS) {
            throw new IllegalStateException("Cannot add more than " + MAX_CARDS + " cards to a poker hand");
        }
        if (cards.contains(card)) {
            throw new IllegalArgumentException("Duplicate card: " + card);
        }
        cards.add(card);
    }

    @Override
    public void removeCard(Card card) {
        cards.remove(card);
    }

    @Override
    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }
}