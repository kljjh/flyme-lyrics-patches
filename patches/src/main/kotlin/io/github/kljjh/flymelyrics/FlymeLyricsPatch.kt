package io.github.kljjh.flymelyrics

import app.morphe.patcher.patch.*

/** Loads on its own; the official source is a checked prerequisite at patch time. */
@Suppress("unused")
val flymeLyricsPatch = patch(
    name = "Flyme status bar lyrics",
    description = "Requires Third-party lyrics from the official Morphe source, selected in the same patching run. Mirrors its lock-screen lyric text to Flyme."
) {
    compatibleWith(Compatibility(
        name = "YouTube Music",
        packageName = "com.google.android.apps.youtube.music",
        apkFileType = ApkFileType.APK_REQUIRED,
        targets = listOf("9.30.53", "9.20.53", "9.15.51").map { AppTarget(version = it, minSdk = 26) }
                + listOf("9.44.52", "9.43.51", "9.42.52").map { AppTarget(version = it, minSdk = 26, isExperimental = true) }
    ))
    extendWith("extensions/flyme.mpe")

    // All sources finish execute before any finalize runs. The official extension is
    // therefore available even though the add-on loads in a separate class loader.
    finalize {
        val official = classDefByOrNull(OfficialHooks.LOCKSCREEN)
            ?: throw PatchException(OfficialHooks.MISSING_OFFICIAL)
        var metadataHook = false
        var playbackHook = false
        classDefForEach { cls ->
            if (!cls.type.startsWith("Lapp/morphe/extension/") && !cls.type.startsWith("Lio/github/kljjh/flymelyrics/")) {
                val found = OfficialHooks.hostHooks(cls)
                metadataHook = metadataHook || found.first
                playbackHook = playbackHook || found.second
            }
        }
        OfficialHooks.requireSelection(metadataHook, playbackHook)
        if (classDefByOrNull(OfficialHooks.UTILS)?.methods?.none {
                it.name == "getContext" && it.parameterTypes.isEmpty() && it.returnType == "Landroid/content/Context;"
            } != false) throw PatchException("Unsupported official shared extension: Utils.getContext is missing.")
        // Validate the official API before editing. Unknown upstream layouts fail visibly.
        OfficialHooks.instrument(mutableClassDefBy(official))
    }
}
