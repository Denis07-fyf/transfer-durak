package ru.vsu.durak;

import ru.vsu.durak.model.GameState;
import ru.vsu.durak.model.Player;

import java.util.List;

/** Временное консольное приложение для проверки раздачи. */
public class Main {
    /** Создаёт партию с фиксированным seed и показывает раздачу. */
    public static void main(String[] args) {
        GameState game = new GameState(List.of("Игрок 1", "Игрок 2"), 42);
        System.out.println("Козырь: " + game.getTrumpCard());
        System.out.println("Осталось в колоде: " + game.getStockSize());

        for (int i = 0; i < game.getPlayers().size(); i++) {
            Player player = game.getPlayers().get(i);
            System.out.println(player.getName() + ": " + player.getHand());
        }
        System.out.println("Атакует: "
                + game.getPlayers().get(game.getAttackerIndex()).getName());
        System.out.println("Защищается: "
                + game.getPlayers().get(game.getDefenderIndex()).getName());
    }
}
