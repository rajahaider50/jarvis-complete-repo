"""Last-resort dependency installer: downloads wheels straight from PyPI with the standard library.

Why this exists: on some phones/containers `pip` is missing, points at another interpreter, or hangs
(Termux + proot is a common case). Instead of failing, the start script calls this file, which:

  1. asks PyPI for each package's JSON,
  2. picks the best wheel for this exact Python + platform (pure-python if available, otherwise the
     right manylinux/musllinux/macos/windows/android build),
  3. unpacks them into `server/.pylibs`,
  4. proves `import flask` works in a subprocess.

Nothing is patched or faked: the wheels are the official ones, only the installer is ours.

    python3 vendor_wheels.py            # into ./pylibs
    python3 vendor_wheels.py --dir X    # into X
"""
from __future__ import annotations

import argparse
import json
import os
import platform
import subprocess
import sys
import urllib.request
import zipfile

RUNTIME = ["flask", "werkzeug", "jinja2", "itsdangerous", "click", "blinker", "markupsafe"]
INDEX = "https://pypi.org/pypi/{name}/json"


def _get(url: str, timeout: int = 90) -> bytes:
    req = urllib.request.Request(url, headers={"User-Agent": "jarvis-vendor-wheels/1.0"})
    with urllib.request.urlopen(req, timeout=timeout) as r:
        return r.read()


def _arch_tags() -> list[str]:
    m = platform.machine().lower()
    machine = {"arm64": "aarch64", "armv8l": "aarch64", "x64": "x86_64", "amd64": "x86_64",
               "i386": "i686", "i686": "i686"}.get(m, m)
    in_termux = "com.termux" in os.environ.get("PREFIX", "")
    if in_termux:
        android = {"aarch64": "arm64_v8a", "armv7l": "armeabi_v7a", "x86_64": "x86_64",
                   "i686": "x86"}.get(machine, "arm64_v8a")
        return [f"android_24_{android}", f"android_21_{android}"]
    sysname = platform.system().lower()
    if sysname == "darwin":
        return ["macosx_11_0_arm64", "macosx_12_0_arm64", "macosx_10_9_x86_64", "macosx_11_0_universal2"]
    if sysname == "windows":
        return ["win_amd64", "win32"]
    return [f"manylinux2014_{machine}", f"manylinux_2_17_{machine}", f"manylinux_2_28_{machine}",
            f"manylinux_2_31_{machine}", f"musllinux_1_1_{machine}", f"musllinux_1_2_{machine}",
            f"linux_{machine}"]


def _cp_tags() -> list[str]:
    v = sys.version_info
    return [f"cp{v.major}{v.minor}", f"cp{v.major}{v.minor}t", f"cp{v.major}{v.minor}m"]


def pick(pkg: str, data: dict) -> dict:
    urls = [u for u in data["urls"] if u["filename"].endswith(".whl")]
    if not urls:
        raise RuntimeError(f"{pkg}: no wheel published (source build would be needed)")
    pure = [u for u in urls if "-py3-none-any.whl" in u["filename"]]
    if pure:
        return pure[-1]
    best: dict | None = None
    for cp in _cp_tags():
        for arch in _arch_tags():
            for u in urls:
                f = u["filename"]
                if cp in f and arch in f and "t-" not in f.replace(cp + "t", "T"):
                    best = u
                    break
            if best:
                return best
    for u in urls:                                   # last resort: any wheel for this platform family
        if any(a in u["filename"] for a in _arch_tags()):
            return u
    return urls[-1]


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--dir", default=os.path.join(os.path.dirname(os.path.abspath(__file__)), ".pylibs"))
    args = ap.parse_args()
    out = os.path.abspath(args.dir)
    os.makedirs(out, exist_ok=True)
    print(f"python   {sys.version.split()[0]}  {platform.machine()}  {platform.system()}")
    print(f"target   {out}")
    got = 0
    for pkg in RUNTIME:
        try:
            data = json.loads(_get(INDEX.format(name=pkg)))
            u = pick(pkg, data)
            blob = _get(u["url"])
            tmp = os.path.join(out, u["filename"])
            with open(tmp, "wb") as fh:
                fh.write(blob)
            with zipfile.ZipFile(tmp) as z:
                z.extractall(out)
            os.remove(tmp)
            got += 1
            print(f"ok    {pkg:<12} {data['info']['version']:<10} {u['filename'][:60]}")
        except Exception as e:                                        # noqa: BLE001 - reported clearly
            print(f"FAIL  {pkg:<12} {type(e).__name__}: {e}")
            return 1
    probe = subprocess.run(
        [sys.executable, "-c", "import sys; sys.path.insert(0, %r); import flask; print(flask.__version__)" % out],
        capture_output=True, text=True)
    if probe.returncode != 0:
        print("FAIL  import check:", (probe.stderr or "").strip()[-300:])
        return 1
    print(f"ok    flask imports from {out} (version {probe.stdout.strip()}) after {got} packages")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
