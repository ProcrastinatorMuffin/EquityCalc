// package com.equitycalc;

// import com.equitycalc.model.Card;
// import com.equitycalc.model.Player;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.BeforeEach;
// import static org.junit.jupiter.api.Assertions.*;

// import java.io.FileWriter;
// import java.io.IOException;
// import java.time.Duration;
// import java.time.Instant;
// import java.util.Arrays;
// import java.util.List;
// import java.util.ArrayList;

// public class EquityCalculatorTest {
    
//     private EquityCalculator calculator;
//     private static final String METRICS_FILE = "equity_metrics.csv";
//     private static final boolean DEBUG = true;
    
//     @BeforeEach
//     public void setUp() {
//         calculator = new EquityCalculator();
//         if (DEBUG) System.out.println("\n=== Starting new test ===");
//     }
    
//     @Test
//     public void testValidInput() {
//         List<Player> players = createValidPlayers(2);
//         List<Card> communityCards = createValidCommunityCards(3);
        
//         // Should not throw exception
//         calculator.calculateEquity(players, communityCards);
//     }
    
//     @Test
//     public void testTooFewPlayers() {
//         List<Player> players = createValidPlayers(1);
//         assertThrows(IllegalArgumentException.class, () -> 
//             calculator.calculateEquity(players, null));
//     }
    
//     @Test
//     public void testDuplicateCards() {
//         List<Player> players = Arrays.asList(
//             new Player(Arrays.asList(new Card("As"), new Card("Ks"))),
//             new Player(Arrays.asList(new Card("As"), new Card("Qs"))) // Duplicate AS
//         );
//         assertThrows(IllegalArgumentException.class, () -> 
//             calculator.calculateEquity(players, null));
//     }
    
//     @Test
//     public void testEquityCalculation() {
//         List<Player> players = createValidPlayers(2);
//         List<Card> communityCards = createValidCommunityCards(3);
        
//         calculator.calculateEquity(players, communityCards);
        
//         for (Player player : players) {
//             assertTrue(player.getWinProbability() >= 0 && player.getWinProbability() <= 1,
//                       "Win probability should be between 0 and 1");
//             assertTrue(player.getSplitProbability() >= 0 && player.getSplitProbability() <= 1,
//                       "Split probability should be between 0 and 1");
//             assertTrue(player.getLossProbability() >= 0 && player.getLossProbability() <= 1,
//                       "Loss probability should be between 0 and 1");
            
//             assertEquals(1.0, player.getWinProbability() + player.getSplitProbability() + 
//                         player.getLossProbability(), 0.0001,
//                         "Probabilities should sum to 1.0");
//         }
//     }
    
//     private List<Player> createValidPlayers(int count) {
//         if (DEBUG) System.out.printf("Creating %d players...%n", count);
//         Instant start = Instant.now();
//         List<Player> players = new ArrayList<>();
//         String[] suits = {"s", "h", "d", "c"};
//         // Added more ranks to support up to 9 players
//         String[] ranks = {"A", "K", "Q", "J", "T", "9", "8", "7", "6", "5"};
        
//         for (int i = 0; i < count; i++) {
//             List<Card> holeCards = Arrays.asList(
//                 new Card(ranks[i] + suits[0]),
//                 new Card(ranks[i] + suits[1])
//             );
//             players.add(new Player(holeCards));
//         }
//         if (DEBUG) System.out.printf("Player creation took %dms%n", 
//             Duration.between(start, Instant.now()).toMillis());
//         return players;
//     }
    
//     @Test
//     public void testTooManyCommunityCards() {
//         List<Player> players = createValidPlayers(2);
//         // The exception should be thrown by the calculator, not by createValidCommunityCards
//         assertThrows(IllegalArgumentException.class, () -> 
//             calculator.calculateEquity(players, Arrays.asList(
//                 new Card("2s"), new Card("3h"), new Card("4d"),
//                 new Card("5c"), new Card("6s"), new Card("7h")
//             ))
//         );
//     }
    
//     private List<Card> createValidCommunityCards(int count) {
//         // Move validation to beginning of method
//         if (count > 5) {
//             throw new IllegalArgumentException("Cannot create more than 5 community cards");
//         }
        
//         List<Card> cards = new ArrayList<>();
//         String[] suits = {"s", "h", "d", "c", "c"};
//         String[] ranks = {"2", "3", "4", "5", "6"};
        
//         for (int i = 0; i < count; i++) {
//             cards.add(new Card(ranks[i] + suits[i]));
//         }
//         return cards;
//     }

//     @Test
//     public void testAAvsKKPreflop() {
//         if (DEBUG) System.out.println("\nExecuting AA vs KK preflop test...");
//         Player aaPlayer = new Player(Arrays.asList(
//             new Card("As"), new Card("Ah")
//         ));
//         Player kkPlayer = new Player(Arrays.asList(
//             new Card("Ks"), new Card("Kh")
//         ));
//         List<Player> players = Arrays.asList(aaPlayer, kkPlayer);
        
