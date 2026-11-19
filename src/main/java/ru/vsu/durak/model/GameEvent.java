package ru.vsu.durak.model;

/** Запись одного выполненного хода для консоли и будущего журнала. */
public record GameEvent(Type type, int playerIndex, Card card, int pairIndex) {
    /** Два вида действий, которые доступны на этом этапе. */
    public enum Type { ATTACK, DEFEND }
}
