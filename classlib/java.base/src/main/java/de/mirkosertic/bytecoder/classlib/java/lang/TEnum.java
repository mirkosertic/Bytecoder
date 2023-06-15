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

import de.mirkosertic.bytecoder.api.NoExceptionCheck;
import de.mirkosertic.bytecoder.api.SubstitutesInClass;

@SubstitutesInClass(completeReplace = true)
public class TEnum implements Comparable<Enum> {

    private final String name;
    private final int ordinalNumber;
    public final Class getDeclaringClass() throws ClassNotFoundException {
        Class clazzObj = getClass();
        String clazzname = clazzObj.getName();

        if(clazzname.matches(".+\\$\\d+")){
            //its a subclassed Enum
            // enum E { VAL {private int i;} }
            // E.VAL.getClass() == E$1
            return Class.forName(clazzname.substring(0,clazzname.lastIndexOf('$')));
        }
        return clazzObj;
    }
    @NoExceptionCheck
    protected TEnum(final String aName, final int aOrdinalNumber) {
        name = aName;
        ordinalNumber = aOrdinalNumber;
    }

    public int ordinal() {
        return ordinalNumber;
    }

    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static Enum valueOf(final Class<Enum> aClass, final String aValue) {
        for (final Enum theEnum : aClass.getEnumConstants()) {
            if (theEnum.name().equals(aValue)) {
                return theEnum;
            }
        }
        throw new IllegalArgumentException();
    }

    @Override
    public int compareTo(final Enum o) {
        final int index = o.ordinal();
        if (index == ordinalNumber) {
            return 0;
        } else if (index < ordinalNumber) {
            return 1;
        } else {
            return -1;
        }
    }
}
