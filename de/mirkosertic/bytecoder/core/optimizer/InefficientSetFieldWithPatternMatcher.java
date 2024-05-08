package de.mirkosertic.bytecoder.core.optimizer;

import de.mirkosertic.bytecoder.core.backend.BackendType;
import de.mirkosertic.bytecoder.core.ir.Copy;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.ResolvedField;
import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;
import de.mirkosertic.bytecoder.core.ir.SetInstanceField;
import de.mirkosertic.bytecoder.core.ir.StandardProjections;
import de.mirkosertic.bytecoder.core.ir.Variable;
import de.mirkosertic.bytecoder.core.parser.CompileUnit;
import de.mirkosertic.bytecoder.core.patternmatcher.Match;
import de.mirkosertic.bytecoder.core.patternmatcher.PatternMatcher;
import org.objectweb.asm.Type;

public class InefficientSetFieldWithPatternMatcher implements Optimizer {

    @Override
    public boolean optimize(final BackendType backendType, final CompileUnit compileUnit, final ResolvedMethod method) {

        final Graph patternToSearch = new Graph(compileUnit.getLogger());
        final Variable source = patternToSearch.newVariable(Type.INT_TYPE);
        final Variable target = patternToSearch.newVariable(Type.INT_TYPE);
        final Copy cp1 = patternToSearch.newCopy();
        cp1.addIncomingData(source);
        final SetInstanceField sif = patternToSearch.newSetInstanceField(new ResolvedField(null, "field", null, null, 0));
        target.addIncomingData(cp1, sif);
        cp1.addControlFlowTo(StandardProjections.DEFAULT, sif);

        final PatternMatcher matcher = new PatternMatcher(compileUnit.getLogger(), sif);
        for (final Match match : matcher.findMatches(method.methodBody)) {
            final Variable sourceMatch = match.mappingFor(source);
            final Copy copyMatch = match.mappingFor(cp1);
            final SetInstanceField sifMatch = match.mappingFor(sif);
            final Variable targetMatch = match.mappingFor(target);

            sourceMatch.addIncomingData(sifMatch);
            copyMatch.deleteFromControlFlow();
            method.methodBody.deleteNode(targetMatch);

            return true;
        }

        return false;
    }
}
