"""One-shot rebrand of the OneLauncher/OneClient fork into Zyro Client.

Idempotent: running it twice is harmless.
Run from the repo root:  python -I branding/rebrand.py
"""

from __future__ import annotations

import json
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

CRATES = [
    "app", "auth", "cluster", "common", "content", "core", "db", "discord",
    "events", "java", "macro", "mc", "net", "polyplus",
]
CRATE_RE = re.compile(r"\boneclient_(" + "|".join(CRATES) + r")\b")

TEXT_EXT = {
    ".rs", ".toml", ".lock", ".yml", ".yaml", ".json", ".md", ".template",
    ".plist", ".entitlements", ".txt", ".sh", ".ps1", ".nsi", ".desktop", ".xml",
}

# String literals that must keep "OneClient" because they are data, not branding:
# Polyfrost bundle names, the OneClient v1 migration source, etc.
KEEP_LINE = re.compile(
    r"Fabric \[|OneClient 1\.|OneClientV1|BaseDirs::new\(\)\.map\(\|d\| d\.data_dir\(\)\.join\(\"OneClient\"\)\)"
)
KEEP_FILES = {"migration/oneclient_v1.rs", "migration/mod.rs"}

STRING_RE = re.compile(r'"(?:[^"\\\n]|\\.)*"')

# Exact, targeted replacements (file suffix -> [(old, new)])
EXACT: dict[str, list[tuple[str, str]]] = {
    "zyro_common/src/paths.rs": [
        ('const ORGANIZATION: &str = "Polyfrost";', 'const ORGANIZATION: &str = "Zyro";'),
        ('"OneClient-dev"', '"ZyroClient-dev"'),
        ('"OneClient"', '"ZyroClient"'),
        ("every Polyfrost product", "every Zyro product"),
    ],
    "zyro_core/src/settings/data_dir.rs": [
        ('pub const FOLDER_NAME: &str = "OneClient";', 'pub const FOLDER_NAME: &str = "ZyroClient";'),
        ('".oneclient_write_test"', '".zyro_write_test"'),
    ],
    "zyro_app/src/ipc.rs": [
        (r"org.polyfrost.OneClient.ipc", r"org.zyro.ZyroClient.ipc"),
        (r"org.polyfrost.OneClient-dev.ipc", r"org.zyro.ZyroClient-dev.ipc"),
    ],
    "zyro_app/src/protocol/mod.rs": [
        ('pub const SCHEME: &str = "oneclient-dev";', 'pub const SCHEME: &str = "zyro-dev";'),
        ('pub const SCHEME: &str = "oneclient";', 'pub const SCHEME: &str = "zyro";'),
    ],
    "zyro_app/src/file_content.rs": [
        ("org.polyfrost.OneClient.shortcut", "org.zyro.ZyroClient.shortcut"),
        ('"oneclient",', '"zyro",'),
        ("x-scheme-handler/oneclient;", "x-scheme-handler/zyro;"),
        ("oneclient://launch", "zyro://launch"),
    ],
    "zyro_app/src/constants.rs": [
        ('pub const WINDOW_TITLE: &str = "OneClient";', 'pub const WINDOW_TITLE: &str = "Zyro Client";'),
        (
            '"https://github.com/Polyfrost/OneLauncher/releases/latest/download/latest.json"',
            'match option_env!("ZYRO_UPDATER_ENDPOINT") {\n    Some(url) => url,\n    None => "",\n}',
        ),
    ],
    "zyro_common/src/constants.rs": [
        ('option_env!("ONECLIENT_SENTRY_DSN")', 'option_env!("ZYRO_SENTRY_DSN")'),
        (
            '"https://e7dff7e07427e1a28b9212cfcc8ddc1e@o4511714343124992.ingest.us.sentry.io/4511714354135040"',
            '// Crash reporting is opt-in for Zyro builds: no DSN, no telemetry.\n        ""',
        ),
        (
            'pub const DISCORD_CLIENT_ID: &str = "1426999264633946334";',
            '// Create your own Discord application and build with ZYRO_DISCORD_CLIENT_ID to enable\n'
            '// Rich Presence. Empty means Rich Presence stays off.\n'
            'pub const DISCORD_CLIENT_ID: &str = match option_env!("ZYRO_DISCORD_CLIENT_ID") {\n'
            '    Some(id) => id,\n'
            '    None => "",\n'
            '};',
        ),
        ('"OneClient v",', '"Zyro Client v",'),
    ],
    "zyro_core/src/reporting.rs": [
        ('option_env!("ONECLIENT_SENTRY_DSN")', 'option_env!("ZYRO_SENTRY_DSN")'),
    ],
    "zyro_app/build.rs": [
        ('"Polyfrost Inc."', '"Zyro"'),
        ('"Â© 2026 Polyfrost Inc."', '"Â© 2026 Zyro. Based on OneLauncher Â© Polyfrost (GPL-3.0)"'),
        ('res.set("ProductName", "OneClient");', 'res.set("ProductName", "Zyro Client");'),
        ('res.set("FileDescription", "OneClient");', 'res.set("FileDescription", "Zyro Client");'),
    ],
    "zyro_app/src/theme/mod.rs": [
        ('.text("ONECLIENT")', '.text("ZYRO")'),
    ],
    "zyro_mc/src/arguments.rs": [
        ('.replace("${launcher_name}", "OneClient")', '.replace("${launcher_name}", "ZyroClient")'),
    ],
    "zyro_discord/src/lib.rs": [
        ('.large_text("OneClient")', '.large_text("Zyro Client")'),
        ('const LARGE_IMAGE: &str = "onelauncher_logo_512";', 'const LARGE_IMAGE: &str = "zyro_logo";'),
    ],
    "zyro_polyplus/src/session.rs": [('"OneClient {} ({})"', '"ZyroClient/{} ({})"')],
    "zyro_net/src/service.rs": [('"OneClient {} ({})"', '"ZyroClient/{} ({})"')],
}


