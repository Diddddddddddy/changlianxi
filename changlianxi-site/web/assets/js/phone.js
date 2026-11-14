// 老人端手机模拟器：状态机与手势、单实例音频。契约见 PRD/SPEC 与 Sites 设计文档。
import { ROLES, contactDisplay } from './store.js';

const VIDEO_ICON = '<svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M17 10.5V7c0-.55-.45-1-1-1H4c-.55 0-1 .45-1 1v10c0 .55.45 1 1 1h12c.55 0 1-.45 1-1v-3.5l4 4v-11l-4 4z"/></svg>';

export class PhoneDemo {
  constructor(root, config, { onAudioChange } = {}) {
    this.config = config;
    this.onAudioChange = onAudioChange;
    this.state = 'idle';
    this.lockedContactId = null;
    this.audioEnabled = false;
    this.audioToken = 0;
    this.audio = new Audio();
    this.audio.preload = 'none';
    this.settleTimer = null;
    this.currentId = config.defaultId;

    this.root = document.createElement('div');
    this.root.className = 'phone';
    this.root.innerHTML = `
      <div class="phone-screen">
        <div class="phone-statusbar"><span>常联系</span><span class="demo-badge">模拟演示</span></div>
        <div class="pager-viewport" tabindex="0" role="group" aria-label="家人照片，上下滑动换人，回车拨打">
          <div class="pager-track"></div>
        </div>
        <button class="big-call" type="button" hidden></button>
        <button class="audio-hint" type="button" hidden>点这里开启声音</button>
        <div class="overlay-root"></div>
      </div>`;
    root.replaceChildren(this.root);
    this.viewport = this.root.querySelector('.pager-viewport');
    this.track = this.root.querySelector('.pager-track');
    this.bigCall = this.root.querySelector('.big-call');
    this.audioHint = this.root.querySelector('.audio-hint');
    this.overlayRoot = this.root.querySelector('.overlay-root');

    this.track.addEventListener('transitionend', () => this.onSettled());
    this.viewport.addEventListener('pointerdown', (e) => this.onPointerDown(e));
    this.viewport.addEventListener('pointermove', (e) => this.onPointerMove(e));
    this.viewport.addEventListener('pointerup', (e) => this.onPointerUp(e));
    this.viewport.addEventListener('pointercancel', () => this.onPointerCancel());
    this.viewport.addEventListener('wheel', (e) => this.onWheel(e), { passive: false });
    this.viewport.addEventListener('keydown', (e) => this.onKey(e));
    this.bigCall.addEventListener('click', () => this.dial());
    this.audioHint.addEventListener('click', () => this.enableAudio());
    document.addEventListener('visibilitychange', () => { if (document.hidden) this.stopAudio(); });

    this.renderPages();
  }

  get contacts() { return this.config.contacts; }
  get currentIndex() { return Math.max(0, this.contacts.findIndex((c) => c.id === this.currentId)); }
  get current() { return this.contacts[this.currentIndex]; }
  // 循环衔接：首尾各放一张克隆页，滑过边界后无动画吸附回真实页，实现首末互wrap。
  get loop() { return this.contacts.length > 1; }
  get offset() { return this.loop ? 1 : 0; }
  get trackCount() { return this.contacts.length + (this.loop ? 2 : 0); }

  updateConfig(config) {
    const keepId = this.currentId;
    this.config = config;
    this.currentId = config.contacts.some((c) => c.id === keepId) ? keepId : config.defaultId;
    this.closeOverlay();
    this.state = 'idle';
    this.renderPages();
  }

  renderPages() {
    this.stopAudio();
    clearTimeout(this.settleTimer);
    this.track.classList.add('dragging');
    this.track.innerHTML = '';
    const n = this.contacts.length;
    const list = this.loop ? [this.contacts[n - 1], ...this.contacts, this.contacts[0]] : this.contacts;
    for (let i = 0; i < list.length; i++) {
      const c = list[i];
      const isClone = this.loop && (i === 0 || i === list.length - 1);
      const page = document.createElement('section');
      page.className = 'page' + (this.config.dialMode === 'tap' ? ' tappable' : '');
      page.dataset.id = c.id;
      if (isClone) page.setAttribute('aria-hidden', 'true');
      else page.setAttribute('role', this.config.dialMode === 'tap' ? 'button' : 'img');
      page.setAttribute('aria-label', this.config.dialMode === 'tap' ? `给${contactDisplay(c)}打视频` : `${contactDisplay(c)}的照片`);
      page.tabIndex = -1;
      page.innerHTML = `
        <img src="${ROLES[c.role].photo}" alt="${contactDisplay(c)}的照片（虚构演示人物）" draggable="false">
        <div class="page-caption">
          <div class="page-name">${contactDisplay(c)}</div>
          ${this.config.dialMode === 'tap' ? `<div class="page-hint">${VIDEO_ICON}<span>点照片，视频联系</span></div>` : ''}
        </div>`;
      this.track.appendChild(page);
    }
    this.pos = this.currentIndex + this.offset;
    this.bigCall.hidden = this.config.dialMode !== 'button';
    this.bigCall.textContent = `给${contactDisplay(this.current)}打视频`;
    this.applyTransform(false);
    requestAnimationFrame(() => this.track.classList.remove('dragging'));
  }

