"""T5：演示呼叫状态机分支验证（拨号/接听/挂断/无人接听/断网/来电接听/拒接/防双击/演示标识）。"""
import time

from emu import find, long_press, shot, shell, start, stop, tap, tap_at, tap_button, type_text, wait_for


def enter_family():
    long_press(540, 150, 4500)
    assert wait_for("输入家属 PIN"), "长按应进入 PIN"
    tap("PIN", wait=0.5); type_text("2468")
    tap("进入设置", wait=1.5)
    assert wait_for("家属设置"), "应在家属页"


def dial_current():
    tap_at(540, 1100, wait=1.5)  # TAP_PHOTO 模式点照片
    assert wait_for("模拟演示"), "应进入演示呼叫"


def main():
    stop(); start()
    assert wait_for("女儿", timeout=20), "冷启动默认女儿"
    time.sleep(1.0)

    # 1) 拨号 → 响铃 → 接听 → 挂断（AC-11 演示标识）
    dial_current()
    assert wait_for("等待对方接听"), "应到响铃态"
    assert find("模拟演示"), "AC-11 演示标识"
    shot("t5-01-ringing")
    assert tap_button("模拟对方接听", wait=1.2), "演示接听控件"
    assert wait_for("模拟已接通"), "应到接通态"
    assert find("模拟演示"), "接通仍标演示"
    shot("t5-02-connected")
    assert tap_button("挂断", wait=1.2), "挂断"
    assert wait_for("女儿") and not find("模拟演示"), "挂断回原联系人"
    print("outgoing answer+hangup ok")

    # 2) 双击只一次呼叫（AC-09）
    shell("input tap 540 1100; sleep 0.12; input tap 540 1100")
    time.sleep(1.5)
    assert find("模拟演示"), "双击应进入呼叫"
    assert tap_button("取消", wait=1.2), "取消"
    assert wait_for("女儿"), "取消一次即返回"
    print("double-click single call ok")

    # 3) 无人接听 → 失败 → 返回原联系人（AC-17）
    dial_current()
    assert wait_for("等待对方接听")
    assert tap_button("模拟无人接听", wait=1.0)
    assert wait_for("电话没有打出去"), "应显示失败"
    assert find("模拟演示"), "失败仍标演示"
    shot("t5-03-failed")
    assert tap_button("返回", wait=1.2), "失败返回"
    assert wait_for("女儿") and not find("模拟演示"), "返回原联系人"
    print("no-answer fallback ok")

    # 4) 断网失败 → 返回原联系人（AC-17）
    dial_current()
    assert wait_for("等待对方接听")
    assert tap_button("模拟断网失败", wait=1.0)
    assert wait_for("电话没有打出去"), "断网失败提示"
    assert tap_button("返回", wait=1.2)
    assert wait_for("女儿"), "断网应回原联系人"
    print("network fail fallback ok")

    # 5) 响铃取消 → 回原联系人
    dial_current()
    assert wait_for("等待对方接听")
    assert tap_button("取消", wait=1.2)
    assert wait_for("女儿") and not find("模拟演示"), "取消回原联系人"
    print("ringing cancel ok")

    # 6) 模拟来电 → 点照片接听 → 挂断（AC-12/AC-13）
    enter_family()
    assert tap_button("模拟来电（演示）", wait=1.5), "来电演示控件"
    assert wait_for("来电"), "应到来电态"
    assert find("点击照片接听"), "来电提示"
    assert find("模拟演示"), "来电标演示"
    shot("t5-04-incoming")
    tap_at(540, 1200, wait=1.2)  # 点照片接听
    assert wait_for("模拟已接通"), "接听应到接通态"
    assert tap_button("挂断", wait=1.2)
    assert wait_for("女儿"), "挂断回原联系人"
    print("incoming answer+hangup ok")

    # 7) 模拟来电 → 拒接 → 回原联系人（AC-12）
    enter_family()
    assert tap_button("模拟来电（演示）", wait=1.5)
    assert wait_for("来电")
    assert tap_button("拒接", wait=1.2), "拒接"
    assert wait_for("女儿") and not find("模拟演示"), "拒接回原联系人"
    shot("t5-05-declined")
    print("incoming decline ok")

    print("ALL CALL CHECKS PASSED")


if __name__ == "__main__":
    main()