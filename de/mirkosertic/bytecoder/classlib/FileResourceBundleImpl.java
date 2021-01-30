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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

public class FileResourceBundleImpl extends ResourceBundle {

    public static final Map<String, FileResourceBundleImpl> CACHE = new HashMap<>();

    private final String baseName;
    private final Locale locale;
    private final Properties p;

    public FileResourceBundleImpl(final String aBaseName, final Locale aLocale, final File aFile) {
        baseName = aBaseName;
        locale = aLocale;
        p = new Properties();
        try {
            if (aFile.exists()) {
                p.load(new InputStreamReader(new FileInputStream(aFile), StandardCharsets.ISO_8859_1.newDecoder()));
            }
        } catch (final IOException e) {
            // Ignore
        }
    }

    @Override
    public void setParent(final ResourceBundle parent) {
        super.setParent(parent);
    }

    @Override
    public String getBaseBundleName() {
        return baseName;
    }

    @Override
    public Locale getLocale() {
        return locale;
    }

    @Override
    public boolean containsKey(final String key) {
        if (key == null) {
            throw new NullPointerException();
        }
        for (FileResourceBundleImpl rb = this; rb != null; rb = (FileResourceBundleImpl) rb.parent) {
            if (rb.handleKeySet().contains(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<String> keySet() {
        final Set<String> s = new HashSet<>(handleKeySet());
        if (parent != null) {
            s.addAll(parent.keySet());
        }
        return s;
    }

    @Override
    protected Set<String> handleKeySet() {
        final Set<String> s = new HashSet<>();
        for (final Object k : p.keySet()) {
            s.add((String) k);
        }
        return s;
    }

    @Override
    protected Object handleGetObject(final String key) {
        return p.get(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        return Collections.enumeration(keySet());
    }
}