  applyTransform(animate = true, extraOffset = 0) {
    if (!animate) this.track.classList.add('dragging');
    this.track.style.transform = `translateY(calc(${-this.pos * 100}% + ${extraOffset}px))`;
    if (!animate) requestAnimationFrame(() => this.track.classList.remove('dragging'));
  }

  // 目标为轨道位置（含首尾克隆页）；落到克隆页后在 onSettled 无动画吸附回真实页。
  goToPos(target) {
    const pos = Math.max(0, Math.min(this.trackCount - 1, target));
    if (pos === this.pos) { this.applyTransform(); return; }
    this.stopAudio();
    clearTimeout(this.settleTimer);
    this.pos = pos;
    const n = this.contacts.length;
    const idx = this.loop ? (pos === 0 ? n - 1 : pos === n + 1 ? 0 : pos - 1) : pos;
    this.currentId = this.contacts[idx].id;
    this.bigCall.textContent = `给${contactDisplay(this.current)}打视频`;
    this.applyTransform();
    // transitionend → onSettled；无动画时（reduced motion）手动触发
    if (getComputedStyle(this.track).transitionDuration === '0s') this.onSettled();
  }

  step(dir) { this.goToPos(this.pos + dir); }

  onSettled() {
    // 首尾克隆页吸附回真实页（无动画），保证继续滑动方向正确
    if (this.loop && (this.pos === 0 || this.pos === this.trackCount - 1)) {
      this.pos = this.pos === 0 ? this.contacts.length : 1;
      this.applyTransform(false);
    }
    if (this.state !== 'idle') return;
    this.bigCall.textContent = `给${contactDisplay(this.current)}打视频`;
    if (this.currentId === this.lastSettledId) return;
    this.lastSettledId = this.currentId;
    clearTimeout(this.settleTimer);
    this.settleTimer = setTimeout(() => this.playVoice(), 300);
  }

  // 抖音式手感：动画进行中可随时抓住（从当前视觉位置继续，不跳变）
  readVisualY() {
    const t = getComputedStyle(this.track).transform;
    if (!t || t === 'none') return -this.pos * this.viewport.clientHeight;
    return new DOMMatrixReadOnly(t).m42;
  }

  // iOS 橡皮筋公式：渐进阻尼，越拖越重，而不是线性打折
  rubberBand(x, h) {
    return Math.sign(x) * (1 - 1 / (Math.abs(x) * 0.55 / h + 1)) * h;
  }

  dragY(dy) {
    const h = this.viewport.clientHeight;
    const minY = -(this.trackCount - 1) * h;
    let y = this.drag.startVisualY + dy;
    if (y > 0) y = this.rubberBand(y, h);
    else if (y < minY) y = minY + this.rubberBand(y - minY, h);
    return y;
  }

  // 近 100ms 采样的瞬时速度：停顿后松手不会被整段平均速度误判成轻扫
  recentVelocity() {
    const s = this.drag.samples;
    if (s.length < 2) return 0;
    const dt = s[s.length - 1].t - s[0].t;
    return dt > 0 ? (s[s.length - 1].y - s[0].y) / dt : 0;
  }

  onPointerDown(e) {
    this.enableAudio();
    if (this.state !== 'idle') return;
    const now = performance.now();
    const visualY = this.readVisualY();
    this.drag = {
      startX: e.clientX, startY: e.clientY, time: now,
      startVisualY: visualY,
      samples: [{ t: now, y: e.clientY }],
    };
    this.dragged = false;
    this.lastSettledId = null;
    // 先把视觉位置钉住再关掉过渡，否则 transition:none 会让画面跳到动画终点
    this.track.style.transform = `translateY(${visualY}px)`;
    this.track.classList.add('dragging');
    this.viewport.setPointerCapture(e.pointerId);
  }

  onPointerMove(e) {
    if (!this.drag || this.state !== 'idle') return;
    const dy = e.clientY - this.drag.startY;
    if (!this.dragged && Math.abs(dy) > 6) {
      this.dragged = true;
      this.stopAudio();
    }
    if (!this.dragged) return;
    // 1:1 跟手，直接写像素位移
    this.track.style.transform = `translateY(${this.dragY(dy)}px)`;
    const now = performance.now();
    this.drag.samples.push({ t: now, y: e.clientY });
    while (this.drag.samples.length > 2 && now - this.drag.samples[0].t > 100) this.drag.samples.shift();
  }

