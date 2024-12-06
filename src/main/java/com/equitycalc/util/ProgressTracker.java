package com.equitycalc.util;

public class ProgressTracker {
    private final int total;
    private int current;
    private final int barWidth = 50;
    private long startTime;
    private static final String ANSI_CLEAR_LINE = "\u001B[1A\u001B[2K";
    
    // ANSI colors
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_BLACK = "\u001B[30m";
    private static final String ANSI_RESET = "\u001B[0m";
    
    // Updated card template (13 chars wide x 9 lines tall)
    private static final String[] CARD_TOP =    {"┌───────────┐ "};
    private static final String[] CARD_BOTTOM = {"└───────────┘ "};
    private static final String[] CARD_EMPTY =  {"│           │ "};
    private static final String[] CARD_TOP_LINE = {"│ %s         │ "}; // Rank
    private static final String[] CARD_TOP_SUIT = {"│ %s         │ "}; // Small suit
    private static final String[] CARD_CENTER =   {"│     %s     │ "}; // Big center suit
    private static final String[] CARD_BOT_SUIT = {"│         %s │ "}; // Small suit (upside down)
    private static final String[] CARD_BOT_LINE = {"│         %s │ "}; // Rank (upside down)
    
    // Big suit symbols (larger Unicode variants)
    private static final String[] BIG_SUITS = {"♥", "♦", "♣", "♠"};
    private static final String[] SMALL_SUITS = {"♥", "♦", "♣", "♠"};
    
    private String currentHandDisplay = "";

    public ProgressTracker(int total) {
        this.total = total;
        this.current = 0;
        this.startTime = System.currentTimeMillis();
    }

    public void setCurrentHand(String hand) {
        this.currentHandDisplay = visualizeHand(hand);
    }

    private String visualizeHand(String hand) {
        if (hand == null || hand.length() != 4) return "";
        
        StringBuilder[] cardLines = new StringBuilder[9];
        for (int i = 0; i < 9; i++) {
            cardLines[i] = new StringBuilder();
        }

        for (int i = 0; i < 4; i += 2) {
            String rank = hand.substring(i, i + 1);
            String suit = hand.substring(i + 1, i + 2);
            addCardToVisual(cardLines, rank, suit);
        }

        StringBuilder result = new StringBuilder("\n");
        for (StringBuilder line : cardLines) {
            result.append(line.toString()).append("\n");
        }
        return result.toString();
    }

    private void addCardToVisual(StringBuilder[] lines, String rank, String suit) {
        String color = suit.equals("h") || suit.equals("d") ? ANSI_RED : ANSI_BLACK;
        String bigSuit = getBigSuitSymbol(suit);
        String smallSuit = getSmallSuitSymbol(suit);
        String formattedRank = formatRank(rank);
        String upsideDownRank = getUpsideDownRank(formattedRank);
        
        lines[0].append(color).append(CARD_TOP[0]).append(ANSI_RESET);
        lines[1].append(color).append(String.format(CARD_TOP_LINE[0], formattedRank)).append(ANSI_RESET);
        lines[2].append(color).append(String.format(CARD_TOP_SUIT[0], smallSuit)).append(ANSI_RESET);
        lines[3].append(color).append(CARD_EMPTY[0]).append(ANSI_RESET);
        lines[4].append(color).append(String.format(CARD_CENTER[0], bigSuit)).append(ANSI_RESET);
        lines[5].append(color).append(CARD_EMPTY[0]).append(ANSI_RESET);
        lines[6].append(color).append(String.format(CARD_BOT_SUIT[0], smallSuit)).append(ANSI_RESET);
        lines[7].append(color).append(String.format(CARD_BOT_LINE[0], upsideDownRank)).append(ANSI_RESET);
        lines[8].append(color).append(CARD_BOTTOM[0]).append(ANSI_RESET);
    }

    private String getBigSuitSymbol(String suit) {
        return switch (suit) {
            case "h" -> "♥";
            case "d" -> "♦";
            case "c" -> "♣";
            case "s" -> "♠";
            default -> "?";
        };
    }

    private String getSmallSuitSymbol(String suit) {
        return switch (suit) {
            case "h" -> "♥";
            case "d" -> "♦";
            case "c" -> "♣";
            case "s" -> "♠";
            default -> "?";
        };
    }

    private String getUpsideDownRank(String rank) {
        // Unicode characters for upside-down numbers/letters
        return switch (rank.trim()) {
            case "2" -> "Z";  // Approximation
            case "3" -> "Ɛ";
            case "4" -> "ᔭ";
            case "5" -> "S";
            case "6" -> "9";
            case "7" -> "L";
            case "8" -> "8";
            case "9" -> "6";
            case "10" -> "0І";
            case "J" -> "ſ";
            case "Q" -> "Ό";
            case "K" -> "ʞ";
            case "A" -> "∀";
            default -> "?";
        };
    }

    private String formatRank(String rank) {
        return switch (rank) {
            case "T" -> "10";
            case "K", "Q", "J", "A" -> rank;
            default -> rank;
        };
    }

    public void update(int progress, double winRate, double splitRate) {
        this.current = progress;
        int percentage = (int) ((current * 100.0) / total);
        int bars = (int) ((current * barWidth) / total);
        
        long elapsed = System.currentTimeMillis() - startTime;
        long eta = (elapsed * (total - current)) / (current > 0 ? current : 1);

        // Clear previous lines
        for (int i = 0; i < 12; i++) { // Increased to accommodate card display
            System.out.print(ANSI_CLEAR_LINE);
            System.out.print("\033[1A");
        }
        
        // Print current hand
        System.out.print(currentHandDisplay);
        
        // Print progress bar
        System.out.printf("\nProgress: [%-" + barWidth + "s] %d%%", 
            "=".repeat(bars), percentage);
        
        // Print stats
        System.out.printf("\nSimulations: %d/%d | Win: %.2f%% | Split: %.2f%%",
            current, total, winRate * 100, splitRate * 100);
            
        // Print time estimates
        System.out.printf("\nElapsed: %.1fs | ETA: %.1fs", 
            elapsed/1000.0, eta/1000.0);
    }

    public void complete() {
        System.out.println("\nSimulation complete!");
    }
}