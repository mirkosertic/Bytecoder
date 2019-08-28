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
package de.mirkosertic.bytecoder.classlib.java.util;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Set;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

@SubstitutesInClass(completeReplace = true)
public class TEnumMap<K extends Enum<K>, V> extends AbstractMap<K, V> {

    private final Class<K> keyClass;
    private final K[] keys;
    private final Object[] values;

    public TEnumMap(final Class<K> keyClass) {
        this.keyClass = keyClass;
        this.keys = keyClass.getEnumConstants();
        this.values = new Object[keys.length];
    }

    @Override
    public V get(final Object aKey) {
        for (int i=0;i<keys.length;i++) {
            if (keys[i] == aKey) {
                return (V) values[i];
            }
        }
        throw new IllegalArgumentException();
    }

    @Override
    public V put(final K aKey, final V aValue) {
        for (int i=0;i<keys.length;i++) {
            if (keys[i] == aKey) {
                final V oldValue = (V) values[i];
                values[i] = aValue;
                return oldValue;
            }
        }
        throw new IllegalArgumentException();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        final Set<Entry<K, V>> entries = new HashSet<>();
        for (int i=0;i<keys.length;i++) {
            final int index = i;
            final K key = keys[i];
            final V v = (V) values[i];
            if (v != null) {
                entries.add(new Entry<K, V>() {
                    @Override
                    public K getKey() {
                        return key;
                    }

                    @Override
                    public V getValue() {
                        return v;
                    }

                    @Override
                    public V setValue(final V value) {
                        final V oldValue = (V) values[index];
                        values[index] = value;
                        return oldValue;
                    }
                });
            }
        }
        return entries;
    }
}
