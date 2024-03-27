package de.mirkosertic.bytecoder.core.optimizer;

import de.mirkosertic.bytecoder.core.ir.ResolvedMethod;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class OptimizerLogging {

    final List<File> graphSteps = new ArrayList<>();
    final List<File> dominatorTreeSteps = new ArrayList<>();

    private final ResolvedMethod rm;

    public OptimizerLogging(final ResolvedMethod rm) {
        this.rm = rm;
        logStep(0);
    }

    public void logStep(final int step) {
/*        try {
            final String prefix = rm.owner.type.getClassName() + "_" + rm.methodNode.name.replace("<", "$").replace(">", "$") + "_";

            final File generatedFilesDir = new File(".");
            final String filenameDg = prefix + "optimization_debuggraph_" + step + ".dot";
            final File unoptdbf = new File(generatedFilesDir, filenameDg);
            try (final FileOutputStream fos = new FileOutputStream(unoptdbf)) {
                rm.methodBody.writeDebugTo(fos);
            }
            graphSteps.add(unoptdbf);

            final String filenameDt = prefix + "optimiztation__dominatortree_" + step + ".dot";
            final File unoptdtf = new File(generatedFilesDir, filenameDt);
            try (final FileOutputStream fos = new FileOutputStream(unoptdtf)) {
                final DominatorTree dt = new DominatorTree(rm.methodBody);
                dt.writeDebugTo(fos);
            }
            dominatorTreeSteps.add(unoptdtf);
        } catch (final IOException e) {
            throw new RuntimeException("Cannot write debug data", e);
        }*/
    }
    public void finished() {
        //if (!rm.owner.type.getClassName().contains("TailcallVarargs") && (!"eval".equals(rm.methodNode.name))) {
            graphSteps.stream().forEach(File::delete);
            dominatorTreeSteps.stream().forEach(File::delete);
        //}
    }
}
