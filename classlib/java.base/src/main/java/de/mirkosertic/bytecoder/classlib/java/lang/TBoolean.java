/*
 * Copyright 2018 Mirko Sertic
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
package de.mirkosertic.bytecoder.classlib.java.lang;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

@SubstitutesInClass(completeReplace = true)
public class TBoolean {

    public static final Boolean TRUE = new Boolean(true);

    public static final Boolean FALSE = new Boolean(false);

    private boolean booleanValue;

    public TBoolean(boolean aBooleanValue) {
        booleanValue = aBooleanValue;
    }

    public boolean booleanValue() {
        return booleanValue;
    }

    public static Boolean valueOf(String aValue) {
        if (parseBoolean(aValue)) {
            return TRUE;
        }
        return FALSE;
    }

    public static Boolean valueOf(boolean aValue) {
        if (aValue) {
            return TRUE;
        }
        return FALSE;
    }

    public static boolean parseBoolean(String aValue) {
        if (aValue != null && aValue.equalsIgnoreCase("true")) {
            return true;
        }
        return false;
    }

    public static boolean getBoolean(String aPropertyName) {
        return false;
    }
}