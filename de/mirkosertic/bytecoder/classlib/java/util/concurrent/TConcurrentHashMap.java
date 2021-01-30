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
package de.mirkosertic.bytecoder.classlib.java.util.concurrent;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

@SubstitutesInClass(completeReplace = true)
public class TConcurrentHashMap<K,V> extends AbstractMap<K,V> implements ConcurrentMap<K,V>, Serializable {

    private final Map<K,V> delegate;

    public TConcurrentHashMap() {
        delegate = new HashMap<>();
    }

    public TConcurrentHashMap(final int initialCapacity) {
        delegate = new HashMap<>(initialCapacity);
    }

    public TConcurrentHashMap(final int initialCapacity, final float loadFactor, final int concurrencyLevel) {
        delegate = new HashMap<>(initialCapacity, loadFactor);
    }

    public TConcurrentHashMap(final Map<K,V> data) {
        delegate = new HashMap<>(data);
    }

    @Override
    public V putIfAbsent(final K key, final V value) {
        return delegate.putIfAbsent(key, value);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return delegate.entrySet();
    }

    @Override
    public boolean remove(final Object key, final Object value) {
        return delegate.remove(key, value);
    }

    @Override
    public V replace(final K key, final V value) {
        return delegate.replace(key, value);
    }

    @Override
    public boolean replace(final K key, final V oldValue, final V newValue) {
        return delegate.replace(key, oldValue, newValue);
    }

    @Override
    public V getOrDefault(final Object key, final V defaultValue) {
        return delegate.getOrDefault(key, defaultValue);
    }

    @Override
    public void forEach(final BiConsumer<? super K, ? super V> action) {
        delegate.forEach(action);
    }

    @Override
    public void replaceAll(final BiFunction<? super K, ? super V, ? extends V> function) {
        delegate.replaceAll(function);
    }

    @Override
    public V computeIfAbsent(final K key, final Function<? super K, ? extends V> mappingFunction) {
        return delegate.computeIfAbsent(key, mappingFunction);
    }

    @Override
    public V computeIfPresent(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return delegate.computeIfPresent(key, remappingFunction);
    }

    @Override
    public V compute(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        return delegate.compute(key, remappingFunction);
    }

    @Override
    public V merge(final K key, final V value, final BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return delegate.merge(key, value, remappingFunction);
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean containsValue(final Object value) {
        return delegate.containsValue(value);
    }

    @Override
    public boolean containsKey(final Object key) {
        return delegate.containsKey(key);
    }

    @Override
    public V get(final Object key) {
        return delegate.get(key);
    }

    @Override
    public V put(final K key, final V value) {
        return delegate.put(key, value);
    }

    @Override
    public V remove(final Object key) {
        return delegate.remove(key);
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
        delegate.putAll(m);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public ConcurrentHashMap.KeySetView keySet() {
        return (ConcurrentHashMap.KeySetView) (Object) new KeySetView<K>(delegate.keySet());
    }

    @Override
    public Collection<V> values() {
        return delegate.values();
    }

    @Override
    public boolean equals(final Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @SubstitutesInClass(completeReplace = true)
    public static class CollectionView<K> extends HashSet<K> {
    }

    @SubstitutesInClass(completeReplace = true)
    public static class KeySetView<K> extends HashSet<K> {

        KeySetView(final Set<K> aKeys) {
            super(aKeys);
        }
    }

    @SubstitutesInClass(completeReplace = true)
    public static class ValuesView<V> extends HashSet<V> {

        ValuesView(final Set<V> aKeys) {
            super(aKeys);
        }
    }

    @SubstitutesInClass(completeReplace = true)
    public static class EntrySetView<K, V> extends HashSet<Map.Entry<K, V>> {

        EntrySetView(final Set<Entry<K, V>> aKeys) {
            super(aKeys);
        }
    }

    public boolean contains(final Object o) {
        return containsKey(o) || containsValue(o);
    }

    public long mappingCount() {
        return size();
    }
}
