package ru.vsu.durak;

import ru.vsu.durak.model.BotGame;
import ru.vsu.durak.model.BotStrategy;
import ru.vsu.durak.model.GameEvent;
import ru.vsu.durak.model.GameState;
import ru.vsu.durak.model.SimpleBot;
import ru.vsu.durak.model.TransferBot;

import java.util.List;

/** Консольная демонстрация полной партии между ботами. */
public class Main {
    /** Запускает партию с фиксированным начальным значением генератора. */
    public static void main(String[] args) {
        List<BotStrategy> bots = List.of(new SimpleBot(), new TransferBot());
        BotGame game = new BotGame(List.of("Простой бот", "Переводящий бот"), bots, 42);
        GameState state = game.getState();

        System.out.println("ПЕРЕВОДНОЙ ДУРАК");
        System.out.println("Козырь: " + state.getTrumpCard());
        while (!state.isFinished()) {
            int start = game.getEvents().size();
            System.out.println("\nРаунд " + state.getRoundNumber());
            game.playRound();
            List<GameEvent> events = game.getEvents();
            for (int i = start; i < events.size(); i++) {
                GameEvent event = events.get(i);
                String player = event.playerIndex() < 0 ? "нет" :
                        state.getPlayers().get(event.playerIndex()).getName();
                String card = event.card() == null ? "" : " " + event.card();
                System.out.println("  " + player + ": " + event.type() + card);
            }
            System.out.println("Колода: " + state.getStockSize());
        }
        if (state.getLoserIndex() == -1) {
            System.out.println("\nНичья!");
        } else {
            System.out.println("\nПроиграл: "
                    + state.getPlayers().get(state.getLoserIndex()).getName());
        }
    }
}
