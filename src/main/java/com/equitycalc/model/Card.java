package com.equitycalc.model;

public class Card {
    public enum Suit {
        SPADES('s'), HEARTS('h'), DIAMONDS('d'), CLUBS('c');

        private final char symbol;

        Suit(char symbol) {
            this.symbol = symbol;
        }

        public char getSymbol() {
            return symbol;
        }

        public static Suit fromSymbol(char symbol) {
            for (Suit suit : values()) {
                if (suit.getSymbol() == symbol) {
                    return suit;
                }
            }
            throw new IllegalArgumentException("Invalid suit symbol: " + symbol);
        }
    }

    public enum Rank {
        TWO('2'), THREE('3'), FOUR('4'), FIVE('5'), SIX('6'), SEVEN('7'), EIGHT('8'), NINE('9'),
        TEN('T'), JACK('J'), QUEEN('Q'), KING('K'), ACE('A');

        private final char symbol;

        Rank(char symbol) {
            this.symbol = symbol;
        }

        public char getSymbol() {
            return symbol;
        }

        public static Rank fromSymbol(char symbol) {
            for (Rank rank : values()) {
                if (rank.getSymbol() == symbol) {
                    return rank;
                }
            }
            throw new IllegalArgumentException("Invalid rank symbol: " + symbol);
        }
    }

    private final Suit suit;
    private final Rank rank;

    public Card(String card) {
        if (card == null || card.length() < 2) {
            throw new IllegalArgumentException("Invalid card format: " + card);
        }
        this.rank = Rank.fromSymbol(card.charAt(0));
        this.suit = Suit.fromSymbol(card.charAt(1));
    }

    public Card(Rank rank, Suit suit) {
        this.rank = rank;
        this.suit = suit;
    }

    public Suit getSuit() {
        return suit;
    }

    public Rank getRank() {
        return rank;
    }

    @Override
    public String toString() {
        return rank.getSymbol() + String.valueOf(suit.getSymbol());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Card card = (Card) o;
        return suit == card.suit && rank == card.rank;
    }

    @Override
    public int hashCode() {
        return 31 * suit.hashCode() + rank.hashCode();
    }
}
