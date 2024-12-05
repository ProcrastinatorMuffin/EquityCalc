package com.equitycalc;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CardTest {

    @Test
    public void testGetSuit() {
        Card card = new Card("AS");
        assertEquals("S", card.getSuit());
    }

    @Test
    public void testGetRank() {
        Card card = new Card("AS");
        assertEquals("A", card.getRank());
    }
}
