package ru.vsu.durak.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Запускает партию ботов, используя существующий игровой движок. */
public class BotGame {
    private final GameState state;
    private final GameEngine engine;
    private final List<BotStrategy> strategies;

    /** Создаёт партию с заданными ботами и seed. */
    public BotGame(List<String> playerNames, List<BotStrategy> strategies, long seed) {
        Objects.requireNonNull(strategies);
        if (playerNames == null || strategies.size() != playerNames.size()
                || strategies.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("На каждого игрока нужна одна стратегия");
        }
        this.state = new GameState(playerNames, seed);
        this.engine = new GameEngine(state);
        this.strategies = List.copyOf(strategies);
    }

    /** Возвращает текущее состояние партии. */
    public GameState getState() { return state; }

    /** Возвращает журнал действий движка. */
    public List<GameEvent> getEvents() { return engine.getEvents(); }

    /** Играет один полный раунд. */
    public void playRound() {
        if (state.isFinished()) throw new IllegalStateException("Партия завершена");

        int attacker = state.getAttackerIndex();
        Card attack = strategies.get(attacker).chooseAttack(state, attacker);
        if (attack == null) throw new IllegalStateException("Бот не смог начать атаку");
        engine.attack(attacker, attack);

        // После каждого действия либо меняется стол, либо раунд заканчивается.
        while (true) {
            if (state.isTaking()) {
                Card extra = chooseExtraAttack();
                if (extra == null) break;
                engine.attack(state.getAttackerIndex(), extra);
                continue;
            }

            int defender = state.getDefenderIndex();
            BotStrategy defenseBot = strategies.get(defender);
            Card transfer = defenseBot.chooseTransfer(state, defender);
            if (transfer != null) {
                engine.transfer(defender, transfer);
                continue;
            }

            boolean took = false;
            for (int i = 0; i < state.getTable().size(); i++) {
                TablePair pair = state.getTable().get(i);
                if (pair.isDefended()) continue;

                Card defense = defenseBot.chooseDefense(state, defender, pair.attack());
                if (defense == null) {
                    engine.take(defender);
                    took = true;
                    break;
                }
                engine.defend(defender, defense, i);
            }
            if (took) continue;

            Card extra = chooseExtraAttack();
            if (extra == null) break;
            engine.attack(state.getAttackerIndex(), extra);
        }
        engine.finishRound();
    }

    /** Играет до проигравшего или ничьей; без вывода в консоль. */
    public void play() {
        int rounds = 0;
        while (!state.isFinished() && rounds < 1000) {
            playRound();
            rounds++;
        }
        if (!state.isFinished()) throw new IllegalStateException("Слишком много раундов");
    }

    private Card chooseExtraAttack() {
        int attacker = state.getAttackerIndex();
        return strategies.get(attacker).chooseAttack(state, attacker);
    }
}
