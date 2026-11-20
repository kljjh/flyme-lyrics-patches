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
        Preference offset = new Preference(c);
        offset.setKey(FlymeTiming.KEY);
        offset.setPersistent(false);
        offset.setTitle(zh ? "状态栏歌词时间偏移" : "Status bar lyric timing");
        offset.setSummary(timingLabel(FlymeTiming.setting(c), zh));
        offset.setOnPreferenceClickListener(pref -> {
            int value = FlymeTiming.setting(c);
            android.widget.LinearLayout layout = new android.widget.LinearLayout(c);
            layout.setOrientation(android.widget.LinearLayout.VERTICAL);
            int pad = (int)(20 * c.getResources().getDisplayMetrics().density);
            layout.setPadding(pad, pad, pad, 0);
            android.widget.TextView label = new android.widget.TextView(c);
            android.widget.SeekBar slider = new android.widget.SeekBar(c);
            slider.setMax(2 * FlymeTiming.LIMIT / FlymeTiming.STEP);
            slider.setProgress(((value == FlymeTiming.FOLLOW ? FlymeTiming.DEFAULT : value) + FlymeTiming.LIMIT) / FlymeTiming.STEP);
            label.setText(timingLabel(slider.getProgress() * FlymeTiming.STEP - FlymeTiming.LIMIT, zh));
            slider.setContentDescription(offset.getTitle());
            slider.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(android.widget.SeekBar bar,int position,boolean fromUser) { label.setText(timingLabel(position * FlymeTiming.STEP - FlymeTiming.LIMIT, zh)); }
                public void onStartTrackingTouch(android.widget.SeekBar bar) {}
                public void onStopTrackingTouch(android.widget.SeekBar bar) {}
            });
            layout.addView(label); layout.addView(slider);
            new android.app.AlertDialog.Builder(c).setTitle(offset.getTitle()).setView(layout)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> saveTiming(offset, slider.getProgress() * FlymeTiming.STEP - FlymeTiming.LIMIT, zh))
                .setNeutralButton(zh ? "跟随官方" : "Use official timing", (dialog, which) -> saveTiming(offset, FlymeTiming.FOLLOW, zh)).show();
            return true;
        });
        parent.addPreference(offset);
    }
    private static void saveTiming(Preference pref,int ms,boolean zh) {
        FlymeTiming.save(pref.getContext(), ms);
        pref.setSummary(timingLabel(ms, zh));
    }
    private static String timingLabel(int ms, boolean zh) {
        if (ms == FlymeTiming.FOLLOW) return zh ? "跟随官方迷你播放器和媒体会话" : "Use mini-player and media session timing";
        if (ms == 0) return zh ? "0 ms（无偏移）" : "0 ms (no offset)";
        return (ms > 0 ? "+" : "") + ms + " ms" + (zh ? (ms < 0 ? "（提前）" : "（延后）") : (ms < 0 ? " (early)" : " (late)"));
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