  onPointerUp(e) {
    if (!this.drag) return;
    const { startY, startX, time } = this.drag;
    const dy = e.clientY - startY;
    const dx = e.clientX - startX;
    const dt = Math.max(1, performance.now() - time);
    const now = performance.now();
    this.drag.samples.push({ t: now, y: e.clientY });
    while (this.drag.samples.length > 2 && now - this.drag.samples[0].t > 100) this.drag.samples.shift();
    const velocity = this.recentVelocity();
    const visualY = this.dragY(dy);
    this.drag = null;
    this.track.classList.remove('dragging');
    // 轻点（位移小、时间短）= 点照片拨打；真实指针下 click 被 setPointerCapture 重定向，故在此判定
    if (!this.dragged && Math.abs(dx) < 12 && Math.abs(dy) < 12 && dt < 600) {
      this.applyTransform();
      this.dragged = false;
      if (this.config.dialMode === 'tap') this.dial(this.current.id);
      return;
    }
    if (this.dragged) {
      // 抖音判定：快扫（>350px/s）或拖过 1/4 页至少翻一页；否则按位置 + 150ms 惯性外推吸附最近页
      const h = this.viewport.clientHeight;
      const delta = visualY + this.pos * h;
      let target = Math.round(-(visualY + velocity * 150) / h);
      if (Math.abs(velocity) > 0.35 || Math.abs(delta) > h * 0.25) {
        const dir = (Math.abs(velocity) > 0.35 ? velocity : delta) < 0 ? 1 : -1;
        target = dir > 0 ? Math.max(target, this.pos + 1) : Math.min(target, this.pos - 1);
      }
      this.goToPos(target);
      // 拖动抬起的那次点击不应触发拨打
      setTimeout(() => { this.dragged = false; }, 80);
    } else {
      this.applyTransform();
    }
  }

  onPointerCancel() {
    if (!this.drag) return;
    this.drag = null;
    this.track.classList.remove('dragging');
    this.applyTransform();
    setTimeout(() => { this.dragged = false; }, 80);
  }

  onWheel(e) {
    if (this.state !== 'idle') return;
    e.preventDefault();
    this.enableAudio();
    const now = performance.now();
    if (now - (this.lastWheel || 0) < 480) return;
    if (Math.abs(e.deltaY) < 12) return;
    this.lastWheel = now;
    this.step(e.deltaY > 0 ? 1 : -1);
  }

