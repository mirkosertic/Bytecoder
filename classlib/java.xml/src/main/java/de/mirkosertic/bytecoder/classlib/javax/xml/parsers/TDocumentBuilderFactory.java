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
package de.mirkosertic.bytecoder.classlib.javax.xml.parsers;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;

import com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

@SubstitutesInClass(completeReplace = true)
public abstract class TDocumentBuilderFactory {

    private static final DocumentBuilderFactoryImpl FACTORY = new DocumentBuilderFactoryImpl();

    private boolean validating;
    private boolean namespaceAware;
    private boolean whitespace;
    private boolean expandEntityRef;
    private boolean ignoreComments;
    private boolean coalescing;

    public TDocumentBuilderFactory() {
        validating = false;
        namespaceAware = false;
        whitespace = false;
        expandEntityRef = true;
        ignoreComments = false;
        coalescing = false;
    }

    public static DocumentBuilderFactory newInstance() throws ParserConfigurationException {
        return FACTORY;
    }

    public abstract DocumentBuilder newDocumentBuilder();

    public abstract void setAttribute(final String name, final Object value) throws IllegalArgumentException;

    public abstract Object getAttribute(final String name) throws IllegalArgumentException;

    public abstract void setFeature(final String name, final boolean value) throws ParserConfigurationException;

    public abstract boolean getFeature(final String name) throws ParserConfigurationException;

    public void setNamespaceAware(final boolean awareness) {
        this.namespaceAware = awareness;
    }

    public void setValidating(final boolean validating) {
        this.validating = validating;
    }

    public void setIgnoringElementContentWhitespace(final boolean whitespace) {
        this.whitespace = whitespace;
    }

    public void setExpandEntityReferences(final boolean expandEntityRef) {
        this.expandEntityRef = expandEntityRef;
    }

    public void setIgnoringComments(final boolean ignoreComments) {
        this.ignoreComments = ignoreComments;
    }

    public void setCoalescing(final boolean coalescing) {
        this.coalescing = coalescing;
    }

    public boolean isNamespaceAware() {
        return namespaceAware;
    }

    public boolean isValidating() {
        return validating;
    }

    public boolean isIgnoringElementContentWhitespace() {
        return whitespace;
    }

    public boolean isExpandEntityReferences() {
        return expandEntityRef;
    }

    public boolean isIgnoringComments() {
        return ignoreComments;
    }

    public boolean isCoalescing() {
        return coalescing;
    }

    public Schema getSchema() {
        throw new UnsupportedOperationException();
    }

    public void setSchema(final Schema schema) {
        throw new UnsupportedOperationException();
    }

    public void setXIncludeAware(final boolean state) {
        if (state) {
            throw new UnsupportedOperationException();
        }
    }

    public boolean isXIncludeAware() {
        throw new UnsupportedOperationException();
    }
}