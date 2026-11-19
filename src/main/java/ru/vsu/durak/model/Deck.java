package ru.vsu.durak.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Создание колоды из 36 карт. */
public class Deck {

    /** Создаёт полную колоду без перемешивания. */
    public static List<Card> create() {
        List<Card> cards = new ArrayList<>();
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(rank, suit));
            }
        }
        return cards;
    }

    /** Создаёт перемешанную колоду с переданным генератором случайных чисел. */
    public static List<Card> shuffle(Random random) {
        List<Card> cards = create();
        Collections.shuffle(cards, random);
        return cards;
    }
}
