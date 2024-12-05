package com.equitycalc;

import com.equitycalc.model.Card;
import com.equitycalc.model.Deck;
import com.equitycalc.model.Hand;
import com.equitycalc.model.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class EquityCalc {

    private List<Player> players;
    private List<Card> communityCards;
    private EquityCalculator equityCalculator;

    public EquityCalc() {
        players = new ArrayList<>();
        communityCards = new ArrayList<>();
        equityCalculator = new EquityCalculator();
    }

    public void inputPlayerHoleCards() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter the number of players (up to 9): ");
        int numPlayers = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        for (int i = 0; i < numPlayers; i++) {
            System.out.println("Enter hole cards for player " + (i + 1) + " (e.g., AS KH): ");
            List<Card> holeCards = new ArrayList<>();
            players.add(new Player(holeCards));
        }
    }

    public void inputCommunityCards() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Enter community cards (e.g., 2D 3H 4S): ");
        String communityCardsInput = scanner.nextLine();
        String[] cards = communityCardsInput.split(" ");
        for (String card : cards) {
            communityCards.add(new Card(card));
        }
    }

    public void calculateAndDisplayProbabilities() {
        equityCalculator.calculateEquity(players, communityCards);
        for (Player player : players) {
            System.out.println("Player " + player.getId() + ":");
            System.out.println("Win probability: " + player.getWinProbability());
            System.out.println("Loss probability: " + player.getLossProbability());
            System.out.println("Split probability: " + player.getSplitProbability());
        }
    }

    public static void main(String[] args) {
        EquityCalc equityCalc = new EquityCalc();
        equityCalc.inputPlayerHoleCards();
        equityCalc.inputCommunityCards();
        equityCalc.calculateAndDisplayProbabilities();
    }
}
