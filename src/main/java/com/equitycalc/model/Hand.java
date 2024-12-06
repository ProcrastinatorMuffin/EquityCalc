package com.equitycalc.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Hand {
    public List<Card> cards;
    private long bitMask;

    public Hand() {
        cards = new ArrayList<>();
        bitMask = 0L;
    }

    // Create hand from bit mask
    public static Hand fromBitMask(long mask) {
        if (Card.countCards(mask) > 2) {
            throw new IllegalArgumentException("Hand cannot contain more than 2 cards");
        }
        Hand hand = new Hand();
        hand.cards = Card.bitsToCards(mask);
        hand.bitMask = mask;
        return hand;
    }

    public void addCard(Card card) {
        if (cards.size() >= 2) {
            throw new IllegalStateException("Cannot add more than 2 cards to a hand");
        }
        if (Card.isBitSet(bitMask, card)) {
            throw new IllegalArgumentException("Duplicate card: " + card);
        }
        cards.add(card);
        bitMask = Card.addCardToBitMask(bitMask, card);
    }

    public void removeCard(Card card) {
        cards.remove(card);
        bitMask = Card.removeCardFromBitMask(bitMask, card);
    }

    public List<Card> getCards() {
        return Collections.unmodifiableList(cards);
    }

    public long toBitMask() {
        return bitMask;
    }

    public boolean containsCard(Card card) {
        return Card.isBitSet(bitMask, card);
    }

    public int getCardCount() {
        return Card.countCards(bitMask);
    }

    public void clear() {
        cards.clear();
        bitMask = 0L;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Card card : cards) {
            sb.append(card.toString()).append(" ");
        }
        return sb.toString().trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Hand hand = (Hand) o;
        return bitMask == hand.bitMask;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(bitMask);
    }
}
