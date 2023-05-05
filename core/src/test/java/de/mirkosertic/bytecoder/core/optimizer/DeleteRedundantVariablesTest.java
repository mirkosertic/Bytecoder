package de.mirkosertic.bytecoder.core.optimizer;

import de.mirkosertic.bytecoder.core.Slf4JLogger;
import de.mirkosertic.bytecoder.core.ir.Copy;
import de.mirkosertic.bytecoder.core.ir.Graph;
import de.mirkosertic.bytecoder.core.ir.Region;
import de.mirkosertic.bytecoder.core.ir.ResolvedField;
import de.mirkosertic.bytecoder.core.ir.Return;
import de.mirkosertic.bytecoder.core.ir.SetClassField;
import de.mirkosertic.bytecoder.core.ir.StandardProjections;
import de.mirkosertic.bytecoder.core.ir.Variable;
import org.junit.Test;
import org.objectweb.asm.Type;

import java.io.FileOutputStream;
import java.io.IOException;

public class DeleteRedundantVariablesTest {

    @Test
    public void test() throws IOException {
        final Graph g = new Graph(new Slf4JLogger());
        final Region startRegion = (Region) g.register(new Region(Graph.START_REGION_NAME));

        final Variable v1 = g.newVariable(Type.INT_TYPE);
        final Copy copy1 = g.newCopy();
        copy1.addIncomingData(g.newInt(10));
        v1.addIncomingData(copy1);

        final Variable v2 = g.newVariable(Type.INT_TYPE);
        final Copy copy2 = g.newCopy();
        copy2.addIncomingData(v1);
        v2.addIncomingData(copy2);

        final Variable v3 = g.newVariable(Type.getType(Object.class));
        final Copy copy3 = g.newCopy();
        v3.addIncomingData(copy3);
        copy3.addIncomingData(g.newNew(Type.getType(Object.class)));

        final ResolvedField f = new ResolvedField(null, "x", Type.INT_TYPE, null, 0);
        final SetClassField setInstanceField = g.newSetClassField(f);
        setInstanceField.addIncomingData(v2);
        v3.addIncomingData(setInstanceField);

        final Return ret = g.newReturnNothing();

        startRegion.addControlFlowTo(StandardProjections.DEFAULT, copy1);
        copy1.addControlFlowTo(StandardProjections.DEFAULT, copy2);
        copy2.addControlFlowTo(StandardProjections.DEFAULT, copy3);
        copy3.addControlFlowTo(StandardProjections.DEFAULT, setInstanceField);
        setInstanceField.addControlFlowTo(StandardProjections.DEFAULT, ret);

        // TODO: Invoke optimizer

        try (final FileOutputStream fos = new FileOutputStream("test.dot")) {
            g.writeDebugTo(fos);
        }
    }
}
