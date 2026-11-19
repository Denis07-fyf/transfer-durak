package ru.vsu.durak;

import ru.vsu.durak.model.Card;
import ru.vsu.durak.model.GameEngine;
import ru.vsu.durak.model.GameState;
import ru.vsu.durak.model.Player;
import ru.vsu.durak.model.TablePair;

import java.util.List;

/** Временный пример атаки и защиты без графического интерфейса. */
public class Main {
    /** Запускает небольшой фрагмент партии с фиксированным seed. */
    public static void main(String[] args) {
        GameState game = new GameState(List.of("Игрок 1", "Игрок 2"), 42);
        GameEngine engine = new GameEngine(game);

        System.out.println("Козырь: " + game.getTrumpCard());
        System.out.println("Колода: " + game.getStockSize() + " карт");
        for (Player player : game.getPlayers()) {
            System.out.println(player.getName() + ": " + player.getHand());
        }

        int attacker = game.getAttackerIndex();
        int defender = game.getDefenderIndex();
        Card firstCard = game.getPlayers().get(attacker).getHand().getFirst();
        engine.attack(attacker, firstCard);
        System.out.println("Атака: " + firstCard);

        Card response = null;
        for (Card card : game.getPlayers().get(defender).getHand()) {
            if (card.beats(firstCard, game.getTrumpSuit())) {
                response = card;
                break;
            }
        }
        if (response != null) {
            engine.defend(defender, response, 0);
            System.out.println("Защита: " + response);
        } else {
            System.out.println("У защитника нет подходящей карты");
        }

        System.out.println("Карты на столе:");
        for (TablePair pair : game.getTable()) {
            System.out.println(pair.attack() + " → "
                    + (pair.defense() == null ? "не отбита" : pair.defense()));
        }
        System.out.println("Событий в журнале: " + engine.getEvents().size());
    }
}
