package de.mirkosertic.bytecoder.classlib;

import de.mirkosertic.bytecoder.api.ClassLibProvider;

public class DatatransferClassLibProvider extends ClassLibProvider {

    @Override
    public String getResourceBase() {
        return "META-INF/modules/java.datatransfer/classes";
    }
}
