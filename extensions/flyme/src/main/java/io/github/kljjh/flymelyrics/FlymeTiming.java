/* SPDX-License-Identifier: GPL-3.0-only */
package io.github.kljjh.flymelyrics;

import android.content.Context;

public final class FlymeTiming {
    static final String KEY = "flyme_status_lyrics_offset_ms";
    static final int DEFAULT = -800, LIMIT = 2000, STEP = 100;
    static final int FOLLOW = Integer.MIN_VALUE;
    private static final ThreadLocal<Integer> reading = new ThreadLocal<>();

    static int setting(Context c) { return c.getSharedPreferences(FlymeController.FILE, 0).getInt(KEY, DEFAULT); }
    static void save(Context c, int value) {
        if (value != FOLLOW && (Math.abs((long)value) > LIMIT || value % STEP != 0)) throw new IllegalArgumentException("Invalid lyric offset");
        c.getSharedPreferences(FlymeController.FILE, 0).edit().putInt(KEY, value).apply();
        FlymeController.refresh();
    }
    static String line(Context c, String[] track, String title) {
        reading.set(setting(c));
        try { return FlymeSource.currentLine(track, title); }
        finally { reading.remove(); }
    }
    // Called only for the duration of the synchronous official line lookup.
    public static boolean isStatusRead() { return reading.get() != null; }
    public static int offset(int official) {
        Integer local = reading.get();
        return local == null || local == FOLLOW ? official : local;
    }
    private FlymeTiming() {}
}
