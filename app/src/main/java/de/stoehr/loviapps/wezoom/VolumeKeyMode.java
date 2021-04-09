package de.stoehr.loviapps.wezoom;

public enum VolumeKeyMode {
    NONE(1),
    ZOOM(2),
    COLOR_FILTER(3),
    EXPOSURE(4),
    THRESHOLD(5),
    FLASHLIGHT(6),
    PAUSE(7),
    TAKE_PICTURE(8),
    COMBO_COLOR_FILTER_AND_FLASHLIGHT(9),
    COMBO_COLOR_FILTER_AND_PAUSE(10),
    COMBO_FLASHLIGHT_AND_PAUSE(11),
    COMBO_FLASHLIGHT_AND_TAKE_PICTURE(12);
    
    public int value;

    private VolumeKeyMode(int i) {
        this.value = i;
    }
}
