package ru.vsu.durak.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoundTest {
    private GameState game() { return new GameState(List.of("А", "Б"), 42); }

    private GameEngine start(GameState game) {
        GameEngine engine = new GameEngine(game);
        int attacker = game.getAttackerIndex();
        engine.attack(attacker, game.getPlayers().get(attacker).getHand().getFirst());
        return engine;
    }

    private record DefenseCase(GameState game, Card attack, Card defense) { }

    private DefenseCase findDefense() {
        for (int seed = 0; seed < 300; seed++) {
            GameState game = new GameState(List.of("А", "Б"), seed);
            for (Card attack : game.getPlayers().get(game.getAttackerIndex()).getHand()) {
                for (Card defense : game.getPlayers().get(game.getDefenderIndex()).getHand()) {
                    if (defense.beats(attack, game.getTrumpSuit())) {
                        return new DefenseCase(game, attack, defense);
                    }
                }
            }
        }
        throw new AssertionError("Нет раздачи для отбоя");
    }

    @Test void cannotFinishEmptyTable() {
        GameState game = game();
        assertThrows(IllegalArgumentException.class, () -> new GameEngine(game).finishRound());
    }

    @Test void cannotFinishUnbeatenTable() {
        GameState game = game();
        GameEngine engine = start(game);
        assertThrows(IllegalArgumentException.class, engine::finishRound);
        assertEquals(1, game.getTable().size());
        assertEquals(1, engine.getEvents().size());
    }

    @Test void onlyDefenderCanTake() {
        GameState game = game();
        GameEngine engine = start(game);
        assertThrows(IllegalArgumentException.class, () -> engine.take(game.getAttackerIndex()));
        assertFalse(game.isTaking());
    }

    @Test void cannotTakeEmptyTable() {
        GameState game = game();
        GameEngine engine = new GameEngine(game);
        assertThrows(IllegalArgumentException.class, () -> engine.take(game.getDefenderIndex()));
    }

    @Test void takeCanBeDeclaredOnce() {
        GameState game = game();
        GameEngine engine = start(game);
        engine.take(game.getDefenderIndex());
        assertThrows(IllegalArgumentException.class, () -> engine.take(game.getDefenderIndex()));
        assertTrue(game.isTaking());
    }

    @Test void takeDoesNotImmediatelyChangeCards() {
        GameState game = game();
        GameEngine engine = start(game);
        int defender = game.getDefenderIndex();
        List<Card> before = game.getPlayers().get(defender).getHand();
        engine.take(defender);
        assertEquals(before, game.getPlayers().get(defender).getHand());
        assertEquals(1, game.getTable().size());
    }

    @Test void cannotDefendAfterTake() {
        GameState game = game();
        GameEngine engine = start(game);
        int defender = game.getDefenderIndex();
        engine.take(defender);
        Card anyCard = game.getPlayers().get(defender).getHand().getFirst();
        assertThrows(IllegalArgumentException.class, () -> engine.defend(defender, anyCard, 0));
    }

    @Test void cannotTransferAfterTake() {
        GameState game = game();
        GameEngine engine = start(game);
        int defender = game.getDefenderIndex();
        engine.take(defender);
        Card anyCard = game.getPlayers().get(defender).getHand().getFirst();
        assertThrows(IllegalArgumentException.class, () -> engine.transfer(defender, anyCard));
    }

    @Test void takeMovesAttackCardToDefender() {
        GameState game = game();
        GameEngine engine = start(game);
        Card attack = game.getTable().getFirst().attack();
        int defender = game.getDefenderIndex();
        engine.take(defender);
        engine.finishRound();
        assertTrue(game.getPlayers().get(defender).getHand().contains(attack));
        assertTrue(game.getTable().isEmpty());
        assertEquals(0, game.getDiscardPile().size());
    }

    @Test void takeAfterDefenseCollectsBothCards() {
        DefenseCase c = findDefense();
        GameEngine engine = new GameEngine(c.game);
        int defender = c.game.getDefenderIndex();
        engine.attack(c.game.getAttackerIndex(), c.attack);
        engine.defend(defender, c.defense, 0);
        engine.take(defender);
        engine.finishRound();
        assertTrue(c.game.getPlayers().get(defender).getHand().contains(c.attack));
        assertTrue(c.game.getPlayers().get(defender).getHand().contains(c.defense));
    }

    @Test void beatenCardsGoToDiscard() {
        DefenseCase c = findDefense();
        GameEngine engine = new GameEngine(c.game);
        engine.attack(c.game.getAttackerIndex(), c.attack);
        engine.defend(c.game.getDefenderIndex(), c.defense, 0);
        assertEquals(GameEvent.Type.BITO, engine.finishRound().type());
        assertEquals(List.of(c.attack, c.defense), c.game.getDiscardPile());
        assertTrue(c.game.getTable().isEmpty());
    }

    @Test void successfulDefenseMakesDefenderNextAttacker() {
        DefenseCase c = findDefense();
        int defender = c.game.getDefenderIndex();
        GameEngine engine = new GameEngine(c.game);
        engine.attack(c.game.getAttackerIndex(), c.attack);
        engine.defend(defender, c.defense, 0);
        engine.finishRound();
        assertEquals(defender, c.game.getAttackerIndex());
    }

    @Test void afterTakeAttackPassesToNextPlayer() {
        GameState game = new GameState(List.of("А", "Б", "В"), 42);
        GameEngine engine = start(game);
        int oldDefender = game.getDefenderIndex();
        engine.take(oldDefender);
        engine.finishRound();
        assertEquals((oldDefender + 1) % 3, game.getAttackerIndex());
    }

    @Test void roundNumberGrowsAndAttackLimitResets() {
        GameState game = game();
        GameEngine engine = start(game);
        assertEquals(6, game.getAttackLimit());
        engine.take(game.getDefenderIndex());
        engine.finishRound();
        assertEquals(2, game.getRoundNumber());
        assertEquals(0, game.getAttackLimit());
        assertFalse(game.isTaking());
    }

    @Test void attackerRefillsToSixAfterTake() {
        GameState game = game();
        GameEngine engine = start(game);
        int attacker = game.getAttackerIndex();
        assertEquals(5, game.getPlayers().get(attacker).getCardCount());
        engine.take(game.getDefenderIndex());
        engine.finishRound();
        assertEquals(6, game.getPlayers().get(attacker).getCardCount());
        assertEquals(23, game.getStockSize());
    }

    @Test void defenderRefillsToSixAfterBito() {
        DefenseCase c = findDefense();
        GameEngine engine = new GameEngine(c.game);
        int defender = c.game.getDefenderIndex();
        engine.attack(c.game.getAttackerIndex(), c.attack);
        engine.defend(defender, c.defense, 0);
        engine.finishRound();
        assertEquals(6, c.game.getPlayers().get(defender).getCardCount());
        assertEquals(22, c.game.getStockSize());
    }

    @Test void eventLogContainsRoundEnd() {
        GameState game = game();
        GameEngine engine = start(game);
        engine.take(game.getDefenderIndex());
        assertEquals(GameEvent.Type.PICKUP, engine.finishRound().type());
        assertEquals(List.of(GameEvent.Type.ATTACK, GameEvent.Type.TAKE, GameEvent.Type.PICKUP),
                engine.getEvents().stream().map(GameEvent::type).toList());
    }

    @Test void cardsStayUniqueAfterSeveralRounds() {
        GameState game = new GameState(List.of("А", "Б", "В"), 42);
        GameEngine engine = new GameEngine(game);
        for (int round = 0; round < 15 && !game.isFinished(); round++) {
            playSimpleRound(engine);
            assertCardsUnique(game);
        }
    }

    @Test void fullGameFinishesWithFixedSeeds() {
        for (long seed : new long[]{0, 1, 42, 99, 2026}) {
            GameState game = new GameState(List.of("А", "Б"), seed);
            GameEngine engine = new GameEngine(game);
            for (int round = 0; round < 1000 && !game.isFinished(); round++) {
                playSimpleRound(engine);
                assertCardsUnique(game);
            }
            assertTrue(game.isFinished(), "Партия не закончилась, seed=" + seed);
            assertEquals(0, game.getStockSize());
            assertTrue(game.getLoserIndex() >= -1 && game.getLoserIndex() < 2);
            assertEquals(GameEvent.Type.GAME_OVER, engine.getEvents().getLast().type());
        }
    }

    @Test void sameSeedProducesSameEntireGame() {
        GameEngine first = playFullGame(42);
        GameEngine second = playFullGame(42);
        assertEquals(first.getEvents(), second.getEvents());
        assertEquals(first.getState().getDiscardPile(), second.getState().getDiscardPile());
        assertEquals(first.getState().getLoserIndex(), second.getState().getLoserIndex());
    }

    private GameEngine playFullGame(long seed) {
        GameState game = new GameState(List.of("А", "Б"), seed);
        GameEngine engine = new GameEngine(game);
        for (int round = 0; round < 1000 && !game.isFinished(); round++) playSimpleRound(engine);
        assertTrue(game.isFinished());
        return engine;
    }

    private void playSimpleRound(GameEngine engine) {
        GameState game = engine.getState();
        int attacker = game.getAttackerIndex();
        int defender = game.getDefenderIndex();
        Card attack = game.getPlayers().get(attacker).getHand().getFirst();
        engine.attack(attacker, attack);
        Card defense = null;
        for (Card card : game.getPlayers().get(defender).getHand()) {
            if (card.beats(attack, game.getTrumpSuit())) {
                defense = card;
                break;
            }
        }
        if (defense == null) engine.take(defender);
        else engine.defend(defender, defense, 0);
        engine.finishRound();
    }

    private void assertCardsUnique(GameState game) {
        List<Card> all = new ArrayList<>(game.getStockCards());
        all.addAll(game.getDiscardPile());
        for (Player player : game.getPlayers()) all.addAll(player.getHand());
        for (TablePair pair : game.getTable()) {
            all.add(pair.attack());
            if (pair.defense() != null) all.add(pair.defense());
        }
        assertEquals(36, all.size());
        assertEquals(36, new HashSet<>(all).size());
    }
}
