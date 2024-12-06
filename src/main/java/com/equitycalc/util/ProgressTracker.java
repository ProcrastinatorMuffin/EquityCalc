package com.equitycalc.util;

public class ProgressTracker {
    private final int total;
    private int current;
    private final int barWidth = 50;
    private long startTime;
    
    // ANSI colors
    private static final String ANSI_RED = "\u001B[38;5;167m";     // Red suit
    private static final String ANSI_ORANGE = "\u001B[38;5;214m";  // Diamond suit
    private static final String ANSI_GREEN = "\u001B[38;5;142m";   // Club suit
    private static final String ANSI_BLUE = "\u001B[38;5;109m";    // Spade suit
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BG_DARK = "\033[48;2;40;40;40m";  // Dark background for cards


    
    // Updated card template (13 chars wide x 9 lines tall)
    private static final String[] CARD_TOP =    {"┌───────────┐ "};
    private static final String[] CARD_BOTTOM = {"└───────────┘ "};
    private static final String[] CARD_EMPTY =  {"│           │ "};
    private static final String[] CARD_TOP_LINE = {"│ %s         │ "}; // Rank
    private static final String[] CARD_TOP_SUIT = {"│ %s         │ "}; // Small suit
    private static final String[] CARD_CENTER =   {"│     %s     │ "}; // Big center suit
    private static final String[] CARD_BOT_SUIT = {"│         %s │ "}; // Small suit (upside down)
    private static final String[] CARD_BOT_LINE = {"│         %s │ "}; // Rank (upside down)

    // ANSI control sequences
    private static final String ANSI_CLEAR_LINE = "\033[2K"; // Clear entire line
    private static final String ANSI_UP = "\033[1A";         // Move cursor up
    
    private boolean isFirstUpdate = true;
    private int lastLineCount = 0;
    
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
        String color = switch (suit) {
            case "h" -> ANSI_RED;
            case "d" -> ANSI_ORANGE;
            case "c" -> ANSI_GREEN;
            case "s" -> ANSI_BLUE;
            default -> ANSI_RESET;
        };
        
        String bigSuit = getBigSuitSymbol(suit);
        String smallSuit = getSmallSuitSymbol(suit);
        String formattedRank = formatRank(rank);
        String upsideDownRank = getUpsideDownRank(formattedRank);
        
        // Replace ANSI_BG_WHITE with ANSI_BG_DARK in all lines
        lines[0].append(ANSI_BG_DARK).append(color).append(CARD_TOP[0]).append(ANSI_RESET);
        lines[1].append(ANSI_BG_DARK).append(color).append(String.format(CARD_TOP_LINE[0], formattedRank)).append(ANSI_RESET);
        lines[2].append(ANSI_BG_DARK).append(color).append(String.format(CARD_TOP_SUIT[0], smallSuit)).append(ANSI_RESET);
        lines[3].append(ANSI_BG_DARK).append(color).append(CARD_EMPTY[0]).append(ANSI_RESET);
        lines[4].append(ANSI_BG_DARK).append(color).append(String.format(CARD_CENTER[0], bigSuit)).append(ANSI_RESET);
        lines[5].append(ANSI_BG_DARK).append(color).append(CARD_EMPTY[0]).append(ANSI_RESET);
        lines[6].append(ANSI_BG_DARK).append(color).append(String.format(CARD_BOT_SUIT[0], smallSuit)).append(ANSI_RESET);
        lines[7].append(ANSI_BG_DARK).append(color).append(String.format(CARD_BOT_LINE[0], upsideDownRank)).append(ANSI_RESET);
        lines[8].append(ANSI_BG_DARK).append(color).append(CARD_BOTTOM[0]).append(ANSI_RESET);
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

        // Build the entire output first
        StringBuilder output = new StringBuilder();
        
        // Clear previous output if not first update
        if (!isFirstUpdate) {
            for (int i = 0; i < lastLineCount; i++) {
                output.append(ANSI_UP).append(ANSI_CLEAR_LINE);
            }
        }
        
        // Add current hand display
        String[] handLines = currentHandDisplay.split("\n");
        for (String line : handLines) {
            output.append(line).append("\n");
        }
        
        // Add progress bar
        output.append(String.format("Progress: [%-" + barWidth + "s] %d%%\n", 
            "=".repeat(bars), percentage));
        
        // Add stats
        output.append(String.format("Simulations: %d/%d | Win: %.2f%% | Split: %.2f%%\n",
            current, total, winRate * 100, splitRate * 100));
            
        // Add time estimates
        output.append(String.format("Elapsed: %.1fs | ETA: %.1fs\n", 
            elapsed/1000.0, eta/1000.0));

        // Calculate number of lines for next update
        lastLineCount = output.toString().split("\n").length;
        
        // Print the output
        System.out.print(output);
        
        isFirstUpdate = false;
    }

    public void complete() {
        // Move to new line and print completion message
        System.out.println("\n" + ANSI_CLEAR_LINE + "Simulation complete!");
    }
}