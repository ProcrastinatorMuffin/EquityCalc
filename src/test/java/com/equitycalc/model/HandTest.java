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
    void throwsOnAddingMoreThanFiveCards() {
        hand.addCard(new Card("As"));
        hand.addCard(new Card("Ks"));
        hand.addCard(new Card("Qs"));
        hand.addCard(new Card("Js"));
        hand.addCard(new Card("Ts"));
        
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
}