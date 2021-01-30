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
package de.mirkosertic.bytecoder.ssa;

import java.util.ArrayList;
import java.util.List;

public class ExpressionList {

    private final List<Expression> expressions;

    public ExpressionList() {
        expressions = new ArrayList<>();
    }

    public void add(final Expression aExpression) {
        expressions.add(aExpression);
    }

    public List<Expression> toList() {
        return new ArrayList<>(expressions);
    }

    public int size() {
        return expressions.size();
    }

    public Expression lastExpression() {
        if (expressions.isEmpty()) {
            return null;
        }
        final int theLastIndex = expressions.size() - 1;
        return expressions.get(theLastIndex);
    }

    public void addBefore(final Expression aNewExpression, final Expression aTarget) {
        expressions.add(expressions.indexOf(aTarget), aNewExpression);
    }

    public void replace(final Expression aExpressionToReplace, final Expression aNewExpression) {
        final int p = expressions.indexOf(aExpressionToReplace);
        if (p>=0) {
            expressions.remove(p);
            expressions.add(p, aNewExpression);
        }
    }

    public void replace(final Expression aExpressionToReplace, final ExpressionList aList) {
        final int p = expressions.indexOf(aExpressionToReplace);
        if (p>=0) {
            expressions.remove(p);
            final List<Expression> theList = aList.toList();
            for (int i = theList.size() - 1; i >= 0; i--) {
                expressions.add(p, theList.get(i));
            }
        }
    }

    public void remove(final Expression aExpression) {
        expressions.remove(aExpression);
    }

    public Expression predecessorOf(final Expression aExpression) {
        final int p = expressions.indexOf(aExpression);
        if (p>0) {
            return expressions.get(p-1);
        }
        return null;
    }

    public boolean endWithNeverReturningExpression() {
        final Expression theLastExpression = lastExpression();
        return theLastExpression instanceof ReturnExpression ||
                theLastExpression instanceof ReturnValueExpression ||
                theLastExpression instanceof TableSwitchExpression ||
                theLastExpression instanceof LookupSwitchExpression ||
                theLastExpression instanceof ThrowExpression ||
                theLastExpression instanceof GotoExpression;
    }

    public boolean endsWithReturn() {
        final Expression theLastExpression = lastExpression();
        return theLastExpression instanceof ReturnExpression ||
                theLastExpression instanceof ReturnValueExpression;
    }

    public ExpressionList deepCopy() {
        final ExpressionList theList = new ExpressionList();
        for (final Expression theExpression : expressions) {
            if (theExpression instanceof ExpressionListContainer) {
                final ExpressionListContainer theContainer = (ExpressionListContainer) theExpression;
                theList.add(theContainer.deepCopy());
            } else {
                theList.add(theExpression);
            }
        }
        return theList;
    }
}
