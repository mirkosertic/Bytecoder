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
package de.mirkosertic.bytecoder.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mirkosertic.bytecoder.ssa.DebugPosition;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

public class SourceMapWriter {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static class SourceMap {
        private int version;
        private String file;
        private String sourceRoot;
        private List<String> names;
        private List<String> sources;
        private String mappings;

        public int getVersion() {
            return version;
        }

        public String getFile() {
            return file;
        }

        public String getSourceRoot() {
            return sourceRoot;
        }

        public List<String> getNames() {
            return names;
        }

        public List<String> getSources() {
            return sources;
        }

        public String getMappings() {
            return mappings;
        }
    }

    private final List<String> sources;
    private final List<String> names;
    private final StringBuilder mappings;
    private int currentLine;

    private boolean firstSegment;

    private int fieldCurrentColumn;
    private int fieldSourceIndex;
    private int fieldOriginalLine;
    private int fieldOriginalColumn;
    private int fieldNamesIndex;

    public SourceMapWriter() {
        sources = new ArrayList<>();
        names = new ArrayList<>();
        mappings = new StringBuilder();
        currentLine = 1;

        fieldCurrentColumn = -1;
        fieldSourceIndex = -1;
        fieldOriginalLine = -1;
        fieldOriginalColumn = -1;
        fieldNamesIndex = -1;
        firstSegment = true;
    }

    private void seekToLine(final int aLineCounter) {
        while(currentLine < aLineCounter) {
            currentLine++;
            fieldCurrentColumn = -1;
            mappings.append(";");
        }
    }

    private void addSegment(final int[] aData) {
        if (firstSegment) {
            firstSegment = false;
        } else {
            mappings.append(",");
        }
        mappings.append(VLQ.encode(aData));
    }

    public void assignName(final int aLineCounter, final int aColumnCounter, final String aSymbol, final DebugPosition aPosition) {
        if (aPosition != null) {
            final String theFileName = aPosition.getFileName();
            if (!sources.contains(theFileName)) {
                sources.add(theFileName);
            }
            if (!names.contains(aSymbol)) {
                names.add(aSymbol);
            }

            seekToLine(aLineCounter);

            final int theSourcesIndex = sources.indexOf(theFileName);
            final int theNamesIndex = names.indexOf(aSymbol);

            final int theColumn = fieldCurrentColumn == -1 ? aColumnCounter : aColumnCounter - fieldCurrentColumn;
            final int theSources = fieldSourceIndex == -1 ? theSourcesIndex : theSourcesIndex - fieldSourceIndex;
            final int theOriginalLine = fieldOriginalLine == -1 ? aPosition.getLineNumber() : aPosition.getLineNumber() - fieldOriginalLine;
            final int theOriginalColumn = 0;
            final int theOriginalSymbol = fieldNamesIndex == -1 ? theNamesIndex : theNamesIndex - fieldNamesIndex;

            addSegment(new int[] {theColumn, theSources, theOriginalLine, theOriginalColumn, theOriginalSymbol});

            fieldCurrentColumn = aColumnCounter;
            fieldSourceIndex = theSourcesIndex;
            fieldOriginalLine = aPosition.getLineNumber();
            fieldOriginalColumn = 0;
            fieldNamesIndex = theNamesIndex;
        }
    }

    public void assignDebugPosition(final int aLineCounter, final int aColumnCounter, final DebugPosition aPosition) {
        if (aPosition != null) {
            final String theFileName = aPosition.getFileName();
            if (!sources.contains(theFileName)) {
                sources.add(theFileName);
            }

            seekToLine(aLineCounter);

            final int theSourcesIndex = sources.indexOf(theFileName);

            final int theColumn = fieldCurrentColumn == -1 ? aColumnCounter : aColumnCounter - fieldCurrentColumn;
            final int theSources = fieldSourceIndex == -1 ? theSourcesIndex : theSourcesIndex - fieldSourceIndex;
            final int theOriginalLine = fieldOriginalLine == -1 ? aPosition.getLineNumber() : aPosition.getLineNumber() - fieldOriginalLine;
            final int theOriginalColumn = 0;

            addSegment(new int[] {theColumn, theSources, theOriginalLine, theOriginalColumn});

            fieldCurrentColumn = aColumnCounter;
            fieldSourceIndex = theSourcesIndex;
            fieldOriginalLine = aPosition.getLineNumber();
            fieldOriginalColumn = 0;
        }
    }

    public String toSourceMap(final String aJSFileName) {
        final SourceMap theResult = new SourceMap();
        theResult.version = 3;
        theResult.file = aJSFileName;
        theResult.sourceRoot = "";
        theResult.sources = sources;
        theResult.names = names;
        theResult.mappings = mappings.toString();
        try {
            final StringWriter theOut = new StringWriter();
            MAPPER.writeValue(theOut, theResult);
            return theOut.toString();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}