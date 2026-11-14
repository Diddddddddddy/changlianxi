"""T3：老人主页 VerticalPager 验证（20 次滑动/循环衔接/误拨/两种模式/单联系人/排序恢复）。"""
import time

from emu import find, long_press, nodes, shot, shell, start, stop, tap, tap_at, tap_button, tap_in_row, tap_radio_near, type_text, wait_for

NAMES = ["女儿", "儿子", "老伴"]  # seed 顺序，默认女儿


def swipe(up=True, ms=300, wait=1.6):
    # 300ms 高速度滑动：800ms 慢拖在本模拟器上常被分页器判为未达阈值而不翻页。
    if up:
        shell(f"input swipe 540 1900 540 500 {ms}")
    else:
        shell(f"input swipe 540 500 540 1900 {ms}")
    time.sleep(wait)


def goto(expected, up, frm):
    """注入一次滑动并轮询等待目标页（容忍模拟器输入延迟）；确认后才返回。"""
    swipe(up=up)
    if expected == frm:
        time.sleep(1.0)  # 边界：应保持不变
        return
    assert wait_for(NAMES[expected], timeout=12), f"滑动后应到 {NAMES[expected]}"
    time.sleep(0.6)  # 停稳后再断言，避免动画中途双页可见


def assert_page(idx, context):
    labels = [n["label"] for n in nodes()]
    assert NAMES[idx] in labels, f"{context}: 应显示 {NAMES[idx]}，实际 {labels}"
    assert "模拟演示" not in labels, f"{context}: 滑动不应误拨"
    others = [n for n in NAMES if n != NAMES[idx] and n in labels]
    assert not others, f"{context}: 不应同时出现 {others}"


def enter_family():
    long_press(540, 150, 4500)
    assert wait_for("输入家属 PIN"), "长按应进入 PIN 验证"
    tap("PIN", wait=0.5); type_text("2468")
    tap("进入设置", wait=1.5)
    assert wait_for("家属设置"), "应在家属页"


def submit_family(wait=1.5):
    """家属页提交按钮可能因内容变长落到折叠区，先滚动到可见再点。"""
    for _ in range(4):
        if find("完成，进入老人首页"):
            return tap("完成，进入老人首页", wait=wait)
        shell("input swipe 540 1800 540 700 300")
        time.sleep(0.6)
    return tap("完成，进入老人首页", wait=wait)


def main():
    stop(); start()
    assert wait_for("女儿"), "AC-01 冷启动应显示默认女儿"
    assert not find("模拟演示"), "AC-01 不应自动拨号"
    shot("t3-01-default")

    # 20 次上下滑动：循环衔接、不错位、不误拨
    moves = [1, 1, 1, -1, 1, -1, -1, 1, 1, -1, 1, -1, -1, 1, -1, 1, 1, -1, -1, 1]
    idx = 0
    for i, m in enumerate(moves):
        expected = (idx + m) % len(NAMES)
        goto(expected, up=(m == 1), frm=idx)
        assert_page(expected, f"第{i + 1}次滑动")
        idx = expected
    print("20 swipes ok, final:", NAMES[idx])
    shot("t3-02-after-20-swipes")

    # 循环衔接：首位再上滑回末位、末位再下滑回首位。
    while idx != 0:
        goto((idx + 1) % len(NAMES), up=True, frm=idx)
        idx = (idx + 1) % len(NAMES)
    goto(len(NAMES) - 1, up=False, frm=0)  # 女儿再上滑 → 老伴
    idx = len(NAMES) - 1
    assert_page(idx, "首位上滑循环到末位")
    goto(0, up=True, frm=idx)  # 老伴再下滑 → 女儿
    idx = 0
    assert_page(idx, "末位下滑循环到首位")
    shot("t3-03-wrap")

    # 快速连滑：只停最终页、单页可见、不误拨（循环下不固定目标）。
    for _ in range(3):
        swipe(up=True, ms=300, wait=0.4)
    time.sleep(2.0)
    labels = [n["label"] for n in nodes()]
    present = [n for n in NAMES if n in labels]
    assert len(present) == 1, f"快速连滑应只停一页，实际 {present}"
    assert "模拟演示" not in labels, "快速连滑不应误拨"
    idx = NAMES.index(present[0])
    shot("t3-03-fast-fling")
    # 回到老伴，供后续点照片/大按钮/排序断言沿用。
    while idx != 2:
        goto((idx + 1) % len(NAMES), up=True, frm=idx)
        idx = (idx + 1) % len(NAMES)

    # TAP_PHOTO：点照片拨号，演示标识明确（AC-07/AC-11）
    tap_at(540, 1100, wait=1.5)
    assert wait_for("模拟演示"), "点照片应进入模拟呼叫"
    assert (find("正在呼叫") or find("等待对方接听")) and find("老伴") and (find("不会真实拨出") or find("非真实通话")), "呼叫页信息应完整"
    shot("t3-04-demo-call")
    tap_button("取消", wait=1.2)
    assert wait_for("老伴") and not find("模拟演示"), "取消应回到原联系人"

    # 双击只产生一次呼叫（AC-09 可观察部分）
    shell("input tap 540 1100; sleep 0.12; input tap 540 1100")
    time.sleep(1.5)
    assert find("模拟演示"), "双击应进入呼叫"
    tap_button("取消", wait=1.2)
    assert wait_for("老伴"), "取消一次即应返回"

    # LARGE_BUTTON：点照片不拨，只有大按钮拨（AC-08）
    enter_family()
    assert tap_radio_near("独立大按钮拨打"), "应切到大按钮模式"
    submit_family()
    assert wait_for("打电话"), "大按钮模式应显示打电话按钮"
    shot("t3-05-large-button")
    tap_at(540, 900, wait=1.2)
    assert not find("模拟演示"), "AC-08 大按钮模式点照片不应拨号"
    tap("打电话", wait=1.5)
    assert wait_for("模拟演示"), "点大按钮应拨号"
    tap_button("取消", wait=1.2)
    assert wait_for("老伴"), "取消应回原联系人"

    # 排序后按 contactId 恢复当前目标（SPEC 4.2.7）
    enter_family()
    tap_in_row("老伴", "上移", wait=0.8)
    tap_in_row("老伴", "上移", wait=0.8)
    submit_family()
    assert wait_for("老伴"), "排序后仍应显示老伴"
    shot("t3-06-reorder-restore")

    # 单联系人稳定（AC-03）：删到只剩老伴
    enter_family()
    tap_in_row("女儿", "删除", wait=0.8); tap_button("删除", wait=1.0)
    tap_in_row("儿子", "删除", wait=0.8); tap_button("删除", wait=1.0)
    submit_family()
    assert wait_for("老伴"), "应只剩老伴"
    for i in range(6):
        swipe(up=(i % 2 == 0))
        assert wait_for("老伴", timeout=6), f"单联系人第{i + 1}次滑动应仍显示老伴"
        assert not find("模拟演示"), "单联系人滑动不应误拨"
    tap("打电话", wait=1.5)  # 当前为大按钮模式，点按钮拨号
    assert find("模拟演示"), "单联系人点大按钮仍可拨号"
    tap_button("取消", wait=1.0)
    shot("t3-07-single-contact")

    # 切回点照片模式，恢复 3 人播种供后续阶段
    enter_family()
    assert tap_radio_near("点照片拨打"), "应切回点照片模式"
    submit_family(wait=1.0)
    print("ALL PAGER CHECKS PASSED")


if __name__ == "__main__":
    main()