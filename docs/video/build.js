// Renders the FindPal YouTube promo (1920x1080, 30 fps, ~42 s) with an original soundtrack.
//   node docs/video/build.js [out.mp4]      (needs Playwright with Chromium, ffmpeg, python3 + numpy)
// Every frame is drawn by render(t) in the page, so the video is fully deterministic.
const { chromium } = require('playwright');
const { spawn, execFileSync } = require('child_process');
const fs = require('fs');
const os = require('os');
const path = require('path');
const { P, css: baseCss } = require('../social/common.js');

const FPS = 30;
const out = path.resolve(process.argv[2] || path.join(__dirname, 'findpal-promo.mp4'));
const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'findpal-video-'));

// ---- Timeline (seconds) -------------------------------------------------------------------
const S = { hook: [0, 4.6], problem: [4.6, 10.6], intro: [10.6, 14.4], demo: [14.4, 32.4], privacy: [32.4, 36.6], cta: [36.6, 42.6] };
const DURATION = S.cta[1];
const DEMO_LEN = 3.6;
const demos = [
  { q: 'wh', cap: 'Any app.<br><span class="hl">Two letters.</span>', eyebrow: 'Apps',
    cards: [['#3a9d8f', 'W', 'Weather', 'App'], ['#5b6cd9', 'W', 'Wallet', 'App']],
    top: ['<em>Wh</em>atsApp', 'App · opened 14 times today', '<span>Open</span><span>App info</span><span>Play Store</span>'], en: '↵ Open' },
  { q: 'rahul', cap: 'Type English.<br><span class="hl">Find हिंदी.</span>', eyebrow: 'Contacts',
    cards: [['#e8743b', 'रा', 'राहुल पटेल', 'Contact · +91 99251 22014'], ['#d9485f', 'R', 'Rahul Office', 'Contact']],
    top: ['राहुल शर्मा', 'Contact · matched “rahul”', '<span>Call</span><span>SMS</span><span class="wa">WhatsApp</span>'], en: '↵ Call' },
  { q: '', otp: true, cap: 'Your OTP.<br><span class="hl">One tap away.</span>', eyebrow: 'OTP',
    cards: [['#f2b54a', 'Pt', 'Paytm', 'Recent app'], ['#2f8fd8', 'Pe', 'PhonePe', 'Recent app']], en: '' },
  { q: '2400*15%', cap: 'Maths, money,<br><span class="hl">units & timers.</span>', eyebrow: 'Quick answers',
    cards: [['#5b6cd9', '⇄', '100 usd in inr', '₹ live rate'], ['#e0567a', '⏱', 'timer 10 min', 'Start in Clock']],
    top: ['= 360', 'Calculator · 2400 × 15%', '<span>Copy</span><span>Share</span>'], en: '↵ Copy' },
  { q: 'payment', cap: 'Search by<br><span class="hl">meaning.</span>', eyebrow: 'On-device AI',
    cards: [['#2f8fd8', 'Pt', 'Paytm', 'App · “payment”'], ['#3a9d8f', 'G', 'Google Pay', 'App · “payment”'], ['#e0567a', 'B', 'BHIM', 'App · “payment”']],
    top: ['PhonePe', 'App · matched by meaning', '<span>Open</span><span>App info</span><span>Play Store</span>'], en: '↵ Open' },
];
const TYPE_START = 0.35, CHAR = 0.12;
const foundAt = (d, i) => S.demo[0] + i * DEMO_LEN + (d.otp ? 0.5 : TYPE_START + d.q.length * CHAR + 0.25);
// Sound cues for the soundtrack: [time, kind].
const cues = [
  ...[5.0, 6.8, 8.6].map((t) => [t, 'thud']),
  [S.intro[0] + 0.15, 'whoosh'], [S.intro[0] + 0.5, 'chime'],
  ...demos.flatMap((d, i) => {
    const typing = d.q.split('').map((_, k) => [S.demo[0] + i * DEMO_LEN + TYPE_START + k * CHAR, 'tick']);
    const extra = d.otp ? [[S.demo[0] + i * DEMO_LEN + 1.6, 'tick'], [S.demo[0] + i * DEMO_LEN + 1.75, 'pop']] : [];
    return [...typing, [foundAt(d, i), 'pop'], ...extra];
  }),
  ...[0, 1, 2].map((k) => [S.privacy[0] + 0.9 + k * 0.45, 'tick']),
  [S.cta[0] + 0.3, 'whoosh'], [S.cta[0] + 0.7, 'chime'],
];

