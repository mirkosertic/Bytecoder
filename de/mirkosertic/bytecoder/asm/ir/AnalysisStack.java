/*
 * Copyright 2023 Mirko Sertic
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
package de.mirkosertic.bytecoder.asm.ir;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class AnalysisStack {

    public static class Action {

        public final String desctiption;

        public Action(final String desctiption) {
            this.desctiption = desctiption;
        }
    }

    private final List<Action> actions;

    private final List<String> debugMessages;

    public AnalysisStack() {
        actions = new ArrayList<>();
        debugMessages = new ArrayList<>();
    }

    AnalysisStack(final List<Action> actions) {
        this.actions = actions;
        this.debugMessages = new ArrayList<>();
    }

    public AnalysisStack addAction(final Action action) {
        final List<Action> newActions = new ArrayList<>(actions);
        newActions.add(action);
        return new AnalysisStack(newActions);
    }

    public void addDebugMessage(final String message) {
        debugMessages.add(message);
    }

    public void dumpAnalysisStack(final PrintStream out) {
        out.println("Current Analysis Stack is : ");
        for (int i = 0; i < actions.size(); i++) {
            for (int j = 0; j < i; j++) {
                out.print(" ");
            }
            out.println(actions.get(i).desctiption);
        }

        final StringBuilder pref = new StringBuilder();
        for (int j = 0; j < actions.size() + 1; j++) {
            pref.append(" ");
        }
        for (final String debug : debugMessages) {
            out.print(pref);
            out.println(debug);
        }
    }
}
