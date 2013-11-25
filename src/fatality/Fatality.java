package fatality;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.components.ApplicationComponent;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by kuba on 25.11.13.
 */
public class Fatality implements ApplicationComponent, AWTEventListener {

    private ActionManager actionManager = ActionManager.getInstance();

    private final Map<KeyStroke, AnAction> key2action = new HashMap<>();

    private int modifiers;

    public Fatality() {
    }

    @Override
    public void initComponent() {
        final ActionGroup mainMenu = (ActionGroup) actionManager.getActionOrStub(IdeActions.GROUP_MAIN_MENU);
        collectActions(key2action, mainMenu);
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
            if ((modifiers & InputEvent.SHIFT_DOWN_MASK) == 0) {
                modifiers += InputEvent.SHIFT_DOWN_MASK;
            }
        } else if (code == KeyEvent.VK_CONTROL) {
            if ((modifiers & InputEvent.CTRL_DOWN_MASK) == 0) {
                modifiers += InputEvent.CTRL_DOWN_MASK;
            }
        } else if (code == KeyEvent.VK_ALT || code == KeyEvent.VK_ALT_GRAPH) {
            if ((modifiers & InputEvent.ALT_DOWN_MASK) == 0) {
                modifiers += InputEvent.ALT_DOWN_MASK;
            }
        } else {
            KeyStroke keyStroke = KeyStroke.getKeyStroke(code, modifiers);
            modifiers = 0;
            AnAction anAction = key2action.get(keyStroke);
            if (anAction != null) {
                ActionManager.getInstance().tryToExecute(anAction, ((InputEvent) event), ((InputEvent) event).getComponent(), null, true);
            }
        }
    }

    private void collectActions(Map<KeyStroke, AnAction> result, ActionGroup group) {
        final AnAction[] actions = group.getChildren(null);
        for (AnAction action : actions) {
            if (action != null) {
                if (action instanceof ActionGroup) {
                    final ActionGroup actionGroup = (ActionGroup) action;
                    collectActions(result, actionGroup);
                } else {
                    for (Shortcut shortcut : action.getShortcutSet().getShortcuts()) {
                        if (shortcut instanceof KeyboardShortcut) {
                            KeyStroke firstKeyStroke = ((KeyboardShortcut) shortcut).getFirstKeyStroke();
                            if (firstKeyStroke != null) {
                                result.put(firstKeyStroke, action);
                            }
                            KeyStroke secondKeyStroke = ((KeyboardShortcut) shortcut).getSecondKeyStroke();
                            if (secondKeyStroke != null) {
                                result.put(secondKeyStroke, action);
                            }
                        }
                    }
                }
            }
        }
    }

}