// ---- Page ---------------------------------------------------------------------------------
const pal = (face, arm, extra, size) => P(face, arm, extra, size);
const moods = (id, size, list) => `<div class="pal" id="${id}">${list.map(([k, f, a, e]) => `<div class="m" data-m="${k}">${pal(f, a, e, size)}</div>`).join('')}</div>`;

const css = baseCss + `
body{width:1920px;height:1080px;overflow:hidden;margin:0}
#stage{position:relative;width:1920px;height:1080px;overflow:hidden;font-family:Inter,sans-serif}
.layer{position:absolute;inset:0;opacity:0}
.bgdark{background:radial-gradient(70% 70% at 25% 10%,#3b220f,#120d09 70%)}
.bgprob{background:radial-gradient(80% 70% at 80% 0%,#3a2a24,#141011 70%)}
.bgorange{background:linear-gradient(150deg,#ffb067,#f07a2e 55%,#d9572a)}
.pal{position:absolute}
.pal .m{display:none}
.pal .m.on{display:block}
.pal svg{display:block;width:100%;height:auto}
h1{font:800 120px/0.98 "Inter Display",Inter,sans-serif;letter-spacing:-.045em;color:#fff1dc;margin:0}
.hl{color:#ffc48a}
.red{color:#ff8a7a}
.pcard{position:absolute;left:820px;width:900px;background:#221b19;border-radius:36px;padding:34px 40px;display:flex;gap:28px;align-items:center;box-shadow:0 30px 60px rgba(0,0,0,.45)}
.pcard .x{flex:none;width:72px;height:72px;border-radius:50%;background:#3d1d1a;color:#ff8a7a;font:800 40px Inter;display:flex;align-items:center;justify-content:center}
.pcard b{display:block;font:800 46px "Inter Display",Inter;color:#fff1dc;letter-spacing:-.02em}
.pcard span{font:500 30px Inter;color:#9c8f84}
.clock{position:absolute;right:90px;top:70px;font:800 44px Inter;color:#ff8a7a;background:#2a1716;border-radius:999px;padding:16px 34px}
.word{font:800 150px/1 "Baloo 2";color:#fff1dc}
.word b{color:#f07a2e}
.sub2{font:600 46px Inter;color:#ffc48a}
.eyebrow2{font:700 30px Inter;letter-spacing:.2em;text-transform:uppercase;color:#f7a35c}
.ph{position:absolute;left:1000px;top:60px;width:540px;border-radius:72px;padding:16px;background:#0b0806;box-shadow:0 0 0 12px #2a1d14,0 60px 120px rgba(0,0,0,.5)}
.ph .scr{border-radius:58px;background:#120d09;height:928px;padding:34px 24px 26px;display:flex;flex-direction:column;gap:14px;justify-content:flex-end;overflow:hidden;position:relative}
.ph .hd{display:flex;align-items:center;gap:12px;margin-bottom:auto;font:800 40px "Baloo 2";color:#fff1dc}
.ph .hd b{color:#f07a2e}
.ph .hd svg{width:66px;height:auto}
#res{display:flex;flex-direction:column;gap:14px;transform-origin:50% 100%}
.top .t{font-size:44px}
.top .s2{font:500 19px Inter;color:#7a5a40}
.otp{display:flex;align-items:center;gap:12px;background:#ffe8c2;color:#1d140e;border-radius:999px;padding:20px 24px;font:700 25px Inter}
.otp em{font-style:normal;color:#d9572a;font:800 34px "Baloo 2"}
.toast{position:absolute;left:50%;top:44%;transform:translate(-50%,-50%);background:#1d3a24;color:#9fe58a;font:800 30px Inter;border-radius:999px;padding:16px 30px;opacity:0}
.ripple{position:absolute;width:120px;height:120px;border-radius:50%;background:rgba(240,122,46,.45);opacity:0}
.ben{display:flex;gap:22px;align-items:center;color:#fff1dc;font:600 48px Inter;opacity:0}
.ben i{font-style:normal;flex:none;width:66px;height:66px;border-radius:50%;background:#f07a2e;color:#1d140e;display:flex;align-items:center;justify-content:center;font:800 36px Inter}
.ctabtn{display:inline-flex;align-items:center;gap:18px;font:800 46px Inter;color:#1d140e;background:linear-gradient(#fff3dc,#ffd9a0);border-radius:999px;padding:30px 56px;box-shadow:0 10px 0 #8a3c14}
.trial{display:inline-block;font:800 40px "Baloo 2";color:#1d140e;background:#fff3dc;border-radius:999px;padding:10px 34px;box-shadow:0 7px 0 #8a3c14}
`;

