"""T2 收尾：MediaRecorder 录音流程实测（权限/过短/录制/试听/保存/重启）。"""
import json
import subprocess
import time

from emu import ADB, PKG, find, long_press, nodes, shot, shell, start, stop, tap, tap_at, tap_button, tap_in_row, type_text, wait_for


def run_as_bytes(path):
    r = subprocess.run([ADB, "exec-out", "run-as", PKG, "cat", path], capture_output=True)
    return r.stdout


def voice_path():
    cfg = json.loads(run_as_bytes("files/config.json").decode("utf-8"))
    c = next(x for x in cfg["contacts"] if x["displayName"] == "儿子")
    return "files/" + c["voicePath"] if not c["voicePath"].startswith("files/") else c["voicePath"]


def main():
    shell("pm reset-permissions")  # 清掉 USER_FIXED，保证权限弹窗可复现
    stop(); start()
    if find("record audio"):  # 系统权限框属独立进程，force-stop 清不掉
        tap_button("Don’t allow", wait=1.0)
    assert wait_for("儿子"), "老人首页应显示儿子"
    long_press(540, 150, 4500)
    assert find("输入家属 PIN"), "长按应进入 PIN 验证"
    tap("PIN", wait=0.5); type_text("2468")
    tap("进入设置", wait=1.5)
    tap_in_row("儿子", "编辑", wait=1.5)
    assert find("编辑家人"), "应进入编辑页"
    assert find("重新录音"), "已有配音应显示重新录音"

    before = run_as_bytes(voice_path())
    print(f"voice before: {len(before)} bytes, ftyp={b'ftyp' in before[:16]}")

    # 1) 拒绝权限：不应开始录音
    btn = find("重新录音")
    tap_at(btn["x"], btn["y"], wait=1.5)
    assert find("record audio") or find("不允许"), "应弹麦克风权限框"
    shot("t2-26-mic-permission")
    tap_button("Don’t allow", wait=1.0)
    assert find("重新录音") and not find("停止录音"), "拒绝后不应录音"

    # 2) 允许后立即停止：<800ms 应被拒绝且保留旧录音
    # 预授权消除权限框动画干扰，单条 shell 精确控制 0.4s 间隔
    shell(f"pm grant {PKG} android.permission.RECORD_AUDIO")
    btn = find("重新录音")
    shell(f"input tap {btn['x']} {btn['y']}; sleep 0.4; input tap {btn['x']} {btn['y']}")
    time.sleep(1.0)
    assert find("录音太短"), f"过短应提示，实际: {[n['label'] for n in nodes() if n['label']][:12]}"
    assert find("试听"), "过短拒绝后旧录音仍在"
    shot("t2-27-too-short")

    # 3) 正常录制 3 秒
    tap("重新录音", wait=1.0)
    assert find("停止录音"), "应进入录音状态"
    time.sleep(3.0)
    shot("t2-28-recording")
    tap("停止录音", wait=1.0)
    assert not find("停止录音") and find("试听"), "停止后应可试听"

    # 4) 试听新录音
    tap("试听", wait=1.0)
    assert find("停止"), "试听中应显示停止"
    time.sleep(2.0)
    shot("t2-29-preview")
    tap_button("停止", wait=0.8)
    assert find("试听"), "停止试听后应回到试听"

    # 5) 保存并校验文件真的是 AAC 录音
    tap_button("保存", wait=1.5)
    assert find("家属设置"), "保存后应回列表"
    after = run_as_bytes(voice_path())
    print(f"voice after: {len(after)} bytes, ftyp={b'ftyp' in after[:16]}")
    assert b"ftyp" in after[:16] and len(after) > 10000, "保存的应为新录的 m4a"
    assert after != before, "录音应已替换旧文件"
    shot("t2-30-saved")

    # 6) 重启后录音仍在且可试听
    stop(); start()
    assert wait_for("儿子"), "重启应回老人首页"
    long_press(540, 150, 4500)
    assert wait_for("输入家属 PIN"), "长按应进入 PIN 验证"
    tap("PIN", wait=0.5); type_text("2468")
    tap("进入设置", wait=1.5)
    tap_in_row("儿子", "编辑", wait=1.5)
    tap("试听", wait=1.0)
    assert find("停止"), "重启后新录音应可试听"
    shot("t2-31-restart-voice")
    tap_button("停止", wait=0.5)
    tap_button("取消", wait=1.0)
    tap("完成，进入老人首页", wait=1.5)
    assert find("儿子"), "应回老人首页"
    print("ALL RECORDING CHECKS PASSED")


if __name__ == "__main__":
    main()