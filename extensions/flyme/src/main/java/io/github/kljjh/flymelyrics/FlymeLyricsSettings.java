package io.github.kljjh.flymelyrics;

import app.morphe.extension.shared.settings.BooleanSetting;

/** Registers the Flyme status bar lyrics switch with Morphe's settings registry. */
public final class FlymeLyricsSettings {
    public static final String KEY = "morphe_music_lyrics_flyme_status_bar";
    public static final BooleanSetting ENABLED = new BooleanSetting(KEY, true, false);

    private FlymeLyricsSettings() { }

    public static void ensureLoaded() { }
}
