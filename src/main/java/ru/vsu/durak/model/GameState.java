package ru.vsu.durak.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Состояние партии: игроки, колода, стол и текущий раунд. */
public class GameState {
    private final List<Player> players = new ArrayList<>();
    private final Deque<Card> stock;
    private final Card trumpCard;
    private final List<TablePair> table = new ArrayList<>();
    private final List<Card> discardPile = new ArrayList<>();
    private final Set<Integer> outPlayers = new HashSet<>();
    private int attackerIndex;
    private int defenderIndex;
    private int roundLeaderIndex;
    private int attackLimit;
    private int roundNumber = 1;
    private boolean taking;
    private boolean finished;
    private int loserIndex = -1;

    /** Создаёт раздачу для 2–4 игроков с заданным seed. */
    public GameState(List<String> playerNames, long seed) {
        if (playerNames == null || playerNames.size() < 2 || playerNames.size() > 4) {
            throw new IllegalArgumentException("Нужно от 2 до 4 игроков");
        }
        if (playerNames.stream().anyMatch(name -> name == null || name.isBlank())
                || new HashSet<>(playerNames).size() != playerNames.size()) {
            throw new IllegalArgumentException("Имена игроков должны быть непустыми и разными");
        }
        for (String name : playerNames) {
            players.add(new Player(name));
        }

        stock = new ArrayDeque<>(Deck.shuffle(new Random(seed)));
        trumpCard = stock.peekLast();
        for (int round = 0; round < 6; round++) {
            for (Player player : players) {
                player.addCard(stock.removeFirst());
            }
        }

        int firstPlayer = 0;
        int lowestTrump = Integer.MAX_VALUE;
        for (int i = 0; i < players.size(); i++) {
            for (Card card : players.get(i).getHand()) {
                if (card.suit() == trumpCard.suit()
                        && card.rank().strength() < lowestTrump) {
                    lowestTrump = card.rank().strength();
                    firstPlayer = i;
                }
            }
        }
        attackerIndex = firstPlayer;
        defenderIndex = (firstPlayer + 1) % players.size();
        roundLeaderIndex = attackerIndex;
    }

    /** Возвращает игроков в порядке посадки. */
    public List<Player> getPlayers() { return List.copyOf(players); }

    /** Возвращает оставшуюся колоду: верхняя карта первая. */
    public List<Card> getStockCards() { return List.copyOf(stock); }

    /** Возвращает число карт в колоде. */
    public int getStockSize() { return stock.size(); }

    /** Возвращает изначально открытую нижнюю карту колоды. */
    public Card getTrumpCard() { return trumpCard; }

    /** Возвращает козырную масть. */
    public Suit getTrumpSuit() { return trumpCard.suit(); }

    /** Возвращает номер текущего атакующего. */
    public int getAttackerIndex() { return attackerIndex; }

    /** Возвращает номер защищающегося. */
    public int getDefenderIndex() { return defenderIndex; }

    /** Возвращает карты на столе в виде пар атака–защита. */
    public List<TablePair> getTable() { return List.copyOf(table); }

    /** Возвращает максимальное количество атакующих карт в раунде. */
    public int getAttackLimit() { return attackLimit; }

    /** Возвращает номер текущего раунда, начиная с 1. */
    public int getRoundNumber() { return roundNumber; }

    /** Возвращает карты, ушедшие в отбой. */
    public List<Card> getDiscardPile() { return List.copyOf(discardPile); }

    /** Показывает, что защитник решил взять карты со стола. */
    public boolean isTaking() { return taking; }

    /** Показывает, закончена ли партия. */
    public boolean isFinished() { return finished; }

    /** Возвращает проигравшего либо -1, если партия ещё идёт или ничья. */
    public int getLoserIndex() { return loserIndex; }

    /** Показывает, вышел ли игрок из партии после опустошения колоды. */
    public boolean isOut(int index) { return outPlayers.contains(index); }

    void putAttack(int playerIndex, Card card) {
        if (table.isEmpty()) {
            attackLimit = Math.min(6, players.get(defenderIndex).getCardCount());
        }
        players.get(playerIndex).removeCard(card);
        table.add(new TablePair(card, null));
    }

    void putDefense(int playerIndex, Card card, int pairIndex) {
        players.get(playerIndex).removeCard(card);
        TablePair pair = table.get(pairIndex);
        table.set(pairIndex, new TablePair(pair.attack(), card));
    }

    void putTransfer(int playerIndex, Card card) {
        players.get(playerIndex).removeCard(card);
        table.add(new TablePair(card, null));
        moveDefenseToNext(playerIndex);
    }

    int nextActiveAfter(int index) {
        int next = (index + 1) % players.size();
        while (outPlayers.contains(next)) {
            next = (next + 1) % players.size();
        }
        return next;
    }

    void startTaking() { taking = true; }

    void completeRound() {
        int oldDefender = defenderIndex;
        boolean pickedUp = taking;

        for (TablePair pair : table) {
            if (pickedUp) {
                players.get(oldDefender).addCard(pair.attack());
                if (pair.defense() != null) players.get(oldDefender).addCard(pair.defense());
            } else {
                discardPile.add(pair.attack());
                if (pair.defense() != null) discardPile.add(pair.defense());
            }
        }
        table.clear();

        // Добор: первый атакующий раунда, остальные по кругу, защитник последний.
        int current = roundLeaderIndex;
        for (int i = 0; i < players.size(); i++) {
            if (current != oldDefender && !outPlayers.contains(current)) {
                drawToSix(current);
            }
            current = (current + 1) % players.size();
        }
        if (!outPlayers.contains(oldDefender)) drawToSix(oldDefender);

        if (stock.isEmpty()) {
            for (int i = 0; i < players.size(); i++) {
                if (players.get(i).getCardCount() == 0) outPlayers.add(i);
            }
        }
        int activeCount = players.size() - outPlayers.size();
        if (activeCount <= 1) {
            finished = true;
            if (activeCount == 1) {
                for (int i = 0; i < players.size(); i++) {
                    if (!outPlayers.contains(i)) loserIndex = i;
                }
            }
        } else {
            int nextAttacker = pickedUp || outPlayers.contains(oldDefender)
                    ? nextActiveAfter(oldDefender) : oldDefender;
            attackerIndex = nextAttacker;
            defenderIndex = nextActiveAfter(nextAttacker);
            roundLeaderIndex = nextAttacker;
        }
        attackLimit = 0;
        taking = false;
        roundNumber++;
    }

    private void drawToSix(int playerIndex) {
        Player player = players.get(playerIndex);
        while (player.getCardCount() < 6 && !stock.isEmpty()) {
            player.addCard(stock.removeFirst());
        }
    }

    private void moveDefenseToNext(int oldDefender) {
        attackerIndex = oldDefender;
        defenderIndex = nextActiveAfter(oldDefender);
        attackLimit = Math.min(6, players.get(defenderIndex).getCardCount());
    }
}
