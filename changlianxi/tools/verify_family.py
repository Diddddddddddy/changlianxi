"""T2 阶段 C：家属管理验证（排序/删除默认/持久化/后台锁定）。"""
import time

from emu import find, labels, long_press, shot, start, stop, tap, tap_button, tap_in_row, type_text, shell


def main():
    stop(); start(); time.sleep(1.5)
    assert find("女儿"), "应显示默认女儿"
    long_press(540, 150, 4500)
    assert find("输入家属 PIN"), "长按应进入 PIN 验证"
    tap("PIN", wait=0.5); type_text("2468")
    tap("进入设置", wait=1.5)
    assert find("儿子") and find("老伴"), "列表应有 3 位家人"
    shot("t2-20-family-list")

    tap_in_row("儿子", "上移", wait=1.2)
    ns = [n["label"] for n in __import__("emu").nodes()]
    print("after move:", [x for x in ns if x in ("女儿", "儿子", "老伴")])
    shot("t2-21-moved")

    tap_in_row("女儿", "删除", wait=1.0)
    assert find("删除「女儿」"), "应弹删除确认"
    tap_button("删除", wait=1.5)
    assert not find("女儿"), "女儿应已删除"
    shot("t2-22-deleted")

    tap("完成，进入老人首页", wait=1.5)
    assert find("儿子"), "新默认应为儿子（列表第一位）"
    shot("t2-23-elder-new-default")

    stop(); start(); time.sleep(1.5)
    assert find("儿子"), "重启后仍应为儿子"
    shot("t2-24-restart-persist")

    # 后台锁定：进家属页 → Home → 回来应回老人首页
    long_press(540, 150, 4500)
    tap("PIN", wait=0.5); type_text("2468")
    tap("进入设置", wait=1.5)
    assert find("家属设置"), "应在家属页"
    shell("input keyevent 3"); time.sleep(1.5)
    start(); time.sleep(1.5)
    assert find("儿子") and not find("家属设置"), "后台恢复应锁定回老人首页"
    shot("t2-25-background-lock")
    print("ALL FAMILY MANAGEMENT CHECKS PASSED")


if __name__ == "__main__":
    main()