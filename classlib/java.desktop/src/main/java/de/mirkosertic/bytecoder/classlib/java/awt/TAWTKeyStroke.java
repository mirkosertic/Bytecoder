/*
 * Copyright 2019 Mirko Sertic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mirkosertic.bytecoder.classlib.java.awt;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.awt.*;
import java.awt.event.KeyEvent;

@SubstitutesInClass(completeReplace = true)
public class TAWTKeyStroke {

    private char keyChar = KeyEvent.CHAR_UNDEFINED;
    private int keyCode = KeyEvent.VK_UNDEFINED;
    private final int modifiers;
    private final boolean onKeyRelease;

    public static AWTKeyStroke getAWTKeyStroke(final char keyChar) {
        return (AWTKeyStroke) (Object) new TAWTKeyStroke(keyChar, KeyEvent.VK_UNDEFINED, 0, false);
    }

    public static AWTKeyStroke getAWTKeyStroke(final Character keyChar, final int modifiers) {
        return (AWTKeyStroke) (Object) new TAWTKeyStroke(keyChar, KeyEvent.VK_UNDEFINED, modifiers, false);
    }

    public static AWTKeyStroke getAWTKeyStroke(final int keyCode, final int modifiers,
                                               final boolean onKeyRelease) {
        return (AWTKeyStroke) (Object) new TAWTKeyStroke(KeyEvent.CHAR_UNDEFINED, keyCode, modifiers, onKeyRelease);
    }

    public static AWTKeyStroke getAWTKeyStroke(final int keyCode, final int modifiers) {
        return (AWTKeyStroke) (Object) new TAWTKeyStroke(KeyEvent.CHAR_UNDEFINED, keyCode, modifiers, false);
    }

    public static AWTKeyStroke getAWTKeyStrokeForEvent(final KeyEvent anEvent) {
        final int id = anEvent.getID();
        switch(id) {
            case KeyEvent.KEY_PRESSED:
            case KeyEvent.KEY_RELEASED:
                return (AWTKeyStroke) (Object) new TAWTKeyStroke(KeyEvent.CHAR_UNDEFINED,
                        anEvent.getKeyCode(),
                        anEvent.getModifiers(),
                        (id == KeyEvent.KEY_RELEASED));
            case KeyEvent.KEY_TYPED:
                return (AWTKeyStroke) (Object) new TAWTKeyStroke(anEvent.getKeyChar(),
                        KeyEvent.VK_UNDEFINED,
                        anEvent.getModifiers(),
                        false);
            default:
                // Invalid ID for this KeyEvent
                return null;
        }
    }

    public static AWTKeyStroke getAWTKeyStroke(final String s) {
        return null;
    }

    protected TAWTKeyStroke(final char keyChar, final int keyCode, final int modifiers,
                            final boolean onKeyRelease) {
        this.keyChar = keyChar;
        this.keyCode = keyCode;
        this.modifiers = modifiers;
        this.onKeyRelease = onKeyRelease;
    }

    protected TAWTKeyStroke() {
        modifiers = 0;
        onKeyRelease = false;
    }

    public char getKeyChar() {
        return keyChar;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public int getModifiers() {
        return modifiers;
    }

    public boolean isOnKeyRelease() {
        return onKeyRelease;
    }
}
