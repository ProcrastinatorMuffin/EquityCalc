// package com.equitycalc;

// import org.junit.jupiter.api.Test;

// import com.equitycalc.model.Card;
// import com.equitycalc.model.Player;

// import static org.junit.jupiter.api.Assertions.assertTrue;

// import java.util.ArrayList;
// import java.util.List;

// public class EquityCalculatorTest {

//     @Test
//     public void testCalculateEquity() {
//         List<Player> players = new ArrayList<>();
//         players.add(new Player("AS KH"));
//         players.add(new Player("QD JC"));

//         List<Card> communityCards = new ArrayList<>();
//         communityCards.add(new Card("2D"));
//         communityCards.add(new Card("3H"));
//         communityCards.add(new Card("4S"));

//         EquityCalculator equityCalculator = new EquityCalculator();
//         equityCalculator.calculateEquity(players, communityCards);

//         for (Player player : players) {
//             assertTrue(player.getWinProbability() >= 0 && player.getWinProbability() <= 1);
//             assertTrue(player.getLossProbability() >= 0 && player.getLossProbability() <= 1);
//             assertTrue(player.getSplitProbability() >= 0 && player.getSplitProbability() <= 1);
//         }
//     }
// }
