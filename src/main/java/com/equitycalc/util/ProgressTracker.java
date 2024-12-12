package com.equitycalc.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Arrays;

import com.equitycalc.model.Player;
import com.equitycalc.simulation.SimulationConfig;
import com.equitycalc.model.Card;

public class ProgressTracker {
    private final int total;
    private int current;
    private final int barWidth = 50;
    private long startTime;
    private final SimulationConfig config;
    
    private List<Player> knownPlayers;
    private List<Card> boardCards;
    private String boardDisplay = "";
    private Map<Integer, String> playerDisplays = new HashMap<>();
    
    // ANSI colors
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_BG_DARK = "\033[48;2;40;40;40m";  // Dark background for cards
    
    // Update ANSI colors to match Gruvbox theme
    private static final String ANSI_GREEN = "\u001B[38;5;142m";   // Club suit (#b8bb26)
    private static final String ANSI_BLACK = "\u001B[38;5;59m";    // Spade suit 
    private static final String ANSI_RED = "\u001B[38;5;167m";     // Heart suit (#fb4934)
    private static final String ANSI_BLUE = "\u001B[38;5;109m";    // Diamond suit (#83a598)

    // Player colors (Gruvbox-inspired)
    private static final String PLAYER_COLOR_1 = "\u001B[38;5;208m"; // Orange (#fe8019)
    private static final String PLAYER_COLOR_2 = "\u001B[38;5;175m"; // Purple (#d3869b)
    private static final String PLAYER_COLOR_3 = "\u001B[38;5;108m"; // Aqua (#8ec07c)
    private static final String PLAYER_COLOR_4 = "\u001B[38;5;214m"; // Yellow (#fabd2f)
    private static final String RANDOM_PLAYER_COLOR = "\u001B[38;5;245m"; // Gray

    // Card back pattern for random players
    private static final String[] CARD_BACK = {
        "┌───────────┐ ",
        "│▒▒▒▒▒▒▒▒▒▒▒│ ",
        "│▒▒▒▒▒▒▒▒▒▒▒│ ",
        "│▒▒▒▒▒▒▒▒▒▒▒│ ",
        "│▒▒▒▒▒▒▒▒▒▒▒│ ",
        "│▒▒▒▒▒▒▒▒▒▒▒│ ",
        "│▒▒▒▒▒▒▒▒▒▒▒│ ",
        "│▒▒▒▒▒▒▒▒▒▒▒│ ",
        "└───────────┘ "
    };

    // Updated card template (13 chars wide x 9 lines tall)
    private static final String[] CARD_TOP =    {"┌───────────┐ "};
    private static final String[] CARD_BOTTOM = {"└───────────┘ "};
    private static final String[] CARD_EMPTY =  {"│           │ "};
    private static final String[] CARD_TOP_LINE = {"│ %-2s        │ "}; // Left-padded rank
    private static final String[] CARD_TOP_SUIT = {"│ %s         │ "}; // Small suit
    private static final String[] CARD_CENTER =   {"│     %s     │ "}; // Big center suit
    private static final String[] CARD_BOT_SUIT = {"│         %s │ "}; // Small suit (upside down)
    private static final String[] CARD_BOT_LINE = {"│         %-2s│ "}; // Right-padded rank

    // ANSI control sequences
    private static final String ANSI_CLEAR_LINE = "\033[2K"; // Clear entire line
    private static final String ANSI_UP = "\033[1A";         // Move cursor up
    
    private boolean isFirstUpdate = true;
    private int lastLineCount = 0;
    
    private String currentHandDisplay = "";

    public ProgressTracker(int total, SimulationConfig config) {
        this.total = total;
        this.current = 0;
        this.startTime = System.currentTimeMillis();
        this.knownPlayers = config.getKnownPlayers();
        this.boardCards = config.getBoardCards();
        this.config = config;  // Store config
        initializeDisplays();
    }

