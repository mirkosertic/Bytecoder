package de.mirkosertic.bytecoder.asm.ir;

import org.objectweb.asm.tree.AnnotationNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnotationUtils {

    public static boolean hasAnnotation(final String desc, final List<AnnotationNode> annotations) {
        if (annotations != null) {
            for (final AnnotationNode node : annotations) {
                if (desc.equals(node.desc)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Map<String, Object> parseAnnotation(final String desc, final List<AnnotationNode> annotations) {
        if (annotations != null) {
            for (final AnnotationNode node : annotations) {
                if (desc.equals(node.desc)) {
                    final HashMap<String, Object> result = new HashMap<>();
                    String lastKey = null;
                    if (node.values != null) {
                        for (int i = 0; i < node.values.size(); i++) {
                            final Object v = node.values.get(i);
                            if (i % 2 == 0) {
                                lastKey = (String) v;
                            } else {
                                result.put(lastKey, v);
                            }
                        }
                    }
                    return result;
                }
            }
        }
        return null;
    }
}
