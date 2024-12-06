package com.equitycalc.model;

import com.equitycalc.model.Card;
import com.equitycalc.model.Hand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class HandTest {
    
    private Hand hand;
    
    @BeforeEach
    void setUp() {
        hand = new Hand();
    }

    @Test
    void newHandIsEmpty() {
        assertTrue(hand.getCards().isEmpty());
    }

    @Test
    void addCardWorksCorrectly() {
        Card card = new Card("As");
        hand.addCard(card);
        assertEquals(1, hand.getCards().size());
        assertTrue(hand.getCards().contains(card));
    }

    @Test
    void throwsOnAddingMoreThanTwoCards() {
        hand.addCard(new Card("As"));
        hand.addCard(new Card("Ks"));
        
        assertThrows(IllegalStateException.class, () -> 
            hand.addCard(new Card("9s")));
    }

    @Test
    void throwsOnDuplicateCard() {
        hand.addCard(new Card("As"));
        assertThrows(IllegalArgumentException.class, () -> 
            hand.addCard(new Card("As")));
    }

    @Test
    void removeCardWorksCorrectly() {
        Card card = new Card("As");
        hand.addCard(card);
        hand.removeCard(card);
        assertTrue(hand.getCards().isEmpty());
    }

    @Test
    void getCardsReturnsUnmodifiableList() {
        hand.addCard(new Card("As"));
        assertThrows(UnsupportedOperationException.class, () ->
            hand.getCards().add(new Card("Ks")));
    }

    @Test
    void toStringFormatsCorrectly() {
        hand.addCard(new Card("As"));
        hand.addCard(new Card("Kh"));
        assertEquals("As Kh", hand.toString());
    }

    @Test
    void equalsWorksCorrectly() {
        Hand hand1 = new Hand();
        Hand hand2 = new Hand();
        
        hand1.addCard(new Card("As"));
        hand1.addCard(new Card("Kh"));
        
        hand2.addCard(new Card("As"));
        hand2.addCard(new Card("Kh"));
        
        assertEquals(hand1, hand2);
        assertNotEquals(hand1, null);
        assertNotEquals(hand1, "Not a hand");
    }

    @Test
    void hashCodeIsConsistent() {
        Hand hand1 = new Hand();
        Hand hand2 = new Hand();
        
        hand1.addCard(new Card("As"));
        hand2.addCard(new Card("As"));
        
        assertEquals(hand1.hashCode(), hand2.hashCode());
    }

    @Test
    void bitMaskIsInitiallyEmpty() {
        assertEquals(0L, hand.toBitMask());
    }

    @Test
    void bitMaskIsUpdatedOnAddCard() {
        Card card = new Card("As");
        hand.addCard(card);
        assertTrue(Card.isBitSet(hand.toBitMask(), card));
    }

    @Test
    void fromBitMaskCreatesCorrectHand() {
        Card card1 = new Card("As");
        Card card2 = new Card("Kh");
        hand.addCard(card1);
        hand.addCard(card2);
        
        long mask = hand.toBitMask();
        Hand newHand = Hand.fromBitMask(mask);
        
        assertEquals(hand, newHand);
        assertEquals(2, newHand.getCardCount());
        assertTrue(newHand.containsCard(card1));
        assertTrue(newHand.containsCard(card2));
    }

    @Test
    void fromBitMaskThrowsOnTooManyCards() {
        Deck deck = new Deck();
        assertThrows(IllegalArgumentException.class, () -> 
            Hand.fromBitMask(deck.toBitMask()));
    }

    @Test
    void getCardCountMatchesBitCount() {
        assertEquals(0, hand.getCardCount());
        hand.addCard(new Card("As"));
        assertEquals(1, hand.getCardCount());
        hand.addCard(new Card("Kh"));
        assertEquals(2, hand.getCardCount());
    }

    @Test
    void clearResetsHandAndBitMask() {
        hand.addCard(new Card("As"));
        hand.addCard(new Card("Kh"));
        hand.clear();
        
        assertEquals(0, hand.getCardCount());
        assertEquals(0L, hand.toBitMask());
        assertTrue(hand.getCards().isEmpty());
    }

    @Test
    void containsCardMatchesBitMask() {
        Card card = new Card("As");
        assertFalse(hand.containsCard(card));
        hand.addCard(card);
        assertTrue(hand.containsCard(card));
        hand.removeCard(card);
        assertFalse(hand.containsCard(card));
    }

    @Test
    void bitMaskConsistencyAfterOperations() {
        Card card1 = new Card("As");
        Card card2 = new Card("Kh");
        
        hand.addCard(card1);
        hand.addCard(card2);
        long expectedMask = hand.toBitMask();
        
        hand.removeCard(card1);
        hand.addCard(card1);
        
        assertEquals(expectedMask, hand.toBitMask());
        assertEquals(2, hand.getCardCount());
    }
}