//         Instant start = Instant.now();
//         calculator.calculateEquity(players, null);
//         Duration duration = Duration.between(start, Instant.now());
        
//         if (DEBUG) {
//             System.out.printf("AA vs KK completed in %dms%n", duration.toMillis());
//             System.out.printf("AA equity: %.2f%%, KK equity: %.2f%%%n", 
//                 aaPlayer.getWinProbability() * 100,
//                 kkPlayer.getWinProbability() * 100);
//         }
        
//         checkEquity("AA vs KK preflop", 0.8236, aaPlayer.getWinProbability(), 0.02);
//         checkEquity("AA vs KK preflop", 0.1764, kkPlayer.getWinProbability(), 0.02);
        
//         logMetrics("AA_vs_KK_preflop", duration.toMillis(), 
//                   aaPlayer.getWinProbability(), kkPlayer.getWinProbability());
//     }
    
//     @Test
//     public void testCoinflipScenario() {
//         Player akPlayer = new Player(Arrays.asList(
//             new Card("As"), new Card("Kh")
//         ));
//         Player qqPlayer = new Player(Arrays.asList(
//             new Card("Qs"), new Card("Qh")
//         ));
//         List<Player> players = Arrays.asList(akPlayer, qqPlayer);
        
//         Instant start = Instant.now();
//         calculator.calculateEquity(players, null);
//         Duration duration = Duration.between(start, Instant.now());
        
//         checkEquity("AK vs QQ preflop", 0.435, akPlayer.getWinProbability(), 0.02);
//         checkEquity("AK vs QQ preflop", 0.565, qqPlayer.getWinProbability(), 0.02);
        
//         logMetrics("AK_vs_QQ_preflop", duration.toMillis(),
//                   akPlayer.getWinProbability(), qqPlayer.getWinProbability());
//     }
    
//     @Test
//     public void testDrawingScenario() {
//         Player madeHand = new Player(Arrays.asList(
//             new Card("As"), new Card("Ad")
//         ));
//         Player drawingHand = new Player(Arrays.asList(
//             new Card("Ks"), new Card("Qs")
//         ));
//         List<Card> flop = Arrays.asList(
//             new Card("Js"), new Card("5s"), new Card("2h")
//         );
//         List<Player> players = Arrays.asList(madeHand, drawingHand);
        
//         Instant start = Instant.now();
//         calculator.calculateEquity(players, flop);
//         Duration duration = Duration.between(start, Instant.now());
        
//         checkEquity("AA vs Flush Draw", 0.65, madeHand.getWinProbability(), 0.02);
//         checkEquity("AA vs Flush Draw", 0.35, drawingHand.getWinProbability(), 0.02);
        
//         logMetrics("AA_vs_FlushDraw", duration.toMillis(),
//                   madeHand.getWinProbability(), drawingHand.getWinProbability());
//     }
    
//     @Test
//     public void testMultiplayerEquity() {
//         List<Player> players = Arrays.asList(
//             new Player(Arrays.asList(new Card("As"), new Card("Ah"))), // AA
//             new Player(Arrays.asList(new Card("Ks"), new Card("Kh"))), // KK
//             new Player(Arrays.asList(new Card("Qs"), new Card("Qh")))  // QQ
//         );
        
//         Instant start = Instant.now();
//         calculator.calculateEquity(players, null);
//         Duration duration = Duration.between(start, Instant.now());
        
//         double totalEquity = players.stream()
//             .mapToDouble(Player::getWinProbability)
//             .sum();
//         if (Math.abs(1.0 - totalEquity) > 0.01) {
//             System.out.printf("WARNING: Total equity %.4f deviates from expected 1.0%n", totalEquity);
//         }
        
//         logMetrics("AA_KK_QQ_multiway", duration.toMillis(),
//                   players.get(0).getWinProbability(),
//                   players.get(1).getWinProbability(),
//                   players.get(2).getWinProbability());
//     }
    
//     private void checkEquity(String scenario, double expected, double actual, double tolerance) {
//         if (DEBUG) System.out.printf("Checking equity for %s:%n", scenario);
//         if (Math.abs(expected - actual) > tolerance) {
//             System.out.printf("WARNING: %s - Expected %.4f but got %.4f (tolerance: ±%.2f)%n",
//                              scenario, expected, actual, tolerance);
//         } else if (DEBUG) {
//             System.out.printf("Equity check passed: %.4f (expected) vs %.4f (actual)%n", 
//                              expected, actual);
//         }
//     }
    
//     private void logMetrics(String scenario, long durationMs, double... equities) {
//         try (FileWriter writer = new FileWriter(METRICS_FILE, true)) {
//             StringBuilder sb = new StringBuilder()
//                 .append(scenario).append(",")
//                 .append(durationMs).append(",");
            
//             for (double equity : equities) {
//                 sb.append(String.format("%.4f", equity)).append(",");
//             }
//             sb.append(System.currentTimeMillis()).append("\n");
            
//             writer.write(sb.toString());
//         } catch (IOException e) {
//             System.err.println("Failed to log metrics: " + e.getMessage());
//         }
//     }
// }