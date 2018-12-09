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
package de.mirkosertic.bytecoder.api.web;

import de.mirkosertic.bytecoder.api.OpaqueProperty;

public interface Document extends Node, ParentNode {

    @OpaqueProperty
    String characterSet();

    @OpaqueProperty
    String doctype();

    @OpaqueProperty
    Element documentElement();

    @OpaqueProperty
    String documentURI();

    @OpaqueProperty
    boolean hidden();

    @OpaqueProperty
    String lastStyleSheetSet();

    Attr createAttribute(String name);

    Comment createComment(String data);

    <T extends Element> T createElement(String tagName);

    <T extends Element> T  createElementNS(String tagName, String nameSpaceURI);

    <T extends Event> T createEvent(String type);

    TextNode createTextNode(String data);

    <T extends Element> T elementFromPoint(int a, int y);

    NodeList getElementsByClassName(String className);

    NodeList getElementsByTagName(String tagName);

    NodeList getElementsByTagNameNS(String tagName, String nameSpaceURI);

    void normalizeDocument();
}
