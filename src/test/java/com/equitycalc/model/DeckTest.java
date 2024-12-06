package com.equitycalc.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;



public class DeckTest {

    private Deck deck;

    @BeforeEach
    void setUp() {
        deck = new Deck();
    }

    @Test
    void newDeckHas52Cards() {
        List<Card> dealtCards = deck.dealCards(52);
        assertEquals(52, dealtCards.size());
    }

    @Test
    void newDeckHasAllUniqueCards() {
        List<Card> dealtCards = deck.dealCards(52);
        Set<Card> uniqueCards = new HashSet<>(dealtCards);
        assertEquals(52, uniqueCards.size());
    }

    @Test
    void dealCardRemovesOneCard() {
        Card card = deck.dealCard();
        assertNotNull(card);
        List<Card> remainingCards = deck.dealCards(51);
        assertEquals(51, remainingCards.size());
    }

    @Test
    void dealCardsReturnsRequestedNumber() {
        List<Card> dealtCards = deck.dealCards(5);
        assertEquals(5, dealtCards.size());
    }

    @Test
    void dealCardThrowsWhenEmpty() {
        deck.dealCards(52); // Empty the deck
        assertThrows(IllegalStateException.class, () -> deck.dealCard());
    }

    @Test
    void dealCardsThrowsWhenNotEnoughCards() {
        deck.dealCards(50); // Leave 2 cards
        assertThrows(IllegalArgumentException.class, () -> deck.dealCards(3));
    }

    @Test
    void shuffleChangesCardOrder() {
        // Deal all cards from an unshuffled deck
        List<Card> unshuffledCards = deck.dealCards(52);
        
        // Create new deck and shuffle it
        Deck shuffledDeck = new Deck();
        shuffledDeck.shuffle();
        List<Card> shuffledCards = shuffledDeck.dealCards(52);

        // Check that cards are not in the same order
        // Note: There's a tiny chance this could fail even with a proper shuffle
        assertFalse(unshuffledCards.equals(shuffledCards));
    }

    @Test
    void toStringContainsCardCount() {
        String deckString = deck.toString();
        assertTrue(deckString.contains("cards=["));
        assertTrue(deckString.contains("]"));
    }

    @Test
    void newDeckContainsAllSuitsAndRanks() {
        List<Card> allCards = deck.dealCards(52);
        
        // Check if deck contains all combinations of suits and ranks
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                final Card.Suit expectedSuit = suit;
                final Card.Rank expectedRank = rank;
                assertTrue(allCards.stream()
                    .anyMatch(card -> card.getSuit() == expectedSuit && card.getRank() == expectedRank));
            }
        }
    }
}