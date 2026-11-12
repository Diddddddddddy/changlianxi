import { ROLES, loadConfig, saveConfig, resetConfig, newId, availableRoles, contactDisplay } from './store.js';
import { PhoneDemo } from './phone.js';

let config = loadConfig();
const listEl = document.getElementById('contact-list');
const addEl = document.getElementById('add-panel');
const phone = new PhoneDemo(document.getElementById('phone-root'), config);
phone.audioHint.hidden = phone.audioEnabled;

function commit() {
  saveConfig(config);
  phone.updateConfig(config);
  renderList();
  renderAddPanel();
}

function renderList() {
  listEl.innerHTML = '';
  config.contacts.forEach((c, i) => {
    const usedByOthers = new Set(config.contacts.filter((x) => x.id !== c.id).map((x) => x.role));
    const card = document.createElement('div');
    card.className = 'contact-card';
    card.innerHTML = `
      <div class="contact-head">
        <img class="contact-thumb" src="${ROLES[c.role].photo}" alt="${ROLES[c.role].name}（虚构演示人物）">
        <input class="contact-name-input" data-field="name" value="${c.name}" aria-label="称呼" maxlength="8">
        <button class="btn btn-danger btn-small contact-delete" data-act="del" type="button" ${config.contacts.length === 1 ? 'disabled' : ''}>删除</button>
      </div>
      <div class="photo-picker" role="group" aria-label="更换人物（照片与声音一起切换，同一人物不可重复）">
        ${Object.entries(ROLES).map(([role, r]) => `
          <button class="photo-option" data-role="${role}" type="button" aria-pressed="${c.role === role}" ${usedByOthers.has(role) ? 'disabled' : ''} aria-label="使用${r.name}（${r.desc}）" title="${r.name} · ${r.desc}">
            <img src="${r.photo}" alt="">
          </button>`).join('')}
      </div>
      <div class="contact-controls">
        <label><input type="radio" name="default-contact" value="${c.id}" ${config.defaultId === c.id ? 'checked' : ''}> 默认（打开先看TA）</label>
        <button class="btn btn-outline btn-small" data-act="up" type="button" ${i === 0 ? 'disabled' : ''}>上移</button>
        <button class="btn btn-outline btn-small" data-act="down" type="button" ${i === config.contacts.length - 1 ? 'disabled' : ''}>下移</button>
        <button class="btn btn-outline btn-small" data-act="listen" type="button">试听声音</button>
      </div>`;

    card.querySelector('[data-field="name"]').addEventListener('change', (e) => {
      c.name = e.target.value.trim() || ROLES[c.role].name;
      commit();
    });
    for (const btn of card.querySelectorAll('.photo-option')) {
      btn.addEventListener('click', () => {
        if (btn.disabled) return;
        c.role = btn.dataset.role;
        commit();
      });
    }
    card.querySelector('input[name="default-contact"]').addEventListener('change', () => {
      config.defaultId = c.id;
      commit();
    });
    card.querySelector('[data-act="up"]').addEventListener('click', () => {
      [config.contacts[i - 1], config.contacts[i]] = [config.contacts[i], config.contacts[i - 1]];
      commit();
    });
    card.querySelector('[data-act="down"]').addEventListener('click', () => {
      [config.contacts[i + 1], config.contacts[i]] = [config.contacts[i], config.contacts[i + 1]];
      commit();
    });
    card.querySelector('[data-act="listen"]').addEventListener('click', () => {
      phone.enableAudio();
      phone.play(ROLES[c.role].voice);
    });
    card.querySelector('[data-act="del"]').addEventListener('click', () => {
      if (config.contacts.length === 1) return;
      config.contacts = config.contacts.filter((x) => x.id !== c.id);
      if (config.defaultId === c.id) config.defaultId = config.contacts[0].id;
      commit();
    });
    listEl.appendChild(card);
  });
}

function renderAddPanel() {
  const avail = availableRoles(config);
  if (avail.length === 0) {
    addEl.innerHTML = '<p class="add-empty">已添加全部预置家人。真实产品里家属可拍摄/导入照片并录制本人声音，继续添加更多家人。</p>';
    return;
  }
  addEl.innerHTML = `
    <div class="add-grid">
      ${avail.map((role) => {
        const r = ROLES[role];
        return `
        <div class="add-card">
          <img src="${r.photo}" alt="${r.name}（虚构演示人物）">
          <div class="add-meta"><strong>${r.name}</strong><span>${r.desc}</span></div>
          <button class="btn btn-primary btn-small" data-add="${role}" type="button">添加</button>
        </div>`;
      }).join('')}
    </div>`;
  for (const btn of addEl.querySelectorAll('[data-add]')) {
    btn.addEventListener('click', () => {
      const role = btn.dataset.add;
      config.contacts.push({ id: newId(), name: ROLES[role].name, role });
      commit();
    });
  }
}

for (const input of document.querySelectorAll('input[name="dialmode"]')) {
  input.checked = input.value === config.dialMode;
  input.onchange = () => { config.dialMode = input.value; commit(); };
}

document.getElementById('family-reset').addEventListener('click', () => {
  resetConfig();
  config = loadConfig();
  commit();
});

renderList();
renderAddPanel();