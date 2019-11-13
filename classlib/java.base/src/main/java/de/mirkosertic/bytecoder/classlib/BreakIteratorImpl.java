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
package de.mirkosertic.bytecoder.classlib;

import java.text.BreakIterator;
import java.text.CharacterIterator;

public class BreakIteratorImpl extends BreakIterator {

    private CharacterIterator text;

    @Override
    public int first() {
        return BreakIterator.DONE;
    }

    @Override
    public int last() {
        return BreakIterator.DONE;
    }

    @Override
    public int next(int n) {
        return BreakIterator.DONE;
    }

    @Override
    public int next() {
        return BreakIterator.DONE;
    }

    @Override
    public int previous() {
        return BreakIterator.DONE;
    }

    @Override
    public int following(int offset) {
        return BreakIterator.DONE;
    }

    @Override
    public int current() {
        return BreakIterator.DONE;
    }

    @Override
    public CharacterIterator getText() {
        return text;
    }

    @Override
    public void setText(CharacterIterator newText) {
        text = newText;
    }
}
