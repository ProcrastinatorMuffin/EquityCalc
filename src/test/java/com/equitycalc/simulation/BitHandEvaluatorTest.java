package com.equitycalc.simulation;

import com.equitycalc.model.Card;
import com.equitycalc.model.PokerHand;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class BitHandEvaluatorTest {
    
    // Helper method to create cards easily
    private Card card(String notation) {
        if (notation == null || notation.length() != 2) {
            throw new IllegalArgumentException("Card notation must be 2 characters");
        }
        return new Card(notation);
    }
    
    // Helper to create a hand from string notations
    private PokerHand hand(String... cardNotations) {
        PokerHand hand = new PokerHand();
        for (String notation : cardNotations) {
            hand.addCard(card(notation));
        }
        return hand;
    }
    
    @Test
    void testNullHand() {
        assertThrows(IllegalArgumentException.class, () -> 
            BitHandEvaluator.evaluateHand(null));
    }
    
    @Test
    void testInvalidHandSize() {
        assertThrows(IllegalArgumentException.class, () -> 
            BitHandEvaluator.evaluateHand(hand("AS", "KS", "QS", "JS")));
    }
    
    @Test
    void testRoyalFlush() throws InterruptedException, ExecutionException {
        HandRanking ranking = BitHandEvaluator.evaluateHand(
            hand("AS", "KS", "QS", "JS", "TS"));
        assertEquals(HandRanking.Type.STRAIGHT_FLUSH, ranking.type);
        assertEquals(Card.Rank.ACE, ranking.tiebreakers.get(0));
    }
    
    @Test
    void testStraightFlush() throws InterruptedException, ExecutionException {
        HandRanking ranking = BitHandEvaluator.evaluateHand(
            hand("9H", "8H", "7H", "6H", "5H"));
        assertEquals(HandRanking.Type.STRAIGHT_FLUSH, ranking.type);
        assertEquals(Card.Rank.NINE, ranking.tiebreakers.get(0));
    }
    
    @Test
    void testFourOfAKind() throws InterruptedException, ExecutionException {
        HandRanking ranking = BitHandEvaluator.evaluateHand(
            hand("AS", "AH", "AD", "AC", "KS"));
        assertEquals(HandRanking.Type.FOUR_OF_A_KIND, ranking.type);
        assertEquals(Card.Rank.ACE, ranking.tiebreakers.get(0));
        assertEquals(Card.Rank.KING, ranking.tiebreakers.get(1)); // Kicker
    }
    
    @Test
    void testFullHouse() throws InterruptedException, ExecutionException {
        HandRanking ranking = BitHandEvaluator.evaluateHand(
            hand("AS", "AH", "AD", "KS", "KH"));
        assertEquals(HandRanking.Type.FULL_HOUSE, ranking.type);
        assertEquals(Card.Rank.ACE, ranking.tiebreakers.get(0)); // Trips
        assertEquals(Card.Rank.KING, ranking.tiebreakers.get(1)); // Pair
    }
    
    @Test
    void testFlush() throws InterruptedException, ExecutionException {
        HandRanking ranking = BitHandEvaluator.evaluateHand(
            hand("AS", "KS", "9S", "7S", "2S"));
        assertEquals(HandRanking.Type.FLUSH, ranking.type);
        List<Card.Rank> expectedRanks = Arrays.asList(
            Card.Rank.ACE, Card.Rank.KING, Card.Rank.NINE, 
            Card.Rank.SEVEN, Card.Rank.TWO
        );
        assertEquals(expectedRanks, ranking.tiebreakers);
    }
    
    @Test
    void testStraight() throws InterruptedException, ExecutionException {
        HandRanking ranking = BitHandEvaluator.evaluateHand(
            hand("AS", "KH", "QD", "JC", "TS"));
        assertEquals(HandRanking.Type.STRAIGHT, ranking.type);
        assertEquals(Card.Rank.ACE, ranking.tiebreakers.get(0));
    }
    
    @Test
    void testThreeOfAKind() throws InterruptedException, ExecutionException {
        HandRanking ranking = BitHandEvaluator.evaluateHand(
            hand("AS", "AH", "AD", "KS", "QH"));
        assertEquals(HandRanking.Type.THREE_OF_A_KIND, ranking.type);
        assertEquals(Card.Rank.ACE, ranking.tiebreakers.get(0));
        assertEquals(Card.Rank.KING, ranking.tiebreakers.get(1)); // First kicker
        assertEquals(Card.Rank.QUEEN, ranking.tiebreakers.get(2)); // Second kicker
    }
    
    @Test
    void testTwoPair() throws InterruptedException, ExecutionException {
        HandRanking ranking = BitHandEvaluator.evaluateHand(
            hand("AS", "AH", "KS", "KH", "QS"));
        assertEquals(HandRanking.Type.TWO_PAIR, ranking.type);
        assertEquals(Card.Rank.ACE, ranking.tiebreakers.get(0)); // Higher pair
        assertEquals(Card.Rank.KING, ranking.tiebreakers.get(1)); // Lower pair
        assertEquals(Card.Rank.QUEEN, ranking.tiebreakers.get(2)); // Kicker
    }
    
    @Test
    void testOnePair() throws InterruptedException, ExecutionException {
        HandRanking ranking = BitHandEvaluator.evaluateHand(
            hand("AS", "AH", "KS", "QH", "JD"));
        assertEquals(HandRanking.Type.ONE_PAIR, ranking.type);
        assertEquals(Card.Rank.ACE, ranking.tiebreakers.get(0));
        List<Card.Rank> expectedKickers = Arrays.asList(
            Card.Rank.KING, Card.Rank.QUEEN, Card.Rank.JACK
        );
        assertEquals(expectedKickers, ranking.tiebreakers.subList(1, 4));
    }
    
    @Test
    void testHighCard() throws InterruptedException, ExecutionException {
        HandRanking ranking = BitHandEvaluator.evaluateHand(
            hand("AS", "KH", "QD", "JC", "9S"));
        assertEquals(HandRanking.Type.HIGH_CARD, ranking.type);
        List<Card.Rank> expectedRanks = Arrays.asList(
            Card.Rank.ACE, Card.Rank.KING, Card.Rank.QUEEN,
            Card.Rank.JACK, Card.Rank.NINE
        );
        assertEquals(expectedRanks, ranking.tiebreakers);
    }
    
    @Test
    void testDuplicateCards() {
        assertThrows(IllegalArgumentException.class, () ->
            hand("AS", "AS", "KS", "QS", "JS"));
    }

    @Test
    void testInvalidCardNotation() {
        assertThrows(IllegalArgumentException.class, () ->
            hand("1S", "KS", "QS", "JS", "TS"));
    }

    @Test
    void testEmptyHand() {
        assertThrows(IllegalArgumentException.class, () ->
            BitHandEvaluator.evaluateHand(new PokerHand()));
    }
    
    @Test
    void testHandComparison() throws InterruptedException, ExecutionException {
        HandRanking royalFlush = BitHandEvaluator.evaluateHand(
            hand("AS", "KS", "QS", "JS", "TS"));
        HandRanking straightFlush = BitHandEvaluator.evaluateHand(
            hand("9S", "8S", "7S", "6S", "5S"));
        HandRanking fourOfAKind = BitHandEvaluator.evaluateHand(
            hand("AS", "AH", "AD", "AC", "KS"));
            
        assertTrue(royalFlush.compareTo(straightFlush) > 0);
        assertTrue(straightFlush.compareTo(fourOfAKind) > 0);
    }
}