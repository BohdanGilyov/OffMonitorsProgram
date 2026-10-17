
package absolutely.real;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

public class Main {

    private static GraphicsDevice[] monitors;
    private static JFrame[] blackScreens;

    private static boolean ctrlPressed;
    private static boolean hotkeyUsed;
    private static boolean bracketPressed;

    public static void main(String[] args) {
        monitors = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();

        blackScreens = new JFrame[monitors.length];

        System.out.println("====================================");
        System.out.println("       OFF MONITOR PROGRAM");
        System.out.println("====================================\n");

        System.out.println("Detected monitors: " + monitors.length);
        System.out.println();
        System.out.println("Keyboard shortcuts:");

        if (monitors.length <= 2) {
            System.out.println("    CTRL + [  - Toggle monitor 1");
            System.out.println("    CTRL + ]  - Toggle monitor 2");
        } else {
            System.out.println("    CTRL + [ + number - Toggle monitor");
            for (int i = 0; i < Math.min(monitors.length, 9); i++) {
                System.out.println("    CTRL + [ + " + (i + 1) + " - Toggle monitor " + (i + 1));
            }
        }
        System.out.println("    CTRL + \\  - Restore all monitors");
        System.out.println();
        registerHotkeys();
    }

    private static void toggleMonitor(int index) {
        if (index < 0 || index >= monitors.length) {
            System.out.println("Monitor " + (index + 1) + " does not exist");
            return;
        }
        if (blackScreens[index] == null) {
            blackenMonitor(index);
            System.out.println("Monitor " + (index + 1) + " blacked out");
        } else {
            restoreMonitor(index);
            System.out.println("Monitor " + (index + 1) + " restored");
        }
    }

    private static void blackenMonitor(int index) {
        if (blackScreens[index] != null) {
            return;
        }
        GraphicsDevice monitor = monitors[index];
        JFrame frame = new JFrame();

        frame.setUndecorated(true);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setBackground(Color.BLACK);
        frame.setFocusableWindowState(false);

        JPanel panel = new JPanel();
        panel.setBackground(Color.BLACK);
        frame.setContentPane(panel);

        blackScreens[index] = frame;
        monitor.setFullScreenWindow(frame);
        frame.setVisible(true);
    }

    private static void restoreMonitor(int index) {
        JFrame frame = blackScreens[index];
        if (frame == null) {
            return;
        }
        if (monitors[index].getFullScreenWindow() == frame) {
            monitors[index].setFullScreenWindow(null);
        }
        frame.dispose();
        blackScreens[index] = null;
    }

    private static void restoreAllMonitors() {
        for (int i = 0; i < monitors.length; i++) {
            restoreMonitor(i);
        }
        bracketPressed = false;
        hotkeyUsed = false;
        System.out.println("All monitors restored");
    }

    private static void registerHotkeys() {
        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);

        try {
            GlobalScreen.registerNativeHook();

            GlobalScreen.addNativeKeyListener(new NativeKeyListener() {

                @Override
                public void nativeKeyPressed(NativeKeyEvent e) {
                    int key = e.getKeyCode();

                    if (key == NativeKeyEvent.VC_CONTROL) {
                        ctrlPressed = true;
                        return;
                    }

                    if (!ctrlPressed || hotkeyUsed) {
                        return;
                    }

                    if (key == NativeKeyEvent.VC_BACK_SLASH) {
                        hotkeyUsed = true;
                        bracketPressed = false;
                        SwingUtilities.invokeLater(Main::restoreAllMonitors);
                        return;
                    }

                    if (monitors.length <= 2) {
                        if (key == NativeKeyEvent.VC_OPEN_BRACKET) {
                            hotkeyUsed = true;
                            SwingUtilities.invokeLater(
                                    () -> toggleMonitor(0)
                            );
                        } else if (key == NativeKeyEvent.VC_CLOSE_BRACKET) {
                            hotkeyUsed = true;
                            SwingUtilities.invokeLater(
                                    () -> toggleMonitor(1)
                            );
                        }
                        return;
                    }

                    if (key == NativeKeyEvent.VC_OPEN_BRACKET) {
                        bracketPressed = true;
                        return;
                    }

                    if (bracketPressed) {
                        int index = -1;

                        if (key >= NativeKeyEvent.VC_1
                                && key <= NativeKeyEvent.VC_9) {
                            index = key - NativeKeyEvent.VC_1;
                        }

                        if (index >= 0 && index < monitors.length) {
                            hotkeyUsed = true;
                            bracketPressed = false;

                            int monitorIndex = index;
                            SwingUtilities.invokeLater(
                                    () -> toggleMonitor(monitorIndex)
                            );
                        }
                    }
                }

                @Override
                public void nativeKeyReleased(NativeKeyEvent e) {
                    int key = e.getKeyCode();

                    if (key == NativeKeyEvent.VC_CONTROL) {
                        ctrlPressed = false;
                        bracketPressed = false;
                        hotkeyUsed = false;
                    }
                }

                @Override
                public void nativeKeyTyped(NativeKeyEvent e) {
                }
            });

            System.out.println("Shortcuts registered:");

        } catch (NativeHookException e) {
            System.err.println("Failed to register shortcuts");
            e.printStackTrace();
        }
    }
}