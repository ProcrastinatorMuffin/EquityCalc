package com.equitycalc.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
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

    @Test
    void newDeckHasCorrectBitMask() {
        assertEquals(Deck.FULL_DECK_MASK, deck.toBitMask());
    }

    @Test
    void bitShufflePreservesAllCards() {
        long originalMask = deck.toBitMask();
        deck.bitShuffle();
        assertEquals(originalMask, deck.toBitMask());
        assertEquals(52, deck.cards.size());
    }

    @Test
    void bitShuffleChangesOrder() {
        List<Card> originalOrder = new ArrayList<>(deck.cards);
        deck.bitShuffle();
        assertFalse(originalOrder.equals(deck.cards));
    }

    @Test
    void fromBitMaskCreatesCorrectDeck() {
        long originalMask = deck.toBitMask();
        Deck newDeck = Deck.fromBitMask(originalMask);
        assertEquals(originalMask, newDeck.toBitMask());
        assertEquals(52, newDeck.cards.size());
    }

    @Test
    void dealCardUpdatesBitMask() {
        Card card = deck.dealCard();
        assertFalse(Card.isBitSet(deck.toBitMask(), card));
        assertEquals(51, Card.countCards(deck.toBitMask()));
    }

    @Test
    void containsCardMatchesBitMask() {
        Card card = deck.dealCard();
        assertFalse(deck.containsCard(card));
        deck.addCard(card);
        assertTrue(deck.containsCard(card));
    }

    @Test
    void remainingCardsMatchesBitCount() {
        assertEquals(52, deck.remainingCards());
        deck.dealCards(10);
        assertEquals(42, deck.remainingCards());
        assertEquals(Card.countCards(deck.toBitMask()), deck.remainingCards());
    }

    @Test
    void removeCardUpdatesBitMaskAndList() {
        Card card = deck.cards.get(0);
        deck.removeCard(card);
        assertFalse(deck.containsCard(card));
        assertFalse(deck.cards.contains(card));
        assertEquals(51, deck.remainingCards());
    }

    @Test
    void addCardUpdatesBitMaskAndList() {
        Card card = deck.dealCard();
        deck.addCard(card);
        assertTrue(deck.containsCard(card));
        assertTrue(deck.cards.contains(card));
        assertEquals(52, deck.remainingCards());
    }

    @Test
    void addDuplicateCardThrows() {
        Card card = new Card(Card.Rank.ACE, Card.Suit.SPADES);
        assertThrows(IllegalArgumentException.class, () -> deck.addCard(card));
    }

    @Test
    void bitMaskConsistencyAfterOperations() {
        // Deal some cards
        List<Card> dealt = deck.dealCards(10);
        
        // Add them back using proper addCard method
        dealt.forEach(card -> deck.addCard(card));
        
        // Verify final state
        assertEquals(52, deck.remainingCards());
        assertEquals(Deck.FULL_DECK_MASK, deck.toBitMask());
    }
}