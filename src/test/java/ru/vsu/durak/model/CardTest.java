package ru.vsu.durak.model;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CardTest {
    @Test
    void deckHas36Cards() {
        assertEquals(36, Deck.create().size());
    }

    @Test
    void cardsAreUnique() {
        assertEquals(36, new HashSet<>(Deck.create()).size());
    }

    @Test
    void strongerCardBeatsWeaker() {
        Card attack = new Card(Rank.SIX, Suit.SPADES);
        Card defense = new Card(Rank.SEVEN, Suit.SPADES);
        assertTrue(defense.beats(attack, Suit.HEARTS));
    }

    @Test
    void weakerCardCannotBeatStronger() {
        Card attack = new Card(Rank.ACE, Suit.SPADES);
        Card defense = new Card(Rank.SIX, Suit.SPADES);
        assertFalse(defense.beats(attack, Suit.HEARTS));
    }

    @Test
    void differentSuitCannotBeatWithoutTrump() {
        Card attack = new Card(Rank.SIX, Suit.SPADES);
        Card defense = new Card(Rank.ACE, Suit.CLUBS);
        assertFalse(defense.beats(attack, Suit.HEARTS));
    }

    @Test
    void trumpBeatsNonTrump() {
        Card attack = new Card(Rank.ACE, Suit.SPADES);
        Card defense = new Card(Rank.SIX, Suit.HEARTS);
        assertTrue(defense.beats(attack, Suit.HEARTS));
    }

    @Test
    void lowerTrumpCannotBeatHigherTrump() {
        Card attack = new Card(Rank.ACE, Suit.HEARTS);
        Card defense = new Card(Rank.SIX, Suit.HEARTS);
        assertFalse(defense.beats(attack, Suit.HEARTS));
    }

    @Test
    void sameSeedGivesSameShuffle() {
        List<Card> first = Deck.shuffle(new Random(42));
        List<Card> second = Deck.shuffle(new Random(42));
        assertEquals(first, second);
    }

    @Test
    void cardCannotContainNull() {
        assertThrows(NullPointerException.class, () -> new Card(null, Suit.SPADES));
        assertThrows(NullPointerException.class, () -> new Card(Rank.SIX, null));
    }
}
