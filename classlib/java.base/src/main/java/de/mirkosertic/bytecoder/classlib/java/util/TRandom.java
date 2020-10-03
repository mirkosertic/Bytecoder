/*
 * Copyright 2017 Mirko Sertic
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
package de.mirkosertic.bytecoder.classlib.java.util;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

@SubstitutesInClass(completeReplace = true)
public class TRandom {

    private long seed;

    public TRandom() {
    }

    public TRandom(final long seed) {
        this.seed = seed;
    }

    public float nextFloat() {
        return (float) Math.random();
    }

    public double nextDouble() {
        return (double) Math.random();
    }

    public int nextInt() {
        return (int) (Math.random() * 2_147_483_647);
    }

    public int nextInt(final int seed) {
        return (int) Math.random();
    }

    public long nextLong() {
        return (long) (Math.random() *  9_223_372_036_854_775_807L);
    }
}
