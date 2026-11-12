"""通过 run-as 预置 3 位虚构联系人 + PIN，验证持久化恢复与家属管理。仅供开发验证。"""
import hashlib
import json
import os
import uuid
from pathlib import Path

from emu import PKG, adb, shell

PIN = "2468"
CONTACTS = [("女儿", "daughter"), ("儿子", "son"), ("老伴", "spouse")]
ASSETS = Path(__file__).resolve().parents[2] / "changlianxi-site" / "web" / "assets"


def push_private(local: Path, remote_name: str):
    adb("push", str(local), f"/data/local/tmp/{remote_name}")
    shell(f"run-as {PKG} cp /data/local/tmp/{remote_name} files/{remote_name}")
    shell(f"rm /data/local/tmp/{remote_name}")


def main():
    salt = os.urandom(16)
    digest = hashlib.pbkdf2_hmac("sha256", PIN.encode(), salt, 120_000, dklen=32)
    pin_record = {"salt": salt.hex(), "hash": digest.hex(), "iterations": 120_000,
                  "failedAttempts": 0, "lockUntil": 0}

    ids = [str(uuid.uuid4()) for _ in CONTACTS]
    config = {
        "schemaVersion": 1,
        "configVersion": 1,
        "settings": {"dialMode": "TAP_PHOTO", "autoPlayVoice": True, "defaultContactId": ids[0]},
        "contacts": [
            {"id": cid, "displayName": name, "photoPath": f"media/photo/{cid}.jpg",
             "voicePath": f"media/voice/{cid}.m4a", "order": i, "callTarget": {"type": "DEMO"}}
            for i, (cid, (name, _)) in enumerate(zip(ids, CONTACTS))
        ],
    }

    shell(f"run-as {PKG} mkdir -p files/media/photo files/media/voice")
    for cid, (_, key) in zip(ids, CONTACTS):
        push_private(ASSETS / "photos" / f"{key}.webp", f"media/photo/{cid}.jpg")
        push_private(ASSETS / "audio" / f"voice-{key}.mp3", f"media/voice/{cid}.m4a")

    tmp = Path(__file__).parent
    (tmp / "_config.json").write_text(json.dumps(config, ensure_ascii=False), encoding="utf-8")
    (tmp / "_pin.json").write_text(json.dumps(pin_record), encoding="utf-8")
    push_private(tmp / "_config.json", "config.json")
    push_private(tmp / "_pin.json", "pin.json")
    (tmp / "_config.json").unlink()
    (tmp / "_pin.json").unlink()

    print("seeded contacts:", [(c["displayName"], c["id"][:8]) for c in config["contacts"]])
    print(shell(f"run-as {PKG} ls -la files files/media/photo files/media/voice"))


if __name__ == "__main__":
    main()