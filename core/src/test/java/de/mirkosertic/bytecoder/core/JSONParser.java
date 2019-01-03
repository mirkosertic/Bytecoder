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
package de.mirkosertic.bytecoder.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JSONParser {

    public interface TokenHandler<T> {

        TokenHandler handle(char aChar);

        TokenHandler resolveChild(TokenHandler aChild);

        T toData();
    }
    public class ListProducingTokenHandler implements TokenHandler<List<Map<String, Object>>> {

        private final MapProducingTokenHandler parent;
        private final List<Map<String, Object>> data;

        public ListProducingTokenHandler(MapProducingTokenHandler aParent) {
            data = new ArrayList<>();
            parent = aParent;
        }

        @Override
        public TokenHandler handle(char aChar) {
            switch (aChar) {
                case '{':
                    return new MapProducingTokenHandler(this);
                case ']':
                    return parent.resolveChild(this);
            }
            return this;
        }

        @Override
        public TokenHandler resolveChild(TokenHandler aChild) {
            data.add((Map<String, Object>) aChild.toData());
            return this;
        }

        @Override
        public List<Map<String, Object>> toData() {
            return data;
        }
    }

    public class MapProducingTokenHandler implements TokenHandler<Map<String, Object>> {

        private Map<String, Object> data;
        private StringBuilder currentToken;
        private String lastToken;
        private boolean inString;
        private boolean inValue;
        private boolean escapeNext;
        private final TokenHandler parent;

        public MapProducingTokenHandler() {
            this(null, null);
        }

        private MapProducingTokenHandler(TokenHandler aParent) {
            this(aParent, new HashMap<>());
        }

        private MapProducingTokenHandler(TokenHandler aParent, Map<String, Object> aData) {
            data = aData;
            currentToken = new StringBuilder();
            inString = false;
            inValue = false;
            parent = aParent;
        }

        @Override
        public TokenHandler<? extends Object> handle(char aChar) {
            if (inString) {
                if (escapeNext) {
                    escapeNext = false;
                    currentToken.append(aChar);
                    return this;
                }
                switch (aChar) {
                    case '\\':
                        escapeNext = true;
                        break;
                    case '\"':
                        inString = false;
                        if (inValue) {
                            data.put(lastToken, currentToken.toString());
                            inValue = false;
                        } else {
                            lastToken = currentToken.toString();
                        }
                        return this;
                    default:
                        currentToken.append(aChar);
                }
                return this;
            }
            switch (aChar) {
                case '\"':
                    inString = true;
                    currentToken = new StringBuilder();
                    return this;
                case '{':
                    if (data == null) {
                        data = new HashMap<>();
                    } else {
                        return new MapProducingTokenHandler(this);
                    }
                    break;
                case ':':
                    inValue = true;
                    break;
                case ',':
                    inValue = false;
                    inString = false;
                    currentToken = new StringBuilder();
                case '\n':
                    break;
                case '\r':
                    break;
                case '\t':
                    break;
                case ' ':
                    break;
                case '}':
                    if (parent != null) {
                        return parent.resolveChild(this);
                    }
                    break;
                case '[':
                    return new ListProducingTokenHandler(this);

                default:
                    currentToken.append(aChar);
            }
            return this;
        }

        @Override
        public Map<String, Object> toData() {
            return data;
        }

        @Override
        public TokenHandler resolveChild(TokenHandler aChild) {
            data.put(currentToken.toString(), aChild.toData());
            return this;
        }
    }

    public <T> T fromJSON(String aValue) {
        TokenHandler theHandler = new MapProducingTokenHandler();
        for (int i=0;i<aValue.length();i++) {
            theHandler = theHandler.handle(aValue.charAt(i));
        }
        return (T) theHandler.toData();
    }
}