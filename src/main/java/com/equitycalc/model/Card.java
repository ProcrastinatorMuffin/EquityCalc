package com.equitycalc.model;

import java.util.ArrayList;
import java.util.List;

public class Card implements Comparable<Card> {
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

    // Bit representation constants
    public static final int SUIT_BITS = 2;  // 2 bits for suit (0-3)
    public static final int RANK_BITS = 4;  // 4 bits for rank (2-14)
    private static final int SUIT_MASK = (1 << SUIT_BITS) - 1;
    private static final int RANK_MASK = (1 << RANK_BITS) - 1;
    
    // Cache bit representation
    private final int bitValue;

    public Card(Rank rank, Suit suit) {
        this.rank = rank;
        this.suit = suit;
        this.bitValue = toBits(rank, suit);
    }

    // String constructor matches common poker notation
    public Card(String card) {
        if (card == null || card.length() != 2) {
            throw new IllegalArgumentException("Invalid card format. Must be rank+suit (e.g., 'As', 'Kh', '2c')");
        }
        this.rank = Rank.fromSymbol(Character.toUpperCase(card.charAt(0)));
        this.suit = Suit.fromSymbol(Character.toLowerCase(card.charAt(1)));
        this.bitValue = toBits(rank, suit);
    }

    private static int toBits(Rank rank, Suit suit) {
        // Put suit in lower bits, rank in upper bits for better organization
        return (suit.ordinal()) | (rank.ordinal() << SUIT_BITS);
    }

    public int toBits() {
        return bitValue;
    }

    // Create card from bits
    public static Card fromBits(int bits) {
        int suitOrdinal = bits & SUIT_MASK;
        int rankOrdinal = (bits >> SUIT_BITS) & RANK_MASK;
        return new Card(Rank.values()[rankOrdinal], Suit.values()[suitOrdinal]);
    }
    
    // Utility methods for bit operations
    public static long cardToBitMask(Card card) {
        return 1L << card.toBits();
    }
    
    public static boolean isBitSet(long bitMask, Card card) {
        return (bitMask & cardToBitMask(card)) != 0;
    }
    
    public static long addCardToBitMask(long bitMask, Card card) {
        return bitMask | cardToBitMask(card);
    }
    
    public static long removeCardFromBitMask(long bitMask, Card card) {
        return bitMask & ~cardToBitMask(card);
    }
    
    // Count bits set in a mask
    public static int countCards(long bitMask) {
        return Long.bitCount(bitMask);
    }
    
    // Convert bit mask to card list
    public static List<Card> bitsToCards(long bitMask) {
        List<Card> cards = new ArrayList<>();
        for (int i = 0; i < 52; i++) {
            if ((bitMask & (1L << i)) != 0) {
                cards.add(fromBits(i));
            }
        }
        return cards;
    }

    public Suit getSuit() {
        return suit;
    }

    public Rank getRank() {
        return rank;
    }

    @Override
    public String toString() {
        return String.valueOf(rank.getSymbol()) + suit.getSymbol();
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

    @Override
    public int compareTo(Card other) {
        int rankComparison = this.rank.compareTo(other.rank);
        if (rankComparison != 0) {
            return rankComparison;
        }
        return this.suit.compareTo(other.suit);
    }
}

