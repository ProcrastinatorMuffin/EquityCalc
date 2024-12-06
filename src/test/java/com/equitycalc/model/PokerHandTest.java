package com.equitycalc.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class PokerHandTest {
    
    private PokerHand hand;
    
    @BeforeEach
    void setUp() {
        hand = new PokerHand();
    }

    @Test
    void newPokerHandIsEmpty() {
        assertEquals(0, hand.getCardCount());
        assertEquals(0L, hand.toBitMask());
    }

    @Test
    void throwsOnAddingMoreThanSevenCards() {
        // Add 7 cards
        hand.addCard(new Card("As"));
        hand.addCard(new Card("Ks"));
        hand.addCard(new Card("Qs"));
        hand.addCard(new Card("Js"));
        hand.addCard(new Card("Ts"));
        hand.addCard(new Card("9s"));
        hand.addCard(new Card("8s"));
        
        assertThrows(IllegalStateException.class, () -> 
            hand.addCard(new Card("7s")));
    }

    @Test
    void fromBitMaskCreatesCorrectHand() {
        hand.addCard(new Card("As"));
        hand.addCard(new Card("Ks"));
        hand.addCard(new Card("Qs"));
        
        long mask = hand.toBitMask();
        PokerHand newHand = PokerHand.fromBitMask(mask);
        
        assertEquals(hand, newHand);
        assertEquals(3, newHand.getCardCount());
    }

    @Test
    void fromBitMaskThrowsOnTooManyCards() {
        Deck deck = new Deck();
        assertThrows(IllegalArgumentException.class, () -> 
            PokerHand.fromBitMask(deck.toBitMask()));
    }

    @Test
    void getSuitBitMaskReturnsCorrectMask() {
        hand.addCard(new Card("As"));
        hand.addCard(new Card("Ks"));
        hand.addCard(new Card("Qh"));
        
        long spadesMask = hand.getSuitBitMask(Card.Suit.SPADES);
        long heartsMask = hand.getSuitBitMask(Card.Suit.HEARTS);
        
        assertEquals(2, Card.countCards(spadesMask));
        assertEquals(1, Card.countCards(heartsMask));
    }

    @Test
    void getRankBitMaskReturnsCorrectMask() {
        hand.addCard(new Card("As"));
        hand.addCard(new Card("Ah"));
        hand.addCard(new Card("Ks"));
        
        long acesMask = hand.getRankBitMask(Card.Rank.ACE);
        long kingsMask = hand.getRankBitMask(Card.Rank.KING);
        
        assertEquals(2, Card.countCards(acesMask));
        assertEquals(1, Card.countCards(kingsMask));
    }

    @Test
    void bitMaskConsistencyAfterOperations() {
        Card aceSpades = new Card("As");
        Card kingHearts = new Card("Kh");
        Card queenDiamonds = new Card("Qd");
        
        hand.addCard(aceSpades);
        hand.addCard(kingHearts);
        hand.addCard(queenDiamonds);
        
        long initialMask = hand.toBitMask();
        hand.removeCard(kingHearts);
        hand.addCard(kingHearts);
        
        assertEquals(initialMask, hand.toBitMask());
        assertEquals(3, hand.getCardCount());
    }
}