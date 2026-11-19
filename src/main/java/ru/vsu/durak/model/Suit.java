package ru.vsu.durak.model;

/** Масти игральных карт. */
public enum Suit {
    CLUBS("♣"), DIAMONDS("♦"), HEARTS("♥"), SPADES("♠");

    private final String symbol;

    Suit(String symbol) {
        this.symbol = symbol;
    }

    /** Возвращает обозначение масти. */
    public String symbol() {
        return symbol;
    }
}
