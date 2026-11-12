"""模拟器 UI 验证辅助：uiautomator dump + adb input。仅供开发验证，不进 APK。"""
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path

ADB = "D:/Android/Sdk/platform-tools/adb.exe"
PKG = "org.changlianxi.app"
SHOTS = Path(__file__).resolve().parent.parent / "screenshots"


def adb(*args):
    r = subprocess.run([ADB, *args], capture_output=True, text=True, encoding="utf-8", errors="replace")
    return r.stdout + r.stderr


def shell(cmd):
    return adb("shell", cmd)


def nodes():
    xml = ""
    for _ in range(3):
        r = shell("uiautomator dump /sdcard/ui.xml 2>&1")
        if "dumped to" in r:
            xml = shell("cat /sdcard/ui.xml")
            break
        time.sleep(0.6)
    if not xml:
        return []
    root = ET.fromstring(xml)
    out = []
    for n in root.iter("node"):
        label = n.get("text") or n.get("content-desc") or ""
        b = re.match(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", n.get("bounds"))
        if not b:
            continue
        x1, y1, x2, y2 = map(int, b.groups())
        out.append({
            "label": label, "x": (x1 + x2) // 2, "y": (y1 + y2) // 2,
            "cls": n.get("class"), "clickable": n.get("clickable") == "true",
            "bounds": (x1, y1, x2, y2),
        })
    return out


def find(text):
    for n in nodes():
        if text in n["label"]:
            return n
    return None


def wait_for(text, timeout=15.0):
    deadline = time.time() + timeout
    while time.time() < deadline:
        n = find(text)
        if n:
            return n
        time.sleep(0.8)
    print(f"TIMEOUT waiting for: {text}")
    return None


def tap(text, wait=1.0):
    n = find(text)
    if not n:
        print(f"NOT FOUND: {text}")
        return False
    shell(f"input tap {n['x']} {n['y']}")
    time.sleep(wait)
    return True


def tap_in_row(row_text, button_text, wait=1.0):
    """联系人列表行内按钮：按行文本的 y 坐标匹配同名按钮。"""
    ns = nodes()
    row = next((n for n in ns if row_text in n["label"]), None)
    if not row:
        print(f"ROW NOT FOUND: {row_text}")
        return False
    btns = [n for n in ns if button_text in n["label"]]
    if not btns:
        print(f"BUTTON NOT FOUND: {button_text}")
        return False
    btn = min(btns, key=lambda n: abs(n["y"] - row["y"]))
    shell(f"input tap {btn['x']} {btn['y']}")
    time.sleep(wait)
    return True


def tap_button(text, wait=1.0):
    """精确匹配对话框按钮（Compose TextButton 报告为 TextView/Button，列表行是 View）。"""
    want = text.replace("’", "'")
    for attempt in range(3):
        ns = nodes()
        for n in ns:
            if n["label"].replace("’", "'") == want and ("Button" in n["cls"] or "TextView" in n["cls"]):
                shell(f"input tap {n['x']} {n['y']}")
                time.sleep(wait)
                return True
        time.sleep(0.6)
    print(f"BUTTON NOT FOUND: {text}")
    for n in ns:
        if n["label"]:
            print(f"  {n['label']!r} {n['cls'].split('.')[-1]}")
    return False


def tap_at(x, y, wait=1.0):
    shell(f"input tap {x} {y}")
    time.sleep(wait)


def tap_radio_near(label, wait=1.0):
    """RadioButton 圆圈本体才可点：按旁边文案的 y 坐标找 RadioButton 节点。"""
    ns = nodes()
    text = next((n for n in ns if label in n["label"]), None)
    if not text:
        print(f"RADIO LABEL NOT FOUND: {label}")
        return False
    radios = [n for n in ns if "RadioButton" in n["cls"]]
    if not radios:
        print(f"RADIO NOT FOUND: {label}")
        return False
    r = min(radios, key=lambda n: abs(n["y"] - text["y"]))
    shell(f"input tap {r['x']} {r['y']}")
    time.sleep(wait)
    return True


def type_text(s):
    shell(f"input text {s}")
    time.sleep(0.3)


def long_press(x, y, ms=4500):
    shell(f"input swipe {x} {y} {x} {y} {ms}")
    time.sleep(1.0)


def shot(name):
    SHOTS.mkdir(exist_ok=True)
    dest = SHOTS / f"{name}.png"
    shell(f"screencap -p /data/local/tmp/{name}.png")
    r = adb("pull", f"/data/local/tmp/{name}.png", str(dest))
    shell(f"rm /data/local/tmp/{name}.png")
    if not dest.is_file():
        print(f"SHOT FAILED: {name}: {r.strip()}")
    else:
        print(f"shot: {dest}")


def start():
    shell(f"am start -n {PKG}/.MainActivity")
    time.sleep(2.5)


def stop():
    shell(f"am force-stop {PKG}")
    time.sleep(0.5)


def labels():
    for n in nodes():
        if n["label"]:
            print(f"  {n['label']!r} ({n['x']},{n['y']}) {n['cls'].split('.')[-1]}")


if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "labels"
    if cmd == "labels":
        labels()
    elif cmd == "tap":
        print(tap(sys.argv[2]))
    elif cmd == "shot":
        shot(sys.argv[2])
    elif cmd == "start":
        start()
    elif cmd == "stop":
        stop()