    private void initializeDisplays() {
        // Generate board display if exists
        if (!boardCards.isEmpty()) {
            this.boardDisplay = visualizeCards(
                boardCards.stream()
                    .map(card -> card.toString())  // Fixed: Use toString()
                    .collect(Collectors.joining())
            );
        }
        
        // Generate displays for each known player
        for (int i = 0; i < knownPlayers.size(); i++) {
            Player player = knownPlayers.get(i);
            String handKey = player.getHoleCards().stream()
                .map(card -> card.toString())  // Fixed: Use toString()
                .collect(Collectors.joining());
            playerDisplays.put(i, visualizeCards(handKey));
        }
    }
    
    // Add missing method
    private String visualizeCards(String cardString) {
        if (cardString == null || cardString.length() % 2 != 0) {
            return "";
        }
    
        StringBuilder[] lines = new StringBuilder[9];
        for (int i = 0; i < 9; i++) {
            lines[i] = new StringBuilder();
        }
    
        // Process cards in pairs of 2 chars
        for (int i = 0; i < cardString.length(); i += 2) {
            String rank = cardString.substring(i, i + 1);
            String suit = cardString.substring(i + 1, i + 2);
            addCardToVisual(lines, rank, suit);
        }
    
        return String.join("\n", Arrays.stream(lines)
            .map(StringBuilder::toString)
            .collect(Collectors.toList()));
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
            case "d" -> ANSI_BLUE;   
            case "c" -> ANSI_GREEN;  
            case "s" -> ANSI_BLACK;  
            default -> ANSI_RESET;
        };
        
        String bigSuit = getBigSuitSymbol(suit);
        String smallSuit = getSmallSuitSymbol(suit);
        String formattedRank = formatRank(rank);
        
        lines[0].append(ANSI_BG_DARK).append(color).append(CARD_TOP[0]).append(ANSI_RESET);
        lines[1].append(ANSI_BG_DARK).append(color).append(String.format(CARD_TOP_LINE[0], formattedRank)).append(ANSI_RESET);
        lines[2].append(ANSI_BG_DARK).append(color).append(String.format(CARD_TOP_SUIT[0], smallSuit)).append(ANSI_RESET);
        lines[3].append(ANSI_BG_DARK).append(color).append(CARD_EMPTY[0]).append(ANSI_RESET);
        lines[4].append(ANSI_BG_DARK).append(color).append(String.format(CARD_CENTER[0], bigSuit)).append(ANSI_RESET);
        lines[5].append(ANSI_BG_DARK).append(color).append(CARD_EMPTY[0]).append(ANSI_RESET);
        lines[6].append(ANSI_BG_DARK).append(color).append(String.format(CARD_BOT_SUIT[0], smallSuit)).append(ANSI_RESET);
        lines[7].append(ANSI_BG_DARK).append(color).append(String.format(CARD_BOT_LINE[0], formattedRank)).append(ANSI_RESET);
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

    private String formatRank(String rank) {
        return switch (rank) {
            case "T" -> "10";
            case "K", "Q", "J", "A" -> rank;
            default -> rank;
        };
    }

    public void update(int progress, List<Double> winRates, List<Double> splitRates) {
        this.current = progress;
        
        StringBuilder output = new StringBuilder();
        clearPreviousOutput(output);
        
        // Display board if exists
        if (!boardDisplay.isEmpty()) {
            output.append("\nBoard:\n").append(boardDisplay);
        }
        
        // Modify display of players:
        for (int i = 0; i < knownPlayers.size(); i++) {
            output.append("\n")
                .append(getPlayerLabel(i, knownPlayers.size()))
                .append(":\n")
                .append(playerDisplays.get(i));
        }

        // Add random players if any:
        for (int i = 0; i < config.getNumRandomPlayers(); i++) {
            output.append("\n")
                .append(RANDOM_PLAYER_COLOR)
                .append("Random Player ")
                .append(knownPlayers.size() + i + 1)
                .append(ANSI_RESET)
                .append(":\n")
                .append(visualizeCardBack());
        }
        
        // Add progress bar
        appendProgressBar(output);
        
        // Add equity distribution bar
        appendEquityBar(output, winRates);
        
        // Add detailed stats for each player
        appendPlayerStats(output, winRates, splitRates);
        
        // Add time estimates
        appendTimeEstimates(output);
        
        // Print the output
        System.out.print(output);
        lastLineCount = output.toString().split("\n").length;
        isFirstUpdate = false;
    }

