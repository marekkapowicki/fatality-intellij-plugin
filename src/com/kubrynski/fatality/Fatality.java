package com.kubrynski.fatality;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.editor.impl.EditorComponentImpl;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by kuba on 25.11.13.
 */
public class Fatality implements ApplicationComponent, AWTEventListener, KeyListener {

    private ActionManager actionManager = ActionManager.getInstance();

    private final Map<KeyStroke, AnAction> key2action = new HashMap<KeyStroke, AnAction>();

    private final Set<Integer> processedComponents = new HashSet<Integer>();

    private int modifiers;
    private char lastActionChar;

    public Fatality() {
    }

    @Override
    public void initComponent() {
        final ActionGroup mainMenu = (ActionGroup) actionManager.getActionOrStub(IdeActions.GROUP_MAIN_MENU);
        collectActions(mainMenu);
        Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK);
    }

    @Override
    public void disposeComponent() {
        Toolkit.getDefaultToolkit().removeAWTEventListener(this);
    }

    @NotNull
    @Override
    public String getComponentName() {
        return "Fatality";
    }

    @Override
    public void eventDispatched(AWTEvent event) {
        KeyEvent keyEvent = (KeyEvent) event;
        int code = keyEvent.getKeyCode();

        if (code == KeyEvent.VK_UNDEFINED) {
            return;
        }

        if (code == KeyEvent.VK_SHIFT) {
            processModifier(InputEvent.SHIFT_DOWN_MASK);
        } else if (code == KeyEvent.VK_CONTROL) {
            processModifier(InputEvent.CTRL_DOWN_MASK);
        } else if (code == KeyEvent.VK_ALT || code == KeyEvent.VK_ALT_GRAPH) {
            processModifier(InputEvent.ALT_DOWN_MASK);
        } else {
            // if key is pressed with any modifier key then just clean modifiers and skip to normal action
            if (keyEvent.getModifiers() != 0) {
                clearState();
                return;
            }

            // register this class as first KeyListener in EditorComponent
            if (!processedComponents.contains(event.getSource().hashCode())) {
                registerKeyListenerInEditor(event);
                processedComponents.add(event.getSource().hashCode());
            }

            KeyStroke keyStroke = KeyStroke.getKeyStroke(code, modifiers);
            AnAction anAction = key2action.get(keyStroke);
            clearState();
            if (anAction != null) {
                ActionManager.getInstance().tryToExecute(anAction, keyEvent, keyEvent.getComponent(), null, true);
                lastActionChar = keyEvent.getKeyChar();
            }
        }
    }

    private void clearState() {
        modifiers = 0;
        lastActionChar = 0;
    }

    private void processModifier(int shiftDownMask) {
        if ((modifiers & shiftDownMask) == 0) {
            modifiers += shiftDownMask;
        }
    }

    private void registerKeyListenerInEditor(AWTEvent event) {
        if (event.getSource() instanceof EditorComponentImpl) {
            EditorComponentImpl eventSource = (EditorComponentImpl) event.getSource();
            KeyListener[] keyListeners = eventSource.getKeyListeners();

            for (KeyListener keyListener : keyListeners) {
                eventSource.removeKeyListener(keyListener);
            }

            eventSource.addKeyListener(this);

            for (KeyListener keyListener : keyListeners) {
                eventSource.addKeyListener(keyListener);
            }
        }
    }

    private void collectActions(ActionGroup group) {
        final AnAction[] actions = group.getChildren(null);
        for (AnAction action : actions) {
            if (action != null) {
                if (action instanceof ActionGroup) {
                    final ActionGroup actionGroup = (ActionGroup) action;
                    collectActions(actionGroup);
                } else {
                    for (Shortcut shortcut : action.getShortcutSet().getShortcuts()) {
                        if (shortcut instanceof KeyboardShortcut) {
                            addKeyStrokeToMap(action, ((KeyboardShortcut) shortcut).getFirstKeyStroke());
                            addKeyStrokeToMap(action, ((KeyboardShortcut) shortcut).getSecondKeyStroke());
                        }
                    }
                }
            }
        }
    }

    private void addKeyStrokeToMap(AnAction action, KeyStroke firstKeyStroke) {
        if (firstKeyStroke != null) {
            // we're adding only shortcuts with modifiers
            if (firstKeyStroke.getModifiers() != 0) {
                key2action.put(firstKeyStroke, action);
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // if this key was used to perform an action then just skip this event
        // otherwise it will be typed into editor
        if (e.getKeyChar() == lastActionChar) {
            e.consume();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        //do nothing
    }

    @Override
    public void keyReleased(KeyEvent e) {
        //do nothing
    }
}
