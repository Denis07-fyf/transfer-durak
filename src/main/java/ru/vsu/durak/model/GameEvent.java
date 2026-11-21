package ru.vsu.durak.model;

/** Одно событие для журнала партии и интерфейса. */
public record GameEvent(Type type, int playerIndex, Card card, int pairIndex) {
    /** Возможные действия и результаты завершения раунда. */
    public enum Type {
        ATTACK, DEFEND, TRANSFER, TRUMP_SHOW_TRANSFER,
        TAKE, PICKUP, BITO, GAME_OVER
    }
}
