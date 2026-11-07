/*
 * Flyme Lyrics Patches community addition, 2026-11-07.
 * SPDX-License-Identifier: GPL-3.0-only
 * See LICENSE and NOTICE. This is not an official Morphe release.
 */
package app.morphe.extension.music.patches.lyrics.session;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.service.notification.StatusBarNotification;

import java.lang.reflect.Field;
import java.util.Objects;

import app.morphe.extension.music.settings.Settings;
import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.Utils;

/**
 * Reuses YT Music's media notification, including its channel, icon, artwork and controls.
 * Only this application's notifications are enumerated. No notification-listener service,
 * overlay permission, new channel, timer, network request or hard-coded OEM flag is needed.
 * Protocol: https://open.flyme.cn/docs?id=239, section 7.
 */
public final class FlymeStatusBarLyrics {
    private static final String OWNER = "community.flymelyrics.owned";
    private static final String ADDED_NO_CLEAR = "community.flymelyrics.added_no_clear";
    private static final int SHOW = readFlag("FLAG_ALWAYS_SHOW_TICKER");
    private static final int UPDATE = readFlag("FLAG_ONLY_UPDATE_TICKER");
    private static boolean loggedFailure;

    private FlymeStatusBarLyrics() { }

    private static int readFlag(String name) {
        try {
            Field field = Notification.class.getDeclaredField(name);
            field.setAccessible(true);
            return field.getInt(null);
        } catch (Exception | LinkageError unsupported) {
            return 0;
        }
    }

    /** Called on every official lyric tick, even if the text is unchanged. */
    public static void update(MediaSession session, String text, PlaybackState state) {
        boolean enabled = Settings.LYRICS_FLYME_STATUS_BAR.get()
                && Settings.LYRICS_ENABLED.get() && Settings.LYRICS_MEDIASESSION.get();
        boolean playing = state != null && state.getState() == PlaybackState.STATE_PLAYING;
        if (!enabled || !playing || text == null || text.trim().isEmpty() || session == null) {
            clear();
            return;
        }
        publish(session, text);
    }

    /** Clears only notifications marked by this bridge, never cancels a media notification. */
    public static void clear() {
        publish(null, null);
    }

    private static void publish(MediaSession session, String text) {
        if (SHOW <= 0 || UPDATE <= 0) return;
        try {
            Context context = Utils.getContext();
            if (context == null) return;
            NotificationManager manager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;
            for (StatusBarNotification entry : manager.getActiveNotifications()) {
                Notification original = entry.getNotification();
                if (original.extras == null) continue;
                boolean owned = original.extras.getBoolean(OWNER, false);
                MediaSession.Token token = original.extras.getParcelable(Notification.EXTRA_MEDIA_SESSION);
                boolean matching = session != null && session.getSessionToken().equals(token);
                boolean show = matching && text != null;
                if (!show && !owned) continue;

                int lyricFlags = SHOW | UPDATE | Notification.FLAG_NO_CLEAR;
                if (show && owned && Objects.equals(text, original.tickerText == null
                        ? null : original.tickerText.toString())
                        && (original.flags & lyricFlags) == lyricFlags) continue;

                Notification changed = original.clone();
                if (show) {
                    if (!owned) changed.extras.putBoolean(ADDED_NO_CLEAR,
                            (original.flags & Notification.FLAG_NO_CLEAR) == 0);
                    changed.extras.putBoolean(OWNER, true);
                    // Keep the exact formatted lock-screen text, including translations.
                    changed.tickerText = text;
                    changed.flags |= lyricFlags;
                } else {
                    changed.tickerText = null;
                    changed.flags &= ~(SHOW | UPDATE);
                    if (changed.extras.getBoolean(ADDED_NO_CLEAR, false)) {
                        changed.flags &= ~Notification.FLAG_NO_CLEAR;
                    }
                    changed.extras.remove(OWNER);
                    changed.extras.remove(ADDED_NO_CLEAR);
                }
                changed.flags |= Notification.FLAG_ONLY_ALERT_ONCE;
                manager.notify(entry.getTag(), entry.getId(), changed);
            }
        } catch (Exception | LinkageError error) {
            // A denied notification update must not stop playback or official lyric updates.
            // Retry on the next existing lyric tick, but do not flood logs every 300 ms.
            if (!loggedFailure) {
                loggedFailure = true;
                Logger.printException(() -> "Flyme status bar lyrics unavailable", error);
            }
        }
    }
}
