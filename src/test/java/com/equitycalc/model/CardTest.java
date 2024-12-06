package com.equitycalc.model;

import com.equitycalc.model.Card;
import com.equitycalc.model.Card.Rank;
import com.equitycalc.model.Card.Suit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.*;
import java.util.stream.Stream;

public class CardTest {

    @Test
    void constructorWithValidStringCreatesCard() {
        Card card = new Card("As");
        assertEquals(Rank.ACE, card.getRank());
        assertEquals(Suit.SPADES, card.getSuit());
    }

    @Test
    void constructorWithEnumsCreatesCard() {
        Card card = new Card(Rank.KING, Suit.HEARTS);
        assertEquals(Rank.KING, card.getRank());
        assertEquals(Suit.HEARTS, card.getSuit());
    }

    @Test
    void toStringReturnsCorrectFormat() {
        Card card = new Card(Rank.QUEEN, Suit.DIAMONDS);
        assertEquals("Qd", card.toString());
    }

    @Test
    void equalsWorksCorrectly() {
        Card card1 = new Card("Th");
        Card card2 = new Card("Th");
        Card card3 = new Card("Ts");
        
        assertTrue(card1.equals(card2));
        assertFalse(card1.equals(card3));
        assertFalse(card1.equals(null));
        assertFalse(card1.equals("Th"));
    }

    @Test
    void hashCodeIsConsistent() {
        Card card1 = new Card("2c");
        Card card2 = new Card("2c");
        assertEquals(card1.hashCode(), card2.hashCode());
    }

    @ParameterizedTest
    @MethodSource("invalidCardStrings")
    void constructorThrowsOnInvalidInput(String invalidCard) {
        assertThrows(IllegalArgumentException.class, () -> new Card(invalidCard));
    }

    private static Stream<Arguments> invalidCardStrings() {
        return Stream.of(
            Arguments.of((String)null),
            Arguments.of(""),
            Arguments.of("A"),
            Arguments.of("XY"),
            Arguments.of("1s"),
            Arguments.of("Ax")
        );
    }

    @Test
    void suitFromSymbolWorksCorrectly() {
        assertEquals(Suit.CLUBS, Suit.fromSymbol('c'));
        assertThrows(IllegalArgumentException.class, () -> Suit.fromSymbol('x'));
    }

    @Test
    void rankFromSymbolWorksCorrectly() {
        assertEquals(Rank.ACE, Rank.fromSymbol('A'));
        assertThrows(IllegalArgumentException.class, () -> Rank.fromSymbol('1'));
    }
}
