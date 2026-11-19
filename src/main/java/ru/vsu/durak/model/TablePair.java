package ru.vsu.durak.model;

import java.util.Objects;

/** Пара карт на столе: атака и, возможно, защита. */
public record TablePair(Card attack, Card defense) {
    /** Создаёт пару, в которой атакующая карта обязательна. */
    public TablePair {
        Objects.requireNonNull(attack);
    }

    /** Показывает, отбита ли атакующая карта. */
    public boolean isDefended() {
        return defense != null;
    }
}
