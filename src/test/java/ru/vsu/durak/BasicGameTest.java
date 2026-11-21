package ru.vsu.durak;

import org.junit.jupiter.api.Test;
import ru.vsu.durak.model.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/** 20 основных тестов механики для второй аттестации. */
class BasicGameTest {
    private GameState state() { return new GameState(List.of("А", "Б"), 42); }
    private BotGame botGame(long seed) {
        return new BotGame(List.of("А", "Б"),
                List.of(new SimpleBot(), new TransferBot()), seed);
    }
    private Card firstAttack(GameState state) {
        return state.getPlayers().get(state.getAttackerIndex()).getHand().getFirst();
    }

    private GameState findTransferState() {
        for (int seed = 0; seed < 1000; seed++) {
            GameState state = new GameState(List.of("А", "Б"), seed);
            Player attacker = state.getPlayers().get(state.getAttackerIndex());
            Player defender = state.getPlayers().get(state.getDefenderIndex());
            for (Card card : attacker.getHand()) {
                if (defender.getHand().stream().anyMatch(other -> other.rank() == card.rank())) {
                    new GameEngine(state).attack(state.getAttackerIndex(), card);
                    return state;
                }
            }
        }
        throw new AssertionError("Не найдена раздача с переводом");
    }

    @Test void deckHas36Cards() { assertEquals(36, Deck.create().size()); }
    @Test void deckHasNoDuplicates() { assertEquals(36, new HashSet<>(Deck.create()).size()); }
    @Test void higherCardBeatsLowerSameSuit() {
        assertTrue(new Card(Rank.SEVEN, Suit.SPADES)
                .beats(new Card(Rank.SIX, Suit.SPADES), Suit.HEARTS));
    }
    @Test void lowerCardCannotBeatHigher() {
        assertFalse(new Card(Rank.SIX, Suit.SPADES)
                .beats(new Card(Rank.SEVEN, Suit.SPADES), Suit.HEARTS));
    }
    @Test void trumpBeatsOtherSuit() {
        assertTrue(new Card(Rank.SIX, Suit.HEARTS)
                .beats(new Card(Rank.ACE, Suit.SPADES), Suit.HEARTS));
    }
    @Test void seededShuffleIsRepeatable() {
        assertEquals(Deck.shuffle(new Random(42)), Deck.shuffle(new Random(42)));
    }
    @Test void twoPlayersGetSixCards() {
        GameState s = state();
        assertEquals(6, s.getPlayers().get(0).getCardCount());
        assertEquals(6, s.getPlayers().get(1).getCardCount());
    }
    @Test void duplicatePlayerNameIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new GameState(List.of("А", "А"), 42));
    }
    @Test void attackerCanPlayCard() {
        GameState s = state();
        GameEngine engine = new GameEngine(s);
        Card card = firstAttack(s);
        engine.attack(s.getAttackerIndex(), card);
        assertEquals(card, s.getTable().getFirst().attack());
    }
    @Test void defenderCannotStartAttack() {
        GameState s = state();
        assertThrows(IllegalArgumentException.class,
                () -> new GameEngine(s).attack(s.getDefenderIndex(),
                        s.getPlayers().get(s.getDefenderIndex()).getHand().getFirst()));
    }
    @Test void attackerCannotPlayCardHeDoesNotOwn() {
        GameState s = state();
        int attacker = s.getAttackerIndex();
        Card foreign = s.getPlayers().get(s.getDefenderIndex()).getHand().getFirst();
        assertThrows(IllegalArgumentException.class,
                () -> new GameEngine(s).attack(attacker, foreign));
        assertTrue(s.getTable().isEmpty());
    }
    @Test void cannotFinishUndefendedAttack() {
        GameState s = state();
        GameEngine engine = new GameEngine(s);
        engine.attack(s.getAttackerIndex(), firstAttack(s));
        assertThrows(IllegalArgumentException.class, engine::finishRound);
    }
    @Test void attackerCannotDefend() {
        GameState s = state();
        GameEngine engine = new GameEngine(s);
        int attacker = s.getAttackerIndex();
        engine.attack(attacker, firstAttack(s));
        assertThrows(IllegalArgumentException.class,
                () -> engine.defend(attacker, s.getPlayers().get(attacker).getHand().getFirst(), 0));
    }
    @Test void takingTransfersCardsToDefender() {
        GameState s = state();
        GameEngine engine = new GameEngine(s);
        Card card = firstAttack(s);
        int defender = s.getDefenderIndex();
        engine.attack(s.getAttackerIndex(), card);
        engine.take(defender);
        engine.finishRound();
        assertTrue(s.getPlayers().get(defender).getHand().contains(card));
    }
    @Test void finishingRoundIncreasesCounter() {
        GameState s = state();
        GameEngine engine = new GameEngine(s);
        engine.attack(s.getAttackerIndex(), firstAttack(s));
        engine.take(s.getDefenderIndex());
        engine.finishRound();
        assertEquals(2, s.getRoundNumber());
    }
    @Test void legalTransferChangesDefender() {
        GameState s = findTransferState();
        int originalAttacker = s.getAttackerIndex();
        int defender = s.getDefenderIndex();
        Rank rank = s.getTable().getFirst().attack().rank();
        Card sameRank = s.getPlayers().get(defender).getHand().stream()
                .filter(card -> card.rank() == rank).findFirst().orElseThrow();
        new GameEngine(s).transfer(defender, sameRank);
        assertEquals(originalAttacker, s.getDefenderIndex());
        assertEquals(2, s.getTable().size());
    }
    @Test void wrongRankTransferIsRejected() {
        GameState s = findTransferState();
        int defender = s.getDefenderIndex();
        Rank rank = s.getTable().getFirst().attack().rank();
        Card wrong = s.getPlayers().get(defender).getHand().stream()
                .filter(card -> card.rank() != rank).findFirst().orElseThrow();
        assertThrows(IllegalArgumentException.class,
                () -> new GameEngine(s).transfer(defender, wrong));
        assertEquals(1, s.getTable().size());
    }
    @Test void showingTrumpTransfersWithoutRemovingCard() {
        for (int seed = 0; seed < 10000; seed++) {
            GameState s = new GameState(List.of("А", "Б"), seed);
            int attacker = s.getAttackerIndex();
            int defender = s.getDefenderIndex();
            for (Card attack : s.getPlayers().get(attacker).getHand()) {
                Card shown = s.getPlayers().get(defender).getHand().stream()
                        .filter(card -> card.rank() == attack.rank()
                                && card.suit() == s.getTrumpSuit())
                        .findFirst().orElse(null);
                if (shown == null) continue;
                GameEngine engine = new GameEngine(s);
                engine.attack(attacker, attack);
                engine.showTrumpAndTransfer(defender, shown);
                assertTrue(s.getPlayers().get(defender).getHand().contains(shown));
                assertEquals(1, s.getTable().size());
                assertEquals(attacker, s.getDefenderIndex());
                return;
            }
        }
        fail("Не найдена подходящая раздача");
    }
    @Test void botGameFinishes() {
        BotGame game = botGame(42);
        game.play();
        assertTrue(game.getState().isFinished());
        GameState s = game.getState();
        List<Card> cards = new ArrayList<>(s.getStockCards());
        cards.addAll(s.getDiscardPile());
        for (Player player : s.getPlayers()) cards.addAll(player.getHand());
        assertEquals(36, cards.size());
        assertEquals(36, new HashSet<>(cards).size());
    }
    @Test void sameSeedReplaysEntireBotGame() {
        BotGame a = botGame(42);
        BotGame b = botGame(42);
        a.play(); b.play();
        assertEquals(a.getEvents(), b.getEvents());
        assertEquals(a.getState().getLoserIndex(), b.getState().getLoserIndex());
    }
}
