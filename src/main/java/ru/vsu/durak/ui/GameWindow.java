package ru.vsu.durak.ui;

import ru.vsu.durak.model.*;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/** Простой графический интерфейс. Правила игры остаются в GameEngine. */
public class GameWindow extends JFrame {
    private static final int HUMAN = 0;
    private static final int BOT = 1;
    private GameState state;
    private GameEngine engine;
    private final BotStrategy bot = new TransferBot();
    private final Board board = new Board();
    private Card selectedCard;
    private int selectedPair = -1;
    private String message = "Выбери карту внизу и действие";
    private long seed = 42;

    /** Создаёт окно и запускает первую партию. */
    public GameWindow() {
        super("Переводной дурак");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 700));
        setSize(1250, 860);
        setLocationRelativeTo(null);
        setContentPane(board);
        newGame();
    }

    private void newGame() {
        state = new GameState(List.of("Вы", "Бот"), seed);
        engine = new GameEngine(state);
        selectedCard = null;
        selectedPair = -1;
        message = "Новая партия! Начинает игрок с младшим козырем";
        botTurn();
        board.repaint();
    }

    // Бот выполняет действия, пока решение снова не потребуется человеку.
    private void botTurn() {
        for (int step = 0; step < 100; step++) {
            if (state.isFinished()) {
                int loser = state.getLoserIndex();
                message = loser == -1 ? "НИЧЬЯ!" :
                        loser == HUMAN ? "Вы проиграли. Попробуем ещё?" : "Победа! Бот остался с картами";
                break;
            }
            if (state.getTable().isEmpty()) {
                if (state.getAttackerIndex() == HUMAN) break;
                Card attack = bot.chooseAttack(state, BOT);
                if (attack == null) break;
                engine.attack(BOT, attack);
                message = "Бот атакует: " + attack;
                continue;
            }
            if (state.isTaking()) {
                // Если бот взял карты, человек может подкинуть или закончить раунд.
                if (state.getDefenderIndex() == BOT) break;
                Card extra = bot.chooseAttack(state, BOT);
                if (extra != null) engine.attack(BOT, extra);
                engine.finishRound();
                message = "Вы взяли карты; начинается новый раунд";
                continue;
            }
            if (state.getDefenderIndex() == HUMAN) {
                // Если бот атакует, он сам докидывает после удачного отбоя.
                if (!allDefended()) break;
                Card extra = bot.chooseAttack(state, BOT);
                if (extra != null) {
                    engine.attack(BOT, extra);
                    message = "Бот подкидывает: " + extra;
                    continue;
                }
                engine.finishRound();
                message = "Бито! Следующий раунд";
                continue;
            }
            Card transfer = bot.chooseTransfer(state, BOT);
            if (transfer != null) {
                engine.transfer(BOT, transfer);
                message = "Бот перевёл картой " + transfer + ". Теперь отбивайтесь вы";
                continue;
            }
            boolean took = false;
            for (int i = 0; i < state.getTable().size(); i++) {
                TablePair pair = state.getTable().get(i);
                if (pair.isDefended()) continue;
                Card defense = bot.chooseDefense(state, BOT, pair.attack());
                if (defense == null) {
                    engine.take(BOT);
                    message = "Бот берёт карты. Можете подкинуть или нажать «Завершить»";
                    took = true;
                    break;
                }
                engine.defend(BOT, defense, i);
                message = "Бот отбился картой " + defense;
            }
            if (took) break;
            // После защиты бот ждёт, будет ли человек подкидывать.
            break;
        }
        selectedPair = -1;
        board.repaint();
    }

    private boolean allDefended() {
        return !state.getTable().isEmpty()
                && state.getTable().stream().allMatch(TablePair::isDefended);
    }

    private void action(String name) {
        try {
            if (name.equals("Новая партия")) {
                String input = JOptionPane.showInputDialog(this,
                        "Начальное число (seed). Одинаковый seed — одинаковая раздача:", seed);
                if (input == null) return;
                seed = Long.parseLong(input.trim());
                newGame();
                return;
            }
            if (state.isFinished()) {
                message = "Партия окончена. Нажмите «Новая партия»";
                board.repaint();
                return;
            }
            switch (name) {
                case "Ход / подкинуть" -> {
                    requireCard();
                    engine.attack(HUMAN, selectedCard);
                    message = "Вы положили " + selectedCard;
                }
                case "Отбить" -> {
                    requireCard();
                    int pair = selectedPair;
                    if (pair < 0 || pair >= state.getTable().size()
                            || state.getTable().get(pair).isDefended()) {
                        pair = -1;
                        for (int i = 0; i < state.getTable().size(); i++) {
                            if (!state.getTable().get(i).isDefended()) {
                                pair = i;
                                break;
                            }
                        }
                    }
                    engine.defend(HUMAN, selectedCard, pair);
                    message = "Вы отбили картой " + selectedCard;
                }
                case "Перевести" -> {
                    requireCard();
                    engine.transfer(HUMAN, selectedCard);
                    message = "Вы перевели ход картой " + selectedCard;
                }
                case "Проездной" -> {
                    requireCard();
                    engine.showTrumpAndTransfer(HUMAN, selectedCard);
                    message = "Вы показали козырь " + selectedCard + " и перевели ход";
                }
                case "Взять" -> {
                    engine.take(HUMAN);
                    message = "Вы объявили взятие";
                }
                case "Бито / завершить" -> {
                    // Бито возможно только после защиты; после взятия ботом — завершение.
                    engine.finishRound();
                    message = "Раунд завершён";
                }
                default -> { return; }
            }
            selectedCard = null;
            selectedPair = -1;
            botTurn();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            message = exception.getMessage();
            board.repaint();
        }
    }

    private void requireCard() {
        if (selectedCard == null) throw new IllegalArgumentException("Сначала выберите карту внизу");
    }

    private static Color c(int rgb) { return new Color(rgb); }

    private final class Board extends JPanel {
        private static final int CARD_W = 73;
        private static final int CARD_H = 104;
        private final List<HotSpot> hotSpots = new ArrayList<>();
        private final Font normal = new Font("Segoe UI", Font.PLAIN, 15);
        private final Color cream = c(0xf9f3e5);
        private final Color gold = c(0xe8c987);

        Board() {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) {
                    for (int i = hotSpots.size() - 1; i >= 0; i--) {
                        HotSpot hot = hotSpots.get(i);
                        if (hot.rect.contains(e.getPoint())) {
                            hot.run.run();
                            repaint();
                            return;
                        }
                    }
                }
            });
        }

        private void text(Graphics2D g, String txt, int x, int y, int size, Color color, boolean strong) {
            g.setColor(color);
            g.setFont(new Font("Segoe UI", strong ? Font.BOLD : Font.PLAIN, size));
            g.drawString(txt, x, y);
        }

        private void centered(Graphics2D g, String txt, int middleX, int y, int size, Color color, boolean strong) {
            g.setFont(new Font("Segoe UI", strong ? Font.BOLD : Font.PLAIN, size));
            FontMetrics fm = g.getFontMetrics();
            text(g, txt, middleX - fm.stringWidth(txt) / 2, y, size, color, strong);
        }

        private void rounded(Graphics2D g, int x, int y, int w, int h, int arc, Color color) {
            g.setColor(color);
            g.fillRoundRect(x, y, w, h, arc, arc);
        }

        private void card(Graphics2D g, Card card, int x, int y, boolean selected, boolean back) {
            if (selected) y -= 15;
            g.setColor(new Color(0, 0, 0, 75));
            g.fillRoundRect(x + 3, y + 5, CARD_W, CARD_H, 13, 13);
            rounded(g, x, y, CARD_W, CARD_H, 13, back ? c(0x243e66) : cream);
            if (back) {
                g.setColor(c(0xa4b7cf));
                g.setStroke(new BasicStroke(1.7f));
                g.drawRoundRect(x + 6, y + 6, CARD_W - 12, CARD_H - 12, 9, 9);
                centered(g, "✦", x + CARD_W / 2, y + 61, 34, c(0xdce4ed), true);
            } else {
                boolean red = card.suit() == Suit.HEARTS || card.suit() == Suit.DIAMONDS;
                Color ink = red ? c(0xb82e40) : c(0x27303c);
                text(g, card.rank().label(), x + 9, y + 23, 20, ink, true);
                text(g, card.suit().symbol(), x + 11, y + 42, 17, ink, true);
                centered(g, card.suit().symbol(), x + CARD_W / 2, y + 81, 40, ink, false);
            }
            g.setStroke(new BasicStroke(1));
        }

        private void addHot(int x, int y, int w, int h, Runnable run) {
            hotSpots.add(new HotSpot(new Rectangle(x, y, w, h), run));
        }

        private void button(Graphics2D g, String label, int x, int y, int w, boolean accent, Runnable run) {
            rounded(g, x, y, w, 41, 13, accent ? gold : c(0x284d43));
            centered(g, label, x + w / 2, y + 26, 13, accent ? c(0x19352b) : cream, true);
            addHot(x, y, w, 41, run);
        }

        @Override protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            hotSpots.clear();
            g.setPaint(new GradientPaint(0, 0, c(0x102e29), w, h, c(0x091c19)));
            g.fillRect(0, 0, w, h);
            int sideW = 245;
            int fx = 21;
            int fy = 23;
            int fw = w - sideW - 55;
            int fh = h - 46;
            rounded(g, fx + 4, fy + 5, fw, fh, 35, new Color(0, 0, 0, 70));
            rounded(g, fx, fy, fw, fh, 35, c(0x164536));
            g.setColor(c(0x27604c));
            g.setStroke(new BasicStroke(2));
            g.drawRoundRect(fx + 15, fy + 15, fw - 30, fh - 30, 26, 26);
            g.setStroke(new BasicStroke(1));
            text(g, "ДУРАК", 52, 71, 25, cream, true);
            text(g, "ПЕРЕВОДНОЙ", 161, 70, 12, gold, true);
            text(g, "РАУНД " + state.getRoundNumber(), fw - 133, 70, 14, gold, true);

            int mid = fx + fw / 2;
            // Opponent
            rounded(g, mid - 72, 86, 144, 33, 17, c(0x123c31));
            centered(g, "БОТ  •  " + state.getPlayers().get(BOT).getCardCount() + " карт", mid, 108, 14, cream, true);
            int botCards = Math.min(state.getPlayers().get(BOT).getCardCount(), 11);
            int spacing = botCards > 8 ? 28 : 35;
            int start = mid - (CARD_W + (botCards - 1) * spacing) / 2;
            for (int i = 0; i < botCards; i++) card(g, null, start + i * spacing, 132, false, true);

            // Deck and trump
            int cx = 75;
            int cy = h / 2 - 54;
            if (state.getStockSize() > 0) {
                card(g, null, cx, cy, false, true);
                centered(g, "Колода: " + state.getStockSize(), cx + CARD_W / 2, cy + CARD_H + 24, 13, cream, true);
            } else {
                rounded(g, cx, cy, CARD_W, CARD_H, 13, c(0x205142));
                centered(g, "Пусто", cx + CARD_W / 2, cy + 60, 13, gold, false);
            }
            text(g, "КОЗЫРЬ", cx - 2, cy - 18, 12, gold, true);
            text(g, state.getTrumpSuit().symbol(), cx + 83, cy + 39, 35,
                    state.getTrumpSuit() == Suit.HEARTS || state.getTrumpSuit() == Suit.DIAMONDS ? c(0xf1a4a9) : cream, true);

            // Cards on table, all pairs can be selected as defense target.
            List<TablePair> table = state.getTable();
            if (table.isEmpty()) {
                centered(g, "СТОЛ СВОБОДЕН", mid + 28, h / 2, 18, c(0x9ec6b3), true);
                centered(g, "Первым ходит тот, у кого младший козырь", mid + 28, h / 2 + 30, 13, c(0x83ad9b), false);
            } else {
                centered(g, "НА СТОЛЕ", mid + 28, h / 2 - 83, 12, gold, true);
                int count = table.size();
                int space = count <= 4 ? 99 : 83;
                int tableStart = mid + 28 - (CARD_W + (count - 1) * space) / 2;
                for (int i = 0; i < count; i++) {
                    int x = tableStart + i * space;
                    int y = h / 2 - 46;
                    if (selectedPair == i) {
                        rounded(g, x - 7, y - 7, CARD_W + 27, CARD_H + 49, 16, new Color(245, 210, 132, 75));
                    }
                    TablePair pair = table.get(i);
                    card(g, pair.attack(), x, y, false, false);
                    if (pair.defense() != null) card(g, pair.defense(), x + 17, y + 34, false, false);
                    final int index = i;
                    addHot(x, y, CARD_W + 25, CARD_H + 44, () -> {
                        selectedPair = index;
                        message = "Выбрана атакующая карта №" + (index + 1);
                    });
                }
            }

            // Player hand
            int handY = h - 235;
            centered(g, "ВАШИ КАРТЫ  •  " + state.getPlayers().get(HUMAN).getCardCount(), mid, handY - 18, 14, cream, true);
            List<Card> hand = new ArrayList<>(state.getPlayers().get(HUMAN).getHand());
            hand.sort(CardOrder.byStrength(state.getTrumpSuit()));
            int n = hand.size();
            if (n > 0) {
                int step = Math.min(80, Math.max(27, (fw - 105 - CARD_W) / Math.max(1, n - 1)));
                int handStart = mid - (CARD_W + (n - 1) * step) / 2;
                for (int i = 0; i < hand.size(); i++) {
                    Card card = hand.get(i);
                    boolean chosen = card.equals(selectedCard);
                    int x = handStart + i * step;
                    card(g, card, x, handY, chosen, false);
                    addHot(x, handY - (chosen ? 15 : 0), step < CARD_W ? step : CARD_W,
                            CARD_H + (chosen ? 15 : 0), () -> {
                                selectedCard = card;
                                message = "Выбрана карта " + card;
                            });
                }
            }

            // Action bar
            int buttonX = 50;
            int buttonY = h - 91;
            int buttonW = (fw - 65) / 6;
            String[] actions = {"Ход / подкинуть", "Отбить", "Перевести", "Проездной", "Взять", "Бито / завершить"};
            for (int i = 0; i < actions.length; i++) {
                String action = actions[i];
                button(g, action, buttonX + i * buttonW, buttonY, buttonW - 6,
                        i == 0 || i == 5, () -> GameWindow.this.action(action));
            }

            // Right information panel
            int rightX = fw + 38;
            int rw = w - rightX - 20;
            rounded(g, rightX, 24, rw, h - 48, 26, c(0x102b26));
            text(g, "ПАРТИЯ", rightX + 20, 64, 21, cream, true);
            text(g, "Seed: " + seed, rightX + 20, 94, 14, gold, false);
            rounded(g, rightX + 14, 116, rw - 28, 94, 15, c(0x1b4036));
            String role = state.isFinished() ? "ИГРА ОКОНЧЕНА" :
                    state.getDefenderIndex() == HUMAN ? "ВЫ ЗАЩИЩАЕТЕСЬ" : "ВЫ АТАКУЕТЕ";
            text(g, role, rightX + 25, 143, 13, gold, true);
            text(g, "Атакует: " + state.getPlayers().get(state.getAttackerIndex()).getName(), rightX + 25, 168, 13, cream, false);
            text(g, "Защита: " + state.getPlayers().get(state.getDefenderIndex()).getName(), rightX + 25, 190, 13, cream, false);
            text(g, "ПОСЛЕДНИЕ ДЕЙСТВИЯ", rightX + 20, 248, 12, gold, true);
            List<GameEvent> events = engine.getEvents();
            int begin = Math.max(0, events.size() - Math.max(4, (h - 500) / 23));
            int row = 271;
            for (int i = begin; i < events.size(); i++) {
                GameEvent event = events.get(i);
                String actor = event.playerIndex() < 0 ? "Игра" :
                        state.getPlayers().get(event.playerIndex()).getName();
                String name = switch (event.type()) {
                    case ATTACK -> "ходит";
                    case DEFEND -> "отбивает";
                    case TRANSFER -> "переводит";
                    case TRUMP_SHOW_TRANSFER -> "показывает козырь";
                    case TAKE -> "берёт";
                    case PICKUP -> "взял карты";
                    case BITO -> "бито";
                    case GAME_OVER -> "конец партии";
                };
                String line = actor + ": " + name + (event.card() == null ? "" : " " + event.card());
                g.setFont(normal);
                if (g.getFontMetrics().stringWidth(line) > rw - 32) {
                    line = line.substring(0, Math.min(line.length(), 23)) + "…";
                }
                text(g, line, rightX + 18, row, 13, c(0xc1d9cc), false);
                row += 23;
            }
            rounded(g, rightX + 14, h - 195, rw - 28, 94, 15, c(0x20483d));
            text(g, "ПОДСКАЗКА", rightX + 26, h - 170, 12, gold, true);
            String[] words = message.split(" ");
            StringBuilder line = new StringBuilder();
            int yy = h - 149;
            for (String word : words) {
                if ((line.length() + word.length()) > 25) {
                    text(g, line.toString(), rightX + 24, yy, 12, cream, false);
                    yy += 18;
                    line.setLength(0);
                }
                if (!line.isEmpty()) line.append(" ");
                line.append(word);
            }
            if (!line.isEmpty()) text(g, line.toString(), rightX + 24, yy, 12, cream, false);
            button(g, "Новая партия", rightX + 14, h - 78, rw - 28, true, () -> GameWindow.this.action("Новая партия"));
            g.dispose();
        }
    }

    private record HotSpot(Rectangle rect, Runnable run) { }
}
