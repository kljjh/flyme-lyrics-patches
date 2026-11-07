package app.morphe.patches.music.layout.flymelyrics

import app.morphe.patcher.patch.patch
import app.morphe.patches.music.layout.lyrics.lyricsPatch
import app.morphe.patches.music.misc.settings.PreferenceScreen
import app.morphe.patches.music.shared.Constants.COMPATIBILITY_YOUTUBE_MUSIC
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference

/** Independent community addition; not an official Morphe patch. */
@Suppress("unused")
val flymeLyricsPatch = patch(
    name = "Flyme status bar lyrics",
    description = "Adds a switch to mirror the official lock-screen lyrics into the Flyme status bar. Requires enabling third-party and lock-screen lyrics."
) {
    dependsOn(lyricsPatch)
    compatibleWith(COMPATIBILITY_YOUTUBE_MUSIC)

    execute {
        PreferenceScreen.LYRICS.addPreferences(
            SwitchPreference("morphe_music_lyrics_flyme_status_bar", summary = true)
        )
    }
}
