// package com.equitycalc;

// import org.junit.jupiter.api.Test;

// import com.equitycalc.model.Card;
// import com.equitycalc.model.Deck;

// import static org.junit.jupiter.api.Assertions.*;

// public class DeckTest {

//     @Test
//     public void testShuffle() {
//         Deck deck = new Deck();
//         String firstCardBeforeShuffle = deck.dealCard().toString();
//         deck.shuffle();
//         String firstCardAfterShuffle = deck.dealCard().toString();
//         assertNotEquals(firstCardBeforeShuffle, firstCardAfterShuffle);
//     }

//     @Test
//     public void testDealCard() {
//         Deck deck = new Deck();
//         Card card = deck.dealCard();
//         assertNotNull(card);
//         assertEquals(51, deck.getCards().size());
//     }
// }
