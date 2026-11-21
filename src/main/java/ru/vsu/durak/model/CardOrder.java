package ru.vsu.durak.model;

import java.util.Comparator;
import java.util.Objects;

/** Сортировка карт: сначала некозырные, затем козырные. */
public final class CardOrder {
    private CardOrder() { }

    /** Сравнивает карты по козырности, достоинству и масти. */
    public static Comparator<Card> byStrength(Suit trump) {
        Objects.requireNonNull(trump);
        return Comparator.comparingInt((Card card) -> card.suit() == trump ? 1 : 0)
                .thenComparingInt(card -> card.rank().strength())
                .thenComparingInt(card -> card.suit().ordinal());
    }
}
