import { loadConfig, saveConfig, resetConfig } from './store.js';
import { PhoneDemo } from './phone.js';

let config = loadConfig();
const audioStatus = document.getElementById('audio-status');
const enableBtn = document.getElementById('enable-audio');

const phone = new PhoneDemo(document.getElementById('phone-root'), config, {
  onAudioChange(state) {
    if (state === true) {
      enableBtn.textContent = '声音已开启';
      enableBtn.disabled = true;
      audioStatus.textContent = '';
    } else if (state === 'blocked') {
      audioStatus.textContent = '浏览器拦住了自动播放，请再点一下手机照片。';
    }
  },
});
phone.audioHint.hidden = phone.audioEnabled;
document.querySelector(`input[name="dialmode"][value="${config.dialMode}"]`).checked = true;

enableBtn.addEventListener('click', () => phone.enableAudio());

document.getElementById('console-incoming').addEventListener('click', () => phone.simulateIncoming());
document.getElementById('console-reset').addEventListener('click', () => {
  resetConfig();
  config = loadConfig();
  phone.updateConfig(config);
  document.querySelector('input[name="dialmode"][value="tap"]').checked = true;
});

for (const input of document.querySelectorAll('input[name="dialmode"]')) {
  input.addEventListener('change', () => {
    config.dialMode = input.value;
    saveConfig(config);
    phone.updateConfig(config);
  });
}
