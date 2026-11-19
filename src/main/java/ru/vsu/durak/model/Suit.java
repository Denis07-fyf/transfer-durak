package ru.vsu.durak.model;

/** Масти */
public enum Suit {//перечисл
    CLUBS("♣"), DIAMONDS("♦"), HEARTS("♥"), SPADES("♠");

    private final String symbol;//для значка

    Suit(String symbol) {
        this.symbol = symbol;
    }//констр для зап знач

    /** Возвращает масти. */
    public String symbol() {
        return symbol;
    }
}
