"""T4：停稳播放本人录音 / 快速连滑不叠音 / 退后台立即停播。以 logcat 音频焦点事件为证。"""
import time

from emu import find, nodes, shot, shell, start, stop, wait_for

NAMES = ["女儿", "儿子", "老伴"]


def swipe(up=True, ms=300, wait=1.6):
    shell(f"input swipe 540 1900 540 500 {ms}" if up else f"input swipe 540 500 540 1900 {ms}")
    time.sleep(wait)


def focus_events():
    """按时间顺序提取 VoicePlayer 的音频焦点 request/abandon 事件（TTS 的 clientId 不同，已排除）。"""
    ev = []
    for line in shell("logcat -d").splitlines():
        if "MediaFocusControl" not in line or "VoicePlayer" not in line:
            continue
        if "requestAudioFocus()" in line:
            ev.append("request")
        elif "abandonAudioFocus()" in line:
            ev.append("abandon")
    return ev


def assert_no_overlap(ev, context):
    for a, b in zip(ev, ev[1:]):
        assert not (a == "request" and b == "request"), f"{context}: 连续两次 request 即叠音 {ev}"


def cur_name():
    labels = [n["label"] for n in nodes() if n["label"]]
    return next((n for n in NAMES if n in labels), None)


def main():
    # 1) 冷启动停稳后播放本人录音（请求音频焦点），不崩溃不误拨
    stop(); shell("logcat -c"); start()
    assert wait_for("女儿", timeout=20), "冷启动应显示默认女儿"
    time.sleep(6.0)  # 覆盖慢冷启动 + 300ms 延迟 + 播放
    ev = focus_events()
    assert "request" in ev, f"停稳后应请求音频焦点播放录音，实际 {ev}"
    assert_no_overlap(ev, "冷启动")
    assert find("女儿") and not find("模拟演示"), "播放不应误拨/崩溃"
    shot("t4-01-settle-play")
    print("settle voice ok, focus events:", ev)

    # 2) 10 次快速连滑：只播最终停住页，不叠音
    shell("logcat -c")
    idx = 0
    for m in [1, 1, -1, 1, -1, -1, 1, 1, -1, 1]:
        idx = max(0, min(2, idx + m))
        swipe(up=(m == 1), ms=300, wait=0.3)
    time.sleep(3.0)  # 最终页停稳 + 300ms + 播放
    final = cur_name()
    assert final == NAMES[idx], f"快速连滑应停在 {NAMES[idx]}，实际 {final}"
    ev = focus_events()
    assert_no_overlap(ev, "快速连滑")
    assert "request" in ev, f"最终停稳页应播放录音，实际 {ev}"
    assert not find("模拟演示"), "快速连滑不应误拨"
    shot("t4-02-fast-fling")
    print(f"fast-fling ok, final={final}, focus events={ev}")

    # 3) 退后台立即停播：HOME 后放弃焦点，且不再请求播放
    swipe(up=(idx != 2), ms=300, wait=0.5)  # 滑到新联系人触发一次新播放
    time.sleep(0.5)  # 进入播放窗口（已过 300ms 延迟，录音仍在播）
    shell("logcat -c")
    shell("input keyevent KEYCODE_HOME")
    time.sleep(2.0)
    ev = focus_events()
    assert "abandon" in ev, f"退后台应放弃音频焦点，实际 {ev}"
    assert "request" not in ev, f"退后台后不应再请求播放，实际 {ev}"
    print("background stop ok, focus events:", ev)

    # 4) 回到前台恢复不崩溃（ViewModel 存活，按 id 恢复当前联系人）
    start()
    deadline = time.time() + 20
    while time.time() < deadline and cur_name() is None:
        time.sleep(0.8)
    assert cur_name() is not None, "回前台应恢复老人首页"
    assert not find("模拟演示"), "回前台不应自动拨号"
    print("ALL AUDIO CHECKS PASSED")


if __name__ == "__main__":
    main()