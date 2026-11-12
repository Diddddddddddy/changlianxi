// 演示配置存储：仅当前浏览器 localStorage，不代表跨设备同步。使用者设定为奶奶。
export const STORAGE_KEY = 'changlianxi.demo.config.v2';

// 六位预置虚构家人：照片为 AI 生成的朴实中国人肖像，语音为对应年龄/性别的合成声。
// 核心三位（女儿/孙子/老伴）默认在列，其余三位可在家属页添加。
export const ROLES = {
  daughter: {
    name: '女儿', shortName: '小芳', relation: '女儿', desc: '四五十岁 · 女',
    photo: 'assets/photos/daughter.webp', voice: 'assets/audio/voice-daughter.mp3',
  },
  grandson: {
    name: '孙子', shortName: '小军', relation: '孙子', desc: '二十多岁 · 男',
    photo: 'assets/photos/grandson.webp', voice: 'assets/audio/voice-grandson.mp3',
  },
  spouse: {
    name: '老伴', shortName: '爷爷', relation: '老伴', desc: '六七十岁 · 男',
    photo: 'assets/photos/spouse.webp', voice: 'assets/audio/voice-spouse.mp3',
  },
  granddaughter: {
    name: '孙女', shortName: '小雅', relation: '孙女', desc: '二十多岁 · 女',
    photo: 'assets/photos/granddaughter.webp', voice: 'assets/audio/voice-granddaughter.mp3',
  },
  son: {
    name: '儿子', shortName: '小强', relation: '儿子', desc: '四五十岁 · 男',
    photo: 'assets/photos/son.webp', voice: 'assets/audio/voice-son.mp3',
  },
  daughterinlaw: {
    name: '儿媳', shortName: '小丽', relation: '儿媳', desc: '四五十岁 · 女',
    photo: 'assets/photos/daughterinlaw.webp', voice: 'assets/audio/voice-daughterinlaw.mp3',
  },
};

export const DEFAULT_CONFIG = {
  contacts: [
    { id: 'daughter', name: '女儿', role: 'daughter' },
    { id: 'grandson', name: '孙子', role: 'grandson' },
    { id: 'spouse', name: '老伴', role: 'spouse' },
  ],
  defaultId: 'daughter',
  dialMode: 'tap',
};

export function newId() {
  return 'c-' + Math.random().toString(36).slice(2, 10);
}

export function loadConfig() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return structuredClone(DEFAULT_CONFIG);
    const cfg = JSON.parse(raw);
    if (!Array.isArray(cfg.contacts) || cfg.contacts.length === 0) return structuredClone(DEFAULT_CONFIG);
    // 仅保留合法角色，且同一人物不重复出现
    const seen = new Set();
    cfg.contacts = cfg.contacts.filter((c) => {
      if (!c || !ROLES[c.role] || seen.has(c.role)) return false;
      seen.add(c.role);
      return true;
    });
    if (cfg.contacts.length === 0) return structuredClone(DEFAULT_CONFIG);
    if (!cfg.contacts.some((c) => c.id === cfg.defaultId)) cfg.defaultId = cfg.contacts[0].id;
    if (!['tap', 'button'].includes(cfg.dialMode)) cfg.dialMode = 'tap';
    return cfg;
  } catch {
    return structuredClone(DEFAULT_CONFIG);
  }
}

export function saveConfig(cfg) {
  try { localStorage.setItem(STORAGE_KEY, JSON.stringify(cfg)); } catch { /* 隐私模式下仅内存态 */ }
}

export function resetConfig() {
  try { localStorage.removeItem(STORAGE_KEY); } catch { /* 忽略 */ }
}

export function contactDisplay(c) {
  return c.name || ROLES[c.role].name;
}

// 尚未加入通讯录的预置人物（用于「添加家人」）
export function availableRoles(cfg) {
  const used = new Set(cfg.contacts.map((c) => c.role));
  return Object.keys(ROLES).filter((r) => !used.has(r));
}