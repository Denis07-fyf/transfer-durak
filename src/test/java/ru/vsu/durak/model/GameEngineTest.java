package ru.vsu.durak.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameEngineTest {
    private GameState game() {
        return new GameState(List.of("А", "Б"), 42);
    }

    private Card firstAttack(GameState game) {
        return game.getPlayers().get(game.getAttackerIndex()).getHand().getFirst();
    }

    @Test
    void firstAttackPutsCardOnTable() {
        GameState game = game();
        GameEngine engine = new GameEngine(game);
        Card card = firstAttack(game);
        engine.attack(game.getAttackerIndex(), card);
        assertEquals(new TablePair(card, null), game.getTable().getFirst());
    }

    @Test
    void attackRemovesCardFromHand() {
        GameState game = game();
        GameEngine engine = new GameEngine(game);
        int attacker = game.getAttackerIndex();
        Card card = firstAttack(game);
        engine.attack(attacker, card);
        assertEquals(5, game.getPlayers().get(attacker).getCardCount());
        assertFalse(game.getPlayers().get(attacker).getHand().contains(card));
    }

    @Test
    void attackReturnsEventAndAddsItToJournal() {
        GameState game = game();
        GameEngine engine = new GameEngine(game);
        int attacker = game.getAttackerIndex();
        Card card = firstAttack(game);
        GameEvent event = engine.attack(attacker, card);
        assertEquals(new GameEvent(GameEvent.Type.ATTACK, attacker, card, 0), event);
        assertEquals(List.of(event), engine.getEvents());
    }

    @Test
    void defenderCannotStartAttack() {
        GameState game = game();
        GameEngine engine = new GameEngine(game);
        int defender = game.getDefenderIndex();
        Card card = game.getPlayers().get(defender).getHand().getFirst();
        assertThrows(IllegalArgumentException.class, () -> engine.attack(defender, card));
        assertTrue(game.getTable().isEmpty());
        assertTrue(engine.getEvents().isEmpty());
    }

    @Test
    void attackWithForeignCardIsRejectedWithoutChanges() {
        GameState game = game();
        GameEngine engine = new GameEngine(game);
        int attacker = game.getAttackerIndex();
        Card foreign = game.getPlayers().get(game.getDefenderIndex()).getHand().getFirst();
        List<Card> before = game.getPlayers().get(attacker).getHand();
        assertThrows(IllegalArgumentException.class, () -> engine.attack(attacker, foreign));
        assertEquals(before, game.getPlayers().get(attacker).getHand());
        assertTrue(game.getTable().isEmpty());
    }

    @Test
    void attackerCannotThrowDifferentRank() {
        GameState game = game();
        GameEngine engine = new GameEngine(game);
        int attacker = game.getAttackerIndex();
        Card first = firstAttack(game);
        engine.attack(attacker, first);
        Card different = game.getPlayers().get(attacker).getHand().stream()
                .filter(card -> card.rank() != first.rank()).findFirst().orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> engine.attack(attacker, different));
        assertEquals(1, game.getTable().size());
        assertTrue(game.getPlayers().get(attacker).getHand().contains(different));
        assertEquals(1, engine.getEvents().size());
    }

    @Test
    void attackerCanThrowMatchingRank() {
        GameState game = null;
        Card first = null;
        Card second = null;
        for (int seed = 0; seed < 200; seed++) {
            GameState candidate = new GameState(List.of("А", "Б"), seed);
            List<Card> hand = candidate.getPlayers().get(candidate.getAttackerIndex()).getHand();
            for (Card a : hand) {
                for (Card b : hand) {
                    if (!a.equals(b) && a.rank() == b.rank()) {
                        game = candidate;
                        first = a;
                        second = b;
                        break;
                    }
                }
                if (first != null) break;
            }
            if (game != null) break;
        }
        assertNotNull(game);
        GameEngine engine = new GameEngine(game);
        int attacker = game.getAttackerIndex();
        engine.attack(attacker, first);
        engine.attack(attacker, second);
        assertEquals(2, game.getTable().size());
        assertEquals(4, game.getPlayers().get(attacker).getCardCount());
    }

    @Test
    void attackLimitIsSixWithFullDefenderHand() {
        GameState game = game();
        GameEngine engine = new GameEngine(game);
        assertEquals(0, game.getAttackLimit());
        engine.attack(game.getAttackerIndex(), firstAttack(game));
        assertEquals(6, game.getAttackLimit());
    }

    @Test
    void attackLimitDependsOnDefenderStartingHand() {
        GameState game = game();
        int defender = game.getDefenderIndex();
        List<Card> hand = new ArrayList<>(game.getPlayers().get(defender).getHand());
        for (int i = 0; i < 5; i++) game.getPlayers().get(defender).removeCard(hand.get(i));
        GameEngine engine = new GameEngine(game);
        int attacker = game.getAttackerIndex();
        Card first = firstAttack(game);
        engine.attack(attacker, first);
        assertEquals(1, game.getAttackLimit());
        Card anyOther = game.getPlayers().get(attacker).getHand().getFirst();
        assertThrows(IllegalArgumentException.class, () -> engine.attack(attacker, anyOther));
        assertEquals(1, game.getTable().size());
    }

    @Test
    void cannotDefendWithoutAttack() {
        GameState game = game();
        GameEngine engine = new GameEngine(game);
        Card card = game.getPlayers().get(game.getDefenderIndex()).getHand().getFirst();
        assertThrows(IllegalArgumentException.class,
                () -> engine.defend(game.getDefenderIndex(), card, 0));
    }

    @Test
    void attackerCannotDefend() {
        GameState game = game();
        GameEngine engine = new GameEngine(game);
        int attacker = game.getAttackerIndex();
        Card first = firstAttack(game);
        engine.attack(attacker, first);
        Card other = game.getPlayers().get(attacker).getHand().getFirst();
        assertThrows(IllegalArgumentException.class, () -> engine.defend(attacker, other, 0));
    }

    @Test
    void cannotDefendWithUnownedCard() {
        GameState game = game();
        GameEngine engine = new GameEngine(game);
        int attacker = game.getAttackerIndex();
        int defender = game.getDefenderIndex();
        Card attack = firstAttack(game);
        engine.attack(attacker, attack);
        Card foreign = game.getPlayers().get(attacker).getHand().getFirst();
        assertThrows(IllegalArgumentException.class, () -> engine.defend(defender, foreign, 0));
        assertFalse(game.getTable().getFirst().isDefended());
    }

    @Test
    void cannotDefendWithWeakCard() {
        GameState game = game();
        GameEngine engine = new GameEngine(game);
        int attacker = game.getAttackerIndex();
        int defender = game.getDefenderIndex();
        Card attack = firstAttack(game);
        engine.attack(attacker, attack);
        Card weak = game.getPlayers().get(defender).getHand().stream()
                .filter(card -> !card.beats(attack, game.getTrumpSuit()))
                .findFirst().orElseThrow();
        assertThrows(IllegalArgumentException.class, () -> engine.defend(defender, weak, 0));
        assertTrue(game.getPlayers().get(defender).getHand().contains(weak));
    }

    @Test
    void validDefenseChangesTableAndHand() {
        Setup setup = findValidDefense();
        GameState game = setup.game;
        GameEngine engine = new GameEngine(game);
        int defender = game.getDefenderIndex();
        engine.attack(game.getAttackerIndex(), setup.attack);
        GameEvent event = engine.defend(defender, setup.defense, 0);
        assertEquals(new TablePair(setup.attack, setup.defense), game.getTable().getFirst());
        assertEquals(GameEvent.Type.DEFEND, event.type());
        assertEquals(5, game.getPlayers().get(defender).getCardCount());
        assertEquals(2, engine.getEvents().size());
    }

    @Test
    void samePairCannotBeDefendedTwice() {
        Setup setup = findValidDefense();
        GameEngine engine = new GameEngine(setup.game);
        int defender = setup.game.getDefenderIndex();
        engine.attack(setup.game.getAttackerIndex(), setup.attack);
        engine.defend(defender, setup.defense, 0);
        assertThrows(IllegalArgumentException.class, () -> engine.defend(defender, setup.defense, 0));
        assertEquals(2, engine.getEvents().size());
    }

    @Test
    void incorrectPairIndexIsRejected() {
        GameState game = game();
        GameEngine engine = new GameEngine(game);
        engine.attack(game.getAttackerIndex(), firstAttack(game));
        Card defense = game.getPlayers().get(game.getDefenderIndex()).getHand().getFirst();
        assertThrows(IllegalArgumentException.class, () -> engine.defend(game.getDefenderIndex(), defense, -1));
        assertThrows(IllegalArgumentException.class, () -> engine.defend(game.getDefenderIndex(), defense, 1));
    }

    @Test
    void tableAndJournalCannotBeChangedOutsideEngine() {
        GameState game = game();
        GameEngine engine = new GameEngine(game);
        engine.attack(game.getAttackerIndex(), firstAttack(game));
        assertThrows(UnsupportedOperationException.class, () -> game.getTable().clear());
        assertThrows(UnsupportedOperationException.class, () -> engine.getEvents().clear());
    }

    @Test
    void wrongPlayerNumberThrowsException() {
        GameState game = game();
        GameEngine engine = new GameEngine(game);
        Card card = firstAttack(game);
        assertThrows(IllegalArgumentException.class, () -> engine.attack(-1, card));
        assertThrows(IllegalArgumentException.class, () -> engine.attack(2, card));
        assertTrue(engine.getEvents().isEmpty());
    }

    @Test
    void sameSeedAndSameMoveGiveSameResult() {
        GameState first = game();
        GameState second = game();
        GameEngine e1 = new GameEngine(first);
        GameEngine e2 = new GameEngine(second);
        e1.attack(first.getAttackerIndex(), firstAttack(first));
        e2.attack(second.getAttackerIndex(), firstAttack(second));
        assertEquals(first.getTable(), second.getTable());
        assertEquals(e1.getEvents(), e2.getEvents());
        assertEquals(first.getPlayers().get(0).getHand(), second.getPlayers().get(0).getHand());
    }

    private Setup findValidDefense() {
        for (int seed = 0; seed < 200; seed++) {
            GameState game = new GameState(List.of("А", "Б"), seed);
            List<Card> attackers = game.getPlayers().get(game.getAttackerIndex()).getHand();
            List<Card> defenders = game.getPlayers().get(game.getDefenderIndex()).getHand();
            for (Card attack : attackers) {
                for (Card defense : defenders) {
                    if (defense.beats(attack, game.getTrumpSuit())) {
                        return new Setup(game, attack, defense);
                    }
                }
            }
        }
        throw new AssertionError("Не нашлась раздача с допустимой защитой");
    }

    private record Setup(GameState game, Card attack, Card defense) { }
}