const html = `<div id="stage">
<div class="layer bgprob" id="hook">
  ${moods('p1', 520, [['w', 'worried', 'none', 'sweat']])}
  <h1 id="hookText" style="position:absolute;left:820px;top:330px"></h1>
</div>
<div class="layer bgprob" id="problem">
  ${moods('p2', 380, [['w', 'worried', 'none', 'sweat']])}
  <div class="clock" id="clock">⏱ 0 s wasted</div>
  <div class="pcard" id="pc0" style="top:190px"><span class="x">✕</span><div><b>4 home screens of apps</b><span>Swipe, scroll, open the drawer, give up…</span></div></div>
  <div class="pcard" id="pc1" style="top:440px"><span class="x">✕</span><div><b>“Your OTP expires in 0:09”</b><span>Leave the app, find it, squint, come back…</span></div></div>
  <div class="pcard" id="pc2" style="top:690px"><span class="x">✕</span><div><b>Saved “मम्मी”. Searched “mummy”.</b><span>No contacts found.</span></div></div>
</div>
<div class="layer bgdark" id="intro">
  ${moods('p3', 470, [['h', 'happy', 'wave', 'sparkle']])}
  <div id="introText" style="position:absolute;left:0;right:0;top:640px;text-align:center">
    <div class="word">Find<b>Pal</b></div>
    <div class="sub2" style="margin-top:10px">Search your whole phone in two letters.</div>
  </div>
</div>
<div class="layer bgdark" id="demo">
  <div style="position:absolute;left:140px;top:330px;width:820px">
    <div class="eyebrow2" id="eyebrow"></div>
    <h1 id="cap" style="margin-top:24px;font-size:112px"></h1>
  </div>
  <div class="ph"><div class="scr" id="scr">
    <div class="hd">${pal('happy', 'none', null, 66)}<span>Find<b>Pal</b></span></div>
    <div id="res"></div>
    <div class="sbar" style="font-size:30px"><span>🔍</span><span class="q" id="q"></span><span class="en" id="en"></span></div>
    <div class="ripple" id="ripple"></div><div class="toast" id="toast">Copied ✓</div>
  </div></div>
  ${moods('p4', 330, [['look', 'look', 'none', null], ['stars', 'stars', 'cheer', 'sparkle'], ['happy', 'happy', 'none', null]])}
</div>
<div class="layer bgdark" id="privacy">
  ${moods('p5', 500, [['cool', 'cool', 'point', 'sparkle']])}
  <div style="position:absolute;left:820px;top:200px;width:1000px">
    <h1 style="font-size:104px">Your phone. Your data.<br><span class="hl">Pal never tells.</span></h1>
    <div style="display:flex;flex-direction:column;gap:30px;margin-top:60px">
      <div class="ben" id="b0"><i>✓</i>Searched on your phone</div>
      <div class="ben" id="b1"><i>✓</i>Never uploaded</div>
      <div class="ben" id="b2"><i>✓</i>No ads. No tracking.</div>
    </div>
  </div>
</div>
<div class="layer bgorange" id="cta">
  ${moods('p6', 420, [['s', 'stars', 'cheer', 'sparkle']])}
  <div id="ctaText" style="position:absolute;left:820px;top:200px;width:1000px;color:#1d140e">
    <h1 style="color:#1d140e;font-size:116px">Stop digging.<br><span style="color:#fff3dc">Start finding.</span></h1>
    <div style="margin-top:44px"><span class="trial">Try Pro free for 7 days</span></div>
    <div style="margin-top:52px"><span class="ctabtn">▶&nbsp;Coming soon on Google Play</span></div>
    <div style="margin-top:44px;font:800 64px 'Baloo 2';color:#1d140e">Find<span style="color:#fff3dc">Pal</span></div>
  </div>
</div>
</div>`;