  onKey(e) {
    if (this.state !== 'idle') return;
    if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
      e.preventDefault();
      this.enableAudio();
      this.step(e.key === 'ArrowDown' ? 1 : -1);
    } else if (e.key === 'Enter') {
      e.preventDefault();
      this.enableAudio();
      this.dial();
    }
  }

  // 音频：单实例 + token，旧任务一律作废
  enableAudio() {
    if (this.audioEnabled) return;
    this.audioEnabled = true;
    this.audioHint.hidden = true;
    this.onAudioChange?.(true);
    this.playVoice();
  }

  playVoice() {
    if (!this.audioEnabled || this.state !== 'idle') return;
    this.play(ROLES[this.current.role].voice);
  }

  play(src) {
    if (!this.audioEnabled) return;
    const token = ++this.audioToken;
    this.audio.pause();
    this.audio.src = src;
    this.audio.currentTime = 0;
    this.audio.play().catch(() => { if (token === this.audioToken) this.onAudioChange?.('blocked'); });
  }

  stopAudio() {
    ++this.audioToken;
    clearTimeout(this.settleTimer);
    this.audio.pause();
    this.audio.currentTime = 0;
  }

  dial(id) {
    if (this.state !== 'idle') return;
    const contact = id ? this.contacts.find((c) => c.id === id) : this.current;
    if (!contact) return;
    this.enableAudio();
    this.stopAudio();
    this.state = 'ringing';
    this.lockedContactId = contact.id;
    this.showRinging(contact);
    this.play(`assets/audio/call-${contact.role}.mp3`);
  }

  // 与 App CallStateMachine 一致：响铃页内由演示控件选择结果，不自动定时接通；过期状态拒绝。
  demoAnswer() {
    const contact = this.contacts.find((c) => c.id === this.lockedContactId);
    if (!contact || this.state !== 'ringing') return;
    this.stopAudio();
    this.state = 'connected';
    this.showConnected(contact);
  }

  demoNoAnswer() {
    const contact = this.contacts.find((c) => c.id === this.lockedContactId);
    if (!contact || this.state !== 'ringing') return;
    this.showFailed(contact, 'noanswer');
  }

  demoNetworkFail() {
    const contact = this.contacts.find((c) => c.id === this.lockedContactId);
    if (!contact || this.state !== 'ringing') return;
    this.showFailed(contact, 'nonetwork');
  }

  showRinging(c) {
    this.overlayRoot.innerHTML = `
      <div class="overlay" role="alert">
        <div class="overlay-photo"><img src="${ROLES[c.role].photo}" alt=""></div>
        <div class="overlay-status">
          <div class="overlay-title">等待对方接听</div>
          <div class="overlay-sub">${contactDisplay(c)} · 演示呼叫，非真实通话</div>
        </div>
        <div class="overlay-demo">
          <div class="overlay-demo-label">演示控制（评委工具，非老人界面）</div>
          <div class="overlay-demo-row">
            <button class="btn btn-outline btn-small" id="demo-answer" type="button">模拟对方接听</button>
            <button class="btn btn-outline btn-small" id="demo-noanswer" type="button">模拟无人接听</button>
          </div>
          <button class="btn btn-outline btn-small overlay-demo-wide" id="demo-nonetwork" type="button">模拟断网失败</button>
        </div>
        <div class="overlay-actions"><button class="btn btn-danger" id="cancel-call" type="button">取消</button></div>
      </div>`;
    this.overlayRoot.querySelector('#demo-answer').addEventListener('click', () => this.demoAnswer());
    this.overlayRoot.querySelector('#demo-noanswer').addEventListener('click', () => this.demoNoAnswer());
    this.overlayRoot.querySelector('#demo-nonetwork').addEventListener('click', () => this.demoNetworkFail());
    this.overlayRoot.querySelector('#cancel-call').addEventListener('click', () => this.closeOverlay());
  }

  showConnected(c) {
    this.overlayRoot.innerHTML = `
      <div class="overlay">
        <div class="overlay-photo"><img src="${ROLES[c.role].photo}" alt=""></div>
        <div class="overlay-status"><div class="overlay-title">模拟已接通</div>
        <div class="overlay-sub">${contactDisplay(c)} · 演示画面，非真实通话</div></div>
        <button class="overlay-hangup" id="hangup" type="button">挂断</button>
      </div>`;
    this.overlayRoot.querySelector('#hangup').addEventListener('click', () => {
      this.play(`assets/audio/ended-${c.role}.mp3`);
      this.closeOverlay({ keepAudio: true });
    });
  }

  showFailed(c, reason) {
    this.state = 'failed';
    const title = reason === 'nonetwork' ? '手机没联网' : '电话没有打出去';
    this.overlayRoot.innerHTML = `
      <div class="overlay" role="alert">
        <div class="overlay-photo"><img src="${ROLES[c.role].photo}" alt=""></div>
        <div class="overlay-status">
          <div class="overlay-title">${title}</div>
          <div class="overlay-sub">演示失败提示，未真实拨出</div>
        </div>
        <div class="overlay-actions">
          <button class="btn btn-primary" id="back" type="button">返回</button>
        </div>
      </div>`;
    this.play(`assets/audio/${reason}-${c.role}.mp3`);
    this.overlayRoot.querySelector('#back').addEventListener('click', () => this.closeOverlay());
  }

  simulateIncoming() {
    if (this.state !== 'idle') return;
    const c = this.current;
    this.enableAudio();
    this.stopAudio();
    this.state = 'incoming';
    this.lockedContactId = c.id;
    this.overlayRoot.innerHTML = `
      <div class="overlay" role="alert">
        <div class="overlay-photo" id="answer-area" role="button" tabindex="0" aria-label="点照片接听"><img src="${ROLES[c.role].photo}" alt=""></div>
        <div class="overlay-status">
          <div class="overlay-title">${contactDisplay(c)}给你打视频电话啦</div>
          <div class="overlay-sub incoming-answer-hint">点一下照片接听</div>
        </div>
        <button class="overlay-hangup" id="reject" type="button">拒接</button>
      </div>`;
    this.play(`assets/audio/incoming-${c.role}.mp3`);
    const answer = () => { this.stopAudio(); this.state = 'connected'; this.showConnected(c); };
    this.overlayRoot.querySelector('#answer-area').addEventListener('click', answer);
    this.overlayRoot.querySelector('#answer-area').addEventListener('keydown', (e) => { if (e.key === 'Enter') answer(); });
    this.overlayRoot.querySelector('#reject').addEventListener('click', () => this.closeOverlay());
  }

  closeOverlay({ keepAudio = false } = {}) {
    if (!keepAudio) this.stopAudio();
    this.overlayRoot.innerHTML = '';
    this.state = 'idle';
    this.lockedContactId = null;
    if (!this.audioEnabled) this.audioHint.hidden = false;
  }
}
