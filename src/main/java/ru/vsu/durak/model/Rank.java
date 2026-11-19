package ru.vsu.durak.model;

/** Достоинства карт */
public enum Rank {
    SIX(6, "6"), SEVEN(7, "7"), EIGHT(8, "8"), NINE(9, "9"),
    TEN(10, "10"), JACK(11, "В"), QUEEN(12, "Д"),
    KING(13, "К"), ACE(14, "Т");

    private final int strength;//для сравнения
    private final String label;// для отбр карты

    Rank(int strength, String label) {
        this.strength = strength;
        this.label = label;
    }

    /** Возвращает силу карты для сравнения. */
    public int strength() {
        return strength;
    }

    /** Возвращает короткое название достоинства. */
    public String label() {
        return label;
    }
}
