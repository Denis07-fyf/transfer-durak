package ru.vsu.durak.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Игрок и карты, которые находятся у него в руке. */
public class Player {
    private final String name;
    private final List<Card> hand = new ArrayList<>();

    /** Создаёт игрока с указанным именем. */
    public Player(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя игрока не должно быть пустым");
        }
        this.name = name;
    }

    /** Возвращает имя игрока. */
    public String getName() {
        return name;
    }

    /** Возвращает список карт; изменить его снаружи нельзя. */
    public List<Card> getHand() {
        return List.copyOf(hand);
    }

    /** Возвращает количество карт на руке. */
    public int getCardCount() {
        return hand.size();
    }

    /** Добавляет карту на руку. */
    public void addCard(Card card) {
        hand.add(Objects.requireNonNull(card));
    }

    /** Убирает карту с руки; если карты нет, сообщает об ошибке. */
    public void removeCard(Card card) {
        if (!hand.remove(card)) {
            throw new IllegalArgumentException("У игрока нет такой карты: " + card);
        }
    }
}
