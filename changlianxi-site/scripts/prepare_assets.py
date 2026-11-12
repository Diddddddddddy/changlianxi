"""生成演示素材：朴实中国家人肖像转 WebP + edge-tts 语音（使用者=奶奶）。一次性脚本。

运行：C:/env/anaconda/envs/changlianxi/python.exe scripts/prepare_assets.py
"""
import asyncio
from pathlib import Path
from PIL import Image
import edge_tts

ROOT = Path(__file__).resolve().parent.parent
WEB = ROOT / "web" / "assets"
VIBE = ROOT.parent / "vibe_images"

# 六位预置虚构家人（核心三位 + 可添加三位），照片为 AI 生成的朴实中国人肖像。
PHOTOS = {
    "daughter": "family-daughter_1794406387985_4bc4ff45.png",
    "grandson": "family-grandson_1794406407374_756d4a19.png",
    "spouse": "family-spouse_1794406428102_ab1452bd.png",
    "granddaughter": "family-granddaughter-v2_1794406590536_c5acda31.png",
    "son": "family-son_1794406506592_4ff543eb.png",
    "daughterinlaw": "family-daughterinlaw_1794406525953_43dad3d9.png",
}

# 各角色音色（匹配年龄/性别）。家人本人录音、来电与系统提示都用同一音色，
# 使整通电话从头到尾都是「这位家人」的声音。
ROLE_VOICE = {
    "daughter": ("zh-CN-XiaoxiaoNeural", "+0%", "+0Hz"),        # 女儿 · 小芳（45 岁女）
    "grandson": ("zh-CN-YunxiNeural", "+0%", "+0Hz"),           # 孙子 · 小军（24 岁男）
    "spouse": ("zh-CN-YunjianNeural", "-12%", "-20Hz"),         # 老伴 · 爷爷（68 岁男，放慢降调显年长）
    "granddaughter": ("zh-CN-XiaoyiNeural", "+0%", "+0Hz"),     # 孙女 · 小雅（22 岁女）
    "son": ("zh-CN-YunyangNeural", "+0%", "+0Hz"),              # 儿子 · 小强（48 岁男）
    "daughterinlaw": ("zh-CN-XiaoxiaoNeural", "-3%", "-3Hz"),   # 儿媳 · 小丽（45 岁女，略沉缓）
}

# 家人本人录音 + 来电（称呼使用者为奶奶；女儿/儿子/儿媳称妈）。
VOICES = [
    ("voice-daughter", "妈，我是小芳。想我了，就点一下我的照片，咱们就能见面说话。", "daughter"),
    ("incoming-daughter", "妈，我是小芳，给您打视频啦，点一下照片接听。", "daughter"),
    ("voice-grandson", "奶奶，我是小军。想我了就点我的照片，我马上来陪您说话。", "grandson"),
    ("incoming-grandson", "奶奶，我是小军，给您打视频啦，点一下照片接听。", "grandson"),
    ("voice-spouse", "老伴儿，是我。想我了就点一下照片，咱老两口说说话。", "spouse"),
    ("incoming-spouse", "老伴儿，是我，给你打视频啦，点一下照片接听。", "spouse"),
    ("voice-granddaughter", "奶奶，我是小雅。想我了就点我的照片，我跟您视频聊天。", "granddaughter"),
    ("incoming-granddaughter", "奶奶，我是小雅，给您打视频啦，点一下照片接听。", "granddaughter"),
    ("voice-son", "妈，我是小强。想我了就点一下照片，咱们视频说说话。", "son"),
    ("incoming-son", "妈，我是小强，给您打视频啦，点一下照片接听。", "son"),
    ("voice-daughterinlaw", "妈，我是小丽。想我们了就点一下照片，全家都惦记您。", "daughterinlaw"),
    ("incoming-daughterinlaw", "妈，我是小丽，给您打视频啦，点一下照片接听。", "daughterinlaw"),
]

# 系统提示：文案对齐 App StatusPhrases，按所呼叫家人的音色逐条播报（不再用通用旁白）。
SYSTEM_PROMPTS = {
    "call": "正在帮你打电话，请稍等。",
    "noanswer": "电话没有打出去，过一会儿再试试。",
    "nonetwork": "手机没联网，暂时打不了电话。",
    "notready": "这个电话还没设置好，请让家里人帮忙。",
    "ended": "电话挂断了。",
}

# 旧版素材：早期按角色拆分的拨号/无人接听，以及通用旁白版系统提示，均已废弃。
OBSOLETE = [
    "call-daughter", "call-son", "call-spouse",
    "noanswer-daughter", "noanswer-son", "noanswer-spouse",
    "call", "noanswer", "nonetwork", "notready", "ended",
]


def prepare_photos():
    out = WEB / "photos"
    out.mkdir(parents=True, exist_ok=True)
    for name, src in PHOTOS.items():
        img = Image.open(VIBE / src).convert("RGB")
        img.thumbnail((800, 1200), Image.LANCZOS)
        dst = out / f"{name}.webp"
        img.save(dst, "WEBP", quality=82, method=6)
        print(f"photo {name}: {img.size[0]}x{img.size[1]} {dst.stat().st_size // 1024} KB")


async def synth(out, name, text, voice, rate, pitch, skip_existing=False):
    dst = out / f"{name}.mp3"
    if skip_existing and dst.exists() and dst.stat().st_size > 0:
        print(f"skip {name} (exists)")
        return
    for attempt in range(3):
        try:
            await edge_tts.Communicate(text, voice, rate=rate, pitch=pitch).save(str(dst))
            print(f"audio {name}: {dst.stat().st_size // 1024} KB [{voice} {rate} {pitch}]")
            return
        except Exception as e:
            if attempt == 2:
                print(f"FAILED {name}: {e}")
            else:
                await asyncio.sleep(2)


async def prepare_audio():
    out = WEB / "audio"
    out.mkdir(parents=True, exist_ok=True)
    for name in OBSOLETE:
        f = out / f"{name}.mp3"
        if f.exists():
            f.unlink()
            print(f"removed obsolete {name}")
    # 家人本人录音与来电：内容未变，已存在则跳过。
    for name, text, role in VOICES:
        voice, rate, pitch = ROLE_VOICE[role]
        await synth(out, name, text, voice, rate, pitch, skip_existing=True)
    # 系统提示：逐条 × 逐角色，用对应家人音色生成。
    for prompt, text in SYSTEM_PROMPTS.items():
        for role, (voice, rate, pitch) in ROLE_VOICE.items():
            await synth(out, f"{prompt}-{role}", text, voice, rate, pitch)


if __name__ == "__main__":
    prepare_photos()
    asyncio.run(prepare_audio())