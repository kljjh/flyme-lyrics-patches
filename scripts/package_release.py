import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import shutil
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def package(upstream, repository):
    config = json.loads((ROOT / "upstream.json").read_text())
    version = config["project_version"]
    source = upstream / f"patches/build/libs/patches-{version}.mpp"
    with zipfile.ZipFile(source) as bundle:
        names = bundle.namelist()
        required = ["classes.dex", "app/morphe/patches/music/layout/flymelyrics/FlymeLyricsPatchKt.class", "META-INF/LICENSE", "META-INF/NOTICE", "META-INF/UPSTREAM.md"]
        for name in required:
            if name not in names:
                raise RuntimeError(f"Invalid bundle: missing {name}")
        music = [n for n in names if n.endswith("music.rve") or n.endswith("music.mpe")]
        if not music:
            raise RuntimeError(f"Music extension not found: {[n for n in names if n.endswith(('.dex', '.rve', '.mpe'))]}")
        if not any(b"FlymeStatusBarLyrics" in bundle.read(n) for n in music):
            raise RuntimeError("Flyme runtime missing from the music extension")
        if b"Flyme Lyrics Patches (community)" not in bundle.read("META-INF/MANIFEST.MF"):
            raise RuntimeError("Bundle still has official branding")
    dist = ROOT / "dist"
    dist.mkdir(exist_ok=True)
    target = dist / f"flyme-lyrics-patches-{version}.mpp"
    shutil.copy2(source, target)
    digest = hashlib.sha256(target.read_bytes()).hexdigest()
    (dist / "SHA256SUMS.txt").write_text(f"{digest}  {target.name}\n", encoding="utf-8")
    manifest = {
        "version": version,
        "created_at": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "description": f"Community Flyme status bar lyrics, based on official Morphe {config['version']}. Requires third-party and lock-screen lyrics. Device validation pending.",
        "download_url": f"https://github.com/{repository}/releases/download/v{version}/{target.name}",
        "signature_download_url": "",
    }
    (dist / "patches-bundle.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    for name in ("LICENSE", "NOTICE", "UPSTREAM.md"):
        shutil.copy2(ROOT / name, dist / name)
    print(f"Validated {target.name}: {digest}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("upstream", type=Path)
    parser.add_argument("--repository", default="kljjh/flyme-lyrics-patches")
    args = parser.parse_args()
    package(args.upstream, args.repository)
