package de.stoehr.loviapps.wezoom;

import java.util.HashMap;
import java.util.Map;

public enum AccessibilityMode {
    UNFILTERED(1),
    BLACK_ON_WHITE(2),
    WHITE_ON_BLACK(3),
    BLACK_ON_YELLOW(4),
    YELLOW_ON_BLACK(5),
    BLUE_ON_WHITE(6),
    WHITE_ON_BLUE(7),
    BLUE_ON_YELLOW(8),
    YELLOW_ON_BLUE(9),
    BLACK_ON_GREEN(10),
    GREEN_ON_BLACK(11);
    
    private static final Map<Integer, AccessibilityMode> lookup = new HashMap();
    public int value;

    static {
        AccessibilityMode[] values = values();
        for (AccessibilityMode accessibilityMode : values) {
            lookup.put(Integer.valueOf(accessibilityMode.value), accessibilityMode);
        }
    }

    private AccessibilityMode(int i) {
        this.value = i;
    }

    public static AccessibilityMode get(Integer num) {
        return lookup.get(num);
    }
}
