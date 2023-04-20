package de.mirkosertic.bytecoder.classlib.java.time;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneRules;

@SubstitutesInClass(completeReplace = true)
public class TZoneId {

    private final String zoneId;
    private final ZoneOffset zoneOffset;

    private static native String defaultZoneId();

    public static ZoneId systemDefault() {
        return of(defaultZoneId());
    }

    public static native int zoneOffsetFor(final String zoneId);

    public static ZoneId of(final String zoneId) {
        return (ZoneId) (Object) new TZoneId(zoneId);
    }

    public TZoneId(final String zoneId) {
        this.zoneId = zoneId;
        this.zoneOffset = ZoneOffset.ofTotalSeconds(zoneOffsetFor(zoneId));
    }

    public ZoneRules getRules() {
        return ZoneRules.of(zoneOffset);
    }

    public String getId() {
        return zoneId;
    }
}
