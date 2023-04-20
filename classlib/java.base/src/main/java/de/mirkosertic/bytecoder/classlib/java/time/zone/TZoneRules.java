package de.mirkosertic.bytecoder.classlib.java.time.zone;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.zone.ZoneRules;

@SubstitutesInClass(completeReplace = true)
public class TZoneRules {

    private final ZoneOffset zoneOffset;

    public static ZoneRules of(final ZoneOffset offset) {
        return (ZoneRules) (Object) new TZoneRules(offset);
    }

    public TZoneRules(final ZoneOffset zoneOffset) {
        this.zoneOffset = zoneOffset;
    }

    public ZoneOffset getOffset(final Instant instant) {
        return zoneOffset;
    }
}
