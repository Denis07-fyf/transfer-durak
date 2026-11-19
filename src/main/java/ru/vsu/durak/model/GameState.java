package ru.vsu.durak.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

/** Начальное состояние партии: игроки, раздача, колода и козырь. */
public class GameState {
    private final List<Player> players = new ArrayList<>();
    private final Deque<Card> stock;
    private final Card trumpCard;
    private final int attackerIndex;
    private final int defenderIndex;

    /** Создаёт партию для 2–4 игроков, перемешивая колоду по seed. */
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

        // Начинает тот, у кого младший козырь. Если ни у кого нет — первый игрок.
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

    /** Возвращает игроков в порядке их ходов. */
    public List<Player> getPlayers() {
        return List.copyOf(players);
    }

    /** Возвращает оставшиеся в колоде карты, верхняя — первая. */
    public List<Card> getStockCards() {
        return List.copyOf(stock);
    }

    /** Возвращает количество карт в колоде. */
    public int getStockSize() {
        return stock.size();
    }

    /** Возвращает открытую козырную карту внизу колоды. */
    public Card getTrumpCard() {
        return trumpCard;
    }

    /** Возвращает козырную масть. */
    public Suit getTrumpSuit() {
        return trumpCard.suit();
    }

    /** Возвращает номер атакующего игрока. */
    public int getAttackerIndex() {
        return attackerIndex;
    }

    /** Возвращает номер защищающегося игрока. */
    public int getDefenderIndex() {
        return defenderIndex;
    }
}
