#!/usr/bin/env python3
"""
Zips the project source for sharing, without secrets or build output.

    python tools/make_source_zip.py <out.zip>

Left out: keystore/ (release key + passwords), local.properties (SDK path), build/ and .gradle/ output,
IDE folders, captures, Python caches, and APK/AAB files. After writing, it re-opens the zip and fails if
anything that looks like a key, keystore or password file slipped in.
"""
import os, re, sys, zipfile

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
SKIP_DIRS = {"keystore", "build", ".gradle", ".kotlin", ".idea", "captures", "__pycache__", ".git", ".cxx"}
SKIP_FILES = {"local.properties", "keystore.properties"}
SKIP_EXT = {".jks", ".keystore", ".p12", ".pfx", ".apk", ".aab", ".iml", ".pem", ".key"}
SECRET_PATTERNS = [
    re.compile(rb"AIza[0-9A-Za-z_\-]{35}"),                     # Google API key
    re.compile(rb"sk-[A-Za-z0-9]{20,}"),                        # generic secret keys
    re.compile(rb"-----BEGIN (RSA |EC )?PRIVATE KEY-----"),
]


def include(rel):
    parts = rel.replace("\\", "/").split("/")
    if any(p in SKIP_DIRS for p in parts[:-1]):
        return False
    name = parts[-1]
    return name not in SKIP_FILES and os.path.splitext(name)[1].lower() not in SKIP_EXT


def main(out):
    out = os.path.abspath(out)
    count = 0
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        for d, dirs, files in os.walk(ROOT):
            dirs[:] = sorted(x for x in dirs if x not in SKIP_DIRS)
            for f in sorted(files):
                full = os.path.join(d, f)
                rel = os.path.relpath(full, ROOT)
                if os.path.abspath(full) == out or not include(rel):
                    continue
                z.write(full, os.path.join("HyperAthlete", rel))
                count += 1
    problems = []
    with zipfile.ZipFile(out) as z:
        for info in z.infolist():
            n = info.filename
            if not include(n.split("/", 1)[1]):
                problems.append(f"excluded path present: {n}")
            data = z.read(n)
            if n.endswith((".kt", ".kts", ".xml", ".properties", ".json", ".md", ".py", ".toml", ".pro", ".txt")):
                for p in SECRET_PATTERNS:
                    if p.search(data):
                        problems.append(f"possible secret in {n}: {p.pattern[:40]!r}")
            # A signing password written as a value (README examples use "...").
            if n.endswith(".properties") and re.search(rb"(?im)^\s*(storePassword|keyPassword)\s*=\s*(?!\.\.\.)\S", data):
                problems.append(f"signing password in {n}")
    if problems:
        os.remove(out)
        sys.exit("Refusing to keep the zip:\n" + "\n".join(problems))
    print(f"{out}: {count} files, no keystore, passwords, keys or build output")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "..", "HyperAthlete-source.zip"))
