# Architecture and upstream evidence

This independent community add-on is not an official Morphe or Meizu product.
Version 2.0, dated 2026-11-07, replaces the 1.0 integrated-source build with a standalone patch and a small Android-only extension. No official patches, lyric implementation, or official extension DEX is redistributed inside the `.mpp`.

## Cross-source prerequisite

Morphe Manager revision `6bd844d99ef9a13fcae08b68742662ff5bd19f83` loads each bundle separately:

- `app/.../patcher/patch/PatchBundle.kt`: `loadPatchesFromDex(setOf(jarFile))` for source metadata.
- `patcher-core/.../host/HostMain.kt`: `loadPatchesFromJar(setOf(file))` for JVM patching.
- `patcher-core/.../PatchPipeline.kt`: selects patches independently per bundle before combining them.

Patcher 2.3.0 `dependsOn` takes actual `Patch` objects, not a source URL or a patch-name reference. Importing official patch classes as a compile-only dependency would therefore break the manager's isolated source loading. Bundling them would recreate the duplicate-official-patch problem.

The add-on instead loads independently, then in **finalize**, after every selected patch's **execute**, checks for the official lyrics extension classes AND the application call to `LyricsPanelInstaller.onTabSelected(ViewGroup)` inserted directly by official `lyricsPatch.execute`. It deliberately does not require media-session host calls at this point, because another official finalize block inserts those later. Presence of unused official runtime classes alone is not sufficient. A missing prerequisite throws an explicit PatchException instructing the user to select official Third-party lyrics. This is a checked cross-source prerequisite, not automatic dependency selection.

## Bytecode adapter

The adapter forwards official playback and metadata events to the Flyme controller, installs an Android switch next to the mini-player preference, and observes official settings changes. It does not intercept mini-player rendering. The default-on preference is stored separately and takes effect without restarting playback.

The independent 300 ms main-thread ticker follows the official `MiniplayerLyrics.update()` line-selection rules: clean the displayed artist/title with `MetadataCleaner`, require synced lyrics and a matching track, then use `LyricsManager.getCurrentLineText()` or the original title on a null result. The official third-party master switch remains authoritative. Compile-only declarations in `official-api/` bind to methods in the official DEX; no provider, network call or official class is packaged in the add-on.

Only during a synchronous Flyme lookup, the official selectionPosition function receives the Flyme offset (default −800 ms) in place of the mini-player offset. Other readers keep the official value, and Flyme reads skip updates to the shared artist-frame carousel. Global and per-track corrections remain part of the original playback position.

The app-scoped README link and manual repository URLs both resolve to the same main-branch JSON in Manager. The release workflow commits the versioned MPP and manifest together. Source downloads stay on the raw metadata host, with identical Release bytes for local import. This removes a possible cross-host failure without claiming to diagnose an individual phone. See [Manager docs](https://github.com/MorpheApp/morphe-manager/blob/main/docs/patch-sources.md) and the [Breal example](https://github.com/brealorg/breal-morphe-patches).

Tested official fixture: Morphe Patches v2.1.0, source commit `8fa2c7a1a832498de8f179748998a3ca87e198eb`.
Artifact SHA-256: `114eafa139ea3018f35af3498e8e67fe7850d4dd22a6065a02e891699dcc5e96`.

## Flyme protocol

[Meizu section 7](https://open.flyme.cn/docs?id=239): reflect `FLAG_ALWAYS_SHOW_TICKER` and `FLAG_ONLY_UPDATE_TICKER` once, set `tickerText`, add both flags and `FLAG_NO_CLEAR`, and reuse the notification ID. Clearing nulls the ticker and removes the two OEM flags. The bridge restores whether it added NO_CLEAR and never cancels the media notification.

The original 1.0 release and source remain available under its tag. The GPL license and original NOTICE are retained for the project lineage, official build tooling and attribution.
