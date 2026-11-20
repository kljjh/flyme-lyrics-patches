/* SPDX-License-Identifier: GPL-3.0-only */
package io.github.kljjh.flymelyrics;

import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;


@SuppressWarnings("deprecation")
public final class FlymePreference {
    public static void install(PreferenceFragment fragment) {
        PreferenceScreen root = fragment.getPreferenceScreen();
        if (root == null || root.findPreference(FlymeController.KEY) != null) return;
        PreferenceGroup parent = parentOf(root, root.findPreference("morphe_music_lyrics_miniplayer"));
        if (parent == null) return;
        Context c = parent.getContext();
        SwitchPreference toggle = new SwitchPreference(c);
        boolean zh = "zh".equals(c.getResources().getConfiguration().getLocales().get(0).getLanguage());
        toggle.setKey(FlymeController.KEY);
        toggle.setPersistent(false);
        toggle.setTitle(zh ? "魅族状态栏歌词" : "Flyme status bar lyrics");
        toggle.setSummary(zh ? "在状态栏显示当前歌词，可单独关闭迷你播放器歌词。" : "Show the current lyric in the status bar, independently of mini-player lyrics.");
        toggle.setChecked(FlymeController.enabled(c));
        toggle.setOnPreferenceChangeListener((pref, value) -> { FlymeController.setEnabled(c, Boolean.TRUE.equals(value)); return true; });
        parent.addPreference(toggle);
    }
    // Official unsorted categories can have a suffix in their key. Locate the actual parent.
    private static PreferenceGroup parentOf(PreferenceGroup group, Preference target) {
        if (target == null) return null;
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference item = group.getPreference(i);
            if (item == target) return group;
            if (item instanceof PreferenceGroup) {
                PreferenceGroup found = parentOf((PreferenceGroup) item, target);
                if (found != null) return found;
            }
        }
        return null;
    }
    private FlymePreference() { }
}
