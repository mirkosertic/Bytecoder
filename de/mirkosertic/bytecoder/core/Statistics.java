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
package de.mirkosertic.bytecoder.core;

import de.mirkosertic.bytecoder.api.Logger;

import java.util.HashMap;
import java.util.Map;

public class Statistics {

    public static class Counter {
        private int value;

        public void increment() {
            value++;
        }
    }

    public static class Context {
        private final Map<String, Counter> counter;

        Context() {
            counter = new HashMap<>();
        }

        public Counter counter(final String name) {
            return counter.computeIfAbsent(name, t -> new Counter());
        }

    }

    private final Map<String, Context> contexts;

    Statistics() {
        contexts = new HashMap<>();
    }

    public Context context(final String name) {
        return contexts.computeIfAbsent(name, t -> new Context());
    }

    public void writeTo(final Logger logger) {
        for (final Map.Entry<String, Context> ctx : contexts.entrySet()) {
            for (final Map.Entry<String, Counter> ctn : ctx.getValue().counter.entrySet()) {
                logger.info("[Statistics] {}::{} = {}", ctx.getKey(), ctn.getKey(), ctn.getValue().value);
            }
        }
    }
}
