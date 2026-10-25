# File path and name: tools/dev/folder_sync_probe.py
# Modified On Timestamp: 2026-10-25 @ 15:10 EDT
# Created On Timestamp: 2026-10-25 @ 15:10 EDT
# File Description: Phase 0 spike helper for 2.0.0: checks that a shared folder (OneDrive, Dropbox,
#   Google Drive, a network share) carries write-once files between two computers completely and in time.
# Uses: Python 3 standard library only.
# Used By: The owner, by hand, on two computers or two Windows accounts (docs/adr/0011-several-computers.md).
# Purpose: Prove the exchange-folder rules of docs/plan-multi-device.md §5.3 on real cloud folders.
"""Shared-folder probe for the 2.0.0 exchange folder.

Each computer writes ONLY its own files, like RANN's Roost 2.0 will: <folder>/probe/<device>/<seq>.chg,
written as .part and then renamed. Each file holds a JSON header line (device, seq, time written, SHA-256)
and random bytes. The other computer reads them and reports what arrived, what is missing or damaged,
how long files took, and any "conflicted copy" files the cloud service made.

On each computer (use a different device name on each), run both at the same time in two windows:
    python tools/dev/folder_sync_probe.py write --folder "<shared folder>" --device laptop-a
    python tools/dev/folder_sync_probe.py read  --folder "<shared folder>" --device laptop-b

write: --count (default 200 files) --interval (seconds between files, default 2) --size (bytes, default 20000)
       --prune (after writing, delete own files 1..N, default 50, as 2.0 prunes old change files).
read:  --device is the OTHER computer's name; --expect (default 200) --timeout (minutes, default 30).
The read report ends with one line starting with RESULT; copy the read output into the session notes.
Times assume both computers' clocks are roughly right (the 2.0 clock does not need that; this probe does).
"""
import argparse
import hashlib
import json
import os
import re
import sys
import time

FILE = re.compile(r"\d{6}\.chg")


def device_dir(folder, device):
    return os.path.join(folder, "probe", device)


def write(args):
    out = device_dir(args.folder, args.device)
    os.makedirs(out, exist_ok=True)
    for seq in range(1, args.count + 1):
        payload = os.urandom(args.size)
        header = {"device": args.device, "seq": seq, "written": time.time(), "sha256": hashlib.sha256(payload).hexdigest()}
        final = os.path.join(out, "%06d.chg" % seq)
        part = final + ".part"
        with open(part, "wb") as f:
            f.write((json.dumps(header) + "\n").encode("utf-8"))
            f.write(payload)
            f.flush()
            os.fsync(f.fileno())
        os.replace(part, final)
        print("wrote %s" % os.path.basename(final), flush=True)
        time.sleep(args.interval)
    for seq in range(1, min(args.prune, args.count) + 1):
        try:
            os.remove(os.path.join(out, "%06d.chg" % seq))
        except FileNotFoundError:
            pass
    print("done: %d written, %d pruned" % (args.count, min(args.prune, args.count)), flush=True)


def read_one(path):
    """Returns (header, ok) or None while the file is not available yet (still downloading)."""
    try:
        with open(path, "rb") as f:
            data = f.read()
    except OSError:
        return None
    line, _, payload = data.partition(b"\n")
    try:
        header = json.loads(line.decode("utf-8"))
    except ValueError:
        return None
    return header, hashlib.sha256(payload).hexdigest() == header.get("sha256")


def read(args):
    src = device_dir(args.folder, args.device)
    seen, damaged, delays, conflicts, parts = {}, set(), [], set(), set()
    deadline = time.time() + args.timeout * 60
    last_report = 0
    while time.time() < deadline:
        names = os.listdir(src) if os.path.isdir(src) else []
        for name in names:
            path = os.path.join(src, name)
            if name.endswith(".part"):
                parts.add(name)
                continue
            if not FILE.fullmatch(name):
                # A cloud service's copy ("000001 (1).chg", "000001-LAPTOP.chg", "conflicted copy"...).
                conflicts.add(name)
                continue
            if name in seen:
                continue
            result = read_one(path)
            if result is None:
                continue
            header, ok = result
            seen[name] = header["seq"]
            if ok:
                delays.append(time.time() - header["written"])
            else:
                damaged.add(name)
        if time.time() - last_report > 30:
            print("seen %d of %d" % (len(seen), args.expect), flush=True)
            last_report = time.time()
        if len(seen) >= args.expect:
            break
        time.sleep(1)
    got = sorted(seen.values())
    missing = sorted(set(range(1, args.expect + 1)) - set(got))
    print("files read: %d of %d" % (len(seen), args.expect))
    print("missing: %s" % (missing[:40] if missing else "none"))
    print("damaged: %s" % (sorted(damaged) or "none"))
    print("conflict copies: %s" % (sorted(conflicts) or "none"))
    print(".part files seen (expected now and then, never kept): %d" % len(parts))
    if delays:
        delays.sort()
        print("delay seconds: median %.1f, 90%% %.1f, worst %.1f" % (delays[len(delays) // 2], delays[int(len(delays) * 0.9)], delays[-1]))
    ok = not missing and not damaged and not conflicts
    print("RESULT %s: %d read, %d missing, %d damaged, %d conflict copies" % ("PASS" if ok else "FAIL", len(seen), len(missing), len(damaged), len(conflicts)))
    return 0 if ok else 1


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = p.add_subparsers(dest="mode", required=True)
    w = sub.add_parser("write")
    w.add_argument("--folder", required=True)
    w.add_argument("--device", required=True)
    w.add_argument("--count", type=int, default=200)
    w.add_argument("--interval", type=float, default=2)
    w.add_argument("--size", type=int, default=20000)
    w.add_argument("--prune", type=int, default=50)
    r = sub.add_parser("read")
    r.add_argument("--folder", required=True)
    r.add_argument("--device", required=True)
    r.add_argument("--expect", type=int, default=200)
    r.add_argument("--timeout", type=float, default=30)
    args = p.parse_args()
    if args.mode == "write":
        write(args)
        return 0
    return read(args)


if __name__ == "__main__":
    sys.exit(main())