def tracked_files() -> list[Path]:
    out = subprocess.run(["git", "ls-files", "-z"], cwd=ROOT, capture_output=True, check=True)
    return [ROOT / p for p in out.stdout.decode("utf-8").split("\0") if p]


def rename_crate_dirs() -> None:
    for crate in CRATES:
        src = ROOT / "packages" / f"oneclient_{crate}"
        dst = ROOT / "packages" / f"zyro_{crate}"
        if src.exists() and not dst.exists():
            subprocess.run(["git", "mv", str(src), str(dst)], cwd=ROOT, check=True)


def rebrand_rust_strings(rel: str, text: str) -> str:
    if any(rel.endswith(k) for k in KEEP_FILES):
        return text
    lines = text.split("\n")
    for i, line in enumerate(lines):
        if "OneClient" not in line and "OneLauncher" not in line:
            continue
        if KEEP_LINE.search(line):
            continue

        def fix(m: re.Match[str]) -> str:
            s = m.group(0)
            if "polyfrost.org" in s or "github.com/Polyfrost" in s:
                return s
            return s.replace("OneLauncher", "Zyro").replace("OneClient", "Zyro")

        lines[i] = STRING_RE.sub(fix, line)
    return "\n".join(lines)


def rebrand_lang(path: Path) -> None:
    data = json.loads(path.read_text(encoding="utf-8"))
    changed = 0
    for key, value in data.items():
        if not isinstance(value, str) or "Fabric [" in value:
            continue
        new = (
            value.replace("OneLauncher", "Zyro")
            .replace("a OneClient", "a Zyro")
            .replace("OneClient", "Zyro")
        )
        if new != value:
            data[key] = new
            changed += 1
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n")
    print(f"lang: {changed} values rebranded in {path.name}")


def main() -> None:
    rename_crate_dirs()
    for path in tracked_files():
        if not path.exists():
            # git ls-files may still list pre-rename paths
            continue
        if path.suffix.lower() not in TEXT_EXT and path.name not in {"Cargo.toml", "Cargo.lock"}:
            continue
        rel = path.relative_to(ROOT).as_posix()
        if rel.startswith("branding/"):
            continue
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        original = text

        text = CRATE_RE.sub(r"zyro_\1", text)

        for suffix, pairs in EXACT.items():
            if rel.endswith(suffix):
                for old, new in pairs:
                    text = text.replace(old, new)

        if path.suffix == ".rs":
            text = rebrand_rust_strings(rel, text)

        if text != original:
            path.write_text(text, encoding="utf-8", newline="")

    for lang in (ROOT / "packages" / "zyro_app" / "lang").glob("*.json"):
        rebrand_lang(lang)
    print("rebrand done")


if __name__ == "__main__":
    main()
