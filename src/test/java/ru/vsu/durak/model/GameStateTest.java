package ru.vsu.durak.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameStateTest {
    @Test
    void twoPlayersGetSixCards() {
        GameState game = new GameState(List.of("A", "B"), 42);
        assertEquals(6, game.getPlayers().get(0).getCardCount());
        assertEquals(6, game.getPlayers().get(1).getCardCount());
        assertEquals(24, game.getStockSize());
    }

    @Test
    void threePlayersGetSixCards() {
        GameState game = new GameState(List.of("A", "B", "C"), 42);
        assertTrue(game.getPlayers().stream().allMatch(p -> p.getCardCount() == 6));
        assertEquals(18, game.getStockSize());
    }

    @Test
    void fourPlayersGetSixCards() {
        GameState game = new GameState(List.of("A", "B", "C", "D"), 42);
        assertTrue(game.getPlayers().stream().allMatch(p -> p.getCardCount() == 6));
        assertEquals(12, game.getStockSize());
    }

    @Test
    void allCardsAreStillUnique() {
        GameState game = new GameState(List.of("A", "B", "C"), 42);
        List<Card> all = new ArrayList<>(game.getStockCards());
        for (Player player : game.getPlayers()) {
            all.addAll(player.getHand());
        }
        assertEquals(36, all.size());
        assertEquals(36, new HashSet<>(all).size());
    }

    @Test
    void sameSeedGivesSameInitialState() {
        GameState first = new GameState(List.of("A", "B"), 42);
        GameState second = new GameState(List.of("A", "B"), 42);
        assertEquals(first.getTrumpCard(), second.getTrumpCard());
        assertEquals(first.getStockCards(), second.getStockCards());
        assertEquals(first.getPlayers().get(0).getHand(), second.getPlayers().get(0).getHand());
        assertEquals(first.getPlayers().get(1).getHand(), second.getPlayers().get(1).getHand());
        assertEquals(first.getAttackerIndex(), second.getAttackerIndex());
    }

    @Test
    void openTrumpIsLastStockCard() {
        GameState game = new GameState(List.of("A", "B"), 42);
        List<Card> stock = game.getStockCards();
        assertEquals(stock.get(stock.size() - 1), game.getTrumpCard());
    }

    @Test
    void startsWithLowestTrumpIfSomeoneHasIt() {
        GameState game = new GameState(List.of("A", "B", "C"), 42);
        int lowest = Integer.MAX_VALUE;
        int expected = 0;
        for (int i = 0; i < game.getPlayers().size(); i++) {
            for (Card card : game.getPlayers().get(i).getHand()) {
                if (card.suit() == game.getTrumpSuit() && card.rank().strength() < lowest) {
                    lowest = card.rank().strength();
                    expected = i;
                }
            }
        }
        assertEquals(expected, game.getAttackerIndex());
    }

    @Test
    void defenderIsNextPlayer() {
        GameState game = new GameState(List.of("A", "B", "C"), 42);
        assertEquals((game.getAttackerIndex() + 1) % 3, game.getDefenderIndex());
    }

    @Test
    void wrongNumberOfPlayersThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> new GameState(List.of("A"), 42));
        assertThrows(IllegalArgumentException.class,
                () -> new GameState(List.of("A", "B", "C", "D", "E"), 42));
    }

    @Test
    void duplicateNamesThrowException() {
        assertThrows(IllegalArgumentException.class,
                () -> new GameState(List.of("A", "A"), 42));
    }

    @Test
    void playerHandCannotBeChangedDirectly() {
        GameState game = new GameState(List.of("A", "B"), 42);
        Player player = game.getPlayers().get(0);
        assertThrows(UnsupportedOperationException.class,
                () -> player.getHand().clear());
        assertEquals(6, player.getCardCount());
    }

    @Test
    void missingCardThrowsException() {
        Player player = new Player("A");
        assertThrows(IllegalArgumentException.class,
                () -> player.removeCard(new Card(Rank.SIX, Suit.SPADES)));
    }

    @Test
    void blankNameThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> new Player(" "));
    }
}
