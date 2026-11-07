# Upstream and modifications

This is an independent community project, not an official Morphe or Meizu release.

The release is built from [Morphe Patches v2.1.0](https://github.com/MorpheApp/morphe-patches/tree/8fa2c7a1a832498de8f179748998a3ca87e198eb), exact commit `8fa2c7a1a832498de8f179748998a3ca87e198eb`.
The upstream GPLv3 license and additional Section 7 NOTICE are preserved in this repository and inside the bundle. The pinned upstream repository supplies the corresponding original source; this repository supplies all modifications and reproducible preparation instructions.

Changes dated 2026-11-07:

- Add the independently selectable `Flyme status bar lyrics` patch, depending on the official `Third-party lyrics` patch in the same bundle.
- Add an opt-in setting and English, Simplified Chinese and Traditional Chinese labels.
- Forward the exact `LockScreenLyrics.getCurrentLine()` result to the existing media notification's ticker. No new lyric provider or parser.
- Clear owned ticker state on pause, stop, null playback state, track/session change, missing synced lyrics, disabled settings and released sessions.
- Preserve media actions, notification identity, channel, icon and artwork. Retry on the official ticker so fresh application notifications receive the flags even when the lyric text is unchanged.
- Change bundle identity to `Flyme Lyrics Patches (community)` and include license notices in the artifact.

Interface source: [Meizu documentation, section 7](https://open.flyme.cn/docs?id=239). Its public document endpoint is `https://apiopen.flyme.cn/api/web/v1/doc-wiki/detail?id=239`.
Flags are reflected once: `FLAG_ALWAYS_SHOW_TICKER`, `FLAG_ONLY_UPDATE_TICKER`; unsupported devices do nothing. Clearing sets `tickerText` to null and removes both flags. `FLAG_NO_CLEAR` is restored to its original state.

Morphe source discovery was checked against `morphe-manager`: repository-root `patches-bundle.json`, `download_url`, `version`, `description`, `created_at`, and the `https://morphe.software/add-source?github=OWNER/REPO` link format.
