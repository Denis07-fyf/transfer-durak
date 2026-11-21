package ru.vsu.durak.model;

import java.util.List;
import java.util.Objects;

/** Простой бот: ходит самой дешёвой картой и пытается отбиться. */
public class SimpleBot implements BotStrategy {

    /** Находит самую слабую карту для допустимой атаки. */
    @Override
    public Card chooseAttack(GameState state, int playerIndex) {
        Objects.requireNonNull(state);
        if (state.isFinished() || state.isOut(playerIndex)
                || playerIndex == state.getDefenderIndex()) return null;

        List<TablePair> table = state.getTable();
        if (table.isEmpty() && playerIndex != state.getAttackerIndex()) return null;
        if (!table.isEmpty() && table.size() >= state.getAttackLimit()) return null;

        return state.getPlayers().get(playerIndex).getHand().stream()
                .filter(card -> table.isEmpty() || table.stream().anyMatch(pair ->
                        card.rank() == pair.attack().rank()
                        || pair.defense() != null && card.rank() == pair.defense().rank()))
                .min(CardOrder.byStrength(state.getTrumpSuit()))
                .orElse(null);
    }

    /** Ищет самую дешёвую карту, которой можно отбить атаку. */
    @Override
    public Card chooseDefense(GameState state, int playerIndex, Card attack) {
        Objects.requireNonNull(state);
        Objects.requireNonNull(attack);
        if (state.isTaking() || playerIndex != state.getDefenderIndex()) return null;

        return state.getPlayers().get(playerIndex).getHand().stream()
                .filter(card -> card.beats(attack, state.getTrumpSuit()))
                .min(CardOrder.byStrength(state.getTrumpSuit()))
                .orElse(null);
    }
}
