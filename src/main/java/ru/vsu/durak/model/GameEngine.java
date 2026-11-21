package ru.vsu.durak.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Правила партии. Здесь нет вывода в консоль и работы с графикой. */
public class GameEngine {
    private final GameState state;
    private final List<GameEvent> events = new ArrayList<>();

    /** Создаёт движок для состояния партии. */
    public GameEngine(GameState state) {
        this.state = Objects.requireNonNull(state);
    }

    /** Возвращает состояние партии. */
    public GameState getState() { return state; }

    /** Возвращает журнал выполненных действий. */
    public List<GameEvent> getEvents() { return List.copyOf(events); }

    /** Выполняет атаку; при неверном ходе выбрасывает исключение. */
    public GameEvent attack(int playerIndex, Card card) {
        checkGameIsRunning();
        Player player = playerAt(playerIndex);
        if (card == null) throw new IllegalArgumentException("Карта не указана");
        if (state.isOut(playerIndex)) throw new IllegalArgumentException("Игрок уже вышел");
        if (playerIndex == state.getDefenderIndex()) {
            throw new IllegalArgumentException("Защищающийся не может атаковать");
        }
        List<TablePair> table = state.getTable();
        if (table.isEmpty() && playerIndex != state.getAttackerIndex()) {
            throw new IllegalArgumentException("Первым ходит назначенный атакующий");
        }
        if (!player.getHand().contains(card)) {
            throw new IllegalArgumentException("Такой карты нет на руке");
        }
        if (!table.isEmpty()) {
            if (table.size() >= state.getAttackLimit()) {
                throw new IllegalArgumentException("Превышен лимит карт для атаки");
            }
            boolean sameRank = false;
            for (TablePair pair : table) {
                if (card.rank() == pair.attack().rank()
                        || pair.defense() != null && card.rank() == pair.defense().rank()) {
                    sameRank = true;
                    break;
                }
            }
            if (!sameRank) {
                throw new IllegalArgumentException("Подкидывать можно только совпадающее достоинство");
            }
        }
        state.putAttack(playerIndex, card);
        GameEvent event = new GameEvent(GameEvent.Type.ATTACK,
                playerIndex, card, state.getTable().size() - 1);
        events.add(event);
        return event;
    }

    /** Отбивает конкретную атакующую карту. */
    public GameEvent defend(int playerIndex, Card card, int pairIndex) {
        checkGameIsRunning();
        Player player = playerAt(playerIndex);
        if (state.isTaking()) throw new IllegalArgumentException("Защитник уже решил взять карты");
        if (card == null) throw new IllegalArgumentException("Карта не указана");
        if (playerIndex != state.getDefenderIndex()) {
            throw new IllegalArgumentException("Отбиваться может только защищающийся");
        }
        List<TablePair> table = state.getTable();
        if (pairIndex < 0 || pairIndex >= table.size()) {
            throw new IllegalArgumentException("Нет такой атаки на столе");
        }
        TablePair pair = table.get(pairIndex);
        if (pair.isDefended()) throw new IllegalArgumentException("Эта карта уже отбита");
        if (!player.getHand().contains(card)) {
            throw new IllegalArgumentException("Такой карты нет на руке");
        }
        if (!card.beats(pair.attack(), state.getTrumpSuit())) {
            throw new IllegalArgumentException("Эта карта не бьёт атаку");
        }
        state.putDefense(playerIndex, card, pairIndex);
        GameEvent event = new GameEvent(GameEvent.Type.DEFEND, playerIndex, card, pairIndex);
        events.add(event);
        return event;
    }

    /** Переводит неотбитую атаку картой того же достоинства. */
    public GameEvent transfer(int playerIndex, Card card) {
        checkGameIsRunning();
        Player player = playerAt(playerIndex);
        checkTransfer(playerIndex, card);
        if (!player.getHand().contains(card)) {
            throw new IllegalArgumentException("Такой карты нет на руке");
        }
        int nextDefender = state.nextActiveAfter(playerIndex);
        if (state.getTable().size() + 1 > state.getPlayers().get(nextDefender).getCardCount()
                || state.getTable().size() + 1 > 6) {
            throw new IllegalArgumentException("У следующего игрока мало карт для перевода");
        }
        state.putTransfer(playerIndex, card);
        GameEvent event = new GameEvent(GameEvent.Type.TRANSFER,
                playerIndex, card, state.getTable().size() - 1);
        events.add(event);
        return event;
    }

    /** Защищающийся объявляет «Беру». После этого можно подкинуть ещё карты. */
    public GameEvent take(int playerIndex) {
        checkGameIsRunning();
        playerAt(playerIndex);
        if (state.getDefenderIndex() != playerIndex) {
            throw new IllegalArgumentException("Взять может только защищающийся");
        }
        if (state.getTable().isEmpty()) throw new IllegalArgumentException("На столе нет карт");
        if (state.isTaking()) throw new IllegalArgumentException("Взятие уже объявлено");
        state.startTaking();
        GameEvent event = new GameEvent(GameEvent.Type.TAKE, playerIndex, null, -1);
        events.add(event);
        return event;
    }

    /** Завершает раунд: карты уходят в отбой или к взявшему, затем идёт добор. */
    public GameEvent finishRound() {
        checkGameIsRunning();
        List<TablePair> table = state.getTable();
        if (table.isEmpty()) throw new IllegalArgumentException("Раунд ещё не начался");
        if (!state.isTaking()) {
            for (TablePair pair : table) {
                if (!pair.isDefended()) {
                    throw new IllegalArgumentException("Нельзя объявить бито: есть неотбитые карты");
                }
            }
        }
        boolean took = state.isTaking();
        int defender = state.getDefenderIndex();
        state.completeRound();
        GameEvent event = new GameEvent(took ? GameEvent.Type.PICKUP : GameEvent.Type.BITO,
                defender, null, -1);
        events.add(event);
        if (state.isFinished()) {
            events.add(new GameEvent(GameEvent.Type.GAME_OVER, state.getLoserIndex(), null, -1));
        }
        return event;
    }

    private void checkTransfer(int playerIndex, Card card) {
        playerAt(playerIndex);
        if (state.getRoundNumber() == 1) {
            throw new IllegalArgumentException("В первом кону перевод запрещён");
        }
        if (state.isTaking()) throw new IllegalArgumentException("Защитник решил взять карты");
        if (card == null) throw new IllegalArgumentException("Карта не указана");
        if (playerIndex != state.getDefenderIndex()) {
            throw new IllegalArgumentException("Перевести может только защищающийся");
        }
        List<TablePair> table = state.getTable();
        if (table.isEmpty()) throw new IllegalArgumentException("Нечего переводить");
        for (TablePair pair : table) {
            if (pair.isDefended()) {
                throw new IllegalArgumentException("После начала отбоя перевод запрещён");
            }
            if (pair.attack().rank() != card.rank()) {
                throw new IllegalArgumentException("Для перевода нужно то же достоинство");
            }
        }
    }

    private void checkGameIsRunning() {
        if (state.isFinished()) throw new IllegalArgumentException("Партия уже закончилась");
    }

    private Player playerAt(int index) {
        if (index < 0 || index >= state.getPlayers().size()) {
            throw new IllegalArgumentException("Нет такого игрока");
        }
        return state.getPlayers().get(index);
    }
}
