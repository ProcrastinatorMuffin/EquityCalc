package com.equitycalc.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Deck {
    public List<Card> cards;
    private long bitMask;
    private static final Random RNG = new Random();
    
    // Full deck bit mask (all 52 cards set)
    public static final long FULL_DECK_MASK;
    static {
        long mask = 0L;
        for (int i = 0; i < 52; i++) {
            mask |= (1L << i);
        }
        FULL_DECK_MASK = mask;
    }

    public Deck() {
        cards = new ArrayList<>();
        bitMask = 0L;
        
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                Card card = new Card(rank, suit);
                cards.add(card);
                bitMask = Card.addCardToBitMask(bitMask, card);
            }
        }
    }
    
    // Create deck from bit mask
    public static Deck fromBitMask(long mask) {
        Deck deck = new Deck();
        deck.cards = Card.bitsToCards(mask);
        deck.bitMask = mask;
        return deck;
    }
    
    public long toBitMask() {
        return bitMask;
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }
    
    // Fisher-Yates shuffle using bit operations
    public void bitShuffle() {
        List<Card> newCards = new ArrayList<>();
        long remainingMask = bitMask;
        
        while (remainingMask != 0) {
            int cardCount = Card.countCards(remainingMask);
            int randomIndex = RNG.nextInt(cardCount);
            
            // Find nth set bit
            int currentBit = 0;
            long mask = remainingMask;
            while (randomIndex > 0) {
                if ((mask & 1L) != 0) {
                    randomIndex--;
                }
                mask >>>= 1;
                currentBit++;
            }
            while ((mask & 1L) == 0) {
                mask >>>= 1;
                currentBit++;
            }
            
            Card card = Card.fromBits(currentBit);
            newCards.add(card);
            remainingMask = Card.removeCardFromBitMask(remainingMask, card);
        }
        
        cards = newCards;
    }

    public Card dealCard() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("No cards left in the deck");
        }
        Card card = cards.remove(cards.size() - 1);
        bitMask = Card.removeCardFromBitMask(bitMask, card);
        return card;
    }

    public List<Card> dealCards(int numberOfCards) {
        if (numberOfCards > cards.size()) {
            throw new IllegalArgumentException("Not enough cards in the deck");
        }
        List<Card> dealtCards = new ArrayList<>();
        for (int i = 0; i < numberOfCards; i++) {
            dealtCards.add(dealCard());
        }
        return dealtCards;
    }
    
    public boolean containsCard(Card card) {
        return Card.isBitSet(bitMask, card);
    }
    
    public int remainingCards() {
        return Card.countCards(bitMask);
    }
    
    public void removeCard(Card card) {
        cards.remove(card);
        bitMask = Card.removeCardFromBitMask(bitMask, card);
    }
    
    public void addCard(Card card) {
        if (Card.isBitSet(bitMask, card)) {
            throw new IllegalArgumentException("Card already in deck: " + card);
        }
        cards.add(card);
        bitMask = Card.addCardToBitMask(bitMask, card);
    }

    @Override
    public String toString() {
        return "Deck{cards=" + cards + "}";
    }
}