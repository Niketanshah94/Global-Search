// Renders the FindPal "problem → solution" Instagram carousels (1080x1350, 4:5) into
// docs/social/problem-solution/. Each carousel is two slides: Pal stuck with an everyday
// phone problem, then FindPal fixing it.
//   node docs/social/build-problem-solution.js        (needs Playwright with Chromium)
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');
const { P, css: baseCss, logo, phone } = require('./common.js');

const out = path.join(__dirname, 'problem-solution');
fs.mkdirSync(out, { recursive: true });

const css = baseCss + `
.prob{background:radial-gradient(80% 60% at 80% 0%,#3a2a24,#141011 70%);color:#efe4d6}
.tag{display:inline-flex;align-items:center;gap:12px;font:800 26px Inter;letter-spacing:.16em;text-transform:uppercase;padding:12px 24px;border-radius:999px;align-self:flex-start;margin-bottom:34px}
.tag.bad{background:#3d1d1a;color:#ff8a7a}
.tag.good{background:#1d3a24;color:#9fe58a}
.cream .tag.good{background:#1d140e;color:#9fe58a}
.prob h1{color:#fff1dc}
.prob .hl{color:#ff8a7a}
.swipe{position:absolute;right:80px;bottom:150px;display:flex;align-items:center;gap:14px;font:800 34px "Baloo 2";color:#1d140e;background:#ffe8c2;border-radius:999px;padding:14px 30px;box-shadow:0 7px 0 #c7682b}
.clock{display:inline-flex;align-items:center;gap:10px;font:800 30px Inter;color:#ff8a7a;background:#2a1716;border-radius:999px;padding:10px 22px}
.pal-abs{position:absolute}
.mess{position:relative;margin-top:60px;flex:1}
.grid{display:grid;grid-template-columns:repeat(5,82px);gap:22px 26px;padding:30px;background:#1e1817;border-radius:44px;width:max-content;transform:rotate(-4deg);box-shadow:0 40px 80px rgba(0,0,0,.5)}
.grid i{width:82px;height:82px;border-radius:24px;display:block;opacity:.9}
.dots{display:flex;gap:10px;justify-content:center;margin-top:22px}
.dots i{width:14px;height:14px;border-radius:50%;background:#5a4a44}
.dots i.on{background:#efe4d6}
.notif{background:#221b19;border-radius:26px;padding:20px 24px;display:flex;gap:18px;align-items:center;width:600px;box-shadow:0 20px 40px rgba(0,0,0,.4)}
.notif .ic{width:54px;height:54px;border-radius:16px;flex:none}
.notif b{display:block;font:700 24px Inter;color:#efe4d6}
.notif span{font:500 20px Inter;color:#9c8f84}
.list{background:#1e1817;border-radius:40px;padding:26px;width:620px;box-shadow:0 40px 80px rgba(0,0,0,.5)}
.list .s{background:#2a2220;border-radius:20px;padding:18px 22px;font:500 28px Inter;color:#efe4d6;display:flex;gap:12px;align-items:center}
.list .empty{text-align:center;padding:70px 0 50px;font:600 30px Inter;color:#9c8f84}
.list .empty big{display:block;font-size:80px;margin-bottom:10px}
.folder{display:flex;align-items:center;gap:18px;background:#221b19;border-radius:22px;padding:18px 22px;font:600 26px Inter;color:#efe4d6;width:560px}
.folder i{font-style:normal;font-size:34px}
.folder span{margin-left:auto;font:500 20px Inter;color:#9c8f84}
.apps3{display:flex;align-items:center;gap:18px;margin-top:20px}
.app3{background:#221b19;border-radius:30px;padding:26px 24px;text-align:center;width:230px}
.app3 .ic{width:84px;height:84px;border-radius:24px;margin:0 auto 14px;display:flex;align-items:center;justify-content:center;font:800 40px Inter;color:#fff}
.app3 b{font:700 26px Inter;color:#efe4d6}
.screen{height:600px}
.arrow{font:800 44px Inter;color:#6b5a52}
`;

const foot = (dark, n) => `<div class="foot">${logo(dark)}<span class="soon">Problem → Solution · ${n}/5</span></div>`;
const swipe = `<div class="swipe">Swipe for the fix →</div>`;
const bad = (t) => `<span class="tag bad">✕ ${t}</span>`;
const good = (t) => `<span class="tag good">✓ ${t}</span>`;
const sadPal = (size, extra = 'sweat') => P('worried', 'none', extra, size);
const sbar = (q, en) => `<div class="sbar"><span style="font-size:30px">🔍</span><span class="q">${q}</span><span class="en">${en}</span></div>`;
const top = (title, sub, acts) =>
  `<div class="top"><div class="lab">TOP HIT</div><div class="t">${title}</div><div style="font:500 20px Inter;color:#7a5a40">${sub}</div>${acts ? `<div class="acts">${acts}</div>` : ''}</div>`;
