"""Apply a small, fail-closed overlay to an exact official source revision."""
import argparse
import json
from pathlib import Path
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def replace_once(path, old, new):
    text = path.read_text(encoding="utf-8")
    if text.count(old) != 1:
        raise RuntimeError(f"Upstream changed: expected one anchor in {path}: {old[:70]!r}")
    path.write_text(text.replace(old, new), encoding="utf-8", newline="\n")


def prepare(target):
    config = json.loads((ROOT / "upstream.json").read_text())
    revision = subprocess.check_output(["git", "-C", str(target), "rev-parse", "HEAD"], text=True).strip()
    if revision != config["commit"]:
        raise RuntimeError(f"Wrong upstream: {revision}; expected {config['commit']}")
    if subprocess.check_output(["git", "-C", str(target), "status", "--porcelain"], text=True).strip():
        raise RuntimeError("Use a clean upstream checkout; prepare is intentionally not destructive or repeatable")

    music = target / "extensions/music/src/main/java/app/morphe/extension/music"
    session = music / "patches/lyrics/session/LockScreenLyrics.java"
    replace_once(session, "            if (session == null || original == null) {\n                return;", """            if (session == null || original == null) {
                FlymeStatusBarLyrics.clear();
                sessionRef = null;
                originalMetadata = null;
                ticker.stop();
                return;""")
    replace_once(session, "            sessionRef = new WeakReference<>(session);", """            MediaSession previous = mediaSession();
            if (previous != null && !previous.getSessionToken().equals(session.getSessionToken())) {
                FlymeStatusBarLyrics.clear();
            }
            sessionRef = new WeakReference<>(session);""")
    replace_once(session, "                LyricsPresentation.resetTrackCycle();", """                FlymeStatusBarLyrics.clear();
                LyricsPresentation.resetTrackCycle();""")
    replace_once(session, "                ticker.stop();\n                lastPushedTitle = null;", """                ticker.stop();
                FlymeStatusBarLyrics.clear();
                lastPushedTitle = null;""")
    replace_once(session, "        if (state == null) {\n            return;", """        if (state == null) {
            playbackState = null;
            FlymeStatusBarLyrics.clear();
            return;""")
    replace_once(session, "        playbackState = state;", """        playbackState = state;
        if (state.getState() != PlaybackState.STATE_PLAYING) {
            FlymeStatusBarLyrics.clear();
        }""")
    replace_once(session, "            // The session was released; wait for the next metadata update.", """            FlymeStatusBarLyrics.clear();
            // The session was released; wait for the next metadata update.""")
    replace_once(session, "        String artist = getTrackLine();", """        String artist = getTrackLine();
        // Before title deduplication: the app can replace its notification between lyric lines.
        FlymeStatusBarLyrics.update(session, newTitle, playbackState);""")
    replace_once(session, "    private static void restoreIfPushed() {", """    private static void restoreIfPushed() {
        FlymeStatusBarLyrics.clear();""")
    replace_once(session, '            Logger.printException(() -> "tick failure", ex);', """            FlymeStatusBarLyrics.clear();
            Logger.printException(() -> "tick failure", ex);""")
    settings = music / "settings/Settings.java"
    anchor = '    public static final BooleanSetting LYRICS_MEDIASESSION ='
    replace_once(settings, anchor, """    public static final BooleanSetting LYRICS_FLYME_STATUS_BAR = new BooleanSetting(
            "morphe_music_lyrics_flyme_status_bar", FALSE, false, parent(LYRICS_ENABLED));
""" + anchor)
    installer = music / "patches/lyrics/ui/LyricsPanelInstaller.java"
    replace_once(installer, "            } else if (Settings.LYRICS_MINIPLAYER.key.equals(key)", """            } else if (Settings.LYRICS_FLYME_STATUS_BAR.key.equals(key)
                    || Settings.LYRICS_MINIPLAYER.key.equals(key)""")
    replace_once(installer, "                MiniplayerLyrics.onSettingsChanged();\n                return;", """                MiniplayerLyrics.onSettingsChanged();
                LockScreenLyrics.onSettingsChanged();
                return;""")
    strings = {
        "values": ("Flyme status bar lyrics", "Mirror the lock-screen lyric text in the Flyme status bar. Enable third-party lyrics and lock-screen lyrics first. Requires a supported Flyme system."),
        "values-zh-rCN": ("魅族状态栏歌词", "将锁屏歌词的相同文本显示到 Flyme 状态栏。请同时启用第三方歌词和锁屏媒体通知卡片歌词，需要系统支持。"),
        "values-zh-rTW": ("魅族狀態列歌詞", "將鎖定畫面歌詞的相同文字顯示到 Flyme 狀態列。請同時啟用第三方歌詞及鎖定畫面媒體通知歌詞，需要系統支援。"),
    }
    for locale, (title, summary) in strings.items():
        path = target / f"patches/src/main/resources/addresources/{locale}/music/strings.xml"
        replace_once(path, "</resources>", f'    <string name="morphe_music_lyrics_flyme_status_bar_title">{title}</string>\n    <string name="morphe_music_lyrics_flyme_status_bar_summary">{summary}</string>\n</resources>')
    for path in (ROOT / "overlay").rglob("*"):
        if path.is_file():
            dest = target / path.relative_to(ROOT / "overlay")
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(path, dest)
    # Distinguish the derivative's bundle identity and preserve the original license.
    build = target / "patches/build.gradle.kts"
    replace_once(build, 'name = "Morphe Patches"', 'name = "Flyme Lyrics Patches (community)"')
    replace_once(build, 'description = "Patches for Morphe"', 'description = "Community bundle: official patches plus Flyme status bar lyrics"')
    replace_once(build, 'source = "git@github.com:MorpheApp/morphe-patches.git"', 'source = "https://github.com/kljjh/flyme-lyrics-patches"')
    replace_once(build, 'author = "MorpheApp"', 'author = "kljjh; upstream Morphe contributors"')
    replace_once(build, 'website = "https://morphe.software"', 'website = "https://github.com/kljjh/flyme-lyrics-patches"')
    replace_once(target / "gradle.properties", f"version = {config['version']}", f"version = {config['project_version']}")
    # Include license notices inside the distributed bundle as well as in its source.
    for name in ("LICENSE", "NOTICE", "UPSTREAM.md"):
        dest = target / "patches/src/main/resources/META-INF" / name
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(ROOT / name, dest)
    print(f"Prepared community bundle {config['project_version']} from {revision}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("upstream", type=Path)
    prepare(parser.parse_args().upstream.resolve())
