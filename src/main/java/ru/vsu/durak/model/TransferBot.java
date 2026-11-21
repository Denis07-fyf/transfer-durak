package ru.vsu.durak.model;

import java.util.List;

/** Бот, который сначала пытается перевести атаку, а потом отбивается. */
public class TransferBot extends SimpleBot {

    /** Выбирает допустимую карту для перевода того же достоинства. */
    @Override
    public Card chooseTransfer(GameState state, int playerIndex) {
        if (state.getRoundNumber() == 1 || state.isTaking() || state.isFinished()
                || playerIndex != state.getDefenderIndex()) return null;

        List<TablePair> table = state.getTable();
        if (table.isEmpty() || table.size() >= 6) return null;

        for (TablePair pair : table) {
            if (pair.isDefended() || pair.attack().rank() != table.getFirst().attack().rank()) {
                return null;
            }
        }
        int nextPlayer = state.nextActiveAfter(playerIndex);
        if (state.getPlayers().get(nextPlayer).getCardCount() < table.size() + 1) {
            return null;
        }
        Rank rank = table.getFirst().attack().rank();
        return state.getPlayers().get(playerIndex).getHand().stream()
                .filter(card -> card.rank() == rank)
                .min(CardOrder.byStrength(state.getTrumpSuit()))
                .orElse(null);
    }
}
