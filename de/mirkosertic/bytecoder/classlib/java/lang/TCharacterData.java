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
package de.mirkosertic.bytecoder.classlib.java.lang;

import de.mirkosertic.bytecoder.api.AnyTypeMatches;
import de.mirkosertic.bytecoder.api.SubstitutesInClass;

@SubstitutesInClass(completeReplace = false)
abstract class TCharacterData {

    private static Object defaultData;
    private static Object data00;
    private static Object data01;
    private static Object data02;
    private static Object data0E;
    private static Object dataPrivate;
    private static Object dataUndefined;

    static AnyTypeMatches of(final int ch) {
        if (ch >>> 8 == 0) {     // fast-path
            if (defaultData == null) {
                try {
                    defaultData = Class.forName("java.lang.CharacterDataLatin1").newInstance();
                } catch (final Exception e) {
                    throw new IllegalArgumentException("Not supported", e);
                }
            }
            return (AnyTypeMatches) defaultData;
        } else {
            switch (ch >>> 16) {  //plane 00-16
                case (0): {
                    if (data00 == null) {
                        try {
                            data00 = Class.forName("java.lang.CharacterData00").newInstance();
                        } catch (final Exception e) {
                            throw new IllegalArgumentException("Not supported", e);
                        }
                    }
                    return (AnyTypeMatches) data00;

                }
                case (1): {
                    if (data01 == null) {
                        try {
                            data01 = Class.forName("java.lang.CharacterData01").newInstance();
                        } catch (final Exception e) {
                            throw new IllegalArgumentException("Not supported", e);
                        }
                    }
                    return (AnyTypeMatches) data01;

                }
                case (2): {
                    if (data02 == null) {
                        try {
                            data02 = Class.forName("java.lang.CharacterData02").newInstance();
                        } catch (final Exception e) {
                            throw new IllegalArgumentException("Not supported", e);
                        }
                    }
                    return (AnyTypeMatches) data02;

                }
                case (14): {
                    if (data0E == null) {
                        try {
                            data0E = Class.forName("java.lang.CharacterData0E").newInstance();
                        } catch (final Exception e) {
                            throw new IllegalArgumentException("Not supported", e);
                        }
                    }
                    return (AnyTypeMatches) data0E;

                }
                case (15):   // Private Use
                case (16):   // Private Use
                {
                    if (dataPrivate == null) {
                        try {
                            dataPrivate = Class.forName("java.lang.CharacterDataPrivateUse").newInstance();
                        } catch (final Exception e) {
                            throw new IllegalArgumentException("Not supported", e);
                        }
                    }
                    return (AnyTypeMatches) dataPrivate;
                }
                default: {
                    if (dataUndefined == null) {
                        try {
                            dataUndefined = Class.forName("java.lang.CharacterDataUndefined").newInstance();
                        } catch (final Exception e) {
                            throw new IllegalArgumentException("Not supported", e);
                        }
                    }
                    return (AnyTypeMatches) dataUndefined;
                }
            }
        }
    }
}