    private String getPlayerLabel(int index, int totalKnownPlayers) {
        String color = getPlayerColor(index);
        if (totalKnownPlayers <= 2) {
            return color + (index == 0 ? "Hero" : "Villain") + ANSI_RESET;
        }
        return color + "Player " + (index + 1) + ANSI_RESET;
    }

    private String visualizeCardBack() {
        StringBuilder result = new StringBuilder();
        for (String line : CARD_BACK) {
            result.append(RANDOM_PLAYER_COLOR)
                  .append(line)
                  .append(RANDOM_PLAYER_COLOR)
                  .append(line)
                  .append(ANSI_RESET)
                  .append("\n");
        }
        return result.toString();
    }

    private void clearPreviousOutput(StringBuilder output) {
        if (!isFirstUpdate) {
            // Move cursor up by lastLineCount lines
            for (int i = 0; i < lastLineCount; i++) {
                output.append(ANSI_UP)         // Move up one line
                     .append(ANSI_CLEAR_LINE); // Clear the line
            }
        }
    }

    private void appendEquityBar(StringBuilder output, List<Double> equities) {
        output.append("\nEquity Distribution: ");
        int totalWidth = 50;
        int currentPosition = 0;
        
        for (int i = 0; i < equities.size(); i++) {
            int width = (int) (equities.get(i) * totalWidth);
            String color = getPlayerColor(i);
            output.append(color)
                .append("█".repeat(width))
                .append(ANSI_RESET);
            currentPosition += width;
        }
        
        // Fill remaining space if any
        if (currentPosition < totalWidth) {
            output.append(" ".repeat(totalWidth - currentPosition));
        }
        output.append("\n");
    }

    private String getPlayerColor(int playerIndex) {
        if (playerIndex >= knownPlayers.size()) {
            return RANDOM_PLAYER_COLOR;
        }
        return switch (playerIndex) {
            case 0 -> PLAYER_COLOR_1;  // Orange for Hero/Player 1
            case 1 -> PLAYER_COLOR_2;  // Purple for Villain/Player 2
            case 2 -> PLAYER_COLOR_3;  // Aqua for Player 3
            case 3 -> PLAYER_COLOR_4;  // Yellow for Player 4
            default -> RANDOM_PLAYER_COLOR;
        };
    }

    private void appendPlayerStats(StringBuilder output, 
        List<Double> winRates, List<Double> splitRates) {
        for (int i = 0; i < knownPlayers.size(); i++) {
            String label = getPlayerLabel(i, knownPlayers.size());
            output.append(String.format("%s: Win: %.2f%% | Split: %.2f%%\n",
                label, winRates.get(i) * 100, splitRates.get(i) * 100));
        }
        // Add random player stats if any
        for (int i = 0; i < config.getNumRandomPlayers(); i++) {
            int idx = knownPlayers.size() + i;
            output.append(String.format("%sRandom Player %d%s: Win: %.2f%% | Split: %.2f%%\n",
                RANDOM_PLAYER_COLOR, idx + 1, ANSI_RESET, 
                winRates.get(idx) * 100, splitRates.get(idx) * 100));
        }
    }

    private void appendProgressBar(StringBuilder output) {
        int percentage = (int) ((current * 100.0) / total);
        int bars = (int) ((current * barWidth) / total);
        output.append(String.format("\nProgress: [%-" + barWidth + "s] %d%%\n", 
            "=".repeat(bars), percentage));
    }
    
    private void appendTimeEstimates(StringBuilder output) {
        long elapsed = System.currentTimeMillis() - startTime;
        long eta = (elapsed * (total - current)) / (current > 0 ? current : 1);
        output.append(String.format("Elapsed: %.1fs | ETA: %.1fs\n", 
            elapsed/1000.0, eta/1000.0));
    }

    public void complete() {
        // Move to new line and print completion message
        System.out.println("\n" + ANSI_CLEAR_LINE + "Simulation complete!");
    }
}