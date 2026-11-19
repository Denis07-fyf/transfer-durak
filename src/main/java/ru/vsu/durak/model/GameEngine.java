package ru.vsu.durak.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Проверяет ходы и изменяет состояние; в консоль ничего не печатает. */
public class GameEngine {
    private final GameState state;
    private final List<GameEvent> events = new ArrayList<>();

    /** Создаёт движок для готового состояния партии. */
    public GameEngine(GameState state) {
        this.state = Objects.requireNonNull(state);
    }

    /** Возвращает текущее состояние партии. */
    public GameState getState() { return state; }

    /** Возвращает журнал выполненных действий. */
    public List<GameEvent> getEvents() { return List.copyOf(events); }

    /** Кладёт атакующую карту на стол или выбрасывает исключение. */
    public GameEvent attack(int playerIndex, Card card) {
        Player player = playerAt(playerIndex);
        if (card == null) throw new IllegalArgumentException("Карта не указана");
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

    /** Отбивает конкретную атаку картой защищающегося игрока. */
    public GameEvent defend(int playerIndex, Card card, int pairIndex) {
        Player player = playerAt(playerIndex);
        if (card == null) throw new IllegalArgumentException("Карта не указана");
        if (playerIndex != state.getDefenderIndex()) {
            throw new IllegalArgumentException("Отбиваться может только защищающийся");
        }
        List<TablePair> table = state.getTable();
        if (pairIndex < 0 || pairIndex >= table.size()) {
            throw new IllegalArgumentException("Нет такой атаки на столе");
        }
        TablePair pair = table.get(pairIndex);
        if (pair.isDefended()) {
            throw new IllegalArgumentException("Эта карта уже отбита");
        }
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

    private Player playerAt(int index) {
        if (index < 0 || index >= state.getPlayers().size()) {
            throw new IllegalArgumentException("Нет такого игрока");
        }
        return state.getPlayers().get(index);
    }
}
