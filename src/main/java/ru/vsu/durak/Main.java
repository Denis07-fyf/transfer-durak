package ru.vsu.durak;

import ru.vsu.durak.model.Card;
import ru.vsu.durak.model.GameEngine;
import ru.vsu.durak.model.GameState;

import java.util.List;

/** Пример одного завершённого раунда. Графика и боты будут в следующих блоках. */
public class Main {
    /** Запускает учебную демонстрацию. */
    public static void main(String[] args) {
        GameState game = new GameState(List.of("Игрок 1", "Игрок 2"), 42);
        GameEngine engine = new GameEngine(game);

        int attacker = game.getAttackerIndex();
        int defender = game.getDefenderIndex();
        Card attack = game.getPlayers().get(attacker).getHand().getFirst();

        System.out.println("Козырь: " + game.getTrumpCard());
        System.out.println("Атакует: " + game.getPlayers().get(attacker).getName());
        System.out.println("Защищается: " + game.getPlayers().get(defender).getName());
        engine.attack(attacker, attack);
        System.out.println("Атака: " + attack);

        Card defense = null;
        for (Card card : game.getPlayers().get(defender).getHand()) {
            if (card.beats(attack, game.getTrumpSuit())) {
                defense = card;
                break;
            }
        }
        if (defense == null) {
            engine.take(defender);
            engine.finishRound();
            System.out.println("Защитник взял карты");
        } else {
            engine.defend(defender, defense, 0);
            engine.finishRound();
            System.out.println("Защита: " + defense + ", бито!");
        }
        System.out.println("Следующий атакующий: "
                + game.getPlayers().get(game.getAttackerIndex()).getName());
        System.out.println("В колоде: " + game.getStockSize());
        System.out.println("В отбое: " + game.getDiscardPile().size());
        System.out.println("События: " + engine.getEvents().size());
    }
}
