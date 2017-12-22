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

    public void add(Expression aExpression) {
        expressions.add(aExpression);
    }

    public List<Expression> toList() {
        return new ArrayList<>(expressions);
    }

    public int size() {
        return expressions.size();
    }

    public Expression firstExpression() {
        return expressions.get(0);
    }

    public Expression lastExpression() {
        if (expressions.isEmpty()) {
            return null;
        }
        int theLastIndex = expressions.size() - 1;
        Expression theLast = expressions.get(theLastIndex);
        while(theLast instanceof CommentExpression) {
            theLastIndex--;
            if (theLastIndex < 0) {
                return null;
            }
            theLast = expressions.get(theLastIndex);
        }
        return theLast;
    }

    public void addBefore(Expression aNewExpression, Expression aTarget) {
        expressions.add(expressions.indexOf(aTarget), aNewExpression);
    }

    public void replace(Expression aExpressionToReplace, Expression aNewExpression) {
        int p = expressions.indexOf(aExpressionToReplace);
        expressions.remove(p);
        expressions.add(p, aNewExpression);
    }

    public void remove(Expression aExpression) {
        expressions.remove(aExpression);
    }

    public void addAll(ExpressionList aExpressionList) {
        expressions.addAll(aExpressionList.expressions);
    }
}
