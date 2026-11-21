package ru.vsu.durak.model;

/** Правила выбора ходов ботом. Сама стратегия не изменяет партию. */
public interface BotStrategy {
    /** Выбирает карту для атаки или подкидывания; null, если ходить нечем. */
    Card chooseAttack(GameState state, int playerIndex);

    /** Выбирает карту защиты для одной атаки; null, если отбиться нечем. */
    Card chooseDefense(GameState state, int playerIndex, Card attack);

    /** Выбирает карту для перевода; null, если перевод не нужен. */
    default Card chooseTransfer(GameState state, int playerIndex) {
        return null;
    }
}
