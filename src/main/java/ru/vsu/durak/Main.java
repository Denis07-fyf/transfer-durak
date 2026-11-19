package ru.vsu.durak;

import ru.vsu.durak.model.Card;
import ru.vsu.durak.model.Deck;

import java.util.List;
import java.util.Random;

/** Точка входа в приложение. */
public class Main {
    /** Пока демонстрирует создание колоды. */
    public static void main(String[] args) {
        List<Card> cards = Deck.shuffle(new Random(42));
        System.out.println("Всего карт: " + cards.size());
        System.out.println("Первые шесть карт: " + cards.subList(0, 6));
    }
}
