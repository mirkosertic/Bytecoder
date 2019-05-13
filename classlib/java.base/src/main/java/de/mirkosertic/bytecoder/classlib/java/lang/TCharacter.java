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
public class TCharacter {

    public static final int MIN_RADIX = 2;
    public static final int MAX_RADIX = 36;

    public static int digit(final char ch, final int radix) {
        switch (ch) {
        case '0':
            return 0;
        case '1':
            return 1;
        case '2':
            return 2;
        case '3':
            return 3;
        case '4':
            return 4;
        case '5':
            return 5;
        case '6':
            return 6;
        case '7':
            return 7;
        case '8':
            return 8;
        case '9':
            return 9;
        case 'A':
        case 'a':
            return 10;
        case 'B':
        case 'b':
            return 11;
        case 'C':
        case 'c':
            return 12;
        case 'D':
        case 'd':
            return 13;
        case 'E':
        case 'e':
            return 14;
        case 'F':
        case 'f':
            return 15;
        default:
            throw new IllegalArgumentException("Not supported character : " + ch);
        }
    }

    public static boolean isDigit(final char aChar) {
        switch (aChar) {
        case '0':
            return true;
        case '1':
            return true;
        case '2':
            return true;
        case '3':
            return true;
        case '4':
            return true;
        case '5':
            return true;
        case '6':
            return true;
        case '7':
            return true;
        case '8':
            return true;
        case '9':
            return true;
        default:
            return false;
        }
    }

    public static boolean isLowerCase(final char aChar) {
        switch (aChar) {
        case 'A':
            return false;
        case 'B':
            return false;
        case 'C':
            return false;
        case 'D':
            return false;
        case 'E':
            return false;
        case 'F':
            return false;
        case 'G':
            return false;
        case 'H':
            return false;
        case 'I':
            return false;
        case 'J':
            return false;
        case 'K':
            return false;
        case 'L':
            return false;
        case 'M':
            return false;
        case 'N':
            return false;
        case 'O':
            return false;
        case 'P':
            return false;
        case 'Q':
            return false;
        case 'R':
            return false;
        case 'S':
            return false;
        case 'T':
            return false;
        case 'U':
            return false;
        case 'V':
            return false;
        case 'W':
            return false;
        case 'X':
            return false;
        case 'Y':
            return false;
        case 'Z':
            return false;
        default:
            return true;
        }
    }

    public static boolean isUpperCase(final char aChar) {
        switch (aChar) {
        case 'A':
            return true;
        case 'B':
            return true;
        case 'C':
            return true;
        case 'D':
            return true;
        case 'E':
            return true;
        case 'F':
            return true;
        case 'G':
            return true;
        case 'H':
            return true;
        case 'I':
            return true;
        case 'J':
            return true;
        case 'K':
            return true;
        case 'L':
            return true;
        case 'M':
            return true;
        case 'N':
            return true;
        case 'O':
            return true;
        case 'P':
            return true;
        case 'Q':
            return true;
        case 'R':
            return true;
        case 'S':
            return true;
        case 'T':
            return true;
        case 'U':
            return true;
        case 'V':
            return true;
        case 'W':
            return true;
        case 'X':
            return true;
        case 'Y':
            return true;
        case 'Z':
            return true;
        default:
            return false;
        }
    }

    public static char toLowerCase(final char aChar) {
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
