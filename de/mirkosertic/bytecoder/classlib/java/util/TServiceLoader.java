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

import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Spliterator;
import java.util.function.Consumer;

@SubstitutesInClass(completeReplace = true)
public class TServiceLoader<S> implements Iterable<S> {

    public static <S> ServiceLoader<S> load(final Class aClass) {
        return (ServiceLoader<S>) (Object) new TServiceLoader<>();
    }

    public static <S> ServiceLoader<S> load(final Class aClass, final ClassLoader aLoader) {
        return (ServiceLoader<S>) (Object) new TServiceLoader<>();
    }

    public static <S> ServiceLoader<S> loadInstalled(final Class<S> service) {
        return load(service, null);
    }

    @Override
    public Iterator<S> iterator() {
        return new Iterator<S>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public S next() {
                return null;
            }
        };
    }

    @Override
    public void forEach(final Consumer<? super S> action) {
    }

    @Override
    public Spliterator<S> spliterator() {
        return null;
    }
}
