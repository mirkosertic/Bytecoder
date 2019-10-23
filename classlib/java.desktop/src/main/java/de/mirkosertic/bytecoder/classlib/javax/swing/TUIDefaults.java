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
package de.mirkosertic.bytecoder.classlib.javax.swing;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.util.Hashtable;
import java.util.Locale;

@SubstitutesInClass(completeReplace = true)
public class TUIDefaults extends Hashtable<Object,Object> {

    public TUIDefaults() {
    }

    public TUIDefaults(final int aValue1, final float aValue2) {
    }

    public void putDefaults(final Object... values) {
    }

    public Object get(final Object aKey, final Locale aLocale) {
        return super.get(aKey);
    }

    public void setDefaultLocale(final Locale aLocale) {
    }

    public ComponentUI getUI(final JComponent aComponent) {
        return null;
    }

    public Color getColor(final Object aKey) {
        return null;
    }

    public Border getBorder(final Object aKey) {
        return null;
    }

    public Font getFont(final Object aKey) {
        return null;
    }

    public Insets getInsets(final Object aKey) {
        return null;
    }

    public String getString(final Object aKey) {
        return null;
    }

    public boolean getBoolean(final Object key) {
        final Object value = get(key);
        return (value instanceof Boolean) ? ((Boolean)value).booleanValue() : false;
    }

    public int getInt(final Object key) {
        final Object value = get(key);
        return (value instanceof Integer) ? ((Integer)value).intValue() : 0;
    }
}
