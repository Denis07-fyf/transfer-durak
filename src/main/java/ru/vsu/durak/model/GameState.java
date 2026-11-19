package ru.vsu.durak.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/** Состояние партии: игроки, колода, козырь и карты на столе. */
public class GameState {
    private final List<Player> players = new ArrayList<>();
    private final Deque<Card> stock;
    private final Card trumpCard;
    private final int attackerIndex;
    private final int defenderIndex;
    private final List<TablePair> table = new ArrayList<>();
    private int attackLimit;

    /** Создаёт начальную раздачу для 2–4 игроков по одному seed. */
    public GameState(List<String> playerNames, long seed) {
        if (playerNames == null || playerNames.size() < 2 || playerNames.size() > 4) {
            throw new IllegalArgumentException("Нужно от 2 до 4 игроков");
        }
        if (new HashSet<>(playerNames).size() != playerNames.size()) {
            throw new IllegalArgumentException("Имена игроков должны отличаться");
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
    }

    /** Возвращает игроков в порядке ходов. */
    public List<Player> getPlayers() { return List.copyOf(players); }

    /** Возвращает оставшуюся колоду, верхняя карта — первая. */
    public List<Card> getStockCards() { return List.copyOf(stock); }

    /** Возвращает число карт в колоде. */
    public int getStockSize() { return stock.size(); }

    /** Возвращает открытую нижнюю карту колоды. */
    public Card getTrumpCard() { return trumpCard; }

    /** Возвращает козырную масть. */
    public Suit getTrumpSuit() { return trumpCard.suit(); }

    /** Возвращает номер атакующего. */
    public int getAttackerIndex() { return attackerIndex; }

    /** Возвращает номер защищающегося. */
    public int getDefenderIndex() { return defenderIndex; }

    /** Возвращает пары карт на столе. */
    public List<TablePair> getTable() { return List.copyOf(table); }

    /** Возвращает максимальное число атак за этот раунд, ноль до первой атаки. */
    public int getAttackLimit() { return attackLimit; }

    // Вызываются только из GameEngine после проверки правил.
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
}
