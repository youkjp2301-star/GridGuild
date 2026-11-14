#!/usr/bin/env python3
"""External UI helpers for the signed Release on the dedicated submission QA AVD.

Examples (PowerShell):
  python scripts/submission-smoke.py --serial emulator-5556 inspect
  python scripts/submission-smoke.py --serial emulator-5556 tap --text '사상구'
  python scripts/submission-smoke.py --serial emulator-5556 tap --description '연결 · 테스트'
  python scripts/submission-smoke.py --serial emulator-5556 swipe --direction up
  python scripts/submission-smoke.py --serial emulator-5556 capture --output artifacts/submission/home.png

Every invocation checks the AVD identity and non-debuggable target package.
No app-data reset, fixture injection, uninstall, or emulator restart is provided.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import os
from pathlib import Path
import re
import shutil
import struct
import subprocess
import sys
import xml.etree.ElementTree as ET


QA_AVD = "GridGuild_Submission_QA_API34"
PACKAGE = "com.gridguild"
ACTIVITY = f"{PACKAGE}/.MainActivity"
BOUNDS = re.compile(r"^\[(\d+),(\d+)\]\[(\d+),(\d+)\]$")


class QaError(RuntimeError):
    pass


def find_adb(explicit: str | None) -> str:
    if explicit:
        candidate = Path(explicit)
        if not candidate.is_file():
            raise QaError("The explicitly selected adb executable does not exist")
        return str(candidate.resolve())
    root = Path(__file__).resolve().parents[1]
    candidates = [root / ".tools/android-sdk/platform-tools/adb.exe"]
    for variable in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        if os.environ.get(variable):
            candidates.append(Path(os.environ[variable]) / "platform-tools/adb.exe")
    for candidate in candidates:
        if candidate.is_file():
            return str(candidate.resolve())
    executable = shutil.which("adb")
    if not executable:
        raise QaError("adb was not found; pass --adb with its executable path")
    return executable


class ReleaseQa:
    def __init__(self, adb: str, serial: str):
        if not re.fullmatch(r"emulator-\d+", serial):
            raise QaError("Only an explicitly named emulator serial is allowed")
        self.adb = adb
        self.serial = serial
        self.identity: dict[str, object] = {}

    def command(self, *args: str, binary: bool = False, timeout: int = 25):
        result = subprocess.run(
            [self.adb, "-s", self.serial, *args],
            capture_output=True, timeout=timeout, check=False,
        )
        if result.returncode:
            error = result.stderr.decode("utf-8", errors="replace").strip()
            raise QaError(f"adb operation failed ({result.returncode}): {error[:500]}")
        return result.stdout if binary else result.stdout.decode("utf-8", errors="replace")

    def verify(self) -> dict[str, object]:
        names = self.command("emu", "avd", "name").splitlines()
        actual = next((line.strip() for line in names if line.strip() and line.strip() != "OK"), "")
        if actual != QA_AVD:
            raise QaError(f"Refusing this emulator: expected {QA_AVD}, found {actual or 'unknown'}")
        if not self.command("shell", "pm", "path", PACKAGE).strip().startswith("package:"):
            raise QaError("The target package is not installed")
        package = self.command("shell", "dumpsys", "package", PACKAGE)
        flags = re.findall(r"(?:pkgFlags|flags)=\[([^\]]*)\]", package)
        if not flags or not any("HAS_CODE" in value.split() for value in flags):
            raise QaError("Could not establish the installed target application flags")
        if any("DEBUGGABLE" in value.split() for value in flags):
            raise QaError("Refusing a debuggable target: install the real signed Release first")
        version = re.search(r"\bversionName=([^\s]+)", package)
        version_code = re.search(r"\bversionCode=(\d+)", package)
        self.identity = {
            "serial": self.serial, "avd": actual, "package": PACKAGE,
            "debuggable": False,
            "version_name": version.group(1) if version else None,
            "version_code": int(version_code.group(1)) if version_code else None,
        }
        return self.identity

    def foreground(self):
        windows = self.command("shell", "dumpsys", "window")
        focus = re.search(r"mCurrentFocus=(.*)", windows)
        if not focus or not re.search(r"\bcom\.gridguild/", focus.group(1)):
            raise QaError("GridGuild is not the focused window; inspect system dialogs before continuing")

    def tree(self):
        self.foreground()
        remote = f"/sdcard/Download/gridguild-submission-smoke-{os.getpid()}.xml"
        self.command("shell", "uiautomator", "dump", remote, timeout=35)
        xml = self.command("exec-out", "cat", remote)
        start = xml.find("<?xml")
        if start < 0:
            start = xml.find("<hierarchy")
        if start < 0:
            raise QaError("UIAutomator did not return an XML hierarchy")
        root = ET.fromstring(xml[start:])
        parents = {child: parent for parent in root.iter() for child in parent}
        nodes = [node for node in root.iter("node") if node.get("package") == PACKAGE]
        if not nodes:
            raise QaError("The visible hierarchy contains no GridGuild nodes")
        return root, parents, nodes

    @staticmethod
    def bounds(node: ET.Element) -> tuple[int, int, int, int]:
        match = BOUNDS.fullmatch(node.get("bounds", ""))
        if not match:
            raise QaError("The selected UI node has unavailable or invalid bounds")
        left, top, right, bottom = map(int, match.groups())
        if right <= left or bottom <= top:
            raise QaError("The selected UI node has empty bounds; scroll it into view first")
        return left, top, right, bottom

    @staticmethod
    def node_info(node: ET.Element) -> dict[str, object]:
        return {
            "text": node.get("text", ""), "description": node.get("content-desc", ""),
            "resource_id": node.get("resource-id", ""), "class": node.get("class", ""),
            "bounds": node.get("bounds", ""), "clickable": node.get("clickable") == "true",
            "enabled": node.get("enabled") == "true", "selected": node.get("selected") == "true",
            "scrollable": node.get("scrollable") == "true",
        }

    def inspect(self, contains: str | None, all_nodes: bool):
        _, _, nodes = self.tree()
        interesting = nodes if all_nodes else [node for node in nodes if
            node.get("text") or node.get("content-desc") or node.get("clickable") == "true" or
            node.get("scrollable") == "true"]
        if contains is not None:
            interesting = [node for node in interesting if contains in
                (node.get("text", "") + " " + node.get("content-desc", ""))]
        return {"nodes": [self.node_info(node) for node in interesting]}

    def tap(self, text: str | None, description: str | None, contains: bool, index: int | None):
        root, parents, nodes = self.tree()
        attribute, expected = ("text", text) if text is not None else ("content-desc", description)
        matches = [node for node in nodes if
            (expected in node.get(attribute, "") if contains else node.get(attribute, "") == expected)]
        # A hidden/zero-size accessibility node is not a valid interaction target.
        matches = [node for node in matches if node.get("enabled") == "true" and
            BOUNDS.fullmatch(node.get("bounds", "")) and
            (lambda b: b[2] > b[0] and b[3] > b[1])(tuple(map(int, BOUNDS.fullmatch(node.get("bounds", "")).groups())))]
        if not matches:
            raise QaError("No enabled visible node matches the selector; inspect or scroll first")
        if index is None and len(matches) != 1:
            raise QaError(f"Selector is ambiguous ({len(matches)} matches); inspect and pass --index explicitly")
        selected_index = 0 if index is None else index
        if selected_index < 0 or selected_index >= len(matches):
            raise QaError("--index is outside the selector matches")
        matched = matches[selected_index]
        target = matched
        while target.get("clickable") != "true" and target in parents:
            parent = parents[target]
            if parent.get("package") != PACKAGE:
                break
            target = parent
        if target.get("clickable") != "true":
            raise QaError("The selected node has no clickable app ancestor")
        if target.get("enabled") != "true":
            raise QaError("The selected action is disabled")
        left, top, right, bottom = self.bounds(target)
        # Clip the tap to every ancestor's visible rectangle.
        ancestor = target
        while ancestor in parents:
            ancestor = parents[ancestor]
            if ancestor is root or not ancestor.get("bounds"):
                continue
            a_left, a_top, a_right, a_bottom = self.bounds(ancestor)
            left, top, right, bottom = max(left, a_left), max(top, a_top), min(right, a_right), min(bottom, a_bottom)
        if right <= left or bottom <= top:
            raise QaError("Selected action is clipped outside the visible viewport")
        x, y = (left + right) // 2, (top + bottom) // 2
        self.command("shell", "input", "tap", str(x), str(y))
        return {"matched": self.node_info(matched), "action": self.node_info(target), "tap": [x, y]}

    def swipe(self, direction: str, coordinates: list[int] | None, duration: int):
        _, _, nodes = self.tree()
        bounds = [self.bounds(node) for node in nodes if BOUNDS.fullmatch(node.get("bounds", "")) and
            (lambda b: b[2] > b[0] and b[3] > b[1])(tuple(map(int, BOUNDS.fullmatch(node.get("bounds", "")).groups())))]
        width, height = max(value[2] for value in bounds), max(value[3] for value in bounds)
        if coordinates:
            x1, y1, x2, y2 = coordinates
        else:
            x1 = x2 = round(width * .58)
            y1, y2 = round(height * .75), round(height * .28)
            if direction == "down":
                y1, y2 = y2, y1
        if not (0 < x1 < width and 0 < x2 < width and 0 < y1 < height and 0 < y2 < height):
            raise QaError("Swipe coordinates must stay inside the visible app display")
        self.command("shell", "input", "swipe", str(x1), str(y1), str(x2), str(y2), str(duration))
        return {"swipe": [x1, y1, x2, y2], "duration_ms": duration}

    def capture(self, output: Path):
        self.foreground()
        if output.exists():
            raise QaError("Refusing to overwrite an existing capture; choose a new output filename")
        png = self.command("exec-out", "screencap", "-p", binary=True)
        if png[:8] != b"\x89PNG\r\n\x1a\n" or len(png) < 24:
            raise QaError("The device did not return a valid PNG screenshot")
        width, height = struct.unpack(">II", png[16:24])
        output.parent.mkdir(parents=True, exist_ok=True)
        # Exclusive creation also protects against a concurrent overwrite.
        with output.open("xb") as stream:
            stream.write(png)
        return {"screenshot": str(output.resolve()), "width": width, "height": height, "bytes": len(png)}


def parser():
    result = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    result.add_argument("--serial", required=True, help="Explicit dedicated QA emulator serial")
    result.add_argument("--adb", help="Optional adb executable path")
    commands = result.add_subparsers(dest="command", required=True)
    commands.add_parser("verify", help="Verify dedicated AVD and genuine non-debuggable Release")
    inspect = commands.add_parser("inspect", help="Read the current visible app accessibility hierarchy")
    inspect.add_argument("--contains", help="Filter text and content descriptions")
    inspect.add_argument("--all", action="store_true", help="Include layout-only app nodes")
    tap = commands.add_parser("tap", help="Tap one visible app text/description and its clickable ancestor")
    selector = tap.add_mutually_exclusive_group(required=True)
    selector.add_argument("--text")
    selector.add_argument("--description")
    tap.add_argument("--contains", action="store_true")
    tap.add_argument("--index", type=int, help="Explicit zero-based index when the selector is ambiguous")
    swipe = commands.add_parser("swipe", help="Scroll the focused app content")
    swipe.add_argument("--direction", choices=("up", "down"), default="up")
    swipe.add_argument("--coordinates", nargs=4, type=int, metavar=("X1", "Y1", "X2", "Y2"))
    swipe.add_argument("--duration", type=int, choices=range(150, 1001), default=350, metavar="150..1000")
    capture = commands.add_parser("capture", help="Save a real focused-app PNG without overwriting existing files")
    capture.add_argument("--output", type=Path, required=True)
    typing = commands.add_parser("type", help="Enter simple ASCII values in the currently focused app field")
    typing.add_argument("--text", required=True, help="ASCII letters/digits or @._:+/- only; no shell syntax")
    key = commands.add_parser("key", help="Send BACK or ENTER to the focused app")
    key.add_argument("--keycode", required=True, choices=("BACK", "ENTER"))
    commands.add_parser("launch", help="Launch the installed target activity without changing its data")
    return result


def main() -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
    args = parser().parse_args()
    record: dict[str, object] = {
        "timestamp_utc": dt.datetime.now(dt.timezone.utc).isoformat(),
        "command": args.command, "serial": args.serial,
    }
    try:
        qa = ReleaseQa(find_adb(args.adb), args.serial)
        record["device"] = qa.verify()
        if args.command == "verify":
            value = {"verified": True}
        elif args.command == "inspect":
            value = qa.inspect(args.contains, args.all)
        elif args.command == "tap":
            value = qa.tap(args.text, args.description, args.contains, args.index)
        elif args.command == "swipe":
            value = qa.swipe(args.direction, args.coordinates, args.duration)
        elif args.command == "capture":
            value = qa.capture(args.output)
        elif args.command == "type":
            qa.foreground()
            if not re.fullmatch(r"[A-Za-z0-9@._:+/\-]+", args.text):
                raise QaError("Input is restricted to simple ASCII values; shell syntax/spaces are rejected")
            qa.command("shell", "input", "text", args.text)
            value = {"typed_characters": len(args.text)}
        elif args.command == "key":
            qa.foreground()
            qa.command("shell", "input", "keyevent", "KEYCODE_" + args.keycode)
            value = {"keycode": args.keycode}
        elif args.command == "launch":
            status = qa.command("shell", "am", "start", "-W", "-n", ACTIVITY)
            if "Error:" in status or "Exception" in status:
                raise QaError("The target activity did not launch successfully")
            value = {"launch_output": status.strip()}
        else:
            raise QaError("Unsupported action")
        record.update({"ok": True, "result": value})
        print(json.dumps(record, ensure_ascii=False))
        return 0
    except (QaError, subprocess.TimeoutExpired, ET.ParseError, OSError, ValueError) as error:
        record.update({"ok": False, "error": str(error)})
        print(json.dumps(record, ensure_ascii=False))
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
