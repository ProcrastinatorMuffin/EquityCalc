package com.equitycalc;

public class Card {
    private String suit;
    private String rank;

    public Card(String card) {
        this.suit = card.substring(card.length() - 1);
        this.rank = card.substring(0, card.length() - 1);
    }

    public String getSuit() {
        return suit;
    }

    public void setSuit(String suit) {
        this.suit = suit;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }
}