const card = (bg, letter, title, sub) =>
  `<div class="card"><span class="ic" style="background:${bg}">${letter}</span><div><b>${title}</b><span>${sub}</span></div></div>`;

const icons = ['#e8743b', '#3a9d8f', '#d9485f', '#f2b54a', '#5b6cd9', '#7acc5a', '#8a5bd9', '#e0567a', '#2f8fd8', '#c98a3a'];

const slides = [
  // 1 · Finding an app
  ['01a-apps-problem', `<div class="post prob">
    ${bad('The problem')}
    <h1>4 home screens.<br><span class="hl">Where is that app?</span></h1>
    <div class="sub">Swipe, scroll, open the drawer, give up, swipe again.</div>
    <div class="mess">
      <div style="position:absolute;left:10px;top:30px"><div class="grid">${Array.from({ length: 20 }, (_, i) => `<i style="background:${icons[(i * 7) % icons.length]}"></i>`).join('')}</div>
      <div class="dots"><i></i><i></i><i class="on"></i><i></i></div></div>
      <div class="pal-abs" style="right:-10px;top:120px">${sadPal(360)}</div>
      <div class="pal-abs" style="left:40px;bottom:120px"><span class="clock">⏱ 18 seconds</span></div>
    </div>
    ${swipe}${foot(true, 1)}</div>`],
  ['01b-apps-solution', `<div class="post cream">
    ${good('The fix')}
    <h1>Type two letters.<br><span class="hl">There it is.</span></h1>
    <div style="display:flex;align-items:flex-end;gap:10px;margin-top:50px">
      ${phone(`${card('#3a9d8f', 'W', 'Weather', 'App')}${top('<em>Wh</em>atsApp', 'App · opened 14 times today', '<span>Open</span><span>App info</span><span>Play Store</span>')}${sbar('wh', '↵ Open')}`)}
      <div style="margin-bottom:90px">${P('stars', 'cheer', 'sparkle', 330)}</div>
    </div>
    <div class="pal-abs" style="right:90px;top:430px"><span class="bubble">1 second!</span></div>
    ${foot(false, 1)}</div>`],

  // 2 · OTP
  ['02a-otp-problem', `<div class="post prob">
    ${bad('The problem')}
    <h1>OTP sent.<br><span class="hl">Code expires in 0:09.</span></h1>
    <div class="sub">Leave the app, find the message, squint, memorise six digits, come back… expired.</div>
    <div class="mess">
      <div style="display:flex;flex-direction:column;gap:16px;transform:rotate(-3deg);margin-top:20px">
        ${[['#2f8fd8', 'Promo', '50% off on everything!! Shop now'], ['#7acc5a', 'Family group', '23 new messages'], ['#e0567a', 'Bank alert', 'Your OTP is 4•2•9•1•… do not share'], ['#f2b54a', 'Delivery', 'Your order is out for delivery']]
          .map(([c, t, s]) => `<div class="notif"><span class="ic" style="background:${c}"></span><div><b>${t}</b><span>${s}</span></div></div>`).join('')}
      </div>
      <div class="pal-abs" style="right:-20px;bottom:150px">${sadPal(330)}</div>
    </div>
    ${swipe}${foot(true, 2)}</div>`],
  ['02b-otp-solution', `<div class="post orange">
    <span class="tag good" style="background:#1d140e">✓ The fix</span>
    <h1>Open FindPal.<br><span class="hl">Tap. Copied.</span></h1>
    <div class="sub">Your latest OTP waits at the top, ready to paste. It never leaves your phone.</div>
    <div style="margin-top:70px;align-self:center" class="chip">🔑 Copy OTP <span style="color:#f07a2e;font-family:'Baloo 2';font-size:44px">482 913</span> · HDFC</div>
    <div style="align-self:center;margin-top:40px">${P('happy', 'wave', 'sparkle', 380)}</div>
    ${foot(false, 2)}</div>`],

  // 3 · Contact saved in Hindi
  ['03a-contact-problem', `<div class="post prob">
    ${bad('The problem')}
    <h1>Saved as “मम्मी”.<br><span class="hl">Searched “mummy”.</span></h1>
    <div class="sub">Your phone's contact search only matches the exact script. So: nothing.</div>
    <div class="mess">
      <div class="list" style="transform:rotate(-3deg)">
        <div class="s">🔍 mummy</div>
        <div class="empty"><big>🤷</big>No contacts found</div>
      </div>
      <div class="pal-abs" style="right:-20px;top:170px">${sadPal(340)}</div>
    </div>
    ${swipe}${foot(true, 3)}</div>`],
  ['03b-contact-solution', `<div class="post dark">
    ${good('The fix')}
    <h1>Type in English.<br><span class="hl">Pal reads हिंदी.</span></h1>
    <div class="sub">Works across 9 Indian scripts, from मराठी to தமிழ்.</div>
    <div style="display:flex;align-items:flex-end;gap:10px;margin-top:40px">
      ${phone(`${card('#e8743b', 'म', 'मामा', 'Contact')}${top('मम्मी', 'Contact · matched “mummy”', '<span>Call</span><span>SMS</span><span class="wa">WhatsApp</span>')}${sbar('mummy', '↵ Call')}`)}
      <div style="margin-bottom:60px">${P('happy', 'point', 'sparkle', 330)}</div>
    </div>
    ${foot(true, 3)}</div>`],

  // 4 · Lost file
  ['04a-file-problem', `<div class="post prob">
    ${bad('The problem')}
    <h1>“Send your Aadhaar.”<br><span class="hl">Where's the PDF?</span></h1>
    <div class="sub">Downloads? WhatsApp? Telegram? Was it scan(3).pdf?</div>
    <div class="mess">
      <div style="display:flex;flex-direction:column;gap:14px;transform:rotate(-3deg);margin-top:10px">
        ${[['📁', 'Downloads', '1,284 files'], ['📁', 'WhatsApp Documents', '612 files'], ['📁', 'Telegram', '389 files'], ['📄', 'scan(3).pdf', '?'], ['📄', 'document_final_v2.pdf', '?']]
          .map(([i, t, s]) => `<div class="folder"><i>${i}</i>${t}<span>${s}</span></div>`).join('')}
      </div>
      <div class="pal-abs" style="right:-20px;bottom:150px">${sadPal(330)}</div>
    </div>
    ${swipe}${foot(true, 4)}</div>`],
  ['04b-file-solution', `<div class="post cream">
    ${good('The fix')}
    <h1>Type “aadhaar”.<br><span class="hl">Found it.</span></h1>
    <div style="display:flex;align-items:flex-end;gap:10px;margin-top:50px">
      ${phone(`${card('#d9485f', 'IMG', 'Aadhaar_back.jpg', 'WhatsApp Images')}${top('<em>Aadhaar</em>_card.pdf', 'Downloads · 412 kB', '<span>Open</span><span>Share</span><span>Folder</span>')}${sbar('aadhaar', '↵ Open')}`)}
      <div style="margin-bottom:90px">${P('stars', 'cheer', 'sparkle', 330)}</div>
    </div>
    ${foot(false, 4)}</div>`],

  // 5 · Three apps for quick answers
  ['05a-maths-problem', `<div class="post prob">
    ${bad('The problem')}
    <h1>Three apps<br><span class="hl">for one answer.</span></h1>
    <div class="sub">Calculator for the tip. A currency app for the price. The clock for the chai timer.</div>
    <div class="apps3" style="margin-top:70px">
      <div class="app3"><div class="ic" style="background:#5b6cd9">±</div><b>Calculator</b></div><span class="arrow">→</span>
      <div class="app3"><div class="ic" style="background:#3a9d8f">$</div><b>Currency</b></div><span class="arrow">→</span>
      <div class="app3"><div class="ic" style="background:#e0567a">⏱</div><b>Clock</b></div>
    </div>
    <div style="align-self:center;margin-top:30px">${sadPal(330)}</div>
    ${swipe}${foot(true, 5)}</div>`],
  ['05b-maths-solution', `<div class="post dark">
    ${good('The fix')}
    <h1>One search bar.<br><span class="hl">Every answer.</span></h1>
    <div style="display:flex;flex-direction:column;gap:20px;margin-top:60px;width:640px">
      ${[['2400*15%', '360'], ['100 usd in inr', 'live rate ₹'], ['5 ft in cm', '152.4 cm'], ['timer 10 min', '⏱ started']]
        .map(([q, a]) => `<div class="sbar" style="font-size:32px;padding:26px 28px"><span class="q" style="flex:1">${q}</span><b style="color:#f07a2e;font:800 34px Inter">${a}</b></div>`).join('')}
    </div>
    <div class="pal-abs" style="right:40px;bottom:150px">${P('cool', 'point', 'sparkle', 330)}</div>
    ${foot(true, 5)}</div>`],
];

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({ viewport: { width: 1080, height: 1350 }, deviceScaleFactor: 1 });
  for (const [name, html] of slides) {
    await page.setContent(`<!doctype html><html><head><meta charset="utf-8"><style>${css}</style></head><body>${html}</body></html>`);
    await page.evaluate(() => document.fonts.ready);
    await page.screenshot({ path: path.join(out, `findpal-ps-${name}.png`) });
  }
  // Overview: each carousel as a row (problem, then fix).
  const thumbs = slides.map(([n]) => `<img src="data:image/png;base64,${fs.readFileSync(path.join(out, `findpal-ps-${n}.png`)).toString('base64')}" style="width:324px;height:405px;border-radius:14px">`).join('');
  const sheet = await browser.newPage({ viewport: { width: 1740, height: 900 } });
  await sheet.setContent(`<body style="margin:0;background:#0b0806;padding:30px;display:grid;grid-template-columns:repeat(5,324px);grid-auto-flow:column;grid-template-rows:repeat(2,405px);gap:24px">${thumbs}</body>`);
  await sheet.screenshot({ path: path.join(out, 'findpal-ps-overview.png'), fullPage: true });
  await browser.close();
})();
