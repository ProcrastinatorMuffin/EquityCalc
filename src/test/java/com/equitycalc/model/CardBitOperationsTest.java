package com.equitycalc.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import com.equitycalc.model.Card.Rank;
import com.equitycalc.model.Card.Suit;

public class CardBitOperationsTest {

    @Test
    void toBitsIsConsistent() {
        Card card = new Card(Rank.ACE, Suit.SPADES);
        int bits = card.toBits();
        Card reconstructed = Card.fromBits(bits);
        assertEquals(card, reconstructed);
    }

    @ParameterizedTest
    @MethodSource("allPossibleCards")
    void allCardsBitsAreUnique(Card card1, Card card2) {
        if (!card1.equals(card2)) {
            assertNotEquals(card1.toBits(), card2.toBits());
        }
    }

    @Test
    void bitMaskOperations() {
        Card aceSpades = new Card(Rank.ACE, Suit.SPADES);
        Card kingHearts = new Card(Rank.KING, Suit.HEARTS);
        
        long mask = 0L;
        mask = Card.addCardToBitMask(mask, aceSpades);
        
        assertTrue(Card.isBitSet(mask, aceSpades));
        assertFalse(Card.isBitSet(mask, kingHearts));
        
        mask = Card.addCardToBitMask(mask, kingHearts);
        assertTrue(Card.isBitSet(mask, kingHearts));
        
        mask = Card.removeCardFromBitMask(mask, aceSpades);
        assertFalse(Card.isBitSet(mask, aceSpades));
        assertTrue(Card.isBitSet(mask, kingHearts));
    }

    @Test
    void countCardsInBitMask() {
        long mask = 0L;
        assertEquals(0, Card.countCards(mask));
        
        mask = Card.addCardToBitMask(mask, new Card(Rank.ACE, Suit.SPADES));
        assertEquals(1, Card.countCards(mask));
        
        mask = Card.addCardToBitMask(mask, new Card(Rank.KING, Suit.HEARTS));
        assertEquals(2, Card.countCards(mask));
    }

    @Test
    void bitsToCardsConversion() {
        Card aceSpades = new Card(Rank.ACE, Suit.SPADES);
        Card kingHearts = new Card(Rank.KING, Suit.HEARTS);
        
        long mask = 0L;
        mask = Card.addCardToBitMask(mask, aceSpades);
        mask = Card.addCardToBitMask(mask, kingHearts);
        
        List<Card> cards = Card.bitsToCards(mask);
        assertEquals(2, cards.size());
        assertTrue(cards.contains(aceSpades));
        assertTrue(cards.contains(kingHearts));
    }

    @Test
    void bitRepresentationBoundaries() {
        // Test lowest card (TWO CLUBS)
        Card lowest = new Card(Rank.TWO, Suit.CLUBS);
        assertTrue(lowest.toBits() >= 0);
        
        // Test highest card (ACE SPADES)
        Card highest = new Card(Rank.ACE, Suit.SPADES);
        assertTrue(highest.toBits() < (1 << (Card.RANK_BITS + Card.SUIT_BITS)));
    }

    private static Stream<Arguments> allPossibleCards() {
        List<Arguments> args = new ArrayList<>();
        for (Rank r1 : Rank.values()) {
            for (Suit s1 : Suit.values()) {
                for (Rank r2 : Rank.values()) {
                    for (Suit s2 : Suit.values()) {
                        args.add(Arguments.of(
                            new Card(r1, s1),
                            new Card(r2, s2)
                        ));
                    }
                }
            }
        }
        return args.stream();
    }
}