#!/usr/bin/env python3
"""
Packages a signed release for direct download (Settings → Check for updates reads downloads/version.json).

    ./gradlew assembleRelease          # signs with keystore/ (same key as every earlier version)
    python tools/make_release.py       # → downloads/HyperAthlete-<version>.apk + downloads/version.json

It refuses to package an APK that is unsigned or signed with a different certificate than the one the
installed app has (Android would reject it as an update). Nothing is uploaded anywhere.
"""
import glob, hashlib, json, os, re, shutil, subprocess, sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
# Release certificate (README → التوقيع): every update must be signed with this key.
EXPECTED_CERT_SHA256 = "0B73E57CA70DA0E9E8F4BD6AE785B6878CF9BEE98844E43E353D39EBD3D0FBB1"


def gradle_value(name):
    text = open(os.path.join(ROOT, "app", "build.gradle.kts"), encoding="utf-8").read()
    m = re.search(rf'{name}\s*=\s*"?([^"\n]+)"?', text)
    return m.group(1).strip()


def update_repo():
    p = os.path.join(ROOT, "gradle.properties")
    for line in open(p, encoding="utf-8"):
        if line.strip().startswith("hyperUpdateRepo="):
            return line.split("=", 1)[1].strip()
    return "OWNER/HyperAthlete"


def apksigner():
    sdk = None
    lp = os.path.join(ROOT, "local.properties")
    if os.path.exists(lp):
        for line in open(lp, encoding="utf-8"):
            if line.startswith("sdk.dir="):
                sdk = line.split("=", 1)[1].strip().replace("\\:", ":").replace("\\\\", "\\")
    sdk = sdk or os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    tools = sorted(glob.glob(os.path.join(sdk, "build-tools", "*", "apksigner*")))
    tools = [t for t in tools if t.endswith((".bat", "apksigner"))]
    if not tools:
        sys.exit("apksigner not found (Android SDK build-tools)")
    return tools[-1]


def signer_sha256(apk):
    out = subprocess.run([apksigner(), "verify", "--print-certs", apk], capture_output=True, text=True, shell=os.name == "nt")
    if out.returncode != 0:
        sys.exit("APK signature does not verify:\n" + out.stderr + out.stdout)
    m = re.search(r"Signer #1 certificate SHA-256 digest:\s*([0-9a-fA-F]+)", out.stdout)
    return m.group(1).upper() if m else None


def notes(lang):
    p = os.path.join(ROOT, "downloads", f"release-notes.{lang}.md")
    return open(p, encoding="utf-8").read().strip() if os.path.exists(p) else ""


def main():
    version_name = gradle_value("versionName")
    version_code = int(gradle_value("versionCode"))
    min_sdk = int(gradle_value("minSdk"))
    src = os.path.join(ROOT, "app", "build", "outputs", "apk", "release", "app-release.apk")
    if not os.path.exists(src):
        sys.exit("No signed app-release.apk. Put the release key in keystore/ (keystore.properties + .jks) and run ./gradlew assembleRelease.")
    cert = signer_sha256(src)
    if cert != EXPECTED_CERT_SHA256:
        sys.exit(f"Signed with a different key ({cert}). It would not install over the current app; not packaging it.")

    name = f"HyperAthlete-{version_name}.apk"
    out_dir = os.path.join(ROOT, "downloads")
    os.makedirs(out_dir, exist_ok=True)
    dst = os.path.join(out_dir, name)
    shutil.copyfile(src, dst)
    sha = hashlib.sha256(open(dst, "rb").read()).hexdigest()
    manifest = {
        "versionCode": version_code,
        "versionName": version_name,
        "apkUrl": f"https://github.com/{update_repo()}/releases/download/v{version_name}/{name}",
        "sha256": sha,
        "releaseNotes": {"en": notes("en"), "ar": notes("ar")},
        "minSdk": min_sdk,
    }
    with open(os.path.join(out_dir, "version.json"), "w", encoding="utf-8", newline="\n") as f:
        json.dump(manifest, f, ensure_ascii=False, indent=2)
        f.write("\n")
    print(f"{dst}\nsha256 {sha}\ncert   {cert} (matches the release key)")


if __name__ == "__main__":
    main()
