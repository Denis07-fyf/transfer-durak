package ru.vsu.durak.model;

import java.util.Objects;

/** Карта с достоинством и мастью. */
public record Card(Rank rank, Suit suit) {

    /** Проверяет, что у карты указаны достоинство и масть. */
    public Card {
        Objects.requireNonNull(rank);
        Objects.requireNonNull(suit);
    }

    /** Проверяет, может ли эта карта побить атаку. */
    public boolean beats(Card attack, Suit trump) {
        Objects.requireNonNull(attack);
        Objects.requireNonNull(trump);
        if (suit == attack.suit()) {
            return rank.strength() > attack.rank().strength();
        }
        return suit == trump && attack.suit() != trump;
    }

    /** Возвращает название карты, например 6♠. */
    @Override
    public String toString() {
        return rank.label() + suit.symbol();
    }
}
