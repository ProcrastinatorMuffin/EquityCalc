package com.equitycalc.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {
    public List<Card> cards;

    public Deck() {
        cards = new ArrayList<>();
        Card.Suit[] suits = Card.Suit.values();
        Card.Rank[] ranks = Card.Rank.values();

        for (Card.Suit suit : suits) {
            for (Card.Rank rank : ranks) {
                cards.add(new Card(rank, suit));
            }
        }
    }

    public void shuffle() {
        Collections.shuffle(cards);
    }

    public Card dealCard() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("No cards left in the deck");
        }
        return cards.remove(cards.size() - 1);
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

    @Override
    public String toString() {
        return "Deck{" +
                "cards=" + cards +
                '}';
    }
}
