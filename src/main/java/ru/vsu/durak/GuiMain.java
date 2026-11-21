package ru.vsu.durak;

import ru.vsu.durak.ui.GameWindow;

import javax.swing.SwingUtilities;

/** Открывает графическую версию игры. */
public class GuiMain {
    /** Точка входа в графическую версию. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GameWindow().setVisible(true));
    }
}
