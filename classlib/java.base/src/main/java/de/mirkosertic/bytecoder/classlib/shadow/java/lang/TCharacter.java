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
package de.mirkosertic.bytecoder.classlib.shadow.java.lang;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

@SubstitutesInClass(completeReplace = true)
public class TCharacter {

    public static char toLowerCase(char aChar) {
        switch (aChar) {
        case 'A':
            return 'a';
        case 'B':
            return 'b';
        case 'C':
            return 'c';
        case 'D':
            return 'd';
        case 'E':
            return 'e';
        case 'F':
            return 'f';
        case 'G':
            return 'g';
        case 'H':
            return 'h';
        case 'I':
            return 'i';
        case 'J':
            return 'j';
        case 'K':
            return 'k';
        case 'L':
            return 'l';
        case 'M':
            return 'm';
        case 'N':
            return 'n';
        case 'O':
            return 'o';
        case 'P':
            return 'p';
        case 'Q':
            return 'q';
        case 'R':
            return 'r';
        case 'S':
            return 's';
        case 'T':
            return 't';
        case 'U':
            return 'u';
        case 'V':
            return 'v';
        case 'W':
            return 'w';
        case 'X':
            return 'x';
        case 'Y':
            return 'y';
        case 'Z':
            return 'z';
        default:
            return aChar;
        }
    }
}
