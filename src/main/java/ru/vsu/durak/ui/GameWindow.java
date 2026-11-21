package ru.vsu.durak.ui;

import ru.vsu.durak.model.*;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
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
    private final List<FlyingCard> flying = new ArrayList<>();
    private final Timer frameTimer;
    private final Timer botTimer;
    private long dealStarted;
    private long celebrationStarted;
    private long pulseUntil;
    private boolean botThinking;
    private long selectedAt;


    /** Создаёт окно и запускает первую партию. */
    public GameWindow() {
        super("Переводной дурак");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(980, 700));
        setSize(1250, 860);
        setLocationRelativeTo(null);
        setContentPane(board);
        frameTimer = new Timer(16, e -> {
            long now = System.currentTimeMillis();
            flying.removeIf(item -> now - item.started >= item.duration);
            board.repaint();
            if (flying.isEmpty() && now > dealStarted + 1500
                    && now > pulseUntil && now > celebrationStarted + 2600) {
                ((Timer) e.getSource()).stop();
            }
        });
        botTimer = new Timer(520, e -> {
            try {
                boolean again = botStep();
                board.repaint();
                if (!again) {
                    ((Timer) e.getSource()).stop();
                    botThinking = false;
                }
            } catch (RuntimeException ex) {
                ((Timer) e.getSource()).stop();
                botThinking = false;
                message = "Ошибка хода: " + ex.getMessage();
                board.repaint();
            }
        });
        newGame();
    }

    private void newGame() {
        botTimer.stop();
        botThinking = false;
        flying.clear();
        state = new GameState(List.of("Вы", "Бот"), seed);
        engine = new GameEngine(state);
        selectedCard = null;
        selectedPair = -1;
        dealStarted = System.currentTimeMillis();
        celebrationStarted = 0;
        message = "Новая партия! Начинает игрок с младшим козырем";
        frameTimer.start();
        scheduleBotTurn();
        board.repaint();
    }

    // Бот делает один ход за один тик таймера: карту хорошо видно в полёте.
    private void scheduleBotTurn() {
        if (botThinking) return;
        botThinking = true;
        botTimer.restart();
    }

    private void moveCard(GameEvent event, Point from, boolean back) {
        if (event.card() == null) return;
        boolean defense = event.type() == GameEvent.Type.DEFEND;
        Point to = board.tablePosition(event.pairIndex(), defense);
        flying.add(new FlyingCard(event.card(), from, to, back, defense,
                System.currentTimeMillis(), 470));
        pulseUntil = System.currentTimeMillis() + 400;
        frameTimer.start();
    }

    private void completeRound(String messageText) {
        boolean taking = state.isTaking();
        int defender = state.getDefenderIndex();
        List<TablePair> table = new ArrayList<>(state.getTable());
        List<FlyingCard> roundCards = new ArrayList<>();
        Point destination = taking
                ? (defender == HUMAN ? board.humanReceivePosition() : board.botPosition())
                : board.discardPosition();
        for (int i = 0; i < table.size(); i++) {
            TablePair pair = table.get(i);
            roundCards.add(new FlyingCard(pair.attack(), board.tablePosition(i, false),
                    destination, false, false, System.currentTimeMillis() + i * 45L, 560));
            if (pair.defense() != null) {
                roundCards.add(new FlyingCard(pair.defense(), board.tablePosition(i, true),
                        destination, false, false, System.currentTimeMillis() + i * 45L, 560));
            }
        }
        engine.finishRound();
        flying.addAll(roundCards);
        message = messageText;
        pulseUntil = System.currentTimeMillis() + 600;
        if (state.isFinished() && state.getLoserIndex() == BOT) {
            celebrationStarted = System.currentTimeMillis();
        }
        frameTimer.start();
    }

    private boolean botStep() {
        if (state.isFinished()) {
            int loser = state.getLoserIndex();
            message = loser == -1 ? "НИЧЬЯ!" :
                    loser == HUMAN ? "Вы проиграли. Попробуем ещё?" : "Победа! Бот остался с картами";
            return false;
        }
        if (state.getTable().isEmpty()) {
            if (state.getAttackerIndex() == HUMAN) return false;
            Card attack = bot.chooseAttack(state, BOT);
            if (attack == null) return false;
            Point from = board.botPosition();
            moveCard(engine.attack(BOT, attack), from, true);
            message = "Бот атакует: " + attack;
            return true;
        }
        if (state.isTaking()) {
            // Если бот берёт карты, человек может подкинуть и нажать «Бито / завершить».
            if (state.getDefenderIndex() == BOT) return false;
            Card extra = bot.chooseAttack(state, BOT);
            if (extra != null) {
                Point from = board.botPosition();
                moveCard(engine.attack(BOT, extra), from, true);
                message = "Бот подкидывает: " + extra;
                return true;
            }
            completeRound("Вы взяли карты. Новый раунд");
            return true;
        }
        if (state.getDefenderIndex() == HUMAN) {
            if (!allDefended()) return false;
            Card extra = bot.chooseAttack(state, BOT);
            if (extra != null) {
                Point from = board.botPosition();
                moveCard(engine.attack(BOT, extra), from, true);
                message = "Бот подкидывает: " + extra;
                return true;
            }
            completeRound("Бито! Следующий раунд");
            return true;
        }
        Card transfer = bot.chooseTransfer(state, BOT);
        if (transfer != null) {
            Point from = board.botPosition();
            moveCard(engine.transfer(BOT, transfer), from, true);
            message = "Бот перевёл картой " + transfer + ". Отбивайтесь вы";
            return true;
        }
        for (int i = 0; i < state.getTable().size(); i++) {
            TablePair pair = state.getTable().get(i);
            if (pair.isDefended()) continue;
            Card defense = bot.chooseDefense(state, BOT, pair.attack());
            if (defense == null) {
                engine.take(BOT);
                message = "Бот берёт карты. Можете подкинуть или завершить раунд";
                return false;
            }
            Point from = board.botPosition();
            moveCard(engine.defend(BOT, defense, i), from, true);
            message = "Бот отбился картой " + defense;
            return true;
        }
        // Бот отбил все карты; теперь человек может подкинуть или закончить раунд.
        return false;
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
            if (botThinking) {
                message = "Бот делает ход — подождите";
                board.repaint();
                return;
            }
            switch (name) {
                case "Ход / подкинуть" -> {
                    requireCard();
                    Point from = board.handPosition(selectedCard);
                    moveCard(engine.attack(HUMAN, selectedCard), from, false);
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
                    Point from = board.handPosition(selectedCard);
                    moveCard(engine.defend(HUMAN, selectedCard, pair), from, false);
                    message = "Вы отбили картой " + selectedCard;
                }
                case "Перевести" -> {
                    requireCard();
                    Point from = board.handPosition(selectedCard);
                    moveCard(engine.transfer(HUMAN, selectedCard), from, false);
                    message = "Вы перевели ход картой " + selectedCard;
                }
                case "Взять" -> {
                    engine.take(HUMAN);
                    message = "Вы объявили взятие";
                }
                case "Бито / завершить" -> {
                    // Бито возможно только после защиты; после взятия ботом — завершение.
                    completeRound("Раунд завершён");
                }
                default -> { return; }
            }
            selectedCard = null;
            selectedPair = -1;
            scheduleBotTurn();
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
        private Point hovered = new Point(-100, -100);

        Point botPosition() {
            int fw = getWidth() - 245 - 55;
            return new Point(21 + fw / 2 - 35, 132);
        }

        Point humanReceivePosition() {
            int fw = getWidth() - 245 - 55;
            return new Point(21 + fw / 2 - 35, getHeight() - 235);
        }

        Point discardPosition() {
            return new Point(64, getHeight() / 2 - 65);
        }

        Point handPosition(Card wanted) {
            int fw = getWidth() - 245 - 55;
            int mid = 21 + fw / 2;
            List<Card> hand = new ArrayList<>(state.getPlayers().get(HUMAN).getHand());
            hand.sort(CardOrder.byStrength(state.getTrumpSuit()));
            int n = hand.size();
            int step = Math.min(80, Math.max(27, (fw - 105 - CARD_W) / Math.max(1, n - 1)));
            int start = mid - (CARD_W + (n - 1) * step) / 2;
            int index = hand.indexOf(wanted);
            return new Point(start + Math.max(0, index) * step, getHeight() - 235);
        }

        Point tablePosition(int index, boolean defense) {
            int fw = getWidth() - 245 - 55;
            int mid = 21 + fw / 2;
            int count = Math.max(1, state.getTable().size());
            int space = count <= 4 ? 99 : 83;
            int x = mid + 28 - (CARD_W + (count - 1) * space) / 2 + index * space;
            int y = getHeight() / 2 - 46;
            return new Point(x + (defense ? 17 : 0), y + (defense ? 34 : 0));
        }

        private boolean cardInFlight(Card card, boolean defense) {
            long now = System.currentTimeMillis();
            return flying.stream().anyMatch(f -> !f.back && f.defense == defense
                    && f.card.equals(card) && now < f.started + f.duration);
        }

        Board() {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    hovered = e.getPoint();
                    repaint();
                }
            });
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
            if (selected) y -= (int) Math.round(15 * ease(progress(
                    System.currentTimeMillis() - selectedAt, 230)));
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
            boolean hover = new Rectangle(x, y, w, 41).contains(hovered);
            rounded(g, x, y, w, 41, 13, accent ? gold : hover ? c(0x39735f) : c(0x284d43));
            if (hover) {
                g.setColor(new Color(255, 243, 207, 160));
                g.drawRoundRect(x + 1, y + 1, w - 2, 39, 12, 12);
            }
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
            for (int i = 0; i < botCards; i++) {
                float reveal = progress(System.currentTimeMillis() - dealStarted - i * 90L, 420);
                Graphics2D cg = (Graphics2D) g.create();
                cg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.05f, reveal)));
                card(cg, null, start + i * spacing, 132 - (int) Math.round((1 - ease(reveal)) * 55), false, true);
                cg.dispose();
            }

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
                    if (!cardInFlight(pair.attack(), false)) card(g, pair.attack(), x, y, false, false);
                    if (pair.defense() != null && !cardInFlight(pair.defense(), true)) {
                        card(g, pair.defense(), x + 17, y + 34, false, false);
                    }
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
                    float reveal = progress(System.currentTimeMillis() - dealStarted - i * 90L, 420);
                    Graphics2D cg = (Graphics2D) g.create();
                    cg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0.05f, reveal)));
                    card(cg, card, x, handY + (int) Math.round((1 - ease(reveal)) * 65), chosen, false);
                    cg.dispose();
                    addHot(x, handY - (chosen ? 15 : 0), step < CARD_W ? step : CARD_W,
                            CARD_H + (chosen ? 15 : 0), () -> {
                                selectedCard = card;
                                selectedAt = System.currentTimeMillis();
                                pulseUntil = selectedAt + 250;
                                frameTimer.start();
                                message = "Выбрана карта " + card;
                            });
                }
            }

            // Action bar
            int buttonX = 50;
            int buttonY = h - 91;
            int buttonW = (fw - 65) / 5;
            String[] actions = {"Ход / подкинуть", "Отбить", "Перевести", "Взять", "Бито / завершить"};
            for (int i = 0; i < actions.length; i++) {
                String action = actions[i];
                button(g, action, buttonX + i * buttonW, buttonY, buttonW - 6,
                        i == 0 || i == 4, () -> GameWindow.this.action(action));
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
            if (state.getRoundNumber() == 1) {
                text(g, "ПЕРВЫЙ КОН: БЕЗ ПЕРЕВОДА", rightX + 20, 227, 11, gold, true);
            }
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

            // Анимации поверх стола: плавный полёт карты и сияние при победе.
            long now = System.currentTimeMillis();
            if (now < pulseUntil) {
                float power = progress(pulseUntil - now, 600);
                g.setColor(new Color(235, 201, 135, (int) (50 * power)));
                g.setStroke(new BasicStroke(3));
                g.drawRoundRect(fx + 17, fy + 17, fw - 34, fh - 34, 25, 25);
                g.setStroke(new BasicStroke(1));
            }
            for (FlyingCard fly : flying) {
                if (now < fly.started) continue;
                float time = progress(now - fly.started, fly.duration);
                double eased = ease(time);
                int x = (int) Math.round(fly.from.x + (fly.to.x - fly.from.x) * eased);
                int y = (int) Math.round(fly.from.y + (fly.to.y - fly.from.y) * eased
                        - 36 * Math.sin(Math.PI * time));
                Graphics2D moving = (Graphics2D) g.create();
                moving.rotate(0.09 * Math.sin(Math.PI * time), x + CARD_W / 2.0, y + CARD_H / 2.0);
                card(moving, fly.card, x, y, false, fly.back);
                moving.dispose();
            }
            if (state.isFinished() && state.getLoserIndex() == BOT && now - celebrationStarted < 2400) {
                long elapsed = now - celebrationStarted;
                for (int i = 0; i < 65; i++) {
                    int x = 38 + (i * 103 + (int) (elapsed / 7)) % Math.max(1, fw - 50);
                    int y = 30 + (i * 73 + (int) (elapsed / 4)) % Math.max(1, h - 65);
                    g.setColor(i % 3 == 0 ? gold : i % 3 == 1 ? c(0xdde7da) : c(0x7ac0a2));
                    g.fillRoundRect(x, y, 5 + i % 4, 8 + i % 6, 3, 3);
                }
                centered(g, "ПОБЕДА!", mid, h / 2 - 110, 34, gold, true);
            }
            g.dispose();
        }
    }

    private static float progress(long elapsed, int duration) {
        return Math.max(0f, Math.min(1f, elapsed / (float) duration));
    }

    private static double ease(float t) {
        return 1 - Math.pow(1 - t, 3);
    }

    private record FlyingCard(Card card, Point from, Point to, boolean back,
                              boolean defense, long started, int duration) {
        boolean onTable() { return true; }
    }

    private record HotSpot(Rectangle rect, Runnable run) { }
}
