package io.github.kljjh.flymelyrics

import app.morphe.patcher.patch.*

/** Loads on its own; the official source is a checked prerequisite at patch time. */
@Suppress("unused")
val flymeLyricsPatch = patch(
    name = "Flyme status bar lyrics",
    description = "Requires Third-party lyrics from the official Morphe source and its mini-player lyrics setting. Mirrors the mini-player lyric text to Flyme."
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
        val miniplayer = classDefByOrNull(OfficialHooks.MINIPLAYER)
            ?: throw PatchException(OfficialHooks.MISSING_OFFICIAL)
        val panelInstaller = classDefByOrNull(OfficialHooks.PANEL_INSTALLER)
            ?: throw PatchException(OfficialHooks.MISSING_OFFICIAL)
        var selected = false
        classDefForEach { cls ->
            if (!cls.type.startsWith("Lapp/morphe/extension/") && !cls.type.startsWith("Lio/github/kljjh/flymelyrics/")) {
                selected = selected || OfficialHooks.hostHooks(cls)
            }
        }
        OfficialHooks.requireSelection(selected)
        if (classDefByOrNull(OfficialHooks.UTILS)?.methods?.none {
                it.name == "getContext" && it.parameterTypes.isEmpty() && it.returnType == "Landroid/content/Context;"
            } != false) throw PatchException("Unsupported official shared extension: Utils.getContext is missing.")
        // Validate the official API before editing. Unknown upstream layouts fail visibly.
        OfficialHooks.instrument(mutableClassDefBy(official))
        OfficialHooks.instrumentMiniplayer(mutableClassDefBy(miniplayer))
        OfficialHooks.instrumentSettings(mutableClassDefBy(panelInstaller))
    }
}
