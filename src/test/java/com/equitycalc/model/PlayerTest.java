package com.equitycalc.model;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    @Test
    void constructorCreatesPlayerWithValidHoleCards() {
        List<Card> holeCards = Arrays.asList(new Card("As"), new Card("Kh"));
        Player player = new Player(holeCards);
        assertEquals(holeCards, player.getHoleCards());
    }

    @Test
    void constructorThrowsOnNullHoleCards() {
        assertThrows(IllegalArgumentException.class, () -> new Player(null));
    }

    @Test
    void constructorThrowsOnInvalidNumberOfHoleCards() {
        List<Card> singleCard = Arrays.asList(new Card("As"));
        List<Card> threeCards = Arrays.asList(new Card("As"), new Card("Kh"), new Card("Qd"));
        
        assertThrows(IllegalArgumentException.class, () -> new Player(singleCard));
        assertThrows(IllegalArgumentException.class, () -> new Player(threeCards));
    }

    @Test
    void setHoleCardsWorksWithValidCards() {
        Player player = new Player(Arrays.asList(new Card("As"), new Card("Kh")));
        List<Card> newHoleCards = Arrays.asList(new Card("Qd"), new Card("Jc"));
        player.setHoleCards(newHoleCards);
        assertEquals(newHoleCards, player.getHoleCards());
    }

    @Test
    void setHoleCardsThrowsOnInvalidInput() {
        Player player = new Player(Arrays.asList(new Card("As"), new Card("Kh")));
        assertThrows(IllegalArgumentException.class, () -> player.setHoleCards(null));
        assertThrows(IllegalArgumentException.class, 
            () -> player.setHoleCards(Arrays.asList(new Card("As"))));
    }

    @Test
    void probabilitiesSetAndGetCorrectly() {
        Player player = new Player(Arrays.asList(new Card("As"), new Card("Kh")));
        
        player.setWinProbability(0.5);
        player.setLossProbability(0.3);
        player.setSplitProbability(0.2);
        
        assertEquals(0.5, player.getWinProbability());
        assertEquals(0.3, player.getLossProbability());
        assertEquals(0.2, player.getSplitProbability());
    }

    @Test
    void uniqueIdsAreAssigned() {
        Player player1 = new Player(Arrays.asList(new Card("As"), new Card("Kh")));
        Player player2 = new Player(Arrays.asList(new Card("Qd"), new Card("Jc")));
        
        assertNotEquals(player1.getId(), player2.getId());
    }

    @Test
    void equalsWorksCorrectly() {
        Player player1 = new Player(Arrays.asList(new Card("As"), new Card("Kh")));
        Player player2 = new Player(Arrays.asList(new Card("As"), new Card("Kh")));
        Player samePlayer = player1;
        
        assertTrue(player1.equals(samePlayer));
        assertFalse(player1.equals(player2));
        assertFalse(player1.equals(null));
        assertFalse(player1.equals("Not a player"));
    }

    @Test
    void hashCodeIsBasedOnId() {
        Player player = new Player(Arrays.asList(new Card("As"), new Card("Kh")));
        assertEquals(Integer.hashCode(player.getId()), player.hashCode());
    }

    @Test
    void toStringContainsAllFields() {
        Player player = new Player(Arrays.asList(new Card("As"), new Card("Kh")));
        player.setWinProbability(0.5);
        player.setLossProbability(0.3);
        player.setSplitProbability(0.2);
        
        String playerString = player.toString();
        assertTrue(playerString.contains("id=" + player.getId()));
        assertTrue(playerString.contains("holeCards="));
        assertTrue(playerString.contains("winProbability=0.5"));
        assertTrue(playerString.contains("lossProbability=0.3"));
        assertTrue(playerString.contains("splitProbability=0.2"));
    }
}