// Runs in the page. Pure function of t: sets every animated property for that instant.
function pageScript(S, demos, DEMO_LEN, TYPE_START, CHAR) {
  const $ = (id) => document.getElementById(id);
  const clamp = (x) => Math.max(0, Math.min(1, x));
  const easeOut = (x) => 1 - Math.pow(1 - clamp(x), 3);
  const back = (x) => { x = clamp(x); const c = 1.9; return 1 + (c + 1) * Math.pow(x - 1, 3) + c * Math.pow(x - 1, 2); };
  const within = (t, [a, b]) => t >= a - 0.4 && t < b + 0.4;
  const fade = (t, [a, b]) => Math.min(clamp((t - a + 0.2) / 0.4), clamp((b + 0.2 - t) / 0.4));

  function palAt(id, t, opts) {
    const el = $(id);
    const { x, y, size, mood, hop = -1, wave = false, scale = 1 } = opts;
    el.style.width = size + 'px';
    const bob = Math.sin(t * 2.4) * 10;
    const h = hop >= 0 && hop < 0.45 ? -46 * Math.sin(Math.PI * hop / 0.45) : 0;
    el.style.transform = `translate(${x}px,${y + bob + h}px) scale(${scale})`;
    el.querySelectorAll('.m').forEach((m) => m.classList.toggle('on', m.dataset.m === mood));
    const svg = el.querySelector('.m.on svg');
    if (!svg) return;
    const eyes = svg.querySelector('.eyes');
    const blink = (t % 3.1) < 0.14;
    if (eyes) eyes.setAttribute('transform', blink ? 'translate(0 88) scale(1 0.12) translate(0 -88)' : '');
    const arms = svg.querySelector('.arms');
    if (arms && wave) arms.setAttribute('transform', `rotate(${Math.sin(t * 7) * 14} 30 132)`);
  }

  window.render = function (t) {
    for (const k of Object.keys(S)) {
      const l = $(k);
      l.style.opacity = within(t, S[k]) ? fade(t, S[k]) : 0;
      l.style.display = within(t, S[k]) ? 'block' : 'none';
    }
    // Hook: worried Pal, headline types in.
    if (within(t, S.hook)) {
      const lt = t - S.hook[0];
      palAt('p1', t, { x: 170, y: 250, size: 520, mood: 'w', scale: 0.85 + 0.15 * easeOut(lt / 0.6) });
      const l1 = 'Lost something', l2 = 'on your phone?';
      const n = Math.floor(clamp((lt - 0.5) / 1.6) * (l1.length + l2.length));
      const a = l1.slice(0, Math.min(n, l1.length)), b = l2.slice(0, Math.max(0, n - l1.length));
      $('hookText').innerHTML = a + (n > l1.length ? `<br><span class="red">${b}</span>` : '');
    }
    // Problem: three cards slam in, a wasted-time counter runs.
    if (within(t, S.problem)) {
      const lt = t - S.problem[0];
      palAt('p2', t, { x: 220, y: 380, size: 380, mood: 'w' });
      [0, 1, 2].forEach((i) => {
        const k = back((lt - 0.4 - i * 1.8) / 0.5);
        const c = $('pc' + i);
        c.style.opacity = clamp((lt - 0.4 - i * 1.8) / 0.25);
        c.style.transform = `translateX(${(1 - k) * 700}px) rotate(${(1 - k) * 4}deg)`;
      });
      $('clock').textContent = `⏱ ${Math.floor(clamp(lt / 5.6) * 23)} s wasted`;
    }
    // Intro: whoosh, Pal pops and waves, wordmark rises.
    if (within(t, S.intro)) {
      const lt = t - S.intro[0];
      const k = back((lt - 0.1) / 0.6);
      palAt('p3', t, { x: 725, y: 110, size: 470, mood: 'h', wave: true, scale: 0.3 + 0.7 * k });
      const tx = $('introText');
      tx.style.opacity = clamp((lt - 0.5) / 0.4);
      tx.style.transform = `translateY(${(1 - easeOut((lt - 0.5) / 0.6)) * 60}px)`;
    }
    // Demos: typing into the phone, results pop up, Pal reacts.
    if (within(t, S.demo)) {
      const lt = Math.max(0, t - S.demo[0]);
      const i = Math.min(demos.length - 1, Math.floor(lt / DEMO_LEN));
      const d = demos[i];
      const dt = lt - i * DEMO_LEN;
      const res = $('res');
      if (res.dataset.i !== String(i)) {
        res.dataset.i = String(i);
        const card = ([bg, l, tt, s]) => `<div class="card"><span class="ic" style="background:${bg}">${l}</span><div><b>${tt}</b><span>${s}</span></div></div>`;
        res.innerHTML = d.otp
          ? `<div class="otp" id="otp">🔑 Copy OTP <em>482 913</em> · HDFC</div>${d.cards.map(card).join('')}`
          : `${d.cards.map(card).join('')}<div class="top"><div class="lab">TOP HIT</div><div class="t">${d.top[0]}</div><div class="s2">${d.top[1]}</div><div class="acts">${d.top[2]}</div></div>`;
        $('cap').innerHTML = d.cap;
        $('eyebrow').textContent = String(i + 1).padStart(2, '0') + ' · ' + d.eyebrow;
        $('en').textContent = d.en;
        $('en').style.display = d.en ? '' : 'none';
      }
      const typed = d.otp ? 0 : Math.max(0, Math.min(d.q.length, Math.floor((dt - TYPE_START) / CHAR) + 1));
      const q = $('q');
      q.textContent = d.otp ? 'Search…' : d.q.slice(0, typed);
      q.style.color = d.otp ? '#bfae98' : '';
      const found = d.otp ? 0.5 : TYPE_START + d.q.length * CHAR + 0.25;
      const k = back((dt - found) / 0.45);
      res.style.opacity = clamp((dt - found) / 0.2);
      res.style.transform = `translateY(${(1 - k) * 80}px) scale(${0.92 + 0.08 * k})`;
      const capEl = $('cap').parentElement;
      capEl.style.opacity = clamp(dt / 0.35) * clamp((DEMO_LEN - dt) / 0.3 + (i === demos.length - 1 ? 1 : 0));
      capEl.style.transform = `translateY(${(1 - easeOut(dt / 0.5)) * 40}px)`;
      // OTP tap: ripple on the pill, then a toast.
      const rp = $('ripple'), toast = $('toast');
      if (d.otp) {
        const tap = (dt - 1.6) / 0.5;
        rp.style.left = '200px'; rp.style.top = '520px';
        rp.style.opacity = tap > 0 && tap < 1 ? 1 - tap : 0;
        rp.style.transform = `scale(${0.3 + 2.2 * clamp(tap)})`;
        toast.style.opacity = clamp((dt - 1.75) / 0.2) * clamp((3.3 - dt) / 0.3);
      } else { rp.style.opacity = 0; toast.style.opacity = 0; }
      const since = dt - found;
      const mood = since < 0 ? 'look' : since < 1.4 ? 'stars' : 'happy';
      palAt('p4', t, { x: 1500, y: 560, size: 330, mood, hop: since });
    }
    // Privacy: cool Pal, ticks appear one by one.
    if (within(t, S.privacy)) {
      const lt = t - S.privacy[0];
      palAt('p5', t, { x: 160, y: 260, size: 500, mood: 'cool', scale: 0.85 + 0.15 * back(lt / 0.6) });
      [0, 1, 2].forEach((k) => {
        const b = $('b' + k);
        b.style.opacity = clamp((lt - 0.9 - k * 0.45) / 0.25);
        b.style.transform = `translateX(${(1 - easeOut((lt - 0.9 - k * 0.45) / 0.4)) * 60}px)`;
      });
    }
    // CTA: cheering Pal, headline, trial badge and Play button.
    if (within(t, S.cta)) {
      const lt = t - S.cta[0];
      palAt('p6', t, { x: 200, y: 280, size: 420, mood: 's', scale: 0.4 + 0.6 * back((lt - 0.2) / 0.6), hop: (lt % 2.2) - 1.2 });
      const c = $('ctaText');
      c.style.opacity = clamp((lt - 0.5) / 0.4);
      c.style.transform = `translateY(${(1 - easeOut((lt - 0.5) / 0.6)) * 50}px)`;
      // Fade to black over the last second.
      $('cta').style.filter = `brightness(${1 - clamp((t - (S.cta[1] - 1)) / 1) * 0.85})`;
    }
  };
}

