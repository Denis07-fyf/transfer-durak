package ru.vsu.durak.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BotGameTest {
    private BotGame newGame(long seed) {
        return new BotGame(List.of("Простой", "Переводящий"),
                List.of(new SimpleBot(), new TransferBot()), seed);
    }

    @Test void firstAttackIsChosenFromHand() {
        GameState state = newGame(42).getState();
        int attacker = state.getAttackerIndex();
        Card chosen = new SimpleBot().chooseAttack(state, attacker);
        assertTrue(state.getPlayers().get(attacker).getHand().contains(chosen));
    }

    @Test void defenderCannotStartAttack() {
        GameState state = newGame(42).getState();
        assertEquals(null, new SimpleBot().chooseAttack(state, state.getDefenderIndex()));
    }

    @Test void botReturnsNullWhenNoDefenseExists() {
        GameState state = newGame(42).getState();
        Card impossible = new Card(Rank.ACE, state.getTrumpSuit());
        assertEquals(null, new SimpleBot().chooseDefense(
                state, state.getDefenderIndex(), impossible));
    }

    @Test void selectedDefenseAlwaysBeatsAttack() {
        GameState state = newGame(42).getState();
        int defender = state.getDefenderIndex();
        for (Card attack : Deck.create()) {
            Card defense = new SimpleBot().chooseDefense(state, defender, attack);
            if (defense != null) {
                assertTrue(defense.beats(attack, state.getTrumpSuit()));
                assertTrue(state.getPlayers().get(defender).getHand().contains(defense));
            }
        }
    }

    @Test void cheapestNonTrumpComesFirst() {
        List<Card> cards = new ArrayList<>(List.of(
                new Card(Rank.ACE, Suit.HEARTS),
                new Card(Rank.SIX, Suit.HEARTS),
                new Card(Rank.SEVEN, Suit.SPADES)));
        cards.sort(CardOrder.byStrength(Suit.HEARTS));
        assertEquals(new Card(Rank.SEVEN, Suit.SPADES), cards.getFirst());
        assertEquals(new Card(Rank.SIX, Suit.HEARTS), cards.get(1));
    }

    @Test void basicBotNeverTransfers() {
        GameState state = newGame(42).getState();
        assertEquals(null, new SimpleBot().chooseTransfer(state, state.getDefenderIndex()));
    }

    @Test void transferBotUsesLegalSameRankCard() {
        for (int seed = 0; seed < 300; seed++) {
            GameState state = new GameState(List.of("А", "Б"), seed);
            GameEngine engine = new GameEngine(state);
            int attacker = state.getAttackerIndex();
            Card card = new SimpleBot().chooseAttack(state, attacker);
            engine.attack(attacker, card);
            int defender = state.getDefenderIndex();
            Card transfer = new TransferBot().chooseTransfer(state, defender);
            if (transfer != null) {
                assertEquals(card.rank(), transfer.rank());
                engine.transfer(defender, transfer);
                assertEquals(2, state.getTable().size());
                return;
            }
        }
        throw new AssertionError("Не найдена подходящая раздача для перевода");
    }

    @Test void cannotCreateGameWithDifferentNumberOfStrategies() {
        assertThrows(IllegalArgumentException.class,
                () -> new BotGame(List.of("А", "Б"), List.of(new SimpleBot()), 42));
    }

    @Test void cannotCreateGameWithNullStrategy() {
        assertThrows(IllegalArgumentException.class,
                () -> new BotGame(List.of("А", "Б"), java.util.Arrays.asList(new SimpleBot(), null), 42));
    }

    @Test void oneRoundChangesRoundNumber() {
        BotGame game = newGame(42);
        game.playRound();
        assertEquals(2, game.getState().getRoundNumber());
        assertTrue(game.getEvents().size() >= 3);
    }

    @Test void fullTwoPlayerGameFinishes() {
        BotGame game = newGame(42);
        game.play();
        assertTrue(game.getState().isFinished());
        assertEquals(GameEvent.Type.GAME_OVER, game.getEvents().getLast().type());
    }

    @Test void fullThreePlayerGameFinishes() {
        BotGame game = new BotGame(List.of("А", "Б", "В"),
                List.of(new SimpleBot(), new TransferBot(), new SimpleBot()), 21);
        game.play();
        assertTrue(game.getState().isFinished());
    }

    @Test void sameSeedProducesSameEventsAndResult() {
        BotGame a = newGame(100);
        BotGame b = newGame(100);
        a.play();
        b.play();
        assertEquals(a.getEvents(), b.getEvents());
        assertEquals(a.getState().getLoserIndex(), b.getState().getLoserIndex());
    }

    @Test void all36CardsPreservedAfterFullGame() {
        BotGame game = newGame(24);
        game.play();
        List<Card> cards = new ArrayList<>();
        for (Player p : game.getState().getPlayers()) cards.addAll(p.getHand());
        cards.addAll(game.getState().getStockCards());
        cards.addAll(game.getState().getDiscardPile());
        assertEquals(36, cards.size());
        assertEquals(36, new HashSet<>(cards).size());
    }

    @Test void cannotContinueFinishedGame() {
        BotGame game = newGame(42);
        game.play();
        assertThrows(IllegalStateException.class, game::playRound);
    }

    @Test void fullGameContainsARealTransfer() {
        boolean transferred = false;
        for (int seed = 0; seed < 50; seed++) {
            BotGame game = newGame(seed);
            game.play();
            if (game.getEvents().stream().anyMatch(e -> e.type() == GameEvent.Type.TRANSFER)) {
                transferred = true;
                break;
            }
        }
        assertTrue(transferred);
    }
}
