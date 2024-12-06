package com.equitycalc;

import javax.swing.*;

import com.equitycalc.model.Player;

import java.awt.*;
import java.util.List;

public class EquityCalcUI extends JFrame {

    private JTextArea playerHandsArea;
    private JTextArea equityProbabilitiesArea;

    public EquityCalcUI() {
        setTitle("Poker Real-Time Equity Calculator");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        playerHandsArea = new JTextArea();
        equityProbabilitiesArea = new JTextArea();

        add(new JScrollPane(playerHandsArea), BorderLayout.CENTER);
        add(new JScrollPane(equityProbabilitiesArea), BorderLayout.SOUTH);
    }

    public void displayPlayerHands(List<Player> players) {
        StringBuilder handsText = new StringBuilder();
        for (Player player : players) {
            handsText.append("Player ").append(player.getId()).append(": ").append(player.getHoleCards()).append("\n");
        }
        playerHandsArea.setText(handsText.toString());
    }

    public void displayEquityProbabilities(List<Player> players) {
        StringBuilder probabilitiesText = new StringBuilder();
        for (Player player : players) {
            probabilitiesText.append("Player ").append(player.getId()).append(":\n");
            probabilitiesText.append("Win probability: ").append(player.getWinProbability()).append("\n");
            probabilitiesText.append("Loss probability: ").append(player.getLossProbability()).append("\n");
            probabilitiesText.append("Split probability: ").append(player.getSplitProbability()).append("\n");
        }
        equityProbabilitiesArea.setText(probabilitiesText.toString());
    }

    public void updateUIInRealTime(List<Player> players) {
        displayPlayerHands(players);
        displayEquityProbabilities(players);
    }

    public static void main(String[] args) {
        EquityCalcUI ui = new EquityCalcUI();
        ui.setVisible(true);

        // Placeholder for real-time updates
        // In a real application, this would be replaced with actual real-time update logic
        new Timer(1000, e -> {
            // Simulate real-time updates
            // This is a placeholder implementation
            // In a real application, this would be replaced with actual real-time update logic
        }).start();
    }
}
