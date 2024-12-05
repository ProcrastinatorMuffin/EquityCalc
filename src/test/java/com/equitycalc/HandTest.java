package com.equitycalc;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class HandTest {

    @Test
    public void testAddCard() {
        Hand hand = new Hand();
        Card card = new Card("AS");
        hand.addCard(card);
        assertTrue(hand.getCards().contains(card));
    }

    @Test
    public void testRemoveCard() {
        Hand hand = new Hand();
        Card card = new Card("AS");
        hand.addCard(card);
        hand.removeCard(card);
        assertFalse(hand.getCards().contains(card));
    }
}
