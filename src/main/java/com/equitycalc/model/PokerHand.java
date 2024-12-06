package com.equitycalc.model;

import java.util.List;

public class PokerHand extends Hand {
    private static final int MAX_CARDS = 7;
    private long bitMask;

    public PokerHand() {
        super();
        this.bitMask = 0L;
    }

    // Create poker hand from bit mask
    public static PokerHand fromBitMask(long mask) {
        if (Card.countCards(mask) > MAX_CARDS) {
            throw new IllegalArgumentException("Cannot create poker hand with more than " + MAX_CARDS + " cards");
        }
        PokerHand hand = new PokerHand();
        hand.bitMask = mask;
        List<Card> cards = Card.bitsToCards(mask);
        cards.forEach(card -> hand.cards.add(card));
        return hand;
    }

    @Override
    public void addCard(Card card) {
        if (getCardCount() >= MAX_CARDS) {
            throw new IllegalStateException("Cannot add more than " + MAX_CARDS + " cards to a poker hand");
        }
        if (Card.isBitSet(bitMask, card)) {
            throw new IllegalArgumentException("Duplicate card: " + card);
        }
        cards.add(card);
        bitMask = Card.addCardToBitMask(bitMask, card);
    }

    @Override
    public void removeCard(Card card) {
        cards.remove(card);
        bitMask = Card.removeCardFromBitMask(bitMask, card);
    }

    @Override 
    public long toBitMask() {
        return bitMask;
    }

    @Override
    public boolean containsCard(Card card) {
        return Card.isBitSet(bitMask, card);
    }

    @Override
    public int getCardCount() {
        return Card.countCards(bitMask);
    }

    @Override
    public void clear() {
        cards.clear();
        bitMask = 0L;
    }

    // Poker-specific bit operations
    public long getSuitBitMask(Card.Suit suit) {
        long mask = 0L;
        for (Card card : cards) {
            if (card.getSuit() == suit) {
                mask = Card.addCardToBitMask(mask, card);
            }
        }
        return mask;
    }

    public long getRankBitMask(Card.Rank rank) {
        long mask = 0L;
        for (Card card : cards) {
            if (card.getRank() == rank) {
                mask = Card.addCardToBitMask(mask, card);
            }
        }
        return mask;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PokerHand hand = (PokerHand) o;
        return bitMask == hand.bitMask;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(bitMask);
    }
}