(async () => {
  fs.writeFileSync(path.join(tmp, 'cues.json'), JSON.stringify({ duration: DURATION, cues }));
  execFileSync('python3', [path.join(__dirname, 'music.py'), path.join(tmp, 'cues.json'), path.join(tmp, 'audio.wav')], { stdio: 'inherit' });

  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1920, height: 1080 }, deviceScaleFactor: 1 });
  await page.setContent(`<!doctype html><html><head><meta charset="utf-8"><style>${css}</style></head><body>${html}</body></html>`);
  await page.evaluate(() => document.fonts.ready);
  await page.addScriptTag({ content: `(${pageScript.toString()})(${JSON.stringify(S)}, ${JSON.stringify(demos)}, ${DEMO_LEN}, ${TYPE_START}, ${CHAR});` });

  const ff = spawn('ffmpeg', ['-y', '-loglevel', 'error', '-f', 'image2pipe', '-framerate', String(FPS), '-i', '-',
    '-i', path.join(tmp, 'audio.wav'),
    '-c:v', 'libx264', '-preset', 'medium', '-crf', '18', '-pix_fmt', 'yuv420p', '-r', String(FPS),
    '-c:a', 'aac', '-b:a', '192k', '-shortest', '-movflags', '+faststart', out], { stdio: ['pipe', 'inherit', 'inherit'] });
  const frames = Math.round(DURATION * FPS);
  for (let f = 0; f < frames; f++) {
    await page.evaluate((t) => window.render(t), f / FPS);
    const png = await page.screenshot({ type: 'png' });
    if (!ff.stdin.write(png)) await new Promise((r) => ff.stdin.once('drain', r));
    if (f % 150 === 0) process.stdout.write(`frame ${f}/${frames}\n`);
  }
  ff.stdin.end();
  await new Promise((r) => ff.on('close', r));

  // YouTube thumbnail (1280x720): big claim, cheering Pal, the two-letter search.
  await page.setViewportSize({ width: 1280, height: 720 });
  await page.setContent(`<!doctype html><html><head><meta charset="utf-8"><style>${css}</style></head><body style="width:1280px;height:720px">
    <div style="position:relative;width:1280px;height:720px;overflow:hidden;background:radial-gradient(80% 90% at 20% 20%,#5a3014,#120d09 70%)">
      <div style="position:absolute;left:70px;top:90px;width:760px">
        <div style="font:800 112px/0.95 'Inter Display',Inter;letter-spacing:-.05em;color:#fff1dc">Find <span style="color:#ffc48a">anything</span><br>on your phone</div>
        <div style="margin-top:34px;display:inline-block;font:800 60px 'Baloo 2';color:#1d140e;background:#ffe8c2;border-radius:999px;padding:6px 36px;box-shadow:0 9px 0 #c7682b">in 2 letters</div>
        <div style="margin-top:56px;display:inline-flex;align-items:center;gap:16px;background:#1f160f;border-radius:26px;padding:18px 26px;font:600 40px Inter;color:#fff1dc;box-shadow:0 0 0 3px #3a2a1d">🔍 wh <span style="width:3px;height:40px;background:#f07a2e"></span> → <b style="color:#f07a2e">WhatsApp</b></div>
      </div>
      <div style="position:absolute;right:30px;top:120px;width:470px">${pal('stars', 'cheer', 'sparkle', 470)}</div>
    </div></body></html>`);
  await page.evaluate(() => document.fonts.ready);
  await page.screenshot({ path: out.replace(/\.mp4$/, '') + '-thumbnail.png' });
  await browser.close();
  fs.rmSync(tmp, { recursive: true, force: true });
  console.log('wrote', out);
})();
