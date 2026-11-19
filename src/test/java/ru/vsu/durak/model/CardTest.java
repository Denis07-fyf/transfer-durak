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
//В колоде 36 карт
    @Test
    void cardsAreUnique() {
        assertEquals(36, new HashSet<>(Deck.create()).size());
    }
//Нет одинаковых карт
    @Test
    void strongerCardBeatsWeaker() {
        Card attack = new Card(Rank.SIX, Suit.SPADES);
        Card defense = new Card(Rank.SEVEN, Suit.SPADES);
        assertTrue(defense.beats(attack, Suit.HEARTS));
    }
//Старшая карта бьёт младшую той же масти
    @Test
    void weakerCardCannotBeatStronger() {
        Card attack = new Card(Rank.ACE, Suit.SPADES);
        Card defense = new Card(Rank.SIX, Suit.SPADES);
        assertFalse(defense.beats(attack, Suit.HEARTS));
    }
//Младшая не бьёт старшую
    @Test
    void differentSuitCannotBeatWithoutTrump() {
        Card attack = new Card(Rank.SIX, Suit.SPADES);
        Card defense = new Card(Rank.ACE, Suit.CLUBS);
        assertFalse(defense.beats(attack, Suit.HEARTS));
    }
//Некозырная карта чужой масти не отбивает
    @Test
    void trumpBeatsNonTrump() {
        Card attack = new Card(Rank.ACE, Suit.SPADES);
        Card defense = new Card(Rank.SIX, Suit.HEARTS);
        assertTrue(defense.beats(attack, Suit.HEARTS));
    }
//Козырь бьёт некозырную карту
    @Test
    void lowerTrumpCannotBeatHigherTrump() {
        Card attack = new Card(Rank.ACE, Suit.HEARTS);
        Card defense = new Card(Rank.SIX, Suit.HEARTS);
        assertFalse(defense.beats(attack, Suit.HEARTS));
    }
//Младший козырь не бьёт старший
    @Test
    void sameSeedGivesSameShuffle() {
        List<Card> first = Deck.shuffle(new Random(42));
        List<Card> second = Deck.shuffle(new Random(42));
        assertEquals(first, second);
    }
//Одинаковый seed даёт одинаковую колоду
    @Test
    void cardCannotContainNull() {
        assertThrows(NullPointerException.class, () -> new Card(null, Suit.SPADES));
        assertThrows(NullPointerException.class, () -> new Card(Rank.SIX, null));
    }
}//Нельзя создать карту с null
